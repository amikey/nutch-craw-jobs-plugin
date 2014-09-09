/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.nutch.scoring.nextpage;

import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map.Entry;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.MapWritable;
import org.apache.hadoop.io.Text;
import org.apache.nutch.crawl.CrawlDatum;
import org.apache.nutch.crawl.Inlinks;
import org.apache.nutch.indexer.NutchDocument;
import org.apache.nutch.metadata.Metadata;
import org.apache.nutch.parse.Outlink;
import org.apache.nutch.parse.Parse;
import org.apache.nutch.parse.ParseData;
import org.apache.nutch.protocol.Content;
import org.apache.nutch.scoring.ScoringFilter;
import org.apache.nutch.scoring.ScoringFilterException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NextPageAnalysisScoringFilter
  implements ScoringFilter {

  private Configuration conf;
  private float normalizedScore = 1.00f;
  public static String LAST_FETCH_TIME = "LAST_FETCH_TIME";
  private SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy");  
  private static final Logger LOG = LoggerFactory.getLogger(NextPageAnalysisScoringFilter.class);
  public static String PARSE_PARENT_URL="PARSE_PARENT_URL";
  
  public NextPageAnalysisScoringFilter() {

  }

  public Configuration getConf() {
    return conf;
  }

  public void setConf(Configuration conf) {
    this.conf = conf;
    normalizedScore = conf.getFloat("link.analyze.normalize.score", 1.00f);
  }

  public CrawlDatum distributeScoreToOutlinks(Text fromUrl,
    ParseData parseData, Collection<Entry<Text, CrawlDatum>> targets,
    CrawlDatum adjust, int allCount)
    throws ScoringFilterException {
    return adjust;
  }

  public float generatorSortValue(Text url, CrawlDatum datum, float initSort)
    throws ScoringFilterException {
    return datum.getScore() * initSort;
  }

  public float indexerScore(Text url, NutchDocument doc, CrawlDatum dbDatum,
    CrawlDatum fetchDatum, Parse parse, Inlinks inlinks, float initScore)
    throws ScoringFilterException {
    return (normalizedScore * dbDatum.getScore());
  }

  public void initialScore(Text url, CrawlDatum datum)
    throws ScoringFilterException {
    datum.setScore(0.0f);
  }

  public void injectedScore(Text url, CrawlDatum datum)
    throws ScoringFilterException {
  }

  public void passScoreAfterParsing(Text url, Content content, Parse parse)
    throws ScoringFilterException { 
	  
	  
	  if(LOG.isDebugEnabled())
		   LOG.debug("nextpage analysis : filtering outlinks for "+url.toString());
	  
	   long lastFetchTime = Long.valueOf(content.getMetadata().get(NextPageAnalysisScoringFilter.LAST_FETCH_TIME)).longValue();
	   
	   Outlink[] links = parse.getData().getOutlinks();
	   List<Outlink> nextPageOutLinks = new ArrayList<Outlink>();
	   for (int i = 0; i < links.length; i++) {
		Outlink outlink = links[i];
		if(outlink.getMetadata()!=null){
			
			
			if(outlink.getMetadata().get(new Text("nextPage"))!=null){
				nextPageOutLinks.add(outlink);
				
			}
			
		}
	
	   }
	   
	   //check for browser generated nextpage
	   if(content.getMetadata().get("nextPageIndex")!=null){
		   try {
			   
			   Outlink nextPageOutlink = generateNextPageLink(url.toString(),content.getMetadata().get("nextPageIndex"));
			   
			   //add the number of links
			   
			   nextPageOutLinks.add(nextPageOutlink);
			   
		} catch (Exception e) {
			// TODO: handle exception
		}
		   
	   }
   
  
   
   List<Outlink> filteredLinks = new ArrayList<Outlink>();
   boolean outdatedLinks = false;
   int totalLinksNumber = links.length ;
   int newLinksNumber= 0;// (posterior to last fetch time )
   
   for (int i = 0; i < links.length; i++) {
		Outlink outlink = links[i];
		if(outlink.getMetadata()!=null && outlink.getMetadata().get(new Text("offerdate"))!=null){
			String offerdate = outlink.getMetadata().get(new Text("offerdate")).toString();
			try {
				if(LOG.isDebugEnabled()){
					String lasFetchDate = new Date(lastFetchTime).toString();
					LOG.debug("Last fetch date : "+lasFetchDate+" link date : "+offerdate);
				}
				long offertime = ((Date)dateFormat.parseObject(offerdate)).getTime();
				if(offertime >= lastFetchTime){
					//some links might overlap between two crawls, as the above inequality is not strict . This is however necessary to avoid missing some links due to
					//time rounding errors. If links are collected two times, they will/should be dedupped on their url/signature basis.
					newLinksNumber++;
					if(outlink.getMetadata().get(new Text("shouldFetch"))==null){
						upgradeOutLinks(content, outlink,false);
						filteredLinks.add(outlink);
					}
					
				}else{
					outdatedLinks = true;
				}
			} catch (Exception e) {
				LOG.warn("Outlink filter Failed to parse offerdate : "+offerdate+" for "+outlink.getToUrl());
			}
			
		}
   }
   
  
   
    
   if(!outdatedLinks && totalLinksNumber > 0){
	   
	   if(nextPageOutLinks.size() == 0){
		   //it's the first page fetched and no next page outlink has been captured by the parsing model
		   try {
			   nextPageOutLinks.add(generateNextPageLink(url.toString(), null));
			} catch (Exception e) {
				LOG.error("Failed to generate nextpage out link", e);
			}
		   
	   }
	   
	   filteredLinks.addAll(nextPageOutLinks);
	   for (Outlink outlink : nextPageOutLinks) {
		   upgradeOutLinks(content, outlink,true);
	   }
	   
	  
   }
   
   /*if(nextPageOutLinks.size()==0){
	   if(LOG.isDebugEnabled())
		   LOG.debug("nextpage analysis : found no nextpage link  ");
	  // return;
   }*/
   if(LOG.isInfoEnabled() && newLinksNumber > 0)
	   LOG.info("nextpage analysis : found "+newLinksNumber+ " new outlinks , over "+totalLinksNumber+" total links number");
  
   if(LOG.isInfoEnabled()&&nextPageOutLinks.size() > 0){
	   LOG.info("nextpage analysis : found "+nextPageOutLinks.size()+"  nextpage links : ");
	   if(LOG.isDebugEnabled()){
		   for (Outlink outlink : nextPageOutLinks) {
			LOG.debug( outlink.getToUrl());
		   }
		   
	   }
   }
   
   Outlink[] updatedLinks = filteredLinks.toArray(new Outlink[filteredLinks.size()]);
   parse.getData().setOutlinks(updatedLinks);
	   		
  }
  
  private Outlink generateNextPageLink(String baseUrlString,String previousNextPageIndexString) throws MalformedURLException{
	  if(previousNextPageIndexString==null)
		  previousNextPageIndexString = "1";
	  
	  int lastNextPageIndex = Integer.valueOf(previousNextPageIndexString).intValue();
	  int nextPageIndex = lastNextPageIndex +1;
	   
	   Outlink nextPageOutlink = new Outlink("", "nextPage "+nextPageIndex);
	   URL base = new URL(baseUrlString);
	   String file = base.getFile();
	   String toUrl = base.toString();
	   if(file.equals("/")){
		   toUrl = base.toString()+"nextPage/"+nextPageIndex;
	   }else{
		   toUrl = base.toString().replace(file, "/nextPage/"+nextPageIndex);
	   }
	   nextPageOutlink.setUrl(toUrl);
	   MapWritable outlinkMeta = new MapWritable();
	   nextPageOutlink.setMetadata(outlinkMeta);
	   outlinkMeta.put(new Text("nextPageIndex"), new IntWritable(nextPageIndex));
	   return nextPageOutlink;
  }
  
  private void upgradeOutLinks(Content parentContent, Outlink outlink,boolean isNextPageLink){
	  
	  MapWritable outlinkMeta = outlink.getMetadata();
	 /* if(isNextPageLink && outlinkMeta.get(new Text("nextPage"))!=null)
		  return;*///No cookies necessary for next page browser driven
	  
	  
	  Metadata parentContentMeta= parentContent.getMetadata();
	  
		
		if(parentContentMeta.get("Cookie")!=null){
			outlinkMeta.put(new Text("Cookie"),new Text(parentContentMeta.get("Cookie")));
			outlinkMeta.put(new Text("CookieDomain"),new Text(parentContentMeta.get("CookieDomain")));
			outlinkMeta.put(new Text("CookiePath"),new Text(parentContentMeta.get("CookiePath")));
			outlinkMeta.put(new Text("CookieExpiry"),new Text(parentContentMeta.get("CookieExpiry")));
			outlinkMeta.put(new Text("CookieSecure"),new Text(parentContentMeta.get("CookieSecure")));
		
		}
		
		//we must add a parent url meta , so that the XPathIndexingFilter plugin can use the correct scheme to cast the metas and add them to Nutch document
		outlinkMeta.put(new Text(PARSE_PARENT_URL), new Text(parentContent.getBaseUrl()));
  }

  public void passScoreBeforeParsing(Text url, CrawlDatum datum, Content content)
    throws ScoringFilterException {
	long lastFetchTime = datum.getFetchTime() - ((long)datum.getFetchInterval() * 1000);
	Date lastFetchDate = new Date(lastFetchTime);
	if(datum.getFetchInterval()>=(24*3600))
		lastFetchDate.setHours(0);
	if(datum.getFetchInterval()>=(3600))
		lastFetchDate.setMinutes(0);
	
	lastFetchDate.setSeconds(0);
	
	lastFetchTime = ((long)Math.ceil(lastFetchDate.getTime()/1000))*1000;
	/*if(LOG.isDebugEnabled()){
		
		LOG.debug("For url  : "+url.toString());
		LOG.debug("LastFetchTime was  : "+lastFetchDate.toString());
	}*/
    content.getMetadata().set(NextPageAnalysisScoringFilter.LAST_FETCH_TIME, "" + lastFetchTime);
    
    
    if(datum.getMetaData().get(new Text("nextPage"))!=null){
    	datum.setStatus(CrawlDatum.STATUS_FETCH_GONE);;
    }
    if(datum.getMetaData().get(new Text("offerdate"))!=null){
    	content.getMetadata().set("offerdate", datum.getMetaData().get(new Text("offerdate")).toString());
    }
  }

  public void updateDbScore(Text url, CrawlDatum old, CrawlDatum datum,
    List<CrawlDatum> inlinked)
    throws ScoringFilterException {
    // nothing to do
  }

}

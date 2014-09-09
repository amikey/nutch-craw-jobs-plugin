/**
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

package org.apache.nutch.indexer.blacklight;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.nutch.metadata.Nutch;
import org.apache.nutch.parse.Parse;
import org.apache.nutch.indexer.IndexingFilter;
import org.apache.nutch.indexer.IndexingException;
import org.apache.nutch.indexer.NutchDocument;
import org.apache.nutch.indexer.NutchField;
import org.apache.nutch.util.StringUtil;
import org.apache.nutch.util.URLUtil;
import org.apache.hadoop.io.MD5Hash;
import org.apache.hadoop.io.Text;
import org.apache.nutch.crawl.CrawlDatum;
import org.apache.nutch.crawl.Inlinks;

import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.apache.hadoop.conf.Configuration;

/** 
 * Adds basic blacklight searchable fields to a document. 
 * The fields added are : 
 * domain is included depending on {@code indexer.add.domain} in nutch-default.xml.
 * title is truncated as per {@code indexer.max.title.length} in nutch-default.xml. 
 *       (As per NUTCH-1004, a zero-length title is not added)
 * content is truncated as per {@code indexer.max.content.length} in nutch-default.xml.
 */

public class BlacklightIndexingFilter implements IndexingFilter {
  public static final Logger LOG = LoggerFactory.getLogger(BlacklightIndexingFilter.class);

  private int MAX_TITLE_LENGTH;
  private int MAX_CONTENT_LENGTH;
  private boolean addDomain = false;
  private Configuration conf;
 

 /**
  * The {@link BasicIndexingFilter} filter object which supports few 
  * configuration settings for adding basic searchable fields. 
  * See {@code indexer.add.domain}, {@code indexer.max.title.length}, 
  * {@code indexer.max.content.length} in nutch-default.xml.
  *  
  * @param doc The {@link NutchDocument} object
  * @param parse The relevant {@link Parse} object passing through the filter 
  * @param url URL to be filtered for anchor text
  * @param datum The {@link CrawlDatum} entry
  * @param inlinks The {@link Inlinks} containing anchor text
  * @return filtered NutchDocument
  */
  public NutchDocument filter(NutchDocument doc, Parse parse, Text url, CrawlDatum datum, Inlinks inlinks)
    throws IndexingException {

    Text reprUrl = (Text) datum.getMetaData().get(Nutch.WRITABLE_REPR_URL_KEY);
    String reprUrlString = reprUrl != null ? reprUrl.toString() : null;
    String urlString = url.toString();
    
    //add url digest (id) url digest to avoid problems of dots within url (natural id) in blacklight/ruby
    
    /*
   
    doc.add("digest", digest);*/
    
    if(doc.getField("offerdate")==null)
    	return null;
    Object pdObject = null;
    String pub_date_t=null;
    if(doc.getField("offerdate")!=null)
    	pdObject = doc.getField("offerdate").getValues().get(0);
    try{
	    if(pdObject == null)
	    	pdObject = new Date(0);
	    if(pdObject instanceof String){
	    	
	    		pdObject =(Date) new SimpleDateFormat("dd.MM.yyyy").parseObject((String)pdObject);
	    	
	    }
	    pub_date_t = new SimpleDateFormat("dd/MM/yyyy").format(pdObject);
    }catch(ParseException e){
		pdObject = new Date(0);
		LOG.warn("failed to parse pub_date for blacklight");
	}
	
    Date pubDate = (Date)pdObject;
    
    long pubdateSort = pubDate.getTime()/1000;
    
    doc.add("pub_date_sort", pubdateSort);
    
    doc.add("pub_date_t", pub_date_t);
    doc.removeField("offerdate");
    
    String cleanedUrl = ((String)doc.getField("url").getValues().get(0)).replaceFirst("http(s)?://", "");
    
    if(doc.getField("Location")!=null){
    	cleanedUrl = ((String)(doc.getField("Location").getValues().get(0))).replaceFirst("http(s)?://", "");
    	doc.removeField("Location");
     
    }
    
    doc.getField("url").getValues().set(0, cleanedUrl);
	String digest = StringUtil.toHexString(MD5Hash.digest(cleanedUrl).getDigest());
	doc.getField("digest").getValues().set(0, digest);
	
	if(doc.getField("host")!=null){
		String host = (String)doc.getField("host").getValues().get(0);
		String host_facet = host.split("\\.")[1];
		doc.add("host_facet", host_facet);
	}
	
	
	//TODO:specify remove cache flag in conf
	doc.removeField("cache");
	
	
	//getSpotlightAnnotations(doc);
    /*
    
   migrated to the schema
    
    //sort fields
    if(key.equals("jobplace_t")||key.equals("societyname_t")){
    	String fieldFacet = (String)val;
    	inputDoc.addField(key.replace("_t", "_facet"), fieldFacet);
    }
    
    
    
  //facets
    if(key.equals("offertitle_t")||key.equals("societyname_t")){
    	String fieldSort = (String)val;
    	inputDoc.addField(key.replace("_t", "_sort"), fieldSort);
    }
    
    //display
    if(key.endsWith("_t")){
    	String fieldDisplay = (String)val;
    	inputDoc.addField(key.replace("_t", "_display"), fieldDisplay);
    }*/
   
   

    return doc;
  }
  
  

  /**
   * Set the {@link Configuration} object
   */
  public void setConf(Configuration conf) {
    this.conf = conf;
    this.MAX_TITLE_LENGTH = conf.getInt("indexer.max.title.length", 100);
    this.addDomain = conf.getBoolean("indexer.add.domain", false);
    this.MAX_CONTENT_LENGTH = conf.getInt("indexer.max.content.length", -1);
  
  }

  /**
   * Get the {@link Configuration} object
   */
  public Configuration getConf() {
    return this.conf;
  }

}

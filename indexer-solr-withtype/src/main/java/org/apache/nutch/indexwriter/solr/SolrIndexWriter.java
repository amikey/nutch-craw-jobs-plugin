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
package org.apache.nutch.indexwriter.solr;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.MD5Hash;
import org.apache.hadoop.mapred.JobConf;
import org.apache.nutch.indexer.IndexWriter;
import org.apache.nutch.indexer.IndexerMapReduce;
import org.apache.nutch.indexer.NutchDocument;
import org.apache.nutch.indexer.NutchField;
import org.apache.nutch.util.StringUtil;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.request.UpdateRequest;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.util.DateUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ibm.icu.text.SimpleDateFormat;

public class SolrIndexWriter implements IndexWriter {

    public static final Logger LOG = LoggerFactory
            .getLogger(SolrIndexWriter.class);

    private SolrServer solr;
    private SolrMappingReader solrMapping;
    private ModifiableSolrParams params;

    private Configuration config;

    private final List<SolrInputDocument> inputDocs = new ArrayList<SolrInputDocument>();

    private int batchSize;
    private int numDeletes = 0;
    private boolean delete = false;

    private SimpleDateFormat pdf = new SimpleDateFormat("dd-MM-yyyy");
    
    public void open(JobConf job, String name) throws IOException {
        SolrServer server = SolrUtils.getCommonsHttpSolrServer(job);
        init(server, job);
    }

    // package protected for tests
    void init(SolrServer server, JobConf job) throws IOException {
        solr = server;
        batchSize = job.getInt(SolrConstants.COMMIT_SIZE, 1000);
        solrMapping = SolrMappingReader.getInstance(job);
        delete = job.getBoolean(IndexerMapReduce.INDEXER_DELETE, false);
        // parse optional params
        params = new ModifiableSolrParams();
        String paramString = job.get(IndexerMapReduce.INDEXER_PARAMS);
        if (paramString != null) {
            String[] values = paramString.split("&");
            for (String v : values) {
                String[] kv = v.split("=");
                if (kv.length < 2) {
                    continue;
                }
                params.add(kv[0], kv[1]);
            }
        }
    }

    public void delete(String key) throws IOException {
        if (delete) {
            try {
                solr.deleteById(key);
                numDeletes++;
            } catch (final SolrServerException e) {
                throw makeIOException(e);
            }
        }
    }

    @Override
    public void update(NutchDocument doc) throws IOException {
        write(doc);
    }

    public void write(NutchDocument doc) throws IOException {
        final SolrInputDocument inputDoc = new SolrInputDocument();
        for (final Entry<String, NutchField> e : doc) {
            for (final Object val : e.getValue().getValues()) {
                // normalise the string representation for a Date
                Object val2 = val;

                if (val instanceof Date) {
                    val2 = DateUtil.getThreadLocalDateFormat().format(val);
                }

                if (e.getKey().equals("content") || e.getKey().equals("title")) {
                    val2 = SolrUtils.stripNonCharCodepoints((String) val);
                }
               
                String key = solrMapping.mapKey(e.getKey());
                
                String type = solrMapping.mapType(e.getKey());
                
                	Object castedValue = castString(type, val);
                		
                	inputDoc.addField(solrMapping.mapKey(e.getKey()), castedValue, e
                        .getValue().getWeight());
                /*	if(LOG.isDebugEnabled())
                	 LOG.debug("adding solr field : "+solrMapping.mapKey(e.getKey())+" "+castedValue.toString().trim().replaceAll("\\n", "").substring(0,Math.min(castedValue.toString().length(),50))+ " "+e.getValue().getWeight());
                */	 
                
                String sCopy = solrMapping.mapCopyKey(e.getKey());
                if (sCopy != e.getKey()) {
                    inputDoc.addField(sCopy, castedValue);
                    if(LOG.isDebugEnabled())
                    	LOG.debug("adding solr field : "+sCopy+" "+castedValue);
                }
            }
        }
        LOG.debug("Next doc");
        inputDoc.setDocumentBoost(doc.getWeight());
        inputDocs.add(inputDoc);
        if (inputDocs.size() + numDeletes >= batchSize) {
            try {
                LOG.info("Indexing " + Integer.toString(inputDocs.size())
                        + " documents");
                LOG.info("Deleting " + Integer.toString(numDeletes)
                        + " documents");
                
                numDeletes = 0;
                
                UpdateRequest req = new UpdateRequest();
                req.add(inputDocs);
                req.setParams(params);
                
                LOG.info(req.toString());
                
                req.process(solr);
                
            } catch (final SolrServerException e) {
            	LOG.error(e.getMessage());
                throw makeIOException(e);
            }
            inputDocs.clear();
        }
    }
    
    private Object castString(String type,Object object){
    	
    	if(!(object instanceof String))
    		return object;
    	
    	String value = (String)object;
    	Object castedValue = null;
    	switch(type) {
		case SolrMappingReader.STRING:
			castedValue = value;
			break;
		case SolrMappingReader.INT:
			castedValue = Integer.valueOf(value);
			break;
		case SolrMappingReader.LONG:
			castedValue = Long.valueOf(value);
			break;
		case SolrMappingReader.DOUBLE:
			castedValue = Double.valueOf(value);
			break;
		case SolrMappingReader.FLOAT:
			castedValue = Float.valueOf(value);
			break;
		case SolrMappingReader.BOOLEAN:
			castedValue = Boolean.valueOf(value);
			break;
		
		case SolrMappingReader.DATE:
			
			// Create SimpleDateFormat object to parse string
			//String dateFormat = xPathIndexerPropertiesField.getDateFormat() == null?"dd.MM.yyyy":xPathIndexerPropertiesField.getDateFormat();
			
			SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd.MM.yyyy");
			
			// If not parseable set the date: 1. January 1970.
			try {
				castedValue = simpleDateFormat.parseObject(value);
				castedValue = DateUtil.getThreadLocalDateFormat().format(castedValue);
				
			} catch (ParseException e) {
				castedValue = new Date(0);
			} 
			
			break;
			//transfer outlinkMetas
		
		default:
			LOG.warn(String.format("Type '%s' not supported, value will be interpreted as String", type));
			castedValue = value;
			break;
	} 
    	
    	return castedValue;
    	
    }

    public void close() throws IOException {
        try {
            if (!inputDocs.isEmpty()) {
                LOG.info("Indexing " + Integer.toString(inputDocs.size())
                        + " documents");
                if(LOG.isDebugEnabled()){
                	String key = solrMapping.mapKey("url");
                	Set<String> set = new HashSet<String>();
                	for (SolrInputDocument doc : inputDocs) {
                		String url= doc.getField(key).getValue().toString();
                		String id = doc.getField("id").getValue().toString();
                		String sn = doc.getField("societyname_t")!=null?doc.getField("societyname_t").getValue().toString():"No society";
						LOG.debug(sn+ " "+url +" "+ id);
						
						if(!set.add(doc.getField("id").getValue().toString())){
							LOG.debug("allready present");
						};
					}
                	LOG.debug("found "+set.size()+" distinct docs");
                	
                }
                if (numDeletes > 0) {
                	
                    LOG.info("Deleting " + Integer.toString(numDeletes)
                            + " documents");
                }
                UpdateRequest req = new UpdateRequest();
                req.add(inputDocs);
                req.setParams(params);
                //LOG.info(req.toString());
                req.process(solr);
                inputDocs.clear();
            }
        } catch (final SolrServerException e) {
        //	e.printStackTrace();
        	LOG.error(e.getMessage());
            throw makeIOException(e);
        }
    }

    @Override
    public void commit() throws IOException {
        try {
            solr.commit();
        } catch (SolrServerException e) {
            throw makeIOException(e);
        }
    }

    public static IOException makeIOException(SolrServerException e) {
        final IOException ioe = new IOException();
        ioe.initCause(e);
        return ioe;
    }

    @Override
    public Configuration getConf() {
        return config;
    }

    @Override
    public void setConf(Configuration conf) {
        config = conf;
        String serverURL = conf.get(SolrConstants.SERVER_URL);
        if (serverURL == null) {
            String message = "Missing SOLR URL. Should be set via -D "
                    + SolrConstants.SERVER_URL;
            message+="\n"+describe();
            LOG.error(message);
            throw new RuntimeException(message);
        }
    }

    public String describe(){
    	StringBuffer sb = new StringBuffer("SOLRIndexWriter\n");
    	sb.append("\t").append(SolrConstants.SERVER_URL).append(" : URL of the SOLR instance (mandatory)\n");
    	sb.append("\t").append(SolrConstants.COMMIT_SIZE).append(" : buffer size when sending to SOLR (default 1000)\n");
    	sb.append("\t").append(SolrConstants.MAPPING_FILE).append(" : name of the mapping file for fields (default solrindex-mapping.xml)\n");
    	sb.append("\t").append(SolrConstants.USE_AUTH).append(" : use authentication (default false)\n");
    	sb.append("\t").append(SolrConstants.USERNAME).append(" : use authentication (default false)\n");
    	sb.append("\t").append(SolrConstants.USE_AUTH).append(" : username for authentication\n");
    	sb.append("\t").append(SolrConstants.PASSWORD).append(" : password for authentication\n");
    	return sb.toString();
    }
    
}

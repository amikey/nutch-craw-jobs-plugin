package com.atlantbh.nutch.filter.xpath;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.MapWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.Writable;
import org.apache.log4j.Logger;
import org.apache.nutch.crawl.CrawlDatum;
import org.apache.nutch.crawl.Inlinks;
import org.apache.nutch.indexer.IndexingException;
import org.apache.nutch.indexer.IndexingFilter;
import org.apache.nutch.indexer.NutchDocument;
import org.apache.nutch.metadata.Metadata;
import org.apache.nutch.parse.Parse;

import com.atlantbh.nutch.filter.xpath.config.FieldType;
import com.atlantbh.nutch.filter.xpath.config.XPathFilterConfiguration;
import com.atlantbh.nutch.filter.xpath.config.XPathIndexerProperties;
import com.atlantbh.nutch.filter.xpath.config.XPathIndexerPropertiesField;

/**
 * Second stage of {@link XPathHtmlParserFilter} the IndexingFilter.
 * It takes the prepared data located in the metadata and indexes
 * it to solr.
 * 
 * 
 * @author Emir Dizdarevic
 * @version 1.4
 * @since Apache Nutch 1.4
 *
 */
public class XPathIndexingFilter implements IndexingFilter {

	// Constants
	private static final Logger log = Logger.getLogger(XPathIndexingFilter.class);
	
	// Configuration
	private Configuration configuration;
	private XPathFilterConfiguration xpathFilterConfiguration;
	
	public XPathIndexingFilter() {}
	
	private void initConfig() {
		
		// Initialize configuration
		xpathFilterConfiguration  = XPathFilterConfiguration.getInstance(configuration);
	}
	
	@Override
	public Configuration getConf() {
		return configuration;
	}

	@Override
	public void setConf(Configuration configuration) {
		this.configuration = configuration;
		initConfig();
	}

	@Override
	public NutchDocument filter(NutchDocument doc, Parse parse, Text url, CrawlDatum datum, Inlinks inlinks) throws IndexingException {
		Metadata metadata = parse.getData().getParseMeta();
		String parseParentUrl = metadata.get("InLink");
		
		String filteredUrl = (parseParentUrl!=null) ?parseParentUrl:new String(url.getBytes()).substring(0, url.getLength());
	
		List<XPathIndexerProperties> xPathIndexerPropertiesList = xpathFilterConfiguration.getXPathIndexerPropertiesList();
		for(XPathIndexerProperties xPathIndexerProperties : xPathIndexerPropertiesList) {
				
			
			if(FilterUtils.isMatch(xPathIndexerProperties.getPageUrlFilterRegex(),filteredUrl)) {
				
				List<XPathIndexerPropertiesField> xPathIndexerPropertiesFieldList = xPathIndexerProperties.getXPathIndexerPropertiesFieldList();
				
				transferMeta(datum,metadata, xPathIndexerPropertiesFieldList, doc);
				
				
			}
		}
		
		return doc;
	}
	
	private void transferMeta(CrawlDatum datum, Metadata metadata,List<XPathIndexerPropertiesField> xPathIndexerPropertiesFieldList, NutchDocument doc){
		
		if(xPathIndexerPropertiesFieldList==null)
			return;
		
		for(XPathIndexerPropertiesField xPathIndexerPropertiesField : xPathIndexerPropertiesFieldList) {
			
			FieldType type = xPathIndexerPropertiesField.getType();
			
			switch (type) {
			case SUBPARSERESULT:
			case OUTLINK:
				List<XPathIndexerPropertiesField> parsemetas = xPathIndexerPropertiesField.getParseMetas();
				transferMeta(datum, metadata, parsemetas, doc);
				
				break;

			default:
				
				//meta values always come first and are prioritary
				for(String stringValue : metadata.getValues(xPathIndexerPropertiesField.getName())) {
					
					transferMeta(type, xPathIndexerPropertiesField, stringValue, doc);
				}
				
				
			
				break;
				
			}
		}
	}
	
	private void transferMeta(FieldType type,XPathIndexerPropertiesField xPathIndexerPropertiesField,String stringValue,NutchDocument doc){
		Object value = null;
		switch(type) {
			case STRING:
				value = stringValue;
				break;
			case INTEGER:
				value = Integer.valueOf(stringValue);
				break;
			case LONG:
				value = Long.valueOf(stringValue);
				break;
			case DOUBLE:
				value = Double.valueOf(stringValue);
				break;
			case FLOAT:
				value = Float.valueOf(stringValue);
				break;
			case BOOLEAN:
				value = Boolean.valueOf(stringValue);
				break;
				
			/*case SUBPARSERESULT:
				List<XPathIndexerPropertiesField> subParseFields = xPathIndexerPropertiesField.getParseMetas();
				transferMeta(datum, metadata, subParseFields, doc);
				break;*/
			case DATE:
				
				// Create SimpleDateFormat object to parse string
				String dateFormat = xPathIndexerPropertiesField.getDateFormat() == null?"dd.MM.yyyy":xPathIndexerPropertiesField.getDateFormat();
				
				SimpleDateFormat simpleDateFormat = new SimpleDateFormat(dateFormat);
				
				// If not parseable set the date: 1. January 1970.
				try {
					value = simpleDateFormat.parseObject(stringValue);
					
				} catch (ParseException e) {
					value = new Date(0);
				} 
				
				break;
				//transfer outlinkMetas
			
			default:
				log.warn(String.format("Type '%s' not supported, value will be interpreted as String", type));
				value = stringValue;
				break;
		} 
		// Add field
		if(doc.getField(xPathIndexerPropertiesField.getName())==null)
			doc.add(xPathIndexerPropertiesField.getName(), value);
		if (log.isDebugEnabled()) {
			log.debug(String.format(getClass().getName()+" added field with name %s and value %s", xPathIndexerPropertiesField.getName(), value.toString().substring(0,Math.min(value.toString().length(), 25))));
		}
		
	
	}
	
	
}

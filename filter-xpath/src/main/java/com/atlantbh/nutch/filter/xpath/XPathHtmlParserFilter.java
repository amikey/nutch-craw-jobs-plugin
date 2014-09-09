package com.atlantbh.nutch.filter.xpath;



import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.regex.PatternSyntaxException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.MapWritable;
import org.apache.hadoop.io.Text;
import org.apache.log4j.Logger;
import org.apache.nutch.crawl.Injector;
import org.apache.nutch.metadata.Metadata;
import org.apache.nutch.metadata.Nutch;
import org.apache.nutch.parse.HTMLMetaTags;
import org.apache.nutch.parse.HtmlParseFilter;
import org.apache.nutch.parse.Outlink;
import org.apache.nutch.parse.Parse;
import org.apache.nutch.parse.ParseData;
import org.apache.nutch.parse.ParseImpl;
import org.apache.nutch.parse.ParseResult;
import org.apache.nutch.parse.ParseStatus;
import org.apache.nutch.parse.ParseText;

import org.apache.nutch.protocol.Content;
import org.htmlcleaner.CleanerProperties;
import org.htmlcleaner.DomSerializer;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.SimpleHtmlSerializer;
import org.htmlcleaner.TagNode;
import org.jaxen.JaxenException;
import org.jaxen.XPath;
import org.jaxen.dom.DOMXPath;
import org.mortbay.log.Log;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.atlantbh.nutch.filter.xpath.config.FieldType;
import com.atlantbh.nutch.filter.xpath.config.OutlinkFilter;
import com.atlantbh.nutch.filter.xpath.config.Rule;
import com.atlantbh.nutch.filter.xpath.config.XPathFilterConfiguration;
import com.atlantbh.nutch.filter.xpath.config.XPathIndexerProperties;
import com.atlantbh.nutch.filter.xpath.config.XPathIndexerPropertiesField;


/**
 * A Xml-Html xpath filter implementation that fetches data
 * from the content, depending on the supplied xpath,
 * and prepares it for the {@link XPathIndexingFilter} to
 * index it into solr.
 * 
 * @author Emir Dizdarevic
 * @version 1.4
 * @since Apache Nutch 1.4
 */
public class XPathHtmlParserFilter implements HtmlParseFilter {
	
	// Constants
	private static final Logger LOG= Logger.getLogger(XPathHtmlParserFilter.class);
	private static final List<String> htmlMimeTypes = Arrays.asList(new String[] {"text/html", "application/xhtml+xml"});
	
	// OLD WAY TO DETERMIN IF IT'S AN XML FORMAT
	//private static final List<String> xmlMimeTypes = Arrays.asList(new String[] {"text/xml", "application/xml"});
	
	// Configuration
	private Configuration configuration;
	private XPathFilterConfiguration xpathFilterConfiguration;
	private String defaultEncoding;
	
	// Internal data
	private HtmlCleaner cleaner;
	private DomSerializer domSerializer;
	private DocumentBuilder documentBuilder;
	private SimpleHtmlSerializer htmlSerializer;
	private  DOMContentUtils utils;
	private boolean exportSamplePage;
	private String samplePageExportPath;
	private SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy");
	 public static String LAST_FETCH_TIME = "LAST_FETCH_TIME";
	private long jobItemFetchInterval;
	
	public XPathHtmlParserFilter() {
		init();
	}

	private void init() {
		
		// Initialize HTMLCleaner
		cleaner = new HtmlCleaner();
		CleanerProperties props = cleaner.getProperties();
		props.setAllowHtmlInsideAttributes(true);
		props.setAllowMultiWordAttributes(true);
		props.setRecognizeUnicodeChars(true);
		props.setOmitComments(true);
		props.setNamespacesAware(false);
		
		// Initialize DomSerializer
		domSerializer = new DomSerializer(props);
		
		// Initialize xml parser		
		try {
			DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
			documentBuilder = documentBuilderFactory.newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			// THIS CAN NEVER HAPPEN
		}
	}
	
	private void initConfig() {

		// Initialize configuration
		xpathFilterConfiguration  = XPathFilterConfiguration.getInstance(configuration);
		defaultEncoding = configuration.get("parser.character.encoding.default", "UTF-8");
		this.utils = new DOMContentUtils(configuration);
		
		exportSamplePage = configuration.getBoolean("parser.export.page", false);
		if(exportSamplePage){
			htmlSerializer = new SimpleHtmlSerializer(cleaner.getProperties());
			samplePageExportPath = configuration.get("sample.export.path", "./selenium-conf/page.html");
		}
		
		jobItemFetchInterval = configuration.getLong("jobitem.fetch.interval", 7776000);
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
	
	
	public ParseResult filter(Content content, ParseResult parseResult, HTMLMetaTags metaTags, DocumentFragment doc) {
		
		Metadata metadata = parseResult.get(content.getUrl()).getData().getParseMeta();
		
		
		byte[] rawContent = content.getContent();
		
		try {
			Document cleanedXmlHtml = documentBuilder.newDocument();
			if(htmlMimeTypes.contains(content.getContentType())) {
				
				// Create reader so the input can be read in UTF-8
				Reader rawContentReader = new InputStreamReader(new ByteArrayInputStream(rawContent), FilterUtils.getNullSafe(metadata.get(Metadata.ORIGINAL_CHAR_ENCODING), defaultEncoding));
				
				// Use the cleaner to "clean" the HTML and return it as a TagNode object
				TagNode tagNode = cleaner.clean(rawContentReader);
				if(exportSamplePage)
					htmlSerializer.writeToFile(tagNode, samplePageExportPath,FilterUtils.getNullSafe(metadata.get(Metadata.ORIGINAL_CHAR_ENCODING), defaultEncoding));
				
				
				cleanedXmlHtml = domSerializer.createDOM(tagNode);
				
			} else if(content.getContentType().contains(new StringBuilder("/xml")) || content.getContentType().contains(new StringBuilder("+xml"))) {
				
				// Parse as xml - don't clean
				cleanedXmlHtml = documentBuilder.parse(new InputSource(new ByteArrayInputStream(rawContent)));	
			} 
			//We suppress all outlinks (this should go in an OutlinkFilterplugin).
			//By default outlink will eventually be enriched by the filter if an outlink filter is found for the url
			
			//the outlinks coming from hrmlparser
			Outlink[] outlinks = parseResult.get(content.getUrl()).getData().getOutlinks();
			
			//the one that will be filetered
			List<Outlink> filteredLinks = new ArrayList<Outlink>();
			filteredLinks.addAll(Arrays.asList(outlinks));
			//the one that might be generated/captured by this parser
			List<Outlink> capturedOutlinks = new ArrayList<Outlink>();
			
			// Once the HTML is cleaned, then you can run your XPATH expressions on the node, 
			// which will then return an array of TagNode objects 
			List<XPathIndexerProperties> xPathIndexerPropertiesList = xpathFilterConfiguration.getXPathIndexerPropertiesList();
			
			for(XPathIndexerProperties xPathIndexerProperties : xPathIndexerPropertiesList) {
				
				
				//****************************
				// CORE XPATH EVALUATION
				//****************************
				/*boolean pageMatches = false;
				if(content.getMetadata().get("Location")!=null)
					pageMatches = pageToProcess(xPathIndexerProperties, cleanedXmlHtml, content.getMetadata().get("Location"));
				if(!pageMatches)*/
					boolean pageMatches = pageToProcess(xPathIndexerProperties, cleanedXmlHtml, content.getBaseUrl());
				/*
				*/
				if(pageMatches) {
					if(LOG.isDebugEnabled())
						LOG.debug(content.getBaseUrl());
						
					List<XPathIndexerPropertiesField> xPathIndexerPropertiesFieldList = xPathIndexerProperties.getXPathIndexerPropertiesFieldList();
					
					for(XPathIndexerPropertiesField xPathIndexerPropertiesField : xPathIndexerPropertiesFieldList) {
						
						//check content meta for the presence of the field allready extracted by selenium
						if(content.getMetadata().get(xPathIndexerPropertiesField.getName())!=null){
							String value = content.getMetadata().get(xPathIndexerPropertiesField.getName()).trim();
							//selenium has set a value for this field
							if(xPathIndexerPropertiesField.getRegexpRule()!=null){
								value = xPathIndexerPropertiesField.getRegexpRule().execute(value);
							}
							if(value!=null){
								metadata.add(xPathIndexerPropertiesField.getName(), value);	
								if(LOG.isDebugEnabled())
									LOG.debug("Passed "+xPathIndexerPropertiesField.getName()+" with value"+value+" from contentMeta to parse meta");
							}
							
							//no need to do it anymore
							continue;
						}
						
						if(xPathIndexerPropertiesField.getXPath().length() == 0)
							continue;
						
						// Evaluate xpath			
						XPath xPath = new DOMXPath(xPathIndexerPropertiesField.getXPath());
						
						List nodeList = xPath.selectNodes(cleanedXmlHtml);
						
						if(LOG.isDebugEnabled())
							LOG.debug("evaluating "+xPathIndexerPropertiesField.toString());
						// Trim?
						boolean trim = FilterUtils.getNullSafe(xPathIndexerPropertiesField.getTrimXPathData(), true);
						
						FieldType type = xPathIndexerPropertiesField.getType();
						
						if(FilterUtils.getNullSafe(xPathIndexerPropertiesField.isConcat(), false)) {
								
							// Iterate trough all found nodes
							String value = new String();
							String concatDelimiter = FilterUtils.getNullSafe(xPathIndexerPropertiesField.getConcatDelimiter(), "");
							for (Object node : nodeList) {
								
								// Extract data	
								String tempValue = FilterUtils.extractTextContentFromRawNode(node,false);
								tempValue = filterValue(tempValue, trim);
								
								// Concatenate tempValue to value
								if(tempValue != null) {
									if(value.isEmpty()) {
										value = tempValue;
									} else {
										value = value + concatDelimiter + tempValue;
									}
								}
							}
							
							// Add the extracted data to meta
							if(value != null) {
								metadata.add(xPathIndexerPropertiesField.getName(), value);
							}
							
						} else {
							switch (type) {
							case SUBPARSERESULT:
								getSubParseResults(utils,content,parseResult,nodeList,xPathIndexerPropertiesField);
								break;
							case OUTLINK:
								getOutlink(utils,content,parseResult,nodeList,xPathIndexerPropertiesField,capturedOutlinks);
								break;
							default:
								// Iterate trough all found nodes
								for (Object node : nodeList) {

									// Add the extracted data to meta
									Boolean childrentext = xPathIndexerPropertiesField.getWithChildrenText();
									String value = FilterUtils.extractTextContentFromRawNode(node,childrentext);					
									value = filterValue(value, trim);
									if(value != null) {
										if(xPathIndexerPropertiesField.getRegexpRule()!=null){
											value = xPathIndexerPropertiesField.getRegexpRule().execute(value);
										}
										if(value!=null){
											metadata.add(xPathIndexerPropertiesField.getName(), value);	
											if(LOG.isDebugEnabled())
												LOG.debug("result "+xPathIndexerPropertiesField.getName()+"->"+value.toString().trim().substring(0,Math.min(value.toString().trim().length(), 25)));
										}
									}
								}
								break;
							}
							
						}
						
					}
					
					//here we pass the offerdate if present : necessary for nextPage analysis
					if(content.getMetadata().get("offerdate")!=null && metadata.get("offerdate")==null){
						metadata.add("offerdate", content.getMetadata().get("offerdate"));
					}
					/*if(!FilterUtils.getNullSafe(xPathIndexerProperties.getRefetchOutlinkRegexp(), "noregexp").equals("noregexp")){
						String regexp = xPathIndexerProperties.getRefetchOutlinkRegexp();
						for (int i = 0; i < outlinks.length; i++) {
							Outlink link = outlinks[i];
							if(FilterUtils.isMatch(regexp, link.getToUrl())){
								MapWritable linkMeta = new MapWritable();
								link.setMetadata(linkMeta);
								linkMeta.put(new Text("refetch"), new Text("true"));
								linkMeta.put(new Text("nextPage"), new Text("true"));
								if(cookies!=null){
									linkMeta.put(new Text("cookies"), new Text(cookies));
								}
							}
							
						}
					}*/
					
					//filter outlinks
					if(xPathIndexerProperties.getOutlinkFilterList()!=null && xPathIndexerProperties.getOutlinkFilterList().size() > 0){
						
						List<OutlinkFilter> outlinkfiltersList = xPathIndexerProperties.getOutlinkFilterList();
						List<Outlink> tempArray = new ArrayList<Outlink>();
						
						tempArray.addAll(filteredLinks);
						filteredLinks.clear();
						
						
						for (Outlink outlink : tempArray) {
							/*if(LOG.isDebugEnabled()){
								LOG.debug("filtering outlink : "+outlink.toString());
							}*/
							boolean filtered = false;
							
							for( OutlinkFilter outlinkFilter: outlinkfiltersList){
										
									List<Rule> rulesList = outlinkFilter.getRegexpRuleList();
									
									for (Rule rule : rulesList) {
									
										String capturedText = rule.execute(outlink);
										if(capturedText!=null){
											filtered = true;
											if(rule.getIsAnchorRule()){
												LOG.debug("filtering outlink text link: "+capturedText);
											}else{
												outlink.setUrl(capturedText);
											}
											break;
										}
										
									}
									if(filtered)
										break;
									
							}	
							
							if(filtered){
								
								filteredLinks.add(outlink);
								if(LOG.isDebugEnabled()){
									LOG.debug("adding filtered outlink : "+outlink.toString());
								}
							}
							
						}
						
						
					}
					
					filteredLinks.addAll(capturedOutlinks);
					//parseResult.get(content.getUrl()).getData().setOutlinks(links.toArray(new Outlink[links.size()]));
					
					
					
					
				}//end pageToProcess
				
				
			}
			
			parseResult.get(content.getUrl()).getData().setOutlinks(filteredLinks.toArray(new Outlink[filteredLinks.size()]));
			
			
			//filter on offerDate
			
				long lastFetchTime = Long.valueOf(content.getMetadata().get(LAST_FETCH_TIME)).longValue();
				lastFetchTime = ((long)Math.ceil(lastFetchTime/1000))*1000;
				if(metadata.get("offerdate")!=null){
					String offerdate = metadata.get("offerdate");
					
					if(LOG.isDebugEnabled()){
						String lasFetchDate = new Date(lastFetchTime).toString();
						LOG.debug("Last fetch date : "+lasFetchDate+" parse date : "+offerdate);
					}
					long offertime = ((Date)dateFormat.parseObject(offerdate)).getTime();
					if(offertime < lastFetchTime){
						parseResult = new ParseStatus(ParseStatus.FAILED, "too ancient offerDate: " + offerdate.toString()).getEmptyParseResult(content.getUrl(), configuration);;
					}else{
						
						
						metadata.add("itemType", "jobItem");
						String fetchInterval = new Long(jobItemFetchInterval).toString();
						metadata.add( Injector.nutchFetchIntervalMDName, fetchInterval);
						if(LOG.isDebugEnabled()){
							LOG.debug("Got new one");
							LOG.debug("Adding metadata : "+Injector.nutchFetchIntervalMDName+" : "+fetchInterval);
							LOG.debug("Adding metadata : itemType -> jobItem");
						}
					}
				}else{
					LOG.debug("No offer date");
				}
			
		} catch(ParseException e) {
			System.err.println(e.getMessage());
			LOG.error("Error parsing offerDate: " + e.getMessage());
			return new ParseStatus(ParseStatus.FAILED, "Error parsing offerDate: " + e.getMessage()).getEmptyParseResult(content.getUrl(), configuration);
		
		} catch (IOException e) {
			// This can never happen because it's an in memory stream
		} catch(PatternSyntaxException e) {
			System.err.println(e.getMessage());
			LOG.error("Error parsing urlRegex: " + e.getMessage());
			return new ParseStatus(ParseStatus.FAILED, "Error parsing urlRegex: " + e.getMessage()).getEmptyParseResult(content.getUrl(), configuration);
		} catch (ParserConfigurationException e) {
			System.err.println(e.getMessage());
			LOG.error("HTML Cleaning error: " + e.getMessage());
			return new ParseStatus(ParseStatus.FAILED, "HTML Cleaning error: " + e.getMessage()).getEmptyParseResult(content.getUrl(), configuration);
		} catch (SAXException e) {
			System.err.println(e.getMessage());
			LOG.error("XML parsing error: " + e.getMessage());
			return new ParseStatus(ParseStatus.FAILED, "XML parsing error: " + e.getMessage()).getEmptyParseResult(content.getUrl(), configuration);
		} catch (JaxenException e) {
			System.err.println(e.getMessage());
			LOG.error("XPath error: " + e.getMessage());
			return new ParseStatus(ParseStatus.FAILED, "XPath error: " + e.getMessage()).getEmptyParseResult(content.getUrl(), configuration);
		}
		
		return parseResult;
	}
	
public  void getSubParseResults(DOMContentUtils utils,Content parentContent,ParseResult parentParseResult,List nodeList,XPathIndexerPropertiesField subParsesField) throws MalformedURLException,JaxenException, ParserConfigurationException{
		
		String baseURL = parentContent.getBaseUrl();
		
		
		
		ArrayList<Outlink> subLinks = new ArrayList<Outlink>();
		
		Rule parseResultUrlRule = subParsesField.getRegexpRule();
		
		for (int i = 0;i<nodeList.size();i++) {
			
			Node subNode = (Node)nodeList.get(i);
			String parseResultURL  = null;
			subLinks.clear();
			try {
				utils.getOutlinks(new URL(baseURL), subLinks, (Node)subNode);
			} catch (MalformedURLException e) {
				LOG.error("Malformed base URL",e);
				throw e;
			}
			
			
			for (Outlink outlink : subLinks) {
				
				//we  examine  outlinks to capture the url of the subparse
				String capturedText = parseResultUrlRule.execute(outlink);
				if(capturedText!=null){
					
					
					if(parseResultUrlRule.isParseResultUrl()){
						parseResultURL=capturedText;
					}

					if(!parseResultUrlRule.getIsAnchorRule()){
						LOG.debug("filtering outlink text link: "+capturedText);
						outlink.setUrl(capturedText);
					}
					
					//subLinks.add(outlink);
					
					break;
				}
			}
			subLinks.clear();
			Outlink[]outlinks = subLinks.toArray(new Outlink[subLinks.size()]);
			//we have the parseResultURL URL now let's get the metas
			
			if(parseResultURL!=null){
				
				
				Metadata parentContentMeta = parentContent.getMetadata();
				Metadata contentMeta = new Metadata();
				contentMeta.add("Content-Type", parentContentMeta.get("Content-Type"));
				contentMeta.add(Nutch.FETCH_TIME_KEY, parentContentMeta.get(Nutch.FETCH_TIME_KEY));
				Metadata parseMetaData = new Metadata();
				parseMetaData.add("Cookie",parentContentMeta.get("Cookie"));
				parseMetaData.add("CookieDomain",parentContentMeta.get("CookieDomain"));
				parseMetaData.add("CookiePath",parentContentMeta.get("CookiePath"));
				parseMetaData.add("CookieExpiry",parentContentMeta.get("CookieExpiry"));
				parseMetaData.add("CookieSecure",parentContentMeta.get("CookieSecure"));
				
				
				//we must add a parent url meta , so that the XPathIndexingFilter plugin can use the correct scheme to cast the metas and add them to Nutch document
				//parseMetaData.add(PARSE_PARENT_URL, baseURL);
				  
				List<XPathIndexerPropertiesField> parseMetas = subParsesField.getParseMetas();
				String subParseResultTitle = "";
				StringBuffer sb = new StringBuffer();
				
				for (XPathIndexerPropertiesField parseMeta : parseMetas) {
					try {
						/*if(parseMeta.getName().equals("offerdate"))
						LOG.debug(parseMeta.getName()+"-> "+ parseMeta.getXPath());
					*/	
						
						XPath xPath = new DOMXPath(parseMeta.getXPath());
						Node currNode = (Node)xPath.selectSingleNode(subNode);
						if(currNode!=null){
							//Node currentNode = (Node)subNodes.get(0);
							// Add the extracted data to meta
							Boolean childrentext = parseMeta.getTrimXPathData();
							String value = FilterUtils.extractTextContentFromRawNode(currNode,childrentext);					
							value = filterValue(value, parseMeta.getTrimXPathData());
							if(value!=null){
								
								if(parseMeta.getRegexpRule()!=null){
									value = parseMeta.getRegexpRule().execute(value);
								}
								if(value!=null){
									sb.append(value);
									
									parseMetaData.add(parseMeta.getName(), value);	
									if(LOG.isDebugEnabled())
										LOG.debug(parseMeta.getName()+"-> "+ value);
									sb.append(value);
								}
							}
							if(value==null){
								LOG.warn("failed to parse meta for xpath "+parseMeta.getName()+" : "+parseMeta.getXPath());
							}
							
							/*if(parseMeta.isTitleField())
								subParseResultTitle = value;*/
							
						}
					} catch (JaxenException e) {
						LOG.error("Failed to generate XPATH for outlink meta extraction", e);
						throw e;
					}
				}
				
				//if present, cookies are passed to metadata
			    /*  if(content.getMetadata().get("Cookie")!=null){
			        	String cookies = content.getMetadata().get("Cookie");
			        	String cookiesDomain = content.getMetadata().get("CookieDomain");
			        	String cookiesPath = content.getMetadata().get("CookiePath");
			        	String cookiesExpiry = content.getMetadata().get("CookieExpiry");
			        	String cookiesSecure = content.getMetadata().get("CookieSecure");
			      	for (int i = 0; i < outlinks.length; i++) {
			  			Outlink outlink = outlinks[i];
			  			outlink.setMetadata(new MapWritable());
			  			outlink.getMetadata().put(new Text("Cookie"), new Text(cookies));
			  			//if (LOG.isTraceEnabled())
			  		       // LOG.trace("cookies "+cookies);
			  			if(cookiesDomain!=null){
			  				outlink.getMetadata().put(new Text("CookieDomain"), new Text(cookiesDomain));
			  				//if (LOG.isTraceEnabled())
			  	  		        //LOG.trace("cookieDomains "+cookiesDomain);
			  			}
			  			if(cookiesPath!=null){
			  				outlink.getMetadata().put(new Text("CookiePath"), new Text(cookiesPath));
			  				//if (LOG.isTraceEnabled())
			  	  		       // LOG.trace("cookiePaths "+cookiesPath);
			  			}
			  			if(cookiesExpiry!=null){
			  				outlink.getMetadata().put(new Text("CookieExpiry"), new Text(cookiesExpiry));
			  				//if (LOG.isTraceEnabled())
			  	  		       // LOG.trace("cookiesExpiry "+cookiesExpiry);
			  			}
			  			if(cookiesSecure!=null){
			  				outlink.getMetadata().put(new Text("CookieSecure"), new Text(cookiesSecure));
			  				//if (LOG.isTraceEnabled())
			  	  		        //LOG.trace("cookiesSecure "+cookiesSecure);
			  			}
			  		}
			      }
			    }*/
				 
				String text = sb.toString();
				/*byte[] content = text.getBytes();
			      Content c = new Content(parseResultURL, baseURL,
			                              (content == null ? new byte[0] : content),
			                              parentContent.getContentType(),
			                              parentContent.getMetadata(), this.configuration);*/
				/*String[] args = new String[2];
				args[0] = parseResultURL;
				args[1] = "0";*/
				
				parseMetaData.add("fetchInterval", "0");
				ParseStatus status = new ParseStatus(ParseStatus.SUCCESS);
			  
				ParseData parseData = new ParseData(status,subParseResultTitle!=null?subParseResultTitle:"" , outlinks,
			            contentMeta, parseMetaData);
				Parse parse = new ParseImpl(text, parseData);
				if(LOG.isDebugEnabled())
					LOG.debug("adding subparse for "+parseResultURL);
				parentParseResult.put(new Text(parseResultURL), new ParseText(parse.getText()), parse.getData());
			  
			}
			
		}
		
	}

public  void getOutlink(DOMContentUtils utils,Content parentContent,ParseResult parentParseResult,List nodeList,XPathIndexerPropertiesField subParsesField,List<Outlink> links) throws MalformedURLException,JaxenException, ParserConfigurationException{
	
	String baseURL = parentContent.getBaseUrl();
	
	ArrayList<Outlink> subLinks = new ArrayList<Outlink>();
	
	Rule parseResultUrlRule = subParsesField.getRegexpRule();
	
	for (int i = 0;i<nodeList.size();i++) {
		
		Node subNode = (Node)nodeList.get(i);
		Outlink outlink  = null;
		subLinks.clear();
		
		try {
			
			utils.getOutlinks(new URL(baseURL), subLinks, (Node)subNode);
			
		} catch (MalformedURLException e) {
			
			LOG.error("Malformed base URL",e);
			
			throw e;
		}
		
		for (Outlink subLink : subLinks) {
			
			//we  examine  outlinks to capture the url of the subparse
			String capturedText = parseResultUrlRule.execute(subLink);
			
			if(capturedText!=null){
				
				if(parseResultUrlRule.isParseResultUrl()){
				
					String parseResultURL=capturedText;
					
					if(!parseResultUrlRule.getIsAnchorRule()){
						
						LOG.debug("capturing outlink text link: "+capturedText);
						subLink.setUrl(capturedText);
					}
					
					outlink = subLink;
				}
				
				
				
				break;
			}
		}
		
		
		//we have the parseResultURL URL now let's get the metas
		
		if(outlink!=null){
			
			links.add(outlink);
			
			Metadata parentContentMeta = parentContent.getMetadata();
			
			MapWritable outlinkMeta = new MapWritable();
			outlink.setMetadata(outlinkMeta);
			if(subParsesField.getInjectMeta()!=null && subParsesField.getInjectMetaValue()!=null){
				outlinkMeta.put(new Text(subParsesField.getInjectMeta()), new Text(subParsesField.getInjectMetaValue()));
			}
			  
			List<XPathIndexerPropertiesField> parseMetas = subParsesField.getParseMetas();
			if(parseMetas==null)
				continue;
			
			
			StringBuffer sb = new StringBuffer();
			
			for (XPathIndexerPropertiesField parseMeta : parseMetas) {
				try {
					/*if(parseMeta.getName().equals("offerdate"))
					LOG.debug(parseMeta.getName()+"-> "+ parseMeta.getXPath());
				*/	
					
					XPath xPath = new DOMXPath(parseMeta.getXPath());
					Node currNode = (Node)xPath.selectSingleNode(subNode);
					if(currNode!=null){
						//Node currentNode = (Node)subNodes.get(0);
						// Add the extracted data to meta
						Boolean childrentext = true;//parseMeta.getWithChildrenText();
						String value = FilterUtils.extractTextContentFromRawNode(currNode,childrentext);					
						value = filterValue(value, true);
						if(value!=null){
							
							if(parseMeta.getRegexpRule()!=null){
								value = parseMeta.getRegexpRule().execute(value);
							}
							if(value!=null){
								sb.append(value);
								
								outlinkMeta.put(new Text(parseMeta.getName()), new Text(value));	
								if(LOG.isDebugEnabled())
									LOG.debug(parseMeta.getName()+"-> "+ value.toString().trim().substring(0,Math.min(value.toString().length(), 25)));
								sb.append(value);
							}
						}
						if(value==null && LOG.isDebugEnabled()){
							LOG.warn("parse meta for xpath "+parseMeta.getName()+" : "+parseMeta.getXPath()+ " has null value");
						}
						
						/*if(parseMeta.isTitleField())
							subParseResultTitle = value;*/
						
					}
				} catch (JaxenException e) {
					LOG.error("Failed to generate XPATH for outlink meta extraction", e);
					throw e;
				}
			}
			
		
			
			if (LOG.isDebugEnabled())
				LOG.debug(outlink.toString());
			
			
		  
		}
		
	}
	
}
	
	
	@SuppressWarnings("rawtypes")
	private boolean pageToProcess(XPathIndexerProperties xPathIndexerProperties, Document cleanedXmlHtml, String url) throws JaxenException {

		boolean processPage = true;
		
		// *************************************
		// URL REGEX CONTENT PAGE FILTERING
		// *************************************
		processPage = processPage && FilterUtils.isMatch(xPathIndexerProperties.getPageUrlFilterRegex(), url);

		// Check return status
		if (!processPage) {
			return false;
		}

		// *************************************
		// XPATH CONTENT PAGE FILTERING
		// *************************************

		if (xPathIndexerProperties.getPageContentFilterXPath() != null) {
			XPath xPathPageContentFilter = new DOMXPath(xPathIndexerProperties.getPageContentFilterXPath());
			List pageContentFilterNodeList = xPathPageContentFilter.selectNodes(cleanedXmlHtml);
			boolean trim = FilterUtils.getNullSafe(xPathIndexerProperties.isTrimPageContentFilterXPathData(), true);
			
			if (FilterUtils.getNullSafe(xPathIndexerProperties.isConcatPageContentFilterXPathData(), false)) {

				// Iterate trough all found nodes
				String value = new String();
				String concatDelimiter = FilterUtils.getNullSafe(xPathIndexerProperties.getConcatPageContentFilterXPathDataDelimiter(), "");

				for (Object node : pageContentFilterNodeList) {

					// Extract data
					String tempValue = FilterUtils.extractTextContentFromRawNode(node,false);
					tempValue = filterValue(tempValue, trim);

					// Concatenate tempValue to value
					if(tempValue != null) {
						if (value.isEmpty()) {
							value = tempValue;
						} else {
							value = value + concatDelimiter + tempValue;
						}
					}
				}

				processPage = processPage && FilterUtils.isMatch(xPathIndexerProperties.getPageContentFilterRegex(), value);
			} else {
				for (Object node : pageContentFilterNodeList) {

					// Add the extracted data to meta
					String value = FilterUtils.extractTextContentFromRawNode(node,false);
					value = filterValue(value, trim);
					if(value != null) {
						processPage = processPage && FilterUtils.isMatch(xPathIndexerProperties.getPageContentFilterRegex(), value);
					}
				}
			}
		}

		return processPage;
	}
	
	private String filterValue(String value, boolean trim) {

		String returnValue = null;
		
		// Filter out empty strings and strings made of space, carriage return and tab characters
		if(!value.isEmpty() && !FilterUtils.isMadeOf(value, " \n\t")) {
			
			// Trim data?
			returnValue = trimValue(value, trim);
		}
		
		return returnValue == null ? null : StringEscapeUtils.unescapeHtml(returnValue);
	}
	
	private String trimValue(String value, boolean trim) {
		
		String returnValue;
		if (trim) {
			returnValue = value.trim();
		} else {
			returnValue = value;
		}
		
		return returnValue;
	}
}

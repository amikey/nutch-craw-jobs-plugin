package com.atlantbh.nutch.filter.xpath.config;


import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.*;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.nutch.parse.Outlink;

import com.ibm.icu.text.SimpleDateFormat;



public class Rule  {
    
	public static final Log LOG = LogFactory.getLog(Rule.class);
	
    private Pattern pattern;
    private boolean accept;
    private String regexp;
    private String type;
    private boolean isAnchorRule;
    private String cachedStringRepresentation;
    private List<Rule> subRules;
    private boolean isParseResultUrl;
    private String inDateFormat;
    private String outDateFormat;
    
    public Rule(){
    	
    	this.regexp =".+";
    	this.accept = true;
    	isAnchorRule = false;
    	isParseResultUrl = false;
    }
    
   
    @XmlElement(name="subrule", nillable=true)
   public List<Rule> getSubRules() {
		return subRules;
	}



	public void setSubRules(List<Rule> subRules) {
		this.subRules = subRules;
	}



public Rule(boolean sign, String regex, String type) {
     
      this.regexp = regex;
      pattern = Pattern.compile(regex);
      this.type=type;
      isAnchorRule = false;
    }
   
   public boolean accept(String url){
	   boolean accept = false;
	   if(accept()&& match(url)){
		   accept=true;
	   }else if(!accept() && !match(url)){
		   accept=true;
	   }
	   
	   return accept;
   }
   
   public boolean accept() { return accept; }

    public boolean match(String url) {
      return pattern.matcher(url).find();
    }
    
    public String execute(Outlink outlink){
    	String text = getIsAnchorRule()?outlink.getAnchor().trim():outlink.getToUrl().trim();
		return execute(text);
    }
    
    public synchronized String execute(String text) {
    	String result = null;
    	try {
	    	Matcher matcher = pattern.matcher(text);
	    	
	    	if(!accept(text))
	    		return result;
	    	
	    	if(matcher.find()){
	    		
	    			StringBuffer contentBuffer = new StringBuffer();
	    			
	    			String captured = null;
	    			if(groupIndex!=null){
	    				Integer index = Integer.valueOf(groupIndex);
	    				 captured = matcher.group(index);
	    			}else{
	    			
	    				captured = matcher.group();
	    			
	    			}
	    				
    				if(captured!=null && captured.length() > 0){
    					
    					if(replaceAll!=null)
    						captured = text.replaceAll(captured, replaceAll);
    					
    					contentBuffer.append(captured);
    					
    				}
	    			//if(groupsIndexes.size() > 0){
	    			
	    				//contentBuffer.append(matcher.group(groupsIndexes.get(0)));
	    				/*if(groupsIndexes.size() > 0 && groupsSeparator.size() > 0){
	    					contentBuffer.append(groupsSeparator.get(0));
	    				}*/
	    				/*for (int i = 1; i < groupsIndexes.size(); i++)
	    				{
	    					String groupStr = matcher.group(groupsIndexes.get(i));
	    					if(groupStr!=null){
	    						if(trimToLowerCase){
	    							groupStr = groupStr.trim().toLowerCase();
	    						}
		    					contentBuffer.append(groupStr);
		    					if(groupsSeparator.size() > i && groupsSeparator.get(i)!=null){
		    						contentBuffer.append(groupsSeparator.get(i));
		    					}
	    					}
	    						
	    				}*/
	    				
	    			
	    			
	    			String tempResult = contentBuffer.toString();
	    			
	    			if(inDateFormat!=null && !tempResult.equals("")){
	    				try
						{
	    					Date date = null;
	    					
	    					if(inDateFormat.equals("d")){
	    						
	    						long timeEllapsed = Long.valueOf(tempResult.trim()).longValue() * 24 *3600 *1000;
	    						long currTime = System.currentTimeMillis();
	    						long  refTime = currTime;//-(currTime%(24 *3600 *1000));
	    						
	    						date = new Date(refTime - timeEllapsed);
	    						
	    					}else if(inDateFormat.equals("H")){
	    						
	    						long timeEllapsed = Long.valueOf(tempResult.trim()).longValue() * 3600 *1000;
	    						long currTime = System.currentTimeMillis();
	    						long  refTime = currTime;//-(currTime%(3600 *1000));
	    						date = new Date(refTime - timeEllapsed);
	    					}else if(inDateFormat.equals("m")){
	    						
	    						long timeEllapsed = Long.valueOf(tempResult.trim()).longValue() * 60 *1000;
	    						long currTime = System.currentTimeMillis();
	    						long  refTime = currTime;//-(currTime%(60 *1000));
	    						date = new Date(refTime - timeEllapsed);
	    					}else{
	    						SimpleDateFormat sd = new SimpleDateFormat(inDateFormat);
	    						date = (Date)sd.parseObject(tempResult);
		    					if(!inDateFormat.contains("y")){
		    						date.setYear(new Date(System.currentTimeMillis()).getYear());
		    						
		    					}
	    					}
	    					
	    					SimpleDateFormat sdo =  (outDateFormat!=null?new SimpleDateFormat(outDateFormat):new SimpleDateFormat("dd.MM.yyyy"));
	    					
	    					tempResult = sdo.format(date);
	    					
	    					
						}
						catch (Exception e)
						{
							LOG.error("Failed to execute Rule ",e);
						}
	    				
	    			}
	    			result = tempResult;	
	    		if(getSubRules()!=null){
	    			for (Rule rule : subRules) {
						tempResult = rule.execute(result);
						if(tempResult!=null)
							result = tempResult;
					}
	    		}
	    			
	    		
	    	}else if(defaultResult!=null){
	    		result=defaultResult;
	    	}
	    	
    	} catch (Exception e) {
			if(LOG.isFatalEnabled()){
				//e.printStackTrace(LogUt.getFatalStream(LOG));
			}
		}
    	return result;
    }
   
    
    private String replace(String base){
    	return base.replaceAll(regexp, replaceAll);
    }
    

    
    

	
	public String toString() {
		
		if(cachedStringRepresentation==null){
			  StringBuffer buffer = new StringBuffer();
			  
			  buffer.append(super.toString());

	          buffer.append(type!=null?type:"No Type");
	        
	          buffer.append('\n');
	          
	         // buffer.append(soft!=null?soft:"No Soft");
		        
	          buffer.append('\n');
	         
	         
	         
	           cachedStringRepresentation = buffer.toString();
			}
			return cachedStringRepresentation;
	}

	/**
	 * @return the type
	 */
	@XmlAttribute(name="type", required=false)
	public String getType()
	{
		return this.type;
	}
	
	public void setType(String type) {
		this.type = type;
	}
	
	
	@XmlAttribute(name="isAnchorRule", required=false)
	public boolean getIsAnchorRule() {
		return isAnchorRule;
	}



	public void setIsAnchorRule(boolean isAnchorRule) {
		this.isAnchorRule = isAnchorRule;
	}


	@XmlAttribute(name="isParseResultUrl", required=false)
	public boolean isParseResultUrl() {
		return isParseResultUrl;
	}


	public void setParseResultUrl(boolean isParseResultUrl) {
		this.isParseResultUrl = isParseResultUrl;
	}

	
	
	


	@XmlAttribute(name="regexp", required=true)
	public String getRegexp() {
		return regexp;
	}

	@XmlAttribute(name="inDateFormat", required=false)
	public String getInDateFormat() {
		return inDateFormat;
	}


	public void setInDateFormat(String inDateFormat) {
		this.inDateFormat = inDateFormat;
	}

	@XmlAttribute(name="outDateFormat", required=false)
	public String getOutDateFormat() {
		return outDateFormat;
	}


	public void setOutDateFormat(String outDateFormat) {
		this.outDateFormat = outDateFormat;
	}


	public void setRegexp(String regexp) {
		this.regexp= regexp;
		pattern = Pattern.compile(regexp);
	}



	//private boolean isLong=false;
	
	//private String dayString;
	//private boolean addDay=false;
	
	/**
	 * @return the groupsIndex
	 */
	@XmlAttribute(name="groupIndex", required=false)
	public String getGroupIndex() {
		return groupIndex;
	}

	/**
	 * @param groupsIndex the groupsIndex to set
	 */
	public void setGroupIndex(String groupIndex) {
		this.groupIndex = groupIndex;
	}



	/**
	 * @return the replaceAll
	 */
	@XmlAttribute(name="replaceAll", required=false)
	public String getReplaceAll() {
		return replaceAll;
	}

	/**
	 * @param replaceAll the replaceAll to set
	 */
	public void setReplaceAll(String replaceAll) {
		this.replaceAll = replaceAll;
	}
	
	

	/**
	 * @return the replaceAll
	 */
	@XmlAttribute(name="accept", required=false)
	public Boolean getAccept() {
		return accept;
	}

	/**
	 * @param replaceAll the replaceAll to set
	 */
	public void setAccept(Boolean accept) {
		this.accept = accept;
	}

	

	//private boolean addYear=false;
	//private String yearString ;
	//private List<String> groupsSeparator = new ArrayList<String>();
	private String groupIndex;
//	private SimpleDateFormat dateFormat;
	private String defaultResult;
	
	private String replaceAll;
//	private boolean trimToLowerCase=false;
	//private String soft;
}
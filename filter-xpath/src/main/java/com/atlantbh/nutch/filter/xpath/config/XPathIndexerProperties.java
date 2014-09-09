package com.atlantbh.nutch.filter.xpath.config;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

public class XPathIndexerProperties {

	private String pageUrlFilterRegex;
	private Boolean trimPageContentFilterXPathData;
	private String pageContentFilterXPath;
	private String pageContentFilterRegex;
	private Boolean concatPageContentFilterXPathData;
	private String concatPageContentFilterXPathDataDelimiter;
	private List<XPathIndexerPropertiesField> xPathIndexerPropertiesFieldList = new ArrayList<XPathIndexerPropertiesField>(0);
	private String refetchOutlinkRegexp;
	private List<OutlinkFilter> outlinkFiltersList= new ArrayList<OutlinkFilter>();

	
	public XPathIndexerProperties() {}
	
	public XPathIndexerProperties(List<XPathIndexerPropertiesField> xPathIndexerPropertiesFieldList) {
		this.xPathIndexerPropertiesFieldList = xPathIndexerPropertiesFieldList;
	}
	
	
	
	@XmlAttribute(name="refetchOutlinkRegexp", required=false)
	public String getRefetchOutlinkRegexp() {
		return refetchOutlinkRegexp;
	}

	public void setRefetchOutlinkRegexp(String refetchOutlinkRegexp) {
		this.refetchOutlinkRegexp = refetchOutlinkRegexp;
	}

	@XmlAttribute(name="pageUrlFilterRegex", required=false)
	public String getPageUrlFilterRegex() {
		return pageUrlFilterRegex;
	}

	public void setPageUrlFilterRegex(String pageUrlFilterRegex) {
		this.pageUrlFilterRegex = pageUrlFilterRegex;
	}

	@XmlAttribute(name="pageContentFilterXPath", required=false)
	public String getPageContentFilterXPath() {
		return pageContentFilterXPath;
	}

	public void setPageContentFilterXPath(String pageContentFilterXPath) {
		this.pageContentFilterXPath = pageContentFilterXPath;
	}

	@XmlAttribute(name="pageContentFilterRegex", required=false)
	public String getPageContentFilterRegex() {
		return pageContentFilterRegex;
	}

	public void setPageContentFilterRegex(String pageContentFilterRegex) {
		this.pageContentFilterRegex = pageContentFilterRegex;
	}

	@XmlAttribute(name="concatPageContentFilterXPathData", required=false)
	public Boolean isConcatPageContentFilterXPathData() {
		return concatPageContentFilterXPathData;
	}

	public void setConcatPageContentFilterXPathData(Boolean concatPageContentFilterXPathData) {
		this.concatPageContentFilterXPathData = concatPageContentFilterXPathData;
	}

	@XmlAttribute(name="concatPageContentFilterXPathDataDelimiter", required=false)
	public String getConcatPageContentFilterXPathDataDelimiter() {
		return concatPageContentFilterXPathDataDelimiter;
	}

	public void setConcatPageContentFilterXPathDataDelimiter(String concatPageContentFilterXPathDataDelimiter) {
		this.concatPageContentFilterXPathDataDelimiter = concatPageContentFilterXPathDataDelimiter;
	}

	@XmlAttribute(name="trimPageContentFilterXPathData", required=false)
	public Boolean isTrimPageContentFilterXPathData() {
		return trimPageContentFilterXPathData;
	}

	public void setTrimPageContentFilterXPathData(Boolean trimPageContentFilterXPathData) {
		this.trimPageContentFilterXPathData = trimPageContentFilterXPathData;
	}

	@XmlElement(name="field", nillable=false)
	public List<XPathIndexerPropertiesField> getXPathIndexerPropertiesFieldList() {
		return xPathIndexerPropertiesFieldList;
	}

	public void setXPathIndexerPropertiesFieldList(List<XPathIndexerPropertiesField> xPathIndexerPropertiesFieldList) {
		this.xPathIndexerPropertiesFieldList = xPathIndexerPropertiesFieldList;
	}
	
	@XmlElement(name="outlinkFilter", nillable=true)
	public List<OutlinkFilter> getOutlinkFilterList() {
		return outlinkFiltersList;
	}

	public void setOutlinkFilterList(List<OutlinkFilter> outlinkFilterList) {
		this.outlinkFiltersList = outlinkFilterList;
	}
	
}

package com.atlantbh.nutch.filter.xpath.config;

import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

public class XPathIndexerPropertiesField {
	
	private String name;
	private String xPath;
	private FieldType type;
	private String dateFormat;
	private Boolean trimXPathData;
	private Boolean concat;
	private String concatDelimiter;
	private Rule regexpRule;
	private boolean withChildrenText;
	private List<XPathIndexerPropertiesField> parseMetas;
	private boolean isTitleField;
	private String injectMeta;
	private String injectMetaValue;
	public XPathIndexerPropertiesField() {}

	public XPathIndexerPropertiesField(String name, String xPath, FieldType type) {
		this.name = name;
		this.xPath = xPath;
		this.type = type;
		this.isTitleField = false;
		this.withChildrenText=false;
	}
	
	
	@XmlAttribute(name="injectMeta", required=false)
	public String getInjectMeta() {
		return injectMeta;
	}

	public void setInjectMeta(String injectMeta) {
		this.injectMeta = injectMeta;
	}
	
	
	@XmlAttribute(name="injectMetaValue", required=false)
	public String getInjectMetaValue() {
		return injectMetaValue;
	}

	public void setInjectMetaValue(String injectMetaValue) {
		this.injectMetaValue = injectMetaValue;
	}

	@XmlAttribute(name="name", required=true)
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@XmlAttribute(name="xPath", required=true)
	public String getXPath() {
		return xPath;
	}

	public void setXPath(String xPath) {
		this.xPath = xPath;
	}
	
	

	@XmlAttribute(name="type", required=true)
	public FieldType getType() {
		return type;
	}
	public void setType(FieldType type) {
		this.type = type;
	}
	
	@XmlAttribute(name="isTitleField", required=false)
	public boolean isTitleField() {
		return isTitleField;
	}
	public void setTitleField(boolean isTitleField) {
		this.isTitleField = isTitleField;
	}
	
	@XmlElement(name="regexpRule", nillable=true)
	public Rule getRegexpRule() {
		return regexpRule;
	}

	public void setRegexpRule(Rule regexpRule) {
		this.regexpRule = regexpRule;
	}
	
	@XmlElement(name="parseMetas", nillable=true)
	public List<XPathIndexerPropertiesField> getParseMetas() {
		return parseMetas;
	}

	public void setParseMetas(List<XPathIndexerPropertiesField> parseMetas) {
		this.parseMetas = parseMetas;
	}

	@XmlAttribute(name="dateFormat", required=false)
	public String getDateFormat() {
		return dateFormat;
	}

	public void setDateFormat(String dateFormat) {
		this.dateFormat = dateFormat;
	}
	
	@XmlAttribute(name="withChildrenText", required=false)
	public boolean getWithChildrenText() {
		return withChildrenText;
	}

	public void setWithChildrenText(boolean childrentext) {
		this.withChildrenText = childrentext;
	}

	@XmlAttribute(name="trimXPathData", required=false)
	public Boolean getTrimXPathData() {
		return trimXPathData;
	}

	public void setTrimXPathData(Boolean trimXPathData) {
		this.trimXPathData = trimXPathData;
	}

	@XmlAttribute(name="concat", required=false)
	public Boolean isConcat() {
		return concat;
	}

	public void setConcat(Boolean concat) {
		this.concat = concat;
	}

	@XmlAttribute(name="concatDelimiter", required=false)
	public String getConcatDelimiter() {
		return concatDelimiter;
	}

	public void setConcatDelimiter(String concatDelimiter) {
		this.concatDelimiter = concatDelimiter;
	}

	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append(getName()).append(": ");
		sb.append(xPath!=null?xPath:"no xpath");
		
		return sb.toString();
	}
	
	
}

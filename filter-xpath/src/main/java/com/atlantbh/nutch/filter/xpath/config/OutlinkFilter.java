/**
 * 
 */
package com.atlantbh.nutch.filter.xpath.config;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

/**
 * @author philippecaparroy
 *
 */
public class OutlinkFilter {

	private List<Rule> regexpRuleList = new ArrayList<Rule>();
	
	/**
	 * 
	 */
	public OutlinkFilter() {
		// TODO Auto-generated constructor stub
	}
	
	@XmlElement(name="regexpRule", nillable=true)
	public List<Rule> getRegexpRuleList() {
		return regexpRuleList;
	}

	public void setRegexpRuleList(List<Rule> regexpRuleList) {
		this.regexpRuleList = regexpRuleList;
	}
	
}

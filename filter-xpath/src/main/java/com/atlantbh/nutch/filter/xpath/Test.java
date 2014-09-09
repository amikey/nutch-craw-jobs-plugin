/**
 * 
 */
package com.atlantbh.nutch.filter.xpath;

/**
 * @author pcaparroy
 *
 */
public class Test {

	/**
	 * 
	 */
	public Test() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public String toString() {
		// TODO Auto-generated method stub
		return getClass().getName();
	}
	
	public static void main(String[] args){
		
		Test test = new Test();
		TestReader reader = new TestReader();
		reader.read(test);
		
	}

}

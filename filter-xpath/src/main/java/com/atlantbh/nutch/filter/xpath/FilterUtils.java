package com.atlantbh.nutch.filter.xpath;

import java.util.regex.Pattern;

import org.apache.nutch.util.NodeWalker;
import org.w3c.dom.Node;

public class FilterUtils {

	/**
	 * Returns the same value. If null returns the defaultValue.
	 * 
	 * @param value The value to return if not null.
	 * @param defaultValue The value to return if null.
	 * @return value or defaultValue. Depends of value is null.
	 */
	public static <T> T getNullSafe(T value, T defaultValue) {
		return value == null?defaultValue:value;
	}

	/**
	 * Extracts the text content from the supplied node.
	 * 
	 * @param node The node to extract text from.
	 * @return String that contains the textual content of the node.
	 */
	public static String extractTextContentFromRawNode(Object node,boolean withchildrenText) {

		// Extract data
		String value = null;
		if (node instanceof Node) {
			if(withchildrenText){
				StringBuffer sb = new StringBuffer();
				
				getTextHelper(sb, (Node)node, false, 0);
				
				value = sb.toString();
			}else{
				value = ((Node) node).getTextContent();
			}
		} else {
			value = String.valueOf(node);
		}

		return value;
	}

	private static boolean getTextHelper(StringBuffer sb, Node node, 
			boolean abortOnNestedAnchors,
			int anchorDepth) {
		boolean abort = false;
		NodeWalker walker = new NodeWalker(node);
		
		while (walker.hasNext()) {

			Node currentNode = walker.nextNode();
			String nodeName = currentNode.getNodeName();
			short nodeType = currentNode.getNodeType();

			if ("script".equalsIgnoreCase(nodeName)) {
				walker.skipChildren();
			}
			if ("style".equalsIgnoreCase(nodeName)) {
				walker.skipChildren();
			}
			if (abortOnNestedAnchors && "a".equalsIgnoreCase(nodeName)) {
				anchorDepth++;
				if (anchorDepth > 1) {
					abort = true;
					break;
				}        
			}
			if (nodeType == Node.COMMENT_NODE) {
				walker.skipChildren();
			}
			if (nodeType == Node.TEXT_NODE) {
				// cleanup and trim the value
				String text = currentNode.getNodeValue();
				text = text.replaceAll("\\s+", " ");
				text = text.trim();
				if (text.length() > 0) {
					if (sb.length() > 0) sb.append(' ');
					sb.append(text);
				}
			}
		}
		
		return abort;
	}

	/**
	 * Check's if the url math the regex. Regex null safe.
	 * 
	 * @param regex The regex to match against.
	 * @param url The data to match.
	 * @return True if the url matches the regex, otherwise false.
	 */
	public static boolean isMatch(String regex, String data) {

		// Compile regex pattern
		Pattern pattern = null;
		if(regex != null) {
			pattern = Pattern.compile(regex);
		} 

		if(pattern != null) {
			if(pattern.matcher(data).matches()) {
				return true;		
			}
		} else {
			return true;
		}

		return false;
	}

	/**
	 * Checks if the string is entirely made of the provided characters.
	 * 
	 * @param string The string to check.
	 * @param charactesr The characters to check against.
	 * @return True if the string is entirely made of the characters supplied, otherwise false.
	 */
	public static boolean isMadeOf(String string, String characters) {

		// Initialize StringBuilder
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.setLength(1);

		// Iterate trough string and check if it contains one of the characters
		for(int i=0;i<string.length();i++) {

			stringBuilder.setCharAt(0, string.charAt(i));
			if(!characters.contains(stringBuilder)) {
				return false;
			}
		}

		return true;
	}
}

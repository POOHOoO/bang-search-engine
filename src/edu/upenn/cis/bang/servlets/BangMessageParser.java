package edu.upenn.cis.bang.servlets;

import java.io.IOException;
import java.io.StringReader;
import java.util.LinkedList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class BangMessageParser {
	
	public static List<SearchResult> getSearchResultsFromXml(String _xmlDoc){
		List<SearchResult> searchResults = new LinkedList<SearchResult>();
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		_xmlDoc = _xmlDoc.replaceAll("&", "&amp;");
		//_xmlDoc = "<searchresults><result><url>http://www.blogger.com/feeds/11300808/posts/default</url><tfidf>0.7294396</tfidf><pagerank>0</pagerank><title>trwe</title><description>qwer</description></result><result><url>http://checkout.google.com/sell</url><tfidf>0.9256598</tfidf><pagerank>1.484433042025473</pagerank><title>fdsa</title><description>asdf</description></result><result><url>http://checkout.google.com/support/sell/bin/answer.py?answer=46174&topic=8681</url><tfidf>0.7900806</tfidf><pagerank>10.115617380239573</pagerank><title>fdsa</title><description>asdf</description></result><result><url>http://books.google.com/support</url><tfidf>0.48918414</tfidf><pagerank>15.681353544892707</pagerank><title>title</title><description>test</description></result></searchresults>";
		
		try {
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.parse(new InputSource(new StringReader(_xmlDoc)));
			
			Element rootElement = doc.getDocumentElement();
			NodeList currNode = rootElement.getElementsByTagName("result");
			if(currNode != null && currNode.getLength() > 0) 
				for(int i = 0 ; i < currNode.getLength();i++) {
					Element curr = (Element)currNode.item(i);
					SearchResult result = getSearchResult(curr);
					searchResults.add(result);
				}
		}catch(ParserConfigurationException pce) {
			return searchResults;
		}catch(SAXException se) {
			return searchResults;
		}catch(IOException ioe) {
			return searchResults;
		}
		
		return searchResults;
	}
	
	public static List<SearchResult> getSearchResultsFromYahooXml(String _xmlDoc){
		List<SearchResult> searchResults = new LinkedList<SearchResult>();
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		
		try {
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.parse(new InputSource(new StringReader(_xmlDoc)));
			
			Element rootElement = doc.getDocumentElement();
			NodeList currNode = rootElement.getElementsByTagName("Result");
			if(currNode != null && currNode.getLength() > 0) 
				for(int i = 0 ; i < currNode.getLength();i++) {
					Element curr = (Element)currNode.item(i);
					SearchResult result = getYahooSearchResult(curr);
					searchResults.add(result);
				}
		}catch(ParserConfigurationException pce) {
			pce.printStackTrace();
		}catch(SAXException se) {
			se.printStackTrace();
		}catch(IOException ioe) {
			ioe.printStackTrace();
		}
		
		return searchResults;
	}
	
	private static SearchResult getSearchResult(Element elementResult) {
		return new SearchResult(
				getTagValue(elementResult,Data.XML_URL_TAG),
				getTagValue(elementResult, Data.XML_TITLE_TAG),
				getTagValue(elementResult, Data.XML_DESC_TAG),
				getTagDoubleValue(elementResult, Data.XML_PAGERANK_TAG),
				getTagDoubleValue(elementResult, Data.XML_TFIDF_TAG)
			);
	}
	
	private static SearchResult getYahooSearchResult(Element elementResult){
		String title = getTagValue(elementResult, Data.YAHOO_XML_TITLE_TAG);
		String desc = getTagValue(elementResult, Data.YAHOO_XML_DESC_TAG);
		if(title == null)
			title = "Yahoo Search Result";
		if(desc == null)
			desc = "Yahoo Search Result";
		return new SearchResult(
				getTagValue(elementResult,Data.YAHOO_XML_URL_TAG),
				Data.YAHOO_SEARCH_RESULT_TAG + getTagValue(elementResult, Data.YAHOO_XML_TITLE_TAG),
				getTagValue(elementResult, Data.YAHOO_XML_DESC_TAG),
				new Double(-1),
				new Double(-1)
			);
	}
	
	private static SearchResult getAmazonSearchResult(Element elementResult){
		String ASIN = getTagValue(elementResult, Data.AMAZON_XML_ASIN_TAG);
		Element itemAttributes = (Element)elementResult.getElementsByTagName(Data.AMAZON_XML_ITEM_ATTRIBUTES_TAG).item(0);
		String title = getTagValue(itemAttributes, Data.AMAZON_XML_TITLE_TAG);
		String manufacturer = getTagValue(itemAttributes, Data.AMAZON_XML_MANUFACTURER_TAG);
		String productGroup = getTagValue(itemAttributes, Data.AMAZON_XML_PRODUCT_GROUP);
		String description = "Amazon.com product #" + ASIN + " - Product Group: " + productGroup + " - Manufactured by " + manufacturer + ".";
		return new SearchResult(
				getTagValue(elementResult, Data.AMAZON_XML_URL_TAG),
				Data.AMAZON_SEARCH_RESULT_TAG + title,
				description,
				new Double(-1),
				new Double(-1)
			);
	}

	private static String getTagValue(Element elementResult, String xmlTag) {
		String value = null;
		NodeList nodes = elementResult.getElementsByTagName(xmlTag);
		if(nodes != null && nodes.getLength() > 0) {
			try{
				Element currNode = (Element)nodes.item(0);
				value = currNode.getFirstChild().getNodeValue();
			} catch(NullPointerException npe){
				return null;
			}
		}

		return value;
	}

	private static double getTagDoubleValue(Element elementResult, String xmlTag) {
		return Double.parseDouble(getTagValue(elementResult,xmlTag));
	}

	public static List<SearchResult> getSearchResultsFromAmazonXml(String _xmlDoc) {
		List<SearchResult> searchResults = new LinkedList<SearchResult>();
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		
		try {
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.parse(new InputSource(new StringReader(_xmlDoc)));
			
			Element rootElement = doc.getDocumentElement();
			NodeList currNode = rootElement.getElementsByTagName("Item");
			if(currNode != null && currNode.getLength() > 0) 
				for(int i = 0 ; i < currNode.getLength();i++) {
					Element curr = (Element)currNode.item(i);
					SearchResult result = getAmazonSearchResult(curr);
					searchResults.add(result);
				}
		}catch(ParserConfigurationException pce) {
			pce.printStackTrace();
		}catch(SAXException se) {
			se.printStackTrace();
		}catch(IOException ioe) {
			ioe.printStackTrace();
		}
		
		return searchResults;
	}
}

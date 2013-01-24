package edu.upenn.cis.bang.servlets;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class AmazonFns {

	public static List<SearchResult> getSearchResultsList(String _query){
		String xml = getXmlForQuery(_query);
		if(xml == null)
			return new LinkedList<SearchResult>();
		return BangMessageParser.getSearchResultsFromAmazonXml(xml);
	}

	private static String getXmlForQuery(String _query){
		  Map<String, String> paramsToSign = new HashMap<String, String>();  
		 
		  paramsToSign.put("Keywords", _query);
		  paramsToSign.put("SearchIndex", "All");
		  paramsToSign.put("Operation", "ItemSearch");
		  paramsToSign.put("Service", "AWSECommerceService");
		        
		  SignedRequestsHelper signature;
		  String strSignature;
		  StringBuilder xmlResponse = new StringBuilder();
		  
		try {
			signature = SignedRequestsHelper.getInstance(Data.AMAZON_ENDPOINT, Data.AMAZON_ACCESS_KEY_ID, Data.AMAZON_SECRET_ACCESS_KEY);
			strSignature = signature.sign(paramsToSign);
			URL url = new URL(strSignature);
			
		    BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
		    
		    String str;
		    while ((str = in.readLine()) != null) {
		        xmlResponse.append(str);
		        xmlResponse.append(Data.NEW_LINE);
		    }
		    
		    in.close();
		    
		    return xmlResponse.toString();
		} catch (InvalidKeyException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (MalformedURLException e) {
		
		} catch (IOException e) {
		
		}
		
		return null;
	}
}

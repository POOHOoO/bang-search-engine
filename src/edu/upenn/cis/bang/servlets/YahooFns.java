package edu.upenn.cis.bang.servlets;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

public class YahooFns {	
	public static List<SearchResult> getSearchResultsList(String _query){
		return BangMessageParser.getSearchResultsFromYahooXml(getXmlForQuery(_query));		
	}
	
	private static String getXmlForQuery(String _query){
		StringBuilder xmlResponse = new StringBuilder();
		try {
		    URL url = new URL(Data.YAHOO_SEARCH_API + _query);

		    BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
		    
		    String str;
		    while ((str = in.readLine()) != null) {
		        xmlResponse.append(str);
		        xmlResponse.append(Data.NEW_LINE);
		    }
		    in.close();
		} catch (MalformedURLException e) {
		} catch (IOException e) {
		}
		
		return xmlResponse.toString();
	}
}

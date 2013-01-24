package edu.upenn.cis.bang.servlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class HandlerIndex implements Handler {
	HttpServletRequest req;
	HttpServletResponse resp;
	
	BangHTMLTemplate tplHome = null;
	
	public HandlerIndex(HttpServletRequest _req, HttpServletResponse _resp) throws IOException, BangException{
		req = _req;
		resp = _resp;
		
		tplHome = new BangHTMLTemplate(Data.HTML_TEMPLATE_INDEX);
	}
	
	public void handleRequest(PrintWriter w) throws BangException{
		
		Map<String, String> indexReplacements = new HashMap<String, String>();

		indexReplacements.put(Data.PARSER_VARIABLE_SEARCH_SUBMIT_PATH, Data.PATH_SEARCH_SUBMIT);
		//indexReplacements.put(Data.PARSER_VARIABLE_MODE_PARAM_KEY, Data.SEARCH_MODE_PARAM_KEY);
		indexReplacements.put(Data.PARSER_SEARCH_MODE_NORMAL, Data.SEARCH_MODE_NORMAL);
		indexReplacements.put(Data.PARSER_SEARCH_MODE_YAHOO, Data.SEARCH_MODE_INCLUDE_YAHOO);
		indexReplacements.put(Data.PARSER_SEARCH_MODE_AMAZON, Data.SEARCH_MODE_INCLUDE_AMAZON);
		indexReplacements.put(Data.PARSER_SEARCH_MODE_RANKINGS, Data.SEARCH_MODE_RANKINGS);
		indexReplacements.put(Data.PARSER_SEARCH_PARAM_QUERY, Data.SEARCH_PARAM_QUERY);
		indexReplacements.put(Data.PARSER_PARAM_FEELING_LUCKY, Data.PARAM_FEELING_LUCKY);
		indexReplacements.put(Data.PARSER_PARAM_PAGE_NUM, Data.PARAM_PAGE_NUM);
		indexReplacements.put(Data.PARSER_SEARCH_LUCKY_TRUE_VALUE, Data.PARAM_TRUE_VALUE);
		indexReplacements.put(Data.PARSER_PARAM_TRUE_VALUE, Data.PARAM_TRUE_VALUE);
		indexReplacements.put(Data.SEARCH_MODE_NORMAL, Data.PARAM_TRUE_VALUE);
		indexReplacements.put(Data.PARAM_SEARCH_MODE_INCLUDE_AMAZON, Data.SEARCH_MODE_INCLUDE_AMAZON);
		indexReplacements.put(Data.PARAM_SEARCH_MODE_INCLUDE_YAHOO, Data.SEARCH_MODE_INCLUDE_YAHOO);
		
		
		tplHome.addParseOperationMap(indexReplacements);
		
		w.append(tplHome.toString());
	}
}

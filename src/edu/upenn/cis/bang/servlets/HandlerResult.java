package edu.upenn.cis.bang.servlets;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class HandlerResult implements Handler{
	
	HttpServletRequest req;
	HttpServletResponse resp;
	
	Map<String, String> parameters;
	
	String userQuery = null;
	boolean isFeelingLucky = false;
	private boolean includeYahooResults = false;
	private boolean includeAmazonResults = false;
	int pageNum = 1;
	String searchMode = Data.SEARCH_MODE_NORMAL;
	
	List<BangHTMLTemplate> pageTemplates;
	BangHTMLTemplate tplHeader;
	BangHTMLTemplate tplResult;
	BangHTMLTemplate tplFooter;
	
	public HandlerResult(HttpServletRequest _req, HttpServletResponse _resp) throws BangException, IOException{
		req = _req;
		resp = _resp;
		
		parameters = req.getParameterMap();
		userQuery = parameters.get(Data.SEARCH_PARAM_QUERY);
		if(parameters.containsKey(Data.PARAM_FEELING_LUCKY)){
			//if(parameters.get(Data.PARAM_FEELING_LUCKY).equals(Data.PARAM_TRUE_VALUE))
			isFeelingLucky = true;
		} else
			isFeelingLucky = false;
		
		if(parameters.containsKey(Data.PARAM_PAGE_NUM)){
			try{
				pageNum = Integer.parseInt(parameters.get(Data.PARAM_PAGE_NUM));
			} catch(NumberFormatException nfe){
				pageNum = 1;
			}			
		}
		
		if(parameters.containsKey(Data.SEARCH_MODE_INCLUDE_YAHOO)){
			  searchMode = parameters.get(Data.SEARCH_MODE_INCLUDE_YAHOO);
			  if(searchMode.equals(Data.PARAM_TRUE_VALUE))
				  includeYahooResults = true;
		}
		
		if(parameters.containsKey(Data.SEARCH_MODE_INCLUDE_AMAZON)){
			  searchMode = parameters.get(Data.SEARCH_MODE_INCLUDE_AMAZON);
			  if(searchMode.equals(Data.PARAM_TRUE_VALUE))
				  includeAmazonResults = true;
		}
		
		pageTemplates = new ArrayList<BangHTMLTemplate>();
		tplHeader = new BangHTMLTemplate(Data.HTML_TEMPLATE_RESULTS_NORMAL_HEADER);
		tplResult = new BangHTMLTemplate(Data.HTML_TEMPLATE_RESULTS_NORMAL_RESULT);
		tplFooter = new BangHTMLTemplate(Data.HTML_TEMPLATE_RESULTS_NORMAL_FOOTER);
		pageTemplates.add(tplHeader);
		pageTemplates.add(tplResult);
		pageTemplates.add(tplFooter);		
	}
	public void handleRequest(PrintWriter w) throws BangException {
		if(userQuery == null){
			//This should not happen if Handler works right.
			//Nonetheless we should handle it somehow anyway
			//TODO
		}
		
		BangHTMLTemplate.addParseOperation(pageTemplates, Data.PARSER_VARIABLE_USER_QUERY, userQuery);
		BangHTMLTemplate.addParseOperation(pageTemplates, Data.PARSER_VARIABLE_SEARCH_MODE, searchMode);
		BangHTMLTemplate.addParseOperation(pageTemplates, Data.PARSER_VARIABLE_SEARCH_SUBMIT_PATH, Data.PATH_SEARCH_SUBMIT);
		BangHTMLTemplate.addParseOperation(pageTemplates, Data.PARSER_SEARCH_PARAM_QUERY, Data.SEARCH_PARAM_QUERY);
		
		BangHTMLTemplate.addParseOperation(pageTemplates, Data.PARAM_BANG_REFLECT_AMAZON_VAR,
				(parameters.containsKey(Data.PARAM_SEARCH_MODE_INCLUDE_AMAZON) ? parameters.get(Data.PARAM_SEARCH_MODE_INCLUDE_AMAZON) : "false"));
		BangHTMLTemplate.addParseOperation(pageTemplates, Data.PARAM_BANG_REFLECT_YAHOO_VAR,
				(parameters.containsKey(Data.PARAM_SEARCH_MODE_INCLUDE_YAHOO) ? parameters.get(Data.PARAM_SEARCH_MODE_INCLUDE_YAHOO) : "false"));
		
		BangQuery query = new BangQuery(userQuery);
		List<SearchResult> allResults = query.getLocalResultsList();
		if(includeYahooResults)
			query.setResultsList(mergeYahooResults(userQuery, allResults));
		if(includeAmazonResults)
			query.setResultsList(mergeAmazonResults(userQuery, allResults));
		int startResult = (((pageNum - 1) * Data.RESULTS_PER_PAGE) + 1);
		int endResult = startResult + Data.RESULTS_PER_PAGE;
		if(endResult > (allResults.size() - 1))
			endResult = allResults.size();
		Collections.sort(allResults);
		Collections.reverse(allResults);
		if(isFeelingLucky && !allResults.isEmpty())
			try{
				resp.sendRedirect(allResults.get(0).getUrl());
			} catch(IOException e){
				
			}
		List<SearchResult> searchResults = query.getResults(startResult, endResult);

		
		int totalPages = query.getTotalResults() / Data.RESULTS_PER_PAGE;
		if((query.getTotalResults() % Data.RESULTS_PER_PAGE) > 0)
			totalPages++;
		
		BangHTMLTemplate.addParseOperation(pageTemplates, Data.PARSER_TOTAL_PAGES, totalPages);
		
		BangHTMLTemplate.addParseOperation(pageTemplates, Data.PARSER_CURR_PAGE_NUM, pageNum);
		int prevPage = (pageNum > 1) ? (pageNum - 1) : pageNum;
		int nextPage = (pageNum < totalPages) ? (pageNum + 1) : pageNum;
		/**
		if(pageNum > 1)
			BangHTMLTemplate.addParseOperation(pageTemplates, Data.PARSER_PREV_PAGE_NUM, pageNum - 1);
		else
			BangHTMLTemplate.addParseOperation(pageTemplates, Data.PARSER_PREV_PAGE_NUM, pageNum);
		if(pageNum < totalPages)
			BangHTMLTemplate.addParseOperation(pageTemplates, Data.PARSER_NEXT_PAGE_NUM, pageNum + 1);
		else
			BangHTMLTemplate.addParseOperation(pageTemplates, Data.PARSER_NEXT_PAGE_NUM, pageNum);
		**/
		
		StringBuilder nextPgQueryStr = new StringBuilder();
		nextPgQueryStr.append(Data.PARAM_PAGE_NUM + "=" + nextPage);
		for(String key : parameters.keySet()){
			if(!key.equals(Data.PARAM_PAGE_NUM))
				nextPgQueryStr.append("&" + key + "=" + parameters.get(key));
		}
		
		BangHTMLTemplate.addParseOperation(pageTemplates, Data.PARSER_NEXT_PAGE_QUERY_STRING, nextPgQueryStr.toString());
		
		StringBuilder prevPgQueryStr = new StringBuilder();
		prevPgQueryStr.append(Data.PARAM_PAGE_NUM + "=" + prevPage);
		for(String key : parameters.keySet()){
			if(!key.equals(Data.PARAM_PAGE_NUM))
				prevPgQueryStr.append("&" + key + "=" + parameters.get(key));
		}
		
		BangHTMLTemplate.addParseOperation(pageTemplates, Data.PARSER_PREV_PAGE_QUERY_STRING, prevPgQueryStr.toString());
		
		
		BangHTMLTemplate.addParseOperation(pageTemplates, Data.PARSER_VARIABLE_NUM_RESULTS, query.getTotalResults());
		BangHTMLTemplate.addParseOperation(pageTemplates, Data.PARSER_VARIABLE_START_RESULT, startResult);
		BangHTMLTemplate.addParseOperation(pageTemplates, Data.PARSER_VARIABLE_END_RESULT, endResult);
		BangHTMLTemplate.addParseOperation(pageTemplates, Data.PARAM_SEARCH_MODE_INCLUDE_AMAZON, Data.SEARCH_MODE_INCLUDE_AMAZON);
		BangHTMLTemplate.addParseOperation(pageTemplates, Data.PARAM_SEARCH_MODE_INCLUDE_YAHOO, Data.SEARCH_MODE_INCLUDE_YAHOO);
		
		//Spell check
		BangSpellChecker spell = null;
		boolean madeSuggestion = false;
		StringBuilder fixedQuery = new StringBuilder();
		try {
			spell = new BangSpellChecker();
			String tokQuery[] = userQuery.split(" ");
			int numWords = tokQuery.length;
			Queue<String> revisedQueryQueue = new PriorityQueue<String>();
			for(int i = 0; i < numWords; i++){
				if(spell.isCorrect(tokQuery[i]))
					revisedQueryQueue.offer(tokQuery[i]);
				else{
					madeSuggestion = true;
					revisedQueryQueue.offer(spell.getSuggestion(tokQuery[i]));
				}
			}
			
			fixedQuery = new StringBuilder();
			while(!revisedQueryQueue.isEmpty()){
				fixedQuery.append(revisedQueryQueue.poll());
				if(!revisedQueryQueue.isEmpty()) fixedQuery.append(" ");
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
		}
		
		if(madeSuggestion){			
			StringBuilder spellSearchQString = new StringBuilder();
			spellSearchQString.append(Data.SEARCH_PARAM_QUERY + "=" + fixedQuery.toString());
			for(String key : parameters.keySet()){
				if(!key.equals(Data.PARAM_PAGE_NUM) && !key.equals(Data.SEARCH_PARAM_QUERY))
					spellSearchQString.append("&" + key + "=" + parameters.get(key));
			}
			
			BangHTMLTemplate.addParseOperation(pageTemplates, Data.PARSER_VARIABLE_SPELLING_MOD_PREF, Data.SPELLING_MOD_PREF);
			BangHTMLTemplate.addParseOperation(pageTemplates, Data.PARSER_VARIABLE_SPELLING_MOD_SUGGESTION, "<a href=\"" + Data.PATH_SEARCH_SUBMIT + "?" + spellSearchQString.toString() + "\">" + fixedQuery.toString());
			BangHTMLTemplate.addParseOperation(pageTemplates, Data.PARSER_VARIABLE_SPELLING_MOD_SUFF, Data.SPELLING_MOD_SUFF);
			
			
		} else {
			BangHTMLTemplate.addParseOperation(pageTemplates, Data.PARSER_VARIABLE_SPELLING_MOD_PREF, "");
			BangHTMLTemplate.addParseOperation(pageTemplates, Data.PARSER_VARIABLE_SPELLING_MOD_SUGGESTION, "");
			BangHTMLTemplate.addParseOperation(pageTemplates, Data.PARSER_VARIABLE_SPELLING_MOD_SUFF, "");
		}
		
		//Parse in URLs to next page and prev page
		
		
		int i = Data.PARSER_COUNTER_START_VALUE;
		for(SearchResult result : searchResults){
			if(result.getUrl() == null){
				searchResults.remove(result);
				continue;
			}
			if((result.getTitle() == null) || result.getTitle().length() < 2)
				result.setTitle(result.getUrl());
			if((result.getDescription() == null) || (result.getDescription().length() < 2))
				result.setDescription(result.getUrl());
			tplResult.addParseOperation(Data.PARSER_VARIABLE_TITLE_PREF + i, result.getTitle());
			tplResult.addParseOperation(Data.PARSER_VARIABLE_URL_PREF + i, result.getUrl());
			if(result.getDescription() != null)
				tplResult.addParseOperation(Data.PARSER_VARIABLE_DESCRIPTION_PREF + i, result.getDescription());
			else
				tplResult.addParseOperation(Data.PARSER_VARIABLE_DESCRIPTION_PREF + i, "This result is from Yahoo.com's search engine.");
			tplResult.addParseOperation(Data.PARSER_VARIABLE_BANG_RANK_PREF + i, result.getBangRank());
			
			i++;
		}
		
		for(; i <= Data.RESULTS_PER_PAGE + 1; i++){
			tplResult.addParseOperation(Data.PARSER_VARIABLE_TITLE_PREF + i, "");
			tplResult.addParseOperation(Data.PARSER_VARIABLE_URL_PREF + i, "");
			tplResult.addParseOperation(Data.PARSER_VARIABLE_DESCRIPTION_PREF + i, "");
			tplResult.addParseOperation(Data.PARSER_VARIABLE_BANG_RANK_PREF + i, "");
		}
		
		w.append(tplHeader.toString());
		w.append(tplResult.toString());
		w.append(tplFooter.toString());
	}
	private List<SearchResult> mergeAmazonResults(String _userQuery,
			List<SearchResult> _bangSearchResults) {

		List<SearchResult> amazonResults = AmazonFns.getSearchResultsList(_userQuery);
		if(amazonResults.isEmpty())
			return _bangSearchResults;
		
		try{
			Map<SearchResult, String> mResultToUrl = new HashMap<SearchResult, String>();
			for(SearchResult result : _bangSearchResults)
				mResultToUrl.put(result, new URI(result.getUrl()).normalize().toString());
		
			for(SearchResult result: amazonResults)
				if(!mResultToUrl.containsValue(result.getUrl())){
					result.setBangRank(BangRankCalculator.calculateExternalBangRank(_userQuery, result, Data.WEIGHT_AMAZON_BASE));
					_bangSearchResults.add(result);
				}
					
			return _bangSearchResults;
		} catch(Exception e){
			return _bangSearchResults;
		}
	}
	private List<SearchResult> mergeYahooResults(String _userQuery,
			List<SearchResult> _bangSearchResults) {

		List<SearchResult> yahooResults = YahooFns.getSearchResultsList(_userQuery);
		if(yahooResults.isEmpty())
			return _bangSearchResults;
		
		try{
			Map<SearchResult, String> mResultToUrl = new HashMap<SearchResult, String>();
			for(SearchResult result : _bangSearchResults)
				mResultToUrl.put(result, new URI(result.getUrl()).normalize().toString());
		
			for(SearchResult result: yahooResults){
				if(!mResultToUrl.containsValue(result.getUrl())){
					result.setBangRank(BangRankCalculator.calculateExternalBangRank(_userQuery, result, Data.WEIGHT_AMAZON_BASE));
					_bangSearchResults.add(result);
				}
			}
			
		
			return _bangSearchResults;
		} catch(Exception e){
			return _bangSearchResults;
		}
	}
}

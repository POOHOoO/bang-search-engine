package edu.upenn.cis.bang.servlets;

import java.util.List;

public class SearchResult implements Comparable<SearchResult> {
	
	private String url;
	private String title;
	private String description;
	//private List<String> keywords;
	private double pageRank;
	private double tfidf;
	//private double percentKeywordsMatched;
	
	private double bangRank;
	
	public SearchResult(String _url, String _title, String _description, double _pageRank, double _tfidf){
		url = _url;
		title = _title;
		description = _description;
		//keywords = _keywords;
		pageRank = _pageRank;
		tfidf = _tfidf;
		
		//percentKeywordsMatched = getPercentKeywordsMatched(_userKeywords);
		bangRank = calculateBangRank();
	}
	
	public double calculateBangRank() {
		return ((Data.WEIGHT_PAGE_RANK * pageRank) + (Data.WEIGHT_TFIDF * tfidf));
	}

	public double getBangRank(){
		return bangRank;
	}
	
	public void setBangRank(double _bangRank){
		bangRank = _bangRank;		
	}
	
	public double getTfidf(){
		return tfidf;
	}
	
	public void setTfidf(double _tfidf){
		tfidf = _tfidf;
	}
	
	/**
	 * Deprecated by TFIDF
	public double getPercentKeywordsMatched(List<String> userKeywords){
		int totalKeywords = userKeywords.size();
		int keywordsMatched = 0;
		for(String keyword : userKeywords)
			if(keywords.contains(keyword))
				keywordsMatched++;
		return (keywordsMatched / totalKeywords);
	}
	
	public void setPercentKeywordsMatched(List<String> userKeywords){
		//percentKeywordsMatched = getPercentKeywordsMatched(userKeywords);
		bangRank = calculateBangRank();
	}

	public double getPercentKeywordsMatched(){
		return percentKeywordsMatched;
	}
	**/
	
	public String getUrl() {
		return url;
	}
	

	public void setUrl(String url) {
		this.url = url;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	/**
	public List<String> getKeywords() {
		return keywords;
	}

	public void setKeywords(List<String> keywords) {
		this.keywords = keywords;
	}
	**/

	public double getPageRank() {
		return pageRank;
	}

	public void setPageRank(double pageRank) {
		this.pageRank = pageRank;
	}
	
	public String getDescription(){
		return description;
	}
	
	public void setDescription(String _description){
		description = _description;
	}

	@Override
	public int compareTo(SearchResult _otherResult) {
		if(getBangRank() > ((SearchResult)_otherResult).getBangRank())
			return 1;
		if(getBangRank() < ((SearchResult)_otherResult).getBangRank())
			return -1;
		return 0;
	}
}

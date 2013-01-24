package edu.upenn.cis.bang.servlets;

public class BangRankCalculator {

	private static double getPctKeywordsInString(String _query, String _content){
		if((_query == null) || (_content == null))
			return 0;
		
		String tokQuery[] = _query.split(" ");
		String tokContent[] = _content.split(" ");
		
		int keywordsMatched = 0;
		int totalKeywords = tokQuery.length;
		
		for(int i = 0; i < tokQuery.length; i++)
			for(int j = 0; j < tokContent.length; j++)
				if(tokQuery[i].equals(tokContent[j]))
					keywordsMatched++;
		
		return (keywordsMatched / totalKeywords);
	}
	
	public static double calculateExternalBangRank(String _userQuery,
			SearchResult result, double baseWeight) {

		return baseWeight
			+ Data.WEIGHT_TITLE_MULTIPLIER * getPctKeywordsInString(_userQuery, result.getTitle())
			+ Data.WEIGHT_DESC_MULTIPLIER * getPctKeywordsInString(_userQuery, result.getDescription());
		
	}

}

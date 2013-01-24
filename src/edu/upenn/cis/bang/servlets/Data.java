package edu.upenn.cis.bang.servlets;

public class Data {

	public static final String LOG_FILE_PATH = "logs";
	public static final String LOG_FILE_PREFIX = "servlet_";
	public static final String LOG_FILE_EXT = "log";
	
	public static final String HTML_TEMPLATE_PATH = "web_tpl";
	public static final String HTML_TEMPLATE_EXT = "tpl";
	public static final String HTML_TEMPLATE_INDEX = "home";	
	public static final String HTML_TEMPLATE_RESULTS_NORMAL_HEADER = "results_normal_header";
	public static final String HTML_TEMPLATE_RESULTS_NORMAL_RESULT = "results_normal_entry";
	public static final String HTML_TEMPLATE_RESULTS_NORMAL_FOOTER = "results_normal_footer";
	
	public static final String DICTIONARY_PATH = "spell_check_word_list_simple.dat";
	
	public static final String PARSER_PREFIX = "###BANG_";
	public static final String PARSER_SUFFIX = "###";
	public static final int PARSER_COUNTER_START_VALUE = 0;
	
	public static final String PATH_SEARCH_SUBMIT = "search";
	public static final String SEARCH_MODE_PARAM_KEY = "mode";
	public static final String SEARCH_MODE_NORMAL = "n";
	//public static final String SEARCH_MODE_YAHOO = "y";
	//public static final String SEARCH_MODE_AMAZON = "a";
	public static final String SEARCH_MODE_RANKINGS = "r";	
	public static final String SEARCH_PARAM_QUERY = "q";
	public static final String PARAM_FEELING_LUCKY = "f";
	public static final String PARAM_PAGE_NUM = "p";
	public static final String PARAM_TRUE_VALUE = "true";
	public static final String SEARCH_MODE_INCLUDE_YAHOO = "y";
	public static final String SEARCH_MODE_INCLUDE_AMAZON = "a";
	public static final String PARAM_SEARCH_MODE_INCLUDE_YAHOO = "SEARCH_MODE_INCLUDE_YAHOO";
	public static final String PARAM_SEARCH_MODE_INCLUDE_AMAZON = "SEARCH_MODE_INCLUDE_AMAZON";
	
	public static final String PARAM_BANG_REFLECT_YAHOO_VAR = "REFLECT_YAHOO_VAR";
	public static final String PARAM_BANG_REFLECT_AMAZON_VAR = "REFLECT_AMAZON_VAR";
	
	public static final String SPELLING_MOD_PREF = "Did you mean: ";
	public static final String SPELLING_MOD_SUFF = "</a>?";
	public static final String PARSER_SPELLING_QUERY = "SPELLING_QUERY_STRING";
	
	public static final String PARSER_VARIABLE_USER_QUERY = "USER_QUERY";
	public static final String PARSER_VARIABLE_NUM_RESULTS = "NUM_RESULTS";
	public static final String PARSER_VARIABLE_START_RESULT = "START_RESULT_NUM";
	public static final String PARSER_VARIABLE_END_RESULT = "END_RESULT_NUM";
	public static final String PARSER_VARIABLE_TITLE_PREF = "RESULT_TITLE_";
	public static final String PARSER_VARIABLE_URL_PREF = "RESULT_URL_";
	public static final String PARSER_VARIABLE_DESCRIPTION_PREF = "RESULT_DESC_";
	public static final String PARSER_VARIABLE_BANG_RANK_PREF = "RESULT_BANGRANK_";
	public static final String PARSER_VARIABLE_SPELLING_MOD_PREF = "SPELLING_MOD_PREF";
	public static final String PARSER_VARIABLE_SPELLING_MOD_SUGGESTION = "SPELLING_MOD_SUGG";
	public static final String PARSER_VARIABLE_SPELLING_MOD_SUFF = "SPELLING_MOD_SUFF";
	public static final String PARSER_VARIABLE_SEARCH_MODE = "SEARCH_MODE";
	
	public static final String PARSER_VARIABLE_SEARCH_SUBMIT_PATH = "SEARCH_SUBMIT_PATH";
	public static final String PARSER_VARIABLE_MODE_PARAM_KEY = "MODE_PARAM_KEY";
	public static final String PARSER_SEARCH_MODE_NORMAL = "MODE_NORMAL";
	public static final String PARSER_SEARCH_MODE_YAHOO = "MODE_YAHOO";
	public static final String PARSER_SEARCH_MODE_AMAZON = "MODE_AMAZON";
	public static final String PARSER_SEARCH_MODE_RANKINGS = "MODE_RANKINGS";
	public static final String PARSER_SEARCH_PARAM_QUERY = "QUERY_PARAM";
	public static final String PARSER_PARAM_FEELING_LUCKY = "FEELING_LUCKY_PARAM";
	public static final String PARSER_PARAM_PAGE_NUM = "PAGE_NUM_PARAM_KEY";
	public static final String PARSER_SEARCH_LUCKY_TRUE_VALUE = "FEELING_LUCKY_TRUE_VALUE";
	public static final String PARSER_CURR_PAGE_NUM = "CURRENT_PAGE_NUM";
	public static final String PARSER_PREV_PAGE_NUM = "PREV_PAGE_NUM";
	public static final String PARSER_NEXT_PAGE_NUM = "NEXT_PAGE_NUM";
	public static final String PARSER_TOTAL_PAGES = "TOTAL_PAGES";
	public static final String PARSER_PARAM_TRUE_VALUE = "PARAM_TRUE_VALUE";
	public static final String PARSER_NEXT_PAGE_QUERY_STRING = "NEXT_PAGE_QUERY_STRING";
	public static final String PARSER_PREV_PAGE_QUERY_STRING = "PREV_PAGE_QUERY_STRING";
	
	public static final int RESULTS_PER_PAGE = 25;
	
	public static final double WEIGHT_PAGE_RANK = 0.30;
	public static final double WEIGHT_TFIDF = 0.70;
	
	public static final String XML_URL_TAG = "url";
	public static final String XML_TITLE_TAG = "title";
	public static final String XML_DESC_TAG = "description";
	public static final String XML_PAGERANK_TAG = "pagerank";
	public static final String XML_TFIDF_TAG = "tfidf";
	
	public static final String XML_TAG_OPEN = "<";
	public static final String XML_DATATYPE_TAG = "type";
	public static final String XML_TAG_CLOSE = ">";
	public static final String XML_DATATYPE_QUERY = "query";
	public static final String XML_TAG_END_SYMBOL = "/";	
	public static final String XML_QUERY_TAG = "query";
	
	public static final String NEW_LINE = "\n";
	
	public static final String YAHOO_APP_ID = "o7KCBP32";
	public static final int YAHOO_NUM_RESULTS = 25;
	public static final String YAHOO_SEARCH_API = "http://search.yahooapis.com/WebSearchService/V1/webSearch?appid=" + YAHOO_APP_ID + "&results=" + YAHOO_NUM_RESULTS + "&query=";
	public static final String YAHOO_XML_URL_TAG = "Url";
	public static final String YAHOO_XML_TITLE_TAG = "Title";
	public static final String YAHOO_XML_DESC_TAG = "Summary";
	public static final String YAHOO_SEARCH_RESULT_TAG = "[YAHOO] ";
	
	public static final String AMAZON_ENDPOINT = "ecs.amazonaws.com";
	public static final String AMAZON_ACCESS_KEY_ID = "AKIAJHQRVVAIBQVKMWCA";
	public static final String AMAZON_SECRET_ACCESS_KEY = "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";
	public static final String AMAZON_XML_URL_TAG = "DetailPageURL";
	public static final String AMAZON_XML_ITEM_ATTRIBUTES_TAG = "ItemAttributes";
	public static final String AMAZON_XML_ASIN_TAG = "ASIN";
	public static final String AMAZON_XML_TITLE_TAG = "Title";
	public static final String AMAZON_XML_MANUFACTURER_TAG = "Manufacturer";
	public static final String AMAZON_XML_PRODUCT_GROUP = "ProductGroup";
	public static final String AMAZON_SEARCH_RESULT_TAG = "[AMAZON] ";
	
	public static final String INDEX_NODE_HOST = "ec2-174-129-137-76.compute-1.amazonaws.com";
	public static final int INDEX_NODE_PORT = 5555;
	
	public static final double WEIGHT_AMAZON_BASE = 0.4;
	public static final double WEIGHT_YAHOO_BASE = 0.5;
	public static final double WEIGHT_TITLE_MULTIPLIER = 1.30;
	public static final double WEIGHT_DESC_MULTIPLIER = 1.20;
}

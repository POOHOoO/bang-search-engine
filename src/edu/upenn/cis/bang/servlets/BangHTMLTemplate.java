package edu.upenn.cis.bang.servlets;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class BangHTMLTemplate {
	
	String rawTpl;
	Map<String, String> parseOperations;
	
	public BangHTMLTemplate(String _tplName) throws IOException{
		parseOperations = new HashMap<String, String>();
		rawTpl = getRawTpl(_tplName);
	}
	
	public static void addParseOperation(List<BangHTMLTemplate> _templates, String _pattern, Object _value){
		for(BangHTMLTemplate tpl : _templates)
			tpl.addParseOperation(_pattern, _value);
	}
	
	public void addParseOperationMap(Map<String, String> operations){
		for(String key : operations.keySet())
			addParseOperation(key, operations.get(key));
	}
	
	public void addParseOperation(String _pattern, Object _value){
		//Will handle String and BangHTMLTemplate as _value
		if(parseOperations.containsKey(_pattern))
			parseOperations.remove(_pattern);
		
		parseOperations.put(_pattern, _value.toString());
	}
	
	public String parseTemplate(){
		String currHTML = rawTpl;
		for(String _key : parseOperations.keySet())
			currHTML = currHTML.replaceAll(Data.PARSER_PREFIX + _key + Data.PARSER_SUFFIX, parseOperations.get(_key));
		
		return currHTML;
	}
	
	@Override
	public String toString(){
		return parseTemplate();
	}
	
	private String getRawTpl(String _tplName) throws IOException{
		String fName = Data.HTML_TEMPLATE_PATH + File.separator + _tplName + "." + Data.HTML_TEMPLATE_EXT;
		File tplFile = new File(fName);
		BufferedReader br = new BufferedReader(new FileReader(tplFile));
		StringBuilder sb = new StringBuilder();
		while(true){
			String currLine = br.readLine();
			if(currLine == null)
				break;
			sb.append(currLine);
		}
		
		return sb.toString();
	}
}

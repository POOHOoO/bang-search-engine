package edu.upenn.cis.bang.servlets;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BangSpellChecker {

	private static ArrayList<String> dictionary = null;
	
	public static void initSpellChecker(String _dictionaryPath){
		dictionary = new ArrayList<String>();
		try{
			BufferedReader in = new BufferedReader(new FileReader(_dictionaryPath));
			for(String temp = ""; temp != null; temp = in.readLine()){
				String words[] = temp.split(" ");
				for(int i = 0; i < words.length; i++)
					if(!dictionary.contains(words[i].toLowerCase()))
						dictionary.add(words[i].toLowerCase());
			}
			in.close();
		} catch(IOException e){
			e.printStackTrace();
		}
	}
	
	public BangSpellChecker() throws IOException{
		if(dictionary == null)
			initSpellChecker(Data.DICTIONARY_PATH);
	}
	
	public BangSpellChecker(String _dictionaryPath) throws IOException {
		if(dictionary == null)
			initSpellChecker(_dictionaryPath);
	}

	public boolean isCorrect(String _queryWord) {
		return dictionary.contains(_queryWord.toLowerCase());
	}

	public String getSuggestion(String _queryWord) {
		_queryWord = _queryWord.toLowerCase();
		if(isCorrect(_queryWord))
			return _queryWord;
		
		ArrayList<String> possibleCorrections = getPossibleCorrections(_queryWord);
		ArrayList<String> candidates = new ArrayList<String>();
		
		for(String s : possibleCorrections)
			if(dictionary.contains(s))
				candidates.add(s);
		if(candidates.size() > 0){
			//bias towards matching words that have the same length as input word
			for(String w : candidates)
				if(w.length() == _queryWord.length())
					return w;
			
			//if not, bias towards matching words with one letter less
			for(String w : candidates)
				if((_queryWord.length() - w.length()) == 1)
					return w;
			
			//if not, bias towards matching words with one more letter
			for(String w : candidates)
				if((w.length() - _queryWord.length()) == 1)
					return w;
			
			//Else, just spit out the first one we have
			return candidates.get(0);
		}
		else
			return _queryWord;
	}


	private ArrayList<String> getPossibleCorrections(String _queryWord) {
		ArrayList<String> correctionsList = new ArrayList<String>();
		
		for(int i=0; i < _queryWord.length(); i++){
			String chunk1 = _queryWord.substring(0, i);
			String chunk2 = _queryWord.substring(i + 1, _queryWord.length());
			correctionsList.add(chunk1 + chunk2);
			for(char letter='a'; letter <= 'z'; letter++)
				correctionsList.add(chunk1 + letter + chunk2);			
		}
		
		for(int i=0; i < _queryWord.length() - 1; i++){
			String chunk1 = _queryWord.substring(0, i);
			String chunk2 = _queryWord.substring(i + 1, i + 2);
			String chunk3 = _queryWord.substring(i, i + 1);
			String chunk4 = _queryWord.substring(i + 2, _queryWord.length());
			
			correctionsList.add(chunk1 + chunk2 + chunk3 + chunk4);
		}

		for(int i = 0; i <= _queryWord.length(); i++)
			for(char letter = 'a'; letter <= 'z'; letter++){
				String chunk1 = _queryWord.substring(0, i);
				String chunk2 = _queryWord.substring(i, _queryWord.length());
				correctionsList.add(chunk1 + String.valueOf(letter) + chunk2);
			}

		return correctionsList;
	}
}

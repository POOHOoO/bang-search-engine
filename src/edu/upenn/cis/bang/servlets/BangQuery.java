package edu.upenn.cis.bang.servlets;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.LinkedList;
import java.util.List;

public class BangQuery {

	private String query;	
	List<SearchResult> resultsList;
	//private int numResults;
	
	public BangQuery(String _userQuery) {
		query = _userQuery;
		
		try{
			String ringResponse = getResultsFromRing(query);
			resultsList = parseResponseToList(ringResponse);
		} catch(Exception e){
			resultsList = new LinkedList<SearchResult>();
		}
		//FOR DEVELOPMENT
		//resultsList = getFakeResultsList();
		//numResults = resultsList.size();
	}
	
	public int getTotalResults(){
		return resultsList.size();
	}
	
	public List<SearchResult> getResults(int _startResult, int _endResult){		
		List<SearchResult> resultsPage = new LinkedList<SearchResult>();		
		for(int i = _startResult; i <= _endResult; i++)
			resultsPage.add(resultsList.get(i - 1));
					
		return resultsPage;
	}
	
	private List<SearchResult> getFakeResultsList() {
		List<SearchResult> fakeResults = new LinkedList<SearchResult>();
		List<String> keywords1 = new LinkedList<String>();
		keywords1.add("aardvark");
		keywords1.add("beetle");
		keywords1.add("cat");
		keywords1.add("dog");
		keywords1.add("elephant");
		List<String> keywords1a = new LinkedList<String>();
		keywords1a.add("aardvark");
		keywords1a.add("beetle");
		keywords1a.add("cat");
		keywords1a.add("bison");
		List<String> keywords2 = new LinkedList<String>();
		keywords2.add("ferret");
		keywords2.add("giraffe");
		keywords2.add("hippo");
		List<String> keywords2a = new LinkedList<String>();
		keywords2a.add("giraffe");
		keywords2a.add("bologna");
		List<String> keywords3 = new LinkedList<String>();
		keywords3.add("cat");
		keywords3.add("dog");
		keywords3.add("hippo");
		keywords3.add("ice");
		keywords3.add("jackal");
		List<String> keywords3a = new LinkedList<String>();
		keywords3a.add("cat");
		keywords3a.add("bison");
		keywords3a.add("buffalo");
		keywords3a.add("mouse");
		List<String> keywords4 = new LinkedList<String>();
		keywords4.add("zebra");
		keywords4.add("yak");
		keywords4.add("xylophone");
		keywords4.add("water");
		keywords4.add("jackal");
		List<String> keywords4a = new LinkedList<String>();
		keywords4a.add("zebra");
		keywords4a.add("yak");
		keywords4a.add("xylophone");
		keywords4a.add("water");
		keywords4a.add("asdf");
		
		fakeResults.add(new SearchResult("http://www.java.com", "Java Home Page", "Description description description <b>description</b> description descrtiption description description <u>description</u> description description", 0.85, 0.50));
		fakeResults.add(new SearchResult("http://www.microsoft.com", "Microsoft", "Description description description <b>description</b> description descrtiption description description <u>description</u> description description", 0.7, 0.60));
		fakeResults.add(new SearchResult("http://www.stackoverflow.com", "Stack Overflow", "Description description description <b>description</b> description descrtiption description description <u>description</u> description description", 0.6, 0.30));
		fakeResults.add(new SearchResult("http://www.google.com", "Google", "Description description description <b>description</b> description descrtiption description description <u>description</u> description description", 0.9, 0.75));
		fakeResults.add(new SearchResult("http://www.yahoo.com", "Yahoo!", "Description description description <b>description</b> description descrtiption description description <u>description</u> description description", 0.85, 0.25));
		fakeResults.add(new SearchResult("http://www.amazon.com", "Amazon.com", "Description description description <b>description</b> description descrtiption description description <u>description</u> description description", 0.7, 0.6));
		fakeResults.add(new SearchResult("http://www.ebay.com", "Ebay", "Description description description <b>description</b> description descrtiption description description <u>description</u> description description", 0.6, 0.30));
		fakeResults.add(new SearchResult("http://www.thedp.com", "The Daily Pennsylvanian", "Description description description <b>description</b> description descrtiption description description <u>description</u> description description", 0.9, 0.40));
		fakeResults.add(new SearchResult("http://www.nytimes.com", "The New York Times", "Description description description <b>description</b> description descrtiption description description <u>description</u> description description", 0.85, 0.65));
		fakeResults.add(new SearchResult("http://www.wsj.com", "The Wall Street Journal", "Description description description <b>description</b> description descrtiption description description <u>description</u> description description", 0.7, 0.80));
		
		return fakeResults;
	}

	private List<SearchResult> parseResponseToList(String _ringResponse) {
		return BangMessageParser.getSearchResultsFromXml(_ringResponse);
	}

	private String getResultsFromRing(String _query) {
		//Construct XML send schema
		StringBuilder data = new StringBuilder();
		data.append(Data.XML_TAG_OPEN);
		data.append(Data.XML_DATATYPE_TAG);
		data.append(Data.XML_TAG_CLOSE);
		data.append(Data.XML_DATATYPE_QUERY);
		data.append(Data.XML_TAG_OPEN);
		data.append(Data.XML_TAG_END_SYMBOL);
		data.append(Data.XML_DATATYPE_TAG);
		data.append(Data.XML_TAG_CLOSE);
		data.append(Data.NEW_LINE);
		data.append(Data.XML_TAG_OPEN);
		data.append(Data.XML_QUERY_TAG);
		data.append(Data.XML_TAG_CLOSE);
		data.append(_query);
		data.append(Data.XML_TAG_OPEN);
		data.append(Data.XML_TAG_END_SYMBOL);
		data.append(Data.XML_QUERY_TAG);
		data.append(Data.XML_TAG_CLOSE);
		data.append(Data.NEW_LINE);
		data.append(Data.NEW_LINE);
		data.append(Data.NEW_LINE);
		
		String response = "";
		
		try{
			Socket socket = new Socket(Data.INDEX_NODE_HOST, Data.INDEX_NODE_PORT);
			OutputStream socketOutputStream = socket.getOutputStream();
			socketOutputStream.write(data.toString().getBytes());
			socketOutputStream.flush();
			//socketOutputStream.close();
			
			
			InputStream input = socket.getInputStream();
            int read = -1;
            StringBuilder sb = new StringBuilder();
            while ((read = input.read()) >= 0)
            	sb.append((char)read);
			/**
			ObjectInputStream socketInputStream = new ObjectInputStream(socket.getInputStream());
			response = (String)socketInputStream.readObject();
			**/
			//socketInputStream.close();
            
            response = sb.toString();
			socket.close();
		} catch(IOException e){
			return response;
		} //catch(ClassNotFoundException cnfe){
			//return response;
		//}
		
		return response;
	}
	
	public List<SearchResult> getLocalResultsList(){
		return resultsList;
	}

	@Override
	public String toString(){
		return query;
	}

	public void setResultsList(List<SearchResult> _results) {
		resultsList = _results;
		
	}

}

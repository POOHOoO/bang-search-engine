package edu.upenn.cis.bang.indexer;

import java.io.Serializable;


public class Url_data  implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	String pagerank = "";
	String title = "";
	String description ="";
	
	public Url_data(String pagerank, String title, String description){
		this.pagerank = pagerank;
		this.title = title;
		this.description = description;
	}
	
}

package edu.upenn.cis.bang.indexer;

import rice.p2p.commonapi.Id;


public class Id_PageRank {

	Id id;
	String pagerank;
	String url;
	
	Id_PageRank(Id id, String pagerank, String url){
		this.id = id;
		this.url = url;
		this.pagerank = pagerank;
		
	}
}



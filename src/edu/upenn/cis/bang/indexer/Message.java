package edu.upenn.cis.bang.indexer;

import rice.p2p.commonapi.NodeHandle;


public class Message implements rice.p2p.commonapi.Message{
	

	private static final long serialVersionUID = 1L;

	NodeHandle from;
	String content = "";
	boolean wantResponse = true;
	String type = "";

	public int getPriority() {
		return 0;
	}

	public Message(NodeHandle nodeHandle, String content){
		this.from = nodeHandle;
		this.content = content;
	}
	
}

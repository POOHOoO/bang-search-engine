

import rice.p2p.commonapi.Id;
import rice.p2p.commonapi.Message;
import rice.p2p.commonapi.NodeHandle;
public class CrawlerMessage implements Message{
	

	private static final long serialVersionUID = 1L;
	private NodeHandle from;
	private String type;
	private boolean needResponse = true;
	private Id toId = null;
	private String url, content;

	
	public String getURL(){
		return url;
	}
	public String getType(){
		return type;
	}
	
	public NodeHandle getFrom(){
		return this.from;
	}
	
	public boolean doesNeedRes(){
		return this.needResponse;
	}
	
	//  message
	public CrawlerMessage(NodeHandle from, Id to, String type, String url, String content, boolean needResponse)
	{
		this.from = from;
		this.toId = to;
		this.type = type;
		this.content = content;
		this.needResponse = needResponse;
		this.url = url;
	}

	public String toString() {
		  return "CrawlerMessage from "+ from.getId() +" to "+toId;
	}

	public int getPriority() {
		return Message.MEDIUM_PRIORITY;
	}
	public String getContent() {
		// TODO Auto-generated method stub
		return content;
	}
}

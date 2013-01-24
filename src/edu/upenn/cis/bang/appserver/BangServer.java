package edu.upenn.cis.bang.appserver;

/**
 * 
 * @author Zach
 *
 *	Wrapper class to run Bang app server
 *
 */
public class BangServer {
	
	public BangServer() throws Exception{
		String[] args = { "8080", "htdocs/", "conf/web.xml" };
		HttpServer.main(args);
	}
	
	//portnum, htdocs, web.xml
	public BangServer(int port, String htdocs, String webXml) throws Exception{
		String[] args = { Integer.toString(port), htdocs, webXml };
		HttpServer.main(args);
	}
	
	public static void main(String[] args) throws Exception{
		//later, these will come from the cmd line or a cfg file
		//For now, run with default
		BangServer b = new BangServer();
	}
	
}

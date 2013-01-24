




import java.io.File;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Vector;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import java.net.InetAddress;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Vector;

import rice.p2p.commonapi.Id;
import rice.p2p.commonapi.Message;
import rice.p2p.commonapi.Node;
import rice.p2p.commonapi.NodeHandle;
import rice.p2p.commonapi.Endpoint;
import rice.p2p.commonapi.Application;
import rice.p2p.commonapi.RouteMessage;


import java.io.File;
import java.net.URL;

import javax.servlet.http.HttpServlet;


public class CrawlerPastryApp implements Application
{

	private static File envHome;
	private BDBEnv myDbEnv;
	DataAccessor da;
	NodeFactory nodeFactory;
	private Node node;
	Endpoint endpoint;

	public static int numWorkers = 10;
	private  Crawler crawler;
	private boolean isBootstrap;
	private BufferedWriter out;
	//temporary storage for response results.
	public  HashMap<String, String> resultMap;
	private boolean isStopped;

	public Node getNode(){
		return node;
	}
	public CrawlerPastryApp(NodeFactory nodeFactory, String url,
			BDBEnv dbEnv, boolean isBootstrap, InetAddress ipaddress, 
			double maxMegabytes, int maxFiles, boolean threaded, boolean continuous,
			String indexerIPAddress, int indexerPort, BufferedWriter out)
	throws URISyntaxException, IOException
	{
		this.out = out;
		this.isBootstrap = isBootstrap;
		resultMap = new HashMap<String, String>();
		//crawler running on that node.
		//if bootstrapping node. Set up DBEnv for content-seen and URL-seen test.

		this.nodeFactory = nodeFactory;
		this.node = nodeFactory.getNode();

		endpoint = node.buildEndpoint(this, "Crawler Pastry App");

		endpoint.register();
		////System.out.println("CrawlerPastryApp with node id="+this.node.getId()+" started.");
		synchronized(out){
			//out.write("CrawlerPastryApp with node id="+this.node.getId()+" started."+"\n");
		}

		if (isBootstrap)
			this.crawler = new Crawler(this, nodeFactory.bootHandle, url, dbEnv,  
					maxMegabytes,  maxFiles,  threaded,  continuous, indexerIPAddress, indexerPort, out);
		else
			this.crawler = new Crawler(this, nodeFactory.bootHandle, maxMegabytes,  
					maxFiles,  threaded,  continuous, indexerIPAddress, indexerPort, out);
	}

	public void run(){
		crawler.start();
		crawler.runThreads();
	}

	//deliver message for crawler
	public void deliver(Id id, Message message)
	{
		CrawlerMessage m = ((CrawlerMessage)message);
		//System.out.println("crawler   " +this.crawler);
		String type = m.getType();

		if (type.equals("crawl")){
			//////System.out.println(this.endpoint.getId()+": received crawl message for "+m.getURL()+": "+ m);  
			synchronized(out){
			}
			this.crawler.addUrl(m.getURL());
		}
		else if(type.equals("content-seen")&& isBootstrap){
			{
				String result = null;
				try {
					result = this.crawler.contentSeenTest(m.getContent());
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				if (m.getContent()!=null){
					if (result!="")
						sendResponseMessage( m.getFrom(), "content-seen-response", m.getURL(), result);
					else
						sendResponseMessage( m.getFrom(), "content-seen-response",m.getURL(), "False");
				}
			}
		}
		else if(type.equals("url-seen")&& isBootstrap){
			{
				String result = null;
				try {
					result = this.crawler.urlSeenTest(toSha1(m.getURL()));
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				if (result!="")
					sendResponseMessage(m.getFrom(),"url-seen-response",  m.getURL(), result);
				else
					sendResponseMessage( m.getFrom(),"url-seen-response", m.getURL(), "False");
			}
		}
		else if(type.equals("url-seen-response")||type.equals("content-seen-response")){
			String url = m.getURL();
			String response = m.getContent();
			synchronized(resultMap){
				this.resultMap.put(url, response);
				resultMap.notifyAll();
			}
		}
		else{
			System.err.println("Wrong message type : "+type);
		}


	}



	public boolean isResponsibleForURL(String url){
		Id keywordId;
		keywordId = nodeFactory.getIdFromBytes(toSha1(url).getBytes());
		if (keywordId.equals(this.node.getId()))
			return true;
		else
			return false;
	}



	public void sendCrawlMessage(String url) throws IOException
	{
		Id keywordId;
		try {
			keywordId = nodeFactory.getIdFromBytes(toSha1(url).getBytes());
			CrawlerMessage m;
			m = new CrawlerMessage(node.getLocalNodeHandle(), keywordId, "crawl", url, "", false);
			endpoint.route(keywordId, m, null);
			////System.out.println(this.endpoint.getId()+": sent crawl message for "+url+": "+ m);   
			node.getEnvironment().getTimeSource().sleep(1);

		} catch(InterruptedException e)
		{
			e.printStackTrace();
		}

	}

	public void sendContentSeenMessage(String url, String content) throws IOException
	{
		Id keywordId;
		try {
			keywordId = nodeFactory.getIdFromBytes(toSha1(url).getBytes());
			CrawlerMessage m;
			if(content!=null)
				m = new CrawlerMessage(node.getLocalNodeHandle(), keywordId, "content-seen", url, content, true);
			else
				m = new CrawlerMessage(node.getLocalNodeHandle(), keywordId, "content-seen", url, null, false);
			endpoint.route(keywordId, m, null);
			////System.out.println(this.endpoint.getId()+" sent content-seen message for "+url+": "+ m);   
			node.getEnvironment().getTimeSource().sleep(1);

		} catch(InterruptedException e)
		{
			e.printStackTrace();
		}
	}

	public void sendUrlSeenMessage(String url) throws IOException
	{
		Id keywordId;
		try {
			keywordId = nodeFactory.getIdFromBytes(toSha1(url).getBytes());
			CrawlerMessage m;

			m = new CrawlerMessage(node.getLocalNodeHandle(), keywordId, "url-seen", url, "", true);
			endpoint.route(keywordId, m, null);
			////System.out.println(this.endpoint.getId()+" sent url-seen message for "+url+": "+ m);   
			if(!this.isStopped)
				node.getEnvironment().getTimeSource().sleep(1);

		} catch(InterruptedException e)
		{
			e.printStackTrace();
		}
	}

	//no implementation
	public void update(NodeHandle handle, boolean joined) {

	}

	//no implementation
	public boolean forward(RouteMessage routeMessage) { 
		return true; 
	}

	//send response message.
	public void sendResponseMessage(NodeHandle recipient, String type, String url, String messageToSend) {

		CrawlerMessage message = new CrawlerMessage(node.getLocalNodeHandle(), recipient.getId(), type, url, messageToSend, false);
		////System.out.println(this.endpoint.getId()+" sent response for url "+url +" to "+recipient);   
		this.endpoint.route(null, message, recipient);

	}

	private static String convertToHex(byte[] data) {
		StringBuffer buf = new StringBuffer();
		for (int i = 0; i < data.length; i++) {
			int halfbyte = (data[i] >>> 4) & 0x0F;
			int two_halfs = 0;
			do {
				if ((0 <= halfbyte) && (halfbyte <= 9))
					buf.append((char) ('0' + halfbyte));
				else
					buf.append((char) ('a' + (halfbyte - 10)));
				halfbyte = data[i] & 0x0F;
			} while(two_halfs++ < 1);
		}
		return buf.toString();
	}

	public static String toSha1(String text)  
	{
		byte[] sha1hash = new byte[40];
		try{
			MessageDigest md;
			md = MessageDigest.getInstance("SHA-1");

			md.update(text.getBytes("iso-8859-1"), 0, text.length());
			sha1hash = md.digest();
			return convertToHex(sha1hash);
		}
		catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	public void shutdown() throws IOException{

		this.isStopped = true;
		try{

			if(this.crawler != null){
				synchronized (crawler) {
					crawler.shutdown();
					crawler.wait();
					////System.out.println("Crawler Thread "+crawler.getName() + " "+ crawler.getState());
				}
			}

			if (this.node != null) {
				this.nodeFactory.shutdownNode(node);
				node = null;
			}
		}
		catch(InterruptedException e){
			e.printStackTrace();
			System.err.println("Did not shut down appropriately.");
			System.exit(-1);
		}

	}
}

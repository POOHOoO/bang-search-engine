



import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import rice.p2p.commonapi.NodeHandle;

import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;

import java.io.File;

public class CrawlerThread extends Thread{

	public static final String EOL = "\r\n";
	private static int numCrawlers = 0;

	private String startURL;
	private int maxFiles = 50;
	private double maxBytes;

	LinkedList<String> localFrontier;
	private boolean sleeping = true;
	private boolean isStopped = true;


	private String host;
	//private Set<String> disallowedSet;
	private boolean isContinuous;
	private DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd/HH:mm:ss");
	private Pattern disallowPattern = Pattern.compile("\\s*Disallow:\\s([^#]+)", Pattern.CASE_INSENSITIVE);
	private Pattern linkPattern = Pattern.compile("\\s*href\\s*=\\s*\"\\s*([^\">\\s]+)\\s*\"", Pattern.CASE_INSENSITIVE);
	private int numCrawled;

	private FixedSizedQueue<String, String> localEncounteredURL;
	private FixedSizedQueue<String, String> localEncounteredContent;

	private Map<String, Set<String>> robotMap;
	private Crawler controller;

	private static final int numlocalURLQueueSize = 20;

	private static final int numlocalContentQueueSize = 10;

	private boolean doContentSeen = false;
	private String indexerIPAddress;
	private int indexerPort;
	BufferedWriter out;
	private boolean isFirstRun;
	private Vector<CrawlerThread> idleWorkers;

	public synchronized boolean isSleeping(){
		return sleeping;
	}

	public synchronized void setSleeping(boolean tf){
		sleeping = tf;
	}

	public CrawlerThread(Crawler controller, Vector<CrawlerThread> idleWorkers, NodeHandle bootHandle,
			double maxMegabytes, int maxFiles, boolean continuous, String indexerIPAddress, int indexerPort
			, BufferedWriter out
	) throws URISyntaxException, MalformedURLException {
		setSleeping(false);
		this.out = out;
		numCrawled = 0;
		this.setName("CrawlerThread-"+numCrawlers);
		numCrawlers++;
		this.localFrontier = new LinkedList<String>();      

		this.idleWorkers = idleWorkers;
		this.isContinuous = continuous;

		if(maxFiles > 0){
			this.maxFiles = maxFiles;
		}

		this.maxBytes = maxMegabytes*1000000.0;

		this.robotMap = new HashMap<String, Set<String>>();
		this.controller = controller;
		localEncounteredURL = new FixedSizedQueue<String, String>(numlocalURLQueueSize);
		localEncounteredContent = new FixedSizedQueue<String, String>(numlocalContentQueueSize);

		this.indexerIPAddress = indexerIPAddress;
		this.indexerPort = indexerPort;
		isStopped = false;
		this.isFirstRun = true;

	}

	public  synchronized String getHost(){
		return host;
	}

	public  synchronized void setHost(String host){
		this.host = host;
		this.isFirstRun = true;
	}

	public void finish(){
		isStopped = true;
	}

	//add urls to crawl to local frontier. Called by crawler controller.
	public synchronized void addUrlToCrawl(String url) throws URISyntaxException {
		synchronized(localFrontier){
			this.localFrontier.add(url);
		}
		setHost(new URI(url).getHost());
		notify();
		//System.out.println(this.getName()+" addUrlToCrawl");
	}
	//run
	public void run() {
		//System.out.println(this.getName()+": Start");
		String urlString = null;
		//finish hasn't called.
		while (!isStopped) {
			// get the first element from the to be searched list
			urlString = this.dequeueLocalFrontier();
			if(urlString==null){
				synchronized (idleWorkers) {
					idleWorkers.add(this);
				}
				synchronized (this) {
					try {
						System.out.println(this.getName()+": local frontier empty. sleeping\n");
						//out.write(this.getName()+": local frontier empty. sleeping\n");
						wait();
					} catch (InterruptedException e) {
						continue;
					}
				}
				//waken up by controller. should have non-empty local frontier.
				if (!this.localFrontier.isEmpty()){
					urlString = this.dequeueLocalFrontier();
					//System.out.println(this.getName()+": "+urlString);
				}
				else{
					System.err.println(this.getName()+"Terminated or Local frontier still empty error ");
					break;
				}
			}
			URI uri = null;
			try {
				uri = new URI(escapeConvert(urlString));
			} catch (URISyntaxException e2) {
				// TODO Auto-generated catch block
				e2.printStackTrace();
			}
			System.err.println(this.getName()+"URL : "+uri.toString());
			uri = uri.normalize();
			if (this.host!=null){
				if(!getHost().equals(uri.getHost())){
					System.err.println(getName()+" Not a host assigned to this crawler : "+uri.getHost()+" Should be "+ host);
					try {
						out.write(getName()+"Not a host assigned to this crawler : "+uri.getHost()+" Should be "+ host+"\n");
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					continue;
				}
			}
			//If host is null, set a new host for this crawler thread.
			else{
				try {
					out.write(getName()+" Host is null!\n");
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			// can only crawl http
			try {
				if (!uri.toURL().getProtocol().equals("http")){ 
					//raise exception or keep log.
					continue;
				}
			} catch (MalformedURLException e1) {
				e1.printStackTrace();
			}
			if(this.isFirstRun){
				String response = "";
				//Check if this url is already seen. Check local entry first and then.
				if(this.localEncounteredURL.contains(uri.toString())&& !this.isContinuous){
					continue;
				}
				else if (this.localEncounteredURL.contains(uri.toString()))
					response = localEncounteredURL.get(uri.toString());
				else{
					try {
						response = controller.sendURLSeenQuery(uri.toString());
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					if (!response.equals("False") && !this.isContinuous){
						//System.err.println(this.getName()+" seen url " + uri.toString());
						synchronized(out){
						}
						continue;
					}
				}
				if(!doContentSeen){

					Crawler.incrementHits();
					Crawler.incrementNumCrawled();
				}
				System.err.println(this.getName()+"URL seen test passed");
			}
			String response = "False";
			try {
				String content ;
				//just check again b/c it is possible that the content or robot.txt has changed since.
				//only check if it is continous crawling.
				if(this.isFirstRun){
					if (isContinuous&&!isOkayToCrawl(uri.toURL())){
						System.err.println("Is not okay to crawl b/c robot.txt " + uri.toString());
						continue;
					}
					else if (sendHeadRequest(uri.toURL(), response)!=0){
						System.err.println("Is not okay to crawl b/c bad head request response or not a html" + uri.toString());
						continue;
					}
					else{
						content = this.getURLContent(uri.toURL());
						System.err.println(this.getName()+" Done Head Request");
						if (doContentSeen){
							if(this.localEncounteredContent.contains(content)){
								if(!this.isContinuous){
									Crawler.incrementHits();
									//Already crawled before by seen content
									continue;
								}
								else {
									response = localEncounteredContent.get(content);
								}
								controller.sendContentSeenQuery(uri.toString(), null);
							}
							else{
								response = controller.contentSeenTest(content);
								if (!response.equals("False") && !this.isContinuous){
									System.err.println("content seen url " + uri.toString());
									Crawler.incrementHits();
									continue;
								}
								Crawler.incrementHits();
								Crawler.incrementNumCrawled();
							}
							//System.out.println("Content seen test passed");
						}
					}
					isFirstRun = false;
				}
				else
					content = this.getURLContent(uri.toURL());
				//get links
				Vector<String> links;
				//System.out.println("Extract Links");
				links = extractLinks(uri);
				if (links == null){
					//link not found.
					continue;
				}
				else{
					System.err.println(this.getName()+"Extract # links : "+links.size());
					try {
						addLinks(links);
					} catch (URISyntaxException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					if(!doContentSeen){
						Crawler.incrementHits();
						Crawler.incrementNumCrawled();
					}
					//this.sendStoreMessage(uri.toString(), content, links);
					this.sendStoreMessage2(uri.toString(), content, links);
					this.localEncounteredURL.insert(uri.toString(), this.getDateString());
					if (doContentSeen)
						this.localEncounteredContent.insert(content, this.getDateString());
				}

				//Not concerned with continuous crawling as for now.
				//if it is a continous crawling, this url is added back to url frontier.
				if(isContinuous){
					String urlHost = new URI(escapeConvert(urlString)).getHost();
					if (urlHost.equals(getHost()))
						this.enqueueLocalFrontier(urlString);
					else
						controller.sendCrawlMessage(urlString);
				}

			} catch (ParseException e) {
				System.err.println("date string is malformed.");
				e.printStackTrace();
				continue;
			} catch (MalformedURLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (URISyntaxException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			numCrawled++;
			System.err.println(this.getName()+"Done for : "+uri.toString());
		}
		System.err.println("Terminated");
	}





	//add links to local frontier if it has the same host name as the page that contains url. 
	//Otherwise add to global url frontier.
	//ensure only one thread is allowed to contact a particular web server.
	private void addLinks(Vector<String> links) throws URISyntaxException, IOException {
		for(String link: links){
			//if not continous crawling, skip the one already seen.
			//checked in extract stage.
			/*
			if (!isContinuous&&this.isAlreadyEncountered(link))
				continue;
			 */
			URI uri = new URI(escapeConvert(link));
			uri.normalize();
			String urlHost = uri.getHost();
			if (urlHost.equals(getHost())){
				System.out.println(getName()+": Added "+uri.toString()+" to local frontier");
				out.write(getName()+": Added "+uri.toString()+" to local frontier\n");
				this.enqueueLocalFrontier(uri.toString());
			}
			else{
				out.write(getName()+": Sent "+uri.toString()+" crawl request\n");
				this.controller.sendCrawlMessage(uri.toString());
			}
		}
	}

	//send store message to indexer.

	private void sendStoreMessage(String url, String content, Vector<String> links) {

		try{
			InetAddress addr = InetAddress.getByName(this.indexerIPAddress); 
			Socket s = new Socket(addr.getHostName(), this.indexerPort);
			//System.out.println("Connection succeeded on port " + this.indexerPort + ".");


			//formulate QUERY
			String data = "<type>store</type>\n<page>\n";
			data +="<url>"+url+"</url>\n";
			data +="<title>"+this.getTitle(content)+"</title>\n";
			data +="<description>"+this.getMetaDesc(content)+"</description>\n";
			if(links != null){
				data +="<links>\n";
				for(int i = 0; i < links.size(); i ++){
					data +="<link>"+links.get(i)+"</link>\n";
				}
				data += "</links>\n";
			}

			data += "<content>\n" + content + "\n</content>\n</page>\n";

			//System.out.println(url + ": " + data.getBytes().length); 
			//Send header 
			BufferedWriter wr = new BufferedWriter(new OutputStreamWriter(s.getOutputStream())); 
			//wr.write("POST / HTTP/1.0\n"); 
			//wr.write("Content-Length: " + data.getBytes().length+"\n"); 
			//wr.write(EOL); 
			wr.write(data); 
			wr.write(EOL); 
			wr.write(EOL); 
			wr.flush();
			//System.err.println(this.getName()+": sendStoreMessage " + url);
			synchronized(out){
			}
			//Crawler.numCrawled ++;
			//Crawler.hits ++;
		}catch(Exception e){
			e.printStackTrace();
		}
	}

	private void sendStoreMessage2(String url, String content,
			Vector<String> links) {
		// TODO Auto-generated method stub
		try{
			//formulate QUERY
			String data = "<type>store</type>\n<page>\n";
			data +="<url>"+url+"</url>\n";
			data +="<title>"+this.getTitle(content)+"</title>\n";
			data +="<description>"+this.getMetaDesc(content)+"</description>\n";
			if(links != null){
				data +="<links>\n";
				for(int i = 0; i < links.size(); i ++){
					data +="<link>"+links.get(i)+"</link>\n";
				}
				data += "</links>\n";
			}

			data += "<content>\n" + content + "\n</content>\n</page>\n";

			//System.out.println(url + ": " + data.getBytes().length); 
			//Send header 
			BufferedWriter wr = new BufferedWriter(new FileWriter("store_messages", true)); 
			//wr.write("POST / HTTP/1.0\n"); 
			//wr.write("Content-Length: " + data.getBytes().length+"\n"); 
			//wr.write(EOL); 
			wr.write(data); 
			wr.write(EOL); 
			wr.write(EOL); 
			wr.flush();

			System.out.println(this.getName()+": sendStoreMessage " + url);
			synchronized(out){
			}
			//Crawler.numCrawled ++;
			Crawler.incrementNumStored();
		}catch(Exception e){
			e.printStackTrace();
		}
	}


	private String getMetaDesc(String content) {
		// TODO Auto-generated method stub
		return null;
	}

	private String getTitle(String content) {
		// TODO Auto-generated method stub
		return null;
	}

	//Need to refine some logic for freshness and Content-seen
	//send head request to see if it is good url to be crawled.
	//returns -1 if not, 0 if html, 1 if xml
	private int sendHeadRequest(URL url, String crawledTime) throws ParseException{
		//System.out.println("sendHeadRequest");
		try{
			HttpURLConnection urlConnection = (HttpURLConnection)url.openConnection();
			urlConnection.setRequestMethod("HEAD"); 
			try{
				urlConnection.connect();
			}
			catch (IllegalArgumentException e){
				System.err.println("IllegalArgumentException: "+url);
				out.write(getName()+" IllegalArgumentException: "+url);
				e.printStackTrace();
				return -1;
			}
			String type = urlConnection.getContentType();
			int responseCode = urlConnection.getResponseCode();
			if (responseCode!=200){
				System.err.println("bad response"+url.toString());
				out.write(getName()+" bad response"+url.toString());
				return -1;
			}
			if (url.toString().endsWith("robots.txt"))
				return 2;
			//check if it was modified since it was crawled.

			long lastModified = urlConnection.getLastModified();
			if (!crawledTime.equals("False") && !this.isFresh(crawledTime, lastModified)){
				//System.out.println("not changed since "+url.toString());
				out.write(getName()+" not changed since "+url.toString());
				return -1;
			}
			if(urlConnection.getContentLength() > maxBytes){
				out.write(getName()+" too big "+url.toString());
				System.err.println("too big "+url.toString());
				return -1;
			}
			urlConnection.disconnect();
			if (type == null){
				out.write(getName()+" no type "+url.toString());
				System.err.println("no type "+url.toString());
				return -1;
			}
			//type should be text/xml or text/html or application/xml or end with +xml
			if (!type.startsWith("text/xml")&&!type.startsWith("text/plain")&&!type.startsWith("text/html")&&!type.startsWith("application/xml")&&
					!type.endsWith("+xml")) {
				System.err.println("bad type "+url.toString());
				out.write(getName()+" bad type "+url.toString());
				return -1;
			}

			//returns 1 for xml, 2 for plain text, 0 for html
			else if(type.startsWith("text/html"))
				return 0;
			else if(type.startsWith("text/plain"))
				return 2;
			else
				return 1;
		}
		catch(IOException e){
			return -1;
		}
	}

	//get links from given url
	//Each link is converted into an absolute URL and tested against a
	//user-supplied URL filter to determine if it should be downloaded
	//never before seen links
	//for case of 
	private Vector<String> extractLinks(URI uri) throws IOException{

		Vector<String> links = new Vector<String>();
		// read in the entire URL
		String urlContent = getURLContent(uri.toURL());
		//System.out.println(urlContent);
		//search for links (href="URL:" and its subtle variations.)
		Matcher linkMatcher = linkPattern.matcher(urlContent);
		//System.err.println("extractLinks");
		while(linkMatcher.find()){
			//System.err.println("match");
			String linkString = linkMatcher.group(1);
			URI pageURI = uri;
			//URI linkURI = pageURI.resolve(linkString).normalize();
			URL linkURL;
			try {
				if(linkString.startsWith("http"))
					linkURL = new URI(escapeConvert(linkString)).toURL();
				else
					linkURL = (URI.create(pageURI.toString() + "/").resolve(escape(linkString))).normalize().toURL();
				String linkURLString = linkURL.toString();
				System.err.println(this.getName()+" link "+linkURL);
				if (linkURL.getProtocol()==null||!linkURL.getProtocol().equals("http")){
					//System.err.println("Is not HTTP");
					continue;
				}
				//url-seen test for link
				String response;
				//Check if this url is already seen. Check local entry first and then.
				if(this.localEncounteredURL.contains(linkURLString)&& !this.isContinuous){
					continue;
				}
				else if (this.localEncounteredURL.contains(linkURLString)){
					response = localEncounteredURL.get(linkURLString);
				}
				else{
					response = controller.sendURLSeenQuery(linkURLString);
					if (!this.isStopped &&!response.equals("False") && !this.isContinuous){
						//System.err.println("seen url " + linkURLString);
						
						out.write(getName()+" : seen url " + linkURLString+" "+response+"\n");
						continue;
					}
				}
				System.err.println(this.getName()+"Extractor URL seen test passed :");

				boolean notOkay = false;
				//Check if it okay to crawl with robots.txt

				if (!isOkayToCrawl(linkURL)){
					notOkay = true;
				}
				if(notOkay){
					out.write("Is not okay to crawl extractor, robots.txt " + linkURLString+"\n");
					continue;
				}
				System.err.println(this.getName()+"Extractor robot test passed");

				int headResult = this.sendHeadRequest(linkURL, response);
				if(headResult<0){
					/*
					might give some overhead...will see.
					linkURL = (URI.create(pageURI.toString() + "/").resolve(linkString+"/")).normalize().toURL();
					//System.err.println("Checking link: " + linkURL.toString());
					if (!isOkayToCrawl(linkURL)){
						//System.out.println("Is not okay to crawl extractor, robots.txt " + linkURL);
						continue;
					}
					headResult = this.sendHeadRequest(linkURL, response);
					if(headResult<0)
					{
					 */
					out.write("Is not okay to crawl extractor, head request " + linkURL+"\n");
					continue;
					//}
				}
				System.err.println(this.getName()+"Extractor head request passed");
				//we only crawl http

				//add this link url to valid link list.
				//head request is sent only for xml. for html head request is 
				if(headResult==1||headResult==2){
					//this(linkURL);
					String content = this.getURLContent(uri.toURL());
					response = "";
					if (doContentSeen){
						if(this.localEncounteredContent.contains(content)){
							if(!this.isContinuous){
								//Already crawled before seen content
								Crawler.incrementHits();
								continue;
							}
						}
						else{
							response = controller.sendContentSeenQuery(uri.toString(), content);
							if (!response.equals("False") && !this.isContinuous){
								//System.out.println("content seen url " + uri.toString());
								Crawler.incrementHits();
								continue;
							}
							Crawler.incrementHits();
							Crawler.incrementNumCrawled();
						}
						this.localEncounteredContent.insert(content, this.getDateString());
						//System.out.println("Content seen test passed");
					}
					//this.sendStoreMessage(linkURL.toString(), content, null);
					if(!doContentSeen){
						Crawler.incrementHits();
						Crawler.incrementNumCrawled();
					}
					this.sendStoreMessage2(linkURL.toString(), content, null);
				}
				else if(headResult==0){
					linkString = linkURL.toString();
					links.add(linkString);
				}
			}
			catch (MalformedURLException e) {
				continue;
			}
			catch (ParseException e){
				continue;
			} catch (URISyntaxException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		out.write("Links " + links+"\n");
		return links;
	}



	private String escape(String urlString){
		urlString=urlString.replaceAll("\\{", "%7B");	
		urlString=urlString.replaceAll("\\}", "%7D");	
		urlString=urlString.replaceAll("\\|", "%7C");	
		urlString=urlString.replaceAll("\\^", "%5E");	
		urlString=urlString.replaceAll(" ", "%20");
		urlString=urlString.replaceAll("\\%", "%25");
		urlString=urlString.replaceAll("\\`", "%60");
		urlString=urlString.replaceAll("\\>", "%3C");
		urlString=urlString.replaceAll("\\<", "%3E");
		urlString=urlString.replaceAll("\"", "%22");

		return urlString;
	}

	private String escapeConvert(String linkString) {
		String urlString=null;
		try {
			URL tempURL = new URL(linkString);
			urlString = tempURL.getProtocol()+"://"+tempURL.getHost()+tempURL.getPath();
			if(tempURL.getQuery()!=null)
				urlString.concat(tempURL.getQuery());

		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		urlString=urlString.replaceAll("\\{", "%7B");	
		urlString=urlString.replaceAll("\\}", "%7D");	
		urlString=urlString.replaceAll("\\|", "%7C");	
		urlString=urlString.replaceAll("\\^", "%5E");	
		urlString=urlString.replaceAll(" ", "%20");
		urlString=urlString.replaceAll("\\%", "%25");
		urlString=urlString.replaceAll("\\`", "%60");
		urlString=urlString.replaceAll("\\>", "%3C");
		urlString=urlString.replaceAll("\\<", "%3E");
		urlString=urlString.replaceAll("\"", "%22");

		return urlString;
	}

	//returns String content of URL by reading inputstream.
	private String getURLContent(URL url){
		// first, read in the entire URL
		try{
			HttpURLConnection connection = (HttpURLConnection)url.openConnection();
			connection.setRequestMethod("GET");
			connection.setAllowUserInteraction(false);
			connection.connect();
			InputStream in = connection.getInputStream();

			Writer writer = new StringWriter();
			char[] buffer = new char[1024];
			try {
				BufferedReader reader = new BufferedReader(new InputStreamReader(in, "UTF-8"));

				int n;
				while ((n = reader.read(buffer)) != -1) {
					writer.write(buffer, 0, n);
				}
			} finally {
				in.close();
				connection.disconnect();
			}
			return writer.toString().replaceAll("[\\n]", " ");
		}
		catch(ProtocolException e){
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;

	}

	boolean isOkayToCrawl(URL url) throws ParseException {

		String hostname = url.getHost();
		String urlString = url.toString();
		Set<String> disallowedSet;
		if(!isContinuous && this.robotMap.containsKey(hostname)){
			System.err.println("Found robots.txt");
			disallowedSet = robotMap.get(hostname);
		}
		else
		{
			try{
				String robotUrlString = "http://" + hostname + "/robots.txt";
				URL robotUrl;
				try { 
					robotUrl = new URL(robotUrlString);
				} 
				//not a valid url. robots.txt does not exist or unreadable.
				catch (MalformedURLException e) {
					e.printStackTrace();
					return true; 
				}
				//System.out.println(robotUrl);
				//robots.txt is not a text file
				if (this.sendHeadRequest(robotUrl, "")!=2){
					return true;
				}
				//System.out.println("robots.txt found ");
				InputStream in = robotUrl.openStream();
				//User-agent: *
				Pattern useragentPattern = Pattern.compile("\\s*User-agent:\\s([^#]+)", Pattern.CASE_INSENSITIVE);
				boolean isForAll = false;
				boolean foundForMe = false;
				disallowedSet = new HashSet<String>();
				BufferedReader reader = new BufferedReader(
						new InputStreamReader(in, "UTF-8"));
				String line = reader.readLine();
				while(line!=null){
					Matcher disallowMatcher = disallowPattern.matcher(line);
					Matcher useragentMatcher = useragentPattern.matcher(line);
					if (useragentMatcher.find()){
						String agent = useragentMatcher.group(1);
						if(agent.equals("*")&&!foundForMe)
							isForAll = true;
						else if(agent.equals("cis455crawler")){
							isForAll = false;
							foundForMe = true;
							disallowedSet.clear();
						}
						else{
							isForAll = false;
							foundForMe = false;
						}
					}
					if (disallowMatcher.find()&&(isForAll||foundForMe)){
						disallowedSet.add(disallowMatcher.group(1));
					}
					line = reader.readLine();
				}
				if(!isContinuous)
					this.robotMap.put(hostname, disallowedSet);
				in.close();
			}
			catch(IOException e){
				System.err.println("IOException occurred during robot exclusion check");
				return true;
			}
		}
		//System.out.println(disallowedSet);
		//Need to fix this.... Fixed.. Need testing.
		for (String disallowed : disallowedSet){
			String path = urlString.substring(urlString.indexOf(hostname)+hostname.length());
			//System.out.println("Path : "+path);
			if (path.startsWith((disallowed)))
				return false;
		}
		return true; 
	}

	public void enqueueLocalFrontier(String url){
		synchronized(localFrontier){
			localFrontier.add(url);
		}
	}


	public String dequeueLocalFrontier(){
		String urlString = null;
		synchronized (localFrontier) {
			if (!localFrontier.isEmpty()) {
				urlString = (String)localFrontier.removeFirst();
				//System.out.println("dequeued from local frontier: " + urlString);

			}
		}
		return urlString;
	}




	//format of crawled :YYYY-MM-DDThh:mm:ss
	private String getDateString() {
		Date date = new Date();
		return dateFormat.format(date).replace("/", "T");
	}

	private boolean isFresh(String crawledTime, long lastModified) throws ParseException{
		//check if it was modified since it was crawled.
		long lastCrawled=-1;
		lastCrawled = dateFormat.parse(crawledTime.replace('T', '/')).getTime();
		//has not modified since it was last crawled.
		if (lastCrawled>lastModified){
			return false;
		}
		else
			return true;
	}

}
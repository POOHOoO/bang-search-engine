




import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Vector;

import rice.p2p.commonapi.NodeHandle;

//Crawler running on each node.
public class Crawler extends Thread{


	private static int numStoreSent;
	private static int numCrawled;
	private static int hits;
	private DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd/HH:mm:ss");
	Boolean threaded = false;
	private int numThreads = 20;
	private LinkedList<String> URLFrontier;
	private FixedSizedQueue<String, String> recentSeenURL;
	private FixedSizedQueue<String, String> recentSeenContent;
	private Vector<CrawlerThread> crawlerThreads;
	private BDBEnv myDbEnv;
	private Boolean stopCrawler;
	private boolean isBoot;
	private static final int maxRecentURL = 100;
	private static final int maxRecentContent = 100;
	private DataAccessor da;
	private CrawlerPastryApp app;

	private BufferedWriter out;
	private boolean doContentSeen = false;
	private int maxFiles;
	private Vector<CrawlerThread> idleWorkers;

	//For bootstrapping crawler. Need to set up 
	public Crawler(CrawlerPastryApp app, 
			NodeHandle bootHandle, String url, BDBEnv dbEnv, 
			double maxMegabytes, int maxFiles, boolean threaded, boolean continuous, 
			String indexerIPAddress, int indexerPort, BufferedWriter out)
	throws URISyntaxException, MalformedURLException {
		this(app, bootHandle, maxMegabytes, maxFiles, threaded, continuous, indexerIPAddress, indexerPort, out);
		isBoot = true;
		myDbEnv = dbEnv;
		da = new DataAccessor(myDbEnv.getEntityStore());
		addUrl(url);
	}

	public Crawler(CrawlerPastryApp app,NodeHandle bootHandle, 
			double maxMegabytes, int maxFiles, boolean threaded, boolean continuous, 
			String indexerIPAddress, int indexerPort, BufferedWriter out)
	throws URISyntaxException, MalformedURLException {

		isBoot = false;
		setup(app, threaded, out, maxFiles);

		crawlerThreads = new Vector<CrawlerThread>();
		for (int i= 0; i< numThreads;i++){
			//
			CrawlerThread crawler = new CrawlerThread(this,  idleWorkers, bootHandle, 
					maxMegabytes, maxFiles, continuous, indexerIPAddress, indexerPort, out);
			crawlerThreads.add(crawler);
		}
	}

	private void setup(CrawlerPastryApp app2, boolean threaded2, BufferedWriter out2, int maxFiles) {

		this.app = app2;
		// TODO Auto-generated method stub
		setName("mainCrawlerThread");
		this.out = out2;
		stopCrawler = new Boolean(false);
		if (!threaded2)
			this.numThreads = 1;
		URLFrontier = new LinkedList<String>();
		threaded = true;
		//data accessor for content seen test and url seen test.
		//what should be the format of this??
		this.recentSeenContent = new FixedSizedQueue<String,String>(maxRecentContent); 
		//url, datestring pair
		this.recentSeenURL = new FixedSizedQueue<String, String>(maxRecentURL);
		hits = 0;
		numCrawled = 0;
		this.maxFiles = maxFiles;
		idleWorkers = new Vector<CrawlerThread>();
		numStoreSent = 0;

	}

	public void runThreads(){
		if (threaded){
			for (int i= 0; i< numThreads;i++){
				crawlerThreads.get(i).start();
			}
		}
	}

	//run
	public void run() {

		System.out.println(this.getName()+": Start");
		try {
			out.write(this.getName()+": Start\n");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		while(!isStopped()){
			//implement s.t.
			//System.out.println(this.getName()+": running");
			try {
				out.write(this.getName()+": running");
			} catch (IOException e3) {
				// TODO Auto-generated catch block
				e3.printStackTrace();
			}
			if(numStoreSent > this.maxFiles){
				try {
					shutdown();
				} catch (InterruptedException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}
			synchronized(URLFrontier){
				try {
					if(URLFrontier.size()>0)
						out.write("URLFrontier = " + URLFrontier+"\n");
				} catch (IOException e2) {
					// TODO Auto-generated catch block
					e2.printStackTrace();
				}
				if(URLFrontier.size()==0){
					try {
						System.out.println(this.getName()+": global frontier empty. sleeping\n");
						//out.write(this.getName()+": local frontier empty. sleeping\n");
						URLFrontier.wait();
					} catch (InterruptedException e) {
						continue;
					}
				}
				Vector<String> temp = new Vector<String>();
				while (!URLFrontier.isEmpty()){
					String url = URLFrontier.removeLast();
					String host = null;
					try {
						host = new URI(url).getHost();
					} catch (URISyntaxException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					boolean assigned = false;
					for (int i =0 ; i<numThreads;i++){
						String crawlerHost = crawlerThreads.get(i).getHost();
						try {
							out.write(crawlerThreads.get(i).getName()+" = "+crawlerHost+"\n");
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						if(crawlerHost!=null&& host.equals(crawlerHost)){
							try {
								crawlerThreads.get(i).addUrlToCrawl(url);
								assigned = true;
							} catch (URISyntaxException e1) {
								// TODO Auto-generated catch block
								e1.printStackTrace();
							}
							System.out.println(this.getName()+": assigned "+url+" to "+crawlerThreads.get(i).getName()+" b/c of the same hostname");
							try {
								out.write(this.getName()+": assigned "+url+" to "+crawlerThreads.get(i).getName()+" b/c of the same hostname\n");
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							break;
						}
					}
					if(!assigned)
						temp.add(url);
				}
				URLFrontier.addAll(temp);
			}

			synchronized (idleWorkers) {
				while (!URLFrontier.isEmpty() && !idleWorkers.isEmpty()) {
					// poll workers from the worker's queue and assign a
					// socket to process
					String url = URLFrontier.remove(0);
					System.out.println("Dequeued from URLFrontier : "+url);
					try {
						CrawlerThread thread = idleWorkers.remove(0);
						thread.addUrlToCrawl(url);
						System.out.println(this.getName()+": assigned "+url+" to "+thread.getName()+" as a new hostname");
						out.write(this.getName()+": assigned "+url+" to "+thread.getName()+" as a new hostname\n");
						thread.setHost(new URL(url).getHost());
					} catch (URISyntaxException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}

		}

	}

	private boolean isStopped() {
		synchronized(this.stopCrawler){
			return stopCrawler.booleanValue();
		}
	}

	private synchronized void setStopped(boolean stop){
		synchronized(this.stopCrawler){
			stopCrawler = stop;
		}
	}

	//called by crawler app. Add url to URL Frontier.
	public void addUrl(String url) {
		synchronized(URLFrontier){
			//System.out.println(this.getName()+": got new url: "+url+" in URL frontier\n");
			try {
				out.write(this.getName()+": got new url: "+url+" in URL frontier\n");
				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			URLFrontier.add(url);
			if (URLFrontier.size()==1){
				URLFrontier.notify();
			}
		}

	}

	//need to be fixed.
	public void shutdown() throws InterruptedException{
		setStopped(true);
		if(isBoot){
			System.out.println("Hits:"+hits);
			System.out.println("# Crawled :"+numCrawled);
			System.out.println("# Total Paged Crawled so far :"+da.getTotalURLs());
			System.out.println("# Total Store so far :"+ numStoreSent);
		}

		myDbEnv.getEntityStore().sync();
		myDbEnv.getEnv().sync();
		myDbEnv.close();
		for (CrawlerThread crawler : crawlerThreads){
			if (crawler != null) {
				synchronized (crawler) {
					System.out.println(crawler.getName()+"'s Host : "+crawler.getHost());
					System.out.println(crawler.getName()+"'s Frontier : "+crawler.localFrontier);
					crawler.finish();
					crawler.notify();
					crawler.interrupt();
				}
			}
		}
		System.out.println(URLFrontier);
		for (CrawlerThread crawler : crawlerThreads){
			//System.out.println(crawler.getName() + " "+ crawler.getState());
		}
		//System.out.println("Shutted Down Successfully.");

	}



	//no partial matching for now.
	private String calculateCheckSum(String content) {
		return CrawlerPastryApp.toSha1(content);
	}

	public String sendURLSeenQuery(String url) throws IOException{
		String result = null;
		////System.out.println(url+" "+app);
		app.sendUrlSeenMessage(url);
		try{

			boolean resultArrived = false;
			while(!resultArrived){
				synchronized(app.resultMap)
				{
					if(app.resultMap.containsKey(url)){
						resultArrived = true;
						result = app.resultMap.get(url);
					}
					if (!resultArrived)
						app.resultMap.wait();
				}

			}
			app.resultMap.remove(url);
		}
		catch(InterruptedException e){
			if(!isStopped())
				e.printStackTrace();
		}
		out.write("URL-seen result for "+url+" is "+result+"\n");
		return result;
	}

	public String sendContentSeenQuery(String url, String content) throws IOException{
		String result = null;
		app.sendContentSeenMessage(url, content);
		try{

			boolean resultArrived = false;
			while(!resultArrived){
				synchronized(app.resultMap)
				{
					if(app.resultMap.containsKey(url)){
						resultArrived = true;
						result = app.resultMap.get(url);
					}
					if (!resultArrived)
						app.resultMap.wait();
				}

			}
			app.resultMap.remove(url);
		}
		catch(InterruptedException e){
			e.printStackTrace();
		}
		return result;
	}

	//perform a url seen test!
	public String urlSeenTest(String sha1) throws IOException {
		if (!isBoot){
			System.err.println("Message routed to a non-boot node");
			return "";
		}
		else{
			if(this.recentSeenURL.contains(sha1))	{
				return recentSeenURL.get(sha1);
			}

			else
			{
				if(this.isStopped())
					return "";
				//search for DB entry
				URLEntity entity = da.getbyURL(sha1);
				if (entity!=null){
					out.write("A seen url "+sha1+"\n");
					return entity.getLastChecked();
				}
				else{
					out.write("Not a seen url "+sha1+"\n");
					recentSeenURL.insert(sha1, getDateString());
					da.addUrlEntity(sha1, getDateString());
					if (!this.doContentSeen){
						//hits++;
						//numCrawled++;
					}
					//if no content seen test, # hits should be incremented here.
					myDbEnv.getEntityStore().sync();
					myDbEnv.getEnv().sync();
					return "";
				}

			}
		}

	}

	//perform a content seen test!
	// or just used for incrementing hits for no-content seen case or those hit by local content cache.
	public String contentSeenTest(String content) throws IOException {
		if (!isBoot){
			System.err.println("Message routed to a non-boot node");
			return "";
		}
		if (content!=null){
			String checksum = calculateCheckSum(content);

			if(this.recentSeenContent.contains(content)){
				hits++;
				return recentSeenContent.get(content);
			}
			else
			{
				if(this.isStopped())
					return "";
				//search for DB entry
				ContentEntity entity = da.getbyContent(content);
				if (entity!=null){
					hits++;
					return entity.getLastChecked();
				}
				else{	
					recentSeenURL.insert(content, getDateString());
					da.addContentEntity(content, getDateString());
					//numCrawled++;
					//hits++;
					myDbEnv.getEntityStore().sync();
					myDbEnv.getEnv().sync();
					return "";
				}

			}

		}
		else{
			recentSeenURL.insert(content, getDateString());
			da.addContentEntity(content, getDateString());
			//if no content seen test, # hits should be incremented here.
			return "";
		}

	}

	public void sendCrawlMessage(String urlString) throws IOException {

		if(app.isResponsibleForURL(urlString)){
			this.addUrl(urlString);
			System.out.println(this.getName()+": Responsible. didn't sent crawl message :"+urlString);
			out.write(this.getName()+": Responsible. didn't sent crawl message :"+urlString+"\n");
		}
		else{
			System.out.println(this.getName()+": sent crawl message :"+urlString);
			out.write(this.getName()+": sent crawl message :"+urlString+"\n");
			this.app.sendCrawlMessage(urlString);
		}
	}

	//format of crawled :YYYY-MM-DDThh:mm:ss
	private String getDateString() {
		Date date = new Date();
		return dateFormat.format(date).replace("/", "T");
	}

	public synchronized static void incrementHits() {
		hits++;
	}

	public static void incrementNumCrawled() {
		numCrawled++;
	}

	public static void incrementNumStored() {
		numStoreSent++;
	}





}
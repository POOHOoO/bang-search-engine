





import java.util.Enumeration;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.File;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URISyntaxException;
import java.net.UnknownHostException;


public class CrawlerMain extends Thread{

	public boolean isbootstrapNode;
	//No virtual nodes here
	//public int numVirtualNodes;
	public String bootstrapIPAddress;
	public int bootstrapPort;

	public static CrawlerPastryApp crawlerPastryApp;

	public static DataAccessor da;
	//for storage. Only needed for bootstrapping node
	public static BDBEnv myDBEnv = null;
	
	private static boolean stopServer;

	/*
	 * Number of virtual nodes to start on this machine.
	 * The Pastry bootstrap node's IP address (may be 127.0.0.1).
	 * The port to use for the Pastry bootstrap node (to allow co-existence with your peers' applications).
If you need to allocate more than one port on a given machine, assign additional ports in
sequential ascending order, starting from the one given on the command line, followed by any
arguments specific to your server.
	 */

	public CrawlerMain(String bootstrapIPAddress, int bootstrapPort, boolean isBoot, 
			String url, String dbEnvDirValue, double maxMegabytes, int maxFiles, 
			boolean threaded, boolean continuous, String indexerIPAddress, int indexerPort, String log) throws IOException{

		stopServer = false;

		isbootstrapNode = isBoot;
		this.bootstrapIPAddress = bootstrapIPAddress;
		this.bootstrapPort = bootstrapPort;

		FileWriter fstream = new FileWriter(log);
        BufferedWriter out = new BufferedWriter(fstream);
        out.write("Crawler Main started\n");

		//For bootstrapping node for content seen and url seen test.
		//setup DB
		InetAddress ipaddress;
		// some bootstrap node setup
		try{
			if (isbootstrapNode) 
			{
				myDBEnv = new BDBEnv();
				if (dbEnvDirValue!=null)
					myDBEnv.setup(new File(dbEnvDirValue), false);
				else
					myDBEnv.setup(new File("./JEDB"), false);
				ipaddress = InetAddress.getByName(this.bootstrapIPAddress);
				Enumeration<NetworkInterface> interEnum = NetworkInterface.getNetworkInterfaces();
				while(interEnum.hasMoreElements()){
					NetworkInterface inter = interEnum.nextElement();
					if (inter.getName().startsWith("eth")){
						Enumeration<InetAddress> inetEnum = inter.getInetAddresses();
						while(inetEnum.hasMoreElements()){
							String address = inetEnum.nextElement().getHostAddress();
							if (!address.contains(":"))
								ipaddress = InetAddress.getByName(address);
						}
					}


				}
			}

			else
			{
				ipaddress = InetAddress.getByName(this.bootstrapIPAddress);
			}

			InetSocketAddress socketaddress = new InetSocketAddress(ipaddress, bootstrapPort);

			NodeFactory nodeFactory = new NodeFactory(bootstrapPort, socketaddress);
			//how to differentiate bootstrapping node and others?

			if(isbootstrapNode){
				crawlerPastryApp = new CrawlerPastryApp(nodeFactory, url, myDBEnv,  true,
						ipaddress , maxMegabytes,  maxFiles,  threaded ,continuous, indexerIPAddress, indexerPort, out);
			}
			else
				crawlerPastryApp = new CrawlerPastryApp(nodeFactory, url, myDBEnv, false, 
						null , maxMegabytes,  maxFiles,  threaded, continuous, indexerIPAddress, indexerPort, out);
		}
		catch(UnknownHostException e){

			System.err.println("Unknown bootstrap node.");
			System.exit(2);
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void run() {

		crawlerPastryApp.run();

		int choice;
		System.out.println("Yeo Ho Yoon, yeyoon");
		System.out.println("\nCrawler running.");
		while (!stopServer) {
			System.out
			.println("\n--------------------------------------------------------");
			System.out.println(" Interactive Menu ");
			System.out
			.println("--------------------------------------------------------");
			System.out.println("1. Show the Bootstrap IP");
			System.out.println("2. Show the Bootstrap Port Number");
			System.out.println("3. Shutdown the Crawler");
			System.out
			.println("--------------------------------------------------------");
			System.out.println("INPUT A NUMBER 1~3");
			System.out
			.println("--------------------------------------------------------\n");
			BufferedReader dataIn = new BufferedReader(new InputStreamReader(
					System.in));
			try {
				choice = Integer.parseInt(dataIn.readLine());
				if (choice != 4 && choice != 1 && choice != 2 && choice != 3)
					System.out
					.println("Wrong Choice of number. Input 1~3.");
				else if (choice == 1)
					showBootIP();
				else if (choice == 2)
					showBootPort();
				else if (choice == 3){
					try {
						shutdown();
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				
			} catch (IOException e) {
				System.err.println("Unexpected I/O Error!");
			} catch (NumberFormatException e) {
				System.err.println("Your Choice should be an integer 1~3");
			}
		}

		System.out.println("Shut Down Complete");
		System.exit(0);
	}

	private void shutdown() {
		try{
			System.out.println("Shutting Down the crawler application....");
			stopServer = true;

			if(crawlerPastryApp != null){
				synchronized (crawlerPastryApp) {
					crawlerPastryApp.shutdown();
				}
			}
		}
		catch(IOException e){
			e.printStackTrace();
			System.err.println("Did not shut down appropriately.");
			System.exit(-1);
		}

	}




	private void showBootIP() {
		System.out.println("Bootstrapping Node IP Address : " + this.bootstrapIPAddress);

	}

	private void showBootPort() {
		System.out.println("Bootstrapping Node Port : " + this.bootstrapPort);

	}

	public static void printUsage(){
		System.err.println("Usage : [options] [bootstrap node IP] [bootstrap port] [maxMegabytes] [maxFiles] [indexer IP] [indexer port] [log]");
	}
				  		


	public static void main(String[] args) throws NumberFormatException, IOException
	{

		CmdLineParser parser = new CmdLineParser();
		CmdLineParser.Option bootstrap = parser.addBooleanOption('b', "bootstrapping");
		CmdLineParser.Option dbEnvDir = parser.addStringOption('e', "dbenvdir");
		CmdLineParser.Option url = parser.addStringOption('u', "url");
		CmdLineParser.Option threaded = parser.addBooleanOption('t', "threaded");
		CmdLineParser.Option continuous = parser.addBooleanOption('c', "continuous");



		try {
			parser.parse(args);
		} catch (CmdLineParser.OptionException e) {
			System.err.println(e.getMessage());
			printUsage();
			System.exit(2);
		}


		Boolean bootstripValue = (Boolean) parser.getOptionValue(bootstrap);
		String dbEnvDirValue = (String)parser.getOptionValue(dbEnvDir);
		String urlValue = (String)parser.getOptionValue(url);
		Boolean threadedValue = (Boolean) parser.getOptionValue(threaded);
		Boolean continuousValue = (Boolean) parser.getOptionValue(continuous);
		

		String[] otherArgs = parser.getRemainingArgs();

		if (otherArgs == null || otherArgs.length !=7) {
			System.err.println("Incorrect Number of Arguments."+otherArgs.length);
			printUsage();
			System.exit(2);
		}
		
		if (threadedValue==null){
			threadedValue = false;
		}
		
		if (continuousValue==null){
			continuousValue = false;
		}
		
		if (bootstripValue==null){
			bootstripValue = false;
		}

		if (urlValue == null && !bootstripValue){
			System.err.println("Bootstrap must have url to start crawl");
			printUsage();
			System.exit(2);
		}
					
		int port=0;
		try {
			port = Integer.parseInt(otherArgs[1]);
		} catch (NumberFormatException x) {
			System.err.println("Could not parse port number.");
			System.exit(2);
		}
		if (port < 0 || port > 65535) {
			System.err.println("Invalid Port Number");
			System.exit(2);
		}

		if(dbEnvDirValue==null)
			dbEnvDirValue = "./JEDB";

		File root = new File(dbEnvDirValue);
		if ((root == null) || (!root.exists()) || (!root.isDirectory())) {
			System.err
			.println("specified dbEnv directory is null or does not exist or is not a directory");
			System.exit(2);
		}

		/*
		 * 		CmdLineParser.Option bootstrap = parser.addBooleanOption('b', "bootstrapping");
		CmdLineParser.Option daemonPort = parser.addIntegerOption('d', "daemonport");
		CmdLineParser.Option dbEnvDir = parser.addStringOption('e', "dbenvdir");
		CmdLineParser.Option url = parser.addStringOption('u', "url");
		CmdLineParser.Option threaded = parser.addBooleanOption('t', "threaded");
		CmdLineParser.Option continuous = parser.addBooleanOption('c', "continuous");
		 * 		Boolean bootstripValue = (Boolean) parser.getOptionValue(bootstrap);
		Integer daemonPortValue = (Integer) parser.getOptionValue(daemonPort);
		String dbEnvDirValue = (String)parser.getOptionValue(dbEnvDir);
		String urlValue = (String)parser.getOptionValue(url);
		Boolean threadedValue = (Boolean) parser.getOptionValue(threaded);
		Boolean continuousValue = (Boolean) parser.getOptionValue(continuous);
		 * System.err.println("Usage : [options] [bootstrap node IP] [bootstrap port] [maxMegabytes]
		 *  [maxFiles] [indexer IP] [indexer port] [log]");
		 * public CrawlerMain(String bootstrapIPAddress, int bootstrapPort, boolean isBoot, 
			String url, String dbEnvDirValue, double maxMegabytes, int maxFiles, 
			boolean threaded, boolean continuous, String indexerIPAddress, int indexerPort){

		 */
		CrawlerMain crawlerMain = new CrawlerMain(otherArgs[0], port, 
				bootstripValue.booleanValue(), urlValue, dbEnvDirValue, Double.parseDouble(otherArgs[2]),
				Integer.parseInt(otherArgs[3]), threadedValue, continuousValue, otherArgs[4], Integer.parseInt(otherArgs[5]), otherArgs[6]);
		
		crawlerMain.start();

	}
}

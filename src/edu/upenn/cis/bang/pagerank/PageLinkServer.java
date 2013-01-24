package edu.upenn.cis.bang.pagerank;

import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class PageLinkServer {

	// TheadPool
	int corePoolSize = 150;
	int maxPoolSize = 500;
	long keepAliveTime = 100;
	ThreadPoolExecutor threadPool = null;
	final LinkedBlockingQueue<Runnable> queue = new LinkedBlockingQueue<Runnable>();

	// server
	int port;
	int shutdown;
	
	String dir;
	BDBstore db;
	String outputFilePath;

	public PageLinkServer(String dir){
		this(dir,0);
	}

	public PageLinkServer(String dir, int port){
		threadPool = new ThreadPoolExecutor(corePoolSize, maxPoolSize, keepAliveTime, TimeUnit.SECONDS, queue);
		this.port = port;
		shutdown = 0;
		
		this.dir = dir;
		db = new BDBstore(this.dir);
		outputFilePath = "/root/pagelinks.txt";
		
		serveConnections();
	}
	
	public BDBstore getDB(){
		return db;
	}

	void serveConnections() {
		ServerSocket listenerSocket = null;
		try{
			//creates sockets
			listenerSocket = new ServerSocket(port);

			PageLinkListener clusterListener = new PageLinkListener(listenerSocket, this);
			threadPool.execute(clusterListener);

			while(shutdown != 1)
			{
				BufferedReader stdin;
				stdin = new BufferedReader (new InputStreamReader (System.in));
				String cmd = stdin.readLine();
				//System.out.println("From Server::CMD input:"+ cmd);
				if(cmd.equalsIgnoreCase("shutdown")){
					shutdown = 1;
				}
				else if (cmd.equalsIgnoreCase("stat")){
					System.out.println("Printing Server stats:");
					System.out.println("port="+port);
					System.out.println("dir="+dir);
					System.out.println("outputPath="+outputFilePath);
				}
				else if(cmd.startsWith("set")){
					String[] split = cmd.split(" ");
					outputFilePath = split[1];
					System.out.println("outputPath set to "+outputFilePath);
				}
				else if(cmd.equalsIgnoreCase("size")){
					Set<Entry<String, LinkedList<String>>> entrySet = db.getAllPageLinks().entrySet();
					int size = entrySet.size();
					System.out.println("Total number of urls = "+size);
				}
				else if(cmd.equalsIgnoreCase("links")){
					Map<String, LinkedList<String>> allPageLinks = db.getAllPageLinks();
					System.err.println("PRINTING ALL PAGELINKS");
					for (Map.Entry<String, LinkedList<String>> entry: allPageLinks.entrySet()){
						String key = entry.getKey();
						LinkedList<String> values = entry.getValue();
						
						System.out.print(key+" >> ");
						for (String str : values){
							System.out.print(str+" ");
						}
						System.out.print("\n");
						
					}
				}
				// write this to a file
				else if (cmd.equalsIgnoreCase("write")){
					Map<String, LinkedList<String>> allPageLinks = db.getAllPageLinks();
					System.err.println("WRITING ALL PAGELINKS TO "+outputFilePath);
					PrintWriter out = null;
					try {
						FileWriter fstream = new FileWriter(outputFilePath);
						out = new PrintWriter(fstream);
						
						Set<Entry<String, LinkedList<String>>> entrySet = allPageLinks.entrySet();
						int size = entrySet.size();
						
						//out.println(size);
						
						for (Map.Entry<String, LinkedList<String>> entry: entrySet){
							String key = entry.getKey();
							LinkedList<String> values = entry.getValue();

							if (key != null && !key.equals("")){
								StringBuffer sb = new StringBuffer(key + " 1.0 ");
								for (String str : values){
									sb.append(str+" ");
								}

								out.println(sb.toString().trim());
							}
						}
					} catch (IOException e){
						e.printStackTrace();
					} finally {
						System.out.println("COMPLETED WRITING PAGELINKS");
						if (out != null) {out.close();}
					}
				}
				else {
					System.out.println("Unknown command");
				}
			}
		} catch (IOException e){
			e.printStackTrace();
		} finally {
			if (listenerSocket != null){
				try { listenerSocket.close(); } catch (IOException e) {}
			}
			// close db
			db.close();
			threadPool.shutdownNow();
		}
	}
	
	static void printUsage(){
		System.out.println("Usage: <directory> [port]");
	}
	
	public static void main(String[] args){
		if (args.length < 1 || args.length > 2){
			printUsage();
			System.exit(0);
		}
		int port = 0;
		if (args.length > 1){
			port = Integer.parseInt(args[1]);
		}
		new PageLinkServer(args[0],port);
	}

}

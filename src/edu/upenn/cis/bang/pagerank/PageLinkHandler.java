package edu.upenn.cis.bang.pagerank;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.LinkedList;
import java.util.List;

public class PageLinkHandler implements Runnable {

	private Socket client = null;
	private long lastClientAction;

	private String id = "";
	private final List<Socket> activeConnections;

	final PageLinkServer server;

	public PageLinkHandler(PageLinkServer server, Socket client, int i, List<Socket> activeConnections){
		this.client = client;
		this.id = String.valueOf(i);
		this.activeConnections = activeConnections;

		this.server = server;
		lastClientAction = System.currentTimeMillis();
	}

	public long getLastClientAction(){
		return lastClientAction;
	}

	public Socket getClient(){
		return this.client;
	}

	public void run() {

		String line = null;
		System.out.println("PageLinkHandler "+ id +" ::New PageLinkHandler thread started.");

		try {


			BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
			PrintWriter out = new PrintWriter(client.getOutputStream(),true);
			//ObjectInputStream in = new ObjectInputStream(client.getInputStream());
			//ObjectOutputStream out = new ObjectOutputStream(client.getOutputStream());

			while(true){
				line = in.readLine();
				lastClientAction = System.currentTimeMillis();
				
				if(line == null){
					//System.out.println("line is null ---------");
					break;
				}
				if (line.toUpperCase().startsWith("INSERT")) {
					doInsert(in, out);
					System.out.println("done with insert");
					out.println("+Insert confirmed");
					out.flush();
					
					break;
				}

				else
				{
					System.err.println("Unknown message received!");
				}
			}


		} catch (IOException e) {
			//System.out.println("ClusterHandler "+id+"::Socket Closed");
			//e.printStackTrace();
		} finally {
			endConnection();
		}

		System.out.println("PageLinkHandler "+ id +"::Connection closed");

	}//End of run()    

	public void doInsert(BufferedReader in, PrintWriter out) throws IOException{
		String line;
		while( ! (line = in.readLine()).equals(".") ){
	
			final String[] split = line.split(" ");
			final String url = split[0];
			final LinkedList<String> outlinks = new LinkedList<String>();
			
			for (int i = 1; i < split.length; i++){
				outlinks.add(split[i]);
			}
			
			insertToDb(url, outlinks);
		}
	}
	
	public void insertToDb(String url, LinkedList<String> outlinks){
		server.getDB().putPageLinks(url, outlinks);
	}

	public void endConnection(){
		// NOTE: Must remove client from active connections
		activeConnections.remove(client);
		try {
			client.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}

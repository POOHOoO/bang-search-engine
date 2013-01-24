package edu.upenn.cis.bang.pagerank;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.Vector;

public class PageLinkListener implements Runnable {

	private final PageLinkServer server;
	private final ServerSocket socket;

	private final List<Socket> activeConnections;

	public PageLinkListener(ServerSocket serversocket, PageLinkServer server){
		this.server = server;
		this.socket = serversocket;
		activeConnections = new Vector<Socket>();
	}

	@Override
	public void run() {
		Socket client = null;
		int threadcount = 0;

		System.out.println("PageLinkListener is listening on port: "+ socket.getLocalPort());
		
		while(server.shutdown != 1)
		{
			try {
				client = socket.accept();
				activeConnections.add(client);
			} catch (IOException e) {
				cleanup();
				System.out.println("PageLinkListener interrupted.");		
				break;
			}
			PageLinkHandler clusterHandler = new PageLinkHandler(server,client,threadcount,activeConnections);
			threadcount++;
			server.threadPool.execute(clusterHandler);
		}

		System.out.println("PageLink listener exiting...");
	}
	
	
	void cleanup(){
		for (Socket s : activeConnections){
			try {
				s.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

}

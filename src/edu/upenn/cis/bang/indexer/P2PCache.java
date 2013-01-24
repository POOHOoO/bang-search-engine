package edu.upenn.cis.bang.indexer;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedList;
import java.util.List;


public class P2PCache {

	private static final int POOLSIZE = 1000;
	static int no_node = 0;
	static InetAddress bootaddr = null;
	static int bootport = 0;
	static int listenport = 0;
	static InetSocketAddress bootsocketaddress = null;
	static NodeFactory nodefactory = null;
	static List<App> lapp;
	
	/// for static database
	static String databaseaddr = null;
	static int databaseport = 0;
	
	/// variable if it is a boot node
	static Workerthread[] threadpool;
	static LinkedList<Socket> workqueue;
	
	public static void main(String[] args){
		
		try{
			no_node = Integer.parseInt(args[0]);
			if (no_node <2){
				System.out.println("Please enter at least 1 node");
			}
			
			bootaddr = InetAddress.getByName(args[1]);
			bootport = Integer.parseInt(args[2]);
			listenport = Integer.parseInt(args[3]);
			databaseaddr = args[4];
			databaseport = Integer.parseInt(args[5]);
			
		}
		catch (Exception e){
			System.out.println("Error in the arguments ");
			e.printStackTrace();
		}
		
		bootsocketaddress = new InetSocketAddress(bootaddr, bootport);	
		System.out.println("Bootsocket address: " + bootsocketaddress.toString());
		System.out.println("no of node: " + no_node);
		System.out.println("*******************");
		createFactory();
		
		
		lapp = new LinkedList<App>();
		for (int i=0; i< no_node; i++){
			lapp.add(createnewapplication());
		}
		lapp.get(0).setIsbootnode(true);
		
		
		CreateThreadPool();
		
	}
	
	
	
	public static App createnewapplication(){
		return new App(nodefactory, threadpool, databaseaddr, databaseport);
	}
	
	public static void createFactory(){
		
		InetAddress localhost = null;
		try{
			localhost = InetAddress.getLocalHost();
		}
		catch(Exception e){
			e.printStackTrace();
		}
		
		/// TODO: for later check if it is the bootstrap
		InetSocketAddress localaddress = new InetSocketAddress(localhost, bootport);	
		System.out.println("localaddress is "+localaddress.getAddress());
		
		
		nodefactory = new NodeFactory(bootport, bootsocketaddress);
		threadpool = new Workerthread[POOLSIZE];
		workqueue = new LinkedList<Socket>();
		
	}
	

	public static void CreateThreadPool() {
		for (int i = 0; i < POOLSIZE; i++) {
			threadpool[i] = new Workerthread(nodefactory,lapp, i);
			threadpool[i].start();
		}
		
		new Dispatcherthread(workqueue, threadpool).start();
		Runnable r = new listenthread();
		Thread t = new Thread(r);
		t.start();

	}
	
	
	static class listenthread implements Runnable{
		public void run() {
			try {
				ServerSocket server = new ServerSocket(listenport);
				System.out.println("Listening on Port " + listenport);
				while (true) {
					Socket connection = server.accept();
					System.out.println("accept connection");
					workqueue.add(connection);
				}
			} catch (Exception e) {
				System.out.println("Problem connecting to socket " + e);
			}
			
		}
		
	}
	
}

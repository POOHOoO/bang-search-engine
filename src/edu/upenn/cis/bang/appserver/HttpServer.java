package edu.upenn.cis.bang.appserver;

import java.net.Socket;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Queue;
import java.util.Scanner;
import java.util.Vector;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

/**
 * 
 * @author Zach Zarrow (zzarrow)
 * 
 * Main class for the HTTP server (HW1 of CIS-455).
 *
 */
public class HttpServer extends Thread{
	
	static Queue<Socket> threadPool;
	static Vector<Worker> workers;
	static Dispatcher dispatch;
	
	private static void setupServlets() {
		//Create context
		Config.context = new HttpServletContext(Config.handler.m_contextParams.get("display-name"));
		Iterator<String> it = Config.contextParamMap.keySet().iterator();
		while(it.hasNext()){
			String curr = it.next();
			Config.context.setParm(curr, Config.contextParamMap.get(curr));
		}
		
		//Config.context.setAttribute("session-timeout", Config.handler.m_session_timeout);
        //Config.context.setAttribute("ServletURLs", Config.handler.m_servletURLs);
        //Config.context.setAttribute("ServletContext", Config.handler.m_display_name);
	
		//Create servlets
		Config.nameToServlet = new HashMap<String,HttpServlet>();
		for(String s : Config.servletMap.keySet()){
			HttpServletConfig cfg = new HttpServletConfig(s);
			//instantiate instance
            Class servletObject = null;
			try {
				servletObject = Class.forName(Config.servletMap.get(s));
			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
            HttpServlet servlet = null;
			try {
				servlet = (HttpServlet) servletObject.newInstance();
			} catch (InstantiationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			if (Config.servletParamMap.containsKey(s))
				for(String parm : Config.servletParamMap.get(s).keySet()){
					cfg.setParm(parm, Config.servletParamMap.get(s).get(parm));
				}
			try {
				servlet.init(cfg);
			} catch (ServletException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			Config.nameToServlet.put(s, servlet);
		}
	}

	public static void main(String[] args) throws Exception{
		
		//Initialize global logger
		//GlobalLog.init();
		//GlobalLog.getLogger().info("Global logger initialized.");
		
		//Validate user input
		if(!Validator.validate(args)){
			//GlobalLog.getLogger().fatal("Bad user input: Exiting.");
			return;
		}
		
		//Input is validated; save into Config class
		Config.PORT_NUM = Integer.parseInt(args[0].trim());
		
		if(!(args[1].endsWith("/")))
			args[1] += "/";
		Config.HTDOCS_PATH = args[1];
		
		Config.WEBXML_PATH = args[2].trim();
		
		//GlobalLog.getLogger().info("User input validated. Port = " + args[0] + ", Path = \"" + args[1] + "\".");

		//Create thread pool
		threadPool = new LinkedBlockingQueue<Socket>();
		//threadPool = new PriorityQueue<Socket>();
		
		//Create and init workers
		workers = new Vector<Worker>();
		Worker.init(workers, threadPool, Config.NUM_THREADS);
		
		//Create the dispatcher
		dispatch = new Dispatcher(threadPool, workers);
		dispatch.start();
		
		//Parse web.xml
		TestHarness.Handler handler = TestHarness.externalParseWebdotxml(Config.WEBXML_PATH);
		//Maps servlet name to Class name
		Config.handler = handler;
		Config.servletMap = handler.m_servlets;
		Config.contextParamMap = handler.m_contextParams;
		Config.servletParamMap = handler.m_servletParams;
		
		//System.out.println("ContextMap:\n" + Config.contextParamMap.toString());
//		System.out.println("\n");
		setupServlets();
		
		//Create the menu
		while(true){
			System.out.println("Bang Search Engine Web Application Running on Port " + args[0] + ".\nServing documents from path \"" + args[1] + "\"");
			System.out.println("Developed by Zach Zarrow for CIS-455 Final Project");
			System.out.println();
			System.out.println("Server menu:\n\t1.) Shutdown Server");
			System.out.println("\t2.) View thread status");
			System.out.println("\t3.) View servlet error log");
			System.out.println("\t4.) Clear servlet error log");
			Scanner s = new Scanner(System.in);
			int input = s.nextInt();
			if(input == 1){
				threadPool.clear();
				Worker.stopAll(workers);
				System.out.println("Server shut down.  Exiting.");
				System.exit(0);
			} else if(input == 2){
				//display thread status
				System.out.println("\n--- Thread Status ---\n");
				for(Worker w : workers){
					String isRunning = w.isAlive() ? "Running" : "Stopped";
					System.out.println("Thread " + w.getId() + "\t" + isRunning + ""); 
				}
				System.out.println("\n" + workers.size() + " threads total.\n");
				
			} else if(input == 3){
				System.out.println("\n--- Server Error Log ---\n");
				for(String e : Config.errorLog)
					System.out.println(e);
				System.out.println("\n" + Config.errorLog.size() + " total errors.\n");
			} else if(input == 4){
				int eSize = Config.errorLog.size();
				Config.errorLog.clear();
				System.out.println("\nServer error log cleared (Removed " + eSize + " entries).\n");
			} else {
				System.out.println("Invalid option.");
			}
		}
	
	}
}

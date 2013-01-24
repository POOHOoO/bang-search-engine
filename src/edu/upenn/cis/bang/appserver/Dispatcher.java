package edu.upenn.cis.bang.appserver;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Queue;
import java.util.Vector;

public class Dispatcher extends Thread{
	
	Queue<Socket> threadPool;
	Vector<Worker> workers;
	
	public Dispatcher(Queue<Socket> _threadPool, Vector<Worker> _workers){
		threadPool = _threadPool;
		workers = _workers;
	}
	
	public void run() {
		super.run();
		
		ServerSocket sock;
		try {
			//Was tested before in Validator to make sure this port would work
			sock = new ServerSocket(Config.PORT_NUM);
			while(true){
				//Put new connections into the queue
				synchronized(threadPool){
					Socket conn = sock.accept();
					threadPool.add(conn);
					//System.out.println("Adding to queue");
					//System.out.println("Waking up a worker");
					//threadPool.notify();
				}

				
			}
		} catch (IOException e) {
	//		GlobalLog.getLogger().error(e.getMessage() + e.getStackTrace().toString());
			e.printStackTrace();
		}
		
	}
	
}

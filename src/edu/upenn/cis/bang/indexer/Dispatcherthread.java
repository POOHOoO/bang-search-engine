package edu.upenn.cis.bang.indexer;


import java.net.Socket;
import java.util.Queue;


public class Dispatcherthread extends Thread{
	
	Queue<Socket> workqueue;
	Workerthread[] threadpool;

	
	public Dispatcherthread(Queue<Socket> workqueue, Workerthread[] threadpool){
		this.workqueue = workqueue;
		this.threadpool = threadpool;
	}
	
	public void run(){
		
		while(true){
			System.out.print("");
			if (!workqueue.isEmpty()){
				System.out.println(workqueue.size());
				NotifiedThreadpool(workqueue);
			}
		}
	}
	
	public void NotifiedThreadpool(Queue<Socket> workerqueue){
		
		while(true){
			for (int i=0; i<threadpool.length; i++){
				if (!(threadpool[i].isRunning())){
					threadpool[i].getNewconnection(workerqueue.remove());
					return;
				}	
			}
		}
		
		
	}

}

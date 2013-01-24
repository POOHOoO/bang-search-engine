package edu.upenn.cis.bang.indexer;


import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

import rice.p2p.commonapi.Id;


class Workerthread extends Thread {
	
	private boolean running = true;
	private Socket socket;
	private NodeFactory nodefactory;
	private List<App> lapp;
	private int id = -1;
	private String datatosend = "";

	
	public Workerthread(NodeFactory nodefactory, List<App> lapp, int id){
		this.nodefactory = nodefactory;
		this.lapp = lapp;
		this.id = id;
	}

	public boolean isRunning() {
		return running;
	}


	public synchronized void getNewconnection(Socket socket){
		notify();
		this.socket = socket;
		running = true;
	}
	
	public synchronized void wakeupandsendresult(String data){
		notify();
		this.datatosend = data+ "\n\n";
	}	
	
	public void run(){
		

		while(true){
			
			synchronized(this){
				try {
					running = false;
					this.wait();
					
				} catch (InterruptedException e) {
					e.printStackTrace();
				}	
			}
			
			//// After the thread is awake
			try {
				
				OutputStream out = socket.getOutputStream();
				InputStream in = socket.getInputStream();

				BufferedReader reader = new BufferedReader(new InputStreamReader(in, "UTF-8"));
				
				String stringin ="";
				
				String line = reader.readLine();
				
				while(line.length() != 0){
	               stringin += line;
	               line = reader.readLine();
	            }
				
				/// Handle first line
				System.out.println("From worker thread: " + stringin);
				
				App bootNode = lapp.get(0);
				
				String type = getType(stringin);
				
				boolean typenotfound = false;
				
				if (type.equals("store")){
					String url = geturl(stringin);
					
					if (!url.equals("")){
						Id nodeid = nodefactory.getIdFromString(SHA_Helper.toSha1(url));
						bootNode.store(nodeid, stringin,id);
					}
					else{
						out.flush();
						out.close();
						in.close();	
					}
					
					
				}
				else if (type.equals("pulllinks")){
					String url = geturl(stringin);
					Id nodeid = nodefactory.getIdFromString(SHA_Helper.toSha1(url));
					bootNode.pulllinks(nodeid, url,id);
				}
				else if (type.equals("setpagerank")){
					
					String data = getdata(stringin);
					
					if (!data.equals("")){
						System.out.println("data is " + data);
						
						bootNode.setpagerank(data,id);
						
					}
					
						out.flush();
						out.close();
						in.close();	
				
					
					
				}
				else if (type.equals("query")){
					String query = getquery(stringin);
					if (!query.equals("")){
						Id nodeid = nodefactory.getIdFromString(SHA_Helper.toSha1(query));
						bootNode.query(nodeid, query,id);
					}
					else{
						out.flush();
						out.close();
						in.close();	
					}
				}
				else{
					out.write(this.datatosend.getBytes());
					out.flush();
					out.close();
					in.close();
					typenotfound = true;
				}
				
				if (!typenotfound){
					synchronized(this){
						try {
							this.wait();
							
						} catch (InterruptedException e) {
							e.printStackTrace();
						}	
					}
					/// wake up after the bootnode reply back
								
					out.write(this.datatosend.getBytes());
					out.flush();
					out.close();
					in.close();	
				}
			
		
				
			
			} catch (Exception e) {
				e.printStackTrace();
			}
			
		}		
	}
	
	
	
	private static String getType(String in){
		String str = "";
		
		System.err.println("******");
		System.out.println(in);
		System.err.println("******");
		
		try{
			String searchterm = "<type>";
			String endsearchterm = "</type>";
			int index = in.indexOf(searchterm);
			int endindex = in.indexOf(endsearchterm);
			
			str = in.substring(index + endsearchterm.length() -1, endindex);
		}
		catch (Exception e){
			e.printStackTrace();
		}
		
		return str;
	}
	
	private static String geturl(String in){
		
		String str = "";
		try{
			String searchterm = "<url>";
			String endsearchterm = "</url>";
			int index = in.indexOf(searchterm);
			int endindex = in.indexOf(endsearchterm);
			
			str = in.substring(index + searchterm.length(), endindex);
		}
		catch (Exception e){
			e.printStackTrace();
		}
		return str;
	}
	

	
	private static String getquery(String in){
		String str = "";
		
		try{
			String searchterm = "<query>";
			String endsearchterm = "</query>";
			int index = in.indexOf(searchterm);
			int endindex = in.indexOf(endsearchterm);
			
			str = in.substring(index + searchterm.length(), endindex);
		}
		catch(Exception e){
			e.printStackTrace();
		}
		return str;
	}
	
	
	private static String getdata(String in){
		String str = "";
		
		try{
			String searchterm = "<data>";
			String endsearchterm = "</data>";
			int index = in.indexOf(searchterm);
			int endindex = in.indexOf(endsearchterm);
			
			str = in.substring(index + searchterm.length(), endindex);
		}
		catch(Exception e){
			e.printStackTrace();
		}
		return str;
	}
	
	
	
	public static String gettitle(String in){
		
		String str = "";
		
		try{
			String searchterm = "<title>";
			String endsearchterm = "</title>";
			int index = in.indexOf(searchterm);
			int endindex = in.indexOf(endsearchterm);
			
			str = in.substring(index + endsearchterm.length() -1, endindex);
		}
		catch (Exception e){
			e.printStackTrace();
		}
		return str;
	}
	
	public static String getdescription(String in){
		String str = "";
		
		
		in = in.toLowerCase();
		
		String searchterm = "<meta name=\"description\"";
		
		int index = in.indexOf(searchterm);
		
		if (index==-1){
			return "NA";
		}
	
		try{
		
			String searchterm2 = "content=\"";
			String endsearchterm = "\"";
			
			int beginindex = in.indexOf(searchterm2,index);
			//System.out.println(beginindex);
			int endindex = in.indexOf(endsearchterm,beginindex+searchterm2.length()+2);
			//System.out.println(endindex);
			
			str = in.substring(beginindex + searchterm2.length(), endindex);
		}
		catch(Exception e){
			e.printStackTrace();
		}
		return str;
	}
	
	
	
	
	public static void main(String[] args){
		String in = "<type>setpagerank</type><data>a 123,b 123,c 43</data>\n\n";
		
		String out = getdata(in);
	
		//System.out.println(get("<type>you</type>"));
		
	
	}
	
}


	



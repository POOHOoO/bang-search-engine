package edu.upenn.cis.bang.indexer;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.Set;

import javax.print.attribute.standard.Severity;

import rice.p2p.commonapi.Application;
import rice.p2p.commonapi.Endpoint;
import rice.p2p.commonapi.Id;
import rice.p2p.commonapi.Node;
import rice.p2p.commonapi.NodeHandle;
import rice.p2p.commonapi.RouteMessage;
import edu.upenn.cis.bang.indexer.Links.Link;


public class App implements Application {

	final String APPNAME = "APP";
	final String DBENV = "./";
	
	NodeFactory nodefactory;
	Node node;
	Endpoint endpoint;
	boolean isbootnode = false;
	Workerthread[] threadpool;
	int workerid = 0;
	BerkeleyDB db;
	
	
	/// MAP for inverted index
	HashMap<String, Integer> id_resultleft;
	HashMap<String, List<String>> id_words;
	HashMap<String, Links> word_links;	
	
	/// MAP for page rank
	
	HashMap<String,HashMap<String, Set<WordandTF>>> id_link_words;	/// MAP for inverted inverted index
	

	HashMap<String, List<String>> id_links;
	HashMap<String, Url_data> links_data;
	HashMap<String, Integer> id_pagerankresultleft;
	/// MAP for returning data to the collect nodehandler
	HashMap<String, NodeHandle> id_nodehandle;
	
	Queue<Id_Message>  id_message;
	Queue<Id_PageRank> id_pagerank;
	
	String databaseaddr = null;
	int databaseport = 0;
	
	int lock = 0;
	
	
	public boolean Isbootnode() {
		return isbootnode;
	}


	public void setIsbootnode(boolean isbootnode) {
		this.isbootnode = isbootnode;
	}


	public App(NodeFactory nodeFactory, Workerthread[] threadpool, String databaseaddr, int databaseport){
		
		this.databaseaddr = databaseaddr;
		this.databaseport = databaseport;
		
		this.nodefactory = nodeFactory;
		this.node = nodeFactory.getNode();
		this.endpoint = node.buildEndpoint(this, APPNAME);
		this.endpoint.register();
		this.threadpool = threadpool;
		
		this.id_resultleft = new HashMap<String, Integer>();
		this.id_pagerankresultleft = new HashMap<String, Integer>();
		this.id_words = new HashMap<String, List<String>>();
		this.word_links = new HashMap<String, Links>();
		this.id_nodehandle = new HashMap<String, NodeHandle>();
		this.id_link_words = new HashMap<String,HashMap<String, Set<WordandTF>>>();
		
		this.id_links = new HashMap<String, List<String>>();
		this.links_data = new HashMap<String, Url_data>();
		
		this.id_message = new LinkedList<Id_Message>();
		this.id_pagerank = new LinkedList<Id_PageRank>();
		
		
		db  = new BerkeleyDB(DBENV);
		db.init();
		
	}
	
	public void store(Id idToSendto, String msg, int workerid){
		this.workerid = workerid;
		Message m = new Message(node.getLocalNodeHandle(), msg);
		m.type = "STORE";
		this.endpoint.route(idToSendto, m, null);
	}
	
	public void setpagerank(String data, int workerid){
		
		
		try{
			String[] str = data.split(",");
			for (int i=0; i<str.length; i++){
				String[] tmp = str[i].split(" ");
				if (tmp.length == 2){
					String url = tmp[0];
					String rank = tmp[1];
					Id nodeid = nodefactory.getIdFromString(SHA_Helper.toSha1(url));
					
					Id_PageRank idpg = new Id_PageRank(nodeid, rank, url);
					
					this.id_pagerank.add(idpg);					
			
				}
			}
		}
		catch (Exception e){
			e.printStackTrace();
		}
		
		checkpagerankandsend(10);

	}
	
	public void pulllinks(Id idToSendto, String msg, int workerid){
		this.workerid = workerid;
		Message m = new Message(node.getLocalNodeHandle(), msg);
		m.type = "PULL_LINKS_FROM_URL";
		this.endpoint.route(idToSendto, m, null);
	}
	
	public void query(Id idToSendto, String msg, int workerid){
		this.workerid = workerid;
		Message m = new Message(node.getLocalNodeHandle(), msg);
		m.type = "QUERY";
		this.endpoint.route(idToSendto, m, null);
	}
	
	
	
	void sendMessage(Id idToSendto, String msgToSend){
		Message m = new Message(node.getLocalNodeHandle(), msgToSend);
		this.endpoint.route(idToSendto, m, null);
	}
	

	public void deliver(Id id, rice.p2p.commonapi.Message message) {
		Message m = (Message) message;
		System.out.println("["+this.node.toString()+"][" + m.type + "]" +"Received message " + m.content +" from " + m.from);
		
		if (m.wantResponse){
			Message reply = new Message(node.getLocalNodeHandle(), "Message received");
			reply.wantResponse = false;
			reply.type = "reply";
			endpoint.route(null, reply, m.from);
		}
		
		if (m.type.equals("STORE")){
			System.out.println("Received a message from crawler to store");			

			/// SENDING REPLY MEsssage
			Message reply = new Message(node.getLocalNodeHandle(), "");
			reply.type = "RESULTS";
			endpoint.route(null, reply, m.from);
			
			String url = CrawlerStringParser.geturl(m.content);
			try{
				//db.setlinksgivenURL(url, CrawlerStringParser.getlinks(m.content));
				String tosend = CrawlerStringParser.getlinks(url, m.content);
				
				tcpsender.send(this.databaseaddr, this.databaseport, tosend);
			}
			catch (Exception e){
				System.err.println("Error connecting to databasse");
				e.printStackTrace();
			}
			
			
			String title = "";
			String description = "";
			String pagerank  = "0";
			//// storing title and description
			try{
				//String pagerank = "0";
				title = ServletResultGenerator.gettitle(m.content);
				description = ServletResultGenerator.getdescription(m.content);
			}
			catch (Exception e){
				e.printStackTrace();
			}
				
			try{
				System.out.println("******: " + title);
				
				Url_data urldata = new Url_data(pagerank, title, description);
				
				db.setData(url, urldata);
				
				
			}
			catch (Exception e){
				System.err.println("Error getting title");
				e.printStackTrace();
			}
			
			
			
			//// Seeing which word to send to who WORDS
			
			try{
				Map<String, Float> word_f = CrawlerStringParser.getwords(m.content);
				
				Iterator<String> it = word_f.keySet().iterator();
				while(it.hasNext()){
					String word = it.next();
					if (CrawlerStringParser.checkword(word)){
						Id nodeid = nodefactory.getIdFromString(SHA_Helper.toSha1(word));
						
						float f = word_f.get(word);
						
						id_message.add(new Id_Message(nodeid, url + "," + word + "," + f));
						
					}
				}
				
			}
			catch (Exception e){
				e.printStackTrace();
			}
			
			/// Sending messages	
			removefromlistandsend(30);
		}
		else if (m.type.equals("STORE_LINKS")){
			System.out.println("Received a message to store links given word");
	
			String[] args = m.content.split(",");
			String link = args[0];
			
			for (int i=1; i<args.length; i++){
				String word = args[i];
				String f = args[i+1];
				i++;
				
				System.out.println("Word is :"+word);
				db.addlinkgivenword(word, link, f);	
			}
			
			Message reply = new Message(node.getLocalNodeHandle(), "");
			reply.wantResponse = false;
			reply.type = "STORE_SUCCESS";
			this.endpoint.route(null, reply, m.from);				
		}
		else if (m.type.equals("STORE_SUCCESS")){
			
			removefromlistandsend(1);
			
		}
		else if (m.type.equals("RESULTS")){
			threadpool[this.workerid].wakeupandsendresult(m.content);
		}
		/// NO LONGER USE THIS
		else if (m.type.equals("PULL_LINKS_FROM_URL")){
			String url = m.content;
			System.out.println("Received a message from pagerank looking for links to " + url);
			Links out = db.getlinksfromURL(url);

			String str = "";
			if (out != null){
				Iterator<Link> it = out.link_set.iterator();
				while (it.hasNext()){
					str += it.next().name + ",";
				}
			}
			
			Message reply = new Message(node.getLocalNodeHandle(), str);
			reply.wantResponse = false;
			reply.type = "RESULTS";
			endpoint.route(null, reply, m.from);	
		}
		else if (m.type.equals("SET_PAGERANK")){	
			String[] args = m.content.split(",");
			String url = args[0];
			String rank = args[1];
			System.out.println("Received a message from pagerank to set page rank of " + url);			

			
			Url_data urldata = db.getdatafromurl(url);
			
			if (urldata == null){
				urldata = new Url_data(rank,"","");
				db.setData(url, urldata);
			}
			else{
				urldata.pagerank = rank;
				db.setData(url,urldata);
			}
			
			
			Message reply = new Message(node.getLocalNodeHandle(), "");
			reply.wantResponse = false;
			reply.type = "STORE_PAGERANK_SUCCESS";
			endpoint.route(null, reply, m.from);
		}
		else if (m.type.equals("STORE_PAGERANK_SUCCESS")){
			checkpagerankandsend(1);
		}
		
		else if (m.type.equals("QUERY")){
			System.out.println("Receieve query for searchterm:" + m.content);
			String[] words = m.content.split(" ");
			
			for (int i=0; i<words.length; i++){
				words[i] =  CrawlerStringParser.cutword(words[i]); /// use helper method by Crawler.. Nothing to do with it
	
			}
			

	        Random rn = new Random();

	        String randomid = Integer.toString(rn.nextInt());
	
			this.id_resultleft.put(randomid, words.length);
			this.id_words.put(randomid, Arrays.asList(words));
			this.id_nodehandle.put(randomid, m.from);
	        
	        
			for (int i=0; i<words.length; i++){
				
				String word = words[i];
				
				this.word_links.put(word, new Links());
				Message forward = new Message(node.getLocalNodeHandle(), randomid+","+word);
				forward.wantResponse = false;
				forward.type = "LOOKUP_LINKS";
				Id nodeid = nodefactory.getIdFromString(SHA_Helper.toSha1(word));
				System.out.println(nodeid.toString());
				this.endpoint.route(nodeid, forward, null);
					
			}	
		}
		else if (m.type.equals("LOOKUP_LINKS")){
			String args[]  = m.content.split(",");
			String messageid = args[0];
			String word = args[1];
			
			String response = word + ",";
			Links l = db.getlinksfromword(word);
			
			if (l!=null){
				Iterator<Link> it = l.link_set.iterator();
				while(it.hasNext()){
					Link temp = it.next();
					response += temp.name + "###" + temp.tf + ",";
				}
			}
			
			Message reply = new Message(node.getLocalNodeHandle(), messageid + "," + response);
			reply.wantResponse = false;
			reply.type = "RETURN_LOOKUP";
			endpoint.route(null, reply, m.from);
			
		}
		else if (m.type.equals("LOOKUP_PAGERANK")){
			String args[] = m.content.split(",");
			String messageid = args[0];
			String url = args[1];
			
			String pagerank;
			String title = " ";
			String description = " ";
			Url_data urldata = db.getdatafromurl(url);
			
			if (urldata == null){
				pagerank = "0";
			}
			else{
				pagerank = urldata.pagerank;
				title = CrawlerStringParser.removecomma(urldata.title);
				description = CrawlerStringParser.removecomma(urldata.description);
			} 
			 
			
			Message reply = new Message(node.getLocalNodeHandle(), messageid + "," + url + "," + pagerank + "," + title + "," + description);
			reply.wantResponse = false;
			reply.type = "RETURN_PAGERANK_LOOKUP";
			endpoint.route(null, reply, m.from);
			
		}
		else if (m.type.equals("RETURN_LOOKUP")){
			String args[] = m.content.split(",");
			String messageid = args[0];
			String word = args[1];
			//System.out.println("***" + this.id_resultleft);
			
			int left = this.id_resultleft.get(messageid) -1;
			this.id_resultleft.put(messageid, left);
			
			
			Links l= this.word_links.get(word);
			for (int i=2; i<args.length; i++){
				System.out.println(args);
				String[] temp = args[i].split("###");
				Link templink = l.new Link();
				templink.name = temp[0];
				templink.tf = temp[1];
				l.link_set.add(templink);
			}
			
			checkstatus(messageid);	
		}
		
		else if (m.type.equals("RETURN_PAGERANK_LOOKUP")){
			String args[] = m.content.split(",");
			
			
			String messageid = "";
			String url = "";
			String pagerank = "";
			String description = "";
			String title = "";
			
			
			try{
				messageid = args[0];
				url = args[1];
				pagerank = args[2];
				title = args[3];
				description = args[4];
			}
			catch(Exception e){
				e.printStackTrace();
			}
			
			
			
			
			
			System.out.println("XXX" + title);
			
			int left = this.id_pagerankresultleft.get(messageid) -1;
			this.id_pagerankresultleft.put(messageid, left);
			
			
			Url_data urldata = new Url_data(pagerank, title, description);
			
			this.links_data.put(url, urldata);

	
			checkpagerankandsend(messageid);	
		
		}
	}
	
	public void checkpagerankandsend(String messageid){	
		
	
		int left = this.id_pagerankresultleft.get(messageid);
		if (left==0){
			
			Map<String, Integer> word_df = new HashMap<String,Integer>();
			Map<String, Set<WordandTF>> link_words_out = new HashMap<String, Set<WordandTF>>();
			Map<String, Url_data> urldata = new HashMap<String, Url_data>();
				
			Iterator<String> it = id_words.get(messageid).iterator();
			while(it.hasNext()){
				String word_from_this_id = it.next();
				word_df.put(word_from_this_id, this.word_links.get(word_from_this_id).link_set.size());
				
			}
			
			it = id_links.get(messageid).iterator();
			//System.out.println("Word search by this id is:" + id_links.get(messageid));
			
			while(it.hasNext()){
				String url_from_this_id = it.next();
				
				link_words_out.put(url_from_this_id, id_link_words.get(messageid).get(url_from_this_id));
				urldata.put(url_from_this_id, this.links_data.get(url_from_this_id));	
			}
			
			System.out.println("Data to be sent to ServletResult Generator");
			System.out.println("word_df is:" + word_df);
			System.out.println("link_words_out is:" + link_words_out);
			System.out.println("pagerank is:" + urldata);
			System.out.println("**************");
			
			String response = ServletResultGenerator.gen(word_df, link_words_out, urldata, 997631);
			Message reply = new Message(node.getLocalNodeHandle(), response);
			reply.wantResponse = false;
			reply.type = "RESULTS";
			endpoint.route(null, reply, this.id_nodehandle.get(messageid));	
		}
		
	}
	
	public void checkstatus(String messageid){
		int left = this.id_resultleft.get(messageid);
		if (left==0){
			
			
			/// Can combine the two while loop below.. do it later... too confusing now!
			/// GET word_DF
			Map<String, Integer> word_df = new HashMap<String,Integer>();
			Iterator<String> it = this.id_words.get(messageid).iterator();
			while(it.hasNext()){
				String i_word = it.next();
				word_df.put(i_word, word_links.get(i_word).link_set.size());
			}
			//System.out.println(word_df);
			
			/// Set inverted inverted index eg. Google.com: <Smart###3>
			it = this.id_words.get(messageid).iterator();
			Set<String> newlyaddedlinks = new HashSet<String>();
			
			while(it.hasNext()){
				String i_word = it.next();
				Iterator<Link> it2 = this.word_links.get(i_word).link_set.iterator();
				while (it2.hasNext()){
					Link i_link = it2.next();
					newlyaddedlinks.add(i_link.name);
					
					HashMap<String,Set<WordandTF>> link_words = this.id_link_words.get(messageid);
					if (link_words==null){
						link_words = new HashMap<String,Set<WordandTF>>();
						this.id_link_words.put(messageid, link_words);
					}
					
					if (!link_words.containsKey(i_link.name)){
						link_words.put(i_link.name, new HashSet<WordandTF>());
					}
					Set<WordandTF> s =link_words.get(i_link.name);
					if (id_words.get(messageid).contains(i_word)){
						s.add(new WordandTF(i_word, i_link.tf));
					}
				}
			}
			
			/// if the newlyaddlinks is null (means found no search result) --> return
			if (newlyaddedlinks.isEmpty()){
				Message reply = new Message(node.getLocalNodeHandle(), "<searchresults></searchresults>");
				reply.wantResponse = false;
				reply.type = "RESULTS";
				endpoint.route(null, reply, this.id_nodehandle.get(messageid));		
			}
			else{
				/// Send request to get pagerank of all pages in newly added set
				id_pagerankresultleft.put(messageid, newlyaddedlinks.size());
				id_links.put(messageid,new LinkedList<String>());
				it = newlyaddedlinks.iterator();
				while (it.hasNext()){
					String url = it.next();
					id_links.get(messageid).add(url);				
					Message forward = new Message(node.getLocalNodeHandle(), messageid+","+url);
					forward.wantResponse = false;
					forward.type = "LOOKUP_PAGERANK";
					Id nodeid = nodefactory.getIdFromString(SHA_Helper.toSha1(url));
					System.out.println(nodeid.toString());
					this.endpoint.route(nodeid, forward, null);	
				}	
			}
		}
	}
	
	
	public boolean forward(RouteMessage arg0) {
		return true;
	}

	public void update(NodeHandle arg0, boolean arg1) {
		// empty
	}
	
	public void removefromlistandsend(int no_of_message_to_send){
					
		int counter = 0;
		
		while(true){
			counter++;
			if (counter> no_of_message_to_send){break; }
			Id_Message id_m = id_message.poll();
			if (id_m == null){
				break;
			}
			Message reply = new Message(node.getLocalNodeHandle(), id_m.message);
			reply.wantResponse = false;
			reply.type = "STORE_LINKS";
			this.endpoint.route(id_m.id, reply, null);
		}
				
	}
	
	public void checkpagerankandsend(int no_of_message_to_send){
		
		int counter = 0;
		
		while(true){
			counter++;
			if (counter> no_of_message_to_send){break; }
			Id_PageRank id_pg = id_pagerank.poll();
			if (id_pg == null){
				break;
			}
			Message reply = new Message(node.getLocalNodeHandle(), id_pg.url + "," +id_pg.pagerank);
			
			reply.wantResponse = false;
			reply.type = "SET_PAGERANK";
			this.endpoint.route(id_pg.id, reply, null);
		}
		
		
	}
	
	
}

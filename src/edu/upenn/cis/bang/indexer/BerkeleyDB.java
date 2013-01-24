package edu.upenn.cis.bang.indexer;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.TimeZone;

import com.sleepycat.bind.EntryBinding;
import com.sleepycat.bind.serial.SerialBinding;
import com.sleepycat.bind.serial.StoredClassCatalog;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseConfig;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.je.LockMode;
import com.sleepycat.je.OperationStatus;

import edu.upenn.cis.bang.indexer.Links.Link;


public class BerkeleyDB{
	
	public Database url_data = null;
	public Database url_links = null;
	public Database word_links = null;
	public Database class_db = null;
	///
	
	public String env = null;
	public Environment myDbEnvironment = null;
	
	/// Constructor
	public BerkeleyDB(String env){
		this.env = env;
	}
	
	
	public void init(){
		myDbEnvironment = null; 
	
		try { 
			// Open the environment. Create it if it does not already exist. 
			EnvironmentConfig envConfig = new EnvironmentConfig(); 
			envConfig.setAllowCreate(true); 
			myDbEnvironment = new Environment(new File(env),envConfig);
			// Open the database. Create it if it does not already exist. 
			DatabaseConfig dbConfig = new DatabaseConfig();
			dbConfig.setAllowCreate(true);
			dbConfig.setSortedDuplicates(false);
			url_data = myDbEnvironment.openDatabase(null,"URL_Pagerank", dbConfig);
			url_links = myDbEnvironment.openDatabase(null,"URL_Links", dbConfig);
			word_links = myDbEnvironment.openDatabase(null,"Xpath_raw of all XML", dbConfig);			
			class_db = myDbEnvironment.openDatabase(null, "classdb", dbConfig);
			
		} catch (DatabaseException dbe) {
			System.out.println("Error in creating database");
			System.out.println(dbe);
		}
		
	}
	
	
	/// Helper function to get data from specified database and key
	public byte[] getvalue(Database db, String key){
		try{
			DatabaseEntry theKey = new DatabaseEntry(key.getBytes("UTF-8")); 
			DatabaseEntry theData = new DatabaseEntry();
			// Perform the get. 
			if (db.get(null, theKey, theData, LockMode.DEFAULT) ==OperationStatus.SUCCESS) {
			// Recreate the data String. 
				byte[] retData = theData.getData(); 
				return retData;
			}
			else{
				return null;
			}
			
		}
		catch (Exception e){
			System.out.println("Error in getting the values from database");
			System.out.println(e);
			return null;
		}
		
	}
	
	public Url_data getdatafromurl(String url){
		
		
		try{
			// Instantiate the class catalog 
			StoredClassCatalog classCatalog = new StoredClassCatalog(class_db);
			// Create the binding 
			EntryBinding<Url_data> dataBinding = new SerialBinding<Url_data>(classCatalog,Url_data.class);
			// Create DatabaseEntry objects for the key and data 
			DatabaseEntry theKey = new DatabaseEntry(url.getBytes("UTF-8")); 
			DatabaseEntry theData = new DatabaseEntry();
			// Do the get as normal 
			url_data.get(null, theKey, theData, LockMode.DEFAULT);
			//System.out.println("the data is " + theData);
	
			if (theData.getData() != null){
				return (Url_data) dataBinding.entryToObject(theData);
			}
			else{
				return null;
			}
			
		}
		catch(Exception e){
			System.out.println("Error in getting data from url");
			e.printStackTrace();
		}
		return null;
			
	}
	
	/// Helper function for set page rank for the speficied URL
	public void setData(String url, Url_data url_data_in){
		

		try{
			/// Store new channel of the specified user
			StoredClassCatalog classCatalog = new StoredClassCatalog(class_db);
			EntryBinding<Url_data> dataBinding = new SerialBinding<Url_data>(classCatalog, Url_data.class);
			
			DatabaseEntry theKey = new DatabaseEntry(url.getBytes("UTF-8"));
			DatabaseEntry theData = new DatabaseEntry();
			dataBinding.objectToEntry(url_data_in, theData);
			url_data.put(null, theKey, theData);
			
		}
		
		catch(Exception e){
			System.out.println("Error adding url_data");
			System.out.println(e);
		}
		
	}
	
	
	/// helper class to get all the channels of a user
	public Links getlinksfromURL(String url){
		try{
			// Instantiate the class catalog 
			StoredClassCatalog classCatalog = new StoredClassCatalog(class_db);
			// Create the binding 
			EntryBinding<Links> dataBinding = new SerialBinding<Links>(classCatalog,Links.class);
			// Create DatabaseEntry objects for the key and data 
			DatabaseEntry theKey = new DatabaseEntry(url.getBytes("UTF-8")); 
			DatabaseEntry theData = new DatabaseEntry();
			// Do the get as normal 
			url_links.get(null, theKey, theData, LockMode.DEFAULT);
			
			if (theData.getData() != null){
				return (Links) dataBinding.entryToObject(theData);
			}
			else{
				return null;
			}
			
		}
		catch(Exception e){
			System.out.println("Error in getting links");
			System.out.println(e);
		}
		return null;
	}
	
	
	public Links getlinksfromword(String word){
		try{
			// Instantiate the class catalog 
			StoredClassCatalog classCatalog = new StoredClassCatalog(class_db);
			// Create the binding 
			EntryBinding<Links> dataBinding = new SerialBinding<Links>(classCatalog,Links.class);
			// Create DatabaseEntry objects for the key and data 
			DatabaseEntry theKey = new DatabaseEntry(word.getBytes("UTF-8")); 
			DatabaseEntry theData = new DatabaseEntry();
			// Do the get as normal 
			word_links.get(null, theKey, theData, LockMode.DEFAULT);
			//System.out.println("the data is " + theData);
	
			if (theData.getData() != null){
				return (Links) dataBinding.entryToObject(theData);
			}
			else{
				return null;
			}
			
		}
		catch(Exception e){
			System.out.println("Error in getting Links from word");
			e.printStackTrace();
		}
		return null;
		
	}
	

	public void setlinksgivenURL(String url, List<String> list){
		Links links = new Links();
		Iterator<String> it = list.iterator();
		while (it.hasNext()){
			Link link = links.new Link();
			link.name = it.next();
			links.link_set.add(link);
		}
		
		try{
			/// Store new channel of the specified user
			StoredClassCatalog classCatalog = new StoredClassCatalog(class_db);
			EntryBinding<Links> dataBinding = new SerialBinding<Links>(classCatalog, Links.class);
			
			DatabaseEntry theKey = new DatabaseEntry(url.getBytes("UTF-8"));
			DatabaseEntry theData = new DatabaseEntry();
			dataBinding.objectToEntry(links, theData);
			url_links.put(null, theKey, theData);
			
		}
		
		catch(Exception e){
			System.out.println("Error adding links");
			System.out.println(e);
		}
		
	}

	
	/// Helper method to add file 
	public void addlinkgivenword(String word, String link_name, String f){
		Links links = this.getlinksfromword(word);
		
		if (links == null){
			links = new Links();
		}
		
		Link link = links.new Link();
		link.name = link_name;
		link.tf = f;
		links.link_set.add(link);
		
		try{
			/// Store new channel of the specified user
			StoredClassCatalog classCatalog = new StoredClassCatalog(class_db);
			EntryBinding<Links> dataBinding = new SerialBinding<Links>(classCatalog, Links.class);
			
			DatabaseEntry theKey = new DatabaseEntry(word.getBytes("UTF-8"));
			DatabaseEntry theData = new DatabaseEntry();
			dataBinding.objectToEntry(links, theData);
			word_links.put(null, theKey, theData);
		}
		
		catch(Exception e){
			System.out.println("Error adding links");
			System.out.println(e);
		}
		
		
	}
	
	
	public void close(){
		url_links.close();
		url_data.close();
		word_links.close();
		class_db.close();
		myDbEnvironment.close();
		
	}
	
	
	public static void main(String[] args) throws UnsupportedEncodingException{
		
		/// test code
		/*
		BerkeleyDB db = new BerkeleyDB("/home/cis555/hw2");
		
		db = new BerkeleyDB("/home/cis555/hw2");
		db.init();
		System.out.println(db.getpagerank("smart"));
		db.close();
		*/
	}
	
	
	
	
	
	
	
	
}



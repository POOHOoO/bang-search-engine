package edu.upenn.cis.bang.pagerank;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import com.sleepycat.bind.EntryBinding;
import com.sleepycat.bind.serial.SerialBinding;
import com.sleepycat.bind.serial.StoredClassCatalog;
import com.sleepycat.je.Cursor;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseConfig;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.je.LockMode;
import com.sleepycat.je.OperationStatus;

public class BDBstore {

	public static String CHARSET = "UTF-8";
	public Database pagelinks;
	public Database classDb;
	public Environment environment;
	public StoredClassCatalog classCatalog;
	
	public BDBstore(String filePath){
		this(filePath, false);
	}
	
	public BDBstore(String filePath, boolean readOnly){
		(new File(filePath)).mkdirs();
		init(filePath, readOnly);
	}
	
	private void init(String filePath, boolean readOnly){
		EnvironmentConfig environmentConfig = new EnvironmentConfig();
		environmentConfig.setAllowCreate(true);
		
		environmentConfig.setReadOnly(readOnly);
		
		// perform other environment configurations
		File file = new File(filePath); 
		environment = new Environment(file, environmentConfig);
		
		DatabaseConfig databaseConfig = new DatabaseConfig();
		databaseConfig.setAllowCreate(true);
		databaseConfig.setReadOnly(readOnly);
		
		// perform other database configurations
		pagelinks = environment.openDatabase(null, "xpaths" , databaseConfig);
		classDb = environment.openDatabase(null, "classDb" , databaseConfig);
		
		classCatalog = new StoredClassCatalog(classDb);
	}
	
	public void close(){
		if (environment != null){
			try {
				pagelinks.close();
				classDb.close();
				environment.close();
			} catch (DatabaseException e){
				e.printStackTrace();
			}
		}
	}
	
	public void addPageLinks(String url, String outlink){
		LinkedList<String> docs = getPageLink(url);
		if (docs == null){
			docs = new LinkedList<String>();
		}
		if (outlink != null){
			if (docs.contains(outlink)){
				docs.remove(outlink);
			}
			docs.add(outlink);
		}
		putPageLinks(url, docs);
	}
	
	public void putPageLinks(String xpath, LinkedList outlinks){
		try {
			EntryBinding<LinkedList> dataBinding = new SerialBinding<LinkedList>(classCatalog, LinkedList.class);
			
			DatabaseEntry key = new DatabaseEntry(xpath.getBytes(CHARSET));
			DatabaseEntry value = new DatabaseEntry();
		
			dataBinding.objectToEntry(outlinks, value);

			pagelinks.put(null, key, value);	
			
			//xpaths.sync();
			
		} catch (DatabaseException e){
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}
	
	public LinkedList<String> getPageLink(String xpath){
		try {
			EntryBinding<LinkedList> dataBinding = new SerialBinding<LinkedList>(classCatalog, LinkedList.class);
			DatabaseEntry key = new DatabaseEntry(xpath.getBytes(CHARSET));
			DatabaseEntry value = new DatabaseEntry();
			
			if (pagelinks.get(null, key, value, LockMode.DEFAULT) == OperationStatus.SUCCESS){
				LinkedList<String> data = (LinkedList<String>) dataBinding.entryToObject(value);
				return data;	
			}

					
		} catch (DatabaseException e){
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public Map<String, LinkedList<String>> getAllPageLinks(){
		Map<String, LinkedList<String>> allPageLinks = new HashMap<String, LinkedList<String>>();
		
		Cursor cursor = null;
		try {

			cursor = pagelinks.openCursor(null, null);
			DatabaseEntry entryKey = new DatabaseEntry();
			DatabaseEntry entryValue = new DatabaseEntry();

			while (cursor.getNext(entryKey, entryValue, LockMode.DEFAULT) == OperationStatus.SUCCESS) {
				
				EntryBinding<LinkedList> dataBinding = new SerialBinding<LinkedList>(classCatalog, LinkedList.class);

				String urlKey = new String(entryKey.getData(), BDBstore.CHARSET);
				LinkedList<String> outlinks = (LinkedList<String>) dataBinding.entryToObject(entryValue);
				
				allPageLinks.put(urlKey, outlinks);
			}


		} catch (DatabaseException e) {
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} finally {
			cursor.close();
		}
		
		return allPageLinks;
	}

	
}
package edu.upenn.cis.bang.appserver;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Vector;

import javax.servlet.http.HttpServlet;

import edu.upenn.cis.bang.appserver.TestHarness.Handler;

public class Config {
	public static int NUM_THREADS = 1;
	public static int PORT_NUM;
	public static String HTDOCS_PATH;
	public static String DATE_FORMAT = "EEE, d MMM yyyy HH:mm:ss z";
	public static String WEBXML_PATH;
	
	public static Handler handler;
	public static HashMap<String, String> servletMap;
	public static HashMap<String, String> contextParamMap;
	public static HashMap<String, HashMap<String, String>> servletParamMap;
	public static HashMap<String,HttpServlet> nameToServlet;
	
	public static HttpServletContext context;
	
	public static HashMap<String, HttpServletSession> activeSessions = new HashMap<String, HttpServletSession>();
	
	public static DateFormat httpDate = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss z");
    public static DateFormat cookieDate = new SimpleDateFormat("EEE, d-MMM-yyyy HH:mm:ss z");
    
    public static Vector<String> errorLog = new Vector<String>();
}

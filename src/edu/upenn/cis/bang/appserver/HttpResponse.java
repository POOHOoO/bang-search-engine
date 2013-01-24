package edu.upenn.cis.bang.appserver;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Vector;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

public class HttpResponse implements HttpServletResponse {
	
	public static int SC_ACCEPTED = 202;
	public static int SC_BAD_GATEWAY = 502;
	public static int SC_BAD_REQUEST = 400;
	public static int SC_CONFLICT = 409;
	public static int SC_CONTINUE = 100;
	public static int SC_CREATED = 201;
	public static int SC_EXPECTATION_FAILED = 417;
	public static int SC_FORBIDDEN = 403;
	public static int SC_FOUND = 302;
	public static int SC_GATEWAY_TIMEOUT = 504;
	public static int SC_GONE = 410;
	public static int SC_HTTP_VERSION_NOT_SUPPORTED = 505;
	public static int SC_INTERNAL_SERVER_ERROR = 500;
	public static int SC_LENGTH_REQUIRED = 411;
	public static int SC_METHOD_NOT_ALLOWED = 405;
	public static int SC_MOVED_PERMANENTLY = 301;
	public static int SC_MOVED_TEMPORARILY = 302;
	public static int SC_MULTIPLE_CHOICES = 300;
	public static int SC_NO_CONTENT = 204;
	public static int SC_NON_AUTHORITATIVE_INFORMATION = 203;
	public static int SC_NOT_ACCEPTABLE = 406;
	public static int SC_NOT_FOUND = 404;
	public static int SC_NOT_IMPLEMENTED = 501;
	public static int SC_NOT_MODIFIED = 304;
	public static int SC_OK = 200;
	public static int SC_PARTIAL_CONTENT = 206;
	public static int SC_PAYMENT_REQUIRED = 402;
	public static int SC_PRECONDITION_FAILED = 412;
	public static int SC_PROXY_AUTHENTICATION_REQUIRED = 407;
	public static int SC_REQUEST_ENTITY_TOO_LARGE = 413;
	public static int SC_REQUEST_TIMEOUT = 408;
	public static int SC_REQUEST_URI_TOO_LONG = 414;
	public static int SC_REQUESTED_RANGE_NOT_SATISFIABLE = 416;
	public static int SC_RESET_CONTENT = 205;
	public static int SC_SEE_OTHER = 303;
	public static int SC_SERVICE_UNAVAILABLE = 503;
	public static int SC_SWITCHING_PROTOCOLS = 101;
	public static int SC_TEMPORARY_REDIRECT = 307;
	public static int SC_UNAUTHORIZED = 401;
	public static int SC_UNSUPPORTED_MEDIA_TYPE = 415;
	public static int SC_USE_PROXY = 305;
	
	private HttpRequest req;
	private StringBuffer resp;
	private PrintStream output;
	private boolean isCommitted;
	
	private HashMap<String, Object> headMap;
	private int httpStatus;

	String usrHtmlStr;
	
	private PrintWriter htmlWriter;
	private ByteArrayOutputStream htmlOutput;
	
	Socket conn;
	java.io.ByteArrayOutputStream baos;
	
	private String contentType;
	private Locale loc;
	
	//private LinkedList<Cookie> cookieJar; 
	private Vector<Cookie> cookieJar; //concurrent
	private HttpServletSession sess;
	
	public HttpResponse(HttpRequest _req, PrintStream _output, Socket _conn){
		req = _req;
		output = _output;
		//baos = new java.io.ByteArrayOutputStream();
		//output = new PrintStream(baos);
		
		htmlOutput = new ByteArrayOutputStream();
		htmlWriter = new PrintWriter(htmlOutput);
		
		contentType = "text/html";
		loc = new Locale("en");
		httpStatus = SC_OK;
		isCommitted = false;
		headMap = new HashMap<String, Object>();
		conn = _conn;
		cookieJar = new Vector<Cookie>();
		setupDefaultHeaders();
	}
	
	private void setupDefaultHeaders(){
		Date httpDate = new Date();
		addDateHeader("Date", httpDate.getTime());
		addHeader("Server", Config.context.getServerInfo());
	}
	
	@Override
	public void addCookie(Cookie _oatmeal) {
		cookieJar.add(_oatmeal);
	}

	@Override
	public void addDateHeader(String _hdr, long _val) {
		if(!headMap.containsKey(_hdr))
			headMap.put(_hdr, Config.httpDate.format(new Date(_val)));
		else
			headMap.put(_hdr, headMap.get(_hdr) + ", " + Config.httpDate.format(new Date(_val)));
	}

	@Override
	public void addHeader(String _key, String _val) {
		if(!headMap.containsKey(_key))
			headMap.put(_key, _val);
		else
			headMap.put(_key, headMap.get(_key) + ", " + _val);
	}

	@Override
	public void addIntHeader(String _key, int _val) {
		addHeader(_key, (new Integer(_val)).toString());
	}

	@Override
	public boolean containsHeader(String _key) {
		return headMap.containsKey(_key);
	}

	@Override
	public String encodeRedirectURL(String _redurl) {
		return encodeURL(_redurl);
	}

	@Override
	public String encodeRedirectUrl(String arg0) { return null; }

	@Override
	public String encodeURL(String _url) {
		return _url;
	}

	@Override
	public String encodeUrl(String arg0) { return arg0; }

	@Override
	public void sendError(int _errorCode) throws IOException {
		if(_errorCode == 500)
			sendError(_errorCode, "Internal Server Error");
		else
			Config.errorLog.add(Config.httpDate.format(new Date().getTime()) + "\tError " + _errorCode + "\n");
		setStatus(_errorCode);
		//flushBuffer();
	}

	@Override
	public void sendError(int _errorCode, String _desc) throws IOException {
		output.println(req.getProtocol() + " " + _errorCode + " " + _desc);
		output.println();
		output.println("<html><body><h1>Error " + _errorCode + "</h1><br /><br />" + _desc + "</body></html>");
		output.flush();
		isCommitted = true;
		Config.errorLog.add(Config.httpDate.format(new Date().getTime()) + "\tError " + _errorCode + "\t" + req.getServletPath() + "\t" + _desc);
		//flushBuffer();
	}

	@Override
	public void sendRedirect(String _url) throws IOException {
		output.println(req.getProtocol() + " " + SC_MOVED_TEMPORARILY + " REDIRECT");
		output.println("Location: " + _url);
		output.println();
		output.println("<html><body><h1>HTTP 302 Redirect</h1><br /><br /><a href=\"" + _url + "\">" + _url + "</a></body></html>");
		setStatus(302);
		output.flush();
		isCommitted = true;
		//flushBuffer();
	}

	@Override
	public void setDateHeader(String _key, long _val) {
		headMap.put(_key, _val);

	}

	@Override
	public void setHeader(String _key, String _val) {
		headMap.put(_key, _val);
	}

	@Override
	public void setIntHeader(String _key, int _val) {
		headMap.put(_key, _val);

	}

	@Override
	public void setStatus(int _status) {
		httpStatus = _status;

	}

	@Override
	public void setStatus(int arg0, String arg1) { return; }
	
	private Vector<Cookie> removeExpiredCookies(){
		Vector<Cookie> deletedCookies = new Vector<Cookie>();
		for(Cookie c : cookieJar)
			if (c.getMaxAge() == 0){
				//cookieJar.remove(c);
				deletedCookies.add(c);
			}
		for(Cookie c : deletedCookies)
			cookieJar.remove(c); //Because we can't modify cookieJar while iterating through it
		return deletedCookies;
	}
	
	private void setupCookieHeaders(PrintStream output){
		sess = (HttpServletSession)req.getSessionWithoutUpdate();
        
        if(sess != null){
        	Cookie vanillaWafer = new Cookie("Session", sess.getId());
        	int maxInactive = sess.getMaxInactiveInterval();
        	vanillaWafer.setMaxAge(maxInactive);
        	addCookie(vanillaWafer);
        }
        
        Vector<Cookie> deletedCookies = removeExpiredCookies();        
        
        long timeInMs;
        boolean addedCookie = false;
        String expiration;
        String cookieHeader = "";
        String cookieValue = "";
        
        for(Cookie c : cookieJar){
            long maxAge = 1000 * c.getMaxAge();
        	timeInMs = c.getName().equals("Session") ?
        				maxAge + sess.getCreationTime() :
        					maxAge + (new Date()).getTime();
  
        	expiration = Config.cookieDate.format(new Date(timeInMs));
        	//Each iteration, add on        	
        	output.println("Set-Cookie: " + c.getName() + "=" + c.getValue() + "; expires=" + expiration);
        	
        }
        
        //Now delete old cookies from the client-side
        for(Cookie c : deletedCookies)
        	//Set past expiration date to delete from client
        	output.println("Set-Cookie: " + c.getName() + "=; expires=Mon, 23-Jul-1990 23:07:00 GMT");
	}
	
	@Override
	public void flushBuffer() throws IOException {
        if(isCommitted())
        	return;
       
        //Write header
        String headerLine = "";
        if(httpStatus == SC_OK)
        	headerLine = req.getProtocol() + " " + SC_OK + " OK";
        if(httpStatus == SC_INTERNAL_SERVER_ERROR)
        	headerLine = req.getProtocol() + " " + SC_INTERNAL_SERVER_ERROR + " INTERNAL SERVER ERROR";
        if(httpStatus == SC_MOVED_TEMPORARILY)
        	headerLine = req.getProtocol() + " " + SC_MOVED_TEMPORARILY + " REDIRECT";
        
        output.println(headerLine);
        
        //Handle cookies
        setupCookieHeaders(output);
        
        for(String _key : headMap.keySet())
        	output.println(_key + ": " + headMap.get(_key));
       
        htmlWriter.flush();
        
        //System.out.println("HtmlOutput: " + htmlOutput.toString());
        
        if(!headMap.containsKey("Content-Length")){
        	output.println("Content-Length: " + htmlOutput.toString().length());
        }
        
        output.println();
        //System.out.println("Buffer: " + baos.toString());
        output.print(htmlOutput.toString());
        output.flush();
        isCommitted = true;

	}

	@Override
	public int getBufferSize() {
		return htmlOutput.size();
	}

	@Override
	public String getCharacterEncoding() {
		return "ISO-8859-1";
	}

	@Override
	public String getContentType() {
		return contentType;
	}

	@Override
	public Locale getLocale() {
		return loc;
	}

	@Override
	public ServletOutputStream getOutputStream() throws IOException { return null; }

	@Override
	public PrintWriter getWriter() throws IOException {
		//return new PrintWriter(output); //Need to check if this actually works
		return htmlWriter;
	}

	@Override
	public boolean isCommitted() {
		return isCommitted;
	}

	@Override
	public void reset() {
		headMap.clear();
		htmlOutput = new ByteArrayOutputStream();
		setStatus(SC_OK);
		isCommitted = false;
	}

	@Override
	public void resetBuffer() {
		htmlOutput = new ByteArrayOutputStream();
	}

	@Override
	public void setBufferSize(int _bufSize) {
		//System.out.println("Call to setBufferSize");
		//htmlOutput.
		//resp.setLength(_bufSize);
		return; //Coudln't find instance when this is called
	}

	@Override
	public void setCharacterEncoding(String arg0) { return; }

	@Override
	public void setContentLength(int _contentLength) {
		addIntHeader("Content-Length", _contentLength);
	}

	@Override
	public void setContentType(String _contentType) {
		contentType = _contentType;
	}

	@Override
	public void setLocale(Locale _loc) {
		loc = _loc;
	}
}

package edu.upenn.cis.bang.appserver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.security.Principal;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Map;
import java.util.Vector;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletInputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

public class HttpRequest implements HttpServletRequest {

	public static final String BASIC_AUTH = "BASIC";
	public static final String FORM_AUTH = "FORM";
	public static final String CLIENT_CERT_AUTH = "CLIENT_CERT";
	public static final String DIGEST_AUTH = "DIGEST";

	private Socket conn;
	private HashMap<String, String> headerParams;
	private String reqType;
	private String httpVersion;
	private String uriQuery;
	private String reqPath;
	private String pathInfo;
	private String servletName;
	
	private HashMap<String, Object> servletAttributes;
	private HashMap<String, String> servletParameters;
	
	private String charEncoding = "ISO-8859-1";
	private Locale locale = null;
	private BufferedReader rd;
	
	//private LinkedList<Cookie> cookieJar;
	private Vector<Cookie> cookieJar;
	private HttpServletSession sess;
	private String reqSid;
	
	public HttpRequest(Socket _conn,
					   HashMap<String, String> _headerParams,
					   String _pathInfo,
					   RequestType _reqType,
					   String _httpVersion,
					   String _uriQuery, //query
					   String _reqPath, //full uri
					   String _servletName //servlet path
					  ){
		
		
		conn = _conn;
		try {
			rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		/**
		if(_reqType == RequestType.POST){
			//parse parameters from reader here
		} else{
			
		}
		**/
		
		headerParams = _headerParams;
		reqType = ((_reqType == RequestType.HEAD) ? "HEAD" : //so much for using an enum
						(_reqType == RequestType.GET) ? "GET" :
						(_reqType == RequestType.POST) ? "POST" :
						"BAD" );
		httpVersion = _httpVersion;
		uriQuery = _uriQuery;
		reqPath = _reqPath;
		servletName = _servletName;
		pathInfo = _pathInfo;
		
		servletAttributes = new HashMap<String, Object>();
		servletParameters = new HashMap<String, String>();
		cookieJar = new Vector<Cookie>();
		//HttpServletSession currSession;
	
		//init sessions
		
        Iterator<String> it = (headerParams.keySet()).iterator();
        while(it.hasNext()) {
                String currKey = it.next().toString();
                
                if(currKey.equals("Session") || currKey.equals("session"))
                                reqSid = headerParams.get(currKey);
                        
                    
                if((currKey.equals("cookie") || currKey.equals("Cookie"))
                 	&& (headerParams.get(currKey).startsWith("session")
                  			|| headerParams.get(currKey).startsWith("Session")
                   		)){
                   	String currVal = headerParams.get(currKey);
                   	String[] sessInfo = currVal.split("=");
                   	reqSid = sessInfo[1];
                   	if (sessInfo[1].contains(";"))
                   		reqSid = reqSid.substring(0, reqSid.indexOf(";"));
                }   
                    
                if((currKey.equals("session") ||
                		currKey.equals("Session") ||
                		currKey.equals("cookie") ||
                		currKey.equals("Cookie"))){
                    //currSession = null; //for an invalid session id
                    if(isRequestedSessionIdValid())
                        sess = (HttpServletSession)Config.activeSessions.get(reqSid); //set it
                    else
                    	sess = null;
                    
                    //System.out.println(Config.activeSessions.keySet().toString());
                    //System.out.println(reqSid);
                    //System.out.println(sess);
                    
                }
                
                    //add cookie
                    if(currKey.equals("cookie") || currKey.equals("Cookie")){
                    	String[] allCookies = (headerParams.get(currKey)).split("; ");
                    	for(int i = 0; i < allCookies.length; i++){
                    		String[] cookieCutter = allCookies[i].split("=");
                    		Cookie oreo = new Cookie(cookieCutter[0], cookieCutter[1]);
                    		cookieJar.add(oreo);
                    	}
                    }
        }	
	}
	
	@Override
	public String getAuthType() {
		return BASIC_AUTH;
	}

	@Override
	public String getContextPath() {
		return "";
	}

	@Override
	public long getDateHeader(String _hdr) {
		if(!headerParams.containsKey(_hdr))
			return -1;
		else{
			String strDate = headerParams.get(_hdr);
			Long l = new Long(strDate);
			return l.longValue();
		}
	}

	@Override
	public String getHeader(String _key) {
		return headerParams.get(_key);
	}

	@Override
	public Enumeration getHeaderNames() {
		return Collections.enumeration(headerParams.keySet());
	}

	@Override
	public Enumeration getHeaders(String _key) {
		if(!headerParams.containsKey(_key))
			return null;
		String value = headerParams.get(_key);
		if(!value.contains(", "))
			return Collections.enumeration(Arrays.asList(new String[]{ value }));

		return Collections.enumeration(Arrays.asList(value.split(", ")));
	}

	@Override
	public int getIntHeader(String _key) {
		String strVal = headerParams.get(_key);
		if(strVal == null)
			return -1;
		Integer n = new Integer(strVal);
		return n.intValue();
	}

	@Override
	public String getMethod() {
		return reqType;
	}

	@Override
	public String getPathTranslated() { return null; }
	
	@Override
	public String getQueryString() {
		return uriQuery;
	}

	@Override
	public String getRemoteUser(){ return null; }

	@Override
	public String getRequestURI() {
		if (!reqPath.contains("\\?"))
			return reqPath;
		else
			return reqPath.substring(0, reqPath.indexOf('?'));
	}

	@Override
	public StringBuffer getRequestURL() {
		return new StringBuffer("http://" + getServerName() + ":" + getServerPort() + getRequestURI());
	}

	@Override
	public Principal getUserPrincipal() { return null; }

	@Override
	public boolean isRequestedSessionIdFromCookie() {
		if(headerParams.containsKey("Cookie"))
			if(headerParams.get("Cookie").startsWith("Session"))
				return true;
		
		return false;
	}

	@Override
	public boolean isRequestedSessionIdFromURL() {
		return headerParams.containsKey("session");
	}

	@Override
	public boolean isRequestedSessionIdFromUrl() { return false; }

	@Override
	public boolean isUserInRole(String arg0) { return false; }

	@Override
	public Object getAttribute(String _attr) {
		return servletAttributes.get(_attr);
	}

	@Override
	public Enumeration getAttributeNames() {
		return Collections.enumeration(servletAttributes.keySet());
	}

	@Override
	public String getCharacterEncoding() {
		return charEncoding;
	}

	@Override
	public int getContentLength() {
		return getIntHeader("Content-Length");
	}

	@Override
	public String getContentType() {
		return getHeader("Content-Type");
	}

	@Override
	public ServletInputStream getInputStream() throws IOException { return null; }

	@Override
	public String getLocalAddr() {
		return conn.getLocalAddress().toString();
	}

	@Override
	public String getLocalName() { return null; }

	@Override
	public int getLocalPort() {
		return conn.getLocalPort();
	}

	@Override
	public Locale getLocale() {
		return locale;
	}

	public void setLocale(Locale _locale){
		locale = _locale;
	}
	
	@Override
	public Enumeration getLocales() { return null; }

	@Override
	public String getParameter(String _key) {
		return servletParameters.get(_key);
	}
	
	public void setParameter(String _key, String _value){
		servletParameters.put(_key, _value);
	}

	@Override
	public Map getParameterMap() {
		return ((servletParameters.size() > 0) ? servletParameters : null);
	}

	@Override
	public Enumeration getParameterNames() {
		return ((servletParameters.size() > 0) ? Collections.enumeration(servletParameters.keySet()) : null);
	}

	@Override
	public String[] getParameterValues(String _key) {
		if(!servletParameters.containsKey(_key))
			return null;
		return new String[]{ servletParameters.get(_key) };
	}

	@Override
	public String getProtocol() {
		return httpVersion;
	}

	public void setProtocol(String _httpVersion){
		httpVersion = _httpVersion;
	}
	
	@Override
	public BufferedReader getReader() throws IOException {
		return rd;
	}
	
	public void setReader(InputStream stream){
		rd = new BufferedReader(new InputStreamReader(stream));
	}

	@Override
	public String getRemoteAddr() {
		return conn.getRemoteSocketAddress().toString();
	}

	@Override
	public String getRemoteHost() {
		return getHeader("User-Agent");
	}

	@Override
	public int getRemotePort() {
		return conn.getPort();
	}

	@Override
	public RequestDispatcher getRequestDispatcher(String arg0) { return null; }

	@Override
	public String getScheme() { return "http"; }

	@Override
	public String getServerName() {
		String svrName = getHeader("Host");
		if (svrName != null)
			return svrName.substring(0, svrName.indexOf(':'));
		return null;
	}

	@Override
	public int getServerPort() {
		String svr = getHeader("Host");
		if(svr != null){
			String port = svr.substring(svr.indexOf(':') + 1, svr.length());
			return new Integer(port).intValue();
		}
		return -1;
	}

	@Override
	public boolean isSecure() { return false; }
	
	@Override
	public void removeAttribute(String _key) {
		servletAttributes.remove(_key);
	}

	@Override
	public void setAttribute(String _key, Object _value) {
		servletAttributes.put(_key, _value);
	}

	@Override
	public void setCharacterEncoding(String _charEncoding)
			throws UnsupportedEncodingException {
		charEncoding = _charEncoding;

	}

	@Override
	public String getRealPath(String arg0) { return null; }

	@Override
	public boolean isRequestedSessionIdValid() {
		return Config.activeSessions.containsKey(reqSid);
	}

	@Override
	public String getRequestedSessionId() {
		return reqSid;
	}

	@Override
	public String getServletPath() {
		return servletName;
	}

	@Override
	public HttpSession getSession() {

		//Create a new one if we don't have one
		if((sess == null) || (!sess.isValid())) 
			return getSession(true);
		
		sess.setNew(false);
		Date now = new Date();
		long inactiveCtr = sess.getLastAccessedTime() + (1000 * sess.getMaxInactiveInterval());
		
		if(inactiveCtr < now.getTime()){
			sess.invalidate();
			sess = new HttpServletSession();
			
			Cookie chipsAhoy = new Cookie("Session", sess.getId());
			cookieJar.add(chipsAhoy);
			headerParams.put("Cookie", "Session=" + sess.getId());
			
			reqSid = sess.getId();
			return sess;
		}
		
		sess.setLastAccessedTime(now.getTime());
		return sess;		
	}

	@Override
	public HttpSession getSession(boolean _isNew) {
		boolean isSessionAlive = ((sess != null) && (sess.isValid())); 
		
		if (_isNew && !isSessionAlive){
			sess = new HttpServletSession();
			Cookie figNewton = new Cookie("Session", sess.getId());
			cookieJar.add(figNewton);
			headerParams.put("Cookie", "Session=" + sess.getId());
			reqSid = sess.getId();
			
			return sess;
		}
		
		if(!_isNew && !isSessionAlive){
			sess = null;
			reqSid = null;
			return sess;
		}
		
		if(!_isNew && isSessionAlive)
			return getSession();
		
		return sess;
	}

	@Override
	public String getPathInfo() {
		return pathInfo;
	}

	@Override
	public Cookie[] getCookies() {
		return (Cookie[])cookieJar.toArray(new Cookie[0]);
	}
	
	public HttpServletSession getSessionWithoutUpdate(){
		return sess;
	}
}

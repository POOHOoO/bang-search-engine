package edu.upenn.cis.bang.appserver;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Set;
import java.util.Vector;

import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

public class HttpServletContext implements ServletContext {

	private String name;
	private HashMap<String, Object> attrMap;
	private HashMap<String, String> parmMap;
	
	public HttpServletContext(String _contextName){
		name = _contextName;
		attrMap = new HashMap<String, Object>();
		parmMap = new HashMap<String, String>();
	}
	
	@Override
	public Object getAttribute(String _attrKey) {
		return attrMap.get(_attrKey);
	}

	@Override
	public Enumeration getAttributeNames() {
		return ((new Vector<String>(attrMap.keySet())).elements());
	}

	@Override
	public ServletContext getContext(String arg0) {
		//Since there's only 1 context, return itself??
		return this;
	}

	@Override
	public String getInitParameter(String _parm) {
		return parmMap.get(_parm);
	}
	
	public void setParm(String _key, String _value){
		parmMap.put(_key, _value);
	}

	@Override
	public Enumeration getInitParameterNames() {
		return ((new Vector<String>(parmMap.keySet())).elements());
	}

	//No server info yet?
	@Override
	public String getRealPath(String _path) {
		return("http://localhost:8080" + _path);
	}

	@Override
	public String getServerInfo() {
		return "ZServer 0.2 HTTP/Java Server";
	}

	@Override
	public void removeAttribute(String _key) {
		attrMap.remove(_key);
	}

	@Override
	public void setAttribute(String _key, Object _value) {
		attrMap.put(_key, _value);
	}
	
	public String getServletContextName() {
		return name;	
	}

	public int getMajorVersion() { return 2; }
	public int getMinorVersion() { return 4; }
	
	public String getMimeType(String arg0) { return null; }
	public RequestDispatcher getNamedDispatcher(String arg0) { return null; }
	public RequestDispatcher getRequestDispatcher(String arg0) { return null; }
	public URL getResource(String arg0) throws MalformedURLException { return null; }
	public InputStream getResourceAsStream(String arg0) { return null; }
	public Set getResourcePaths(String arg0) { return null; }
	public Servlet getServlet(String arg0) throws ServletException { return null; }
	public Enumeration getServletNames() { return null; }
	public Enumeration getServlets() { return null; }
	public void log(String arg0) { return; }
	public void log(Exception arg0, String arg1) { return; }
	public void log(String arg0, Throwable arg1) { return; }

}

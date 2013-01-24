package edu.upenn.cis.bang.appserver;

import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.UUID;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionContext;

public class HttpServletSession implements HttpSession {

	private HashMap<String, Object> attrMap;
	private long creationTime;
	private String sid;
	private long lastAccessed;
	private int maxInactive;
	private boolean isValid;
	private boolean isNew;
	
	public HttpServletSession(){
		attrMap = new HashMap<String, Object>();
		isValid = true;
		isNew = true;
		Date now = new Date();
		creationTime = lastAccessed = now.getTime();
		sid = UUID.randomUUID().toString();
		
		String timeoutAttr = (String)Config.context.getAttribute("session-timeout");
		
		//Default 3 hours
		maxInactive = (timeoutAttr == null) ?
				(60 * 60 * 3) :
					(60 * (new Integer(timeoutAttr.toString()).intValue()));
		
		Config.activeSessions.put(sid, this);
	}
	
	@Override
	public Object getAttribute(String _key) {
		return attrMap.get(_key);
	}

	@Override
	public Enumeration getAttributeNames() {
		return Collections.enumeration(attrMap.keySet());
	}

	@Override
	public long getCreationTime() {
		return creationTime;
	}

	@Override
	public String getId() {
		return sid;
	}

	@Override
	public long getLastAccessedTime() {
		return lastAccessed;
	}
	
	public void setLastAccessedTime(long _lastAccessed){
		lastAccessed = _lastAccessed;
	}

	@Override
	public int getMaxInactiveInterval() {
		return maxInactive;
	}

	@Override
	public ServletContext getServletContext() {
		return Config.context;
	}

	@Override
	public HttpSessionContext getSessionContext() { return null; }

	@Override
	public Object getValue(String arg0) { return null; } //Deprecated

	@Override
	public String[] getValueNames() { return null; } //Deprecated

	@Override
	public void invalidate() {
		attrMap.clear();
		isValid = false;
		Config.activeSessions.remove(sid);
	}

	@Override
	public boolean isNew() {
		return isNew;
	}
	
	public void setNew(boolean _val){
		isNew = _val;
	}

	@Override
	public void putValue(String arg0, Object arg1) { return; } //Deprecated

	@Override
	public void removeAttribute(String _key) {
		attrMap.remove(_key);
	}

	@Override
	public void removeValue(String arg0) { return; } //Deprecated

	@Override
	public void setAttribute(String _key, Object _value) {
		attrMap.put(_key, _value);
	}

	@Override
	public void setMaxInactiveInterval(int _max) {
		maxInactive = _max;
	}
	
	public boolean isValid(){
		//return Config.activeSessions.containsValue(this);
		return isValid;
	}

}

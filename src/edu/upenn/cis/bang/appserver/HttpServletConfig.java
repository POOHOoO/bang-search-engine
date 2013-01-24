package edu.upenn.cis.bang.appserver;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Vector;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;

public class HttpServletConfig implements ServletConfig {

	private String servletName;
	private HashMap<String, String> initParams;
	
	public HttpServletConfig(String _servletName){
		servletName = _servletName;
		initParams = new HashMap<String, String>();
	}
	
	public void setParm(String _key, String _value){
		initParams.put(_key, _value);
	}
	
	@Override
	public String getInitParameter(String _parmKey) {
		return initParams.get(_parmKey);
	}

	@Override
	public Enumeration getInitParameterNames() {
        return((new Vector<String>(initParams.keySet())).elements());
	}

	@Override
	public ServletContext getServletContext() {
		return Config.context; //Yeah, kind of lame having that static
							   //Probably should have just passed it around
	}

	@Override
	public String getServletName() {
		return servletName;
	}

}

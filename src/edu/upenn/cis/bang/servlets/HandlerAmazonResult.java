package edu.upenn.cis.bang.servlets;

import java.io.PrintWriter;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class HandlerAmazonResult implements Handler {

	HttpServletRequest req;
	HttpServletResponse resp;
	
	Map<String, String> parameters;
	
	public HandlerAmazonResult(HttpServletRequest _req, HttpServletResponse _resp){
		req = _req;
		resp = _resp;
		
		parameters = req.getParameterMap();
	}
	public void handleRequest(PrintWriter w) throws BangException{
		
	}
}

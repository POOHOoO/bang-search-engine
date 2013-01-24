package edu.upenn.cis.bang.servlets;

import java.io.PrintWriter;

public interface Handler {
	
	public enum Type{
		MAIN, RESULTS, RESULTS_WITH_YAHOO, RESULTS_WITH_AMAZON, RESULTS_WITH_RANKINGS
	}
	
	public void handleRequest(PrintWriter w) throws BangException;
		
	
}
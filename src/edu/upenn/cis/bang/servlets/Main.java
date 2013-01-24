package edu.upenn.cis.bang.servlets;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.*;

public class Main extends HttpServlet{
	  public void init () throws ServletException {
	  
			BangSpellChecker.initSpellChecker(Data.DICTIONARY_PATH);
	  }

	  public void doGet (HttpServletRequest req, HttpServletResponse res)
	    throws ServletException, IOException {

		  doRequest(req, res);
		  
	  }

	  public void doPost (HttpServletRequest req, HttpServletResponse res)
	    throws ServletException, IOException {

		  doRequest(req, res);

	  }
	  
	  public void doRequest (HttpServletRequest req, HttpServletResponse res)
	    throws ServletException, IOException {
		  	res.setContentType("text/html");
		    PrintWriter w = res.getWriter();
		  
		    String pathInfo = req.getPathInfo();
		    Map<String, String> params = req.getParameterMap();
		    Handler.Type handlerType = getHandlerType(pathInfo, params);
		    Handler handler = null;
		    
		    try {
		    	handler = getHandlerInstance(handlerType, req, res, w);
				handler.handleRequest(w);
			} catch (BangException bangError) {
				logError(bangError);
				handleError();
			} finally {		    		    
				w.flush(); // Commits the response
				w.close();
			}		  
	  }
		    	  
	  private void handleError() {
		//TODO
	  }

	private void logError(BangException bangError) throws IOException {
			bangError.printStackTrace();
			File logFile = new File(Data.LOG_FILE_PATH + File.separator
					+ Data.LOG_FILE_PREFIX
					+ (new SimpleDateFormat("yyyy.MM.dd_hh.mm.ss")).format(new Date())
					+ "."
					+ Data.LOG_FILE_EXT);
			
			if(logFile.createNewFile()){
				PrintWriter pw = new PrintWriter(new FileWriter(logFile));
				pw.print(bangError.toString());
				pw.flush();
				pw.close();
			}		
	  }

	private Handler getHandlerInstance(Handler.Type handlerType, HttpServletRequest req,
			HttpServletResponse res, PrintWriter w) throws BangException, IOException {

		  	Handler handler;
		    switch(handlerType){
		    case MAIN:
		    	handler = new HandlerIndex(req, res);
		    	break;
		    case RESULTS:
		    	handler = new HandlerResult(req, res);
		    	break;
		    case RESULTS_WITH_AMAZON:
		    	handler = new HandlerAmazonResult(req, res);
		    	break;
		    case RESULTS_WITH_YAHOO:
		    	handler = new HandlerYahooResult(req, res);
		    	break;
		    case RESULTS_WITH_RANKINGS:
		    	handler = new HandlerRankingsResult(req, res);
		    	break;
		    default:
		    	handler = new HandlerIndex(req, res); 
		    	break;
		    }

		    return handler;
	  }

	  private Handler.Type getHandlerType(String pathInfo, Map<String, String> params) {
		  //This code doesn't really do anything anymore but I want to keep it here
		  //because it works.  The default results handler will be called for both
		  //YAHOO and AMAZON.  It handles them.
		  if(pathInfo == null)
		    	return Handler.Type.MAIN;
		  if(pathInfo.endsWith(Data.PATH_SEARCH_SUBMIT)){
			  return Handler.Type.RESULTS;
			  /**
			  if(params.containsKey(Data.SEARCH_MODE_PARAM_KEY)){
				  String searchMode = params.get(Data.SEARCH_MODE_PARAM_KEY);
				  if(searchMode.equals(Data.SEARCH_MODE_YAHOO))
					  //return Handler.Type.RESULTS_WITH_YAHOO;
					  return Handler.Type.RESULTS;
				  if(searchMode.equals(Data.SEARCH_MODE_AMAZON))
					 // return Handler.Type.RESULTS_WITH_AMAZON;
					  return Handler.Type.RESULTS;
				  if(searchMode.equals(Data.SEARCH_MODE_RANKINGS))
					  return Handler.Type.RESULTS_WITH_RANKINGS;
				  if(searchMode.equals(Data.SEARCH_MODE_NORMAL))
					  return Handler.Type.RESULTS;
			  } else
				  return Handler.Type.RESULTS;	**/			  
		  }		  
		  return Handler.Type.MAIN;
	}

	public void destroy () {
	    //TODO
	  }

	  public String getServletInfo () {
	    return "Bang Search Engine Servlet 1.0";
	  }
	
}

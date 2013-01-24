package edu.upenn.cis.bang.appserver;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.Reader;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLDecoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Queue;
import java.util.Scanner;
import java.util.Vector;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 
 * @author Zach Zarrow (zzarrow)
 * 
 * Each Worker thread carries out a server request by reading requests
 * from the dispatcher's queue.
 *
 */
public class Worker extends Thread {
	
	Queue<Socket> queue;
	private boolean isRunning = false;
	
	public static void init(Vector<Worker> workers, Queue<Socket> threadPool, int threads){	
		for(int i = 0; i < threads; i++){
			Worker w = new Worker();
			workers.addElement(w);
			w.setQueue(threadPool);
			w.start();
		}
	}
	
	public void setQueue(Queue<Socket> _queue){
		queue = _queue;
	}
	
	private String getMimeType(String path){
		
		//support directory listing
		if ((new File(path)).isDirectory())
			return ("text/html");
		
		if(path.endsWith(".txt"))
			return ("text/plain");
		if(path.endsWith(".html") || path.endsWith(".htm"))
			return ("text/html");
		if(path.endsWith(".jpg") || path.endsWith(".jpeg"))
			return ("image/jpeg");
		if(path.endsWith(".png"))
			return ("image/png");
		
		//If not, let's guess octet-stream for now
		return ("application/octet-stream");
	}
	
	private String getDirectoryListingHTML(String path){
		
		//Known insignificant bug here - See README
		File f = new File(path);
		File[] filesInDir = f.listFiles();
		
		String files = "";
		for(int i = 0; i < filesInDir.length; i++){
			if(path.endsWith("/"))
				files += "<tr><td><font face=\"Arial\">" +
					"<a style=\"underline:none;\"href=\"" + filesInDir[i].getName() + "\">"
					+ filesInDir[i].getName()
					+ "</a></font></td>" 
					+ "<td><font face = \"Arial\">" + filesInDir[i].length() + "</font></td>"
					+ "<td><font face = \"Arial\">" + (new java.util.Date(filesInDir[i].lastModified())).toString() + "</font></td></tr>";
			else
				files += "<tr><td><font face=\"Arial\">" +
					"<a style=\"underline:none;\"href=\"" + (filesInDir[i].getParent()).replace( Config.HTDOCS_PATH, "") + "/"+ filesInDir[i].getName() + "\">"
					+ filesInDir[i].getName()
					+ "</a></font></td>" 
					+ "<td><font face = \"Arial\">" + filesInDir[i].length() + "</font></td>"
					+ "<td><font face = \"Arial\">" + (new java.util.Date(filesInDir[i].lastModified())).toString() + "</font></td></tr>";
		}
		
		String header =  "<html><head><title>Directory Listing of " + f.getName() + "</title></head>" +
				"<body><center><h3><b><font face = \"Arial\">Directory Listing of " + f.getName() +
				"</font></b></h3><br /><hr><br />" +
				"<table><tr><td><b>Name</b></td><td><b>Size</b></td><td><b>Last Modified</b></td>" +
				"</tr>";
		
		String footer = "</table><br /><br /><hr><font face = \"Arial\">" +
				"<i>Generated at "
				+ (new java.sql.Timestamp(
						java.util.Calendar.getInstance().getTime().getTime())).toString()
						+ "<br />ZServer 0.1 by Zach Zarrow<br />CIS-455 Homework 1"
						+ "</i></font></center></body></html>";
		
		return header + files + footer;
	}
	
	private long getContentLength(String path){
		if((new File(path).isDirectory()))
				return getDirectoryListingHTML(path).length();
		
		return((new File(path)).length());
	}
	
	/**
	 * 
	 * @param output
	 * @param reqType
	 * @param path
	 * @param http11
	 * @return -1 on error, meaning do not write the content of the page
	 * @throws IOException
	 */
	private int sendHeader(PrintStream output, RequestType reqType, String path, String request) throws IOException{
		//GlobalLog.getLogger().info("Sending header.");
		
		String httpVersion = "1.0";
		if (request.contains("HTTP/1.1"))
			httpVersion = "1.1";
		
		File f = new File(path);
		
		//Handle If-Modified-Since and If-Unmodified-Since for HTTP/1.1
		if(request.contains("If-Modified-Since:") || request.contains("If-Unmodified-Since:")){
			//Parse out date
			//Compare to modified date of file
			//if modified date of file > requested date
				//we can ignore this header and proceed as usual
			//Else, return HTTP/1.1 304 Not Modified, then Date, then blank line
			
			int start = request.indexOf("If-Modified-Since:") + 18;
			if(start == -1)
				start = request.indexOf("If-Unmodified-Since:") + 20;
			int end = request.indexOf('\n', start);
			
			String reqDateStr = request.substring(start, end).trim();
			String formatString = "";
			//3 possible formats: determine which one
			if(reqDateStr.indexOf(' ') == 4)
				//Fri, 31 Dec 1999 23:59:59 GMT;
				formatString = "EEE, d MMM yyyy HH:mm:ss z";
			else if(reqDateStr.indexOf(' ') == 7)
				//Friday, 31-Dec-99 23:59:59 GMT;
				formatString = "EEEEE, d-MMM-yy HH:mm:ss z";
			else if(reqDateStr.indexOf(' ') == 3)
				//Fri Dec 31 23:59:59 1999;
				formatString = "EEE MMM dd HH:mm:ss yyyy";
			//Else there's a date error, so ignore this header
			
			if(!formatString.equals("")){
				try {
					Date reqDate = (new SimpleDateFormat(formatString)).parse(reqDateStr);
					Date fileDate = new Date(f.lastModified());
					if(request.contains("If-Modified-Since:"))
						//If request date is before file date, we want to send
						if (!(reqDate.before(fileDate))){
							//Don't send the page; send a 304
							//GlobalLog.getLogger().info("Sending 304 Not Modified");
							output.append("HTTP/1.1 304 Not Modified\n");
							output.append("Date: " + (new SimpleDateFormat(Config.DATE_FORMAT)).format(new Date()) + "\n\n");
							return -1;
						}
					if(request.contains("If-Unmodified-Since:"))
						//If request date is after file date, we want to send
						if (!(reqDate.after(fileDate))){
							//GlobalLog.getLogger().info("Sending 412 Precondition Failed");
							output.append("HTTP/1.1 412 Precondition Failed\n\n");
							return -1;
						}
						
				} catch (ParseException e) {
					// TODO Auto-generated catch block
					//GlobalLog.getLogger().error("Date formatting error: " + e.getMessage());
				}
			}
		
		}
		
		String footer = "<br /><hr><center><font face = \"Arial\">" +
		"<i>Generated at "
		+ (new java.sql.Timestamp(
				java.util.Calendar.getInstance().getTime().getTime())).toString()
				+ "<br />ZServer 0.1 by Zach Zarrow<br />CIS-455 Homework 1"
				+ "</i></font></center></body></html>";
		
		//More HTTP/1.1 support
		if((reqType == RequestType.BAD) && ((request.startsWith("POST")) ||
											(request.startsWith("PUT")) ||
											(request.startsWith("DELETE")) ||
											(request.startsWith("OPTIONS")) ||
											(request.startsWith("TRACE")))){
			String htmlMessage = "<html><body><h1><font face = \"Arial\">501: Not Implemented</font></h1>" +
			 "<hr><p><ul><li><font face = \"Arial\">" +
			 "This server does not support the type of request your browser made.</li>" +
			 "<li>If the problem persists, please try using a" +
			 "different browser.</font></li></ul></p>" +
			 "<br /><br /><br /><br /><br /><br />" +
			 "</body></html>" + footer;

			output.append("HTTP/" + httpVersion + " 501 Not Implemented\n");
			output.append("Date: " + (new SimpleDateFormat(Config.DATE_FORMAT)).format(new Date()) + "\n");
			output.append("Connection: close\n");
			if(reqType != RequestType.HEAD){
				output.append("Content-Type: text/html\n");
				output.append("Content-Length: " + htmlMessage.length() + "\n");
				output.append("\n");

				output.append(htmlMessage);
			}
			//GlobalLog.getLogger().warn("501 Not Implemented");
			return -1;	
			
		} else if ((reqType == RequestType.BAD) || !(request.contains(" HTTP/1.")) || (path.contains("..")) || (path.startsWith("/"))){
			String htmlMessage = "<html><body><h1><font face = \"Arial\">400: Bad Request</font></h1>" +
								 "<hr><p><ul><li><font face = \"Arial\">" +
								 "There was a problem processing your request.  Please try agin.</li>" +
								 "<li>If the problem persists, please try using a" +
								 "different browser.</font></li></ul></p>" +
								 "<br /><br /><br /><br /><br /><br />" +
								 "</body></html>" + footer;
			
			output.append("HTTP/" + httpVersion + " 400 Bad Request\n");
			output.append("Date: " + (new SimpleDateFormat(Config.DATE_FORMAT)).format(new Date()) + "\n");
			output.append("Connection: close\n");
			if(reqType != RequestType.HEAD){
				output.append("Content-Type: text/html\n");
				output.append("Content-Length: " + htmlMessage.length() + "\n");
				output.append("\n");
			
				output.append(htmlMessage);
			}
			//GlobalLog.getLogger().warn("400 Bad Request");
			return -1;
		} else if (!f.exists()){
			String htmlMessage = "<html><body><h1><font face = \"Arial\">404: Not Found</font></h1>" +
			 "<hr><p><ul><li><font face = \"Arial\">" +
			 "The requested resource was not found on this server.</li></font></ul></p>" +
			 "<br /><br /><br /><br /><br /><br />" +
			 "</body></html>" + footer;
			output.append("HTTP/" + httpVersion + " 404 Not Found\n");
			output.append("Date: " + (new SimpleDateFormat(Config.DATE_FORMAT)).format(new Date()) + "\n");
			output.append("Connection: close\n");
			if(reqType != RequestType.HEAD){
				output.append("Content-Type: text/html\n");
				output.append("Content-Length: " + htmlMessage.length() + "\n");
				output.append("\n");
			
				output.append(htmlMessage);
			}
			//GlobalLog.getLogger().warn("404 Not Found for path " + path);
			return -1; //error state
		} else if (!f.canRead()){
			String htmlMessage = "<html><body><h1><font face = \"Arial\">403: Forbidden</font></h1>" +
			 "<hr><p><ul><li><font face = \"Arial\">" +
			 "The server does not have access to the specified resource.</li></font></ul></p>" +
			 "<br /><br /><br /><br /><br /><br />" +
			 "</body></html>" + footer;
			output.append("HTTP/" + httpVersion + " 403 Forbidden\n");
			output.append("Date: " + (new SimpleDateFormat(Config.DATE_FORMAT)).format(new Date()) + "\n");
			output.append("Connection: close\n");
			if(reqType != RequestType.HEAD){
				output.append("Content-Type: text/html\n");
				output.append("Content-Length: " + htmlMessage.length() + "\n");
				output.append("\n");
			
				output.append(htmlMessage);
			}
			//GlobalLog.getLogger().warn("403 Forbidden for path " + path);
			return -1; //error state
		} else {
			if(httpVersion.equals("1.1"))
				output.append("HTTP/1.1 100 Continue\n\n");
			output.append("HTTP/" + httpVersion + " 200 OK\n");
			output.append("Date: " + (new SimpleDateFormat(Config.DATE_FORMAT)).format(new Date()) + "\n");
			output.append("Connection: close\n");
		}
		
		output.append("Content-Type: " + getMimeType(path) + "\n");
		output.append("Content-Length: " + getContentLength(path) + "\n");
		
		output.append("\n");

		return 0; //indicates success
	}
	
	private void sendData(PrintStream output, RequestType reqType, String reqPath) throws IOException {
		//GlobalLog.getLogger().info("Sending data.");	
		File reqFile = new File(reqPath); //At this point, should be valid.
		if(reqFile.isDirectory()){
			output.append(getDirectoryListingHTML(reqPath));
			return;
		}
		
		InputStream in = new FileInputStream(reqFile);
		
		if (reqFile.length() < 1024){
			byte[] b = new byte[1]; //1 byte at a time
			while(in.read(b, 0, 1) != -1)
				output.write(b);
		} else{
			byte[] b = new byte[1024]; //1MB at a time
			while(in.read(b, 0, 1024) != -1)
				output.write(b);
		}
	}
	
	private void doServletRequest(Socket conn, String request, String servletName, String pathInfo, String reqPath,
			RequestType reqType, String uriParms, PrintStream output) {
		
		if(reqType == RequestType.BAD)
			return;
		
		//We have a mapping of servlet name to class
		//We have our context built
		//We have our servlet object built
		
		HttpServlet servlet = Config.nameToServlet.get(servletName);
		//Handle cookies

		//Takes all incoming headers, creates map of key to value
		Scanner s = new Scanner(request);
		String httpVersion = s.nextLine();
		httpVersion = httpVersion.substring(httpVersion.length() - 9, httpVersion.length()).trim(); //all we need from first line is HTTP version
		HashMap<String, String> headerParams = new HashMap<String,String>();
		
		while(s.hasNextLine()){
			String nextLine = s.nextLine();
			if(nextLine.equals("") || nextLine.equals("\n") || nextLine.equals("\r")) break;
			String curr[] = nextLine.split(":", 2);
			headerParams.put(curr[0].trim(), curr[1].trim());
		}
		
		//input format: asdf?var1=abc&var2=def&var3=ghi
		String[] arrQuery = reqPath.split("\\?");
		String uriQuery = (arrQuery.length > 1) ? arrQuery[1] : ""; //everything to the right of ?
		
		
		//change servlet name
		
        HttpRequest constructedReq = new HttpRequest(conn,
        													headerParams,
        													pathInfo,
        													reqType,
        													httpVersion,
        													uriQuery, //query
        													reqPath, //full uri
        													servletName //servlet path
        													);
		/**  
        if(constructedReq.getMethod().equals("POST")){
        	InputStream in;
			try {
				in = conn.getInputStream();
			
				uriQuery = "";
				char curr = (char) in.read();
				while(curr != -1){
					uriQuery += curr;
					curr = (char) in.read();
				}
			} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
        }**/
    
        if(constructedReq.getMethod().equals("POST")){
        	Scanner s2 = new Scanner(request);
        	String lastLine = "";
        	while(s2.hasNext()){
        		lastLine = s2.nextLine();
        	}
        	uriQuery = lastLine;
        }
        	
        
    	//Will handle GET and POST (POST overwrites uriQuery)
        //uriQuery has something like:
        //a=1&b=2&c=3...
        //So we'll split by the &
        //then split by the =
        //have to handle no value
        String[] byPair = uriQuery.split("&");
        String[] keyToValue;
        for (int i = 0; i < byPair.length; i++){
                keyToValue = byPair[i].split("=");
                if(keyToValue.length == 1)
                	//blank value
                	constructedReq.setParameter(keyToValue[0], "");
                else //check this
                	constructedReq.setParameter(keyToValue[0], URLDecoder.decode(keyToValue[1]));
        }
             
        HttpResponse serverResponse = new HttpResponse(constructedReq, output, conn);
        
        //Do It! (Finally)
        try {
                servlet.service(constructedReq,
                				serverResponse
                				);
                serverResponse.flushBuffer();
                
        } catch(Exception e) {
                try {
					serverResponse.sendError(500, "Internal Server Error");
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
        }
        
        serverResponse.reset();
        
        //System.out.println("Output buffer: " + output.toString());
        
        try {
			conn.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void handleRequest(Socket conn) throws IOException{
		if(conn == null)
			return;
		
		//GlobalLog.getLogger().info(this.toString() + " handling request.");
		
		Reader input = new BufferedReader(new InputStreamReader(conn.getInputStream()));
		PrintStream output = new PrintStream(conn.getOutputStream());
		
		//Read first line of request for parsing
		String request = "";
		//Looping here fixes a bug where the InputStream lags/times out when there is
		//a long time between requests.
		while(true){
			if(!input.ready()){
				//GlobalLog.getLogger().error("Input stream error: Stream not ready.");
				continue;
			}
			else
				for(char curr = (char)input.read();
				input.ready() && (curr != '\n') && (curr != '\r'); // \r just in case
				request += curr, curr = (char)input.read());
			break;
		}
		
		//GlobalLog.getLogger().info("Request of length " + request.length() + " received: " + request);
		
		//Parse first line of request
		RequestType reqType = RequestType.BAD; //If not overwritten, send error later
		String reqPath = "";
		boolean reqData = false; //Just header, or data too?
	
		if(request.length() < 14)
			reqType = RequestType.BAD;
		
		if(!(request.substring(0, request.length() - 1).endsWith("HTTP/1.")))
			reqType = RequestType.BAD;
		else if((request.startsWith("HEAD")) && request.length() >= 15){
			reqType = RequestType.HEAD;
			//Adjust parsing to be HTTP 1.1 compliant for Absolute URLs
			if (request.charAt(5) == '/')
				reqPath = request.substring(6, request.length() - 9); //omit fwd slash
			else
				reqPath = request.substring(5, request.length() - 9);
			reqData = false;
		} else if((request.startsWith("POST")) && request.length() >= 15){
			reqType = RequestType.POST;
			//Adjust parsing to be HTTP 1.1 compliant for Absolute URLs
			if (request.charAt(5) == '/')
				reqPath = request.substring(6, request.length() - 9); //omit fwd slash
			else
				reqPath = request.substring(5, request.length() - 9);
			reqData = false;
		} else if(request.startsWith("GET") && request.length() >= 14){
			reqType = RequestType.GET;
			//Adjust parsing to by HTTP 1.1 compliant for Absolute URLs
			if (request.charAt(4) == '/')
				reqPath = request.substring(5, request.length() - 9); //omit fwd slash
			else
				reqPath = request.substring(4, request.length() - 9);
			reqData = true;
		}
		//expandable to more request types
		
		//Finish reading in request
		for(char curr = (char)input.read();
			input.ready();
			request += curr, curr = (char)input.read());		
		
		//If HTTP/1.1, ensure presence of Host header
		if(request.contains("HTTP/1.1") && !request.contains("Host:")){
			reqType = RequestType.BAD;
			reqPath = "";
			reqData = false;
			//GlobalLog.getLogger().warn("HTTP/1.1 request without a Host header. Responding 400.");
		}
		
		//String servletName = reqPath.substring(0, ((reqPath.indexOf('?') == -1) ? reqPath.length() : reqPath.indexOf('?')));
		String servletName = null;
		String params = null;
		String pathInfo = null;
		for(String s : Config.servletMap.keySet()){
			if (reqPath.startsWith(s))
				servletName = s;
		}
		if(servletName != null){
			if (reqPath.length() > servletName.length()){
				//we get URI params later
				if(!reqPath.contains("?"))
					pathInfo = reqPath.substring(servletName.length(), reqPath.length());
				else
					pathInfo = reqPath.substring(servletName.length(), reqPath.indexOf('?'));
				
			}
		}
		
		if((reqPath.indexOf('?') != -1) && (reqPath.indexOf('?') < (reqPath.length() - 1)))
			params = reqPath.substring(reqPath.indexOf('?') + 1, reqPath.length());
		//if((Config.servletMap.get(reqPath.substring(0, ((reqPath.indexOf('?') == -1) ? reqPath.length() : reqPath.indexOf('?'))))) != null){
		if(servletName != null) {
			doServletRequest(conn, request, servletName, pathInfo, reqPath, reqType, params, output);
		} else{
		
			reqPath = Config.HTDOCS_PATH + reqPath;

			//if (reqType == RequestType.BAD)
				//GlobalLog.getLogger().warn("Final request state is BAD.");
			//GlobalLog.getLogger().info("Request parsed: Path is \"" + reqPath + "\", reqData = " + reqData);
		
			//sendHeader will handle error cases (e.g. reqType = BAD)
			//sendHeader returns -1 on error
			if((sendHeader(output, reqType, reqPath, request) != -1) && reqData)
				sendData(output, reqType, reqPath);
		
		output.flush();
		
		//GlobalLog.getLogger().info("Closing connection.");
		
		conn.close();
		}
	}

	public void run() {
		super.run();
		isRunning = true;
		while(isRunning ){
			//synchronized(queue){
				if(queue.isEmpty()){}
					/**
					try {
						queue.wait();
					} catch (InterruptedException e) {
						GlobalLog.getLogger().error(e.getMessage() + e.getStackTrace().toString());
						e.printStackTrace();
					}**/
				else{
					//Handle request
					try {
						//System.out.println("HANDLE REQUEST");
						handleRequest(queue.poll());
					} catch (IOException e) {
						//GlobalLog.getLogger().error(e.getMessage());
						e.printStackTrace();
					}
				}
				
			//}
			
			/**
			if (goToSleep)
				try {
					this.wait();
				} catch (InterruptedException e) {
					GlobalLog.getLogger().error(e.getMessage());
				}
			
			**/
			
		}
		
	}

	public static void stopAll(Vector<Worker> workers) {
		Iterator<Worker> it = workers.iterator();
		Worker curr;
		while(it.hasNext()){
			curr = it.next();
			curr.killWorker();
		}
		//Kill servlets
		for(HttpServlet s : Config.nameToServlet.values())
			s.destroy();
	}

	private void killWorker() {
		isRunning = false;
	}
}

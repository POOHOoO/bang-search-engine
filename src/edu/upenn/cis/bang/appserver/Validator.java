package edu.upenn.cis.bang.appserver;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;

/**
 * 
 * @author Zach Zarrow (zzarrow)
 * 
 * Contains static validator methods used by the web server.
 *
 */
public class Validator {
	public static boolean isValidPath(String _path){
		if ((new File(_path)).isDirectory())
				//&& (!(_path.contains(".."))) // REMOVED This CAN be legitimate, but I'm
											 //making the security design decision
											 //to disallow it... there is no legitimate
											 //need for this.
				//&& (_path.charAt(0) != '/'))
			//Then it's valid
			return true;
		
		return false;
	}
	
	/**
	 * 
	 * @param strPort
	 * 	The port input by the user, in string representation.
	 * @return
	 *  True if the string is a valid port number
	 *  False if not
	 */
	public static boolean isValidPort(String _strPort){
		try{
			int port = Integer.parseInt(_strPort.trim());
			if((port < 0) || (port > 65535)) //Valid = 0 to 65535
				return false;
		} catch (NumberFormatException e){
			return false;
		}
		
		try{
			ServerSocket sock = new ServerSocket(Config.PORT_NUM);
			sock.close();
		} catch (IOException e){
			return false;
		}
		
		return true;
	}
	
	/**
	 * 
	 * @param args
	 *  The arguments the user passed to the server
	 * @return
	 *  True if the args are correct and we can initialize the server
	 *  False if we need to exit
	 */
	public static boolean validate(String[] _args){
		if (_args.length == 0){
			System.out.println("Zach Zarrow (zzarrow)");
			return false;
		}
		
		if (_args.length != 3){
			//Invalid (for milestone 2)
			System.out.println("Usage:\n\tjava HttpServer [PORT_NUMBER] [HTDOCS_PATH] [WEB.XML_PATH]");
			System.out.println("Or, to get author name and PennKey:\n\tjava HttpServer\n");
			return false;
		} else{
			if(!isValidPort(_args[0])){
				System.out.println("Error: Port " + _args[0] + " is not a valid port.");
				return false;
			}
			if(!isValidPath(_args[1])){
				System.out.println("Error: Path " + _args[1] + " is not a valid path.");
				return false;
			}
			if((!(new File(_args[2])).exists()) || (!(new File(_args[2])).canRead())){
				System.out.println("Error: File " + _args[2] + " is not a valid file.");
				return false;
			}
		}
		
		//If we reach here, the input is valid
		return true;
		
	}
	
}

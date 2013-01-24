package edu.upenn.cis.bang.pagerank;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class PageRankStore {

	public static void printUsage(){
		System.out.println("Usage <pagelinks file path> <indexer DNS> <index port>");
	}

	public static void main(String[] args){

		if (args.length < 3 || args.length > 3){
			printUsage();
			System.exit(0);
		}

		try{
			// Open the file that is the first 
			// command line parameter
			FileInputStream fstream = new FileInputStream(args[0]);
			// Get the object of DataInputStream
			DataInputStream in = new DataInputStream(fstream);
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			String line;
			
			int port = Integer.parseInt(args[2]);
			
			Socket socket = new Socket(args[1],port);
			PrintWriter out = new PrintWriter(socket.getOutputStream(),true);
			
			out.println("<type>setpagerank</type>");
			out.println("<data>");
			
			//StringBuffer sb = new StringBuffer();
			//Read File Line By Line
			while ((line = br.readLine()) != null)   {
				String[] split = line.split(" ");
				String urlKey = split[0].trim();
				double pageRank = 0.0;
				try {
					pageRank = Double.parseDouble(split[1]);
				} catch (NumberFormatException e){
					e.printStackTrace();
					continue;
				}
				out.print(urlKey + " "+pageRank+",");
				//sb.append(urlKey + " "+pageRank+",");
				System.out.print(urlKey + " "+pageRank+",");
			}
			//System.out.println(sb.substring(0, sb.length()-1));
			
			//out.println(sb.substring(0, sb.length()-1));
			out.print("\n");
			out.println("</data>");
			out.println();
			out.flush();
			out.println();
			out.flush();
			
			out.close();
			in.close();
		}catch (Exception e){
			System.err.println("Error: " + e.getMessage());
		}

	}
}

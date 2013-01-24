package edu.upenn.cis.bang.indexer;

import java.io.Serializable;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

public class Links implements Serializable {


	private static final long serialVersionUID = 1L;
	public Set<Link> link_set = new HashSet<Link>();
	
	
	public Links(){
		link_set = new HashSet<Link>();
	}
	

	public class Link implements Serializable{
	
		private static final long serialVersionUID = 1L;
		public String name = null;
		public String tf = null;
		
		
		@Override 
		public boolean equals(Object aThat) {
			if (((Link)aThat).name.equals(this.name)){
				  return true;
				  }
			else{
				return false;
				}
			  
		  }
		  
		  @Override
		  public int hashCode() {  
			    return  this.name.hashCode();
			  }
		
		
	}
	
	
}

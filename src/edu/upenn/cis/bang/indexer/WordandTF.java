package edu.upenn.cis.bang.indexer;

import java.util.HashSet;
import java.util.Set;

public class WordandTF {
	String word = "";
	String tf = "";
	
	WordandTF(String word, String tf){
		this.word = word;
		this.tf = tf;
	}
	
  @Override 
  public String toString() {
	  return this.word+"###"+this.tf;
	  }
  
  
  @Override 
  public boolean equals(Object aThat) {
	  if (((WordandTF)aThat).word.equals(this.word)){
		  return true;
	  }
	  else{
		  return false;
	  }
	  
  }
  
  @Override
  public int hashCode() {  
	    return  this.word.hashCode();
	  }
  
  
  // TEST CODE
  public static void main(String[] args){
	  WordandTF one = new WordandTF("abc","123");
	  WordandTF two = new WordandTF("abc","12321312431");
	  Set<WordandTF> s = new HashSet<WordandTF>();
	  s.add(one);
	  s.add(two);
	  System.out.println(one.equals(two));
	  System.out.println(s);
	  
  }
  
}



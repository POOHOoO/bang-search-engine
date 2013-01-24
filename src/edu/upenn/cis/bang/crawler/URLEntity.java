



import static com.sleepycat.persist.model.DeleteAction.CASCADE;
import static com.sleepycat.persist.model.Relationship.MANY_TO_ONE;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

import com.sleepycat.persist.model.Entity;
import com.sleepycat.persist.model.PrimaryKey;
import com.sleepycat.persist.model.SecondaryKey;
import static com.sleepycat.persist.model.Relationship.*;
import static com.sleepycat.persist.model.DeleteAction.*;

@Entity
public class URLEntity {
	@PrimaryKey         
	private String url;
	private String lastChecked;
	
	public void setURL(String url){
		this.url=url;
	}
	public String getURL(){
		return url;
	}
	
	public void setLastChecked(String date){
		this.lastChecked =date;
	}
	
	public String getLastChecked(){
		return lastChecked;
	}
	
	
}

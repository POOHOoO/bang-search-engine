



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
public class ContentEntity {
	@PrimaryKey         
	private String content;
	private String lastChecked;
	
	public void setContent(String url){
		this.content=content;
	}
	public String getContent(){
		return content;
	}
	
	public void setLastChecked(String date){
		this.lastChecked =date;
	}
	
	public String getLastChecked(){
		return lastChecked;
	}
	
	
}

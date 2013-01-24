



import java.io.File;
import java.net.URL;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;

import com.sleepycat.je.DatabaseException;
import com.sleepycat.persist.EntityCursor;
import com.sleepycat.persist.EntityStore;
import com.sleepycat.persist.PrimaryIndex;
import com.sleepycat.persist.SecondaryIndex;


public class DataAccessor {
	// Index Accessors
	PrimaryIndex<String,URLEntity> urlByID;
	PrimaryIndex<String, ContentEntity> contentByID;
	
	// Open the indices
	public DataAccessor(EntityStore store) throws DatabaseException {

		urlByID = store.getPrimaryIndex(String.class, URLEntity.class);
		contentByID = store.getPrimaryIndex(String.class, ContentEntity.class);
		
	}

	public boolean addUrlEntity(String url, String dateString) {
		URLEntity entity = new URLEntity();
		entity.setURL(url);
		entity.setLastChecked(dateString);
		boolean isNew = urlByID.putNoOverwrite(entity);
		return isNew;
	}

	public URLEntity getbyURL(String url) {
		return this.urlByID.get(url);
	}

	public ContentEntity getbyContent(String content) {
		return this.contentByID.get(content);
	}

	public boolean addContentEntity(String content, String dateString) {
		ContentEntity entity = new ContentEntity();
		entity.setContent(content);
		entity.setLastChecked(dateString);
		boolean isNew = contentByID.putNoOverwrite(entity);
		return isNew;
		
	}

	public int getTotalURLs() {
		
		int count = 0;
		EntityCursor<URLEntity> sec_cursor =urlByID.entities();
		try {
			for (URLEntity xml : sec_cursor) {
				count++;
			}
		}
		finally{
			sec_cursor.close();
		}
		return count;

	}


}
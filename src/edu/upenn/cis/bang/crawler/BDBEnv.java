


import java.io.File;

import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;

import com.sleepycat.persist.EntityStore;
import com.sleepycat.persist.StoreConfig;

public class BDBEnv {

	private Environment myEnv;
	private EntityStore store;

	// Our constructor does nothing
	public BDBEnv() {}

	// The setup() method opens the environment and store
	// for us.
	public void setup(File envHome, boolean readOnly)
	throws DatabaseException {

		try{
			EnvironmentConfig myEnvConfig = new EnvironmentConfig();
			StoreConfig storeConfig = new StoreConfig();

			myEnvConfig.setReadOnly(readOnly);
			storeConfig.setReadOnly(readOnly);

			// If the environment is opened for write, then we want to be 
			// able to create the environment and entity store if 
			// they do not exist.
			myEnvConfig.setAllowCreate(!readOnly);
			storeConfig.setAllowCreate(!readOnly);

			// Open the environment and entity store
			myEnv = new Environment(envHome, myEnvConfig);
			store = new EntityStore(myEnv, "EntityStore", storeConfig);
		}
		catch(DatabaseException dbe) {
			System.err.println("Error opening environment and store: " +
					dbe.toString());
			System.exit(-1);
		}

	}


// Return a handle to the entity store
public EntityStore getEntityStore() {
	return store;
}

// Return a handle to the environment
public Environment getEnv() {
	return myEnv;
}

// Close the store and environment.
public void close() {
	if (store != null) {
		try {
			store.close();
		} catch(DatabaseException dbe) {
			System.err.println("Error closing store: " +
					dbe.toString());
			System.exit(-1);
		}
	}

	if (myEnv != null) {
		try {
			// Finally, close the environment.
			myEnv.close();
		} catch(DatabaseException dbe) {
			System.err.println("Error closing MyDbEnv: " +
					dbe.toString());
			System.exit(-1);
		}
	}
}
} 

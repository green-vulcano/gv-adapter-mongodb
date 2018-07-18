package it.greenvulcano.mongodb;

import com.datastax.driver.core.Session;

public interface MongoDBConnector {
	
	Session getSession();
	
	Session getSession(String keyspace);

}

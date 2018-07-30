/**
 * 
 */
package it.greenvulcano.gvesb.channel.mongodb.service;

import com.mongodb.MongoClient;

/**
 * Useful business interface to provide a {@link MongoClient} instance 
 * in a dynamic dependecy injection context
 * 
 * 
 *
 */
public interface MongoClientProvider {
	
	
	
	public MongoClient getMongoClient();

}

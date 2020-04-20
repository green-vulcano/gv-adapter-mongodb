package it.greenvulcano.gvesb.channel.mongodb.service.connector;

import  org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.Optional;

import org.slf4j.Logger;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;

public class MongoDBConnector implements it.greenvulcano.gvesb.channel.mongodb.service.MongoClientProvider {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(MongoDBConnector.class);
	
	private final String name, connectionString;
	private MongoClient mongoClient;
	
	public MongoDBConnector(String name, String connectionString) {
		this.name = name;
		this.connectionString = connectionString;
	}
	
	public void init() {
		try {
			
			synchronized (this) {	
			
				if (Objects.isNull(mongoClient)) {
					
					LOGGER.debug("MongoClientProvider "+ name + " - creating MongoClient instance using connectionString: "+connectionString);
									
					mongoClient = MongoClients.create(connectionString);
					
					LOGGER.info("MongoClientProvider "+ name + " - MongoClient instance created ");
				}
			
			}
		} catch (Exception e) {
			LOGGER.error("MongoClientProvider "+ name + " - FAILED to create MongoClient using connectionString: "+connectionString, e);
		}
		
	}
	
	@Override
	public MongoClient getMongoClient() {		
		return mongoClient;
	}
	
	public void destroy() {
		synchronized (this) {	
			
			Optional.ofNullable(mongoClient).ifPresent(c-> {
				try {
								
					c.close();
				
				} catch (Exception e) {
					LOGGER.error("MongoClientProvider "+ name + " - Excetpion closing MongoClient instance ", e);
				}
			});
		
		}
	}

}

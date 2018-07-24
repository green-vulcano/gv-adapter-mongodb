package it.greenvulcano.gvesb.virtual.mongodb.dbo;

import org.bson.Document;
import org.slf4j.Logger;

import com.mongodb.client.MongoCollection;

import it.greenvulcano.gvesb.buffer.GVBuffer;
import it.greenvulcano.gvesb.buffer.GVException;
import it.greenvulcano.gvesb.virtual.mongodb.MongoDBQueryCallOperation;
import it.greenvulcano.util.metadata.PropertiesHandlerException;

public abstract class MongoDBO {
	
	protected static final Logger logger = org.slf4j.LoggerFactory.getLogger(MongoDBQueryCallOperation.class);
	
	public abstract String getDBOperationName();
	
	public abstract void execute(MongoCollection<Document> mongoCollection, GVBuffer gvBuffer) throws PropertiesHandlerException, GVException;

}
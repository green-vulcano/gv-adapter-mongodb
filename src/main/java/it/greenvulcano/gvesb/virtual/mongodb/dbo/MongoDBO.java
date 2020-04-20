package it.greenvulcano.gvesb.virtual.mongodb.dbo;

import org.bson.Document;
import org.bson.json.JsonMode;
import org.bson.json.JsonWriterSettings;
import org.slf4j.Logger;

import com.mongodb.client.MongoCollection;

import it.greenvulcano.gvesb.buffer.GVBuffer;
import it.greenvulcano.gvesb.buffer.GVException;
import it.greenvulcano.util.metadata.PropertiesHandlerException;

public abstract class MongoDBO {
	
	protected static final Logger logger = org.slf4j.LoggerFactory.getLogger(MongoDBO.class);
	protected static final JsonWriterSettings JSON_SETTINGS = JsonWriterSettings.builder().outputMode(JsonMode.RELAXED).build();
	
	public abstract String getDBOperationName();	
	
	public abstract void execute(MongoCollection<Document> mongoCollection, GVBuffer gvBuffer) throws PropertiesHandlerException, GVException;

}
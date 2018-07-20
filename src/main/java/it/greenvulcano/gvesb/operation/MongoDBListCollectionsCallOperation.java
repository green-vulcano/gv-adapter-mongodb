package it.greenvulcano.gvesb.operation;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoCursor;
import it.greenvulcano.configuration.XMLConfig;
import it.greenvulcano.gvesb.buffer.GVBuffer;
import it.greenvulcano.gvesb.channel.MongoDBChannel;
import it.greenvulcano.gvesb.virtual.*;
import it.greenvulcano.util.metadata.PropertiesHandler;
import org.slf4j.Logger;
import org.w3c.dom.Node;

import java.util.NoSuchElementException;

public class MongoDBListCollectionsCallOperation implements CallOperation {

    private static final Logger logger = org.slf4j.LoggerFactory.getLogger(MongoDBListCollectionsCallOperation.class);

    private OperationKey key = null;

    private String name;
    
    private String database;

    
    private MongoClient mongoClient;
    
    @Override
    public void init(Node node) throws InitializationException {

        logger.debug("Initializing mongodb-list-collections-call...");

        try {
        	
        	name = XMLConfig.get(node, "@name");
        	
        	mongoClient = MongoDBChannel.getMongoClient(node)
        			                    .orElseThrow(()-> new NoSuchElementException("MongoClient instance not foud for Operation " + name));
        	
        	database = XMLConfig.get(node, "@database");

            logger.debug("Initialization completed");

        } catch (Exception e) {

            throw new InitializationException("GV_INIT_SERVICE_ERROR",
                    new String[][] { { "message", e.getMessage() } }, e);

        }

    }

    @Override
    public GVBuffer perform(GVBuffer gvBuffer) throws ConnectionException, CallException, InvalidDataException {

        try {
        	
        	String actualDatabase = PropertiesHandler.expand(database, gvBuffer);

        	logger.debug("Getting the list of collections in the MongoDB database...");
        	
        	MongoCursor<String> resultset = mongoClient.getDatabase(actualDatabase)
    				.listCollectionNames()
    				.iterator();
    			
    		StringBuilder jsonResult = new StringBuilder("[");
    		
    		while (resultset.hasNext()) {

    		    jsonResult.append(resultset.next());
    			
    		    if (resultset.hasNext())

    		        jsonResult.append(",");
    		    else

    		        break;

    		}               
    		
    		jsonResult.append("]");

            gvBuffer.setObject(jsonResult);

        } catch (Exception exc) {

            throw new CallException("GV_CALL_SERVICE_ERROR",
                    new String[][] { { "service", gvBuffer.getService() }, { "system", gvBuffer.getSystem() },
                            { "tid", gvBuffer.getId().toString() }, { "message", exc.getMessage() } },
                    exc);

        }

        return gvBuffer;

    }

    @Override
    public void cleanUp() {
        // do nothing
    }

    @Override
    public void destroy() {
        // do nothing
    }

    @Override
    public void setKey(OperationKey operationKey) {
        this.key = operationKey;
    }

    @Override
    public OperationKey getKey() {
        return key;
    }

    @Override
    public String getServiceAlias(GVBuffer gvBuffer) {
        return gvBuffer.getService();
    }

}

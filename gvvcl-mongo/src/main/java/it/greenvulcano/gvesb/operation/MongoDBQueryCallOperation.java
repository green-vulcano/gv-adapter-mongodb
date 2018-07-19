package it.greenvulcano.gvesb.operation;

import it.greenvulcano.configuration.XMLConfig;
import it.greenvulcano.gvesb.buffer.GVBuffer;
import it.greenvulcano.gvesb.channel.MongoDBChannel;
import it.greenvulcano.gvesb.channel.service.MongoDBService;
import it.greenvulcano.gvesb.virtual.*;
import it.greenvulcano.util.metadata.PropertiesHandler;

import java.util.NoSuchElementException;

import org.bson.Document;
import org.slf4j.Logger;
import org.w3c.dom.Node;

<<<<<<< HEAD
import com.mongodb.MongoClient;
import com.mongodb.client.MongoCursor;
=======
import java.util.Objects;
>>>>>>> 5ce3e77aefc7a7c7d070747202b842fb135ae77b

public class MongoDBQueryCallOperation implements CallOperation {

    private static final Logger logger = org.slf4j.LoggerFactory.getLogger(MongoDBQueryCallOperation.class);

    private OperationKey key = null;

<<<<<<< HEAD
    private String name;
    
    private String database;
    private String collection;
    private String query;
=======
    private String hostname;

    private Integer portNumber;

    private String databaseName;

    private String collectionName;

    private String query;


>>>>>>> 5ce3e77aefc7a7c7d070747202b842fb135ae77b

    
    private MongoClient mongoClient;
    
    @Override
    public void init(Node node) throws InitializationException {

        logger.debug("Initializing mongodb-query-call...");

        try {
        	
        	name = XMLConfig.get(node, "@name");
        	
        	mongoClient = MongoDBChannel.getMongoClient(node)
        			                    .orElseThrow(()-> new NoSuchElementException("MongoClient instance not foud for Operation "+name));
        	
        	database = XMLConfig.get(node, "@database");
        	collection = XMLConfig.get(node, "@collection");

        	query = XMLConfig.get(node, "./query[text()]");       

            collectionName = XMLConfig.get(node, "@collectionName");

            query = XMLConfig.get(node, "@query");

            logger.debug("Initialization completed");

        } catch (Exception e) {

            throw new InitializationException("GV_INIT_SERVICE_ERROR",
                    new String[][] { { "message", e.getMessage() } }, e);

        }

    }

    @Override
    public GVBuffer perform(GVBuffer gvBuffer) throws ConnectionException, CallException, InvalidDataException {

        try {
<<<<<<< HEAD
        	
        	String actualDatabase = PropertiesHandler.expand(database, gvBuffer);
        	String actualCollection = PropertiesHandler.expand(collection, gvBuffer);
        	String actualQuery = PropertiesHandler.expand(query, gvBuffer);
        	
        	logger.debug("[ "+actualDatabase+":" +actualCollection+ "]Executing statement " + actualQuery);
        	
        	MongoCursor<String> resultset = mongoClient.getDatabase(actualDatabase)
    				.getCollection(actualCollection)
    				.find(Document.parse(actualQuery))				
    				.map(Document::toJson)
    				.iterator();
    			
    		StringBuilder jsonResult = new StringBuilder("[");
    		
    		while(resultset.hasNext()) {
    			jsonResult.append(resultset.next());
    			
    		    if(resultset.hasNext()) {
    		    	jsonResult.append(",");
    		    } else {
    		    	break;
    		    }
    		}               
    		
    		jsonResult.append("]");


            gvBuffer.setObject(jsonResult);
=======

            logger.info("GVBuffer: " + gvBuffer.toString());

            String statement = PropertiesHandler.expand(query, gvBuffer);

            MongoDBService mongo = Objects.requireNonNull(MongoDBChannel.getMongoDBService(hostname, portNumber, databaseName), "No connection to MongoDB found");

            logger.debug("Executing statement: " + statement);

            String result = mongo.findToJSON(null, collectionName, statement);

            gvBuffer.setObject(result);

            logger.info("Query executed successfully");
>>>>>>> 5ce3e77aefc7a7c7d070747202b842fb135ae77b

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

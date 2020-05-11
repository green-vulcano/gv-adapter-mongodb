package it.greenvulcano.gvesb.virtual.mongodb;

import com.mongodb.client.MongoClient;
import it.greenvulcano.configuration.XMLConfig;
import it.greenvulcano.gvesb.buffer.GVBuffer;
import it.greenvulcano.gvesb.channel.mongodb.MongoDBChannel;
import it.greenvulcano.gvesb.virtual.*;
import it.greenvulcano.util.metadata.PropertiesHandler;
import it.greenvulcano.util.xml.XMLUtils;

import org.bson.BsonArray;
import org.bson.Document;
import org.slf4j.Logger;
import org.w3c.dom.Node;

import java.util.NoSuchElementException;
import java.util.Optional;

public class MongoDBCreateUserCallOperation implements CallOperation {

    private static final Logger logger = org.slf4j.LoggerFactory.getLogger(MongoDBCreateUserCallOperation.class);

    private OperationKey key = null;

    private String name;
    private String uri;
    private String database;
    private String username, password, roles;

    
    @Override
    public void init(Node node) throws InitializationException {

        logger.debug("Initializing mongodb-create-user...");

        try {
        	
        	name = XMLConfig.get(node, "@name");        	
        	uri = XMLConfig.get(node, "@uri",  PropertiesHandler.expand(XMLUtils.get_S(node.getParentNode(), "@endpoint")));
        	
        	database = XMLConfig.get(node, "@database");
                username = XMLConfig.get(node, "@username");
                password = XMLConfig.get(node, "@password");
                roles = Optional.ofNullable(XMLConfig.get(node, "./text()")).map(String::trim).orElse("[]");
                
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
        	String actualUsername = PropertiesHandler.expand(username, gvBuffer);
        	String actualPassword = PropertiesHandler.expand(password, gvBuffer);
        	String actualRoles =  PropertiesHandler.expand(roles, gvBuffer) ;

        	logger.debug("Getting the list of collections in the MongoDB database...");
        	String actualUri = PropertiesHandler.expand(uri, gvBuffer);
        	MongoClient mongoClient = MongoDBChannel.getMongoClient(actualUri).orElseThrow(() -> new NoSuchElementException("MongoClient instance not found for Operation " + name));
        	
        	Document createUser =  new Document();
        	createUser.put("createUser", actualUsername);        	
        	Optional.ofNullable(actualPassword).ifPresent(pwd -> createUser.put("pwd", pwd));
        	
        	createUser.put("roles",  BsonArray.parse(actualRoles));
        	
        	Document result = mongoClient.getDatabase(actualDatabase)
        	                             .runCommand(createUser);

                gvBuffer.setObject(result.toJson());

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

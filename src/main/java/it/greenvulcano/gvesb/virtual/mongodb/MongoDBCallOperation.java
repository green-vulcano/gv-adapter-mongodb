package it.greenvulcano.gvesb.virtual.mongodb;

import it.greenvulcano.configuration.XMLConfig;
import it.greenvulcano.gvesb.buffer.GVBuffer;
import it.greenvulcano.gvesb.channel.mongodb.MongoDBChannel;
import it.greenvulcano.gvesb.virtual.*;
import it.greenvulcano.gvesb.virtual.mongodb.dbo.MongoDBO;
import it.greenvulcano.gvesb.virtual.mongodb.dbo.MongoDBOFactory;
import it.greenvulcano.util.metadata.PropertiesHandler;
import it.greenvulcano.util.xml.XMLUtils;

import java.util.NoSuchElementException;

import org.bson.Document;
import org.slf4j.Logger;
import org.w3c.dom.Node;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;

public class MongoDBCallOperation implements CallOperation {

    private static final Logger logger = org.slf4j.LoggerFactory.getLogger(MongoDBCallOperation.class);

    private OperationKey key = null;

    private String name;

    private String uri;
    private String database;
    private String collection;

    private MongoDBO dbo;

    @Override
    public void init(Node node) throws InitializationException {

        logger.debug("Initializing mongodb-call...");

        try {

            name = XMLConfig.get(node, "@name");
            uri = XMLConfig.get(node, "@uri",  PropertiesHandler.expand(XMLUtils.get_S(node.getParentNode(), "@endpoint")));          
            
            database = XMLConfig.get(node, "@database");
            collection = XMLConfig.get(node, "@collection");

            dbo = MongoDBOFactory.build(node);

            logger.debug("Configured DBOperation " + dbo.getDBOperationName());

        } catch (Exception e) {

            throw new InitializationException("GV_INIT_SERVICE_ERROR", new String[][] { { "message", e.getMessage() } }, e);

        }

    }

    @Override
    public GVBuffer perform(GVBuffer gvBuffer) throws ConnectionException, CallException, InvalidDataException {

        try {

            String actualUri = PropertiesHandler.expand(uri, gvBuffer);
            String actualDatabase = PropertiesHandler.expand(database, gvBuffer);
            String actualCollection = PropertiesHandler.expand(collection, gvBuffer);

            MongoClient mongoClient = MongoDBChannel.getMongoClient(actualUri).orElseThrow(() -> new NoSuchElementException("MongoClient instance not found for Operation " + name));
            logger.debug("Preparing MongoDB operation " + dbo.getDBOperationName() + "  on database: " + actualDatabase + " collection: " + actualCollection);

            MongoCollection<Document> mongoCollection = mongoClient.getDatabase(actualDatabase).getCollection(actualCollection);

            dbo.execute(mongoCollection, gvBuffer);

        } catch (Exception exc) {
            throw new CallException("GV_CALL_SERVICE_ERROR",
                                    new String[][] { { "service", gvBuffer.getService() },
                                                     { "system", gvBuffer.getSystem() },
                                                     { "tid", gvBuffer.getId().toString() },
                                                     { "message", exc.getMessage() } },
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
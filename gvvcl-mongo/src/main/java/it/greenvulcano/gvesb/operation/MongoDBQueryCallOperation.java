package it.greenvulcano.gvesb.operation;

import it.greenvulcano.configuration.XMLConfig;
import it.greenvulcano.gvesb.buffer.GVBuffer;
import it.greenvulcano.gvesb.channel.MongoDBChannel;
import it.greenvulcano.gvesb.channel.service.MongoDBService;
import it.greenvulcano.gvesb.virtual.*;
import it.greenvulcano.util.metadata.PropertiesHandler;
import org.slf4j.Logger;
import org.w3c.dom.Node;

import java.util.Objects;

public class MongoDBQueryCallOperation implements CallOperation {

    private static final Logger logger = org.slf4j.LoggerFactory.getLogger(MongoDBQueryCallOperation.class);

    private OperationKey key = null;

    private String hostname;

    private Integer portNumber;

    private String databaseName;

    private String collectionName;

    private String query;



    @Override
    public void init(Node node) throws InitializationException {

        logger.debug("Initializing mongodb-query-call...");

        try {

            hostname = XMLConfig.get(node, "@hostname");

            portNumber = Integer.valueOf(XMLConfig.get(node, "@portNumber"));

            databaseName = XMLConfig.get(node, "@databaseName");

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

            logger.info("GVBuffer: " + gvBuffer.toString());

            String statement = PropertiesHandler.expand(query, gvBuffer);

            MongoDBService mongo = Objects.requireNonNull(MongoDBChannel.getMongoDBService(hostname, portNumber, databaseName), "No connection to MongoDB found");

            logger.debug("Executing statement: " + statement);

            String result = mongo.findToJSON(null, collectionName, statement);

            gvBuffer.setObject(result);

            logger.info("Query executed successfully");

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

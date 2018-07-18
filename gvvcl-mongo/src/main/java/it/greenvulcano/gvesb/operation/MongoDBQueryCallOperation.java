package it.greenvulcano.gvesb.operation;

import it.greenvulcano.configuration.XMLConfig;
import it.greenvulcano.gvesb.buffer.GVBuffer;
import it.greenvulcano.gvesb.channel.MongoDBChannel;
import it.greenvulcano.gvesb.virtual.*;
import it.greenvulcano.util.metadata.PropertiesHandler;
import org.slf4j.Logger;
import org.w3c.dom.Node;

import javax.mail.Session;
import java.util.Objects;

public class MongoDBQueryCallOperation implements CallOperation {

    private static final Logger logger = org.slf4j.LoggerFactory.getLogger(MongoDBQueryCallOperation.class);

    private OperationKey key = null;

    private String hostname;

    private Integer portNumber;

    private String databaseName;



    @Override
    public void init(Node node) throws InitializationException {

        logger.debug("Initializing mongodb-query-call...");

        try {

            hostname = XMLConfig.get(node, "@hostname");

            portNumber = Integer.valueOf(XMLConfig.get(node, "@portNumber"));

            databaseName = XMLConfig.get(node, "@databaseName");

            logger.debug("Initialization completed");

        } catch (Exception e) {

            throw new InitializationException("GV_INIT_SERVICE_ERROR",
                    new String[][] { { "message", e.getMessage() } }, e);

        }

    }

    @Override
    public GVBuffer perform(GVBuffer gvBuffer) throws ConnectionException, CallException, InvalidDataException {

        try {

            String jndiname = PropertiesHandler.expand(this.endpoint, gvBuffer);
            String keyspace = PropertiesHandler.expand(this.keyspace, gvBuffer);

            Session session = Objects.requireNonNull(MongoDBChannel.getMongoDBService(jndiname, keyspace), "Active session not found");

            String query = PropertiesHandler.expand(statement, gvBuffer);

            logger.debug("Executing statement " + query);

            ResultSet queryResult = session.execute(query);
            gvBuffer.setObject(mapper.map(queryResult));

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

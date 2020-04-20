package it.greenvulcano.gvesb.virtual.mongodb.dbo;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.result.DeleteResult;

import it.greenvulcano.configuration.XMLConfig;
import it.greenvulcano.gvesb.buffer.GVBuffer;
import it.greenvulcano.gvesb.buffer.GVException;
import it.greenvulcano.util.metadata.PropertiesHandler;
import it.greenvulcano.util.metadata.PropertiesHandlerException;
import org.bson.Document;
import org.w3c.dom.Node;

import java.util.Optional;
import java.util.function.Function;

public class MongoDBODelete extends MongoDBO {

    static final String NAME = "delete";

    static final Function<Node, Optional<MongoDBO>> BUILDER = node -> {

        try {
            String filter = XMLConfig.get(node, "./filter/text()");
            return Optional.of(new MongoDBODelete(filter));

        } catch (Exception e) {

            return Optional.empty();

        }

    };

    private final String filter;
    private MongoDBODelete(String filter) {
        this.filter = filter;
    }
    
    @Override
    public String getDBOperationName() {

        return NAME;
    }

    @Override
    public void execute(MongoCollection<Document> mongoCollection, GVBuffer gvBuffer) throws PropertiesHandlerException, GVException {

        String deleteFilter = filter != null ? PropertiesHandler.expand(filter, gvBuffer) : gvBuffer.getObject().toString();

        logger.debug("Executing DBO Delete: " + deleteFilter);
        DeleteResult result = mongoCollection.deleteMany(Document.parse(deleteFilter));

        gvBuffer.setProperty("REC_DELETED", Long.toString(result.getDeletedCount()));
             

    }
}

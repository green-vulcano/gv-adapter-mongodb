package it.greenvulcano.gvesb.virtual.mongodb.dbo;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import org.bson.Document;
import org.json.JSONArray;
import org.w3c.dom.Node;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.IndexModel;

import it.greenvulcano.configuration.XMLConfig;
import it.greenvulcano.gvesb.buffer.GVBuffer;
import it.greenvulcano.gvesb.buffer.GVException;
import it.greenvulcano.util.metadata.PropertiesHandler;
import it.greenvulcano.util.metadata.PropertiesHandlerException;

public class MongoDBOCreateIndex extends MongoDBO {

    static final String NAME = "createIndex";
    
    static final Function<Node, Optional<MongoDBO>> BUILDER = node -> {
        
        try {
        
            List<String> keys = new LinkedList<>();
            for (Node keysNode : XMLConfig.getNodeListCollection(node, "./keys")) {
                
                keys.add(XMLConfig.get(keysNode, "./text()", "{}"));
            }
            
            return Optional.of(new MongoDBOCreateIndex(keys));
        } catch (Exception e) {
            
            return Optional.empty();
        }
        
    };
    
    private final List<String> keys;
    
    private MongoDBOCreateIndex(List<String> keys) {
      this.keys = keys;
    }
    
    @Override
    public String getDBOperationName() {
        return NAME;
    }

    @Override
    public void execute(MongoCollection<Document> mongoCollection, GVBuffer gvBuffer) throws PropertiesHandlerException, GVException {

        List<IndexModel> indexes = new LinkedList<>();
        for (String indexKeys : keys) {
            String actualKeys = PropertiesHandler.expand(indexKeys, gvBuffer);
            
            indexes.add(new IndexModel(Document.parse(actualKeys)));
            
        }       
        
        JSONArray names = new JSONArray();        
        mongoCollection.createIndexes(indexes).forEach(names::put);
        
        gvBuffer.setObject(names.toString());
        
    }
    
    

}

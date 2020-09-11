package it.greenvulcano.gvesb.virtual.mongodb.dbo;

import com.mongodb.client.MongoCollection;
import it.greenvulcano.gvesb.buffer.GVBuffer;
import it.greenvulcano.gvesb.buffer.GVException;
import it.greenvulcano.util.metadata.PropertiesHandlerException;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.w3c.dom.Node;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public class MongoDBOInsert extends MongoDBO {

    static final String NAME = "insert";

    static final Function<Node, Optional<MongoDBO>> BUILDER = node -> {

        try {

            return Optional.of(new MongoDBOInsert());

        } catch (Exception e) {

            return Optional.empty();

        }

    };

    @Override
    public String getDBOperationName() {

        return NAME;
    }

    @Override
    public void execute(MongoCollection<Document> mongoCollection, GVBuffer gvBuffer) throws PropertiesHandlerException, GVException {

        String bufferObj = Optional.ofNullable(gvBuffer.getObject()).map(Object::toString).map(String::trim).orElseThrow();

        logger.debug("Executing DBO Insert: {}", bufferObj);
        
        List<Document> dbEntries = new LinkedList<>();
        
        if (bufferObj.startsWith("{")) { //is a json object
            
            Document entry = Document.parse(bufferObj);
            entry.putIfAbsent("_id", ObjectId.get());

            dbEntries.add(entry);
            
        } else if (bufferObj.startsWith("[")) { //is a json object
            
            JSONArray items = new JSONArray(bufferObj);
            
            for (int i = 0; i<items.length(); i++) {
                
                Document entry = Document.parse(items.getJSONObject(i).toString());
                entry.putIfAbsent("_id", ObjectId.get());

                dbEntries.add(entry);
                
            }
            
        }               

        mongoCollection.insertMany(dbEntries);
        
        if(dbEntries.size()==1) {
            gvBuffer.setObject(dbEntries.get(0).toJson(JSON_SETTINGS));
        } else {        
            gvBuffer.setObject(dbEntries.stream().map(d -> d.toJson(JSON_SETTINGS)).collect(Collectors.joining(",","[", "]")));
        }

    }

}
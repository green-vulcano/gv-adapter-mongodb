package it.greenvulcano.gvesb.virtual.mongodb.dbo;

import com.mongodb.client.MongoCollection;
import it.greenvulcano.gvesb.buffer.GVBuffer;
import it.greenvulcano.gvesb.buffer.GVException;
import it.greenvulcano.util.metadata.PropertiesHandlerException;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.w3c.dom.Node;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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

        String bufferObj = Optional.ofNullable(gvBuffer.getObject()).map(Object::toString).orElseThrow();

        List<Document> dbEntries = new LinkedList<>();

        Matcher jsonMatcher = Pattern.compile("\\{[^}]*\\}").matcher(bufferObj);
        while (jsonMatcher.find()) {
            Document entry = Document.parse(jsonMatcher.group());
            entry.putIfAbsent("_id", ObjectId.get());

            dbEntries.add(entry);
        }

        mongoCollection.insertMany(dbEntries);
        
        if(dbEntries.size()==1) {
            gvBuffer.setObject(dbEntries.get(0).toJson(JSON_SETTINGS));
        } else {        
            gvBuffer.setObject(dbEntries.stream().map(d -> d.toJson(JSON_SETTINGS)).collect(Collectors.joining(",","[", "]")));
        }

    }

}
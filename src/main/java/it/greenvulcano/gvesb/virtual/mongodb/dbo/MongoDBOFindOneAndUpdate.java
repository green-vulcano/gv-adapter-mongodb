package it.greenvulcano.gvesb.virtual.mongodb.dbo;

import java.util.Optional;
import java.util.function.Function;

import org.bson.Document;
import org.w3c.dom.Node;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.ReturnDocument;
import it.greenvulcano.configuration.XMLConfig;
import it.greenvulcano.gvesb.buffer.GVBuffer;
import it.greenvulcano.gvesb.buffer.GVException;
import it.greenvulcano.util.metadata.PropertiesHandler;
import it.greenvulcano.util.metadata.PropertiesHandlerException;

public class MongoDBOFindOneAndUpdate extends MongoDBO {
	
	static final String NAME = "findOneAndModify";
	static final Function<Node, Optional<MongoDBO>> BUILDER = node ->{
		
		try {
		
			String filter = XMLConfig.get(node, "./filter/text()", "{}");
			String statement = XMLConfig.get(node, "./statement/text()");
			boolean upsert = XMLConfig.getBoolean(node, "@upsert", false);
			boolean original = XMLConfig.getBoolean(node, "@return-original", false);
			
			return Optional.of(new MongoDBOFindOneAndUpdate(filter, statement,  upsert, original));
			
		} catch (Exception e) {
			
			return Optional.empty();
		}
		
	};	
	
	private final String filter;
	private final String statement;	
	private final boolean upsert;
        private final boolean original;	
        
	public MongoDBOFindOneAndUpdate(String filter, String statement, boolean upsert, boolean original) {
		this.filter = filter;
		this.statement = statement;
		this.upsert = upsert;
		this.original = original;
	}

	@Override
	public String getDBOperationName() {		
		return NAME;
	}

	@Override
	public void execute(MongoCollection<Document> mongoCollection, GVBuffer gvBuffer) throws PropertiesHandlerException, GVException {
		
		String actualFilter = PropertiesHandler.expand(filter, gvBuffer);
		String actualStatement = PropertiesHandler.expand(statement, gvBuffer);
				
		FindOneAndUpdateOptions options = new FindOneAndUpdateOptions();
		options.upsert(upsert);		
		options.returnDocument(original ? ReturnDocument.BEFORE : ReturnDocument.AFTER);
				
		Document result = mongoCollection.findOneAndUpdate(Document.parse(actualFilter), Document.parse(actualStatement), options);
		
		if (result != null) {
		    gvBuffer.setProperty("REC_READ", "1");
	            gvBuffer.setProperty("REC_UPDATE", "1");
		    gvBuffer.setObject(result.toJson(JSON_SETTINGS));
		} else {
		    gvBuffer.setProperty("REC_READ", "0");
                    gvBuffer.setProperty("REC_UPDATE", "0");
                    gvBuffer.setObject(null);
		}
		
	}

}

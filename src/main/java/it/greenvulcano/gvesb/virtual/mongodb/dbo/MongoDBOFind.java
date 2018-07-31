package it.greenvulcano.gvesb.virtual.mongodb.dbo;

import java.util.Optional;
import java.util.function.Function;

import org.bson.Document;
import org.w3c.dom.Node;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;

import it.greenvulcano.gvesb.buffer.GVBuffer;
import it.greenvulcano.gvesb.buffer.GVException;
import it.greenvulcano.util.metadata.PropertiesHandler;
import it.greenvulcano.util.metadata.PropertiesHandlerException;

public class MongoDBOFind extends MongoDBO {
	
	static final String NAME = "find";
	static final Function<Node, Optional<MongoDBO>> BUILDER = node ->{
		
		try {
		
			String query = node.getTextContent();				
			return Optional.of(new MongoDBOFind(query));
			
		} catch (Exception e) {
			
			return Optional.empty();
		}
		
	};
	
	private final String query;
	
	MongoDBOFind(String query) {
		this.query = query;
	}
	
	@Override
	public String getDBOperationName() {		
		return NAME;
	} 

	@Override
	public void execute(MongoCollection<Document> mongoCollection, GVBuffer gvBuffer) throws PropertiesHandlerException, GVException {
		
		String actualQuery = PropertiesHandler.expand(query, gvBuffer);
    	
    	logger.debug("Executing DBO Find: " + actualQuery);		
		
		MongoCursor<String> resultset = mongoCollection.find(Document.parse(actualQuery)).map(Document::toJson).iterator();		
		
		StringBuilder jsonResult = new StringBuilder("[");
		
		int count = 0;
		while(resultset.hasNext()) {
			count++;
			jsonResult.append(resultset.next());
			
		    if(resultset.hasNext()) {
		    	jsonResult.append(",");
		    } else {
		    	break;
		    }
		}               
		
		jsonResult.append("]");

		gvBuffer.setProperty("REC_READ", Integer.toString(count));
        gvBuffer.setObject(jsonResult);
		
	}	

}

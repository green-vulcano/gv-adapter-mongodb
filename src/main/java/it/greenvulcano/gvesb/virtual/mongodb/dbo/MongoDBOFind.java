package it.greenvulcano.gvesb.virtual.mongodb.dbo;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import it.greenvulcano.configuration.XMLConfig;
import it.greenvulcano.gvesb.buffer.GVBuffer;
import it.greenvulcano.gvesb.buffer.GVException;
import it.greenvulcano.util.metadata.PropertiesHandler;
import it.greenvulcano.util.metadata.PropertiesHandlerException;
import org.bson.Document;
import org.w3c.dom.Node;

import java.util.Optional;
import java.util.function.Function;

public class MongoDBOFind extends MongoDBO {
	
	static final String NAME = "find";
	static final Function<Node, Optional<MongoDBO>> BUILDER = node ->{
		
		try {
		
			String query = XMLConfig.get(node, "./query/text()", "{}");
			String projection = XMLConfig.get(node, "./projection/text()", "{}");
			String sort = XMLConfig.get(node, "./sort/text()", "{}");

			Integer skip = XMLConfig.getInteger(node, "@offset", 0);
			Integer limit = XMLConfig.getInteger(node, "@limit", 0);

			return Optional.of(new MongoDBOFind(query,
					sort, projection, skip, limit));
			
		} catch (Exception e) {
			
			return Optional.empty();

		}
		
	};
	
	private final String query;
	private final String sort;
	private final String projection;

	private final Integer skip;
	private final Integer limit;

	
	MongoDBOFind(String query,
				 String sort,
				 String projection,
				 Integer skip,
				 Integer limit) {

		this.query = query;
		this.sort = sort;
		this.projection = projection;
		this.skip = skip;
		this.limit = limit;

	}
	
	@Override
	public String getDBOperationName() {		
		return NAME;
	} 

	@Override
	public void execute(MongoCollection<Document> mongoCollection, GVBuffer gvBuffer) throws PropertiesHandlerException, GVException {

		String queryCommand = PropertiesHandler.expand(query, gvBuffer);
		String querySort = PropertiesHandler.expand(sort, gvBuffer);
		String queryProjection = PropertiesHandler.expand(projection, gvBuffer);

		Integer querySkip = Integer.valueOf(PropertiesHandler.expand(Integer.toString(skip), gvBuffer));
		Integer queryLimit = Integer.valueOf(PropertiesHandler.expand(Integer.toString(limit), gvBuffer));

		Document commandDocument = Document.parse(queryCommand);
		Document sortDocument = Document.parse(querySort);
		Document projectionDocument = Document.parse(queryProjection);
    	
    	logger.debug("Executing DBO Find: " + queryCommand
				+ "; sort " + querySort
				+ "; projection " + queryProjection
				+ "; skip " + querySkip
				+ "; limit " + queryLimit);
		
		MongoCursor<String> resultset = mongoCollection
				.find(commandDocument)
				.projection(projectionDocument)
				.sort(sortDocument)
				.skip(querySkip)
				.limit(queryLimit)
				.map(Document::toJson).iterator();
		
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

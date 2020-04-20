package it.greenvulcano.gvesb.virtual.mongodb.dbo;

import com.mongodb.client.MongoCollection;
import it.greenvulcano.configuration.XMLConfig;
import it.greenvulcano.gvesb.buffer.GVBuffer;
import it.greenvulcano.gvesb.buffer.GVException;
import it.greenvulcano.util.metadata.PropertiesHandler;
import it.greenvulcano.util.metadata.PropertiesHandlerException;
import org.bson.Document;
import org.w3c.dom.Node;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public class MongoDBOFind extends MongoDBO {
	
	static final String NAME = "find";
	static final Function<Node, Optional<MongoDBO>> BUILDER = node ->{
		
		try {
		
			String query = XMLConfig.get(node, "./query/text()", "{}");
			String projection = XMLConfig.get(node, "./projection/text()", "{}");
			String sort = XMLConfig.get(node, "./sort/text()", "{}");

			String skip = XMLConfig.get(node, " @offset", "0");
			String limit = XMLConfig.get(node, "@limit", Integer.toString(Integer.MAX_VALUE));

			return Optional.of(new MongoDBOFind(query,
					sort, projection, skip, limit));
			
		} catch (Exception e) {
			
			return Optional.empty();

		}
		
	};
	
	private final String query;
	private final String sort;
	private final String projection;

	private final String skip;
	private final String limit;

	
	MongoDBOFind(String query,
				 String sort,
				 String projection,
				 String skip,
				 String limit) {

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

		// expand the content of children of find element from the GVBuffer
		String queryCommand = PropertiesHandler.expand(query, gvBuffer);
		String querySort = PropertiesHandler.expand(sort, gvBuffer);
		String queryProjection = PropertiesHandler.expand(projection, gvBuffer);

		// prepare the skip and limit parameters of the find element
		Integer querySkip = null;
		Integer queryLimit = null;

		try {

			// expand the the value of skip and limit parameters from the GVBuffer
			querySkip = Integer.valueOf(PropertiesHandler.expand(skip, gvBuffer));
			queryLimit = Integer.valueOf(PropertiesHandler.expand(limit, gvBuffer));

		} catch (NumberFormatException e) {

			// a non-integer value was found for either skip or limit parameter
			String exceptionMessage = "Non-integer parameter passed to <find> element: " + e.getCause();

			logger.error(exceptionMessage);

			throw new GVException(exceptionMessage);

		}

		Document commandDocument = Document.parse(queryCommand);
		Document sortDocument = Document.parse(querySort);
		Document projectionDocument = Document.parse(queryProjection);
    	
        	logger.debug("Executing DBO Find: {}"
    				+ "; sort {}"
    				+ "; projection {}"
    				+ "; skip {}"
    				+ "; limit {}",queryCommand, querySort, queryProjection, querySkip, queryLimit);
        	 
        	List<Document> resultSet = new LinkedList<>();
		mongoCollection.find(commandDocument)
			       .projection(projectionDocument)
			       .sort(sortDocument)
			       .skip(querySkip)
			       .limit(queryLimit)
			       .iterator()
			       .forEachRemaining(resultSet::add);
	
		gvBuffer.setObject(resultSet.stream().map(d -> d.toJson(JSON_SETTINGS)).collect(Collectors.joining(",","[", "]")));
		gvBuffer.setProperty("REC_READ", Integer.toString(resultSet.size()));        
		
	}	

}

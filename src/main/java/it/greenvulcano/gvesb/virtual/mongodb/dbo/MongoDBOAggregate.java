package it.greenvulcano.gvesb.virtual.mongodb.dbo;

import com.mongodb.client.MongoCollection;
import it.greenvulcano.configuration.XMLConfig;
import it.greenvulcano.gvesb.buffer.GVBuffer;
import it.greenvulcano.gvesb.buffer.GVException;
import it.greenvulcano.util.metadata.PropertiesHandler;
import it.greenvulcano.util.metadata.PropertiesHandlerException;
import org.bson.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class MongoDBOAggregate extends MongoDBO {
	
	static final String NAME = "aggregate";
	static final Function<Node, Optional<MongoDBO>> BUILDER = node ->{
		
		try {
		
			NodeList stagesConfig =XMLConfig.getNodeList(node, "./stage");
			
			
			List<String> stages = IntStream.range(0, stagesConfig.getLength())
			         .mapToObj(stagesConfig::item)
			         .map(Node::getTextContent)
			         .collect(Collectors.toList());
			
			
			return Optional.of(new MongoDBOAggregate(stages));
			
		} catch (Exception e) {
			
			return Optional.empty();

		}
		
	};
	
	private final List<String> stages;
	
	MongoDBOAggregate(List<String> stages) {

		this.stages = stages;
	
	}
	
	@Override
	public String getDBOperationName() {		
		return NAME;
	} 

	@Override
	public void execute(MongoCollection<Document> mongoCollection, GVBuffer gvBuffer) throws PropertiesHandlerException, GVException {
				
		List<Document> stagesBson = new ArrayList<>();
			
		for (String s: stages) {
			
			try {
			
				String statement = PropertiesHandler.expand(s, gvBuffer);				
				
				logger.debug("Adding stage to aggregation: {}", statement);				
				stagesBson.add(Document.parse(statement));
			} catch (Exception e) {
				
				logger.error("Error adding stage to aggregation", e);

				throw new GVException("Error adding stage to aggregation" + e.getClass().getName());

			}
		}
			
		List<Document> resultSet = new LinkedList<>();
		
		mongoCollection.aggregate(stagesBson)
			       .iterator()
			       .forEachRemaining(resultSet::add);
				
		mongoCollection.insertMany(resultSet);	        
	        gvBuffer.setObject(resultSet.stream().map(d -> d.toJson(JSON_SETTINGS)).collect(Collectors.joining(",","[", "]")));
	        gvBuffer.setProperty("REC_READ", Integer.toString(resultSet.size())); 
		
	}	

}

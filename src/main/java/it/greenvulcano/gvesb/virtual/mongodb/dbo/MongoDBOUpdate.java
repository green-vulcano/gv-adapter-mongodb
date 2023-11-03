package it.greenvulcano.gvesb.virtual.mongodb.dbo;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.bson.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.result.UpdateResult;

import it.greenvulcano.configuration.XMLConfig;
import it.greenvulcano.gvesb.buffer.GVBuffer;
import it.greenvulcano.gvesb.buffer.GVException;
import it.greenvulcano.util.metadata.PropertiesHandler;
import it.greenvulcano.util.metadata.PropertiesHandlerException;

public class MongoDBOUpdate extends MongoDBO {
	
	static final String NAME = "update";
	static final Function<Node, Optional<MongoDBO>> BUILDER = node ->{
		
		try {
		
			String filter = XMLConfig.get(node, "./filter/text()", "{}");
			String statement = XMLConfig.get(node, "./statement/text()");

			NodeList arrayFiltersConfig =XMLConfig.getNodeList(node, "./arrayFilter");
			List<String> arrayFilters = IntStream.range(0, arrayFiltersConfig.getLength())
			         .mapToObj(arrayFiltersConfig::item)
			         .map(Node::getTextContent)
			         .collect(Collectors.toList());
			boolean upsert = XMLConfig.getBoolean(node, "@upsert", false);
			return Optional.of(new MongoDBOUpdate(filter, statement, arrayFilters, upsert));
			
		} catch (Exception e) {
			
			return Optional.empty();
		}
		
	};	
	
	private final String filter;
	private final String statement;	
	private final List<String> arrayFilters;
	private final boolean upsert;

	public MongoDBOUpdate(String filter, String statement, List<String> arrayFilters, boolean upsert) {
		this.filter = filter;
		this.statement = statement;
		this.arrayFilters = arrayFilters;
		this.upsert = upsert;
	}

	@Override
	public String getDBOperationName() {		
		return NAME;
	}

	@Override
	public void execute(MongoCollection<Document> mongoCollection, GVBuffer gvBuffer) throws PropertiesHandlerException, GVException {
		
		String actualFilter = PropertiesHandler.expand(filter, gvBuffer);
		String actualStatement = PropertiesHandler.expand(statement, gvBuffer);
		
		List<Document> arrayFiltersBson = new ArrayList<>();
		for (String f: arrayFilters) {
			try {
				String fs = PropertiesHandler.expand(f, gvBuffer);				
				logger.debug("Adding arrayFilter to update: {}", fs);				
				arrayFiltersBson.add(Document.parse(fs));
			} catch (Exception e) {
				logger.error("Error adding arrayFilter to update", e);
				throw new GVException("Error adding arrayFilter to update" + e.getClass().getName());
			}
		}

		
		logger.debug("Executing DBO Update filter: {}", actualFilter);
		logger.debug("Executing DBO Update statement: {}", actualStatement);
		
		UpdateOptions updateOptions = new UpdateOptions();
		if (!arrayFiltersBson.isEmpty()) {
			updateOptions.arrayFilters(arrayFiltersBson);
		}
		updateOptions.upsert(upsert);
		
		UpdateResult updateResult = mongoCollection.updateMany(Document.parse(actualFilter), Document.parse(actualStatement), updateOptions);
		
		gvBuffer.setProperty("REC_READ", Long.toString(updateResult.getMatchedCount()));
		gvBuffer.setProperty("REC_UPDATE", Long.toString(updateResult.getModifiedCount()));

		if (upsert) {
			Optional.ofNullable(updateResult.getUpsertedId())			  
			        .ifPresent(id->{
						try {
						    
						    if (id.isObjectId()) {
                                                        gvBuffer.setProperty("REC_IDS", id.asObjectId().getValue().toHexString());
						    } else if (id.isString()) {
                                                        gvBuffer.setProperty("REC_IDS", id.asString().getValue());
						    } else if (id.isNumber()) {
						        gvBuffer.setProperty("REC_IDS", id.asNumber().toString());
						    } else {
						        gvBuffer.setProperty("REC_IDS", id.toString());
						    }
						} catch (GVException e) {}
					});
			
		}
		
	}

}

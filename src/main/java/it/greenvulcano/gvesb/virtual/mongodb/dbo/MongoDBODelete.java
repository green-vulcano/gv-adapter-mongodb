package it.greenvulcano.gvesb.virtual.mongodb.dbo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.result.DeleteResult;
import it.greenvulcano.gvesb.buffer.GVBuffer;
import it.greenvulcano.gvesb.buffer.GVException;
import it.greenvulcano.util.metadata.PropertiesHandlerException;
import org.bson.Document;
import org.json.JSONException;
import org.w3c.dom.Node;

import java.util.Optional;
import java.util.function.Function;

public class MongoDBODelete extends MongoDBO {

	static final String NAME = "delete";

	static final Function<Node, Optional<MongoDBO>> BUILDER = node -> {

		try {

			return Optional.of(new MongoDBODelete());

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

		// prepare the JSON filter to select the documents to delete from the specified collection
		String deleteFilter = gvBuffer.getObject().toString();

		logger.debug("Executing DBO Delete: " + deleteFilter);

		// prepare the delete operation result object
		DeleteResult result = null;

		// perform the delete operation and count the deleted rows
		result = mongoCollection.deleteMany(Document.parse(deleteFilter));

		// prepare the JSON string containing the amount of deleted documents
		StringBuilder jsonResultSet = new StringBuilder();

		// prepare a simple bean reporting the amount of deleted
		MongoDBODeleteReport report = new MongoDBODeleteReport(result.getDeletedCount());

		// prepare the JSON serializer
		ObjectMapper mapper = new ObjectMapper();

		try {

			jsonResultSet.append(mapper.writeValueAsString(report));

		} catch (JsonProcessingException e) {

			String message = "Unable to produce the JSON representation of the MongoDB delete operation report";

			logger.error(message);

			throw new JSONException(message);

		}

		// set the outbound content of the GVBuffer equal to the JSON representation of the delete operation report
		gvBuffer.setObject(jsonResultSet.toString());

	}

	public static class MongoDBODeleteReport {

		private Long deletedDocuments;

		public MongoDBODeleteReport(Long deletedDocuments) {
			this.deletedDocuments = deletedDocuments;
		}

		public Long getDeletedDocuments() {
			return deletedDocuments;
		}

		public void setDeletedDocuments(Long deletedDocuments) {
			this.deletedDocuments = deletedDocuments;
		}

	}

}

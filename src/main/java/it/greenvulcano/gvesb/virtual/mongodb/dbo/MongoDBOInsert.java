package it.greenvulcano.gvesb.virtual.mongodb.dbo;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import it.greenvulcano.gvesb.buffer.GVBuffer;
import it.greenvulcano.gvesb.buffer.GVException;
import it.greenvulcano.util.metadata.PropertiesHandlerException;
import org.bson.Document;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Node;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

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

		// prepare the JSON document(s) to insert into the specified MongoDB collection
		String actualInsert = gvBuffer.getObject().toString();

		// prepare a list of Documents to persist, if a JSON list is specified
		List<Document> docs = new LinkedList<>();

		logger.debug("Executing DBO Insert: " + actualInsert);

		// if the input buffer contains a JSON array, then process each of its elements as a separate MongoDB document
		if (isValidJSONArray(actualInsert)) {

			// parse the JSON array from the GVBuffer content
			JSONArray docsArray = new JSONArray(actualInsert);

			// parse each JSON element of the array as a single JSON object, and add it to the list of JSON objects
			for (int i = 0; i < docsArray.length(); i++) {

				JSONObject doc = docsArray.getJSONObject(i);

				docs.add(Document.parse(doc.toString()));

			}

			// insert each of the JSON objects into the MongoDB collection
			mongoCollection.insertMany(docs);

		}

		// otherwise, if the input buffer contains a single JSON object, then insert just it into the MongoDB database
		else if (isValidJSONObject(actualInsert)) {

			// the specified JSON is a single object
			mongoCollection.insertOne(Document.parse(actualInsert));

		}

		// otherwise, the string is not a valid JSON
		else {

			// the specified JSON is not syntactically valid
			String message = "Invalid JSON payload passed to persist as MongoDB document(s)";

			logger.error(message);

			throw new JSONException(message);

		}

		// prepare the JSON string representing the inserted document(s)
		StringBuilder jsonResultSet = new StringBuilder();

		// if the JSON string contained a single JSON element, then build the JSON response for the outbound GVBuffer as
		// the single persisted document
		if (docs.isEmpty()) {

			String jsonResult = "";

			MongoCursor<String> resultSet = mongoCollection.find(Document.parse(actualInsert)).map(Document::toJson).iterator();

			while (resultSet.hasNext()) {

				jsonResult = resultSet.next();

			}

			jsonResultSet = new StringBuilder(jsonResult);

		}

		// otherwise, build the JSON response for the outbound GVBuffer as a list of the persisted documents
		else {

			jsonResultSet.append("[");

			// for each document, find its latest occurrence and add it to the result set
			for (Document doc : docs) {

				String jsonResult = "";

				MongoCursor<String> resultSet = mongoCollection.find(doc).map(Document::toJson).iterator();

				// pick the document with the highest id, in case more than one document is returned
				while (resultSet.hasNext()) {

					jsonResult = resultSet.next();

				}

				jsonResultSet.append(jsonResultSet.length() <= 1 ? "" : ", ").append(jsonResult);

			}

			jsonResultSet.append("]");

		}

		// set the outbound content of the GVBuffer equal to the persisted document(s)
		gvBuffer.setObject(jsonResultSet.toString());

	}

}

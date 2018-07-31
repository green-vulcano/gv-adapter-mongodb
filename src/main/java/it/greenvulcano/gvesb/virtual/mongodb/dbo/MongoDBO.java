package it.greenvulcano.gvesb.virtual.mongodb.dbo;

import org.bson.Document;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;

import com.mongodb.client.MongoCollection;

import it.greenvulcano.gvesb.buffer.GVBuffer;
import it.greenvulcano.gvesb.buffer.GVException;
import it.greenvulcano.util.metadata.PropertiesHandlerException;

public abstract class MongoDBO {
	
	protected static final Logger logger = org.slf4j.LoggerFactory.getLogger(MongoDBO.class);
	
	public abstract String getDBOperationName();
	
	public abstract void execute(MongoCollection<Document> mongoCollection, GVBuffer gvBuffer) throws PropertiesHandlerException, GVException;

	public boolean isValidJSON(String json) throws IllegalArgumentException {

		// first, check the parameters
		if (json == null) {

			String message = "Null reference mandatory parameter";

			logger.error(message);

			throw new IllegalArgumentException(message);

		}

		// first, determine whether the specified JSON string represents a single JSON element
		try { new JSONObject(json); }

		// if an exception occurred, first determine whether the specified JSON string represents a JSON array
		catch (JSONException e) {

			try { new JSONArray(json); }

			catch (JSONException e1) { return false; }

		}

		// the specified string is a valid JSON string
		return true;

	}

	public boolean isValidJSONArray(String json) throws IllegalArgumentException {

		// first, check the parameters
		if (json == null) {

			String message = "Null reference mandatory parameter";

			logger.error(message);

			throw new IllegalArgumentException(message);

		}

		// first, determine whether the specified JSON string represents a JSON array
		try { new JSONArray(json); }

		// if an exception occurred, then the specified string does not represent a JSON array
		catch (JSONException e) { return false; }

		// the specified string is a valid JSON array
		return true;

	}

	public boolean isValidJSONObject(String json) throws IllegalArgumentException {

		// first, check the parameters
		if (json == null) {

			String message = "Null reference mandatory parameter";

			logger.error(message);

			throw new IllegalArgumentException(message);

		}

		// first, determine whether the specified JSON string represents a JSON object
		try { new JSONObject(json); }

		// if an exception occurred, then the specified string does not represent a JSON object
		catch (JSONException e) { return false; }

		// the specified string is a valid JSON object
		return true;

	}

}
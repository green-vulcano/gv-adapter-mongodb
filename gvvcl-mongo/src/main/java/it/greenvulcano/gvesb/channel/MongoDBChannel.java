package it.greenvulcano.gvesb.channel;

import it.greenvulcano.gvesb.channel.service.MongoDBService;
import it.greenvulcano.gvesb.j2ee.JNDIHelper;

import java.util.HashMap;
import java.util.Map;

public class MongoDBChannel {
	
	private static Map<String, MongoDBService> mongoDBServices = new HashMap<>();

	// private final static JNDIHelper jndiContext = new JNDIHelper();



	static void shutdown() {

		for (MongoDBService service : mongoDBServices.values())

			service.close();

	}

	public static MongoDBService getMongoDBService(String hostname, Integer portNumber, String databaseName) {

		// first, check the parameters
		if (hostname == null || portNumber == null || databaseName == null)

			throw new IllegalArgumentException("Null-reference mandatory parameters");

		// compose the key corresponding to the MongoDB session client to return to the caller
		String key = hostname + "~" + portNumber + "~" + databaseName;

		// lookup the MongoDB client matching the search parameters
		MongoDBService service = mongoDBServices.get(key);

		// if found, then return it
		if (service != null) return service;



		// otherwise, initialize it
		service = new MongoDBService(hostname, portNumber, databaseName);

		// insert the key-value matching the new MongoDB session client into the map
		mongoDBServices.put(key, service);

		// return the generated client
		return service;

		/*String sessionKey =  Optional.ofNullable(keyspace).orElse("default")+"@"+connectorJndiName;

		if (!cassandraSessions.containsKey(sessionKey)) {
			try {

				CassandraConnector cassandraConnector = (CassandraConnector) jndiContext.lookup(connectorJndiName);

				Session session = Optional.ofNullable(keyspace).map(cassandraConnector::getSession).orElseGet(cassandraConnector::getSession);
				cassandraSessions.putIfAbsent(sessionKey, session);


			} catch (NamingException e) {
				LOG.error("Error retrieving CassandraConnector instance", e);
			} catch (Exception e) {
				LOG.error("Error building Cassandra session instance", e);
			}
		}

		return cassandraSessions.get(sessionKey);*/

	}

}

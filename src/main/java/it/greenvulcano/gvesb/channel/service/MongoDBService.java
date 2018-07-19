package it.greenvulcano.gvesb.channel.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.mongodb.BasicDBObject;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.MongoCommandException;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoIterable;
import it.greenvulcano.gvesb.channel.exception.QueryFailedException;
import it.greenvulcano.gvesb.channel.util.DocumentSerializer;
import it.greenvulcano.gvesb.channel.util.Properties;
import org.bson.Document;

import java.util.LinkedList;
import java.util.List;

public class MongoDBService {

    private MongoClient mongoClient;

    private MongoDatabase database;



    /*
        constructors and database selector method
     */

    public MongoDBService() throws Exception {

        // fetch the properties concerning the mongoDB connection
        String hostname = null;
        String portNumberStr = null;
        Integer portNumber = null;

        try {

            hostname = Properties.getPropertyValueAsString("com.greenvulcano.gvesb.db.hostname");
            portNumberStr = Properties.getPropertyValueAsString("com.greenvulcano.gvesb.db.portNumber");
            portNumber = Integer.valueOf(portNumberStr);

            if (hostname == null || portNumber == null) {

                throw new IllegalArgumentException("Undefined MongoDB connection parameters");

            }

        } catch (Exception e) {

            System.out.println(e.getMessage());

            throw e;

        }

        // initialize the MongoDB object by connecting it to the MongoDB server
        mongoClient = new MongoClient(hostname, portNumber);

    }

    public MongoDBService(String hostname, Integer portNumber, String dbName) throws IllegalArgumentException {

        // first, check the parameters
        if (hostname == null || portNumber == null)

            throw new IllegalArgumentException("Undefined MongoDB connection parameters");



        // initialize the mongoDB object by connecting it to the mongo server
        mongoClient = new MongoClient(hostname, portNumber);

        // connect to the specified database
        database = mongoClient.getDatabase(dbName);

    }

    public MongoDBService(String URIString) throws IllegalArgumentException {

        // first, check the parameters
        if (URIString == null)

            throw new IllegalArgumentException("Undefined MongoDB connection URI");



        // prepare the connection URI
        MongoClientURI URI = new MongoClientURI(URIString);

        // initialize the mongoDB object by connecting it to the database
        mongoClient = new MongoClient(URI);

        // if the URI refers to a database, then connect the client to the database
        if (URI.getDatabase() != null)

            database = mongoClient.getDatabase(URI.getDatabase());

    }

    public MongoDatabase useDatabase(String databaseName) {

        // if the database parameter is specified, then access the database with the specified name
        MongoDatabase db = databaseName != null ? mongoClient.getDatabase(databaseName) : this.database;

        // if no database is specified, then this is a bad request
        if (db == null)

            throw new IllegalArgumentException("Undefined database name");

        // otherwise, return the database
        return db;

    }

    public void close() {

        mongoClient.close();

        mongoClient = null;

    }



    /*
        databases
     */

    public MongoIterable<String> listDatabases() {

        // return the databases currently present in the MongoDB server
        return mongoClient.listDatabaseNames();

    }



    /*
        collections
     */

    public void createCollection(String database, String collectionName) throws IllegalArgumentException, MongoCommandException {

        // if the database parameter is specified, then access the database with the specified name
        MongoDatabase db = useDatabase(database);

        // if no database is specified, then this is a bad request
        if (db == null)

            throw new IllegalArgumentException("Undefined database name");

        // check the parameters
        if (collectionName == null)

            throw new IllegalArgumentException("Null-reference mandatory parameters");

        // create the collection in the database
        db.createCollection(collectionName);

    }

    public MongoCollection getCollection(String database, String collectionName) throws IllegalArgumentException, MongoCommandException {

        // if the database parameter is specified, then access the database with the specified name
        MongoDatabase db = useDatabase(database);

        // if no database is specified, then this is a bad request
        if (db == null)

            throw new IllegalArgumentException("Undefined database name");

        // check the parameters
        if (collectionName == null)

            throw new IllegalArgumentException("Null-reference mandatory parameters");

        // retrieve the collection with the specified name from the database
        return db.getCollection(collectionName);

    }

    public MongoIterable<String> listCollections(String database) {

        // if the database parameter is specified, then access the database with the specified name
        MongoDatabase db = useDatabase(database);

        // if no database is specified, then this is a bad request
        if (db == null)

            throw new IllegalArgumentException("Undefined database name");

        // return the collections in the selected database
        return db.listCollectionNames();

    }



    /*
        documents
     */

    public void createDocument(String database, String collectionName, Document document) throws IllegalArgumentException {

        // first, check the parameters
        if (collectionName == null || collectionName.length() == 0 || document == null) {

            String message = "Null-reference mandatory parameters";

            System.out.println(message);

            throw new IllegalArgumentException(message);

        }

        // if the database parameter is specified, then access the database with the specified name
        MongoDatabase db = useDatabase(database);

        // if no database is specified, then this is a bad request
        if (db == null)

            throw new IllegalArgumentException("Undefined database name");

        // retrieve the collection with the specified name from the database
        MongoCollection collection = db.getCollection(collectionName);

        // create the document in the retrieved collection (even if it does not exist)
        collection.insertOne(document);

    }

    public void createDocument(String database, String collection, String document) throws QueryFailedException {

        // first, check the parameters
        if (collection == null || collection.length() == 0 || document == null || document.length() == 0) {

            String message = "Null-reference mandatory parameters";

            System.out.println(message);

            throw new IllegalArgumentException(message);

        }

        try {

            Document documentBSON = Document.parse(document);

            createDocument(database, collection, documentBSON);

        } catch (Exception e) {

            throw new QueryFailedException(e.getMessage());

        }

    }

    public FindIterable<Document> find(String database, String collection, String query) throws Exception {

        // retrieve the collection with the specified name from the specified database
        MongoCollection dbCollection = getCollection(database, collection);

        // prepare the parsed query from the query string
        BasicDBObject parsedQuery = null;

        try {

            // use the find-all string if the query string is null or empty
            if (query == null || query.length() == 0)

                parsedQuery = BasicDBObject.parse("{}");

            // otherwise, try to parse the query string into a BSON
            else

                parsedQuery = BasicDBObject.parse(query);

        }

        catch (Exception e) {

            String message = "Invalid BSON format for the query";

            System.out.println(message);

            throw new Exception(message);

        }

        // return the documents matching the query
        return dbCollection.find(parsedQuery);

    }

    public String findToJSON(String database, String collection, String query) throws Exception {

        // perform the specified query on the specified database and collection
        FindIterable<Document> queryResult = find(database, collection, query);

        // initialize an empty list of JSON documents
        List<String> resultJSONs = new LinkedList<>();

        // inject the JSON of each document separating into the list of JSON documents
        for (Document document : queryResult) { resultJSONs.add(document.toJson()); }

        // prepare the JSON serializer
        ObjectMapper mapper = new ObjectMapper();

        SimpleModule module = new SimpleModule("DocumentSerializer");
        module.addSerializer(Document.class, new DocumentSerializer());
        mapper.registerModule(module);

        // generate and return the JSON representation of the list of JSON values
        return mapper.writeValueAsString(resultJSONs);

    }

    public FindIterable<Document> findAll(String database, String collection) throws Exception {

        // invoke the main method with the find-all MongoDB query
        return find(database, collection, "{}");

    }

}

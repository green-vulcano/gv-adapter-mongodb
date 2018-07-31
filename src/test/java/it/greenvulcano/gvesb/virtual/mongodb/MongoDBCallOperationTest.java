package it.greenvulcano.gvesb.virtual.mongodb;

import com.mongodb.MongoClient;
import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodProcess;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.IMongodConfig;
import de.flapdoodle.embed.mongo.config.MongodConfigBuilder;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.process.runtime.Network;
import it.greenvulcano.configuration.XMLConfig;
import it.greenvulcano.gvesb.buffer.GVBuffer;
import it.greenvulcano.gvesb.buffer.GVException;
import it.greenvulcano.gvesb.channel.mongodb.MongoDBChannel;
import it.greenvulcano.gvesb.core.GreenVulcano;
import it.greenvulcano.gvesb.virtual.OperationFactory;
import org.bson.Document;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.Assert.*;

public class MongoDBCallOperationTest {
	
	private final static Logger LOG = LoggerFactory.getLogger(MongoDBCallOperationTest.class);
	
	private static MongodExecutable mongodExecutable;
	private static MongodProcess mongod;
	
	//private final String CONNECTION_STRING = "mongodb://localhost:27017";
	
	@BeforeClass
	public static void init() throws Exception {
		MongodStarter starter = MongodStarter.getDefaultInstance();

		String bindIp = "localhost";
		int port = 27017;
		IMongodConfig mongodConfig = new MongodConfigBuilder()
			.version(Version.Main.PRODUCTION)
			.net(new Net(bindIp, port, Network.localhostIsIPv6()))
			.build();

		mongodExecutable = starter.prepare(mongodConfig);		
		mongod = mongodExecutable.start();
			
		MongoClient mongoClient = new MongoClient(bindIp, port);
	
		Path sampleDataPath = Paths.get(MongoDBCallOperationTest.class.getResource("/measures_1.json").toURI());
		String sampleDataJSON = Files.lines(sampleDataPath, Charset.forName("UTF-8"))
				           .collect(Collectors.joining());
		
		JSONArray devices = new JSONArray(sampleDataJSON);
		List<Document> deviceDocuments = IntStream.range(0, devices.length()) 
												 .mapToObj(devices::getJSONObject)
				                                 .map(JSONObject::toString)
				                                 .map(Document::parse)
				                                 .collect(Collectors.toList());
		LOG.debug("Loading sample data for devices");
		mongoClient.getDatabase("gviot").getCollection("measures_1").insertMany(deviceDocuments);
		
		mongoClient.close();
		
		
		XMLConfig.setBaseConfigPath(MongoDBCallOperationTest.class.getClassLoader().getResource(".").getPath());		
		OperationFactory.registerSupplier("mongodb-call", MongoDBCallOperation::new);	
				
		MongoDBChannel.setup();		
		
		
	}
	
	@Test
	public void testFind() throws GVException {
				
		GVBuffer inputGVBuffer = new GVBuffer();		
		inputGVBuffer.setService("TEST");
		inputGVBuffer.setProperty("FILTER", "{\"sensor.physicalId\": { $eq:\"BATTERY\" } }");
		inputGVBuffer.setObject("test input");		
		
		GreenVulcano greenVulcano = new GreenVulcano();		
		GVBuffer outputGVBuffer = greenVulcano.forward(inputGVBuffer, "testFind");
				
		assertNotNull(outputGVBuffer.getObject());
		
		JSONArray result = new JSONArray(outputGVBuffer.getObject().toString());
		assertTrue(IntStream.range(0, result.length())
					         .mapToObj(result::getJSONObject)
					         .map(measure-> measure.getJSONObject("sensor").getString("physicalId"))
							 .allMatch("BATTERY"::equals));
	}

	@Test
	public void testInsertOne() throws GVException {

		// CREATE a single BATTERY record

		GVBuffer inputCreateGVBuffer = new GVBuffer();
		inputCreateGVBuffer.setService("TEST");
		inputCreateGVBuffer.setObject("{ \"fakePhysicalId\": \"BATTERY\" }");

		GreenVulcano greenVulcano = new GreenVulcano();
		GVBuffer outputCreateGVBuffer = greenVulcano.forward(inputCreateGVBuffer, "testCreate");

		assertNotNull(outputCreateGVBuffer.getObject());

		JSONObject createResult = new JSONObject(outputCreateGVBuffer.getObject().toString());

		String insertedDocumentId = null;

		try {

			insertedDocumentId = createResult.getJSONObject("_id").getString("$oid");

		} catch (Exception e) { throw new GVException(e.getMessage()); }



		// READ the inserted record

		GVBuffer inputFindGVBuffer = new GVBuffer();
		inputFindGVBuffer.setService("TEST");
		inputFindGVBuffer.setProperty("FILTER", "{\"_id\": { $oid:\"" + insertedDocumentId + "\" } }");
		inputFindGVBuffer.setObject("test input");

		GVBuffer outputFindGVBuffer = greenVulcano.forward(inputFindGVBuffer, "testFind");

		assertNotNull(outputFindGVBuffer.getObject());

		JSONArray find1Result = new JSONArray(outputFindGVBuffer.getObject().toString());
		assertEquals(1, find1Result.length());
		assertTrue(IntStream.range(0, find1Result.length())
				.mapToObj(find1Result::getJSONObject)
				.map(battery-> battery.getJSONObject("_id").getString("$oid"))
				.allMatch(insertedDocumentId::equals));

	}

	@Test
	public void testInsertMany() throws GVException {

		// CREATE a BATTERY record and a GPS record

		GVBuffer inputCreateGVBuffer = new GVBuffer();
		inputCreateGVBuffer.setService("TEST");
		inputCreateGVBuffer.setObject("[{ \"fakePhysicalId\": \"BATTERY\" }, { \"fakePhysicalId\": \"GPS\" }]");

		GreenVulcano greenVulcano = new GreenVulcano();
		GVBuffer outputCreateGVBuffer = greenVulcano.forward(inputCreateGVBuffer, "testCreate");

		assertNotNull(outputCreateGVBuffer.getObject());

		JSONArray createResult = new JSONArray(outputCreateGVBuffer.getObject().toString());

		assertEquals(2, createResult.length());



		// READ the inserted records

		GVBuffer inputFindGVBuffer = new GVBuffer();
		inputFindGVBuffer.setService("TEST");
		inputFindGVBuffer.setProperty("FILTER", "{ \"$or\": [ { \"fakePhysicalId\": \"BATTERY\" }, { \"fakePhysicalId\": \"GPS\" } ] }");
		inputFindGVBuffer.setObject("test input");

		GVBuffer outputFindGVBuffer = greenVulcano.forward(inputFindGVBuffer, "testFind");

		assertNotNull(outputFindGVBuffer.getObject());

		JSONArray find1Result = new JSONArray(outputFindGVBuffer.getObject().toString());
		assertEquals(2, find1Result.length());
		assertTrue(IntStream.range(0, find1Result.length())
				.mapToObj(find1Result::getJSONObject)
				.map(device-> device.getString("fakePhysicalId")).allMatch(id -> id.equals("BATTERY") || id.equals("GPS")));

	}

	@Test
	public void testUpdate() throws GVException {

		// Update all BATTERY records adding new field

		GVBuffer inputGVBuffer = new GVBuffer();
		inputGVBuffer.setService("TEST");
		inputGVBuffer.setProperty("FILTER", "{\"sensor.physicalId\": { $eq:\"BATTERY\" } }");
		inputGVBuffer.setProperty("STATEMENT", "{$set :{\"verified\": true }}");
		inputGVBuffer.setObject("test input");

		GreenVulcano greenVulcano = new GreenVulcano();
		GVBuffer outputGVBuffer = greenVulcano.forward(inputGVBuffer, "testUpdate");

		assertEquals("2", outputGVBuffer.getProperty("REC_UPDATE"));

		// Test actual data update

		inputGVBuffer = new GVBuffer();
		inputGVBuffer.setService("TEST");
		inputGVBuffer.setProperty("FILTER", "{\"sensor.physicalId\": { $eq:\"BATTERY\" } }");
		inputGVBuffer.setObject("test input");

		greenVulcano = new GreenVulcano();
		outputGVBuffer = greenVulcano.forward(inputGVBuffer, "testFind");

		assertEquals("2", outputGVBuffer.getProperty("REC_READ"));

		JSONArray result = new JSONArray(outputGVBuffer.getObject().toString());
		assertTrue(IntStream.range(0, result.length())
					         .mapToObj(result::getJSONObject)
							 .allMatch(measure->measure.getBoolean("verified")));
	}

	@Test
	public void testDeleteOne() throws GVException {

		// CREATE a single BATTERY record

		GVBuffer inputCreateGVBuffer = new GVBuffer();
		inputCreateGVBuffer.setService("TEST");
		inputCreateGVBuffer.setObject("{ \"fakePhysicalId\": \"BATTERY\" }");

		GreenVulcano greenVulcano = new GreenVulcano();
		GVBuffer outputCreateGVBuffer = greenVulcano.forward(inputCreateGVBuffer, "testCreate");

		assertNotNull(outputCreateGVBuffer.getObject());

		JSONObject createResult = new JSONObject(outputCreateGVBuffer.getObject().toString());

		String insertedDocumentId = null;

		try {

			insertedDocumentId = createResult.getJSONObject("_id").getString("$oid");

		} catch (Exception e) { throw new GVException(e.getMessage()); }



		// READ the inserted record

		GVBuffer inputFind1GVBuffer = new GVBuffer();
		inputFind1GVBuffer.setService("TEST");
		inputFind1GVBuffer.setProperty("FILTER", "{\"_id\": { $oid:\"" + insertedDocumentId + "\" } }");
		inputFind1GVBuffer.setObject("test input");

		GVBuffer outputFind1GVBuffer = greenVulcano.forward(inputFind1GVBuffer, "testFind");

		assertNotNull(outputFind1GVBuffer.getObject());

		JSONArray find1Result = new JSONArray(outputFind1GVBuffer.getObject().toString());
		assertEquals(1, find1Result.length());
		assertTrue(IntStream.range(0, find1Result.length())
				.mapToObj(find1Result::getJSONObject)
				.map(battery-> battery.getJSONObject("_id").getString("$oid"))
				.allMatch(insertedDocumentId::equals));



		// DELETE the inserted record

		GVBuffer inputDeleteGVBuffer = new GVBuffer();
		inputDeleteGVBuffer.setService("TEST");
		inputDeleteGVBuffer.setObject("{\"_id\": { $oid:\"" + insertedDocumentId + "\" } }");

		GVBuffer outputDeleteGVBuffer = greenVulcano.forward(inputDeleteGVBuffer, "testDelete");

		assertNotNull(outputDeleteGVBuffer.getObject());

		JSONObject deleteResult = new JSONObject(outputDeleteGVBuffer.getObject().toString());

		assertFalse(deleteResult.isNull("deletedDocuments"));
		assertEquals(1, deleteResult.getInt("deletedDocuments"));



		// READ the deleted record

		GVBuffer inputFind2GVBuffer = new GVBuffer();
		inputFind2GVBuffer.setService("TEST");
		inputFind2GVBuffer.setProperty("FILTER", "{\"_id\": { $oid:\"" + insertedDocumentId + "\" } }");
		inputFind2GVBuffer.setObject("test input");

		GVBuffer outputFind2GVBuffer = greenVulcano.forward(inputFind2GVBuffer, "testFind");

		assertNotNull(outputFind2GVBuffer.getObject());

		JSONArray find2Result = new JSONArray(outputFind2GVBuffer.getObject().toString());
		assertEquals(0, find2Result.length());

	}

	@Test
	public void testDeleteMany() throws GVException {



		// CREATE a BATTERY record and a GPS record

		GVBuffer inputCreateGVBuffer = new GVBuffer();
		inputCreateGVBuffer.setService("TEST");
		inputCreateGVBuffer.setObject("[{ \"fakePhysicalId\": \"BATTERY\" }, { \"fakePhysicalId\": \"GPS\" }]");

		GreenVulcano greenVulcano = new GreenVulcano();
		GVBuffer outputCreateGVBuffer = greenVulcano.forward(inputCreateGVBuffer, "testCreate");

		assertNotNull(outputCreateGVBuffer.getObject());

		JSONArray createResult = new JSONArray(outputCreateGVBuffer.getObject().toString());

		assertEquals(2, createResult.length());



		// READ the inserted records

		GVBuffer inputFind1GVBuffer = new GVBuffer();
		inputFind1GVBuffer.setService("TEST");
		inputFind1GVBuffer.setProperty("FILTER", "{ \"$or\": [ { \"fakePhysicalId\": \"BATTERY\" }, { \"fakePhysicalId\": \"GPS\" } ] }");
		inputFind1GVBuffer.setObject("test input");

		GVBuffer outputFind1GVBuffer = greenVulcano.forward(inputFind1GVBuffer, "testFind");

		assertNotNull(outputFind1GVBuffer.getObject());

		JSONArray find1Result = new JSONArray(outputFind1GVBuffer.getObject().toString());
		assertEquals(2, find1Result.length());
		assertTrue(IntStream.range(0, find1Result.length())
				.mapToObj(find1Result::getJSONObject)
				.map(device-> device.getString("fakePhysicalId")).allMatch(id -> id.equals("BATTERY") || id.equals("GPS")));



		// DELETE the inserted records

		GVBuffer inputDeleteGVBuffer = new GVBuffer();
		inputDeleteGVBuffer.setService("TEST");
		inputDeleteGVBuffer.setObject("{ \"$or\": [ { \"fakePhysicalId\": \"BATTERY\" }, { \"fakePhysicalId\": \"GPS\" } ] }");

		GVBuffer outputDeleteGVBuffer = greenVulcano.forward(inputDeleteGVBuffer, "testDelete");

		assertNotNull(outputDeleteGVBuffer.getObject());

		JSONObject deleteResult = new JSONObject(outputDeleteGVBuffer.getObject().toString());

		assertFalse(deleteResult.isNull("deletedDocuments"));
		assertEquals(2, deleteResult.getInt("deletedDocuments"));



		// READ the deleted record

		GVBuffer inputFind2GVBuffer = new GVBuffer();
		inputFind2GVBuffer.setService("TEST");
		inputFind2GVBuffer.setProperty("FILTER", "{ \"$or\": [ { \"fakePhysicalId\": \"BATTERY\" }, { \"fakePhysicalId\": \"GPS\" } ] }");
		inputFind2GVBuffer.setObject("test input");

		GVBuffer outputFind2GVBuffer = greenVulcano.forward(inputFind2GVBuffer, "testFind");

		assertNotNull(outputFind2GVBuffer.getObject());

		JSONArray find2Result = new JSONArray(outputFind2GVBuffer.getObject().toString());
		assertEquals(0, find2Result.length());

	}

	@AfterClass
	public static void destroy() throws Exception {		
		
		MongoDBChannel.shutdown();
	
		mongod.stop();
		mongodExecutable.stop();
	}

}

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
import org.junit.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.Assert.*;

public class MongoDBCallOperationTest {

    private final static Logger LOG = LoggerFactory.getLogger(MongoDBCallOperationTest.class);

    private static MongodExecutable mongodExecutable;
    private static MongodProcess mongod;

    private static MongoClient mongoClient;

    @BeforeClass
    public static void init() throws Exception {

        MongodStarter starter = MongodStarter.getDefaultInstance();

        String bindIp = "localhost";
        int port = 27017;
        IMongodConfig mongodConfig = new MongodConfigBuilder().version(Version.Main.PRODUCTION).net(new Net(bindIp, port, Network.localhostIsIPv6())).build();

        mongodExecutable = starter.prepare(mongodConfig);
        mongod = mongodExecutable.start();

        mongoClient = new MongoClient(bindIp, port);

        XMLConfig.setBaseConfigPath(MongoDBCallOperationTest.class.getClassLoader().getResource(".").getPath());
        OperationFactory.registerSupplier("mongodb-call", MongoDBCallOperation::new);
        OperationFactory.registerSupplier("mongodb-create-user-call", MongoDBCreateUserCallOperation::new);

        MongoDBChannel.setup();

    }

    @Before
    public void initDb() throws URISyntaxException, IOException {

        Path sampleDataPath = Paths.get(MongoDBCallOperationTest.class.getResource("/measures_1.json").toURI());
        String sampleDataJSON = Files.lines(sampleDataPath, Charset.forName("UTF-8")).collect(Collectors.joining());

        JSONArray devices = new JSONArray(sampleDataJSON);
        List<Document> deviceDocuments = IntStream.range(0, devices.length())
                                                  .mapToObj(devices::getJSONObject)
                                                  .map(JSONObject::toString)
                                                  .map(Document::parse)
                                                  .collect(Collectors.toList());

        LOG.debug("Loading sample data for devices");
        mongoClient.getDatabase("gviot").getCollection("measures_1").insertMany(deviceDocuments);
    }

    @After
    public void cleanDb() {

        mongoClient.getDatabase("gviot").getCollection("measures_1").deleteMany(Document.parse("{}"));
    }
    
    @Test
    public void testCreateIndex() throws GVException {
        
        assertFalse( mongoClient.getDatabase("gviot").listCollectionNames().into(new HashSet<>()).contains("test_index") );
        
        GVBuffer inputGVBuffer = new GVBuffer();
        inputGVBuffer.setService("TEST");
        
        GreenVulcano greenVulcano = new GreenVulcano();
        GVBuffer result = greenVulcano.forward(inputGVBuffer, "testCreateIndex");
        
        JSONArray createIndexes = new JSONArray(result.getObject().toString());
        
        assertEquals(2, createIndexes.length());
        
        List<String> indexes = new LinkedList<>();
        mongoClient.getDatabase("gviot").getCollection("test_index").listIndexes().map(d-> d.toJson()).into(indexes);
        assertEquals(3, indexes.size());
        
    }
    
    @Test
    public void testCreateUser() throws GVException {
                        
        GVBuffer inputGVBuffer = new GVBuffer();
        inputGVBuffer.setService("TEST");
        
        GreenVulcano greenVulcano = new GreenVulcano();
        GVBuffer result = greenVulcano.forward(inputGVBuffer, "testCreateUser");
        
        assertEquals("{\"ok\": 1.0}", result.getObject().toString());
    }

    @Test
    public void testFind() throws GVException {

        GVBuffer inputGVBuffer = new GVBuffer();
        inputGVBuffer.setService("TEST");
        inputGVBuffer.setProperty("FILTER", "{\"sensor.physicalId\": { $eq:\"BATTERY\" } }");
        inputGVBuffer.setProperty("SORT", "{\"timestamp\": -1 }");

        GreenVulcano greenVulcano = new GreenVulcano();
        GVBuffer outputGVBuffer = greenVulcano.forward(inputGVBuffer, "testFind");

        assertNotNull(outputGVBuffer.getObject());

        JSONArray result = new JSONArray(outputGVBuffer.getObject().toString());
        assertTrue(IntStream.range(0, result.length())
                            .mapToObj(result::getJSONObject)
                            .map(measure -> measure.getJSONObject("sensor").getString("physicalId"))
                            .allMatch("BATTERY"::equals));

        inputGVBuffer.setProperty("limit", "1");
        outputGVBuffer = greenVulcano.forward(inputGVBuffer, "testFind");

        assertNotNull(outputGVBuffer.getObject());

        result = new JSONArray(outputGVBuffer.getObject().toString());

        assertEquals(1, result.length());

    }

    @Test
    public void testSort() throws GVException {

        GVBuffer inputGVBuffer = new GVBuffer();
        inputGVBuffer.setService("TEST");
        inputGVBuffer.setProperty("FILTER", "{}");
        inputGVBuffer.setProperty("SORT", "{\"sensor.physicalId\": 1 }");

        GreenVulcano greenVulcano = new GreenVulcano();
        GVBuffer outputGVBuffer = greenVulcano.forward(inputGVBuffer, "testFind");

        assertNotNull(outputGVBuffer.getObject());

        JSONArray result = new JSONArray(outputGVBuffer.getObject().toString());
        assertEquals("ACCELEROMETER", result.getJSONObject(0).query("/sensor/physicalId").toString());

        // Reverse order
        inputGVBuffer.setProperty("SORT", "{\"sensor.physicalId\": -1 }");
        outputGVBuffer = greenVulcano.forward(inputGVBuffer, "testFind");

        result = new JSONArray(outputGVBuffer.getObject().toString());
        assertEquals("GPS", result.getJSONObject(0).query("/sensor/physicalId").toString());

    }

    @Test
    public void testSkipLimit() throws GVException {

        GVBuffer inputGVBuffer = new GVBuffer();
        inputGVBuffer.setService("TEST");
        inputGVBuffer.setProperty("FILTER", "{}");
        inputGVBuffer.setProperty("SORT", "{}");
        inputGVBuffer.setProperty("offset", "4");

        GreenVulcano greenVulcano = new GreenVulcano();
        GVBuffer outputGVBuffer = greenVulcano.forward(inputGVBuffer, "testFind");

        assertNotNull(outputGVBuffer.getObject());

        JSONArray resultJSON = new JSONArray(outputGVBuffer.getObject().toString());

        assertNotNull(resultJSON);

        assertEquals(1, resultJSON.length());

        inputGVBuffer.setProperty("offset", "0");
        inputGVBuffer.setProperty("limit", "5");

        outputGVBuffer = greenVulcano.forward(inputGVBuffer, "testFind");

        assertNotNull(outputGVBuffer.getObject());

        resultJSON = new JSONArray(outputGVBuffer.getObject().toString());

        assertNotNull(resultJSON);

        assertEquals(5, resultJSON.length());

        inputGVBuffer.setProperty("offset", "3");
        inputGVBuffer.setProperty("limit", "1");

        outputGVBuffer = greenVulcano.forward(inputGVBuffer, "testFind");

        assertNotNull(outputGVBuffer.getObject());

        resultJSON = new JSONArray(outputGVBuffer.getObject().toString());

        assertNotNull(resultJSON);

        assertEquals(1, resultJSON.length());

        inputGVBuffer.setProperty("offset", "4");
        inputGVBuffer.setProperty("limit", "5");

        outputGVBuffer = greenVulcano.forward(inputGVBuffer, "testFind");

        assertNotNull(outputGVBuffer.getObject());

        resultJSON = new JSONArray(outputGVBuffer.getObject().toString());

        assertNotNull(resultJSON);

        assertEquals(1, resultJSON.length());

    }

    @Test
    public void testProjectionWhitelist() throws GVException {

        GVBuffer inputGVBuffer = new GVBuffer();
        inputGVBuffer.setService("TEST");
        inputGVBuffer.setProperty("FILTER", "{}");
        inputGVBuffer.setProperty("SORT", "{}");
        inputGVBuffer.setProperty("PROJECTION", "{ \"sensor.device.physicalId\" : 1 }");

        GreenVulcano greenVulcano = new GreenVulcano();
        GVBuffer outputGVBuffer = greenVulcano.forward(inputGVBuffer, "testFind");

        assertNotNull(outputGVBuffer.getObject());

        JSONArray resultJSON = new JSONArray(outputGVBuffer.getObject().toString());

        assertNotNull(resultJSON);

        assertEquals(5, resultJSON.length());

        Set<String> expectedRootKeySet = new HashSet<>();
        expectedRootKeySet.add("_id");
        expectedRootKeySet.add("sensor");

        Set<String> expectedSensorKeySet = new HashSet<>();
        expectedSensorKeySet.add("device");

        Set<String> expectedDeviceKeySet = new HashSet<>();
        expectedDeviceKeySet.add("physicalId");

        for (int i = 0; i < resultJSON.length(); i++) {

            JSONObject element = resultJSON.getJSONObject(i);

            assertEquals(element.keySet(), expectedRootKeySet);

            JSONObject sensor = element.getJSONObject("sensor");

            assertEquals(sensor.keySet(), expectedSensorKeySet);

            JSONObject device = sensor.getJSONObject("device");

            assertEquals(device.keySet(), expectedDeviceKeySet);

        }

    }

    @Test
    public void testProjectionBlacklist() throws GVException {

        GVBuffer inputGVBuffer = new GVBuffer();
        inputGVBuffer.setService("TEST");
        inputGVBuffer.setProperty("FILTER", "{}");
        inputGVBuffer.setProperty("SORT", "{}");
        inputGVBuffer.setProperty("PROJECTION", "{ \"sensor.device.physicalId\" : 0, \"sensor.physicalId\": 0, \"type\": 0 }");

        GreenVulcano greenVulcano = new GreenVulcano();
        GVBuffer outputGVBuffer = greenVulcano.forward(inputGVBuffer, "testFind");

        assertNotNull(outputGVBuffer.getObject());

        JSONArray resultJSON = new JSONArray(outputGVBuffer.getObject().toString());

        assertNotNull(resultJSON);

        assertEquals(5, resultJSON.length());

        Set<String> expectedRootKeySet = new HashSet<>();
        expectedRootKeySet.add("_id");
        expectedRootKeySet.add("sensor");

        Set<String> expectedSensorKeySet = new HashSet<>();
        expectedSensorKeySet.add("device");

        Set<String> expectedDeviceKeySet = new HashSet<>();
        expectedDeviceKeySet.add("physicalId");

        for (int i = 0; i < resultJSON.length(); i++) {

            JSONObject element = resultJSON.getJSONObject(i);

            assertFalse(element.keySet().contains("type"));

            JSONObject sensor = element.getJSONObject("sensor");

            assertFalse(sensor.keySet().contains("physicalId"));

            JSONObject device = sensor.getJSONObject("device");

            assertFalse(device.keySet().contains("physicalId"));

        }

    }

    @Test
    public void testInsertOne() throws GVException {

        // CREATE a single BATTERY record

        GVBuffer inputCreateGVBuffer = new GVBuffer();
        inputCreateGVBuffer.setService("TEST");
        inputCreateGVBuffer.setObject("{ \"fakePhysicalId\": \"BATTERY\", \"timestamp\": " +System.currentTimeMillis()
                + " }");

        GreenVulcano greenVulcano = new GreenVulcano();
        GVBuffer outputCreateGVBuffer = greenVulcano.forward(inputCreateGVBuffer, "testCreate");

        assertNotNull(outputCreateGVBuffer.getObject());

        JSONObject createResult = new JSONObject(outputCreateGVBuffer.getObject().toString());

        String insertedDocumentId = null;

        try {

            insertedDocumentId = createResult.getJSONObject("_id").getString("$oid");

        } catch (Exception e) {
            throw new GVException(e.getMessage());
        }

        // READ the inserted record

        GVBuffer inputFindGVBuffer = new GVBuffer();
        inputFindGVBuffer.setService("TEST");
        inputFindGVBuffer.setProperty("FILTER", "{\"_id\": { $oid:\"" + insertedDocumentId + "\" } }");
        inputFindGVBuffer.setProperty("SORT", "{\"timestamp\": -1 }");
        inputFindGVBuffer.setObject("test input");

        GVBuffer outputFindGVBuffer = greenVulcano.forward(inputFindGVBuffer, "testFind");

        assertNotNull(outputFindGVBuffer.getObject());

        JSONArray find1Result = new JSONArray(outputFindGVBuffer.getObject().toString());
        assertEquals(1, find1Result.length());
        assertTrue(IntStream.range(0, find1Result.length())
                            .mapToObj(find1Result::getJSONObject)
                            .map(battery -> battery.getJSONObject("_id").getString("$oid"))
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
        inputFindGVBuffer.setProperty("SORT", "{\"timestamp\": -1 }");
        inputFindGVBuffer.setObject("test input");

        GVBuffer outputFindGVBuffer = greenVulcano.forward(inputFindGVBuffer, "testFind");

        assertNotNull(outputFindGVBuffer.getObject());

        JSONArray find1Result = new JSONArray(outputFindGVBuffer.getObject().toString());
        assertEquals(2, find1Result.length());
        assertTrue(IntStream.range(0, find1Result.length())
                            .mapToObj(find1Result::getJSONObject)
                            .map(device -> device.getString("fakePhysicalId"))
                            .allMatch(id -> id.equals("BATTERY") || id.equals("GPS")));

    }

    @Test
    public void testUpdate() throws GVException {

        // Update all BATTERY records 
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
        inputGVBuffer.setProperty("SORT", "{\"timestamp\": -1 }");
        inputGVBuffer.setObject("test input");

        greenVulcano = new GreenVulcano();
        outputGVBuffer = greenVulcano.forward(inputGVBuffer, "testFind");

        assertEquals("2", outputGVBuffer.getProperty("REC_READ"));

        JSONArray result = new JSONArray(outputGVBuffer.getObject().toString());
        assertTrue(IntStream.range(0, result.length()).mapToObj(result::getJSONObject).allMatch(measure -> measure.getBoolean("verified")));
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

        } catch (Exception e) {
            throw new GVException(e.getMessage());
        }

        // READ the inserted record

        GVBuffer inputFind1GVBuffer = new GVBuffer();
        inputFind1GVBuffer.setService("TEST");
        inputFind1GVBuffer.setProperty("FILTER", "{\"_id\": { $oid:\"" + insertedDocumentId + "\" } }");
        inputFind1GVBuffer.setProperty("SORT", "{\"timestamp\": -1 }");
        inputFind1GVBuffer.setObject("test input");

        GVBuffer outputFind1GVBuffer = greenVulcano.forward(inputFind1GVBuffer, "testFind");

        assertNotNull(outputFind1GVBuffer.getObject());

        JSONArray find1Result = new JSONArray(outputFind1GVBuffer.getObject().toString());
        assertEquals(1, find1Result.length());
        assertTrue(IntStream.range(0, find1Result.length())
                            .mapToObj(find1Result::getJSONObject)
                            .map(battery -> battery.getJSONObject("_id").getString("$oid"))
                            .allMatch(insertedDocumentId::equals));

        // DELETE the inserted record

        GVBuffer inputDeleteGVBuffer = new GVBuffer();
        inputDeleteGVBuffer.setService("TEST");
        inputDeleteGVBuffer.setObject("{\"_id\": { $oid:\"" + insertedDocumentId + "\" } }");

        GVBuffer outputDeleteGVBuffer = greenVulcano.forward(inputDeleteGVBuffer, "testDelete");
        assertEquals("1", outputDeleteGVBuffer.getProperty("REC_DELETED"));

        // READ the deleted record

        GVBuffer inputFind2GVBuffer = new GVBuffer();
        inputFind2GVBuffer.setService("TEST");
        inputFind2GVBuffer.setProperty("FILTER", "{\"_id\": { $oid:\"" + insertedDocumentId + "\" } }");
        inputFind2GVBuffer.setProperty("SORT", "{\"timestamp\": -1 }");
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
        inputFind1GVBuffer.setProperty("SORT", "{\"timestamp\": -1 }");
        inputFind1GVBuffer.setObject("test input");

        GVBuffer outputFind1GVBuffer = greenVulcano.forward(inputFind1GVBuffer, "testFind");

        assertNotNull(outputFind1GVBuffer.getObject());

        JSONArray find1Result = new JSONArray(outputFind1GVBuffer.getObject().toString());
        assertEquals(2, find1Result.length());
        assertTrue(IntStream.range(0, find1Result.length())
                            .mapToObj(find1Result::getJSONObject)
                            .map(device -> device.getString("fakePhysicalId"))
                            .allMatch(id -> id.equals("BATTERY") || id.equals("GPS")));

        // DELETE the inserted records

        GVBuffer inputDeleteGVBuffer = new GVBuffer();
        inputDeleteGVBuffer.setService("TEST");
        inputDeleteGVBuffer.setObject("{ \"$or\": [ { \"fakePhysicalId\": \"BATTERY\" }, { \"fakePhysicalId\": \"GPS\" } ] }");

        GVBuffer outputDeleteGVBuffer = greenVulcano.forward(inputDeleteGVBuffer, "testDelete");
        assertEquals("2", outputDeleteGVBuffer.getProperty("REC_DELETED"));

        // READ the deleted record

        GVBuffer inputFind2GVBuffer = new GVBuffer();
        inputFind2GVBuffer.setService("TEST");
        inputFind2GVBuffer.setProperty("FILTER", "{ \"$or\": [ { \"fakePhysicalId\": \"BATTERY\" }, { \"fakePhysicalId\": \"GPS\" } ] }");
        inputFind2GVBuffer.setProperty("SORT", "{\"timestamp\": -1 }");
        inputFind2GVBuffer.setObject("test input");

        GVBuffer outputFind2GVBuffer = greenVulcano.forward(inputFind2GVBuffer, "testFind");
        assertEquals("0", outputFind2GVBuffer.getProperty("REC_READ"));

    }

    @Test
    public void testAggregate() throws GVException {

        GVBuffer inputGVBuffer = new GVBuffer();
        inputGVBuffer.setService("TEST");
        inputGVBuffer.setProperty("FILTER", "{$match : {\"sensor.device.physicalId\": { $eq:\"AA0011223344\" } }}");
        inputGVBuffer.setProperty("STATEMENT", "{ $group: {_id: \"$sensor.device.physicalId\", \"readings\" : {$sum: 1 } } }");

        GreenVulcano greenVulcano = new GreenVulcano();
        GVBuffer outputGVBuffer = greenVulcano.forward(inputGVBuffer, "testAggregation");

        assertNotNull(outputGVBuffer.getObject());

        JSONArray result = new JSONArray(outputGVBuffer.getObject().toString());

        assertEquals(1, result.length());
        assertEquals(4, result.getJSONObject(0).getInt("readings"));

    }
    
    @Test
    public void testLocalConnection() throws GVException {

        GreenVulcano greenVulcano = new GreenVulcano();
        
        GVBuffer inputFindGVBuffer = new GVBuffer();
        inputFindGVBuffer.setService("TEST");
        inputFindGVBuffer.setProperty("FILTER", "{\"sensor.physicalId\": { $eq:\"BATTERY\" } }");
        inputFindGVBuffer.setProperty("SORT", "{\"timestamp\": -1 }");
        inputFindGVBuffer.setProperty("MONGO_HOST", "127.0.0.1");
        inputFindGVBuffer.setProperty("MONGO_PORT", "27017");
        inputFindGVBuffer.setObject("test input");

        GVBuffer outputFindGVBuffer = greenVulcano.forward(inputFindGVBuffer, "testLocalConnection");
        assertEquals("2", outputFindGVBuffer.getProperty("REC_READ"));
        
    }
    
    @AfterClass
    public static void destroy() throws Exception {

        mongoClient.close();

        MongoDBChannel.shutdown();

        mongod.stop();
        mongodExecutable.stop();
    }

}

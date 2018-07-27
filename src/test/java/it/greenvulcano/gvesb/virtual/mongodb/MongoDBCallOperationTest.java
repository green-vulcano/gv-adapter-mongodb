package it.greenvulcano.gvesb.virtual.mongodb;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.bson.Document;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
		inputGVBuffer.setObject("test input");
		
		GreenVulcano greenVulcano = new GreenVulcano();		
		GVBuffer outputGVBuffer = greenVulcano.forward(inputGVBuffer, "testCallOperation");
				
		assertNotNull(outputGVBuffer.getObject());
		
		JSONArray result = new JSONArray(outputGVBuffer.getObject().toString());
		assertTrue(IntStream.range(0, result.length())
					         .mapToObj(result::getJSONObject)
					         .map(measure-> measure.getJSONObject("sensor").getString("physicalId"))
							 .allMatch("BATTERY"::equals));	
	}
	
	@AfterClass
	public static void destroy() throws Exception {		
		
		MongoDBChannel.shutdown();
	
		mongod.stop();
		mongodExecutable.stop();
	}
}

package it.greenvulcano.gvesb.channel;

import it.greenvulcano.configuration.XMLConfig;
import it.greenvulcano.configuration.XMLConfigException;
import it.greenvulcano.gvesb.core.config.GreenVulcanoConfig;
import it.greenvulcano.util.xml.XMLUtils;
import it.greenvulcano.util.xpath.XPathFinder;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;

public class MongoDBChannel {
	
	private final static Logger LOG = LoggerFactory.getLogger(MongoDBChannel.class);
	private final static Map<String, MongoClient> mongoClients = new HashMap<>();
		
	static void setup() {
		
		try {
		
			NodeList mongoChannelList = XMLConfig.getNodeList(GreenVulcanoConfig.getSystemsConfigFileName(),"//Channel[@type='MongoDBAdapter' and @enabled='true']");
			
			LOG.debug("Enabled MongoDBAdapter channels found: "+mongoChannelList.getLength());
			IntStream.range(0, mongoChannelList.getLength())
	        		 .mapToObj(mongoChannelList::item)		         
	        		 .forEach(MongoDBChannel::buildMongoClient);
		
		} catch (XMLConfigException e) {
			LOG.error("Error reading configuration", e);
		}
	}

	static void shutdown() {

		for (Entry<String, MongoClient> client : mongoClients.entrySet()) {
			
			try {
				client.getValue().close();
			} catch (Exception e) {
				LOG.error("Error closing client for Channel " + client.getKey(), e);
			}
		}
		
		mongoClients.clear();
		
	}
	
	
    private static void buildMongoClient(Node mongoChannelNode) {
    	try {
    		
    		String uri = XMLUtils.get_S(mongoChannelNode, "@endpoint");    		
    		MongoClientURI mongoClientURI = new MongoClientURI(uri);
    		
    		mongoClients.put(XPathFinder.buildXPath(mongoChannelNode), new MongoClient(mongoClientURI));
    		
    	} catch (Exception e) {
    		 LOG.error("Error configuring MongoClient", e);
		}
    	
    }
    
    public static Optional<MongoClient> getMongoClient(Node callOperationNode) {
    	
    	String xpath = XPathFinder.buildXPath(callOperationNode.getParentNode());
    	
    	return Optional.ofNullable(mongoClients.get(xpath));
    	
    }
	

	

}

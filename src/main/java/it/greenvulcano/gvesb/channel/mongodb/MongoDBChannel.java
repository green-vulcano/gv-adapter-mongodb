package it.greenvulcano.gvesb.channel.mongodb;

import it.greenvulcano.configuration.XMLConfig;
import it.greenvulcano.configuration.XMLConfigException;
import it.greenvulcano.gvesb.channel.mongodb.service.MongoClientProvider;
import it.greenvulcano.gvesb.core.config.GreenVulcanoConfig;
import it.greenvulcano.gvesb.j2ee.JNDIHelper;
import it.greenvulcano.util.metadata.PropertiesHandler;
import it.greenvulcano.util.metadata.PropertiesHandlerException;
import it.greenvulcano.util.xml.XMLUtils;
import it.greenvulcano.util.xml.XMLUtilsException;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.IntStream;

import javax.naming.NamingException;

import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.mongodb.ConnectionString;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;

public class MongoDBChannel {

    private final static Logger LOG = LoggerFactory.getLogger(MongoDBChannel.class);
    private final static ConcurrentMap <String, MongoClient> mongoClients = new ConcurrentHashMap<>();
    private final static JNDIHelper jndiContext = new JNDIHelper();

    public static void setup() {

        try {

            NodeList mongoChannelList = XMLConfig.getNodeList(GreenVulcanoConfig.getSystemsConfigFileName(), "//Channel[@type='MongoDBAdapter']");

            LOG.debug("Enabled MongoDBAdapter channels found: " + mongoChannelList.getLength());
            IntStream.range(0, mongoChannelList.getLength())
                     .mapToObj(mongoChannelList::item)
                     .map(MongoDBChannel::getChannelEndpoint)
                     .filter(Optional::isPresent)
                     .map(Optional::get)
                     .forEach(MongoDBChannel::getMongoClient);

        } catch (XMLConfigException e) {
            LOG.error("Error reading configuration", e);
        }
    }

    public static void shutdown() {

        for (Entry<String, MongoClient> client : mongoClients.entrySet()) {

            try {
                client.getValue().close();
            } catch (Exception e) {
                LOG.error("Error closing client for Channel " + client.getKey(), e);
            }
        }

        mongoClients.clear();

    }

    private static Optional<String> getChannelEndpoint(Node mongoChannelNode) {

        try {

            if (XMLConfig.exists(mongoChannelNode, "@endpoint") && XMLConfig.getBoolean(mongoChannelNode, "@enabled", true)) {

                LOG.info("Configuring MongoClient instance for Channel " + XMLUtils.get_S(mongoChannelNode, "@id-channel") + " in System"
                          + XMLUtils.get_S(mongoChannelNode.getParentNode(), "@id-system"));

                String uri = PropertiesHandler.expand(XMLUtils.get_S(mongoChannelNode, "@endpoint"));
                LOG.debug("MongoDB URI: "+uri);
                return Optional.of(uri);

            }

        } catch (Exception e) {
            LOG.error("Error configuring MongoClient", e);
        }
        
        return Optional.empty();

    }
    
    private static MongoClient buildMongoClient(String uri) {
        
        try {
             ConnectionString connectionString = new ConnectionString(uri);
             return MongoClients.create(connectionString);
             
        } catch (Exception e) {
            LOG.error("Error configuring MongoClient", e);
        }
        
        return null;
        
    }

    public static Optional<MongoClient> getMongoClient(String uri) {
        
        String key = DigestUtils.md5Hex(uri);        
        MongoClient client = mongoClients.computeIfAbsent(key, k ->  buildMongoClient(uri));  
        
        return Optional.ofNullable(client);
    }    
    
    public static Optional<MongoClient> getMongoClient(Node callOperationNode) {

        Optional<MongoClient> client = Optional.empty();

        try {
           if (XMLConfig.exists(callOperationNode, "@client-jndi-name")) {

                String jndiName = XMLConfig.get(callOperationNode, "@client-jndi-name");
                LOG.debug("Retrieving MongoClientProvider by  JNDI name: " + jndiName);

                MongoClientProvider mongoClientProvider = (MongoClientProvider) jndiContext.lookup(jndiName);
                client = Optional.ofNullable(mongoClientProvider.getMongoClient());

            } else {

                String uri = PropertiesHandler.expand(XMLUtils.get_S(callOperationNode.getParentNode(), "@endpoint"));               
                LOG.debug("Retrieving MongoClient from Channel map for URI: " + uri);

                client = Optional.ofNullable(mongoClients.get(DigestUtils.md5Hex(uri)));

            }
        } catch (PropertiesHandlerException|XMLUtilsException|XMLConfigException e) {
            LOG.debug("Error reading XML config", e);
        } catch (NamingException e) {
            LOG.debug("Error retrieving MongoClientProvider from JNDI context", e);
        } 

        return client;
    }

}
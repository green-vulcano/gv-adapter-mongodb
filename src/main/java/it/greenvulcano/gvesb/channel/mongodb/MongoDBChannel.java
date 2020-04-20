package it.greenvulcano.gvesb.channel.mongodb;

import it.greenvulcano.configuration.XMLConfig;
import it.greenvulcano.configuration.XMLConfigException;
import it.greenvulcano.gvesb.channel.mongodb.service.MongoClientProvider;
import it.greenvulcano.gvesb.core.config.GreenVulcanoConfig;
import it.greenvulcano.gvesb.j2ee.JNDIHelper;
import it.greenvulcano.util.metadata.PropertiesHandler;
import it.greenvulcano.util.xml.XMLUtils;
import it.greenvulcano.util.xpath.XPathFinder;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.IntStream;

import javax.naming.NamingException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.mongodb.ConnectionString;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;

public class MongoDBChannel {

    private final static Logger LOG = LoggerFactory.getLogger(MongoDBChannel.class);
    private final static Map<String, MongoClient> mongoClients = new HashMap<>();
    private final static JNDIHelper jndiContext = new JNDIHelper();

    public static void setup() {

        try {

            NodeList mongoChannelList = XMLConfig.getNodeList(GreenVulcanoConfig.getSystemsConfigFileName(), "//Channel[@type='MongoDBAdapter']");

            LOG.debug("Enabled MongoDBAdapter channels found: " + mongoChannelList.getLength());
            IntStream.range(0, mongoChannelList.getLength()).mapToObj(mongoChannelList::item).forEach(MongoDBChannel::buildMongoClient);

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

    private static void buildMongoClient(Node mongoChannelNode) {

        try {

            if (XMLConfig.exists(mongoChannelNode, "@endpoint") && XMLConfig.getBoolean(mongoChannelNode, "@enabled", true)) {

                LOG.debug("Configuring MongoClient instance for Channel " + XMLUtils.get_S(mongoChannelNode, "@id-channel") + " in System"
                          + XMLUtils.get_S(mongoChannelNode.getParentNode(), "@id-system"));

                String uri = PropertiesHandler.expand(XMLUtils.get_S(mongoChannelNode, "@endpoint"));
                ConnectionString connectionString = new ConnectionString(uri);
                mongoClients.put(XPathFinder.buildXPath(mongoChannelNode), MongoClients.create(connectionString));

            }

        } catch (Exception e) {
            LOG.error("Error configuring MongoClient", e);
        }

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

                String xpath = XPathFinder.buildXPath(callOperationNode.getParentNode());
                LOG.debug("Retrieving MongoClient from Channel map using key: " + xpath);

                client = Optional.ofNullable(mongoClients.get(xpath));

            }
        } catch (XMLConfigException e) {
            LOG.debug("Error reading XML config", e);
        } catch (NamingException e) {
            LOG.debug("Error retrieving MongoClientProvider from JNDI context", e);
        }

        return client;
    }

}
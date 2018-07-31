package it.greenvulcano.gvesb.virtual.mongodb.dbo;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;

import it.greenvulcano.configuration.XMLConfig;
import it.greenvulcano.configuration.XMLConfigException;

public class MongoDBOFactory {

	private final static Logger LOG = LoggerFactory.getLogger(MongoDBOFactory.class);

	static final Map<String, Function<Node, Optional<MongoDBO>>> dboSuppliers = new LinkedHashMap<>();
	
	static {

		dboSuppliers.put(MongoDBOFind.NAME, MongoDBOFind.BUILDER);

		dboSuppliers.put(MongoDBOUpdate.NAME, MongoDBOUpdate.BUILDER);

		dboSuppliers.put(MongoDBOInsert.NAME, MongoDBOInsert.BUILDER);

		dboSuppliers.put(MongoDBODelete.NAME, MongoDBODelete.BUILDER);

		// TODO repeat for each DBO operation

	}

	public static MongoDBO build(Node callOperationNode) throws XMLConfigException {

		Node dboConfigurationNode = XMLConfig.getNode(callOperationNode, "./*[1]"); // extract the first child node to return its corresponding MongoDBO object
		
		if (dboConfigurationNode != null && dboSuppliers.containsKey(dboConfigurationNode.getNodeName())) {

			LOG.debug("Looking for DBO with name " + dboConfigurationNode.getNodeName());

			Optional<MongoDBO> dbo = dboSuppliers.get(dboConfigurationNode.getNodeName()).apply(dboConfigurationNode);

			return dbo.orElseThrow(() -> new XMLConfigException("Configuration failed for DBO [" + dboConfigurationNode.getNodeName() + "]"));

		}

		throw new XMLConfigException("No valid DBO found for node" + (dboConfigurationNode != null ? " " + dboConfigurationNode.getNodeName() : ""));

	}

}
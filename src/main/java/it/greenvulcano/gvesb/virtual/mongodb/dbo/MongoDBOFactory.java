package it.greenvulcano.gvesb.virtual.mongodb.dbo;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import org.w3c.dom.Node;

import it.greenvulcano.configuration.XMLConfigException;

public class MongoDBOFactory {

	static final Map<String, Function<Node, Optional<MongoDBO>>> dboSuppliers = new LinkedHashMap<>();
	
	static {			
		dboSuppliers.put(MongoDBOFind.NAME, MongoDBOFind.BUILDER);		
	}

	public static MongoDBO build(Node callOperationNode) throws XMLConfigException {

		Node dboConfigurationNode = callOperationNode.getFirstChild();

		if (dboConfigurationNode != null && dboSuppliers.containsKey(dboConfigurationNode.getNodeName())) {

			Optional<MongoDBO> dbo = dboSuppliers.get(dboConfigurationNode.getNodeName()).apply(dboConfigurationNode);

			return dbo.orElseThrow(() -> new XMLConfigException("Configuration fail for DBO [" + dboConfigurationNode.getNodeName() + "]"));
		}

		throw new XMLConfigException("No valid DBO found");

	}

}
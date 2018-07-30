package it.greenvulcano.gvesb.virtual.mongodb.dbo;

import it.greenvulcano.configuration.XMLConfigException;
import org.slf4j.Logger;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public class MongoDBOFactory {

	private static final Logger log = org.slf4j.LoggerFactory.getLogger(MongoDBOFactory.class);

	static final Map<String, Function<Node, Optional<MongoDBO>>> dboSuppliers = new LinkedHashMap<>();

	// register the DBO builder methods for each DBO operation
	static {

		dboSuppliers.put(MongoDBOFind.NAME, MongoDBOFind.BUILDER);

		dboSuppliers.put(MongoDBOInsert.NAME, MongoDBOInsert.BUILDER);

	}



	public static MongoDBO build(Node callOperationNode) throws XMLConfigException {

		NodeList children = callOperationNode.getChildNodes();

		Node dboNode = null;

		// pick the first child node matching a DBO operation
		for (int i = 0; i < children.getLength(); i++) {

			Node child = children.item(i);

			if (dboSuppliers.containsKey(child.getNodeName())) { dboNode = child; break; }

		}

		// if a DBO node was found, then build it by picking its specific builder
		if (dboNode != null) {

			String operationName = dboNode.getNodeName();

			Optional<MongoDBO> dbo = dboSuppliers.get(operationName).apply(dboNode);

			return dbo.orElseThrow(() -> new XMLConfigException("Configuration failed for DBO [" + operationName + "]"));

		}

		throw new XMLConfigException("No valid DBO found");

	}

}
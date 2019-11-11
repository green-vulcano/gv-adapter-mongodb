# GreenVulcano VCL Adapter for MongoDB

This is the implementation of a GreenVulcano VCL adapter for the MongoDB database engine. It is meant to be runned as an Apache Karaf bundle.

<!-- TOC depthFrom:1 depthTo:6 withLinks:1 updateOnSave:1 orderedList:0 -->

- [GreenVulcano VCL Adapter for MongoDB](#greenvulcano-vcl-adapter-for-mongodb)
	- [Getting started](#getting-started)
		- [Installation](#installation)
		- [Sample usage](#sample-usage)

<!-- /TOC -->

## Getting started

### Installation

First, you need to have installed Java Development Kit (JDK) 11 or above.

Then, you need to have installed Apache Karaf 4.2.x. Please refer to the following links for further reference: [Apache Karaf](http://karaf.apache.org/manual/latest/)

Next, you need to install the GreenVulcano engine on the Apache Karaf container. Please refer to [this link](https://greenvulcano.github.io/gv-documentation/pages/installation/Installation/#installation) for further reference.

In order to install the bundle in Apache Karaf to use it for a GreenVulcano application project, you need to install its dependencies. Open the Apache Karaf terminal by running the Karaf executable and type the following command:

```shell
karaf@root()> feature:install gvvcl-mongodb
```

### Sample usage

In order to use the features of the MongoDB VCL adapter in your GreenVulcano project, you need to define a proper System-Channel-Operation set of nodes.

**For the rest of the paragraph**, let's assume you want to interact with a MongoDB database called ```documents``` hosted on ```192.168.10.10``` on port ```27017```.

### Declaring the System-Channel-Operation for the MongoDB database

Insert the ```<mongodb-query-call>``` and ```<mongodb-list-collections-call>``` XML nodes in the ```<Systems></Systems>``` section of file ```GVCore.xml```. Here's an example:

```xml
<System id-system="mongodb">
    <Channel id-channel="mongodb_db1" enabled="true" endpoint="xmlp{{db_host_port}}"
        type="MongoDBAdapter">
        <mongodb-query-call type="call" name="query" database="xmlp{{db_name}}" collection="json{{collection}}">
            <query><![CDATA[json{{query}}]]></query>
        </mongodb-query-call>
        <mongodb-list-collections-call type="call" name="list-collections" database="xmlp{{db_name}">
        </mongodb-list-collections-call>
    </Channel>
</System>
```

Some constraints apply to these XML nodes.

- The ```<Channel>``` XML node must comply with the following syntax:
    - ```endpoint``` must contain a URI string correctly referencing the hostname and the port of an operational MongoDB server; refer to [this link](https://docs.mongodb.com/manual/reference/connection-string/) for further reference.
- The ```<mongodb-query-call>``` XML node must comply with the following syntax:
    - ```type``` must be declared and set equal to ```"call"```;
    - ```name``` must be declared: it defines the name of the Operation node;
    - ```database``` must be declared: it defines the name of the MongoDB database to query;
    - ```collection``` must be declared: it defines the name of the MongoDB collection of the specified database to query;
    - the ```<query>``` element must contain the query to perform against the specified database and collection; it must comply the MongoDB query syntax.
- The ```<mongodb-list-collections-call>``` XML node must comply with the following syntax:
    - ```type``` must be declared and set equal to ```"call"```;
    - ```name``` must be declared: it defines the name of the Operation node;
    - ```database``` must be declared: it defines the name of the MongoDB database to query.

Use this operation in your project as needed.  
Whenever you need to deploy a new version of the application on GreenVulcano, you have to make sure the properties of the MongoDB operation nodes are set correctly.



Let's recall the System-Channel-Operation nodes previously defined:

```xml
<System id-system="mongodb">
    <Channel id-channel="mongodb_db1" enabled="true" endpoint="xmlp{{db_host_port}}"
        type="MongoDBAdapter">
        <mongodb-query-call type="call" name="query" database="xmlp{{db_name}}" collection="json{{collection}}">
            <query><![CDATA[json{{query}}]]></query>
        </mongodb-query-call>
        <mongodb-list-collections-call type="call" name="list-collections" database="xmlp{{db_name}">
        </mongodb-list-collections-call>
    </Channel>
</System>
```
In case these operations are used by any Service defined in the application, it's necessary to define the following properties in the GreenVulcano dashboard (Properties section) before running such Services:

- ```db_host_port: mongodb://192.168.10.10:27017```
- ```db_name: documents```

Once the application properties are correctly set, it is possible to run the Services defined in the application deployed on GreenVulcano. In this example, the following Services are defined:

```xml
<Services>
    <Description>This section contains a list of all services provided by
        GreenVulcano ESB</Description>
    <Service group-name="DEFAULT_GROUP" id-service="MONGODB" service-activation="on"
                statistics="off">
        <Operation class="it.greenvulcano.gvesb.core.flow.GVFlowWF" name="List Collections"
                    operation-activation="on" out-check-type="none"
                    type="operation">
            <Flow first-node="list_coll_operation">
                <GVOperationNode class="it.greenvulcano.gvesb.core.flow.GVOperationNode"
                                    id="list_coll_operation" id-channel="mongodb_db1"
                                    id-system="mongodb"
                                    next-node-id="end_step" op-type="call"
                                    operation-name="list-collections"
                                    output="output_query" point-x="254" point-y="150"
                                    type="flow-node"/>
                <GVEndNode class="it.greenvulcano.gvesb.core.flow.GVEndNode"
                            id="end_step" op-type="end" output="output_query"
                            point-x="510" point-y="150" type="flow-node"/>
            </Flow>
        </Operation>
        <Operation class="it.greenvulcano.gvesb.core.flow.GVFlowWF" name="Query"
                    operation-activation="on" out-check-type="none"
                    type="operation">
            <Flow first-node="query_operation">
                <GVOperationNode class="it.greenvulcano.gvesb.core.flow.GVOperationNode"
                                    id="query_operation" id-channel="mongodb_db1"
                                    id-system="mongodb" input="input_query"
                                    next-node-id="end_step" op-type="call"
                                    operation-name="query"
                                    output="output_query" point-x="254" point-y="150"
                                    type="flow-node"/>
                <GVEndNode class="it.greenvulcano.gvesb.core.flow.GVEndNode"
                            id="end_step" op-type="end" output="output_query"
                            point-x="510" point-y="150" type="flow-node"/>
            </Flow>
        </Operation>
    </Service>
</Services>
```

You can test these Services selecting them in the Execute section of the GreenVulcano dashboard:

- **List Collections**: no input Body is needed; after a successful execution, the output window will display the list of the collections defined in the MongoDB database referred by the application properties;
- **Query**: a JSON representing the query to perform against the MongoDB is required; such JSON must have the following structure:

    ```json
    {
        "collection": "<the name of the collection to query - it must be inside the declared database>",
        "query": "<the query to perform against the specified collection - it must comomply with the MongoDB query syntax>"
    }
    ```

**N.B.**: since the MongoDB query has a JSON format, the \" symbols must be escaped (with \\ symbol) in the JSON sent to the GreenVulcano application. Example:

```json
{
    "collection": "measures_1",
    "query": "{\"sensor.physicalId\": { $eq: \"GPS\" } }"
}
```

Notice the escaped \" symbols.

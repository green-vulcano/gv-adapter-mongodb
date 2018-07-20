# GreenVulcano Adapter for MongoDB

This is the implementation of a GreenVulcano VCL adapter for the MongoDB database. It runs as an Apache Karaf bundle.

## Getting started

### Prerequisites

You need to have Maven installed in your computer. Please refer to [this link](https://maven.apache.org/install.html) for further reference.

In order to install the bundle in Apache Karaf to use it for a GreenVulcano application project, you need to install its dependencies. Open the Apache Karaf terminal and type the following command:

```shell
karaf@root()> bundle:install mvn:org.mongodb/mongo-java-driver/3.4.1
```

In case of success, this command will print the ID of the installed bundle:

```shell
Bundle ID: n
```

Before installing the VCL adapter bundle, you have to start the mongo-java-driver bundle by specifying its ID. In the Apache Karaf terminal, type (replacing n with the bundle ID):

```shell
karaf@root()> start n
```

In case you can't find the bundle ID, just type:

```shell
karaf@root()> list | grep mongo-java-driver
```

The bundle ID will be the first field of the row that will appear in the terminal.

Once you started the bundle, use the ```list | grep mongo-java-driver``` command to make sure the bundle is in ```Active``` status.

Then, you need to install the VCL adapter bundle itself in Apache Karaf.

### Installing the VCL adapter bundle in Apache Karaf

Clone or download this repository on your computer, and then run ```mvn install``` in its root folder:

```shell
git clone https://github.com/green-vulcano/gv-adapter-mongodb
cd gv-adapter-mongodb
mvn install
```

In case of success, the ```mvn install``` command will install the VCL adapter bundle in the local Maven repository folder.  
After this operation, you have to add the Maven repository project as an Apache Karaf bundle, telling Karaf to load it after the GreenVulcano core bundles, since the VCL adapter requires the GreenVulcano bundles in order to start correctly.  
This constraint can be enforced by properly configuring the *levels* of the Karaf bundles: the lower the level number, the earlier the bundle will be loaded by Karaf.

For the VCL adapter bundler, we will use a bundle level higher than the GreenVulcano core bundles (i.e. ```80```). The following command will install the VCL adapter bundle and set its level to ```96``` by convention, using the ```-l``` attribute:

```shell
karaf@root()> bundle:install -l 96 mvn:it.greenvulcano.gvesb.adapter/gvvcl-mongo/1.0.0-SNAPSHOT

Bundle ID: x
```

Make sure that the bundle ```GreenVulcano ESB VCL interface for MongoDB``` appears in the ```list``` of installed bundles in ```Installed``` status and with bundle level (```Lvl```) equal to ```96``` (or at least higher than ```80```). Then, use its ID to put the bundle in ```Active``` status by executing the following command:

```shell
karaf@root()> start x

list | grep GreenVulcano ESB VCL interface for MongoDB
```

## Using the VCL adapter in your GreenVulcano project

In order to use the features of the MongoDB VCL adapter in your GreenVulcano project, you need to define a System-Channel-Operation set of nodes.  
**For the rest of the paragraph**, let's assume you want to interact with a MongoDB database called ```documents``` hosted on ```192.168.10.10``` on port ```27017```.

### Declaring the System-Channel-Operation for the MongoDB database

Insert the following XML node in the ```<Systems></Systems>``` section:

```xml
<System id-system="mongodb">
    <Channel id-channel="mongodb_db1" enabled="true" endpoint="xmlp{{db_host_port}}"
        type="MongoDBAdapter">
        <mongodb-query-call type="call" name="query" database="xmlp{{db_name}}" collection="json{{collection}}">
            <query><![CDATA[json{{query}}]]></query>
        </mongodb-query-call>
    </Channel>
</System>
```

Use this operation in your project as needed.  
Whenever you need to deploy a new version of the application on GreenVulcano, you have to make sure the following application properties are set (**replace their values with your database connection settings**):

```
db_host_port: mongodb://192.168.10.10:27017
db_name: documents
```

## TODO complete this section: ensure the VCL adapter consumes a JSON string


## TODO show this sample query
```json
{
    "collection": "measures_1",
    "query": "{\"sensor.physicalId\": { $eq: \"GPS\" } }"
}
```

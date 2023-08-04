# VirtualOPCUA2SPQARL (working project title)

**Caution: This is just a proof of concept implementation**

More Information about the integration procedure can be found in the article [_"Ontology-Based OPC UA Data Access via Custom Property Functions"_](https://ieeexplore.ieee.org/document/8869436)

There are basically two steps:  
1) to read the OPC UA information model from a server and transform it into OWL
2) to load the owl-file into Fuseki for accessing the runtime data via custom property functions

## OPC UA Informtation model to OWL

The relevant files for the automatic transformation from the OPC UA information model into an OWL ontology can be found in [this folder](OPC2OWL%20Transformer)

## Run-time data access via custom property functions
The implementation of the custom property functions can be found [here](SPARQL%20ARQ%20Extension).

The generated jar-files have to be included for the start-up of the Fuseki server. This is already done in the start-script for [windows](Fuseki%20Testserver/fuseki-server_Start_with_opc2sparql.bat). The [jar file](Fuseki%20Testserver/virtualOPCEndpoint-0.0.1-SNAPSHOT.jar) has to be in the same folder as the start-up script. 

Additionally, the class files for the custom property functions have to be added to the [config.ttl file](Fuseki%20Testserver/apache-jena-fuseki-3.10.0/run/config.ttl)

## Further Readings
The approach can be extended by transforming parts of the OPC OWL ontology into a domain specific one. More information can be found in the article [_"Transforming OPC UA Information Models into Domain-Specific Ontologies"_](https://ieeexplore.ieee.org/document/9468254)


## Contact
Gernot Steindl   
gernot.steindl@tuwien.ac.at  
Institute of Computer Engineering  
Research Unit Automation Systems  
TU Vienna 


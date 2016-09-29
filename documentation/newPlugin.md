# How to develop a new plugin

## Manifest file

In the manifest file of the jar file some information are required
to initialize the plugin and load required classes.
The entries can be added to the manifest file using the ```maven-jar-plugin```
for maven.

### General
**plugin-name** (required)  
The (short) name of the plugin. The name should be reasonable short,
unique and describes the plugin. The name is used in the URL to access 
the ontology functions and as key to to register the components of the plugin
in the OBA server.
 
### Ontology function
**function-path-name** (deprecated, use name instead)

the name under which the class with the semantic
function of the plugin is registred and which is used
in the URL pattern to access these functions.
  
**function-main-class**

The class which implements the REST functions of the plugin
The complete name including the package (without file extension) has to
be specified

**load_by_plugin**
A class to load an ontology. The class should implement the
interface ```de.sybig.oba.server.pluginManagment.OntologyLoader```.
If an ontology specified the name of a plugin in its property
file under the key ```load_by_plugin```. This class is used
to process the property file of the ontology and should return
an ObaOntology.


    
### Marshallers

**provider-classes**  
A list of classes with ```Providers``` for jersey, limited by a colon ":". The complete name including the package (without file extension) has to
be specified. The specified classes are initialized and registered to the resource config of Jersey before the HTTP server is started.

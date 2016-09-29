# Loading ontologies

## Loading ontologies from the filesystem of the host

## Ontology directory

On startup the server scans the ontology directory for
ontologies to load. By default this directory is
```/srv/onotlogies``` but can be changed in the property
file for the OBA service with the key ```ontology_directory```

### Property file

Each ontology to be loaded by the OBA service has to be
described in a property file. The keys of the file are
described below.


**identifier** (required)

Each ontology needs a unique identifier. If the identifier
is missing the ontology is not loaded. It is not checked
if the identifier is unique.

**description**

ToDo


**file**

ToDo

**indexAnnotations**

ToDo


**load_lazy**

ToDo  
```true``` or ```false```


**depends_on** (optional)

If the ontology should be loaded after one or more other ontologies,
the identifier of these ontologies has to be specified as value of
the key ```depends_on```. Several ontologies should be seperated by
";". 

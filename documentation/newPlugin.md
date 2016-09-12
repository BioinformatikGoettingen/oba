# How to develop a new plugin

## Manifest file

In the manifest file of the jar file some information are required
to initialize the plugin and load required classes.
The entries can be added to the manifest file using the ```maven-jar-plugin```
for maven.


  ;  function-path-name
  :  the name under which the class with the semantic
    function of the plugin is registred and which is used
    in the URL pattern to access these functions.
  ; function-main-class
  : The class which implements the REST functions of the plugin
    The complete name including the package (without file extension) has to
    be specified
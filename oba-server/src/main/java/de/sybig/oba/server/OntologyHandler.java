/*
 * Created on Mar 23, 2010
 *
 */
package de.sybig.oba.server;

import de.sybig.oba.server.pluginManagment.OntologyLoader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The OntologyHandler to administer all loaded ontologies and the plugin
 * classes for additional functions.
 *
 * @author juergen.doenitz@bioinf.med.uni-goettingen.de
 *
 */
public final class OntologyHandler {

    private static volatile OntologyHandler instance = null;
    private Properties generalProps;

    private final Map<String, OntologyResource> ontologyMap = new HashMap<>();
    private final Map<String, OntologyResource> preparedOntologyMap = new HashMap<>();
    private final Map<String, Properties> ontologyPropertiesMap = new HashMap<>();
    private final Map<String, OntologyFunction> functionMap = new HashMap<>();
    private final Map<String, OntologyLoader> ontologyLoaderMap = new HashMap<>();
    private final Logger logger = LoggerFactory.getLogger(OntologyHandler.class);

    /**
     * The OntologyHandler is a singleton class, so no public constructor is
     * available. To get the instance of the OntologyHandler use {
     *
     * @#getInstance()}.
     */
    private OntologyHandler() {
        // no public constructor
    }

    /**
     * Get the only instance of the OntologyHandler.
     *
     * @return the singleton instance
     */
    public static OntologyHandler getInstance() {
        if (instance == null) {
            instance = new OntologyHandler();
        }
        return instance;
    }

    /**
     * Sets the general properties for the server. This properties should
     * contain a directory where ontologies and property files to load them can
     * be found. If in the property file of an ontology the path to the ontology
     * is not absolute the ontology directory is retrieved from the global
     * properties.
     *
     * @param props The properties of the server.
     */
    public void setGeneralProperties(final Properties props) {
        this.generalProps = props;

    }

    /**
     * Get the ontology registered with the given name. If no ontology was
     * registered under this name <code>null</code> is returned.
     *
     * @param name the name under which the ontology was registered
     * {@link #addOntology(String, OntologyResource)}
     * @return the ontology or <code>null</code>
     */
    public OntologyResource getOntology(final String name) {
        if (ontologyMap == null) {
            return null;
        }
        if (!ontologyMap.containsKey(name)
                && preparedOntologyMap.containsKey(name)) {
            OntologyResource onto = preparedOntologyMap.get(name);
            try {
                onto.getOntology().init();
            } catch (OWLOntologyCreationException e) {
                logger.error("could not load ontology {}, due to {}", name,
                        e.getMessage());
            }
            ontologyMap.put(name, onto);
            preparedOntologyMap.remove(name);
        }
        return ontologyMap.get(name);
    }

    public String getNameOfOntology(OWLOntology ontology) {
        // if (ontologyMap == null) {
        // return null;
        // }
        for (String name : ontologyMap.keySet()) {
            if (ontologyMap.get(name).getOntology().getOntology()
                    .equals(ontology)) {
                return name;
            }
        }
        return null;
    }

    public OntologyFunction getFunctionClass(String name) {
        if (functionMap == null) {
            return null;
        }
        return functionMap.get(name);
    }

    /**
     * Checks if an ontology was registered under this name. If an ontology was
     * registered but not yet loaded the name is also found.
     *
     * @param name The name of the ontology
     * @return <code>true</code> if an ontology was registered under this name.
     */
    public boolean containsOntology(String name) {
        // if (ontologyMap == null && preparedOntologyMap == null) {
        // return false;
        // }

        return ontologyMap.containsKey(name)
                || preparedOntologyMap.containsKey(name);
    }

    public boolean containsFunctionClass(String name) {
        if (functionMap == null) {
            return false;
        }
        return functionMap.containsKey(name);
    }

    /**
     * Adds an ontology to the ontologies available by the server. In the
     * 'ontology directory', specified in the general properties the property
     * file for the ontology is searched. The name of the property file should
     * be 'name.properties' where name is the string given as parameter.
     * According to the properties of the ontology, the ontology is directly
     * loaded or only added to the prepared ontologies and loaded on the first
     * access. The properties are added to the map
     * <code>ontologyPropertiesMap</code> to enable other methods to access
     * values of the property file.
     *
     * @param name The name of the property file (without the appendage
     * ".properties")
     */
    public void addOntology(String name) {
        if (containsOntology(name)) {
            logger.info("did not load the ontology {}, because it was already loaded");
            return;
        }
        String directory = generalProps.getProperty("ontology_directory");
        File propFile = new File(directory, name + ".properties");
        if (!propFile.exists() || !propFile.canRead()) {
            logger.error(
                    "could not load the ontology {}, because the property file {} can not be read",
                    name, propFile);
            return;
        }
        Properties p = new Properties();
        FileInputStream is = null;
        try {
            is = new FileInputStream(propFile);
            p.load(is);
            addOntology(p);
        } catch (FileNotFoundException e) {
            logger.error("could not load the ontology {}, because: {}", name,
                    e.getMessage());
        } catch (IOException e) {
            logger.error("could not load the ontology {}, because: {}", name,
                    e.getMessage());
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    //
                }
            }
        }
    }

    /**
     * Adds an ontoloyg to the map of available ontologies. The method is only
     * used for testing, for other cases uses
     * {@link #addOntology(java.util.Properties)} which stores also the
     * properties for an ontology.
     *
     * @param name The name of the ontolgy
     * @param or The ontology resource to register.
     */
    public void addOntology(String name, OntologyResource or) {
        ontologyMap.put(name, or);
    }

    /**
     * Adds a single ontology defined in its own property file.
     *
     * @param p The properties for the ontology.
     */
    public void addOntology(Properties p) {
        String name = p.getProperty("identifier");
        ObaOntology po = null;
        if (!p.containsKey("load_by_plugin")) {
            po = buildOntologyWrapper(p);
        } else {
            OntologyLoader ontologyLoader = getOntologyLoader(p.getProperty("load_by_plugin"));
            if (ontologyLoader == null) {
                logger.warn("The plugin {} to load the ontology does not provide an ontology loader", p.getProperty("load_by_plugin"));
                return;
            }
            po = ontologyLoader.loadOntology(p);
        }
        po.setProperties(p);
        OntologyResource onto = new OntologyResource();
        onto.setOntology(po);
        if (p.getProperty("load_lazy", "false").equalsIgnoreCase("true")) {
            preparedOntologyMap.put(name, onto);
        } else {
            try {
                po.init();
                ontologyMap.put(name, onto);
            } catch (OWLOntologyCreationException e) {
                logger.error("could not load ontology {}, due to {}",
                        name, e.getMessage());
                return;
            }
        }
        ontologyPropertiesMap.put(name, p);
    }

    private ObaOntology buildOntologyWrapper(Properties p) {
        File ontoFile = new File(p.getProperty("file").trim());

        if (!ontoFile.isAbsolute()) {
            ontoFile = new File(generalProps.getProperty("ontology_directory",
                    System.getProperty("java.io.tmpdir")),
                    p.getProperty("file"));
        }
        if (!ontoFile.canRead()) {
            logger.error(
                    "could not load the ontology {}, will skip the ontology",
                    ontoFile);
        }
        String name = p.getProperty("identifier");
        ObaOntology po = new ObaOntology();
        po.setOwlURI(IRI.create(ontoFile));
        return po;
    }

    public Properties getOntologyProperties(String name) {
        return ontologyPropertiesMap.get(name);
    }

    public void deleteOntology(ObaOntology ontology) {
        for (String name : ontologyMap.keySet()) {
            if (ontology.equals(ontologyMap.get(name).getOntology())) {
                ontologyMap.remove(name);
                logger.info("removing ontology '{}'", ontology);
                break;
            }
        }
    }

    /**
     * Removes the given ontology from the map of perpared or loaded ontologies.
     * As long as the ontology is referenced some where else, e.g. by plugins,
     * the ontology is not removed from the memory. The plugins should remove
     * references to its ontoloy on reset.
     *
     * @param ontology The name of the ontology to remove.
     */
    public void deleteOntology(String ontology) {
        for (String name : preparedOntologyMap.keySet()) {
            if (ontology.equals(name)) {
                preparedOntologyMap.remove(name);
                break;
            }
        }
        for (String name : ontologyMap.keySet()) {
            if (ontology.equals(name)) {
                ontologyMap.remove(name);
                break;
            }
        }
    }

    /**
     * Instantiate the named class and register it under the given name. For
     * loading the class loader of the application is used, so classes from
     * plugins can not be loaded using this method. For plugins use the method
     * {#see #addFunctionClass(String, OntologyFunction)}
     *
     * @param name The name under which the class is registered
     * @param className The class to load
     * @throws ClassNotFoundException
     * @throws InstantiationException
     * @throws IllegalAccessException
     */
    public void addFunctionClass(String name, String className)
            throws ClassNotFoundException, InstantiationException,
            IllegalAccessException {

        Class<? extends OntologyFunction> functionClass = (Class<? extends OntologyFunction>) Class
                .forName(className);
        OntologyFunction classInstance = functionClass.newInstance();
        addFunctionClass(name, classInstance);
    }

    /**
     * Register an instance of an OntologyFunction under the given name. The
     * class has to be already instanciated, so that a class loader for the
     * plugins can be used. To load classes from the classpath of the
     * application see also {#see #addFunctionClass(String, String)}
     *
     * @param name The name under which the class is registered
     * @param classInstance The class with the ontology functions
     */
    public void addFunctionClass(String name, OntologyFunction classInstance) {
        functionMap.put(name, classInstance);
    }

    public void addOntologyLoader(String name, OntologyLoader instance) {
        ontologyLoaderMap.put(name, instance);
    }

    public OntologyLoader getOntologyLoader(String name) {
        return ontologyLoaderMap.get(name);
    }

    /**
     * Get the names of available loaded ontologies. This includes loaded and
     * known but not loaded ontologies. The name of an ontology is the name the
     * ontology is registered with and defined in the property file of the
     * ontology.
     *
     * @return A list of names of known ontologies.
     */
    public Set<String> getOntologyNames() {
        HashSet<String> out = new HashSet<String>();
        out.addAll(ontologyMap.keySet());
        out.addAll(preparedOntologyMap.keySet());
        return out;
    }

    /**
     * Returns the description of the ontology as stored in the property file of
     * the ontology. If the given name does not match the name of any loaded
     * ontology <code>null</code> is returned. If no description is stored in
     * the property file an empty string is returned.
     *
     * @param name the identifier of the ontology
     * @return The description of the ontology.
     */
    public String getDescriptionForOntology(String name) {
        Properties p = ontologyPropertiesMap.get(name);
        if (p == null) {
            return null;
        }
        return p.getProperty("description", "");
    }

    public Set<String> getFunctionNames() {
        return functionMap.keySet();
    }

    public void resetFunctionClass(String functionName) {
        for (String name : getFunctionNames()) {
            if (name.equals(functionName)) {
                OntologyFunction function = functionMap.get(name);
                function.reset();
                return;
            }
        }
        throw new IllegalArgumentException("unknown function class");
    }
    // public OWLOntologyWalker getWalker() {
    // return walker;
    // }

    public OntologyResource getOntologyResource(OWLClass c) {
        for (String key : ontologyMap.keySet()) {
            OntologyResource or = ontologyMap.get(key);
            org.semanticweb.owlapi.model.OWLOntology onto = or.getOntology()
                    .getOntologyForClass(c);
            if (onto != null) {
                return or;
            }
        }
        logger.error("could not get the ontology ressource for the class {}", c);
        return null;
    }

    public OntologyResource getOntologyResource(OWLOntology ontology) {
        for (String key : ontologyMap.keySet()) {
            OntologyResource or = ontologyMap.get(key);
            if (or.getOntology().getOntology().equals(ontology)) {
                return or;
            }
        }
        logger.error(
                "could not get the ontology ressource for the ontology {}",
                ontology);
        return null;
    }

    public OWLOntology getOntologyForClass(OWLClass c) {

        for (String key : ontologyMap.keySet()) {
            OntologyResource or = ontologyMap.get(key);
            OWLOntology onto = or.getOntology().getOntologyForClass(c);
            if (onto != null || c.isOWLThing()) {
                // owl:thing does not have to be defined in the owl file
                return onto;
            }
        }
        logger.error(
                "could not get ontology for class '{}'. Loaded ontologies: {}",
                c, ontologyMap.keySet());
        return null;
    }

    /**
     * Gets a ontology class with the given name in the given namespace from any
     * loaded ontology. Ontologies that are registered but not loaded yet (lazy
     * loading) are not considered. If no matching class is found,
     * <code>null</code> is returned.
     *
     * @param name The name of the ontology class.
     * @param ns The namespace of the class.
     * @return The ontology class or <code>null</code>
     */
    public ObaClass getClass(String name, String ns) {
        for (String key : ontologyMap.keySet()) {
            OntologyResource or = ontologyMap.get(key);
            ObaClass c = or.getOntology().getOntologyClass(name, ns);
            if (c != null) {
                return c;
            }
        }
        return null;
    }

    public OWLOntology getOntologyForProperty(OWLObjectProperty c) {
        for (String key : ontologyMap.keySet()) {
            OntologyResource or = ontologyMap.get(key);
            OWLOntology onto = or.getOntology().getOntologyForProperty(c);
            if (onto != null) {
                // owl:thing does not have to be defined in the owl file
                return onto;
            }
        }
        logger.error(
                "could not get ontology for object property '{}'. Loaded ontologies: {}",
                c, ontologyMap.keySet());
        return null;
    }

    /**
     * Get the root of the ontology. The root class is returned as proxy
     * {@link ObaClass} with the ontology set.
     *
     * @param ontology
     * @return The root of the ontology
     */
    public ObaClass getRootOfOntology(OWLOntology ontology) {
        OWLClass root = ontology.getOWLOntologyManager().getOWLDataFactory()
                .getOWLThing();

        return new ObaClass(root, ontology);

    }

}

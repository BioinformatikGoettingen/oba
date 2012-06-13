/*
 * Created on Mar 23, 2010
 *
 */
package de.sybig.oba.server;

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

	private Map<String, OntologyResource> ontologyMap = new HashMap<String, OntologyResource>();
	private Map<String, OntologyResource> preparedOntologyMap = new HashMap<String, OntologyResource>();
	private Map<String, Properties> ontologyPropertiesMap = new HashMap<String, Properties>();
	private Map<String, OntologyFunction> functionMap;

	private Logger logger = LoggerFactory.getLogger(OntologyHandler.class);

	/**
	 * The OntologyHandler is a singleton class, so no public constructor is
	 * available. To get the instance of the OntologyHandler use
	 * {@#getInstance()}.
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
	 * @param props
	 *            The properties of the server.
	 */
	public void setGeneralProperties(final Properties props) {
		this.generalProps = props;

	}

	/**
	 * Get the ontology registered with the given name. If no ontology was
	 * registered under this name <code>null</code> is returned.
	 * 
	 * @param name
	 *            the name under which the ontology was registered
	 *            {@link #addOntology(String, OntologyResource)}
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
	 * @param name
	 *            The name of the ontology
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
	 * @param name
	 *            The name of the property file (without the appendage
	 *            ".properties")
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
	 * Adds a single ontology defined in its own property file
	 * 
	 * @param p
	 */
	public void addOntology(Properties p) {
		File ontoFile = new File(p.getProperty("file"));

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
		String name = p.getProperty("identifier",
				ontoFile.getName().split("\\.")[0]);
		ObaOntology po = new ObaOntology();
		po.setOwlURI(IRI.create(ontoFile));
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
						ontoFile.getName(), e.getMessage());
				return;
			}
		}
		ontologyPropertiesMap.put(name, p);
	}

        public Properties getOntologyProperties(String name){
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

	public void addFunctionClass(String name, String className)
			throws ClassNotFoundException, InstantiationException,
			IllegalAccessException {

		Class<? extends OntologyFunction> functionClass = (Class<? extends OntologyFunction>) Class
				.forName(className);
		OntologyFunction classInstance = functionClass.newInstance();
		addFunctionClass(name, classInstance);
	}
    public void addFunctionClass(String name, OntologyFunction classInstance){
        if (functionMap == null) {
            functionMap = new HashMap<String, OntologyFunction>();
        }
        functionMap.put(name, classInstance);
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
	 * @param name
	 *            the identifier of the ontology
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

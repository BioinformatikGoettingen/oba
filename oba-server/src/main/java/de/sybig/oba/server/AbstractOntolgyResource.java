package de.sybig.oba.server;

import java.io.IOException;
import java.util.Properties;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.PathSegment;

import org.semanticweb.owlapi.model.OWLClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractOntolgyResource {

    private final static Logger log = LoggerFactory.getLogger(AbstractOntolgyResource.class);
    protected ObaOntology ontology;
    protected Properties properties;

    /**
     * Sets the ontology used by the functions implemented by the subclasses.
     *
     * @param ontology
     */
    public void setOntology(ObaOntology ontology) {
        this.ontology = ontology;
    }

    /**
     * Get the ontology the ontology functions work on.
     *
     * @return The ontology
     */
    public ObaOntology getOntology() {
        return ontology;
    }

    protected ObaClass getClassFromPathSegement(PathSegment pathSegment) {
        return getClassFromPathSegement(pathSegment, null);
    }

    /**
     * Gets a class from the ontology as specified in the path segment.
     * Therefore the name of the class it optional namespace is extracted from
     * the path segment. The namespace can be specified by the matrix parameter
     * 'ns', if the value of the namespace contains '/' it should be replaced by
     * '$'.
     *
     * If the path segment does not specify a valid ontology class, a web
     * application exception 404 is thrown.
     *
     * @param pathSegment The path segment to parse if no query parameter is
     * used.
     * @param queryParameter The path parameter of the URL, if available.
     * @return The owl class or <code>null</code>
     */
    protected ObaClass getClassFromPathSegement(PathSegment pathSegment,
            String queryParameter) {
        String nsX = queryParameter;
        if (queryParameter == null
                && pathSegment.getMatrixParameters().get("ns") != null) {
            nsX = pathSegment.getMatrixParameters().get("ns").get(0);
            nsX = nsX.replace("$", "/"); // TODO / = %2f but breaks jersey
            // pattern matching
        }
        OWLClass clsX = ontology.getOntologyClass(pathSegment.getPath(), nsX);
        if (clsX == null) {
            throw new WebApplicationException(404);
        }
        return new ObaClass(clsX, ontology.getOntology());
    }

    protected void setProperties(Properties p) {
        properties = p;
    }

    protected Properties getProperties() {
        return properties;
    }

    public void loadPropertiesFromJar(String name) {

        properties = new Properties();
        try {
            properties.load(this.getClass().getResourceAsStream(
                    name));
        } catch (IOException e) {
            log.error("could not load properties from file {} for function class {}",
                    name, this.getClass());
        }
    }
}

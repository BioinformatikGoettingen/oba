package de.sybig.oba.server;

import java.io.IOException;
import java.util.Properties;
import javax.validation.constraints.Null;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.PathSegment;

import org.semanticweb.owlapi.model.OWLClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract class with some service methods for depending classes. All classes
 * implementing ontology functions should inherit from this class.
 *
 * @author juergen.doenitz@bioinf.med.uni-goettingen.de
 */
public abstract class AbstractOntolgyResource {

    private static final Logger log  = LoggerFactory.getLogger(AbstractOntolgyResource.class);
    protected ObaOntology ontology;
    protected Properties properties;

    /**
     * Sets the ontology used by the functions implemented by the subclasses.
     *
     * @param ontology The ontology to be used by the function class.
     */
    public void setOntology(ObaOntology ontology) {
        this.ontology = ontology;
    }

    /**
     * Get the ontology the ontology functions work on.
     *
     * @return The ontology The ontology to be used by the function class
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
     * <p>If the path segment does not specify a valid ontology class, a web
     * application exception 404 is thrown.</p>
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

    /**
     * Sets the properties for the function class. The properties can be also
     * loaded by using {@link #loadPropertiesFromJar(java.lang.String) }
     *
     * @param p The properties to set.
     */
    protected void setProperties(Properties p) {
        properties = p;
    }

    /**
     * Get the properties for the function class. The properties should have
     * been set previously by {@link #setProperties(java.util.Properties)} or by
     * {@link #loadPropertiesFromJar(java.lang.String) }.
     *
     * @return The properties of the function class, or <code>null</code>
     */
    @Null
    protected Properties getProperties() {
        return properties;
    }

    /**
     * Loads a property file for the function class from the source / jar file.
     *
     * @param name The path to the jar file starting from the class directory,
     * starting with '/'
     */
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

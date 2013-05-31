/*
 * Created on Mar 22, 2010
 *
 */
package de.sybig.oba.server;

import java.util.Set;
import javax.ws.rs.*;
import javax.ws.rs.core.PathSegment;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An ontology resource class handles the request for one ontology
 *
 * @author jdo@bioinf.med.uni-goettingen.de
 */
public class OntologyResource extends AbstractOntolgyResource {

    private Logger logger = LoggerFactory.getLogger(OntologyResource.class);

    @GET
    @Produces("application/json, text/plain, text/html")
    @Path("cls")
    public OWLClass getClsRoot() {
//        logger.debug("getting root node for ontology: {} by ressource {}",
//                ontology, this);
        OWLClass c = ontology.getRoot();
        return c;

    }

    @GET
    @Path("cls/{cls}/children")
    @Produces("application/json, text/plain, text/html")
    public Set<ObaClass> getChildren(@PathParam("cls") PathSegment cls) {
        System.err.println("gettingen children for " + cls);
        // String ns = cls.getMatrixParameters().get("ns").get(0);
        OWLClass c = getClassFromPathSegement(cls);
        return OntologyHelper.getChildren(c, ontology.getOntology());
    }

    @GET
    @Path("cls/{cls}/parents")
    @Produces("text/plain, application/json, text/html")
    public Set<ObaClass> getParents(@PathParam("cls") PathSegment cls) {
        OWLClass c = getClassFromPathSegement(cls, null);
        return OntologyHelper.getParents(c, ontology.getOntology());
    }

    @GET
    @Path("cls/{cls}")
    @Produces("application/json, text/plain, text/html")
    public OWLClass getCls(@PathParam("cls") String cls,
                           @QueryParam("ns") String ns) {
        logger.trace("getting ontology class '{}' from ontology '{}'" + cls,
                ontology);
        OWLClass c = ontology.getOntologyClass(cls, ns);
        if (c == null) {
            logger.warn(
                    "the requested class {} in namespace {} is not available",
                    cls, ns);
            throw new WebApplicationException(404);
        }
        ObaClass proxy = new ObaClass(c, ontology.getOntology());
        return proxy;
    }

    /**
     * Gets a set of all object properties (annotations) defined in the ontology.
     *
     * @return All annotations of the ontology.
     */
    @GET
    @Produces("text/plain,text/html, application/json")
    @Path("objectProperty")
    public Object getRestrictions() {
        return ontology.getObjectProperties();

    }

    @GET
    @Path("objectProperty/{property}")
    @Produces("text/plain, application/json, text/html")
    public OWLObjectProperty getRestriction(@PathParam("property") String cls,
                                            @QueryParam("ns") String ns) {
        String namespace = ns != null ? ns.replace("$", "/") : null;
        logger.debug("get property '{}' in namespace '{}'", cls, namespace);
        OWLObjectProperty prop = ontology.getPropertyByName(cls, namespace);
        return prop;
    }
}

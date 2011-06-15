/*
 * Created on Mar 22, 2010
 *
 */
package de.sybig.oba.server;

import java.util.Set;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.PathSegment;

import org.semanticweb.owlapi.model.OWLClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An ontology resource class handles the request for one ontology
 * 
 * @author jdo@bioinf.med.uni-goettingen.de
 * 
 */
public class OntologyResource extends AbstractOntolgyResource {

	private Logger logger = LoggerFactory.getLogger(OntologyResource.class);

	@GET
	@Produces("text/plain, application/json, text/html")
	@Path("cls")
	public OWLClass getClsRoot() {
		logger.debug("getting root node for ontology: {} by ressource {}",
				ontology, this);
		OWLClass c = ontology.getRoot();
		ObaClass proxy = new ObaClass(c, ontology.getOntology());
		return proxy;

	}

	@GET
	@Path("cls/{cls}")
	@Produces("text/plain, application/json, text/html")
	public OWLClass getCls(@PathParam("cls") String cls,
			@QueryParam("ns") String ns) {
		logger.debug("getting ontology class '{}' from ontology '{}'" + cls,
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

	@GET
	@Path("cls/{cls}/children")
	@Produces("text/plain, application/json, text/html")
	public Set<ObaClass> getChildren(@PathParam("cls") PathSegment cls) {
		OWLClass c = getClassFromPathSegement(cls, null);
		return OntologyHelper.getChildren(c, ontology.getOntology());
	}

	@GET
	@Path("cls/{cls}/paretns")
	@Produces("text/plain, application/json, text/html")
	public Set<ObaClass> getParents(@PathParam("cls") PathSegment cls) {
		OWLClass c = getClassFromPathSegement(cls, null);
		return OntologyHelper.getParents(c, ontology.getOntology());
	}

	@GET
	@Produces("text/plain,text/html, application/json")
	@Path("objectProperty")
	// @HtmlBase("objectProperty/")
	public Object getRestrictionRoot() {
		return ontology.getObjectProperties();

	}

	@GET
	@Path("objectProperty/{property}")
	@Produces("text/plain, application/json, text/html")
	public Object getRestriction(@PathParam("property") String cls,
			@QueryParam("ns") String ns) {
		String namespace = ns != null ? ns.replace("$", "/") : null;
		logger.debug("get property '{}' in namespace '{}'", cls, namespace);
		Object prop = ontology.getPropertyByName(cls, namespace);
		return prop;
	}

}

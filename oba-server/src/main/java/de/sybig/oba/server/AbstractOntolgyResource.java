package de.sybig.oba.server;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.PathSegment;

import org.semanticweb.owlapi.model.OWLClass;

public abstract class AbstractOntolgyResource {
	protected ObaOntology ontology;

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
	 * @param pathSegment
	 *            The path segment to parse
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

}

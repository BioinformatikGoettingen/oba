/*
 * Created on Apr 15, 2010
 *
 */
package de.sybig.oba.server;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.Produces;
import javax.ws.rs.ext.Provider;

import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import org.semanticweb.owlapi.model.OWLOntology;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Provider
@Produces("text/plain")
public class TextMarshaller extends OntoMarshaller {
	private Logger logger = LoggerFactory.getLogger(OntoMarshaller.class);

	@Override
	protected String listCls(Collection<ObaClass> cls, Annotation[] annotations) {
		StringBuffer out = new StringBuffer();
		for (OWLClass c : cls) {
			out.append(c.getIRI() + "\n");
		}
		return out.toString();
	}

	@Override
	protected String listProperties(Collection<OWLObjectProperty> list,
			Annotation[] arg3) {
		StringBuffer out = new StringBuffer();
		for (OWLObjectProperty p : list) {
			out.append(p.getIRI());
			out.append("\n");
		}
		return out.toString();
	}

	@Override
	protected String listList(Collection<Collection> list, Annotation[] arg3) {
		StringBuffer out = new StringBuffer();
		for (Collection<OWLClass> subList : list) {
			for (OWLClass c : subList) {
				out.append(c.getIRI() + "\t");
			}
			out.append("\n");
		}
		return out.toString();
	}

	@Override
	protected String listMap(Map<ObaClass, Collection> map,
			Annotation[] annotations) {
		StringBuffer out = new StringBuffer();
		for (OWLClass cls : map.keySet()) {
			out.append(cls.getIRI());
			Collection<OWLClass> subList = map.get(cls);
			for (OWLClass c : subList) {
				out.append(c.getIRI() + "\t");
			}
			out.append("\n");
		}
		return out.toString();
	}

	protected String convertCls(ObaClass cls, Annotation[] annotations) {

		OWLOntology ontology = getOntology(cls);
		String namespace = cls.getIRI().getStart();
		logger.info("converting class '{}' from ontology '{}' to text ", cls,
				ontology);

		StringBuffer out = new StringBuffer();
		out.append("name\t" + cls.getIRI().getFragment() + "\n");
		out.append("namespace\t" + namespace + "\n");

		// Parents
		Collection<ObaClass> parents = getParents(cls, ontology);
		for (OWLClass p : parents) {
			out.append(String.format("parent\t %s \n", namespace.equals(p
					.getIRI().getStart()) ? p.getIRI().getFragment() : p
					.getIRI().getFragment()));
		}
		// Children
		Set<ObaClass> children = getChildren(cls, ontology);
		for (OWLClass child : children) {
			out.append(String.format("child\t %s \n", namespace.equals(child
					.getIRI().getStart()) ? child.getIRI().getFragment()
					: child.getIRI().getFragment()));
		}

		for (ObaAnnotation property : getAnnotationProperties(cls, ontology)) {
			out.append(String.format("%s [%s]\t%s\n", property.getName(),
					property.getLanguage(), property.getValue()));
		}
		Set<ObaObjectPropertyExpression> properties = getObjectRestrictions(
				cls, ontology);
		for (ObaObjectPropertyExpression p : properties) {
			// TODO add namespace if it is different from the class

			out.append(String.format("%s\t%s\n", p.getRestriction().getIRI()
					.getFragment().toString(), namespace.equals(p.getTarget()
					.getIRI().getStart()) ? p.getTarget().getIRI()
					.getFragment() : p.getTarget().getIRI()));
		}
		return out.toString();
	}

	@Override
	protected String convertProperty(OWLObjectProperty r, Annotation[] arg3) {
		OWLOntology ontology = getOntology(r);
		String namespace = r.getIRI().getStart();
		StringBuffer out = new StringBuffer();

		out.append(String.format("Name\t%s\n", r.getIRI().getFragment()));
		out.append(String.format("Namespace\t%s\n", namespace));
		Set<OWLObjectProperty> parentRestrictions = OntologyHelper
				.getParentRroperties(r, ontology);
		for (OWLObjectProperty p : parentRestrictions) {
			out.append(String.format("Super property\t %s \n", namespace
					.equals(p.getIRI().getStart()) ? p.getIRI().getFragment()
					: p.getIRI().getFragment()));
		}
		Set<OWLObjectProperty> childRestrictions = OntologyHelper
				.getChildRroperties(r, ontology);
		for (OWLObjectProperty p : childRestrictions) {
			out.append(String.format("Sub property\t %s \n", namespace.equals(p
					.getIRI().getStart()) ? p.getIRI().getFragment() : p
					.getIRI().getFragment()));
		}

		Set<OWLObjectPropertyExpression> inverseSet = r.getInverses(ontology);
		if (inverseSet.size() > 0) {
			OWLObjectProperty p = inverseSet.iterator().next()
					.getObjectPropertiesInSignature().iterator().next();
			out.append(String.format("Inverse property\t%s\n", namespace
					.equals(p.getIRI().getStart()) ? p.getIRI().getFragment()
					: p.getIRI().getFragment()));
		}

		return out.toString();
	}

}
/*
 * Created on Apr 16, 2010
 *
 */
package de.sybig.oba.server;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.Produces;
import javax.ws.rs.ext.Provider;

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import org.semanticweb.owlapi.model.OWLOntology;

@Provider
@Produces("text/html")
public class HtmlMarshaller extends OntoMarshaller {

	@Override
	protected String listCls(Collection<OWLClass> cls, Annotation[] annotations) {
		StringBuffer out = new StringBuffer();
		String htmlBase = getHtmlBase(annotations);
		out.append("<ul>");
		for (OWLClass c : cls) {
			// if (cls instanceof ObaClass) {
			// System.out.println(((ObaClass) cls).getOntology());
			// }
			out.append(String.format("<li>%s</li>", getHtmlLink(c)));
		}
		out.append("</ul>\n");
		return out.toString();
	}

	@Override
	protected String listProperties(Collection<OWLObjectProperty> list,
			Annotation[] arg3) {
		StringBuffer out = new StringBuffer();
		// String htmlBase = getHtmlBase(arg3);
		out.append("<ul>");
		for (OWLObjectProperty c : list) {
			out.append(String.format("<li>%s</li>",
					getHtmlLink(c.getIRI(), "objectProperty/")));
		}
		out.append("</ul>\n");
		return out.toString();
	}

	@Override
	protected String listList(Collection<Collection> list, Annotation[] arg3) {
		StringBuffer out = new StringBuffer();
		out.append("<ul class=\"superLevel\">");

		for (Collection<OWLClass> subList : list) {
			out.append("<li></li>");
			out.append(listCls(subList, arg3));
		}
		out.append("</ul>");
		return out.toString();
	}

	@Override
	protected String listMap(Map<OWLClass, Collection> map,
			Annotation[] annotations) {
		StringBuffer out = new StringBuffer();
		out.append("<ul class=\"superLevel\">");

		for (OWLClass cls : map.keySet()) {
			out.append(String.format("<li>%s</li>", cls.getIRI().getFragment()));
			out.append(listCls(map.get(cls), annotations));
		}
		out.append("</ul>");
		return out.toString();
	}

	@Override
	protected String convertCls(OWLClass cls, Annotation[] annotations) {
		// String htmlBase = getHtmlBase(annotations);
		OWLOntology ontology = getOntology(cls);
		StringBuffer out = new StringBuffer("<html><head></head><body>");

		out.append(String.format("<h1>%s</h1>", cls.getIRI()));

		// Attributes

		printAnnotations(out, cls, ontology);

		// parents
		out.append("<h2>Parents</h2>\n<ul>");
		for (OWLClass p : getParents(cls, ontology)) {
			out.append(String.format("<li>%s</li>\n", getHtmlLink(p.getIRI())));
		}
		out.append("</ul>\n");

		// childs
		out.append("<h2>Children</h2>\n<ul>");
		Set<ObaClass> children = getChildren(cls, ontology);
		for (OWLClass c : children) {
			out.append(String.format("<li>%s</li>\n", getHtmlLink(c)));
		}
		out.append("</ul>\n");

		// Restrictions
		out.append("<h2>Relations</h2><dl>");
		HashSet<ObaObjectPropertyExpression> properties = getObjectRestrictions(
				cls, ontology);
		for (ObaObjectPropertyExpression p : properties) {
			out.append(String
					.format("<dt><a href=\"../property/%s?ns=%s\">%s</a></dt><dd>%s</dd>\n",
							p.getRestriction().getIRI().getFragment(), p
									.getRestriction().getIRI().getStart(), p
									.getRestriction().getIRI().getFragment(),
							getHtmlLink(p.getTarget().getIRI())));
		}
		out.append("</dl>");

		return out.toString();
	}

	@Override
	protected String convertProperty(OWLObjectProperty r, Annotation[] arg3) {
		StringBuffer out = new StringBuffer();
		String htmlBase = getHtmlBase(arg3);
		OWLOntology ontology = getOntology(r);

		out.append(String.format("<h1>Object property \"%s\"</h1>\n", r
				.getIRI().getFragment()));
		out.append(String.format("<h2>Namespace</h2>%s\n", r.getIRI()
				.getStart()));

		printAnnotations(out, r, ontology);

		// super propterties
		HashSet<OWLObjectProperty> parentRestrictions = OntologyHelper
				.getParentRroperties(r, ontology);
		if (parentRestrictions.size() > 0) {
			out.append("<h2>Super properties</h2><ul>");
			for (OWLObjectProperty p : parentRestrictions) {
				out.append(String.format("<li>%s</li>",
						getHtmlLink(p.getIRI(), htmlBase)));
			}
			out.append("</ul>");
		}

		// sub properteis
		HashSet<OWLObjectProperty> childRestrictions = OntologyHelper
				.getChildRroperties(r, ontology);
		if (childRestrictions.size() > 0) {
			out.append("<h2>Sub properties</h2><ul>");
			for (OWLObjectProperty p : childRestrictions) {
				out.append(String.format("<li>%s</li>",
						getHtmlLink(p.getIRI(), htmlBase)));
			}
			out.append("</ul>");
		}
		Set<OWLObjectPropertyExpression> inverseSet = r.getInverses(ontology);
		if (inverseSet.size() > 0) {
			out.append("<h2>Inverse property</h2>");
			OWLObjectProperty p = inverseSet.iterator().next()
					.getObjectPropertiesInSignature().iterator().next();
			out.append(String.format("<li>%s</li>",
					getHtmlLink(p.getIRI(), htmlBase)));
		}
		return out.toString();
	}

	private void printAnnotations(StringBuffer out, OWLEntity cls,
			OWLOntology ontology) {
		Set<ObaAnnotation> annotations = OntologyHelper
				.getAnnotationProperties(cls, ontology);
		if (annotations.size() < 1) {
			return;
		}
		out.append("<h2>Properties</h2><dl>");
		for (ObaAnnotation property : annotations) {
			out.append(String.format("<dt>%s</dt><dd>[%s] %s</dd>",
					property.getName(), property.getLanguage(),
					property.getValue()));
		}
		out.append("</dl>");
	}

	private String getHtmlBase(Annotation[] annotations) {
		for (Annotation ann : annotations) {
			if (ann.annotationType().equals(HtmlBase.class)) {
				return ((HtmlBase) ann).value();
			}
		}
		return "";
	}

	private String getHtmlLink(IRI iri) {
		return getHtmlLink(iri, "");

	}

	private String getHtmlLink(IRI iri, String htmlBase) {
		return String.format("<a href=%s%s?ns=%s>%s</a>", htmlBase,
				iri.getFragment(), iri.getStart(), iri.getFragment());
	}

	private String getHtmlLink(OWLClass cls) {
		if (cls instanceof ObaClass) {
			OWLOntology ontology = ((ObaClass) cls).getOntology();
			String name = OntologyHandler.getInstance().getNameOfOntology(
					ontology);
			String htmlBase = String.format("/%s/cls/", name);
			return getHtmlLink(cls.getIRI(), htmlBase);
		}
		return getHtmlLink(cls.getIRI());

	}
}

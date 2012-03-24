/*
 * Created on Apr 16, 2010
 *
 */
package de.sybig.oba.server;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.ws.rs.WebApplicationException;

import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationValue;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import org.semanticweb.owlapi.model.OWLOntology;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.manchester.cs.owl.owlapi.OWLLiteralImpl;
import uk.ac.manchester.cs.owl.owlapi.OWLSubClassOfAxiomImpl;

public class OntologyHelper {
    private static Logger logger = LoggerFactory
            .getLogger(OntologyHelper.class);

    /**
     * Get the ontology the class or property belongs to. If the class is a proxy object of the type {@link ObaClass},
     * the ontology is from the proxy class is returned. Otherwise from the loaded ontologies the first one which
     * defines a class with this IRI is returned. If no ontology can be found a WebApplicationException is thrown.
     *
     * @param cls A OWLClass to get the ontology for.
     * @return The (first) ontology which defines this class.
     */
    public static OWLOntology getOntology(OWLEntity cls) {
        OWLOntology ontology = null;
        if (cls instanceof ObaClass) {
            ontology = ((ObaClass) cls).getOntology();
        } else if (cls instanceof OWLClass) {
            OntologyHandler oh = OntologyHandler.getInstance();
            ontology = oh.getOntologyForClass((OWLClass) cls);
        } else if (cls instanceof OWLObjectProperty) {
            OntologyHandler oh = OntologyHandler.getInstance();
            ontology = oh.getOntologyForProperty((OWLObjectProperty) cls);
        }
        if (ontology == null) {
            // if ontology is null, cls wasn't found in any ontology, return
            // 404
            throw new WebApplicationException(404);
        }
        return ontology;
    }

    public static Set<ObaClass> getChildren(ObaClass parent) {
        if (parent == null) {
            return null;
        }
        return getChildren(parent, parent.getOntology());
    }

    /**
     * Get the sub classes of the given class. If the class is owl:thing also classes without explicitly defined super
     * classes are returned.
     *
     * @param parent
     * @param ontology
     * @return
     */
    public static Set<ObaClass> getChildren(OWLClass parent,
                                            OWLOntology ontology) {

        Set<ObaClass> outChildren = new HashSet<ObaClass>();

        Set<OWLClassExpression> children;
        children = parent.getSubClasses(ontology);

        for (OWLClassExpression p : children) {
            if (!p.getClassExpressionType().getName().equals("Class")) {
                continue;
            }
            OWLClass c = p.getClassesInSignature().iterator()
                    .next();
            outChildren.add(new ObaClass(c, ontology));     
        }
        if (parent.isOWLThing()) {
            OntologyHandler oh = OntologyHandler.getInstance();
            OntologyResource or = oh.getOntologyResource(((ObaClass) parent)
                    .getOntology());
            outChildren.addAll(or.getOntology().getOrphanChildren());
            outChildren.removeAll(or.getOntology().getObsoleteClasses());
            outChildren.remove(parent);
        }
        return outChildren;
    }

    public static Set<ObaAnnotation> getAnnotationProperties(
            final OWLEntity cls, final OWLOntology ontology) {
        Set<ObaAnnotation> properties = new HashSet<ObaAnnotation>();
        Set<OWLAnnotation> attribs = cls.getAnnotations(ontology);
        for (OWLAnnotation a : attribs) {
            OWLAnnotationValue value = a.getValue();
            if (value instanceof OWLLiteralImpl) {
                ObaAnnotation odp = new ObaAnnotation();
                odp.setLanguage(((OWLLiteralImpl) value).getLang());
                odp.setIri(a.getProperty().getIRI());
                odp.setValue(((OWLLiteralImpl) value).getLiteral());

                properties.add(odp);
                // } else if (value instanceof OWLAnonymousIndividualImpl) {
                // TODO check with GO
                // ObaAnnotation odp = new ObaAnnotation();
                // System.out.println(((OWLAnonymousIndividualImpl) value)
                // .getIndividualsInSignature());
                // } else {
                // logger.debug(
                // "could not get data property for {} because value is not an OWLLiteralImpl, but {}",
                // a, value.getClass());
            }
        }
        return properties;
    }

    public static Set<ObaClass> getParents(final ObaClass cls) {
        return getParents(cls, cls.getOntology());
    }

    public static Set<ObaClass> getParents(final OWLClass cls,
                                           org.semanticweb.owlapi.model.OWLOntology ontology) {
        if (ontology == null) {
            ontology = getOntology(cls);
        }
        HashSet<ObaClass> out = new HashSet<ObaClass>();

        if (ontology == null) {
            WebApplicationException ex = new WebApplicationException(404);
            throw ex;
        }
        Set<OWLClassExpression> parents = cls.getSuperClasses(ontology);
        if (parents.size() < 1 && !cls.isOWLThing()) {
            // if a class has no superclass explicit superclass in the owl the
            // list is empty.
            OntologyHandler oh = OntologyHandler.getInstance();
            ObaClass thing = oh.getRootOfOntology(ontology);
            out.add(thing);
        }
        for (OWLClassExpression p : parents) {
            if (!p.getClassExpressionType().getName().equals("Class")) {
                continue;
            }
            OWLClass c = p.getClassesInSignature().iterator().next();
            out.add(new ObaClass(c, ontology));
        }
        return out;
    }

    public static Set<ObaObjectPropertyExpression> getObjectRestrictions(
            OWLClass startingClass) {
        if (!(startingClass instanceof ObaClass)) {
            logger.error(
                    "could not get object properties for class '{}', because it is no ObaClass, ontology is missing",
                    startingClass);
            return null;
        }
        return getObjectRestrictions(startingClass,
                ((ObaClass) startingClass).getOntology());
    }

    public static Set<ObaObjectPropertyExpression> getObjectRestrictions(
            OWLClass startingClass,
            org.semanticweb.owlapi.model.OWLOntology ontology) {
        HashSet<ObaObjectPropertyExpression> out = new HashSet<ObaObjectPropertyExpression>();

        Set<OWLAxiom> properties = startingClass.getReferencingAxioms(ontology);
        for (OWLAxiom p : properties) {
            // System.out.println(p);
            OWLSubClassOfAxiomImpl pp = null;
            if (p instanceof OWLSubClassOfAxiomImpl) {
                ObaObjectPropertyExpression obaRestriction = new ObaObjectPropertyExpression();
                pp = (OWLSubClassOfAxiomImpl) p;

                if (!pp.getSubClass().equals(startingClass)) {
                    continue;
                }
                Set<OWLObjectProperty> property = pp
                        .getObjectPropertiesInSignature();
                if (property.size() < 1) {
                    // we have the subclass restriction
                    continue;
                }
                ObaClass oc = new ObaClass(pp.getSuperClass()
                        .getClassesInSignature().iterator().next(), ontology);
                obaRestriction.setTarget(oc);
                obaRestriction.setRestriction(property.iterator().next());

                out.add(obaRestriction);
                // }
            }
        }
        return out;
    }

    public static Set<OWLObjectProperty> getParentRroperties(
            OWLObjectProperty r, OWLOntology ontology) {
        Set<OWLObjectPropertyExpression> parents = r
                .getSuperProperties(ontology);
        return extractFromObjectPropertyExpression(parents);

    }

    /**
     * Get the sub properties of a object property
     *
     * @param r
     * @param ontology
     * @return
     */
    public static Set<OWLObjectProperty> getChildRroperties(
            OWLObjectProperty r, OWLOntology ontology) {
        Set<OWLObjectPropertyExpression> children = r
                .getSubProperties(ontology);
        return extractFromObjectPropertyExpression(children);
    }

    private static Set<OWLObjectProperty> extractFromObjectPropertyExpression(
            Set<OWLObjectPropertyExpression> list) {
        HashSet<OWLObjectProperty> out = new HashSet<OWLObjectProperty>();

        for (OWLObjectPropertyExpression p : list) {
            out.add(p.getObjectPropertiesInSignature().iterator().next());
        }
        return out;
    }

    // ///////// Object properties

    /**
     * Get a list of object properties as object with the names given in the list <code>name</code>. If
     * <code>name</code> is <code>null</code> all object properties are returned.
     *
     * @param model
     * @param names The list of (local) names to search for
     * @return
     */
    public static Set<OWLObjectProperty> getObjectProperties(OWLOntology model,
                                                             Collection<String> names) {

        Set<OWLObjectProperty> restrictions = model
                .getObjectPropertiesInSignature();
        Iterator<OWLObjectProperty> it = restrictions.iterator();
        while (it.hasNext()) {
            OWLObjectProperty prop = it.next();
            String name = prop.getIRI().getFragment();
            if (names != null && !names.contains(name)) {
                it.remove();
            }
        }
        return restrictions;
    }

}

/*
 * Created on Apr 16, 2010
 *
 */
package de.sybig.oba.server;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;

import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLOntology;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class OntoMarshaller implements MessageBodyWriter<Object> {

    private Logger log = LoggerFactory.getLogger(OntoMarshaller.class);

    private final int CACHE_TIME = 3600 * 24 * 30;

    @Override
    public void writeTo(Object arg0, Class arg1, Type arg2, Annotation[] arg3,
            MediaType arg4, MultivaluedMap httpHeader, OutputStream os)
            throws IOException, WebApplicationException {

        setCachControle(httpHeader);
        if (arg0 instanceof ObaClass) { // If we have more subclasses from OWLClass, we have to add them here and below for list and map
            os.write(convertCls((ObaClass) arg0, arg3).getBytes());
        } else if (arg0 instanceof OWLNamedIndividual) {
            os.write(convertIndividual((OWLNamedIndividual) arg0, arg3)
                    .getBytes());
        } else if (arg0 instanceof OWLObjectProperty) {
            os.write(convertProperty((OWLObjectProperty) arg0, arg3).getBytes());
        } else if (arg0 instanceof Set || arg0 instanceof List) {
            if (!((Collection) arg0).iterator().hasNext()) {
                // empty collection
                return;
            }
            Object first = ((Collection) arg0).iterator().next();
            if (first instanceof ObaClass) {
                Collection<ObaClass> set = (Collection<ObaClass>) arg0;
                os.write(listCls(set, arg3).getBytes());
            } else if (first instanceof OWLObjectProperty) {
                Collection<OWLObjectProperty> set = (Collection<OWLObjectProperty>) arg0;
                os.write(listProperties(set, arg3).getBytes());
            } else if (first instanceof Collection) {
                Collection<Collection> set = (Collection<Collection>) arg0;
                os.write(listList(set, arg3).getBytes());
            }
        } else if (arg0 instanceof Map) {
            Map<ObaClass, Collection> map = (Map<ObaClass, Collection>) arg0;
            os.write(listMap(map, arg3).getBytes());
        }
    }

    @Override
    public boolean isWriteable(Class arg0, Type arg1, Annotation[] arg2,
            MediaType arg3) {
        Class type = arg0;
        while (type != null) {

            Class[] interfaces = type.getInterfaces();
            for (Class c : interfaces) {
                if (c.equals(OWLClass.class)
                        || c.equals(OWLNamedIndividual.class)
                        || c.equals(OWLObjectProperty.class)) {
                    return true;
                } else if (c.equals(Set.class) || c.equals(List.class)) {
                  ;
                    if (arg1 instanceof ParameterizedType) {
                        ParameterizedType t = (ParameterizedType) arg1;
                        if (t.getActualTypeArguments()[0].equals(ObaClass.class)
                                || t.getActualTypeArguments()[0].equals(OWLClass.class)) {
                            //both have the format "class de.sybig.oba.server.ObaClass"
                            return true;
                        }
                    } else if (arg1 instanceof Class) {
                        // 2 dimensional list
                            // the condiditon below was arg0, changed it to arg0 for the example
                            // http://localhost:9998/go/functions/basic/XdownstreamOfY/GO_0060076;ns=http:$$purl.obolibrary.org$obo$/GO_0005575?ns=http://purl.obolibrary.org/obo/
                        if (arg0.equals(LinkedList.class)) {
                            //TODO how to get the type of the second list?
                            return true;
                        }
                    } else {
                        log.error("can not marshall list of " + arg1);
                        return false;
                    }

                    return false;
                } else if (c.equals(Map.class)) {
                    if (((ParameterizedType) arg1).getActualTypeArguments()[0].toString().equals("interface org.semanticweb.owlapi.model.OWLClass")
                            && ((ParameterizedType) arg1).getActualTypeArguments()[1].toString().equals("java.util.List<org.semanticweb.owlapi.model.OWLClass>")) {
                        //TODO make better
                        return true;
                    }                  
                    return false;
                }
            }
            type = type.getSuperclass();
        }
        return false;
    }

    @Override
    public long getSize(Object arg0, Class arg1, Type arg2, Annotation[] arg3,
            MediaType arg4) {
        // let the http layer determine the size
        return -1;
    }

    protected static OWLOntology getOntology(OWLEntity cls) {
        return OntologyHelper.getOntology(cls);
    }

    protected abstract String convertCls(ObaClass c, Annotation[] arg3);

    // protected abstract String convertIndividual(OWLIndividual c, Annotation[]
    // arg3);
    protected String convertIndividual(OWLNamedIndividual c, Annotation[] arg3) {
        return "not implemented yet";
    }

    protected abstract String convertProperty(OWLObjectProperty r,
            Annotation[] arg3);

    protected abstract String listCls(Collection<ObaClass> c, Annotation[] arg3);

    protected abstract String listProperties(
            Collection<OWLObjectProperty> list, Annotation[] arg3);

    protected abstract String listList(Collection<Collection> list,
            Annotation[] arg3);

    protected abstract String listMap(Map<ObaClass, Collection> map,
            Annotation[] annotations);

    /**
     * Get all super classes for the given class.
     *
     * @param cls
     * @return
     */
    protected Set<OWLClassExpression> getParents(OWLClass cls) {
        OntologyHandler oh = OntologyHandler.getInstance();

        Set<OWLClassExpression> parents = cls.getSuperClasses(oh
                .getOntologyForClass(cls));
        return parents;
    }

    /**
     * @see OntologyHelper#getChildren(OWLClass, OWLOntology)
     */
    protected Set<ObaClass> getChildren(OWLClass parent,
            org.semanticweb.owlapi.model.OWLOntology ontology) {

        return OntologyHelper.getChildren(parent, ontology);
    }

    protected Set<ObaClass> getParents(final OWLClass cls,
            org.semanticweb.owlapi.model.OWLOntology ontology) {
        return OntologyHelper.getParents(cls, ontology);
    }

    protected Set<ObaObjectPropertyExpression> getObjectRestrictions(
            OWLClass cls, org.semanticweb.owlapi.model.OWLOntology ontology) {
        return OntologyHelper.getObjectRestrictions(cls, ontology);
    }

    private Set<ObaAnnotation> getAnnotationProperties(final OWLClass cls) {
        OntologyHandler oh = OntologyHandler.getInstance();
        OWLOntology ontology = oh.getOntologyForClass(cls);
        return getAnnotationProperties(cls, ontology);
    }

    protected Set<ObaAnnotation> getAnnotationProperties(final OWLClass cls,
            final OWLOntology ontology) {
        return OntologyHelper.getAnnotationProperties(cls, ontology);
    }

    protected void setCachControle(MultivaluedMap httpHeader) {
        if (!httpHeader.containsKey("Cache-Control")) {
            httpHeader.add("Cache-Control", String.format(
                    "public,max-age=%d,s-maxage=%d", CACHE_TIME, CACHE_TIME));
        }
    }
}

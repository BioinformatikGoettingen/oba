/*
 * Created on Apr 20, 2010
 *
 */
package de.sybig.oba.server;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLOntology;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.annotate.JsonSerialize.Inclusion;

/**
 *
 * @author juergen.doenitz@bioinf.med.uni-goettingen.de
 *
 */
@Provider
@Produces("application/json")
public class JsonMarshaller implements MessageBodyWriter<Object> {

    private static final Logger log = LoggerFactory.getLogger(JsonMarshaller.class);
    private static final int CACHE_TIME = 3600 * 24 * 30;

    @Override
    public boolean isWriteable(Class arg0, Type arg1, Annotation[] arg2,
            MediaType arg3) {
        //TODO Copy & Pasted from OntoMarshaller, implement better solution
        Class type = arg0;
        while (type != null) {

            Class[] interfaces = type.getInterfaces();
            for (Class c : interfaces) {
                if (c.equals(OWLClass.class)
                        || c.equals(OWLNamedIndividual.class)
                        || c.equals(OWLObjectProperty.class)) {
                    return true;
                } else if (c.equals(Set.class) || c.equals(List.class)) {

                    if (arg1 instanceof ParameterizedType) {

                        ParameterizedType t = (ParameterizedType) arg1;
                        if (t.getActualTypeArguments()[0].equals(ObaClass.class)
                                || t.getActualTypeArguments()[0].equals(OWLClass.class)) {
                            //both have the format "class de.sybig.oba.server.ObaClass"
                            return true;
                        }
                    } else if (arg1 instanceof Class) {
                        // 2 dimensional list
                        if (arg1.equals(LinkedList.class)) {
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

    public void writeTo(Object arg0, Class arg1, Type arg2, Annotation[] arg3,
            MediaType arg4, MultivaluedMap httpHeader, OutputStream os)
            throws IOException, WebApplicationException {
        setCachControle(httpHeader);
        if (arg0 instanceof OWLClass || arg0 instanceof ObaClass) {
            OWLClass c = (OWLClass) arg0;
            JsonCls oc = copyCls(c, true);
            marshallObject(oc, JsonCls.class, os);
        } else if (arg0 instanceof OWLObjectProperty) {
            JsonObjectProperty property = copyProperty(
                    (OWLObjectProperty) arg0, true);
            marshallObject(property, JsonObjectProperty.class, os);
        } else if (arg0 instanceof Set || arg0 instanceof List) {
            Collection set = (Collection) arg0;
            if (set.size() < 1) {
                return;
            }
            Object first = set.iterator().next();

            if (first instanceof OWLClass) {
                JsonClsList outList = copyClsList((Collection<OWLClass>) set);
                outList.setRawEntities(null); // otherwise the list is
                // duplicated
                marshallObject(outList, JsonClsList.class, os);
            } else if (first instanceof OWLObjectProperty) {
                JsonPropertyList outList = copyPropertyList((Collection<OWLObjectProperty>) set);
                marshallObject(outList, JsonPropertyList.class, os);
            } else if (first instanceof Collection) {
                Json2DClsList outList = copy2DClsList((Collection<Collection<OWLClass>>) set);
                outList.setRawEntities(null);
                marshallObject(outList, Json2DClsList.class, os);
            }

        } else if (arg0 instanceof Map) {
            Map<OWLClass, Collection<OWLClass>> map = (Map<OWLClass, Collection<OWLClass>>) arg0;
            if (map.size() < 1) {
                return;
            }
            Json2DClsList outList = new Json2DClsList();

            for (OWLClass key : map.keySet()) {
                JsonClsList innerList = new JsonClsList();
                innerList.add(copyCls(key, false));
                for (OWLClass value : map.get(key)) {
                    innerList.add(copyCls(value, false));
                }
                innerList.setRawEntities(null); // otherwise the list is
                // duplicated
                outList.add(innerList);
            }
            outList.setRawEntities(null);
            marshallObject(outList, Json2DClsList.class, os);
        }
    }

    @Override
    public long getSize(Object t, Class<?> type, Type genericType,
            Annotation[] annotations, MediaType mediaType) {
        return -1;
    }

    /**
     * Marshalles the given object into the json format and writes it to the
     * output stream.
     *
     * @param o The object to marshall
     * @param c The type of the marshalled class
     * @param os The stream to write the output to.
     */
    private void marshallObject(Object o, Class<?> c, OutputStream os)
            throws IOException {
        try {
			// JAXBContext ctx = JAXBContext.newInstance(c);
            // JSONMarshaller m =
            // JSONJAXBContext.getJSONMarshaller(ctx.createMarshaller());
            // m.marshallToJSON(o, os);
            ObjectMapper mapper = new ObjectMapper();
			// mapper.configure(SerializationConfig.WRITE_NULL_PROPERTIES,
            // false);
            mapper.getSerializationConfig().setSerializationInclusion(
                    Inclusion.NON_NULL);
            mapper.writeValue(os, o);
        } catch (Exception e) {
            log.error("could not marshall object {} to json, reason: ", o, e);
             throw new WebApplicationException(500);
        }
    }

    private JsonCls copyCls(OWLClass c, boolean fillcomplete) {
        JsonCls out = new JsonCls();
        out.setShell(!fillcomplete);
        out.setName(c.getIRI().getFragment());
        out.setNamespace(c.getIRI().getStart());
        OWLOntology ontology = OntoMarshaller.getOntology(c);
        out.setAnnotations(getJsonAnnotations(c, ontology));

        if (!fillcomplete) {
            out.setIsMarshalling(true);
            return out;
        }

        Collection<ObaClass> parents = OntologyHelper.getParents(c, ontology);
        for (OWLClass p : parents) {
            out.addParent(copyCls(p, false));
        }

        Set<ObaClass> children = OntologyHelper.getChildren(c, ontology);
        // init list, so that we have at least an empty list, not null
        out.setChildren(new HashSet<JsonCls>());
        for (OWLClass child : children) {
            out.addChild(copyCls(child, false));
        }

        Set<ObaObjectPropertyExpression> properties = OntologyHelper
                .getObjectRestrictions(c, ontology);
        // OntologyHelper.getObjectRestrictions(startingClass, ontology)
        for (ObaObjectPropertyExpression p : properties) {
            JsonObjectPropertyExpression jp = new JsonObjectPropertyExpression();
            jp.setProperty(copyProperty(p.getRestriction(), true));
            jp.setTarget(copyCls(p.getTarget(), false));
            out.addRestriction(jp);

        }
        out.setIsMarshalling(true);
        return out;
    }

    private JsonClsList copyClsList(Collection<OWLClass> list) {
        JsonClsList outList = new JsonClsList();

        for (OWLClass cls : list) {
            outList.add(copyCls(cls, false));
        }
        return outList;
    }

    private Json2DClsList copy2DClsList(Collection<Collection<OWLClass>> list) {
        Json2DClsList outList = new Json2DClsList();
        for (Collection<OWLClass> l : list) {
            outList.add(copyClsList(l));
        }
        return outList;
    }

    private JsonPropertyList copyPropertyList(Collection<OWLObjectProperty> list) {
        JsonPropertyList outList = new JsonPropertyList();
        for (OWLObjectProperty p : list) {
            outList.add(copyProperty(p, true));
        }
        return outList;
    }

    /**
     * Generates a JsonObjectProperty from an OWLObjectProperty.
     *
     * @param property The template property.
     * @param fillcomplete If the class should be filled completely, i. e with
     * sub- and super properties.
     * @return
     */
    private JsonObjectProperty copyProperty(OWLObjectProperty property,
            boolean fillcomplete) {
        JsonObjectProperty out = new JsonObjectProperty();
        out.setName(property.getIRI().getFragment());
        out.setNamespace(property.getIRI().getStart());
        OWLOntology ontology = OntoMarshaller.getOntology(property);
        out.setAnnotations(getJsonAnnotations(property, ontology));
        if (!fillcomplete) {
            return out;
        }
        out.setShell(!fillcomplete);
        Set<OWLObjectProperty> parents = OntologyHelper.getParentRroperties(
                property, ontology);
        for (OWLObjectProperty p : parents) {
            out.addSuperProperty(copyProperty(p, false));
        }
        Set<OWLObjectProperty> children = OntologyHelper.getChildRroperties(
                property, ontology);
        for (OWLObjectProperty p : children) {
            out.addSubProperty(copyProperty(p, false));
        }
        return out;
    }

    private Set<JsonAnnotation> getJsonAnnotations(OWLEntity c,
            OWLOntology ontology) {
        Set<JsonAnnotation> out = new HashSet<JsonAnnotation>();
        for (ObaAnnotation property : OntologyHelper.getAnnotationProperties(c,
                ontology)) {
            JsonAnnotation ja = new JsonAnnotation();
            ja.setLanguage(property.getLanguage());
            ja.setName(property.getName());
            ja.setNamespace(property.getIri().getStart());
            ja.setValue(property.getValue());
            out.add(ja);
        }
        return out;
    }

    protected void setCachControle(MultivaluedMap httpHeader) {
        if (!httpHeader.containsKey("Cache-Control")) {
            httpHeader.add("Cache-Control", String.format(
                    "public,max-age=%d,s-maxage=%d", CACHE_TIME, CACHE_TIME));
        }
    }
}

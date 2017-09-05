package de.sybig.oba.client;

import java.util.HashSet;
import java.util.Set;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;

import de.sybig.oba.server.JsonAnnotation;
import de.sybig.oba.server.JsonCls;
import de.sybig.oba.server.JsonObjectPropertyExpression;
import java.net.ConnectException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@XmlType
@XmlRootElement
public class OntologyClass<C extends OntologyClass> extends JsonCls<C> {
    private static Logger log = LoggerFactory.getLogger(OntologyClass.class);
    @XmlTransient
    protected Set<C> ocChildren;
    @XmlTransient
    protected Set<C> ocParents;
    // @XmlTransient
    // protected Set<JsonObjectPropertyExpression<C>> ocRestrictions;
    @XmlTransient
    private GenericConnector connector;

    public OntologyClass() {
        super();
    }

    public OntologyClass(JsonCls c) {
        fillWithTemplate(c);
    }

    public Set<JsonAnnotation> getLabels() {
        return getAnnotationValues("label");
    }

    /**
     * Get the value of an annotation. If the class has no annotation with this
     * name <code>null</code> is returned. If the class has multiple annotations
     * with this name but with values in different language only the value of
     * the first annotation is returned. The order of the annotations is not
     * defined.
     *
     * @param name
     *            The name of the annotation to get the value for.
     * @return The value of the annotation, or <code>null</code>
     */
    public String getSingleAnnotationValue(String name) {
        Set<JsonAnnotation> annotationsValues = getAnnotationValues(name);
        if (annotationsValues == null || annotationsValues.size() < 1) {
            return null;
        }
        return annotationsValues.iterator().next().getValue();
    }

    public Set<JsonAnnotation> getAnnotationValues(String name) {
        if (annotations == null) {
            return null;
        }
        Set<JsonAnnotation> foundAnnotations = new HashSet<JsonAnnotation>();
        for (JsonAnnotation annotation : annotations) {
            if (!name.equals(annotation.getName())) {
                continue;
            }
            foundAnnotations.add(annotation);
        }
        return foundAnnotations;
    }

    @Override
    public Set<C> getChildren() {
        if (_children == null && !shell) {
            return null;
        }
        if (shell) {
            fillCls();
        }
        if (ocChildren == null && _children != null) {
            ocChildren = new HashSet<C>();
            for (Object o : _children) {
                JsonCls c = (JsonCls) o;
                C child = (C) createNewOntologyClass(c);
                child.setConnector(connector);
                ocChildren.add(child);
            }
        }
        return ocChildren;
    }

    @Override
    public Set<C> getParents() {
        if (shell) {
            fillCls();
        }
        if (ocParents == null && _parents != null) {
            ocParents = new HashSet<C>();
            for (JsonCls c : _parents) {
                C parent = (C) createNewOntologyClass(c);
                parent.setConnector(connector);
                ocParents.add(parent);
            }
        }
        return ocParents;
    }

    @Override
    public Set<JsonObjectPropertyExpression> getProperties() {

        if (properties != null) {
            for (JsonObjectPropertyExpression ope : properties) {
                JsonCls target = ope.getTarget();
                if (!(target instanceof OntologyClass)) {
                    C newTarget = createNewOntologyClass(target);
                    newTarget.setConnector(connector);
                    ope.setTarget(newTarget);
                }
            }
        }

        return properties;
    }

    public Set<JsonAnnotation> getAnnotations() {
        if (shell) {
            fillCls();
        }
        return annotations;
    }

    public void setConnector(GenericConnector connector) {
        this.connector = connector;
    }

    protected C createNewOntologyClass(JsonCls c) {
        return (C) new OntologyClass(c);
    }

    private void fillWithTemplate(JsonCls c) {
        shell = c.isShell();
        annotations = c.getAnnotations();
        _children = c.getRawChildren();
        name = c.getName();
        namespace = c.getNamespace();
        _parents = c.getRawParents();

        properties = c.getProperties();
    }

    private void fillCls() {
        if (connector == null) {
            // The connector is set to null if the class is sent back to the
            // server. Otherwise the marshaller would fetch the whole ontology.
            return;
        }
        JsonCls filledClass;
        try {
            filledClass = connector.getCls(this);
        } catch (ConnectException ex) {
            log.error("Could not reach the oba server to fill this class", ex);
            return;
        }
        fillWithTemplate(filledClass);
    }

    public String toString() {

        if (getLabels() != null && getLabels().size() > 0) {
            return getLabels().iterator().next().getValue();
        }
        return name;
    }

    public static String getVersion() {
        return "20111204";
    }
}

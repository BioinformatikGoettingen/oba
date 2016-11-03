/*
 * Created on Dec 3, 2009
 *
 */
package de.sybig.oba.server;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonProperty;

/**
 * A list of ontology classes. The type of the ontology class implementation can
 * be given as parameter for the class.
 *
 * <p>
 * The ontology classes have an own implementation of a list because the
 * entities from the unmarshalling step differ from the entities used by an
 * application in the implementations extending this list class. During the
 * unmarshalling the entities are stored in the list <code>_entities</code>,
 * during accessing they are copied to the list <code>entities</code> and may be
 * altered in this step, i.d. the connector is set in each entity.</p>
 *
 * <p>
 * This class is not defined as <code>abstract</code> to allow cloning.
 *
 * @author juergen.doenitz@bioinf.med.uni-goettingen.de
 * @param <T> The type of the ontology classes to use.
 */
@XmlRootElement
@XmlType
public class JsonClsList<T extends JsonCls> implements Cloneable {

    @XmlElement(name = "entities")
    protected List<JsonCls> _entities;
    @XmlTransient
    protected List<T> entities;

    /**
     * Get the ontology classes stored in this list. The classe's content may
     * differ from the objects unmarshalled (e.g. connector may be added).
     *
     * @return The ontology classes of this list, or <code>null</code>
     */
    @JsonIgnore
    public List<T> getEntities() {
        if (entities == null) {
            if (_entities == null) {
                return null;
            }
            entities = new LinkedList<T>();
            entities.addAll((Collection<? extends T>) _entities);
        }
        return entities;
    }

    /**
     * Sets the entities of the list. The entities are stored in the list used
     * by the application and the list for marshalling.
     *
     * @param entities the entities to set
     */
    public void setEntities(List<T> entities) {
        this.entities = entities;
        if (_entities == null) {
            _entities = new LinkedList<JsonCls>();
        }
        for (T e : entities) {
            _entities.add(e);
        }

    }

    /**
     * Add an ontology class to the list of ontology classes stored by this
     * class. The class is added to the list which is used by an application as
     * well as to the list used for (un)marshalling. If one of the lists does
     * not exists yet, it is created
     *
     * @param cls
     * @return <code>true</code> if the entity could be added to the list of
     * entities, <code>false</code> otherwise.
     */
    public boolean add(T cls) {
        if (entities == null) {
            entities = new LinkedList<T>();
        }
        if (_entities == null) {
            _entities = new LinkedList<JsonCls>();
        }
        _entities.add(cls);
        return entities.add(cls);
    }

    /**
     * Get the entity at the given index.
     *
     * @param i the index
     * @return the entity at the index
     * @throws IndexOutOfBoundsException if the index is out of range (<tt>index
     * &lt; 0 || index &gt;= size()</tt>)
     *
     */
    public T get(int i) {

        return getEntities().get(i);
    }

    /**
     * Get the number of elements in the list.
     *
     * @return The size of the list.
     */
    public int size() {
        //TODO getEntities may be null
        return getEntities().size();
    }

    @Override
    public JsonClsList<T> clone() throws CloneNotSupportedException {
        super.clone();
        JsonClsList<T> newList = new JsonClsList<T>();
        newList.setEntities(getEntities());
        return newList;
    }

    /**
     * Get the internal list of entities as they are unmarshalled from JSON. the
     * entities returned by {@link #getEntities()} are extended with a
     * connector, and perhaps more, in the inherited implementations of this
     * class.
     *
     * @return The internal list of entities.
     */
    @JsonProperty("entities")
    protected List<JsonCls> getRawEntities() {
        if (_entities == null) {
            return (List<JsonCls>) entities;
        }
        return _entities;
    }

    /**
     * Sets the list of entites for the marshalling.
     *
     * @param e
     */
    protected void setRawEntities(List<JsonCls> e) {
        _entities = e;
    }
}

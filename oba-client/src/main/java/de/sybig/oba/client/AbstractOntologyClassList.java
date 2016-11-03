package de.sybig.oba.client;

import java.util.LinkedList;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;

import de.sybig.oba.server.JsonCls;
import de.sybig.oba.server.JsonClsList;
import de.sybig.oba.server.JsonEntity;

/**
 * A list of ontology classes. The type of the ontology class implementation can
 * be given as parameter for the class.
 *
 * @author juergen.doenitz@bioinf.med.uni-goettingen.de
 * @param <C> The representation of the ontology class to use.
 */
@XmlRootElement
@XmlType
public class AbstractOntologyClassList<C extends OntologyClass> extends JsonClsList<C> {

    protected List<C> ontologyEntities;

    @XmlTransient
    public GenericConnector connector;

    public AbstractOntologyClassList() {
        super();
    }

    public AbstractOntologyClassList(JsonClsList c) {
        fillWithTemplate(c);
    }

    /**
     * @return the classes
     */
    @Override
    public List<C> getEntities() {
        if (ontologyEntities == null) {
            if (_entities == null) {
                return null;
            }
            if (ontologyEntities == null) {
                ontologyEntities = new LinkedList<C>();

                for (JsonEntity c : _entities) {
                    if (c instanceof OntologyClass) {
                        ontologyEntities.add((C) c);
                        continue;
                    }
                    C nc = (C) new OntologyClass((JsonCls) c);
                    nc.setConnector(connector);
                    ontologyEntities.add(nc);
                }
            }
        }
        return ontologyEntities;
    }

    public void setConnector(GenericConnector connector) {
        this.connector = connector;
        if (connector == null && ontologyEntities != null) {
            // remove connector from all entities
            for (OntologyClass e : ontologyEntities) {
                e.setConnector(null);
            }
        }
    }

    @Override
    public void setEntities(List<C> entities) {
        this.entities = entities;
        if (entities == null) {
            return;
        }
        if (_entities == null) {
            _entities = new LinkedList<JsonCls>();
        }
        for (C e : entities) {
            _entities.add(e);
        }

    }

    private void fillWithTemplate(JsonClsList list) {
        this._entities = list.getEntities();
    }

    private GenericConnector getConnector() {
        return connector;
    }
}

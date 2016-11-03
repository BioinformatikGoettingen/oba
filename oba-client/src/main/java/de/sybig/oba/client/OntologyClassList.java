package de.sybig.oba.client;

import java.util.LinkedList;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import de.sybig.oba.server.JsonCls;
import de.sybig.oba.server.JsonClsList;
import de.sybig.oba.server.JsonEntity;

@XmlRootElement
@XmlType
public class OntologyClassList extends AbstractOntologyClassList<OntologyClass> {

//	protected List<C> ontologyEntities;
//	@XmlTransient
//	public GenericConnector connector;
    public OntologyClassList() {
        super();
    }

    public OntologyClassList(JsonClsList c) {
        fillWithTemplate(c);
    }

    /**
     * @return the classes
     */
    @Override
    public List<OntologyClass> getEntities() {
        if (ontologyEntities == null) {
            if (_entities == null) {
                return null;
            }
            if (ontologyEntities == null) {
                ontologyEntities = new LinkedList<OntologyClass>();

                for (JsonEntity c : _entities) {
                    if (c instanceof OntologyClass) {
                        ontologyEntities.add((OntologyClass) c);
                        continue;
                    }
                    OntologyClass nc = new OntologyClass((JsonCls) c);
                    nc.setConnector(connector);
                    ontologyEntities.add(nc);
                }
            }
        }
        return ontologyEntities;
    }

    public OntologyClassList clone() {
        OntologyClassList newList = new OntologyClassList();
        newList.setRawEntities(getRawEntities());
        newList.setConnector(connector);
        return newList;
    }

    private void fillWithTemplate(JsonClsList list) {
        this._entities = list.getEntities();
    }

}

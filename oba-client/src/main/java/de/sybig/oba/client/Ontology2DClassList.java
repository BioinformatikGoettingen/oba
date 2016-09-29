package de.sybig.oba.client;

import java.util.LinkedList;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import de.sybig.oba.server.JsonClsList;

@XmlRootElement
@XmlType
public class Ontology2DClassList extends
        AbstractOntology2DClassList<OntologyClassList, OntologyClass> {

    private List<OntologyClassList> listEntities;

    @Override
    public List getEntities() {
        if (_entities == null) {
            if (entities == null) {
                return null;
            }
            _entities = entities;
        }
        if (listEntities == null) {
            listEntities = new LinkedList<OntologyClassList>();
            for (JsonClsList c : _entities) {
                if (c instanceof OntologyClassList) {
                    listEntities.add((OntologyClassList) c);
                }
                OntologyClassList nc = (OntologyClassList) new OntologyClassList(
                        (JsonClsList) c);
                listEntities.add(nc);
            }
        }
        return listEntities;
    }
}

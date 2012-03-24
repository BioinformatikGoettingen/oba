package de.sybig.oba.client;

import java.util.LinkedList;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import de.sybig.oba.server.JsonClsList;

@XmlType
@XmlRootElement
public class Cytomer2DClassList extends
		Ontology2DClassList<CytomerClassList, CytomerClass> {

	// Json2DClsList<CytomerClassList, CytomerClass> {
	private List<CytomerClassList> listEntities;

	@Override
	public List getEntities() {
		if (_entities == null) {
			if (entities == null) {
				return null;
			}
			_entities = entities;
		}
		if (listEntities == null) {
			listEntities = new LinkedList<CytomerClassList>();
			for (JsonClsList c : _entities) {
				if (c instanceof OntologyClassList) {
					listEntities.add((CytomerClassList) c);
				}
				CytomerClassList nc = (CytomerClassList) new CytomerClassList(
						(JsonClsList) c);
				listEntities.add(nc);
			}
		}
		return listEntities;
	}
}

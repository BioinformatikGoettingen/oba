package de.sybig.oba.client;

import java.util.LinkedList;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import de.sybig.oba.server.JsonCls;
import de.sybig.oba.server.JsonClsList;
import de.sybig.oba.server.JsonEntity;

@XmlType
@XmlRootElement
public class CytomerClassList extends OntologyClassList<CytomerClass> {
	// protected List<CytomerClass> ontologyEntities;
	public CytomerClassList() {
		super();
	}

	public CytomerClassList(JsonClsList c) {
		fillWithTemplate(c);
	}

	@Override
	public List<CytomerClass> getEntities() {

		if (_entities == null) {
			return null;
		}
		if (ontologyEntities == null) {
			ontologyEntities = new LinkedList<CytomerClass>();

			for (JsonEntity c : _entities) {
				if (c instanceof CytomerClass) {
					ontologyEntities.add((CytomerClass) c);
					continue;
				}
				CytomerClass nc = (CytomerClass) new CytomerClass((JsonCls) c);
				nc.setConnector(connector);
				ontologyEntities.add(nc);
			}
		}
		return ontologyEntities;
	}

	public CytomerClassList clone() {
		CytomerClassList newList = new CytomerClassList();
		newList.setRawEntities(getRawEntities());
		newList.setConnector(connector);
		return newList;
	}

	private void fillWithTemplate(JsonClsList list) {
		this._entities = list.getEntities();
	}
}

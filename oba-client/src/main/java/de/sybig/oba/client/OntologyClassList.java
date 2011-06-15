package de.sybig.oba.client;

import java.util.LinkedList;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;

import de.sybig.oba.server.JsonCls;
import de.sybig.oba.server.JsonClsList;
import de.sybig.oba.server.JsonEntity;

@XmlRootElement
@XmlType
public class OntologyClassList<C extends OntologyClass> extends JsonClsList<C> {

	protected List<C> ontologyEntities;

	@XmlTransient
	public GenericConnector connector;

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

	public OntologyClassList<C> clone() {
		OntologyClassList<C> newList = new OntologyClassList<C>();
		newList.setRawEntities(getRawEntities());
		newList.setConnector(connector);
		return newList;
	}

	/**
	 * @param classes
	 *            the classes to set
	 */
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

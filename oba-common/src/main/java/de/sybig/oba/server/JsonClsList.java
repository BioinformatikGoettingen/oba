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

@XmlRootElement
@XmlType
public class JsonClsList<T extends JsonCls> implements Cloneable {

	@XmlElement(name = "entities")
	protected List<JsonCls> _entities;
	@XmlTransient
	protected List<T> entities;

	/**
	 * @return the classes
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
	 * @param classes
	 *            the classes to set
	 */
	public void setEntities(List<T> entities) {
		this.entities = entities;
	}

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

	public T get(int i) {
		return getEntities().get(i);
	}

	public int size() {
		return getEntities().size();
	}

	public JsonClsList<T> clone() {
		JsonClsList<T> newList = new JsonClsList<T>();
		newList.setEntities(getEntities());
		return newList;
	}

	@JsonProperty("entities")
	protected List<JsonCls> getRawEntities() {
		if (_entities == null) {
			return (List<JsonCls>) entities;
		}
		return _entities;
	}

	protected void setRawEntities(List<JsonCls> e) {
		_entities = e;
	}
}

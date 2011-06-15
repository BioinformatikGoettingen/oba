package de.sybig.oba.server;

import java.util.LinkedList;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlRootElement
@XmlType
public class JsonPropertyList<T extends JsonObjectProperty> {
	protected List<T> entities;

	/**
	 * @return the classes
	 */
	public List<T> getEntities() {
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
		return entities.add(cls);

	}

	public T get(int i) {
		return entities.get(i);
	}

	public int size() {
		return entities.size();
	}
}

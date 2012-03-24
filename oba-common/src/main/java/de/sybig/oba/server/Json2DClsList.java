package de.sybig.oba.server;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;
import org.codehaus.jackson.annotate.JsonIgnore;

@XmlRootElement
@XmlType
public class Json2DClsList<T extends JsonClsList<JsonCls>, C extends JsonCls> {

	@XmlElement(name = "entities")
	protected List<JsonClsList<JsonCls>> _entities;

	@XmlTransient
	protected transient List<T> entities;

	/**
	 * @return the classes
	 */

	public List<T> getEntities() {
		// if (_entities == null) {
		// return null;
		// }
		// entities = new ArrayList<T>();
		// for (JsonClsList e : _entities) {
		// entities.add((T) e);
		// }
		// return entities;
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
			_entities = new LinkedList<JsonClsList<JsonCls>>();
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

	protected void setRawEntities(List<JsonClsList<JsonCls>> e) {
		_entities = e;
	}
}

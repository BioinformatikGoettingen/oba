/*
 * Created on Nov 9, 2009
 *
 */
package de.sybig.oba.server;

import java.util.Collection;

import java.util.HashSet;
import java.util.Set;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.annotate.JsonSerialize;

/**
 * A class representing the a class of the ontology with its important
 * properties. This class can be easily be serialized to a JSON object and
 * transfered to a client.
 */
@XmlType
@XmlRootElement
public class JsonCls<C extends JsonCls> extends JsonEntity {
	// Avoid class cast exceptions
	// children and parents are filled in each case with JsonCls by the
	// unmarshaller. In the method signature we need the generic type C to allow
	// sub classes.

	@XmlElement(name = "children")
	protected Set<JsonCls> _children = new HashSet<JsonCls>();
	@XmlElement(name = "parents")
	protected Set<JsonCls> _parents = new HashSet<JsonCls>();
	// @XmlTransient
	protected transient Set<C> children = new HashSet<C>();
	// @XmlTransient
	protected transient Set<C> parents = new HashSet<C>();;
	protected Set<JsonAnnotation> annotations;
	protected Set<JsonObjectPropertyExpression> restrictions;
	// @XmlTransient
	private transient boolean isMarshalling = false;

	// private boolean isLazy = true;
	public JsonCls() {
		// JAXB needs this
	}

	/**
	 * Get the siblings of this class. If {@link #isShell()} is <true> they
	 * don't have to be set. If the class has no children, an empty list is
	 * returned.
	 * 
	 * @return the children
	 */
	@JsonIgnore
	public Set<C> getChildren() {
		if (_children == null || isMarshalling) {
			return null;
		}
		children = new HashSet<C>();
		children.addAll((Collection<? extends C>) _children);

		return children;
	}

	/**
	 * Sets the siblings of this class.
	 * 
	 * @param children
	 *            the children to set
	 */
	public void setChildren(Set<C> children) {
		this.children = children;
	}

	/**
	 * Adds a child to the list of children of this class.
	 * 
	 * @param child
	 */
	public void addChild(C child) {
		if (children == null) {
			children = new HashSet<C>();
		}
		if (_children == null) {
			_children = new HashSet<JsonCls>();
		}
		children.add(child);
		_children.add(child);
	}

	/**
	 * Get the superclasses of this class. If {@link #isShell()} is
	 * <code>true</code> the parents don't have to be set.
	 * 
	 * @return the parents
	 */
	@JsonIgnore
	public Set<C> getParents() {
		if (_parents == null || isMarshalling) {
			return null;
		}
		parents = new HashSet<C>();
		parents.addAll((Collection<? extends C>) _parents);

		return parents;
	}

	/**
	 * Sets the superclasses of this class.
	 * 
	 * @param parents
	 *            the parents to set
	 */
	public void setParents(Set<C> parents) {
		this.parents = parents;
	}

	/**
	 * Adds a class to the list of parents of this class.
	 * 
	 * @param parent
	 */
	public void addParent(C parent) {
		if (parents == null) {
			parents = new HashSet<C>();
		}
		if (_parents == null) {
			_parents = new HashSet<JsonCls>();
		}
		parents.add(parent);
		_parents.add(parent);
	}

	public Set<JsonAnnotation> getAnnotations() {
		return annotations;
	}

	public void setAnnotations(Set<JsonAnnotation> annotations) {
		this.annotations = annotations;
	}

	public void addAnnotation(JsonAnnotation ja) {
		if (annotations == null) {
			annotations = new HashSet<JsonAnnotation>();
		}
		annotations.add(ja);
	}

	public Set<JsonObjectPropertyExpression> getProperties() {
		return restrictions;
	}

	public void setProperties(Set<JsonObjectPropertyExpression> properties) {
		this.restrictions = properties;
	}

	public void addRestriction(JsonObjectPropertyExpression jp) {
		if (restrictions == null) {
			restrictions = new HashSet<JsonObjectPropertyExpression>();
		}
		restrictions.add(jp);
	}

	@XmlTransient
	@JsonIgnore
	public boolean isIsMarshalling() {
		return isMarshalling;
	}

	public void setIsMarshalling(boolean isMarshalling) {
		this.isMarshalling = isMarshalling;
	}

	// public boolean isIsLazy() {
	// return isLazy;
	// }
	//
	// public void setIsLazy(boolean isLazy) {
	// this.isLazy = isLazy;
	// }
	@Override
	public String toString() {
		return "JsonCls: " + name;
	}

	@JsonProperty("children")
	public Set<JsonCls> getRawChildren() {
		return _children;
	}

	@JsonProperty("parents")
	public Set<JsonCls> getRawParents() {
		return _parents;
	}
}

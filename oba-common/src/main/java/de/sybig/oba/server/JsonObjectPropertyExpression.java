package de.sybig.oba.server;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlType
@XmlRootElement
public class JsonObjectPropertyExpression<C extends JsonCls> {
	private JsonObjectProperty property;
	private String propertyName;
	private String propertyNamespace;

	private C target;

	public JsonObjectProperty getProperty() {
		return property;
	}

	public void setProperty(JsonObjectProperty property) {
		this.property = property;
	}

	public C getTarget() {
		return target;
	}

	public void setTarget(C target) {
		this.target = target;
	}

}

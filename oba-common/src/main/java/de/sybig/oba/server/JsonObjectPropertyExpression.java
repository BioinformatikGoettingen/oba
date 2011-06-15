package de.sybig.oba.server;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlType
@XmlRootElement
public class JsonObjectPropertyExpression {
	private JsonObjectProperty property;
	private String propertyName;
	private String propertyNamespace;

	private JsonCls target;

	public JsonObjectProperty getProperty() {
		return property;
	}

	public void setProperty(JsonObjectProperty property) {
		this.property = property;
	}

	// public String getPropertyName() {
	// return propertyName;
	// }
	//
	// public void setPropertyName(String propertyName) {
	// this.propertyName = propertyName;
	// }
	//
	// public String getPropertyNamespace() {
	// return propertyNamespace;
	// }
	//
	// public void setPropertyNamespace(String propertyNamespace) {
	// this.propertyNamespace = propertyNamespace;
	// }

	public JsonCls getTarget() {
		return target;
	}

	public void setTarget(JsonCls target) {
		this.target = target;
	}

}

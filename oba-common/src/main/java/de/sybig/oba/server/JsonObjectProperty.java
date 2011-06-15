package de.sybig.oba.server;

import java.util.HashSet;
import java.util.Set;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlType
@XmlRootElement
public class JsonObjectProperty extends JsonEntity {

	private Set<JsonAnnotation> annotations;
	private Set<JsonObjectProperty> superProperties;
	private Set<JsonObjectProperty> subProperties;

	private JsonObjectProperty inverse;

	public Set<JsonAnnotation> getAnnotations() {
		return annotations;
	}

	public void setAnnotations(Set<JsonAnnotation> annotations) {
		this.annotations = annotations;
	}

	public Set<JsonObjectProperty> getSuperProperties() {
		return superProperties;
	}

	public void setSuperProperties(Set<JsonObjectProperty> superProperties) {
		this.superProperties = superProperties;
	}

	public void addSuperProperty(JsonObjectProperty superProperty) {
		if (superProperties == null) {
			superProperties = new HashSet<JsonObjectProperty>();
		}
		superProperties.add(superProperty);
	}

	public Set<JsonObjectProperty> getSubProperties() {
		return subProperties;
	}

	public void setSubProperties(Set<JsonObjectProperty> subProperties) {
		this.subProperties = subProperties;
	}

	public void addSubProperty(JsonObjectProperty subProperty) {
		if (subProperties == null) {
			subProperties = new HashSet<JsonObjectProperty>();
		}
		subProperties.add(subProperty);
	}

	public JsonObjectProperty getInverse() {
		return inverse;
	}

	public void setInverse(JsonObjectProperty inverse) {
		this.inverse = inverse;
	}

}

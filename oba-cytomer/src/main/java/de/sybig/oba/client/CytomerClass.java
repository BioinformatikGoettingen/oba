package de.sybig.oba.client;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import de.sybig.oba.server.JsonCls;

@XmlType
@XmlRootElement
public class CytomerClass extends OntologyClass {

	public CytomerClass() {
		super();
	}

	public CytomerClass(JsonCls c) {
		super(c);
	}

	public String getACC() {
		return getSingleAnnotationValue("ACC"); // http://protege.stanford.edu/plugins/owl/protege
	}

	public String toString() {
		return name;
	}
}

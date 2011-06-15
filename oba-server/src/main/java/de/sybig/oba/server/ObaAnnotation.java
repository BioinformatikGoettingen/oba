package de.sybig.oba.server;

import org.semanticweb.owlapi.model.IRI;

public class ObaAnnotation {

	private IRI iri;
	private String language;
	private String value;

	public IRI getIri() {
		return iri;
	}

	public void setIri(IRI iri) {
		this.iri = iri;
	}

	public String getLanguage() {
		return language;
	}

	public void setLanguage(String language) {
		this.language = language;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public String getName() {
		return (iri != null) ? iri.getFragment() : null;

	}

}

package de.sybig.oba.client;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import de.sybig.oba.server.JsonAnnotation;
import de.sybig.oba.server.JsonCls;

@XmlType
@XmlRootElement
public class CytomerClass extends OntologyClass {

	static Map<String, String> supportedLanguages;

	public CytomerClass() {
		super();
	}

	public CytomerClass(JsonCls c) {
		super(c);
	}

	public String getACC() {
		return getSingleAnnotationValue("ACC"); // http://protege.stanford.edu/plugins/owl/protege
	}

	public Set<JsonAnnotation> getLabel() {
		Set<JsonAnnotation> labels = new HashSet<JsonAnnotation>();
		for (String language : getSupportedLanguages().keySet()) {
			JsonAnnotation annotation = new JsonAnnotation();
			annotation.setLanguage(language);
			annotation.setName("label");
			annotation.setValue(getLabel(language));
		}
		return labels;
	}

	public String getLabel(String language) {
		if (language.equals("en")) {
			return getSingleAnnotationValue("nameEnglish");
		} else if (language.equals("de")) {
			return getSingleAnnotationValue("nameGerman");
		} else if (language.equals("med")) {
			return getSingleAnnotationValue("nameMedicine");
		}
		throw new IllegalArgumentException(String.format(
				"The language %s is not supported for the label", language));
	}

	public Set<JsonAnnotation> getDefinition() {
		Set<JsonAnnotation> defs = new HashSet<JsonAnnotation>();
		for (String language : getSupportedLanguages().keySet()) {
			JsonAnnotation annotation = new JsonAnnotation();
			annotation.setName("definition");
			annotation.setLanguage("language");
			annotation.setValue(getDefinition("language"));
			defs.add(annotation);
		}
		return defs;
	}

	public String getDefinition(String language) {
		if (language.equals("en")) {
			return getSingleAnnotationValue("definitionEnglish");
		} else if (language.equals("de")) {
			return getSingleAnnotationValue("definitionGerman");
		}
		throw new IllegalArgumentException(
				String.format(
						"The language %s is not supported for the definition",
						language));
	}

	public String toString() {
		return name;
	}

	public static Map<String, String> getSupportedLanguages() {
		if (supportedLanguages == null) {
			supportedLanguages = new HashMap<String, String>();
			supportedLanguages.put("en", "English");
			supportedLanguages.put("de", "German");
		}
		return supportedLanguages;
	}

	@Override
	protected OntologyClass createNewOntologyClass(JsonCls c) {
		return new CytomerClass(c);
	}
}

package de.sybig.oba.client;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import de.sybig.oba.server.JsonAnnotation;
import de.sybig.oba.server.JsonCls;

@XmlType
@XmlRootElement
public class CytomerClass extends OntologyClass {

    private static Map<String, String> supportedLanguages;
    private static HashMap<String, String> supportedLanguages4Names;

    public CytomerClass() {
        super();
    }

    public CytomerClass(JsonCls c) {
        super(c);
    }

    public void setChildren(List<JsonCls> o) {
        System.out.println("setting children " + o);
    }

    /**
     * Get the ACC or ID of the class. Every class should have an unique ID
     * which does not change over time.
     *
     * @return
     */
    public String getACC() {
        return getSingleAnnotationValue("ACC"); // http://protege.stanford.edu/plugins/owl/protege
    }

    @Deprecated
    public Set<JsonAnnotation> getLabel() {
        return getLabels();
    }

    /**
     * Get a set of annotations for the label in each supported (
     * {@link #getSupportedLanguages()}) language. If the class has no label
     * annotated for a supported language the set does not contain an annotation
     * for this language.
     *
     * @return A set of annotations for the label in each available language.
     */
    public Set<JsonAnnotation> getLabels() {
        return getAnnotationValues("label");
//		Set<JsonAnnotation> labels = new HashSet<JsonAnnotation>();
//		for (String language : getSupportedLanguages().keySet()) {
//			JsonAnnotation annotation = new JsonAnnotation();
//			annotation.setValue(getLabel(language));
//			if (annotation.getValue() == null) {
//				continue;
//			}
//			annotation.setLanguage(language);
//			annotation.setName("label");
//		}
//		return labels;
    }

    /**
     * Get the label in the required language. The supported languages are "en",
     * "de" and "med". for other languages an {@link IllegalArgumentException}
     * is thrown. If the language is supported but the class has no label in
     * this language annotated
     * <code>null</code> is returned.
     *
     * @param language
     * @return
     */
    public String getLabel(String language) {
        Set<JsonAnnotation> labels = getAnnotationValues("label");
        for (JsonAnnotation label : labels) {
            if (language.equals(label.getLanguage())) {
                return label.getValue();
            }
        }
        return null;
    }
//	public String getLabel(String language) {
//		if (language.equals("en")) {
//			return getSingleAnnotationValue("nameEnglish");
//		} else if (language.equals("de")) {
//			return getSingleAnnotationValue("nameGerman");
//		} else if (language.equals("med")) {
//			return getSingleAnnotationValue("nameMedicine");
//		}
//		throw new IllegalArgumentException(String.format(
//				"The language %s is not supported for the label", language));
//	}

    @Deprecated
    public Set<JsonAnnotation> getDefinition() {
        return getComments();
    }

    /**
     * Get a set of annotations for the definition in each supported (
     * {@link #getSupportedLanguages()}) language. If the class has no
     * definition annotated for a supported language the set does not contain an
     * annotation for this language.
     *
     * @return A set of annotations for the definition in each available
     * language.
     */
    public Set<JsonAnnotation> getComments() {
        return getAnnotationValues("comment");
//		Set<JsonAnnotation> defs = new HashSet<JsonAnnotation>();
//		for (String language : getSupportedLanguages().keySet()) {
//			JsonAnnotation annotation = new JsonAnnotation();
//			annotation.setValue(getComments("language"));
//			if (annotation.getValue() == null) {
//				continue;
//			}
//			annotation.setName("definition");
//			annotation.setLanguage("language");
//			defs.add(annotation);
//		}
//		return defs;
    }

    /**
     * Get the definition in the required language. The supported languages are
     * "en" and "de", for other languages an {@link IllegalArgumentException} is
     * thrown. Medicine is not a supported language for the definition. If the
     * language is supported but the class has no label in this language
     * annotated
     * <code>null</code> is returned.
     *
     * @param language
     * @return
     */
    public String getDefinition(String language) {
        for (JsonAnnotation def : getComments()) {
            if (language.equals(def.getLanguage())) {
                return def.getValue();
            }
        }
        return null;
    }
//	public String getComments(String language) {
//		if (language.equals("en")) {
//			return getSingleAnnotationValue("definitionEnglish");
//		} else if (language.equals("de")) {
//			return getSingleAnnotationValue("definitionGerman");
//		}
//		throw new IllegalArgumentException(
//				String.format(
//						"The language %s is not supported for the definition",
//						language));
//	}

    /**
     * Get the synonyms in the required language. The supported languages are
     * "en", "de" and "med", for other languages an
     * {@link IllegalArgumentException} is thrown. If the language is supported
     * but the class has no synonyms in this language annotated
     * <code>null</code> is returned.
     *
     * @param language
     * @return
     */
    public String getSynonyms(String language) {
        if (language.equals("en")) {
            return getSingleAnnotationValue("nameEnglishSynonym");
        } else if (language.equals("de")) {
            return getSingleAnnotationValue("nameGermanSynonym");
        } else if (language.equals("med")) {
            return getSingleAnnotationValue("nameMedicineSynonym");
        }
        throw new IllegalArgumentException(String.format(
                "The language %s is not supported for the label", language));
    }

    /**
     * Get a map of supported languages for all annotations.
     *
     * @return
     */
    public static Map<String, String> getSupportedLanguages() {
        if (supportedLanguages == null) {
            supportedLanguages = new HashMap<String, String>();
            supportedLanguages.put("en", "English");
            supportedLanguages.put("de", "German");
        }
        return supportedLanguages;
    }

    /**
     * Get a map of supported languages for the label and synonyms.
     *
     * @return
     */
    public static Map<String, String> getSupportedLanguagesForNames() {
        if (supportedLanguages4Names == null) {
            supportedLanguages4Names = new HashMap<String, String>();
            supportedLanguages4Names.putAll(getSupportedLanguages());
            supportedLanguages4Names.put("med", "Medicine");
        }
        return supportedLanguages4Names;
    }

    public String toString() {
        return name;
    }

    @Override
    protected OntologyClass createNewOntologyClass(JsonCls c) {
        return new CytomerClass(c);
    }
}

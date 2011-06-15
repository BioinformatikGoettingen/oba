package de.sybig.oba.server;

import org.semanticweb.owlapi.model.OWLObjectProperty;

/**
 * A class to handle object property expressions inside of the oba server. A
 * ObaObjectPropertyExpression bundles together the object property and the
 * target class. For the serialization with JSON
 * {@link JsonObjectPropertyExpression} is used.
 * 
 * @author juergen.doenitz@bioinf.med.uni-goettingen.de
 * 
 */
public class ObaObjectPropertyExpression {
	private OWLObjectProperty restriction;
	private ObaClass target;

	public OWLObjectProperty getRestriction() {
		return restriction;
	}

	public void setRestriction(OWLObjectProperty restriction) {
		this.restriction = restriction;
	}

	public ObaClass getTarget() {
		return target;
	}

	public void setTarget(ObaClass target) {
		this.target = target;
	}

	public String toString() {
		return String.format("Object property %s to %s", restriction, target);
	}
}

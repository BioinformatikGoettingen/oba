/*
 * Created on Jul 28, 2010
 *
 */
package de.sybig.oba.server;

/**
 * 
 * @author juergen.doenitz@bioinf.med.uni-goettingen.de
 * 
 */
public interface OntologyFunction {

	/**
	 * Sets the ontology the implementing ontology function class will work on.
	 * 
	 * @param obaOntology
	 */
	public void setOntology(ObaOntology obaOntology);

	/**
	 * Returns a list of functions provided by this function class
	 * 
	 * @return
	 */
	public String getRoot();
}

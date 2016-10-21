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

    public String getVersion();

    /**
     * Resets the function class. All cached values should be reset and the
     * ontology should be refetched from the
     * {@link de.sybig.oba.server.OntologyHandler}. Ontologies can be removed
     * from the OBA server on runtime, therefore the function class should reset
     * all references to the ontolog.
     *
     */
    public void reset();
}

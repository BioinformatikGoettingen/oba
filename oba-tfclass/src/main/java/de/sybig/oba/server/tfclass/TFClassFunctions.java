package de.sybig.oba.server.tfclass;

import de.sybig.oba.server.ObaClass;
import de.sybig.oba.server.ObaObjectPropertyExpression;
import de.sybig.oba.server.OntologyFunctions;
import de.sybig.oba.server.OntologyHelper;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

/**
 *
 * @author juegen.doenitz@bioinf.med.uni-goettingen.de
 */
//@Path("/")
public class TFClassFunctions extends OntologyFunctions {

    private static final String CONTAINS_RELATION = "contains";
    private static final String TAXON_ATTRIBUTE = "ncbi_taxon";
    private static final String PARTOF_RELATION = "isPartOf";

    @GET
    @Path("/downstreamSpecies/{cls}")
    @Produces(ALL_TYPES)
    public Set<ObaClass> getDownstreamSpecies(@PathParam("cls") String cls,
            @QueryParam("ns") String ns) {
        ObaClass startClass = ontology.getOntologyClass(cls, ns);

        Set<ObaClass> taxons = getDownstreamSpecies(startClass);
        System.out.println("# of taxons " + taxons.size());
        return taxons;
    }

    private Set<ObaClass> getDownstreamSpecies(ObaClass cls) {
        Set<ObaClass> taxons = new HashSet<>();
        Set<ObaObjectPropertyExpression> restrictions = OntologyHelper.getObjectRestrictions(cls, ontology.getOntology());
        for (ObaObjectPropertyExpression r : restrictions) {
            if (CONTAINS_RELATION.equals(r.getRestriction().getIRI().getFragment())) {
                ObaClass target = r.getTarget();
                taxons.addAll(getSpeciesOf(target));
            }
        }
        for (ObaClass child : OntologyHelper.getChildren(cls)) {
            taxons.addAll(getDownstreamSpecies(child));
        }
        return taxons;
    }

    private Set<ObaClass> getSpeciesOf(ObaClass target) {
        Set<ObaClass> parents = OntologyHelper.getParents(target);
        Set<ObaClass> taxons = new HashSet<>();
        for (ObaClass parent : parents) {
            Set<ObaObjectPropertyExpression> restrictions = OntologyHelper.getObjectRestrictions(parent, ontology.getOntology());
            for (ObaObjectPropertyExpression r : restrictions) {
                if (PARTOF_RELATION.equals(r.getRestriction().getIRI().getFragment())) {
                    taxons.add(r.getTarget());
                }
            }
        }
        return taxons;
    }
}

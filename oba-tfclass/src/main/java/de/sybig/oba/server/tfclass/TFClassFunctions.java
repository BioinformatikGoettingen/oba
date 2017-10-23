package de.sybig.oba.server.tfclass;

import de.sybig.oba.server.ObaClass;
import de.sybig.oba.server.ObaObjectPropertyExpression;
import de.sybig.oba.server.OntologyFunctions;
import de.sybig.oba.server.OntologyHelper;
import java.util.HashMap;
import java.util.HashSet;
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
    private static final String BELONGSTO_RESTRICTION = "belongs_to";
    private HashMap<ObaClass, Set<ObaClass>> hasPart;

    @GET
    @Path("/speciesDownstream/{cls}")
    @Produces(ALL_TYPES)
    public Set<ObaClass> getDownstreamSpecies(@PathParam("cls") String cls,
            @QueryParam("ns") String ns) {
        ObaClass startClass = ontology.getOntologyClass(cls, ns);

        Set<ObaClass> taxons = getDownstreamSpecies(startClass);
        return taxons;
    }
 @GET
    @Path("/generaDownstream/{cls}")
    @Produces(ALL_TYPES)
    public Set<ObaClass> getGeneraDownstream(@PathParam("cls") String cls,
            @QueryParam("ns") String ns) {
        ObaClass startClass = ontology.getOntologyClass(cls, ns);
         Set<ObaClass> genera = getDownstreamGenera(startClass);
        return genera;
    }
    @GET
    @Path("/factorsForSpecies/{cls}")
    @Produces(ALL_TYPES)
    public Set<ObaClass> getFactorsForSpecies(@PathParam("cls") String cls,
            @QueryParam("ns") String ns) {
        HashSet<ObaClass> out = new HashSet<ObaClass>();
        ObaClass startClass = ontology.getOntologyClass(cls, ns);
        Set<ObaClass> proteinGroups = getHasPart(startClass);
        for (ObaClass proteinGroup : proteinGroups) {
            out.addAll(OntologyHelper.getChildren(proteinGroup));
        }
        return out;
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

    private Set<ObaClass>getDownstreamGenera(ObaClass cls){
        Set<ObaClass> genera = new HashSet<>();
        Set<ObaClass> children = OntologyHelper.getChildren(cls);
        if (children == null || children.size() < 1){
            HashSet<ObaClass> self = new HashSet<>();
            self.add(cls);
            return self;
        }
        for (ObaClass child : children){
            genera.addAll(getDownstreamGenera(child));
        }
        return genera;
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

    private Set<ObaClass> getHasPart(ObaClass cls) {
        if (hasPart == null) {
            initHasPartMap();
        }
        return hasPart.get(cls);
    }

    private void initHasPartMap() {
        hasPart = new HashMap<ObaClass, Set<ObaClass>>();
        ObaClass root = ontology.getRoot();
        for (ObaClass child : OntologyHelper.getChildren(root)) {
            findHasPart(child);
        }

    }

    private void findHasPart(ObaClass cls) {
        Set<ObaObjectPropertyExpression> partOfRestrictions = getHasPartRestrictions(cls);
        for (ObaObjectPropertyExpression ope : partOfRestrictions) {
            ObaClass target = ope.getTarget();
            if (!hasPart.containsKey(target)) {
                hasPart.put(target, new HashSet<ObaClass>());
            }
            hasPart.get(target).add(cls);
        }

        for (ObaClass child : OntologyHelper.getChildren(cls)) {
            if (cls.equals(child)) {

                continue;
            }
            findHasPart(child);
        }
    }

    private Set<ObaObjectPropertyExpression> getHasPartRestrictions(ObaClass cls) {
        HashSet<ObaObjectPropertyExpression> partOf = new HashSet<ObaObjectPropertyExpression>();
        for (ObaObjectPropertyExpression ope : OntologyHelper.getObjectRestrictions(cls)) {
            if (!ope.getRestriction().getIRI().getFragment().equals(PARTOF_RELATION)) {
                continue;
            }
            partOf.add(ope);
        }
        return partOf;
    }

}

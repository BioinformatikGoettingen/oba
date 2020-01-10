/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.sybig.oba.server.IC_plugin;

import de.sybig.oba.server.ObaClass;
import org.opencompare.hac.experiment.DissimilarityMeasure;
import org.opencompare.hac.experiment.Experiment;
/**
 *
 * @author kraynrads
 */
public interface OntologyTermExperimentInterface extends Experiment{
    public ObaClass getAnnotation(int index);
}

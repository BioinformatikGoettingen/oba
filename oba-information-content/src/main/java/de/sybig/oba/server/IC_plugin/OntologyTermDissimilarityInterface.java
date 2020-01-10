/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.sybig.oba.server.IC_plugin;

import org.opencompare.hac.experiment.DissimilarityMeasure;
import org.opencompare.hac.experiment.Experiment;

/**
 *
 * @author kconrads
 */
interface OntologyTermDissimilarityInterface <T extends Experiment>{
  public double computeDissimilarity(T arg0, int arg1, int arg2);   
}

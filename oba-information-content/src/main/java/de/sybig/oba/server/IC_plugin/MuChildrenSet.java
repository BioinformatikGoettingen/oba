/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.sybig.oba.server.IC_plugin;


/**
 *
 * @author kraynrads
 */
public class MuChildrenSet {
    private double mu;
    private double children;

    public MuChildrenSet(double mu, double children) {
        setMu(mu);
        setChildren(children);
    }

    public void setMu(double mu) {
        this.mu = mu;
    }

    public double getMu() {
        return mu;
    }

    public void setChildren(double children) {
        this.children = children;
    }

    public double getChildren() {
        return children;
    }

    Double getTermValue() {
       return mu/ children;
    }
    
    
    
}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dk.sdu.kpm.results;

/**
 *
 * @author nalcaraz
 */
public class PercentageParameters {
    int K;
    
    double lPer;

    public PercentageParameters(int K, double lPer) {
        this.K = K;
        this.lPer = lPer;
    }
    
    public PercentageParameters(PercentageParameters pp) {
        this.K = pp.K;
        this.lPer = pp.lPer;
    }

    public int getK() {
        return K;
    }

    public double getlPer() {
        return lPer;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 67 * hash + this.K;
        hash = 67 * hash + (int) (Double.doubleToLongBits(this.lPer) ^ (Double.doubleToLongBits(this.lPer) >>> 32));
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final PercentageParameters other = (PercentageParameters) obj;
        if (this.K != other.K) {
            return false;
        }
        if (Double.doubleToLongBits(this.lPer) != Double.doubleToLongBits(other.lPer)) {
            return false;
        }
        return true;
    }
    
    
}

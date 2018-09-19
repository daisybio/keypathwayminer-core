/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package dk.sdu.kpm.graph;

import dk.sdu.kpm.Heuristic;
import dk.sdu.kpm.utils.Comparator;
import dk.sdu.kpm.utils.Comparison;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author nalcaraz
 */
public class GeneNode implements Comparable<GeneNode>, Serializable {

    public static final char UPREGULATED = '+';
    
    public static final char DOWNREGULATED = '-';
    
    public static final char NODIFFERENCE = '*';
    
    public double pheromone = 0.5;
    
    private int lastIterationPheromoneUpdated = 0;
    
    public String nodeId;
    
    public String symbol;
        
    public Map<String, char[]> differenceMap;
    
    public Map<String, double[]> differenceIntMap;
    
    public Map<String, Integer> numUpExpressedCasesMap;
    
    public Map<String, Integer> numDownExpressedCasesMap;
    
    public Map<String, Integer> numNoDiffExpressedCasesMap;
    
    public Map<String, Integer> numCasesMap;
    
    public int totalUpCases;
    
    public int totalDownCases;
    
    public int totalNoDiffCases;
    
    public int totalCases;
    
    private double averageNeighborExpression;
    
    private boolean isValid;

    public double getPvalue() {
        return pvalue;
    }

    //TODO: change to map for multiple datasets
    private  double pvalue;

    public  HashMap<String, Double> getAveragePvalue() {
        return averagePvalue;
    }

    public boolean use_double;

    // Average p-value is calculated per gene from the samples
    // Must not actually be p-values
    // For each Dataset an separate average is calculated
    public volatile HashMap<String, Double> averagePvalue = new HashMap<String, Double>();
    
    public GeneNode(String nodeId,
                    String symbol,
                    Map<String, double[]> differenceIntMap,
                    double pvalue,
                    boolean use_double) {
        this.nodeId = nodeId;
        this.symbol = symbol;
        isValid = false;
        this.pvalue = pvalue;
        this.use_double = use_double;
        this.differenceIntMap = differenceIntMap;
        computeDifferenceMap();
    }
    
    public GeneNode(GeneNode n){
    	this.pheromone = n.pheromone;
    	this.nodeId = n.nodeId;
    	this.symbol = n.symbol;
    	this.isValid = n.isValid;
    	this.lastIterationPheromoneUpdated = n.lastIterationPheromoneUpdated;
    	this.totalUpCases = n.totalUpCases;
    	this.totalDownCases = n.totalDownCases;
    	this.totalNoDiffCases = n.totalNoDiffCases;
    	this.totalCases = n.totalCases;
    	this.averageNeighborExpression = n.averageNeighborExpression;
    	this.totalDownCases = n.totalDownCases;
    	this.totalDownCases = n.totalDownCases;
    	this.differenceMap = new HashMap<String, char[]>(n.differenceMap);
    	this.differenceIntMap = new HashMap<String, double[]>(n.differenceIntMap);
    	this.numUpExpressedCasesMap = new HashMap<String, Integer>(n.numUpExpressedCasesMap);
    	this.numDownExpressedCasesMap = new HashMap<String, Integer>(n.numDownExpressedCasesMap);
    	this.numNoDiffExpressedCasesMap = new HashMap<String, Integer>(n.numNoDiffExpressedCasesMap);
    	this.numCasesMap = new HashMap<String, Integer>(n.numCasesMap);
    	//this.pvalue = new HashMap<String, Double>(n.pvalue);
        this.pvalue = n.pvalue;
    	this.averagePvalue = new HashMap<String, Double>(n.averagePvalue);
    	this.use_double = n.use_double;
    }
    
    private void computeDifferenceMap() {
        differenceMap = new HashMap<String, char[]>();
        numUpExpressedCasesMap = new HashMap<String, Integer>();
        numDownExpressedCasesMap = new HashMap<String, Integer>();
        numNoDiffExpressedCasesMap = new HashMap<String, Integer>();
        numCasesMap = new HashMap<String, Integer>();
        totalUpCases = 0;
        totalDownCases = 0;
        totalNoDiffCases = 0;
        totalCases = 0;

        for (String expId : differenceIntMap.keySet()) {
            double[] differArray = differenceIntMap.get(expId);
            int ncases = differArray.length;
            totalCases += ncases;
            int numUp = 0;
            int numDown = 0;
            int numDiff = 0;

            numCasesMap.put(expId, ncases);

            // Here UP or DOWNREGULATIOn is checked
            if(!use_double) {
                char[] diffarray = new char[ncases];
                for (int i = 0; i < ncases; i++) {
                    double option = differArray[i];
                    if (Math.abs(option-1.0)<=0.0000000001) {
                        diffarray[i] = UPREGULATED;
                        numUp++;
                        totalUpCases++;
                    } else if (Math.abs(option+1.0)<=0.0000000001) {
                        diffarray[i] = DOWNREGULATED;
                        numDown++;
                        totalDownCases++;
                    } else {
                        diffarray[i] = NODIFFERENCE;
                        numDiff++;
                        totalNoDiffCases++;
                    }
                }
                differenceMap.put(expId, diffarray);
                numUpExpressedCasesMap.put(expId, numUp);
                numDownExpressedCasesMap.put(expId, numDown);

            }
            else{
                double sum = 0;
                for (int i = 0; i<differArray.length; i++){
                    sum+=differArray[i];
                    // TODO: adapt to accept different thresholds and different comparators
                    if(!Comparison.evalate(differArray[i],0.05, Comparator.LET)){
                        // numDiff no Differential expression = insignficant p-value
                        numDiff++;
                    }
                }
                double avgDouble = sum/(differArray.length)*1.0;
                this.averagePvalue.put(expId, avgDouble);
                differenceMap.put(expId, null);
                numUpExpressedCasesMap.put(expId, -1);
                numDownExpressedCasesMap.put(expId, -1);
            }
            numNoDiffExpressedCasesMap.put(expId, numDiff);
        }
    }

    public boolean isValid() {
        return isValid;
    }

    public void setIsValid(boolean isValid) {
        this.isValid = isValid;
    }

    public char[] getDifferenceArray(String expId) {
        return differenceMap.get(expId);
    }
    public Map<String, char[]> getDifferenceMap() {
        return differenceMap;
    }

    public Map<String, double[]> getDifferenceIntMap() {
        return differenceIntMap;
    }
    public void setDifferenceIntMap(Map<String, double[]> differenceIntMap) {
        this.differenceIntMap = differenceIntMap;
        computeDifferenceMap();
    }
    public Map<String, Integer> getNumDownExpressedCasesMap() {
        return numDownExpressedCasesMap;
    }

    public Map<String, Integer> getNumNoDiffExpressedCasesMap() {
        return numNoDiffExpressedCasesMap;
    }

    public Map<String, Integer> getNumUpExpressedCasesMap() {
        return numUpExpressedCasesMap;
    }

    public Map<String, Integer> getNumCasesMap() {
        return numCasesMap;
    }
        
    public int getNumCases(String expId) {
        return numCasesMap.get(expId);
    }
    
    public int getNumDownExpressedCases(String expId) {
        return numDownExpressedCasesMap.get(expId);
    }
    
    public int getNumUpExpressedCases(String expId) {
        return numUpExpressedCasesMap.get(expId);
    }
    
    public int getNumNoDiffExpressedCases(String expId) {
        return numNoDiffExpressedCasesMap.get(expId);
    }
    
    public int getNumDiffExpressedCases(String expId) {
        return numUpExpressedCasesMap.get(expId) + numDownExpressedCasesMap.get(expId);
    }
    
    public int getTotalCases() {
        return totalCases;
    }

    public int getTotalDownCases() {
        return totalDownCases;
    }

    public int getTotalNoDiffCases() {
        return totalNoDiffCases;
    }

    public int getTotalUpCases() {
        return totalUpCases;
    }
    
    public int getTotalDiffExpressedCases() {
        return totalDownCases + totalUpCases;
    }
    
    public double getAverageExpressedCasesNormalized() {
        double aux = 0.0;
        for (String expId: getDifferenceMap().keySet()) {
            double cases = (double)getNumCases(expId);
            double totalExp = (double)getNumDiffExpressedCases(expId);
            aux += totalExp / cases;
        }
        return aux / (double)getDifferenceMap().size();
    }

    public void setPvalue(double pvalue) {
        this.pvalue = pvalue;
    }

    public int getAverageExpressedCases() {
        double den = 0.0;
        for (String expId: getDifferenceMap().keySet()) {
            double cases = (double)getNumCases(expId);
            double totalNonExp = (double)getNumDiffExpressedCases(expId);
            double aux = cases * totalNonExp;
            den += aux;
        }
        return (int)Math.round(den / (double)getTotalCases());
    }
    public int getAverageNonExpressedCases() {
        double den = 0.0;
        for (String expId: getDifferenceMap().keySet()) {
            double cases = (double)getNumCases(expId);
            double totalNonExp = (double)getNumNoDiffExpressedCases(expId);
            double aux = cases * totalNonExp;
            den += aux;
        }
        return (int)Math.round(den / (double)getTotalCases());
    }

    public double getAverageNonExpressedCasesNormalized() {
        return 1.0 - getAverageExpressedCasesNormalized();
    }
    
    public int getHeuristicValue(Heuristic val) {       
        switch(val) {
            case AVERAGE:
                return getAverageNonExpressedCases();
            case TOTAL:
                return getTotalNoDiffCases();
        }       
        return getAverageNonExpressedCases();               
    }
    

    public String getSymbol() {
        return symbol;
    }

    public String getNodeId() {
        return nodeId;
    }


    public String toString() {
        return symbol;
    }

    public double getAverageNeighborExpression() {
        return averageNeighborExpression;
    }

    public void setAverageNeighborExpression(double averageNeighborExpression) {
        this.averageNeighborExpression = averageNeighborExpression;
    }
    
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((nodeId == null) ? 0 : nodeId.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        GeneNode other = (GeneNode) obj;
        if (nodeId == null) {
            if (other.nodeId != null) {
                return false;
            }
        } else if (!nodeId.equals(other.nodeId)) {
            return false;
            //if the two nodes contain expression data from different datasets they are not equal!
        } else if (!other.differenceIntMap.keySet().containsAll(this.differenceIntMap.keySet())){
            return false;
        }
        return true;
    }

    @Override
    public int compareTo(GeneNode o) {
        int result = -(new Double(getAverageExpressedCases()).compareTo(new Double(o.getAverageExpressedCases())));
        if (result != 0) {
            return result;
        } else {
            return nodeId.compareTo(o.nodeId);
        }
    }

    public double getPheromone() {
        return pheromone;
    }

    public void reset() {
        pheromone = 1.0 / 2.0;
        lastIterationPheromoneUpdated = 0;
    }

    public int getLastIterationPheromoneUpdated() {
        return lastIterationPheromoneUpdated;
    }

    public void setLastIterationPheromoneUpdated(
            int lastIterationPheromoneUpdated) {
        this.lastIterationPheromoneUpdated = lastIterationPheromoneUpdated;
    }

    public void setPheromone(double pheromone, double tau_min) {
        if (pheromone < tau_min || pheromone > 1 - tau_min) {
            throw new IllegalArgumentException("Pheromone out of bounds.");
        }
        this.pheromone = pheromone;
    }
}
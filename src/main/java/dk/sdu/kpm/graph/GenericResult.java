package dk.sdu.kpm.graph;
import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import dk.sdu.kpm.KPMSettings;
import dk.sdu.kpm.algo.glone.Subgraph;

/**
 *
 * @author nalcaraz
 */
public class GenericResult implements Result, Serializable {

    Map<String, GeneNode> visitedNodes;
    
    public GenericResult() {
        visitedNodes = new HashMap<String, GeneNode>();
    }
    
    public GenericResult(Collection<GeneNode> nodes) {
        this();
        for (GeneNode node: nodes) {
            visitedNodes.put(node.getNodeId(), node);
        }
                
    }
    
    public void add(GeneNode node) {
        visitedNodes.put(node.getNodeId(), node);
    }
    @Override
    public Map<String, GeneNode> getVisitedNodes() {
        return visitedNodes;
    }

    @Override
    public double getAverageDiffExpressedCases() {
        double diffExp = 0;
        for (GeneNode node: visitedNodes.values()) {
            diffExp += node.getAverageExpressedCasesNormalized();
        }
        return diffExp;
    }

    private double log2(double d) {
        return Math.log(d) / Math.log(2.0);
    }
    
    public double getInformationGainExpressed2(Map<String, Integer> num_cases_map) {


        if (visitedNodes.size() < 2) {
            return 0.0;
        }
        int alphabetSize = 2;
        double bits = log2((double) alphabetSize);
        int nDatasets = num_cases_map.size();
        double avg = 0.0;
        for (String expId : num_cases_map.keySet()) {
            int nCases = num_cases_map.get(expId);
            int[][] frequencies = new int[alphabetSize][nCases];
            
                for (GeneNode node : visitedNodes.values()) {
            
                    char[] expressionArray = node.getDifferenceArray(expId);
                    for (int j = 0; j < expressionArray.length; j++) {
                        char symbol = expressionArray[j];
                        if (symbol == GeneNode.NODIFFERENCE) {
                            frequencies[0][j]++;
                        } else {
                            frequencies[1][j]++;
                        }
                    }
                }
            
            double totalColumnInfo = 0.0;
            for (int j = 0; j < nCases; j++) {
                double columnInfo = 0.0;
                for (int i = 0; i < alphabetSize; i++) {
                    if (frequencies[i][j] > 0) {
                        double aux = (double) frequencies[i][j] / (double) visitedNodes.size();
                        columnInfo += (aux * log2(aux));
                    }
                }
                columnInfo += bits;
                totalColumnInfo += columnInfo;
            }
            avg += totalColumnInfo / (double) nCases;
        }

        return avg / (double) nDatasets;
    }

    @Override
    public double getInformationGainExpressed() {
        LinkedList<GeneNode> nodeList = new LinkedList<GeneNode>(
                getVisitedNodes().values());

        if (nodeList.size() < 2) {
            return 0.0;
        }
        int alphabetSize = 2;

        double avg = 0.0;
        double studies = 0.0;
        GeneNode aNode = nodeList.getFirst();

        for (String expId : aNode.getDifferenceMap().keySet()) {
            studies++;
            int nCases = aNode.getNumCases(expId);
            int[][] frequencies = new int[alphabetSize][nCases];
            for (GeneNode node : nodeList) {
                char[] expressionArray = node.getDifferenceArray(expId);
                for (int j = 0; j < expressionArray.length; j++) {
                    char symbol = expressionArray[j];
                    if (symbol == GeneNode.NODIFFERENCE) {
                        frequencies[0][j]++;
                    } else {
                        frequencies[1][j]++;
                    }
                }
            }

            double[] informationVector = new double[nCases];
            for (int j = 0; j < nCases; j++) {
                double sum = 0.0;
                for (int alph = 0; alph < alphabetSize; alph++) {
                    if (frequencies[alph][j] > 0) {
                        double aux = (double) nodeList.size();
                        sum += ((double) frequencies[alph][j] / aux)
                                * (Math.log(frequencies[alph][j] / aux) / Math.log(2.0));
                    }
                }
                informationVector[j] = (Math.log(alphabetSize) / Math.log(2)) + sum;
            }
            double averageInformation = 0.0;
            for (int j = 0; j < nCases; j++) {
                averageInformation += informationVector[j];
            }

            avg += (averageInformation / (double) nCases);
        }

        return avg / studies;
    }

    @Override
    public int getFitness() {
        return visitedNodes.size();
    }

    @Override
    public int getInstances() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void setInstances(int instances) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int getNumExceptionNodes() {
        int exc = 0;
        for (GeneNode node: visitedNodes.values()) {
            if (!node.isValid()) {
                exc++;
            }
        }
        return exc;
    }

    @Override
    public int getNonDifferentiallyExpressedCases() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void flagExceptionNodes() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean haveOverlap(Object o) {
        Set<String> currentSet = getVisitedNodes().keySet();
        Set<String> compareSet = ((Subgraph) o).getVisitedNodes().keySet();
        if (currentSet.size() <= compareSet.size()) {
            for (String nodeId : currentSet) {
                if (compareSet.contains(nodeId)) {
                    return true;
                }
            }
        } else {
            for (String nodeId : compareSet) {
                if (currentSet.contains(nodeId)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public int compareTo(Result o) {
        return -(new Integer(getFitness())).compareTo(o.getFitness());
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 53 * hash + (this.visitedNodes != null ? this.visitedNodes.hashCode() : 0);
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
        final GenericResult other = (GenericResult) obj;
        if ((!this.visitedNodes.keySet().containsAll(other.getVisitedNodes().keySet()))
                || (!other.visitedNodes.keySet().containsAll(this.getVisitedNodes().keySet()))) {
            return false;
        }
        return true;
    }
    
}


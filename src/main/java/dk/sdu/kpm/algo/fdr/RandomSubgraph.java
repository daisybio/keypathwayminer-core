package dk.sdu.kpm.algo.fdr;

import dk.sdu.kpm.graph.GeneEdge;
import dk.sdu.kpm.graph.GeneNode;
import dk.sdu.kpm.graph.KPMGraph;
import edu.uci.ics.jung.algorithms.filters.Filter;
import edu.uci.ics.jung.graph.SparseGraph;
import edu.uci.ics.jung.graph.util.EdgeType;
import edu.uci.ics.jung.graph.util.Pair;
import org.apache.commons.math3.distribution.ChiSquaredDistribution;

import java.util.*;


import java.io.Serializable;

/**
 * Class that create subgraphs of a KPMGraph, but does not provide the full functionalities
 * of a KPM Graph
 */

public class RandomSubgraph extends SparseGraph<GeneNode, GeneEdge> implements Serializable {

    private double pval = 1.0;

    public double getTestStatistics() {
        return testStatistics;
    }

    private double testStatistics = -1;
    private boolean is_significant = false;
    private double significanceLevel = 0.05;

    public RandomSubgraph(KPMGraph kpmGraph, int size) {
        super();
        generateRandomSizeN(kpmGraph, size);
    }

    private void generateRandomSizeN(KPMGraph kpmGraph, int size) {
        Random rand = new Random();
        int randomNodeIndex = rand.nextInt(kpmGraph.getVertices().size());
        GeneNode[] nodes = kpmGraph.getVertices().toArray(new GeneNode[kpmGraph.getVertices().size()]);
        GeneNode nextNode = nodes[randomNodeIndex];
        this.addVertex(nextNode);
        ArrayList<GeneNode> candidates = new ArrayList<GeneNode>();
        candidates.addAll(kpmGraph.getNeighbors(nextNode));
        int i = 1;
        while (i < size) {
            if (candidates.size() == 0) {
                break;
            }
            randomNodeIndex = rand.nextInt(candidates.size());
            nextNode = candidates.get(randomNodeIndex);
            //if node can be added, remove node from candidate list, but add all neighbours
            if (this.addVertex(nextNode)) {
                candidates.remove(randomNodeIndex);
                candidates.addAll(kpmGraph.getNeighbors(nextNode));
                i++;
                // add all edges for the newly created node
                for (GeneEdge e : kpmGraph.getOutEdges(nextNode)) {
                    if (this.containsVertex(this.getEndpoints(e).getFirst()) &&
                            this.containsVertex(this.getEndpoints(e).getSecond())) {
                        addEdge(e, new Pair<GeneNode>(getEndpoints(e)), EdgeType.UNDIRECTED);
                    }
                }
            }
            // if node can't be added (because it is already contained in node, remove it - this
            // will lead to all nodes being removed eventually, if the network size is smaller than the
            // targeted subnetwork.
            else {
                candidates.remove(randomNodeIndex);
            }
        }
        int degFree = calculatePvalFisher() * 2;
        significanceTest(degFree, this.testStatistics, this.significanceLevel);
    }

    private int calculatePvalFisher() {
        int nrTests = 0;
        double sumOfLogPvals = 0;
        for (GeneNode n : this.getVertices()) {
            nrTests++;
            sumOfLogPvals += Math.log(n.getPvalue());
        }
        double testStat = -2 * sumOfLogPvals;
        this.testStatistics = testStat;
        return nrTests;
    }

    private void significanceTest(int degFreedom, double testStatistics, double significanceLevel) {
        ChiSquaredDistribution chiSquare = new ChiSquaredDistribution(degFreedom);
        this.pval = chiSquare.cumulativeProbability(pval);
        if (this.pval <= significanceLevel) {
            this.is_significant = true;
        }
    }

}

package dk.sdu.kpm.algo.fdr;

import dk.sdu.kpm.graph.GeneEdge;
import dk.sdu.kpm.graph.GeneNode;
import dk.sdu.kpm.graph.KPMGraph;
import dk.sdu.kpm.graph.Result;
import edu.uci.ics.jung.algorithms.filters.Filter;
import edu.uci.ics.jung.graph.SparseGraph;
import edu.uci.ics.jung.graph.util.EdgeType;
import edu.uci.ics.jung.graph.util.Pair;
import org.apache.commons.math3.distribution.ChiSquaredDistribution;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.*;
import java.io.FileWriter;


import java.io.Serializable;

/**
 * Class that create subgraphs of a KPMGraph, but does not provide the full functionalities
 * of a KPM Graph
 * however povides other functionalities like p-values
 */

public class RandomSubgraph extends SparseGraph<GeneNode, GeneEdge> implements Serializable, Result {

    public double getPval() {
        return pval;
    }

    private double pval = 1.0;

    public double getTestStatistics() {
        return testStatistics;
    }

    private double testStatistics = -1;
    private boolean is_significant = false;
    private double significanceLevel = 0.05;

    /*
    * this constructor can be used to generate the random networks for the background distribution
     */
    public RandomSubgraph(KPMGraph kpmGraph, int size, boolean includeBackgroundNodes) {
        super();
        generateRandomSizeN(kpmGraph, size, includeBackgroundNodes);
    }
    /*
    * This constructor will be used by the Algorithm to generate candidate networks
     */
    public RandomSubgraph(KPMGraph kpmGraph){

    }

    private void generateRandomSizeN(KPMGraph kpmGraph, int size, boolean includeBackgroundNodes) {
        Random rand = new Random();
        int randomNodeIndex;
        GeneNode[] nodes = kpmGraph.getVertices().toArray(new GeneNode[kpmGraph.getVertices().size()]);
        GeneNode nextNode = null;

        boolean first = false;
        while(!first) {
            randomNodeIndex = rand.nextInt(kpmGraph.getVertices().size());
            nextNode = nodes[randomNodeIndex];
            if (!kpmGraph.getBackNodesMap().containsKey(nextNode.nodeId) || includeBackgroundNodes) {
                first = this.addVertex(nextNode);
            }

        }
        ArrayList<GeneNode> candidates = new ArrayList<GeneNode>();
        candidates.addAll(kpmGraph.getNeighbors(nextNode));
        int i = 1;
        while (i < size) {
            if (candidates.size() == 0) {
                //System.out.println("Ended: no remaining candidate");
                break;
            }
            randomNodeIndex = rand.nextInt(candidates.size());
            nextNode = candidates.get(randomNodeIndex);
            //if node can be added, remove node from candidate list, but add all neighbours
            // do not sample nodes not contained in the expression data
            //System.out.println((!kpmGraph.getBackNodesMap().containsKey(nextNode.nodeId)|| includeBackgroundNodes) );
            if ((!kpmGraph.getBackNodesMap().containsKey(nextNode.nodeId)|| includeBackgroundNodes) && this.addVertex(nextNode)) {

                candidates.remove(randomNodeIndex);
                candidates.addAll(kpmGraph.getNeighbors(nextNode));
                // TODO. random
                //System.out.println(remo.size());
                i++;
                // add all edges for the newly created node
                for (GeneEdge e : kpmGraph.getOutEdges(nextNode)) {
                    if (this.containsVertex(kpmGraph.getEndpoints(e).getFirst()) &&
                            this.containsVertex(kpmGraph.getEndpoints(e).getSecond())) {
                        addEdge(e, new Pair<GeneNode>(kpmGraph.getEndpoints(e)), EdgeType.UNDIRECTED);
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
        //System.out.println(this.getVertices().size());
        int degFree = calculatePvalFisher() * 2;
        significanceTest(degFree, this.testStatistics, this.significanceLevel);
        //System.out.println(this.getVertices().size());
        writeToFile("/home/anne/Documents/Master/MA/Testing/out_new/dist/pvalsFall.txt",
                "/home/anne/Documents/Master/MA/Testing/out_new/nodeDist/nodeDistFall.txt");
    }

    private int calculatePvalFisher() {
        double sumOfLogPvals = 0;
        int infcounter = 0;
        for (GeneNode n : this.getVertices()) {
            // TODO: Currently random value is chosen
            String akey = n.averagePvalue.keySet().toArray(new String[n.averagePvalue.keySet().size()])[0];
            sumOfLogPvals += Math.log(n.averagePvalue.get(akey));

            if(Double.isInfinite(Math.log(n.averagePvalue.get(akey)))){
                System.out.println(n.nodeId);
                infcounter ++;
            }

        }
        //System.out.println(sumOfLogPvals);
        double testStat = -2 * sumOfLogPvals;
        this.testStatistics = testStat;
        int deg = 2*this.getVertices().size();
        significanceTest(deg, this.testStatistics, 0.05);
        //System.out.println(infcounter);
        return this.getVertices().size();
    }

    private void significanceTest(int degFreedom, double testStatistics, double significanceLevel) {
        ChiSquaredDistribution chiSquare = new ChiSquaredDistribution(degFreedom);
        if(!Double.isInfinite(testStatistics)) {
            this.pval = 1.0- chiSquare.cumulativeProbability(testStatistics);
        }
        else{
            this.pval = 0.0;
        }
        if (this.pval <= significanceLevel) {
            this.is_significant = true;
        }
    }
    public void writeToFile(String filename, String filename2) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(filename,true)); BufferedWriter degreeWriter = new BufferedWriter(new FileWriter(filename2,true))) {
            int counter = this.getVertices().size();
            // first column: network size
            degreeWriter.write(counter+"\t");
            bw.write(counter+"\t");
            for (GeneNode n : this.getVertices()) {
                    if(counter>1) {
                        degreeWriter.write(this.getNeighbors(n).size() + "\t");
                        bw.write(n.getAveragePvalue().get(n.getAveragePvalue().keySet().toArray()[0]) + "\t");
                    }
                    else
                    {
                        degreeWriter.write(this.getNeighbors(n).size()+"");
                        bw.write(n.getAveragePvalue().get(n.getAveragePvalue().keySet().toArray()[0])+"");

                    }
                    counter--;

                }
                degreeWriter.write("\n");
                bw.write("\n");
        }
        catch (IOException e){
            e.printStackTrace(

            );
        }
    }

    @Override
    public Map<String, GeneNode> getVisitedNodes() {
        return null;
    }

    @Override
    public double getAverageDiffExpressedCases() {
        return 0;
    }

    @Override
    public double getInformationGainExpressed() {
        return 0;
    }

    @Override
    public int getFitness() {
        return 0;
    }

    @Override
    public int getInstances() {
        return 0;
    }

    @Override
    public void setInstances(int instances) {

    }

    @Override
    public int getNumExceptionNodes() {
        return 0;
    }

    @Override
    public int getNonDifferentiallyExpressedCases() {
        return 0;
    }

    @Override
    public void flagExceptionNodes() {

    }

    @Override
    public boolean haveOverlap(Object o) {
        return false;
    }

    @Override
    public int compareTo(Result o) {
        return 0;
    }
}

package dk.sdu.kpm.algo.fdr;

import dk.sdu.kpm.KPMSettings;
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
import java.nio.Buffer;
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

    public double getGeneralPval() {
        return generalPval;
    }

    private double generalPval = 1.0;

    public double getTestStatistics() {
        return testStatistics;
    }

    private double testStatistics = -1;

    public double getGeneralTeststat() {
        return generalTeststat;
    }

    private double generalTeststat = -1;
    private boolean is_significant = false;
    private double significanceLevel = 0.05;

    private KPMSettings kpmSettings;

    /*
    * this constructor can be used to generate the random networks for the background distribution
     */
    public RandomSubgraph(KPMGraph kpmGraph, int size, boolean includeBackgroundNodes, String filename, KPMSettings kpmSettings) {
        super();
        this.kpmSettings = kpmSettings;
        generateRandomSizeN(kpmGraph, size, includeBackgroundNodes, filename);
    }
    /*
    * This constructor will be used by the Algorithm to generate candidate networks
     */

    public RandomSubgraph(GeneNode node){
    //super();
    this.addVertex(node);
    }

    private void generateRandomSizeN(KPMGraph kpmGraph, int size, boolean includeBackgroundNodes, String filename) {
        Random rand = new Random();
        int randomNodeIndex;
        GeneNode[] nodes = kpmGraph.getVertices().toArray(new GeneNode[kpmGraph.getVertices().size()]);
        // TODO Array sorting for deterministic behaviour?
        // Order lexicographically - nodeId
        //Arrays.sort(nodes);
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
        int degFree = calculateNetworkScore(kpmSettings.AGGREGATION_METHOD);
        //System.out.println(this.getVertices().size());
        writeToFile(filename+"pvalsSamplewise.txt", filename+"nodeDist.txt", filename+"pvalsGeneral.txt");
    }

    protected int calculateNetworkScore(String method){
        int degFree = -1;
        switch(method){
            case "mean":
                degFree =  calculateMeanPval();
                break;
            case "fisher":
                degFree =  calculatePvalFisher();
                //significanceTest(degFree, this.testStatistics, this.significanceLevel);
                degFree = calculateGeneralPvalFisher();
                break;
            case "median":
                degFree = calculateMedianPval();
                break;
            default:
                System.exit(1);
        }
        return degFree;
    }

    private int calculateMedianPval(){
        double[] nodes = new double[this.getVertexCount()];
        double[] medianTest = new double[this.getVertexCount()];
        int i = 0;
        for(GeneNode n: this.getVertices()){
            nodes[i]=Math.abs(n.getAveragePvalue().get("L1"));
            medianTest[i] = Math.abs(n.getPvalue());
            i++;
        }
        Arrays.sort(nodes);
        Arrays.sort(medianTest);
        int medIndx = (int)Math.floor(nodes.length/2.0);
        this.testStatistics = nodes[medIndx];
        this.generalTeststat = medianTest[medIndx];
        return this.getVertexCount();
    }


    private int calculateMeanPval(){

        double sumOfPvals = 0.0;
        double sumOfGeneralPvals = 0.0;

        for (GeneNode n: this.getVertices()){
            // Use absolute values here to account for possibly negative zscores.
            sumOfPvals=sumOfPvals+Math.abs(n.getAveragePvalue().get("L1"));
            sumOfGeneralPvals = sumOfGeneralPvals + Math.abs(n.getPvalue());
        }
        double meanPval = sumOfPvals/this.getVertexCount();
        double meanGenPval = sumOfGeneralPvals/this.getVertexCount();

        this.testStatistics = meanPval;
        this.generalTeststat = meanGenPval;
        return this.getVertexCount();
    }



    private int calculatePvalFisher() {
        double sumOfLogPvals = 0;
        int infcounter = 0;
        for (GeneNode n : this.getVertices()) {
            // TODO: Currently random value is chosen
            String akey = n.averagePvalue.keySet().toArray(new String[n.averagePvalue.keySet().size()])[0];
            if(!akey.equals("L1")){
                System.out.println("not L1");
            }
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
        ChiSquaredDistribution chiSquare = new ChiSquaredDistribution(degFreedom,  1.0E-20 );
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

    // TODO remove ugly code duplicates
    protected int calculateGeneralPvalFisher() {
        double sumOfLogPvals = 0;
        int infcounter = 0;
        for (GeneNode n : this.getVertices()) {

            sumOfLogPvals += Math.log(n.getPvalue());

            if(Double.isInfinite(Math.log(n.getPvalue()))){
                System.out.println(n.nodeId);
                infcounter ++;
            }

        }
        //System.out.println(sumOfLogPvals);
        double testStat = -2 * sumOfLogPvals;
        this.generalTeststat = testStat;
        int deg = 2*this.getVertices().size();
        generalSignificanceTest(deg, this.generalTeststat, 0.05);
        //System.out.println(infcounter);
        return this.getVertices().size();
    }

    private void generalSignificanceTest(int degFreedom, double testStatistics, double significanceLevel) {
        ChiSquaredDistribution chiSquare = new ChiSquaredDistribution(degFreedom,  1.0E-220 );
        if(!Double.isInfinite(testStatistics)) {
            this.generalPval = 1.0- chiSquare.cumulativeProbability(testStatistics);
        }
        else{
            this.generalPval = 0.0;
        }
        if (this.generalPval <= significanceLevel) {
            this.is_significant = true;
        }
    }


    public void writeToFile(String filename, String filename2, String pvalsAll) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(filename,true));
             BufferedWriter pvall = new BufferedWriter(new FileWriter(pvalsAll,true));
             BufferedWriter degreeWriter = new BufferedWriter(new FileWriter(filename2,true))) {
            int counter = this.getVertices().size();
            // first column: network size
            degreeWriter.write(counter+"\t");
            bw.write(counter+"\t");
            pvall.write(counter +"\t");
            for (GeneNode n : this.getVertices()) {
                    if(counter>1) {
                        degreeWriter.write(this.getNeighbors(n).size() + "\t");
                        bw.write(n.getAveragePvalue().get(n.getAveragePvalue().keySet().toArray()[0]) + "\t");
                        pvall.write(n.getPvalue()+"\t");
                    }
                    else
                    {
                        degreeWriter.write(this.getNeighbors(n).size()+"");
                        bw.write(n.getAveragePvalue().get(n.getAveragePvalue().keySet().toArray()[0])+"");
                        pvall.write(n.getPvalue()+"");

                    }
                    counter--;

                }
                degreeWriter.write("\n");
                bw.write("\n");
                pvall.write("\n");
        }
        catch (IOException e){
            e.printStackTrace(

            );
        }
    }

    public void writeGraphToFile(String filename, String name, boolean general){
            try (BufferedWriter bw = new BufferedWriter(new FileWriter(filename+".graph", true));
                 BufferedWriter stat = new BufferedWriter(new FileWriter(filename+".stat", true));
                 BufferedWriter nodes = new BufferedWriter(new FileWriter(filename+".nodes", true))){
                // file for test p value and teststatistics for each solution
                if(!general) {
                    stat.write(name + "\t" + this.pval + "\t" + this.testStatistics + "\n");
                    // separate entries for each node
                    for (GeneNode n : this.getVertices()) {
                        nodes.write(n.nodeId+"\t"+name +"\t"+n.getAveragePvalue().get("L1") +"\t"+ this.getNeighborCount(n) + "\n");
                    }
                }
                else{
                    stat.write(name + "\t" + this.getGeneralPval() + "\t" + this.generalTeststat + "\n");
                    // separate entries for each node
                    for (GeneNode n : this.getVertices()) {
                        nodes.write(n.nodeId+"\t"+name +"\t"+n.getPvalue()  +"\t"+ this.getNeighborCount(n) + "\n");
                    }
                }

                //write graph to file in sif format
                for (GeneEdge e : this.getEdges()) {
                    bw.write(name +"\t"+getEndpoints(e).getFirst() + "\tpp\t" + getEndpoints(e).getSecond() + "\n");
                }

                //bw.append("###");
            } catch (IOException e) {
                e.printStackTrace();
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

package dk.sdu.kpm.algo.fdr;

import dk.sdu.kpm.KPMSettings;
import dk.sdu.kpm.graph.GeneEdge;
import dk.sdu.kpm.taskmonitors.IKPMTaskMonitor;
import dk.sdu.kpm.graph.GeneNode;
import dk.sdu.kpm.graph.KPMGraph;
import dk.sdu.kpm.graph.Result;
import dk.sdu.kpm.utils.Comparison;
import edu.uci.ics.jung.graph.DelegateForest;
import edu.uci.ics.jung.graph.Forest;
import edu.uci.ics.jung.graph.util.EdgeType;
import edu.uci.ics.jung.graph.util.Pair;
import edu.uci.ics.jung.algorithms.shortestpath.*;

import java.awt.*;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import dk.sdu.kpm.utils.Comparison;
import dk.sdu.kpm.utils.Comparator;
import org.apache.commons.collections15.map.HashedMap;
import org.apache.commons.math3.stat.inference.MannWhitneyUTest;

public class FDRGreedy implements Serializable {

    private int k;

    private Map<String, Integer> l_map;

    private final KPMGraph g;
    private final KPMGraph copy;

    public List<Result> allsds;

    public IKPMTaskMonitor taskMonitor;

    private volatile KPMSettings kpmSettings;

    ConcurrentHashMap<GeneNode, Boolean> visited;

    boolean general;



    // Store whether we are currently searching networks or whether we are
    // currently generating networks of a certain size.
    boolean isSearch;
    //used when searching otherwise null
    private final DistributionGenerator dg;

    //used when generating
    int nrSamples = 0;
    int maxSize = Integer.MAX_VALUE;
    GeneNode start;

    /*
    Use this constructor to generate candidate networks.
     */
    public FDRGreedy(KPMGraph g, IKPMTaskMonitor taskMonitor, KPMSettings settings, DistributionGenerator dg, boolean general) {
        this.g = g;
        this.copy = g;
        this.taskMonitor = taskMonitor;
        this.k = settings.GENE_EXCEPTIONS;
        this.l_map = settings.CASE_EXCEPTIONS_MAP;
        this.kpmSettings = settings;
        this.dg = dg;
        this.general = general;
        this.maxSize=settings.MAX_NETWORK_SIZE;
        this.nrSamples = Integer.MAX_VALUE;
        this.isSearch = true;
    }
    /*
    Use this constructor to create the "random" background

     */
    public FDRGreedy(KPMGraph g, IKPMTaskMonitor taskMonitor, KPMSettings settings, boolean general, int maxSize,
                     int nrSamples) {
        this.g = g;
        this.copy = g;
        this.taskMonitor = taskMonitor;
        this.k = settings.GENE_EXCEPTIONS;
        this.l_map = settings.CASE_EXCEPTIONS_MAP;
        this.kpmSettings = settings;
        this.dg = null;
        this.general = general;
        this.maxSize=maxSize;
        this.nrSamples = nrSamples;
        this.isSearch=false;

    }




    public List<Result> getResults() {
        return allsds;
    }

    public List<Result> runGreedy() {
        // this.g = g;
        List<Result> toReturn = new ArrayList<Result>();
        int nodesComputed = 0;
        int numV = g.getVertexCount();
        ExecutorService pool = Executors.newFixedThreadPool(kpmSettings.NUMBER_OF_PROCESSORS);


        List<Future<Result>> futures = new LinkedList<Future<Result>>();


        int counter = 0;
        visited = new ConcurrentHashMap<GeneNode, Boolean>();

        // starting nodes are selected from all the nodes in the network
        // to avoid substructure in network effects those are shuffled
        List<GeneNode> l = new ArrayList<GeneNode>();
        l.addAll(g.getVertices());
        Collections.shuffle(l, kpmSettings.R);

        // iterate over nodes
        for (final GeneNode node : l) {
            visited.put(node, false);

            // Stop loop if enough samples have been taken
            if(!isSearch && counter>=nrSamples){
                break;
            }

            // for computational feasibility seeds need values need to be better than a certain
            // threshold, say top 20% (is user defined)
            if (checkFitness(node, general)) {
                futures.add(pool.submit(new Callable<Result>() {

                    @Override
                    public Result call() throws Exception {
                        return fromStartingNode(node);
                    }
                }));
                //fromStartingNode(node);
                counter++;
            }
        }


        for (Future<Result> f : futures) {
            Result r;
            try {
                r = f.get();
                if (r != null) {
                    toReturn.add(r);
                }
                nodesComputed++;
                if (!kpmSettings.IS_BATCH_RUN) {
                    double completed = (double) nodesComputed / (double) numV;
                    taskMonitor.setProgress(completed);
                }
            } catch (InterruptedException ex) {
                Logger.getLogger(FDRGreedy.class.getName()).log(Level.SEVERE, null, ex);
            } catch (ExecutionException ex) {
                Logger.getLogger(FDRGreedy.class.getName()).log(Level.SEVERE, ex.getMessage(), ex);
            }

        }

        pool.shutdown();
        // recomended to filter out trivial networks.
        toReturn = filterNetworks(toReturn, 0, Integer.MAX_VALUE);
        // This step needs to be done before duplicates are removed
        // to retain correct weights for solutions
        RandomSubgraph reconciledResult = calculateEdgeWeight(toReturn);
        RandomSubgraph nodeReconciledResult = calculateNodeWeight(toReturn);
        toReturn = removeDuplicates(toReturn);
        // letzes Ergebnis ist das reconciled result
        //
        //RandomSubgraph reconciledResult = calculateEdgeWeight(toReturn);
        //RandomSubgraph nodeReconciledResult2 = calculateNodeWeight(toReturn);
        //toReturn.add(reconciledResult);
        //toReturn.add(nodeReconciledResult);
        //toReturn.add(nodeReconciledResult2);
        //toReturn = rankAndTrimResults(toReturn, 20);
        //toReturn = pruneAll(toReturn);

        /*System.out.println(this.kpmSettings.MAIN_GRAPH.getVertexCount());
        //prune graph remove all nodes of degree 1 or smaller
        for (GeneNode n: nn){
            this.kpmSettings.MAIN_GRAPH.addVertex(n);
        }
        for(int i =0; i<ee.size(); i++){
            this.kpmSettings.MAIN_GRAPH.addEdge(ee.get(i), start.get(i), end.get(i));
        }
        // for good measure also update backup
        this.kpmSettings.MAIN_GRAPH_BACKUP = this.kpmSettings.MAIN_GRAPH;

        List<Result> resi = new ArrayList<>();
        for(Result r: toReturn){
            resi.add(extendNetwork((RandomSubgraph)r, false));
        }
        System.out.println(this.kpmSettings.MAIN_GRAPH.getVertexCount());
        resi = pruneAll(resi);
        return resi;*/
        return toReturn;
    }

    private Result fromStartingNode(GeneNode startingNode) {
        RandomSubgraph sd = new RandomSubgraph(startingNode);
        //System.out.println(startingNode);
        sd.add2lastNNodes(startingNode);
        sd = extendNetwork(sd, false);
        //extendSeedNetwork(sd);
        return sd;
    }

    /*
    Improve generated Results by exchaning low scoring nodes with high scoring nodes
    while still keeping the candidate network connected
     */
    private RandomSubgraph pruneNetworks(RandomSubgraph rs){
        // non member ordered in increasing order
        Map <GeneNode, Integer> degrees = new HashedMap<GeneNode, Integer>();
        PriorityQueue<GeneNode> nonMembers = new PriorityQueue<GeneNode>(new java.util.Comparator<GeneNode>() {
            @Override
            public int compare(GeneNode o1, GeneNode o2) {
                if (Math.abs(o1.getPvalue()) <= Math.abs(o2.getPvalue())) {
                    return -1;
                } else {
                    return 1;
                }
            }
        });
        // members ordered in decreasing order
        PriorityQueue<GeneNode> members = new PriorityQueue<GeneNode>(new java.util.Comparator<GeneNode>() {
            @Override
            public int compare(GeneNode o1, GeneNode o2) {
                if (Math.abs(o1.getPvalue()) <= Math.abs(o2.getPvalue())) {
                    return 1;
                } else {
                    return -1;
                }
            }
        });
        for(GeneNode n: rs.getVertices()){
            members.add(n);
            degrees.put(n, rs.getNeighborCount(n));
            for(GeneNode neigh: rs.getNeighbors(n)){
                if(!rs.containsVertex(neigh)){
                    nonMembers.add(neigh);
                }
            }
        }


        while(members.peek()!=null && nonMembers.peek()!=null && members.peek().getPvalue()>nonMembers.peek().getPvalue()){
            boolean condition = true;
               /*  for(GeneNode n: rs.getNeighbors(members.peek())){
                   if(degrees.get(n)==1){
                        condition = false;
                    }
                }*/
                if(condition) {
                    rs.removeVertex(members.poll());
                    rs.addVertex(nonMembers.poll());
                }
                // remove first from stack, such that next worse node gets exchanged
                // with best node in nonMember queue.
                // This should keep the graph connected
                else{
                    members.poll();
                }
        }
        return rs;
    }

    private List<Result> pruneAll(List<Result> seedNets){
        List<Result> rlist = new ArrayList<Result>();
        for(Result rs : seedNets){
            rlist.add(pruneNetworks((RandomSubgraph) rs));
        }
        return rlist;
    }

    private RandomSubgraph calculateEdgeWeight(List<Result> seedNets) {
        double threshold = 0.02;
        Map<GeneEdge, Integer> edgeCount = new HashMap<GeneEdge, Integer>();

        for (Result r : seedNets) {
            RandomSubgraph res = (RandomSubgraph) r;
            for (GeneEdge e : res.getEdges()) {
                if (edgeCount.containsKey(e)) {
                    edgeCount.put(e, edgeCount.get(e) + 1);
                } else {
                    edgeCount.put(e, 1);
                }
            }
        }
        RandomSubgraph reconciledResult = new RandomSubgraph();
        for (GeneEdge e : edgeCount.keySet()) {
            if (edgeCount.get(e) * 1.0 / seedNets.size() * 1.0 >= threshold) {
            //if (edgeCount.get(e) > 1) {
                reconciledResult.addVertex(this.g.getEndpoints(e).getFirst());
                reconciledResult.addVertex(this.g.getEndpoints(e).getSecond());
                reconciledResult.addEdge(e, this.g.getEndpoints(e).getFirst(), this.g.getEndpoints(e).getSecond(), EdgeType.UNDIRECTED);
            }
        }
        //System.out.println(edgeCount);
        //System.out.println(seedNets.size());
        //System.out.println(reconciledResult.getVertexCount());
        if(reconciledResult.getVertexCount()>0) {
            reconciledResult.calculateNetworkScore(kpmSettings.AGGREGATION_METHOD, copy);
        }
        return reconciledResult;
    }


    private RandomSubgraph calculateNodeWeight(List<Result> seedNets) {
        double threshold = 0.5;
        Map<GeneNode, Integer> nodeCount = new HashMap<GeneNode, Integer>();

        for (Result r : seedNets) {
            RandomSubgraph res = (RandomSubgraph) r;
            for (GeneNode node : res.getVertices()) {
                if (nodeCount.containsKey(node)) {
                    nodeCount.put(node, nodeCount.get(node) + 1);
                } else {
                    nodeCount.put(node, 1);
                }
            }
        }
        RandomSubgraph reconciledResult = new RandomSubgraph();
        for (GeneNode node : nodeCount.keySet()) {
            if (nodeCount.get(node) * 1.0 / seedNets.size() * 1.0 >= threshold) {
                //if (edgeCount.get(e) > 1) {
                reconciledResult.addVertex(node);
            }
        }
        for(GeneNode n: reconciledResult.getVertices()){
            for (GeneEdge e : copy.getOutEdges(n)) {
                if (reconciledResult.containsVertex(copy.getEndpoints(e).getFirst()) &&
                        reconciledResult.containsVertex(copy.getEndpoints(e).getSecond())) {
                    reconciledResult.addEdge(e, new Pair<GeneNode>(copy.getEndpoints(e)), EdgeType.UNDIRECTED);
                }
            }
        }
        //System.out.println(edgeCount);
        //System.out.println(seedNets.size());
        // make sure the network size is greater 0; median needs an array length >0
        if(reconciledResult.getVertexCount()>0) {
            reconciledResult.calculateNetworkScore(kpmSettings.AGGREGATION_METHOD,copy);
        }
        return reconciledResult;
    }
    /*
    Networks are sorted by score before being returned.
     */
    private List<Result> rankAndTrimResults(List<Result> seedNets, int nrResultsToReturn){
        List<Result> result = new ArrayList<Result>();
        for (Result r : seedNets) {
            RandomSubgraph res = (RandomSubgraph) r;
            if(kpmSettings.RANKING_METHOD.equals("mean")) {
                res.setPerNodeScore(res.getGeneralTeststat() / res.getVertexCount());
            }
            else if (kpmSettings.RANKING_METHOD.equals("median")){
                double[]  d = new double[((RandomSubgraph) r).getVertexCount()];
                int counter = 0;
                for (GeneNode n: res.getVertices()) {
                    d[counter] = n.getPvalue();
                    counter++;
                }
                Arrays.sort(d);
                int midIndex = (int) Math.floor(res.getVertexCount()/2);
                res.setPerNodeScore(d[midIndex]);
            }
            else{
                // default to mean
                res.setPerNodeScore(res.getGeneralTeststat() / res.getVertexCount());
            }
        }
        Collections.sort(seedNets, new java.util.Comparator<Result>() {
            @Override
            public int compare(Result o1, Result o2) {
                return ((RandomSubgraph)o1).getPerNodeScore() < ((RandomSubgraph)o2).getPerNodeScore() ? -1:
                        ((RandomSubgraph)o1).getPerNodeScore() > ((RandomSubgraph)o2).getPerNodeScore() ? 1:
                                0;
            }
        });

        //
        nrResultsToReturn = Math.min(nrResultsToReturn, seedNets.size());
        for(int i = 0; i<nrResultsToReturn; i++){
            result.add(seedNets.get(i));
        }
    return result;
    }

    private List<Result> removeDuplicates(List<Result> seedNets) {
        ArrayList<Integer> indx = new ArrayList<>();
        RandomSubgraph[] ar = seedNets.toArray(new RandomSubgraph[seedNets.size()]);
        for (int i = 0; i < ar.length; i++) {
            for (int j = i + 1; j < ar.length; j++) {
                if (ar[i].duplicated(ar[j])) {
                    indx.add(j);
                }
            }
        }
        List<Result> res = new ArrayList<>();
        for (int i = 0; i < ar.length; i++) {
            if (!indx.contains(i)) {
                res.add(ar[i]);
            }
        }
        //System.out.println("Number of duplicated networks: " + indx.size());
        return res;
    }

    private List<Result> filterNetworks(List<Result> seedNets, int lowerLimit, int upperLimit) {
        List<Result> res = new ArrayList<>();
        int counter = 0;
        for (Result r : seedNets) {
            if (((RandomSubgraph) r).getVertexCount() > lowerLimit && ((RandomSubgraph) r).getVertexCount() < upperLimit) {
                res.add(r);
            } else {
                counter++;
            }
        }
        taskMonitor.setStatusMessage("Removed " + counter +
                " networks of size " + lowerLimit + " or smaller and size " + upperLimit + " or larger");
        return res;
    }

    private RandomSubgraph extendSeedNetwork(RandomSubgraph sg) {
        //UnweightedShortestPath usp = new UnweightedShortestPath(this.g);
        //Map<GeneNode, Number> distanceMap = usp.getDistanceMap(sg.getVertices().toArray(new GeneNode[sg.getVertexCount()])[0]);

        GeneNode randomGeneNode = sg.getVertices().toArray(new GeneNode[sg.getVertexCount()])[0];
        return sg;
    }


    private RandomSubgraph extendNetwork(RandomSubgraph sd, boolean hubCorrection) {
        double minint = 0;
        RandomSubgraph currentBest = sd;
            ArrayList<GeneNode> candidates = new ArrayList<GeneNode>();
            HashSet<GeneNode> queried = new HashSet<GeneNode>();
            ArrayList<GeneNode> order = new ArrayList<GeneNode>();

            for (GeneNode n : sd.getVertices()) {
                candidates.addAll(copy.getNeighbors(n));
            }
            PriorityQueue<GeneNode> queue = new PriorityQueue<GeneNode>(new java.util.Comparator<GeneNode>() {
                @Override
                public int compare(GeneNode o1, GeneNode o2) {
                    if (Math.abs(o1.getPvalue()) <= Math.abs(o2.getPvalue())) {
                        return -1;
                    } else {
                        return 1;
                    }
                }
            });
            queue.addAll(candidates);

            boolean condition = true;
            Wrapper w = null;
            while (condition) {
                GeneNode g = queue.poll();
                if (g != null) {
                    if (queried.contains(g)) {
                        continue;
                    }
                    queried.add(g);
                    order.add(g);
                    if (sd.addVertex(g)) {
                        sd.add2lastNNodes(g);
                        if (checkFitness(sd, general)) {
                            for (GeneEdge e : copy.getOutEdges(g)) {
                                if (sd.containsVertex(copy.getEndpoints(e).getFirst()) &&
                                        sd.containsVertex(copy.getEndpoints(e).getSecond())) {
                                    sd.addEdge(e, new Pair<GeneNode>(copy.getEndpoints(e)), EdgeType.UNDIRECTED);
                                }
                            }
                            queue.addAll(copy.getNeighbors(g));
                            sd.add2ScoreTracker(sd.getGeneralTeststat());
                            w = track(sd,dg, minint);

                        } else {
                            sd.removeVertex(g);
                            // is this really necessary here?
                            sd.calculateNetworkScore(kpmSettings.AGGREGATION_METHOD, copy);
                            condition = false;
                        }
                    }
                } else {
                    condition = false;
                }
            }
            if(isSearch && w!=null){
                currentBest = returnBest(sd, w, order);
            }
        return currentBest;
    }

    /*
    Stores the best subnetwork
     */
    private Wrapper track(RandomSubgraph sd, DistributionGenerator dg, double minint){
        if(!isSearch){
            return new Wrapper(0, minint);
        }
        else{
            if (Math.abs(sd.getGeneralTeststat()-dg.getMeanTS(sd.getVertexCount()))>minint) {
                minint = Math.abs(sd.getGeneralTeststat()-dg.getMeanTS(sd.getVertexCount()));
                return new Wrapper(sd.getVertexCount(), minint);
            }
            else{
                return new Wrapper(sd.getMinInd(), minint);
            }
        }

    }

    /*
    Returns the network which corresponds to the maximum distance of the score to the network
    or the same network when this option is not selected
     */
    private RandomSubgraph returnBest(RandomSubgraph currentBest, Wrapper w, ArrayList<GeneNode> order){
        RandomSubgraph fin = null;
        if(kpmSettings.TERMINATION_CRITERION.equals("maximum_distance")) {
            fin = new RandomSubgraph();
            for (int i = 0; i < w.minInd; i++) {
                GeneNode g = order.get(i);
                fin.addVertex(g);
                for (GeneEdge e : copy.getOutEdges(g)) {
                    if (fin.containsVertex(copy.getEndpoints(e).getFirst()) &&
                            fin.containsVertex(copy.getEndpoints(e).getSecond())) {
                        fin.addEdge(e, new Pair<GeneNode>(copy.getEndpoints(e)), EdgeType.UNDIRECTED);
                    }
                }
            }
            fin.setMinInd(w.minInd);
            fin.setScoreTracker(currentBest.getScoreTracker());
            fin.calculateNetworkScore(kpmSettings.AGGREGATION_METHOD, copy);
        }
        // default to largest graph that fullfills criterion.
        else{
            fin = currentBest;
        }
        return fin;
    }


    /*
     * User can choose between using averaged p-values for the patients
     * or single p-value per gene
     */
    private boolean checkFitness(GeneNode n, boolean general) {
        double sig = kpmSettings.SIGNIFICANCE_LEVEL;
        if(!isSearch){
            sig = 1.0;
        }
        boolean fit = false;
        if (!general) {

            // TODO: currently a random p value is chosen
            String akey = n.getAveragePvalue().keySet().
                    toArray(new String[n.getAveragePvalue().keySet().size()])[0];
            if (Comparison.evalate(Math.abs(n.getAveragePvalue().get("L1")), sig , kpmSettings.COMPARATOR)) {
                fit = true;
                //System.out.println(n.nodeId+"\t"+fit);
            } else {
                fit = false;
            }
        } else {
            if (Comparison.evalate(Math.abs(n.getPvalue()), sig, kpmSettings.COMPARATOR)) {
                fit = true;
            }
        }
        return fit;
    }



    /*
     * compare combined p value of the current subnetwork to the fdr of the random networks of that size.
     */
    private boolean checkFitness(RandomSubgraph sg, boolean general) {
        boolean fit = false;
        sg.calculateNetworkScore(kpmSettings.AGGREGATION_METHOD, copy);

        //PHASE 1:
        // Break of criterion when sampling graphs
        // maxSize reached
        if(!isSearch){
            if(sg.getVertexCount()<=this.maxSize) {
                return true;
            }
            else{
                return false;
            }
        }

        //PHASE 2:
        // break search off if maximum size is reached
        if(sg.getVertexCount()>=kpmSettings.MAX_NETWORK_SIZE+1){
            return false;
        }
        // should be redundant, because thresholds maximally as long as maximal network size
        if (sg.getVertices().size() >= dg.getThresholds().length) {
            return false;
        }
        if(!kpmSettings.SLIDING_WINDOW) {
            if (!general) {
                // subnetwork pvalue must be smaller than threshold
                if (kpmSettings.AGGREGATION_METHOD.equals("fisher")) {
                    if (Comparison.evalate(sg.getPval(), dg.getThreshold(sg.getVertexCount()), Comparator.LET)) {
                        fit = true;
                    }
                } else {
                    if (Comparison.evalate(sg.getTestStatistics(), dg.getMeanTS(sg.getVertexCount()), kpmSettings.COMPARATOR)) {
                        fit = true;
                    }
                }
            } else {
                if (kpmSettings.AGGREGATION_METHOD.equals("fisher")) {
                    if (Comparison.evalate(sg.getGeneralPval(), dg.getThreshold(sg.getVertexCount()), Comparator.LET)) {
                        fit = true;
                    }
                } else {
                    if (Comparison.evalate(sg.getGeneralTeststat(), dg.getMeanTS(sg.getVertexCount()), kpmSettings.COMPARATOR)) {
                        fit = true;
                    }
                }
            }
        }else{
            //int size = (int) Math.floor(sg.getVertexCount()*0.67);
            int size=sg.getN();
            if (!general) {

                // TODO: adapt mean
                // refresh P value of subnetwork

                // subnetwork pvalue must be smaller than threshold
                if (kpmSettings.AGGREGATION_METHOD.equals("fisher")) {
                    if (Comparison.evalate(sg.getPvalLastN(), dg.getThreshold(size), kpmSettings.COMPARATOR)) {
                        fit = true;
                    }
                } else {
                    if (Comparison.evalate(sg.getTestStatisticsLastN(), dg.getMeanTS(size), kpmSettings.COMPARATOR)) {
                        fit = true;
                    }
                }
            } else {
                if (kpmSettings.AGGREGATION_METHOD.equals("fisher")) {
                    if (Comparison.evalate(sg.getGeneralPvalLastN(), dg.getThreshold(size), kpmSettings.COMPARATOR)) {
                        fit = true;
                    }
                } else {
                    if (Comparison.evalate(sg.getGeneralTeststatLastN(), dg.getMeanTS(size), kpmSettings.COMPARATOR)) {
                        fit = true;
                    }
                }
            }
        }
        return fit;
    }


    private volatile boolean isCancelled = false;

    private synchronized boolean isCancelled() {
        return this.isCancelled;
    }

    public synchronized void cancel() {
        this.isCancelled = true;
//        if(pool != null){
//            pool.shutdownNow();
//        }
    }

    /*
    Inner class wrapper used to store one int and one double
     */
    private class Wrapper{
        int minInd = 0;
        double minDouble = 0.0;
        private Wrapper(int i, double d){
            this.minDouble = d;
            this.minInd = i;
        }
    }

}

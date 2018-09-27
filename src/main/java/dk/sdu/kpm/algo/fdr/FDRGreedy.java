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

    boolean general;

    //thread safe
    //private volatile DistributionGenerator dg; ??
    private final DistributionGenerator dg;
    ConcurrentHashMap<GeneNode, Boolean> visited;

    public FDRGreedy(KPMGraph g, IKPMTaskMonitor taskMonitor, KPMSettings settings, DistributionGenerator dg, boolean general) {
        this.g = g;
        this.copy = g;
        this.taskMonitor = taskMonitor;
        this.k = settings.GENE_EXCEPTIONS;
        this.l_map = settings.CASE_EXCEPTIONS_MAP;
        this.kpmSettings = settings;
        this.dg = dg;
        this.general = general;
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


        // for computational feasibility seeds need values need to be better than a certain
        // threshold.

        // TODO: make the concurrent execution more efficient
        int counter = 0;
        visited = new ConcurrentHashMap<GeneNode, Boolean>();

        for (final GeneNode node : g.getVertices()) {
            visited.put(node, false);
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
        //RandomSubgraph reconciledResult = calculateEdgeWeight(toReturn);
        toReturn = removeDuplicates(toReturn);
        // TODO:verbesserungswürdige Rückgabe des Gesamtergebnisses.
        // letzes Ergebnis ist das reconciled result
        //toReturn.add(reconciledResult);
        //toReturn = rankAndTrimResults(toReturn, 20);
        //toReturn = pruneAll(toReturn);
        return toReturn;
    }

    private Result fromStartingNode(GeneNode startingNode) {
        RandomSubgraph sd = new RandomSubgraph(startingNode);
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
                for(GeneNode n: rs.getNeighbors(members.peek())){
                    if(degrees.get(n)==1){
                        condition = false;
                    }
                }
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
        System.out.println(edgeCount);
        System.out.println(seedNets.size());
        reconciledResult.calculateNetworkScore(kpmSettings.AGGREGATION_METHOD);
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
        System.out.println("Number of duplicated networks: " + indx.size());
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
        RandomSubgraph currentBest = null;
        try(BufferedWriter bw = new BufferedWriter(new FileWriter("/home/anne/Masterarbeit/Testing/max_distance4.txt", true));
                BufferedWriter b2 = new BufferedWriter(new FileWriter("/home/anne/Masterarbeit/Testing/scores4.txt", true));
            BufferedWriter b3 = new BufferedWriter(new FileWriter("/home/anne/Masterarbeit/Testing/per_node_Score4.txt", true))) {

            //RandomSubgraph sol = new RandomSubgraph(startingNode);
            //starting node can be addedKPMGraph kpmGraph
            //GeneNode st = new GeneNode(startingNode);

            int noImprovementCounter = 0;
            ArrayList<GeneNode> candidates = new ArrayList<GeneNode>();
            HashSet<GeneNode> queried = new HashSet<GeneNode>();

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

            int allowedFails = 0;
            boolean condition = true;
            while (condition) {
                GeneNode g = queue.poll();
                if (g != null) {
                    if (queried.contains(g)) {
                        continue;
                    }
                    queried.add(g);
                    if (sd.addVertex(g)) {
                        //Add all neighbour nodes of candidate node before checking the fitness to see if
                        // node brings enough good neighbours -> idea: avoid hubnodes, that bring to much
                        // junk
                        // Hashmap stores if add was succesfull to not remove any node which was
                        // in the network before after the fitness check.
                        HashMap<GeneNode, Boolean> added = new HashMap<GeneNode, Boolean>();
                    /*if(hubCorrection&&copy.getNeighborCount(g)>5){
                        for(GeneNode neigh : copy.getNeighbors(g)){
                            added.putIfAbsent(neigh,sd.addVertex(neigh));
                        }
                    }*/
                        if (checkFitness(sd, general)) {
                            for (GeneEdge e : copy.getOutEdges(g)) {
                                if (sd.containsVertex(copy.getEndpoints(e).getFirst()) &&
                                        sd.containsVertex(copy.getEndpoints(e).getSecond())) {
                                    sd.addEdge(e, new Pair<GeneNode>(copy.getEndpoints(e)), EdgeType.UNDIRECTED);
                                }
                            }
                            queue.addAll(copy.getNeighbors(g));
                            sd.calculateNetworkScore(kpmSettings.AGGREGATION_METHOD);
                            // Additive score aggregation score of candidate mus be below threshold
                            double d = dg.getMeanTS(sd.getVertexCount()) - sd.getGeneralTeststat();
                            bw.write(sd.id+"\t"+d+"\n");
                            b2.write(sd.id+"\t"+sd.getGeneralTeststat()+"\n");
                            b3.write(sd.id+"\t"+sd.getGeneralTeststat()/sd.getVertexCount()+"\n");
                            if (sd.getMaxDistanceFromThresh() < dg.getMeanTS(sd.getVertexCount()) - sd.getGeneralTeststat()) {
                                sd.setMaxDistanceFromThresh(dg.getMeanTS(sd.getVertexCount()) - sd.getGeneralTeststat());
                                currentBest = sd;
                                noImprovementCounter = 0;
                            } else {
                                noImprovementCounter++;
                            }
                        } else {
                            sd.removeVertex(g);
                            sd.calculateNetworkScore(kpmSettings.AGGREGATION_METHOD);
                            condition = false;
                        }
                        //if(noImprovementCounter>5){
                        //  condition = false;
                        //}
                        // remove neighbours of the candidate node again.
                    /*if(hubCorrection&&copy.getNeighborCount(g)>5){
                        for(GeneNode neigh : copy.getNeighbors(g)){
                            if(added.get(neigh)) {
                                sd.removeVertex(neigh);
                            }
                        }
                    }*/
                    }
                } else {
                    condition = false;
                }
            }
            //if(!checkFitness(sd, general)){
            //    condition = false;

            //}

            //return sd;
        }
        catch (IOException e){
            e.printStackTrace();
        }
        return currentBest;
    }


    /*
     * User can choose between using averaged p-values for the patients
     * or single p-value per gene
     */
    private boolean checkFitness(GeneNode n, boolean general) {
        boolean fit = false;
        if (!general) {

            // TODO: currently a random p value is chosen
            String akey = n.getAveragePvalue().keySet().
                    toArray(new String[n.getAveragePvalue().keySet().size()])[0];
            if (Comparison.evalate(Math.abs(n.getAveragePvalue().get("L1")), kpmSettings.SIGNIFICANCE_LEVEL, kpmSettings.COMPARATOR)) {
                fit = true;
                //System.out.println(n.nodeId+"\t"+fit);
            } else {
                fit = false;
            }
        } else {
            if (Comparison.evalate(Math.abs(n.getPvalue()), kpmSettings.SIGNIFICANCE_LEVEL, kpmSettings.COMPARATOR)) {
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
        sg.calculateNetworkScore(kpmSettings.AGGREGATION_METHOD);
        if (sg.getVertices().size() >= dg.getThresholds().length) {
            return false;
        }
        if (!general) {
            // TODO: adapt mean
            // refresh P value of subnetwork

            // subnetwork pvalue must be smaller than threshold
            if (kpmSettings.AGGREGATION_METHOD.equals("fisher")) {
                if (sg.getPval() - dg.getThreshold(sg.getVertexCount()) < Double.MIN_VALUE) {
                    fit = true;
                }
            } else {
                if (sg.getTestStatistics() - dg.getMeanTS(sg.getVertexCount()) < Double.MIN_VALUE) {
                    fit = true;
                }
            }
        } else {
            if (kpmSettings.AGGREGATION_METHOD.equals("fisher")) {
                if (sg.getGeneralPval() - dg.getThreshold(sg.getVertexCount()) < Double.MIN_VALUE) {
                    fit = true;
                }
            } else {
                if ((sg.getGeneralTeststat() - dg.getMeanTS(sg.getVertexCount())) < Double.MIN_VALUE) {
                    fit = true;
                }
            }
        }
        return fit;

            /* double [] ts = new double[sg.getVertexCount()];
                int counter = 0;
                for(GeneNode cand : sg.getVertices()){
                    ts[counter] = Math.abs(cand.getPvalue());
                    //System.out.print(ts[counter]+"\t");
                    counter++;
                }
                //System.out.println(ts);
                //System.out.println(dg.getMeanTS(sg.getVertexCount()));
                MannWhitneyUTest mw = new MannWhitneyUTest();
                int ind = sg.getVertexCount();
                while(!dg.getDistribution().keySet().contains(ind)){
                    ind++;
            }
                double [] dummy = dg.getDistribution().get(ind);
                //System.out.println(mw.mannWhitneyUTest(ts, dummy));
                if(mw.mannWhitneyUTest(ts, dummy)<0.02){
                    fit = true;
                }
               */
    }


    private void mutate() {

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

}

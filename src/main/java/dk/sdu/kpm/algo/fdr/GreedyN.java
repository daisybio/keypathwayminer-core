package dk.sdu.kpm.algo.fdr;

import dk.sdu.kpm.KPMSettings;
import dk.sdu.kpm.graph.GeneEdge;
import dk.sdu.kpm.taskmonitors.IKPMTaskMonitor;
import dk.sdu.kpm.graph.GeneNode;
import dk.sdu.kpm.graph.KPMGraph;
import dk.sdu.kpm.graph.Result;
import dk.sdu.kpm.utils.Comparison;
import edu.uci.ics.jung.graph.util.EdgeType;
import edu.uci.ics.jung.graph.util.Pair;

import java.awt.*;
import java.io.Serializable;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import dk.sdu.kpm.utils.Comparison;
import dk.sdu.kpm.utils.Comparator;
import org.apache.commons.math3.stat.inference.MannWhitneyUTest;

public class GreedyN implements Serializable {

    private int k;

    private Map<String, Integer> l_map;

    private final KPMGraph g;
    private final KPMGraph copy;

    public List<Result> allsds;

    public IKPMTaskMonitor taskMonitor;

    private volatile KPMSettings kpmSettings;

    boolean general;

    int sampleSize;
    //thread safe
    //private volatile DistributionGenerator dg; ??
    //private final DistributionGenerator dg;
    ConcurrentHashMap<GeneNode, Boolean> visited;
    int targetSize = 0;

    public GreedyN(KPMGraph g, IKPMTaskMonitor taskMonitor, KPMSettings settings, boolean general, int targetSize, int sampleSize) {
        this.g = g;
        this.copy = g;
        this.taskMonitor = taskMonitor;
        this.k = settings.GENE_EXCEPTIONS;
        this.l_map = settings.CASE_EXCEPTIONS_MAP;
        this.kpmSettings = settings;
       // this.dg = dg;
        this.general = general;
        this.targetSize = targetSize;
        this.sampleSize = sampleSize;
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

        int counter = 0;
        visited =  new ConcurrentHashMap<GeneNode, Boolean>();

        Random rand = new Random(3920);
        Set<Integer> sample = new HashSet<Integer>();
        while(sample.size()<sampleSize){
            sample.add(rand.nextInt(g.getVertexCount()));
        }

        for (final GeneNode node : g.getVertices()) {
            visited.put(node, false);

            if (sample.contains(counter)) {
                futures.add(pool.submit(new Callable<Result>() {

                    @Override
                    public Result call() throws Exception {
                        return fromStartingNode(node);
                    }
                }));

            }
            counter++;
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
                    double completed = (double)nodesComputed / (double)numV;
                    taskMonitor.setProgress(completed);
                }
            } catch (InterruptedException ex) {
                Logger.getLogger(FDRGreedy.class.getName()).log(Level.SEVERE, null, ex);
            } catch (ExecutionException ex) {
                Logger.getLogger(FDRGreedy.class.getName()).log(Level.SEVERE, ex.getMessage(), ex);
            }

        }
        pool.shutdown();
        return toReturn;
    }

    private Result fromStartingNode(GeneNode startingNode) {
        RandomSubgraph sd = new RandomSubgraph(startingNode);
        sd = extendNetwork(sd, false);

        return sd;
    }

    private RandomSubgraph extendNetwork(RandomSubgraph sd, boolean isSecond) {
        //RandomSubgraph sol = new RandomSubgraph(startingNode);
        // starting node can be addedKPMGraph kpmGraph
        //GeneNode st = new GeneNode(startingNode);
        ArrayList<GeneNode> candidates = new ArrayList<GeneNode>();
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

        int targetCounter = 0;
        while (targetCounter < targetSize) {

            GeneNode n = queue.poll();
            if(n!=null) {
                if(sd.addVertex(n)) {
                    targetCounter++;
                    for (GeneEdge e : copy.getOutEdges(n)) {
                        if (sd.containsVertex(copy.getEndpoints(e).getFirst()) &&
                                sd.containsVertex(copy.getEndpoints(e).getSecond())) {
                            sd.addEdge(e, new Pair<GeneNode>(copy.getEndpoints(e)), EdgeType.UNDIRECTED);
                        }
                    }
                    queue.addAll(copy.getNeighbors(n));
                }
            }
            else{
                // Set while condition to false
                targetCounter=targetSize;
            }
        }

        sd.calculateNetworkScore(kpmSettings.AGGREGATION_METHOD);
        return sd;


    }

    private boolean checkFitness(GeneNode n, boolean general){
        boolean fit = false;
        if (!general) {

            // TODO: currently a random p value is chosen
            String akey = n.getAveragePvalue().keySet().
                    toArray(new String[n.getAveragePvalue().keySet().size()])[0];
            if (Comparison.evalate(Math.abs(n.getAveragePvalue().get("L1")), kpmSettings.SIGNIFICANCE_LEVEL, kpmSettings.COMPARATOR)) {
                fit = true;
                //System.out.println(n.nodeId+"\t"+fit);
            }
        } else {
            if (Comparison.evalate(Math.abs(n.getPvalue()), kpmSettings.SIGNIFICANCE_LEVEL, kpmSettings.COMPARATOR)) {
                fit = true;
            }
        }
        return fit;
    }

    private volatile boolean isCancelled = false;

    private synchronized boolean isCancelled(){
        return this.isCancelled;
    }

    public synchronized void cancel() {
        this.isCancelled = true;
//        if(pool != null){
//            pool.shutdownNow();
//        }
    }

}

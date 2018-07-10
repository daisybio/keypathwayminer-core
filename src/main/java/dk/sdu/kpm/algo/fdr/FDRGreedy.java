package dk.sdu.kpm.algo.fdr;

import dk.sdu.kpm.KPMSettings;
import dk.sdu.kpm.graph.GeneEdge;
import dk.sdu.kpm.taskmonitors.IKPMTaskMonitor;
import dk.sdu.kpm.graph.GeneNode;
import dk.sdu.kpm.graph.KPMGraph;
import dk.sdu.kpm.graph.Result;
import edu.uci.ics.jung.graph.util.EdgeType;
import edu.uci.ics.jung.graph.util.Pair;

import java.awt.*;
import java.io.Serializable;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FDRGreedy implements Serializable {

    private int k;

    private Map<String, Integer> l_map;

    private final KPMGraph g;
    private final KPMGraph copy;

    public List<Result> allSolutions;

    public IKPMTaskMonitor taskMonitor;

    private volatile KPMSettings kpmSettings;

    //thread safe
    //private volatile DistributionGenerator dg; ??
    private final DistributionGenerator dg;
    ConcurrentHashMap<GeneNode, Boolean> visited;

    public FDRGreedy(KPMGraph g, IKPMTaskMonitor taskMonitor, KPMSettings settings, DistributionGenerator dg) {
        this.g = g;
        this.copy = g;
        this.taskMonitor = taskMonitor;
        this.k = settings.GENE_EXCEPTIONS;
        this.l_map = settings.CASE_EXCEPTIONS_MAP;
        this.kpmSettings = settings;
        this.dg = dg;
    }

    public List<Result> getResults() {
        return allSolutions;
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
        visited =  new ConcurrentHashMap<GeneNode, Boolean>();

        GeneNode n = null;
        Iterator it = g.getVertices().iterator();
        while(it.hasNext()){
            n = (GeneNode)it.next();
            if(n.nodeId.equals("208")){
                break;
            }
        }
        fromStartingNode(n);
        fromStartingNode(n);

        /*for (final GeneNode node : g.getVertices()) {
            visited.put(node, false);
            if (checkFitness(node)) {
            /*futures.add(pool.submit(new Callable<Result>() {

                @Override
                public Result call() throws Exception {
                    return fromStartingNode(node);
                }
            }));
            //fromStartingNode(node);
                counter++;
        }
    }*/

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
        //if(visited.get(startingNode)){
         //   return null;
        //}
        //slow and unnecessary, can be final
        //KPMGraph copy = new KPMGraph(this.g);
        RandomSubgraph solution = new RandomSubgraph();
        // starting node can be addedKPMGraph kpmGraph
        //GeneNode st = new GeneNode(startingNode);
        ArrayList<GeneNode> candidates = new ArrayList<GeneNode>();
        HashSet<GeneNode> queried = new HashSet<GeneNode>();
        candidates.addAll(copy.getNeighbors(startingNode));
        candidates.sort(new Comparator<GeneNode>() {
            @Override
            public int compare(GeneNode o1, GeneNode o2) {
                return o1.nodeId.compareTo(o2.nodeId);
            }
        });

        if (solution.addVertex(startingNode)) {
            //visited.put(startingNode, true);
            queried.add(startingNode);

            boolean condition = true;
            while (condition) {
                ArrayList<GeneNode> checked = new ArrayList<GeneNode>();
                HashSet<GeneNode> temp = new HashSet<GeneNode>();
                for (GeneNode g : candidates) {
                    if (checkFitness(g)) {
                        solution.addVertex(g);
                        if (checkFitness(solution)) {
                            // Add edges for newly created node
                            System.out.println(g.nodeId);
                            for (GeneEdge e : copy.getOutEdges(g)) {
                                if (solution.containsVertex(copy.getEndpoints(e).getFirst()) &&
                                        solution.containsVertex(copy.getEndpoints(e).getSecond())) {
                                    solution.addEdge(e, new Pair<GeneNode>(copy.getEndpoints(e)), EdgeType.UNDIRECTED);
                                }
                            }
                            temp.addAll(copy.getNeighbors(g));
                        } else {
                            solution.removeVertex(g);
                        }

                    }
                    /*312
                    699
                    1425
                    616
                    258
                    197
                    37
                    0
                    208	50
                    312
                    699
                    1425
                    616
                    258
                    197
                    37
                    0*/
                    checked.add(g);
                    queried.add(g);
                    //visited.put(g, true);
                }
                //System.out.println(temp.size());
                for (GeneNode tmpN : temp) {
                    if (!queried.contains(tmpN)) {
                        candidates.add(tmpN);
                    }
                }
                candidates.removeAll(checked);
                if(!checkFitness(solution)){
                    condition = false;
                }
                if(candidates.size()==0){
                    condition=false;
                }
                candidates.sort(new Comparator<GeneNode>() {
                    @Override
                    public int compare(GeneNode o1, GeneNode o2) {
                        return o1.nodeId.compareTo(o2.nodeId);
                    }
                });


            }
        }
        System.out.println(startingNode.nodeId+"\t"+solution.getVertices().size());
        return solution;
    }

    private boolean checkFitness(GeneNode n){
        boolean fit = false;
        // TODO: currently a random p value is chosen
        String akey = n.getAveragePvalue().keySet().
                toArray(new String[n.getAveragePvalue().keySet().size()])[0];
        if((n.getAveragePvalue().get(akey)-0.05)<=0.00000000000000001){
            fit = true;
        }
        else{
            fit = false;
        }
        return fit;
    }

    /*
    * compare combined p value of the current subnetwork to the fdr of the random networks of that size.
     */
    private boolean checkFitness(RandomSubgraph sg){
        boolean fit = false;
        // TODO insert individual threshold for network size here
        if(sg.getVertices().size()>=dg.getThresholds().length){
            return false;
        }
        // refresh P value of subnetwork
        sg.calculatePvalFisher();
        // subnetwork pvalue must be smaller than threshold
        if(sg.getPval()- dg.getThreshold(sg.getVertexCount())<0.0000000000000001){
            fit = true;
        }
        else{
            fit = false;
        }
        return fit;
    }



    private void mutate(){

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

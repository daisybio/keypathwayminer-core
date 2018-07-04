package dk.sdu.kpm.algo.fdr;

import dk.sdu.kpm.KPMSettings;
import dk.sdu.kpm.taskmonitors.IKPMTaskMonitor;
import dk.sdu.kpm.graph.GeneNode;
import dk.sdu.kpm.graph.KPMGraph;
import dk.sdu.kpm.graph.Result;
import dk.sdu.kpm.algo.glone.*;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Greedy implements Serializable {

    private int k;

    private Map<String, Integer> l_map;

    private KPMGraph g;

    public List<Result> allSolutions;

    public IKPMTaskMonitor taskMonitor;

    private volatile KPMSettings kpmSettings;

    public Greedy(KPMGraph g, IKPMTaskMonitor taskMonitor, KPMSettings settings) {
        this.g = g;
        this.taskMonitor = taskMonitor;
        this.k = settings.GENE_EXCEPTIONS;
        this.l_map = settings.CASE_EXCEPTIONS_MAP;
        this.kpmSettings = settings;
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
        for (final GeneNode node : g.getVertices()) {
            if (checkFitness(node)) {
            futures.add(pool.submit(new Callable<Result>() {

                @Override
                public Result call() throws Exception {
                    return fromStartingNode(node);
                }
            }));
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
                    double completed = (double)nodesComputed / (double)numV;
                    taskMonitor.setProgress(completed);
                }
            } catch (InterruptedException ex) {
                Logger.getLogger(Greedy.class.getName()).log(Level.SEVERE, null, ex);
            } catch (ExecutionException ex) {
                Logger.getLogger(Greedy.class.getName()).log(Level.SEVERE, null, ex);
            }

        }
        pool.shutdown();
        return toReturn;
    }

    private Result fromStartingNode(GeneNode startingNode) {
        RandomSubgraph s = new RandomSubgraph(this.g);
        g.getNeighbors(startingNode);

        return solution;
    }

    private boolean checkFitness(GeneNode n){
        boolean fit = false;
        // TODO: currently a random p value is chosen
        String akey = n.getPvalue().keySet().toArray(new String[n.getPvalue().keySet().size()])[0];
        if((n.getPvalue().get(akey)-0.05)<=0.000001){
            fit = true;
        }
        else{
            fit = false;
        }
        return fit;
    }

    private boolean checkFitness(Subgraph sg){

        return false;
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

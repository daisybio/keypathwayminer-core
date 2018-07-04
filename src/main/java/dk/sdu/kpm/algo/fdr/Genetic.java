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

public class Genetic implements Serializable {

    private int k;

    private Map<String, Integer> l_map;

    private KPMGraph g;

    public List<Result> allSolutions;

    public IKPMTaskMonitor taskMonitor;

    private volatile KPMSettings kpmSettings;

    public Genetic(KPMGraph g, IKPMTaskMonitor taskMonitor, KPMSettings settings) {
        this.g = g;
        this.taskMonitor = taskMonitor;
        this.k = settings.GENE_EXCEPTIONS;
        this.l_map = settings.CASE_EXCEPTIONS_MAP;
        this.kpmSettings = settings;
    }

    public List<Result> getResults() {
        return allSolutions;
    }

    public List<Result> runGenetic() {
        // this.g = g;
        List<Result> toReturn = new ArrayList<Result>();
        int nodesComputed = 0;
        int numV = g.getVertexCount();


        ExecutorService pool = Executors.newFixedThreadPool(kpmSettings.NUMBER_OF_PROCESSORS);

        List<Future<Result>> futures = new LinkedList<Future<Result>>();


        // all nodes are possible entry points
        for (final GeneNode node : g.getVertices()) {
            futures.add(pool.submit(new Callable<Result>() {

                @Override
                public Result call() throws Exception {
                    return fromStartingNode(node);
                }
            }));
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
                Logger.getLogger(Genetic.class.getName()).log(Level.SEVERE, null, ex);
            } catch (ExecutionException ex) {
                Logger.getLogger(Genetic.class.getName()).log(Level.SEVERE, null, ex);
            }

        }
        pool.shutdown();
        return toReturn;
    }

    private Result fromStartingNode(GeneNode startingNode) {
        Subgraph solution = new Subgraph(kpmSettings);


        return solution;
    }

    private void fitnessTracker(){

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


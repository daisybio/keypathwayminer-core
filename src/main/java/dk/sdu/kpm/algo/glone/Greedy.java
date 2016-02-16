package dk.sdu.kpm.algo.glone;

import dk.sdu.kpm.KPMSettings;
import dk.sdu.kpm.taskmonitors.IKPMTaskMonitor;
import dk.sdu.kpm.graph.GeneNode;
import dk.sdu.kpm.graph.KPMGraph;
import dk.sdu.kpm.graph.Result;

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
                Logger.getLogger(Greedy.class.getName()).log(Level.SEVERE, null, ex);
            } catch (ExecutionException ex) {
                Logger.getLogger(Greedy.class.getName()).log(Level.SEVERE, null, ex);
            }

        }
        pool.shutdown();
        return toReturn;
    }

    private Result fromStartingNode(GeneNode startingNode) {
        Subgraph solution = new Subgraph(kpmSettings);
        if (k == 0) {
            if (!solution.canAdd(startingNode)) {
                return null;
            }
        }

        solution.add(startingNode);
        HashSet<GeneNode> currentNeighbors = new HashSet<GeneNode>();
        currentNeighbors.addAll(g.getNeighbors(startingNode));


//        assert (solution.getNonDifferentiallyExpressedCases() <= l);

        while (true && !isCancelled()) {
            List<GeneNode> newNodes = new ArrayList<GeneNode>();
            double bestFitness = Double.MAX_VALUE;
            HashSet<GeneNode> validNeighbors = new HashSet<GeneNode>();
            for (GeneNode node: currentNeighbors) {
                if (solution.canAdd(node)) {
                    validNeighbors.add(node);
                }
            }
            currentNeighbors = validNeighbors;               
            if (currentNeighbors.isEmpty()) {
                break;
            }

            // choose the neighbor with the best fitness. If there are
            // several
            // neighbors with the same best fitness, choose one uniformly at
            // random
            for (GeneNode node : currentNeighbors) {
                if(isCancelled()){
                    break;
                }

                if (node.getHeuristicValue(kpmSettings.NODE_HEURISTIC_VALUE) < bestFitness) {
                    bestFitness = node.getHeuristicValue(kpmSettings.NODE_HEURISTIC_VALUE);
                    newNodes.clear();
                    newNodes.add(node);
                } else if (node.getHeuristicValue(kpmSettings.NODE_HEURISTIC_VALUE) == bestFitness) {
                    newNodes.add(node);
                }
            }

            if (newNodes.size() == 0) {
                break;
                //TODO: Figure out what to do here instead of throwing the exception:
                //throw new IllegalStateException("We did not pick a node...");
            }

            GeneNode newNode = newNodes.get(kpmSettings.R.nextInt(newNodes.size()));

            if (!solution.canAdd(newNode)) {
                break;
            }

            solution.add(newNode);

            updateNeighbors(solution, currentNeighbors, newNode);
        }

        // flag the most expensive nodes as exception nodes
        solution.flagExceptionNodes();

        return solution;
    }

    private void updateNeighbors(Subgraph solution,
            Set<GeneNode> currentNeighbors, GeneNode newNode) {
        currentNeighbors.remove(newNode);
        for (GeneNode node : g.getNeighbors(newNode)) {
            if (!solution.contains(node)) {
                currentNeighbors.add(node);
            }
        }
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

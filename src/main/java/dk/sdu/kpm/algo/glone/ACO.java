package dk.sdu.kpm.algo.glone;

import dk.sdu.kpm.KPMSettings;
import dk.sdu.kpm.taskmonitors.IKPMTaskMonitor;
import dk.sdu.kpm.graph.GeneNode;
import dk.sdu.kpm.graph.KPMGraph;
import dk.sdu.kpm.graph.Result;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class ACO implements Serializable {

    private KPMGraph g;

    private int iteration = 1;

    private int iterationsWithoutChange = 0;

    private double[] rhoExp = null;

    private List<Integer> fitnessInIterationList = new ArrayList<Integer>();

    private int currentBestFitness = 0;

    private IKPMTaskMonitor taskMonitor;

    public List<Result> allSolutions;

    private volatile KPMSettings kpmSettings;

    public ACO(KPMGraph g, IKPMTaskMonitor taskMonitor, KPMSettings settings) {
        this.kpmSettings = settings;
        this.g = g;
        this.taskMonitor = taskMonitor;
    }

    public List<Result> getResults() {
        return allSolutions;
    }

    public List<Result> runACO() {


        precomputeRhoExp();

        List<Result> solutions = new ArrayList<Result>();

        Collection<GeneNode> startingNodes = chooseStartingNodes(kpmSettings.NUM_STARTNODES);

        for (GeneNode n : startingNodes) {
            if(isCancelled()){
                break;
            }
            g.refreshGraph(kpmSettings); // reset all pheromone values


            if (!kpmSettings.ITERATION_BASED) {
                solutions.add(Collections.min(globalBestACOFromNode(n)));
            } else {
                solutions.add(Collections.min(iterationBestACOFromNode(n)));
            }
        }

        return solutions;
    }

    /**
     * Precomputes the values of (1 - RHO)^x and stores them in an array. Later,
     * when vaporating a pheromone that was last updated x steps ago, one can
     * simply multiplay with the value in rhoExp[x] instead of computing the
     * power anew.
     */
    private void precomputeRhoExp() {
        rhoExp = new double[g.getVertexCount()];
        rhoExp[0] = 1;

        for (int i = 1; i < rhoExp.length; i++) {
            rhoExp[i] = rhoExp[i - 1] * (1 - kpmSettings.RHO);
        }
    }

    /**
     * Since we have a big graph (several thousand nodes), but relatively small
     * solution sizes (< 100 nodes), our pheromones will usually evaporate too
     * fast when picking random starting nodes. This is why we heuristically
     * choose KPMParameters.NUM_STARTNODES from where we always begin our ACO. The
     * first node in the solution is therefore always fixed, which gives us a
     * smaller sub-graph on which our ACO runs, yielding more meaningful values
     * for the pheromones (i.e. not all pheromones are evaporated when we reach
     * them again)
     *
     * As heuristic, we use for each node the average number of differentially
     * expressed cases (=good cases) of its neighbors.
     *
     *
     *

     *
     * @param n the number of starting nodes to use
     * @return the n heuristically best starting nodes
     */
    private Collection<GeneNode> chooseStartingNodes(int n) {
        List<GeneNode> nodes = new ArrayList<GeneNode>();
        Subgraph aux = new Subgraph(kpmSettings);
        for (GeneNode node : g.getVertices()) {
            if (aux.canAdd(node)) {
                nodes.add(node);
            }
        }

        Collections.sort(nodes, new Comparator<GeneNode>() {

            @Override
            public int compare(GeneNode o1, GeneNode o2) {
                int result = -new Double(o1.getAverageNeighborExpression()).compareTo(o2.getAverageNeighborExpression());
                if (result == 0) {
                    return -1;
                } else {
                    return result;
                }
            }
        });
        if (nodes.size() < n) {
            return nodes;
        }
        return nodes.subList(0, n);
    }

    /**
     * Yields the results from an global-best ACO approach (meaning, the
     * pheromone values are always updated according to the globally best
     * available solution)
     *
     * @param startingNode The starting node which has to be in the solution
     * @return an unsorted list of solutions that contain startingNode
     */
    private List<Result> globalBestACOFromNode(GeneNode startingNode) {
        List<Result> solutions = new ArrayList<Result>();
        Subgraph bestSolution = null;
        iteration = 1;
        iterationsWithoutChange = 0;

        while (iterationsWithoutChange < kpmSettings.MAX_RUNS_WITHOUT_CHANGE && !isCancelled()) {
            Subgraph solution = buildSolution(startingNode, iteration);
            solution = kpmSettings.L_SEARCH.localSearch(solution, g, kpmSettings);

            if (bestSolution == null
                    || solution.getFitness() > bestSolution.getFitness()) {
                bestSolution = solution;
                iterationsWithoutChange = 0;
            } else {
                iterationsWithoutChange++;
            }

            // FOR EVALUATION PURPOSES ONLY
            if (kpmSettings.EVAL) {
                if (solution.getFitness() > currentBestFitness) {
                    currentBestFitness = solution.getFitness();
                }
                fitnessInIterationList.add(currentBestFitness);
            }
            // END EVAL

            updatePheromones(bestSolution, false);

            assert solution.isConnected(g);
            solutions.add(solution);
            solution.flagExceptionNodes();

            iteration++;
            if (!kpmSettings.IS_BATCH_RUN) {
                double completed = (double) iteration / (double) kpmSettings.MAX_ITERATIONS;
                taskMonitor.setProgress(completed);
            }

        }

        return solutions;
    }

    private List<Result> iterationBestACOFromNode(final GeneNode startingNode) {
        List<Result> toReturn = new ArrayList<Result>();
        Subgraph bestSolution = null;

        int iterationsWithoutChange = 0;
        iteration = 1;

        if (kpmSettings.MAX_RUNS_WITHOUT_CHANGE == Integer.MAX_VALUE
                && kpmSettings.MAX_ITERATIONS == Integer.MAX_VALUE) {
            System.out.println("Either maxrunswithoutchange or iterations must be set to a value != 0.");
            System.exit(-1);
        }

        ExecutorService pool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

        while (iterationsWithoutChange < kpmSettings.MAX_RUNS_WITHOUT_CHANGE
                && iteration <= kpmSettings.MAX_ITERATIONS && !isCancelled()) {
            Subgraph[] solutions = new Subgraph[kpmSettings.NUMBER_OF_SOLUTIONS_PER_ITERATION];
            List<Future<Subgraph>> futures = new LinkedList<Future<Subgraph>>();

            for (int j = 0; j < solutions.length; j++) {
                futures.add(pool.submit(new Callable<Subgraph>() {

                    @Override
                    public Subgraph call() throws Exception {
                        return buildSolution(startingNode, iteration);
                    }
                }));
            }

            Subgraph iterationBest = null;

            int j = 0;
            for (Future<Subgraph> f : futures) {

                // compute the solutions concurrently
                try {
                    solutions[j] = f.get();
                } catch (Exception e) {
                    e.printStackTrace();
                    continue;
                }

                if (iterationBest == null
                        || solutions[j].getFitness() > iterationBest.getFitness()) {
                    iterationBest = solutions[j];
                }
                j++;
            }

            iterationBest = kpmSettings.L_SEARCH.localSearch(iterationBest, g, kpmSettings);

            if (bestSolution == null
                    || iterationBest.getFitness() > bestSolution.getFitness()) {
                bestSolution = iterationBest;
                iterationsWithoutChange = 0;
            } else {
                iterationsWithoutChange++;
            }

            updatePheromones(iterationBest, true);

            assert iterationBest.isConnected(g);
            toReturn.add(iterationBest);
            iterationBest.flagExceptionNodes();

            iteration++;
            if (!kpmSettings.IS_BATCH_RUN) {
                double completed = (double) iteration / (double) kpmSettings.MAX_ITERATIONS;
                taskMonitor.setProgress(completed);
            }

        }

        pool.shutdown();
        return toReturn;
    }

    private void updatePheromones(Subgraph bestSolution,
                                  boolean addFitnessToPheromone) {
        for (GeneNode n : bestSolution) {
            if(isCancelled()){
                return;
            }

            double currentPheromone = n.getPheromone()
                    * powRho((iteration - 1)
                    - n.getLastIterationPheromoneUpdated());

            if (currentPheromone < kpmSettings.TAU_MIN) {
                currentPheromone = kpmSettings.TAU_MIN;
            }

            if (!addFitnessToPheromone) {
                currentPheromone += kpmSettings.RHO;
            } else {
                currentPheromone += (1 - (1.0 / bestSolution.getFitness()));
            }

            if (currentPheromone > 1 - kpmSettings.TAU_MIN) {
                currentPheromone = 1 - kpmSettings.TAU_MIN;
            }

            n.setPheromone(currentPheromone, kpmSettings.TAU_MIN);
            n.setLastIterationPheromoneUpdated(iteration);
        }
        // double rhoHat = 0;
        //
        // switch (KPMParameters.RHO_DECAY) {
        // case CONSTANT:
        // rhoHat = KPMParameters.RHO;
        // break;
        //
        // case QUADRATIC:
        // rhoHat = ((0.5 - KPMParameters.RHO) / (KPMParameters.MAX_RUNS_WITHOUT_CHANGE *
        // KPMParameters.MAX_RUNS_WITHOUT_CHANGE))
        // * iterationsWithoutChange
        // * iterationsWithoutChange
        // + KPMParameters.RHO;
        // break;
        //
        // case LINEAR:
        // rhoHat = (0.5 - KPMParameters.RHO) / KPMParameters.MAX_RUNS_WITHOUT_CHANGE
        // * iterationsWithoutChange + KPMParameters.RHO;
        // break;
        //
        // case EXPONENTIAL:
        // rhoHat = KPMParameters.RHO
        // * Math.pow(Math.E, Math.log(0.5 / KPMParameters.RHO)
        // / KPMParameters.MAX_RUNS_WITHOUT_CHANGE);
        // break;
        // }
        //
        // for (GeneNode n : g.getVertices()) {
        // double currentPheromone = n.getPheromone() * rhoHat;
        //
        // if (currentPheromone < KPMParameters.TAU_MIN) {
        // currentPheromone = KPMParameters.TAU_MIN;
        // }
        //
        // if (bestSolution.contains(n)) {
        // if (!addFitnessToPheromone) {
        // currentPheromone += rhoHat;
        // } else {
        // currentPheromone += (1 - (1.0 / bestSolution.getFitness()));
        // }
        //
        // if (currentPheromone > 1 - KPMParameters.TAU_MIN) {
        // currentPheromone = 1 - KPMParameters.TAU_MIN;
        // }
        // }
        // n.setPheromone(currentPheromone);
        // n.setLastIterationPheromoneUpdated(iteration);
        // }
    }

    /**
     * Constructs one particular solution from a given starting node.
     *
     * @param n
     * @param iteration
     * @return
     */
    private Subgraph buildSolution(GeneNode n, int iteration) {
        Subgraph solution = new Subgraph(kpmSettings);
        solution.add(n);
        Set<GeneNode> currentNeighbors = new HashSet<GeneNode>(
                g.getNeighbors(n));

        while (addNodeToSolution(solution, currentNeighbors, iteration) && !isCancelled());

        return solution;
    }

    private boolean addNodeToSolution(Subgraph currentSolution,
                                      Set<GeneNode> currentNeighbors, int iteration) {

        // we iterate over all neighbors, yielding their probability of getting
        // them.
        double totalProbability = 0.0;
        Map<GeneNode, Double> probabilities = new HashMap<GeneNode, Double>();

        for (GeneNode neighbor : currentNeighbors) {
            if(isCancelled()){
                return false;
            }
            if (currentSolution.canAdd(neighbor)) {
                double neighborProbability = computeProbability(neighbor,
                        iteration);
                probabilities.put(neighbor, neighborProbability);
                totalProbability += neighborProbability;
            }
        }

        // System.out.println("Total prob: " + totalProbability);
        if (totalProbability == 0.0) // can't add more nodes
        {
            return false;
        }

        // next, we draw a random number and chose the new node
        double pick = kpmSettings.R.nextDouble() * totalProbability;
        GeneNode newNode = null;
        for (GeneNode neighbor : probabilities.keySet()) {
            if(isCancelled()){
                return false;
            }
            double p = probabilities.get(neighbor);
            if (pick > p) {
                pick -= p;
            } else {
                newNode = neighbor;
                break;
            }
        }

        assert newNode != null;

        currentSolution.add(newNode);
        currentSolution.updateNeighbors(currentNeighbors, newNode, g);

        return true;
    }

    /**
     * Computes the probability that a node is chosen.
     *
     * @param neighbor the node to be added
     * @param iteration
     * @param iteration in which iteration this algorithm is
     * @return the probability (does not have to be normalized to 1.0)
     */
    private double computeProbability(GeneNode neighbor, int iteration) {
        // add +1 in case it has 0 non-differentially expressed cases
        double weight = 1.0 / (neighbor.getHeuristicValue(kpmSettings.NODE_HEURISTIC_VALUE) + 1);

        // double pheromone = neighbor.getPheromone();
        // assert pheromone >= KPMParameters.TAU_MIN;
        // assert pheromone <= (1 - KPMParameters.TAU_MIN);

        double pheromone = neighbor.getPheromone()
                * powRho((iteration - 1)
                - neighbor.getLastIterationPheromoneUpdated());

        if (pheromone < kpmSettings.TAU_MIN) {
            pheromone = kpmSettings.TAU_MIN;
        } else if (pheromone > 1 - kpmSettings.TAU_MIN) {
            throw new IllegalStateException(
                    "Pheromone was too big although this should not happen.");
        }

        return tradeOff(pheromone, weight);
    }

    private double tradeOff(double pheromone, double weight) {
        // we don't want the whole term to be 0
        if (weight <= 0 || pheromone <= 0) {
            throw new IllegalArgumentException("weight or pheromone was 0.");
        }

        if (kpmSettings.MULTIPLICATIVE_TRADEOFF) {
            return pow(pheromone, kpmSettings.ALPHA) * pow(weight, kpmSettings.BETA);
        } else {
            return kpmSettings.ALPHA * pheromone + kpmSettings.BETA * weight;
        }
    }

    private double pow(double a, double b) {
        long c = Math.round(b);
        if (c != b) {
            return Math.pow(a, b);
        }

        double response = 1;

        for (int i = 0; i < c; i++) {
            response *= a;
        }

        return response;
    }

    /**
     * Pre-computes (1-RHO)^x to save time. For x>N, this method returns
     * (1-RHO)^N, which is almost always very very small such that it doesn't
     * make a difference anyhow
     *
     * @param exponent
     * @return
     */
    private double powRho(int exponent) {
        if (exponent < 0) {
            System.out.println(exponent);
            throw new IllegalArgumentException();
        }
        if (exponent > rhoExp.length - 1) {
            System.out.println("Incorrect Exponent?");
            return rhoExp[rhoExp.length - 1];
        }

        return rhoExp[exponent];
    }

    /**
     * After the ACO ran, this method can be called to obtain the best fitness
     * in a given iteration. This is for evaluation purposes.
     *
     * @return The list that contains at the i-th position the best fitness in
     * step i+1.
     */
    public List<Integer> getFitnessInIterationList() {
        return fitnessInIterationList;
    }

    private volatile boolean isCancelled = false;

    private synchronized boolean isCancelled(){
        return this.isCancelled;
    }

    public synchronized void cancel() {
        this.isCancelled = true;
        if(kpmSettings.L_SEARCH != null){
            kpmSettings.L_SEARCH.cancel();
        }
//        if(pool != null){
//            pool.shutdownNow();
//        }
    }
}

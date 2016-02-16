package dk.sdu.kpm.algo.glone;

import dk.sdu.kpm.KPMSettings;
import dk.sdu.kpm.taskmonitors.IKPMTaskMonitor;
import dk.sdu.kpm.graph.GeneNode;
import dk.sdu.kpm.graph.KPMGraph;
import dk.sdu.kpm.graph.Result;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Optimal implements Serializable {

    private KPMGraph g = null;

    private IKPMTaskMonitor taskMonitor;

    public List<Result> allSolutions;
    
    private volatile KPMSettings kpmSettings;

    public List<Result> getResults() {
        return allSolutions;
    }

    public Optimal(KPMGraph input, IKPMTaskMonitor taskMonitor, KPMSettings settings) {
        g = new KPMGraph(input);
        this.taskMonitor = taskMonitor;
        this.kpmSettings = settings;
    }

    /**
     * This algorithm calculates the optimal solution. It employs a
     * branch-and-bound-method. As a lower bound, the best current solution is
     * used. To compute the upper bound, the algorithm proceeds as follows:
     * Assume you are allowed to have l case-exc. in your solution, and you
     * already have l-x in your working subgraph. Then, the algorithm determines
     * how many more steps it can do without crossing the case exception limit.
     * This amount of steps, y, is used to create the y-neighborhood of the
     * current subgraph. In this neighborhood, nodes are added until the case
     * exception limit is crossed.
     *
     * @return The optimal solution.
     */
    public List<Result> runOptimal() {
        //g = new KPMGraph(input);
        if (! kpmSettings.IS_BATCH_RUN) {
            taskMonitor.setStatusMessage("Performing preprocessing...");
        }
        Subgraph opt = (Subgraph) Collections.min((new Greedy(g, taskMonitor, kpmSettings)).runGreedy());

        g.setAllNodesValid();
        int lowerBound = opt.getFitness();
        if (! kpmSettings.IS_BATCH_RUN) {
            taskMonitor.setStatusMessage("Extracting pathways...");
        }
//        System.out.println("greedy: " + lowerBound);

        int count = 0;
        int numSteps = g.getVertexCount();

        while (true && !isCancelled()) {
            // System.out.println("Step " + ++count + " of " + numSteps);
            // works b/c this vertex is removed in the next run
            GeneNode startingVertex = g.getVertices().iterator().next();

            Subgraph init = new Subgraph(kpmSettings);
            if (init.canAdd(startingVertex)) {
                init.add(startingVertex);
                Set<GeneNode> currentNeighbors = new HashSet<GeneNode>();
                currentNeighbors.addAll(g.getNeighbors(startingVertex));

                Subgraph best = branchSolution(init, currentNeighbors,
                        Collections.<GeneNode>emptySet(), lowerBound, g, kpmSettings);

                if (best != null && best.getFitness() > lowerBound) {
                    opt = best;
                    lowerBound = best.getFitness();
                }
            }

            g.removeVertex(startingVertex);

            if (g.getVertices().isEmpty()) {
                break;
            }
            count++;
            if (! kpmSettings.IS_BATCH_RUN) {
                double completed = (double) count / (double) numSteps;
                taskMonitor.setProgress(completed);
            }

        }
        
        if (! kpmSettings.IS_BATCH_RUN) {
            taskMonitor.setProgress(99);
        }
        opt.flagExceptionNodes();
        return Collections.singletonList((Result) opt);
    }

    /**
     * Takes an existing solution with x<=l case exceptions. Branches the
     * solution to add more nodes.
     *
     *
     *

     *
     * @param init What solution to start with
     * @param currentNeighbors all nodes that can be reached from init within
     * exactly 1 step
     * @param visitedNodes all Nodes that are already covered by another branch,
     * such that we can ignore them
     * @param lowerBound the current highest known lower bound
     * @param g the graph on which we work
     * @return false, if the solution's upper bound fitness does not match the
     * current lower bound fitness. True otherwise.
     */
    public Subgraph branchSolution(Subgraph init,
                                          Set<GeneNode> currentNeighbors, Set<GeneNode> visitedNodes,
                                          int lowerBound, KPMGraph g, KPMSettings kpmSettings) {
        assert init.isConnected(g);

        Subgraph bestSolution = init;
        if (lowerBound < bestSolution.getFitness()) {
            lowerBound = bestSolution.getFitness();
        }

        Set<GeneNode> visitedNodesFromHere = new HashSet<GeneNode>(visitedNodes);

        for (GeneNode newNode : currentNeighbors) {
            if(isCancelled()){
                break;
            }

            if (!init.canAdd(newNode) || visitedNodes.contains(newNode)) {
                continue;
            }

            // make a deep copy
            Subgraph branch = new Subgraph(kpmSettings);
            branch.addAll(init);
            branch.add(newNode);
            Set<GeneNode> currentNodesNeighbors = new HashSet<GeneNode>(
                    currentNeighbors);

            branch.updateNeighbors(currentNodesNeighbors, newNode, g);

            // Bound.
            if (boundSolution(branch, currentNodesNeighbors, g) <= lowerBound) {
                continue;
            }

            // Branch.
            Subgraph branchedSolution = branchSolution(branch,
                    currentNodesNeighbors, visitedNodesFromHere, lowerBound, g, kpmSettings);

            if (lowerBound < branchedSolution.getFitness()) {
                lowerBound = branchedSolution.getFitness();
                bestSolution = branchedSolution;
            }

            visitedNodesFromHere.add(newNode);
        }

        return bestSolution;
    }

    // /**
    // * Takes an existing solution with x<=l case exceptions. Branches the
    // * solution to add more nodes.
    // *
    // * @param init
    // * What solution to start with
    // * @return false, if the solution's upper bound fitness does not match the
    // * current lower bound fitness. True otherwise.
    // */
    // public Subgraph branchSolution(Subgraph init, GeneNode newNode,
    // Set<GeneNode> currentNeighbors, int lowerBound) {
    // if (init.getNonDifferentiallyExpressedCases() > l) {
    // return null;
    // }
    //
    // // make a deep copy
    // Subgraph branch = new Subgraph();
    // branch.addAll(init);
    // branch.add(newNode);
    // currentNeighbors = new HashSet<GeneNode>(currentNeighbors);
    //
    // updateNeighbors(branch, currentNeighbors, newNode);
    //
    // // Bound.
    // if (boundSolution(branch, currentNeighbors) < lowerBound) {
    // return null;
    // }
    //
    // Subgraph bestSolution = init;
    // if (lowerBound <= bestSolution.getFitness())
    // lowerBound = bestSolution.getFitness();
    //
    // // Branch.
    // for (GeneNode node : currentNeighbors) {
    // assert (!branch.contains(node));
    //
    // Subgraph newSol = branchSolution(branch, node, currentNeighbors,
    // lowerBound);
    //
    // if (newSol != null && newSol.getFitness() > lowerBound) {
    // lowerBound = newSol.getFitness();
    // bestSolution = newSol;
    // }
    //
    // }
    //
    // return bestSolution;
    // }
    /**
     * Takes a solution with x<l case exceptions. Upper bounds the maximal
     * fitness this solution can obtain.
     *
     *
     *

     *
     * @param g the solution to upper bound
     * @return the maximal fitness this solution can get when adding k-x more
     * exception nodes (does not have to be tight)
     */
//	private static int boundSolution2(Subgraph toBound,
//			Set<GeneNode> currentNeighbors, KPMGraph g) {
//		if (toBound.getNonDifferentiallyExpressedCases() >= l
//				|| currentNeighbors.isEmpty())
//			return toBound.getFitness();
//
//		// First, we have to identify all nodes that can be reached from this
//		// solution
//
//		List<Integer> diffCasesArray = toBound.getCasesArray();
//
//		Set<GeneNode> neighbors = new HashSet<GeneNode>();
//		Set<GeneNode> stepsFromSolution = new HashSet<GeneNode>();
//		Queue<GeneNode> neighborRing = new PriorityQueue<GeneNode>();
//		Queue<GeneNode> nextNeighborRing = new PriorityQueue<GeneNode>();
//
//		neighborRing.addAll(currentNeighbors);
//		stepsFromSolution.addAll(toBound);
//
//		int toAdd = Collections.max(neighborRing).getHeuristicValue();
//		int lastIndex = Collections.binarySearch(diffCasesArray, toAdd,
//				new Comparator<Integer>() {
//					@Override
//					public int compare(Integer o1, Integer o2) {
//						return -o1.compareTo(o2);
//					}
//				});
//		if (lastIndex < 0)
//			lastIndex = -lastIndex - 1;
//		diffCasesArray.add(lastIndex, toAdd);
//
//		if (Subgraph.getNonDifferentiallyExpressedCasesFromList(diffCasesArray) > l)
//			return toBound.getFitness();
//
//		while (Subgraph
//				.getNonDifferentiallyExpressedCasesFromList(diffCasesArray) < l
//				&& !neighborRing.isEmpty()) {
//			stepsFromSolution.addAll(neighborRing);
//			neighbors.addAll(neighborRing);
//
//			while (!neighborRing.isEmpty()) {
//				for (GeneNode possNextNeighbor : g.getNeighbors(neighborRing
//						.poll()))
//					if (!stepsFromSolution.contains(possNextNeighbor))
//						nextNeighborRing.add(possNextNeighbor);
//			}
//
//			neighborRing = nextNeighborRing;
//			nextNeighborRing = new PriorityQueue<GeneNode>();
//			if (neighborRing.isEmpty())
//				break;
//
//			toAdd = Collections.max(neighborRing).getHeuristicValue();
//			lastIndex = Collections.binarySearch(diffCasesArray, toAdd,
//					new Comparator<Integer>() {
//						@Override
//						public int compare(Integer o1, Integer o2) {
//							return -o1.compareTo(o2);
//						}
//					});
//			if (lastIndex < 0)
//				lastIndex = -lastIndex - 1;
//			diffCasesArray.add(lastIndex, toAdd);
//
//		}
//
//		// Now, we check the all the nodes that can still be added.
//		diffCasesArray = toBound.getCasesArray();
//		List<GeneNode> neighborList = new ArrayList<GeneNode>(neighbors);
//		Collections.sort(neighborList);
//
//		while (!neighborList.isEmpty()) {
//			toAdd = neighborList.get(neighborList.size() - 1)
//					.getHeuristicValue();
//			lastIndex = Collections.binarySearch(diffCasesArray, toAdd,
//					new Comparator<Integer>() {
//						@Override
//						public int compare(Integer o1, Integer o2) {
//							return -o1.compareTo(o2);
//						}
//					});
//			if (lastIndex < 0)
//				// binarySearch gives a weird insertion point...
//				lastIndex = -lastIndex - 1;
//
//			diffCasesArray.add(lastIndex, toAdd);
//			neighborList.remove(neighborList.size() - 1);
//
//			if (Subgraph
//					.getNonDifferentiallyExpressedCasesFromList(diffCasesArray) > l) {
//				diffCasesArray.remove(lastIndex);
//				break;
//			}
//
//		}
//
//		return diffCasesArray.size();
//	}
    private static int boundSolution(Subgraph toBound,
            Set<GeneNode> currentNeighbors, KPMGraph g) {
        return Integer.MAX_VALUE;
    }



    private volatile boolean isCancelled = false;

    private synchronized boolean isCancelled(){
        if(isCancelled){
            System.out.println("Optimal has been cancelled");
        }
        return this.isCancelled;
    }

    public synchronized void cancel() {
        this.isCancelled = true;
        System.out.println("cancelled Optimal");
//        if(pool != null){
//            pool.shutdownNow();
//        }
    }
}

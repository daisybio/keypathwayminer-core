package dk.sdu.kpm.algo.glone;

import dk.sdu.kpm.Heuristic;
import dk.sdu.kpm.KPMSettings;
import dk.sdu.kpm.graph.GeneNode;
import dk.sdu.kpm.graph.KPMGraph;

import java.util.Collections;
import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.Set;


public enum LocalSearch {
	GREEDY1, GREEDY2, OPTIMAL, OFF;

    private volatile KPMSettings kpmSettings;
    
	/**
	 * Improves a given solution by local searching its neighborhood. For this,
	 * it removes one node and tries to add other nodes such that the solution
	 * gets better.
	 * 
	 * @param s
	 *            the solution to improve
	 * @return the solution which has been locally optimized
	 */
	public Subgraph localSearch(Subgraph s, KPMGraph g, KPMSettings settings) {
		if (this == LocalSearch.OFF || s.size() == 1)
			return s;
		
		this.kpmSettings = settings;
		// currently ignores depth
		Subgraph bestSolution = s;

		for (GeneNode n : s) {
			Subgraph clone = new Subgraph(kpmSettings);
			for (GeneNode node : s)
				if (!node.equals(n))
					clone.add(node);

			assert clone.size() == s.size() - 1;

			if (!clone.isConnected(g))
				continue;

			// compute current neighbors
			Set<GeneNode> currentNeighbors = new HashSet<GeneNode>();
			for (GeneNode inClone : clone)
				for (GeneNode potentialNeighbor : g.getNeighbors(inClone))
					if (!clone.contains(potentialNeighbor))
						currentNeighbors.add(potentialNeighbor);

			// must have neighbors since we just removed one node.. !
			assert !currentNeighbors.isEmpty();

			Subgraph newSol = null;

			switch (this) {
			case GREEDY1:
				newSol = localSearchGreedy1Step(clone, currentNeighbors, g);
				break;
			case GREEDY2:
				newSol = localSearchGreedy2Step(clone, currentNeighbors, g, kpmSettings.NODE_HEURISTIC_VALUE);
				break;
			case OPTIMAL:
				newSol = localSearchOptimalStep(clone, currentNeighbors, g);
				break;
			default:
			}

			if (newSol.getFitness() > bestSolution.getFitness())
				bestSolution = newSol;
		}

		return bestSolution;
	}

	/**
	 * Constructs an optimal subgraph from the given initial subgraph.
	 * 
	 * @param clone
	 *            the given subgraph, will be modified! (should be cloned
	 *            before)
	 * @param currentNeighbors
	 *            the neighbors of that subgraph
	 * @return the optimal subgraph that can be constructed from clone
	 */
	private Subgraph localSearchOptimalStep(Subgraph clone,
			Set<GeneNode> currentNeighbors, KPMGraph g) {

        return branchSolution(clone, currentNeighbors,
				Collections.<GeneNode> emptySet(), 0, g, kpmSettings);
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
    private Subgraph branchSolution(Subgraph init,
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

    private static int boundSolution(Subgraph toBound,Set<GeneNode> currentNeighbors, KPMGraph g) {
        return Integer.MAX_VALUE;
    }
	/**
	 * Greedily adds nodes to the solution until no new nodes are possible to
	 * add.
	 * 
	 * @param clone
	 *            the given subgraph, will be modified! (should be cloned
	 *            before)
	 * @param currentNeighbors
	 *            the neighbors of that subgraph
	 * @return
	 */
	private Subgraph localSearchGreedy1Step(Subgraph clone,
			Set<GeneNode> currentNeighbors, KPMGraph g) {

		while (!currentNeighbors.isEmpty()) {
			// max since they are in the wrong order
			GeneNode smallest = Collections.max(currentNeighbors);
			if (clone.canAdd(smallest)) {
				clone.add(smallest);

				// update neighbors
				clone.updateNeighbors(currentNeighbors, smallest, g);
			} else
				return clone;
		}

		return clone;
	}

	/**
	 * 2-Greedily adds nodes to the solution until no new nodes are possible to
	 * add. 2-Greedily means that it explores a depth of 2 steps instead of one
	 * step as with regular greedy algorithms.
	 * 
	 * @return
	 */
	private Subgraph localSearchGreedy2Step(Subgraph clone,
			Set<GeneNode> currentNeighbors, KPMGraph g, Heuristic node_heuristic_value) {
		while (!currentNeighbors.isEmpty()) {
			// for each neighbor, save the cost for (that neighbor +
			// cheapest neighborsNeighbor)
			Set<TwoNodes> costs = new HashSet<TwoNodes>();
			for (GeneNode n : currentNeighbors) {
				GeneNode minNeighbor = null;
				for (GeneNode neighborsNeighbor : g.getNeighbors(n))
					if (!clone.contains(neighborsNeighbor)
							&& !currentNeighbors.contains(neighborsNeighbor)
							&& (minNeighbor == null || neighborsNeighbor
									.getHeuristicValue(node_heuristic_value) < minNeighbor
									.getHeuristicValue(node_heuristic_value)))
						minNeighbor = neighborsNeighbor;

				if (minNeighbor != null)
					costs.add(new TwoNodes(n, minNeighbor, kpmSettings.NODE_HEURISTIC_VALUE));
			}

			// finally, add the two cheapest nodes in the current neighbors
			GeneNode smallest = Collections.max(currentNeighbors);
			currentNeighbors.remove(smallest);

			if (currentNeighbors.isEmpty()) {
				// do nothing ... is captured by call to 1-greedy
			} else {
				GeneNode secondSmallest = Collections.max(currentNeighbors);
				costs.add(new TwoNodes(smallest, secondSmallest, kpmSettings.NODE_HEURISTIC_VALUE));
			}

			currentNeighbors.add(smallest);

			// now, determine the minimum cost.
			TwoNodes minimum;
			try {
				minimum = Collections.min(costs);
			} catch (NoSuchElementException e) {
				return localSearchGreedy1Step(clone, currentNeighbors, g);
			}

			// now, add the nodes to the solution and repeat.
			if (!clone.canAdd(minimum.getN1()))
				return localSearchGreedy1Step(clone, currentNeighbors, g);
			else {
				clone.add(minimum.getN1());
				clone.updateNeighbors(currentNeighbors, minimum.getN1(), g);
			}

			if (!clone.canAdd(minimum.getN2()))
				return clone;
			else {
				clone.add(minimum.getN2());
				clone.updateNeighbors(currentNeighbors, minimum.getN2(), g);
			}
		}

		return localSearchGreedy1Step(clone, currentNeighbors, g);
	}


    private volatile boolean isCancelled = false;

    private synchronized boolean isCancelled(){
        return this.isCancelled;
    }

    public synchronized void cancel() {
        this.isCancelled = true;
    }
}

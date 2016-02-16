package dk.sdu.kpm.algo.ines;

import dk.sdu.kpm.KPMSettings;
import dk.sdu.kpm.taskmonitors.IKPMTaskMonitor;
import dk.sdu.kpm.graph.GeneEdge;
import dk.sdu.kpm.graph.Result;
import edu.uci.ics.jung.graph.SparseGraph;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class LComponentGraph extends SparseGraph<GeneCluster, GeneEdge> {

	private static final long serialVersionUID = 5229355419374288736L;

	private int k;

	private IKPMTaskMonitor taskMonitor;

	public List<Result> allSolutions;

	private volatile KPMSettings kpmSettings;



	public LComponentGraph(IKPMTaskMonitor taskMonitor, KPMSettings settings) {
		this.taskMonitor = taskMonitor;
		this.kpmSettings = settings;
		this.k = this.kpmSettings.GENE_EXCEPTIONS;
	}

	public List<Result> getResults() {
		return allSolutions;
	}

	public int getExceptionVertexCount() {
		int count = 0;
		for (GeneCluster n : getVertices())
			if (!n.isValid())
				count++;
		return count;
	}

	/**
	 * Takes possible solutions from a previous ACO iteration and updates the
	 * pheromones tau on the graph accordingly. First, the current pheromones on
	 * the vertices are evaporated: tau := (1 - rho) tau. Next, the pheromones
	 * are updated based on the fitness of the given solutions: tau := tau + rho
	 * * f(s) where the fitness is given by the number of non-exception vertices
	 * covered.
	 * 
	 * @param solutions
	 */
	public void updatePheromones(LCGSubgraph[] solutions) {
		// evaporation
		for (GeneCluster node : getVertices())
			if (!node.isValid())
				node.setPheromone((1 - kpmSettings.RHO) * node.getPheromone());

		// pheromone update
		for (LCGSubgraph solution : solutions) {
			int fitness = solution.getFitness();
			for (GeneCluster node : solution)
				if (!node.isValid())
					node.setPheromone(node.getPheromone() + kpmSettings.RHO
							* fitness);
		}

		// check bounds
		for (GeneCluster node : getVertices()) {
			if (node.isValid())
				continue;
			else if (node.getPheromone() > kpmSettings.N)
				node.setPheromone(kpmSettings.N);
			else if (node.getPheromone() < 1)
				node.setPheromone(1);
		}
	}

	/**
	 * Sets the fitness of all nodes to their respective weights.
	 */
	public void resetPheromones() {
		for (GeneCluster node : getVertices())
			if (!node.isValid())
				node.setPheromone(kpmSettings.N / ((double) 2));
	}

	/**
	 * 
	 * @return some exception vertex from the graph. Returns null, if no
	 *         exception vertex is in the graph.
	 */
	private GeneCluster getExceptionVertex() {
		for (GeneCluster n : getVertices())
			if (!n.isValid())
				return n;
		return null;
	}

	/**
	 * Constructs a single solution for the problem from the current graph and
	 * pheromones. It proceeds by picking a possible exc. neighbor with a
	 * probability proportional to their pheromone and weight. The balance
	 * between pheromone and weight is given by a tradeoff function.
	 * 
	 * @param k
	 *            the number of exception vertices we allow in our solution
	 * @return a possible solution (does not have to be the optimal one)
	 */
	public LCGSubgraph constructSingleSolution(int k) {
		LCGSubgraph solution = new LCGSubgraph(kpmSettings);
		HashSet<GeneCluster> currentExceptionNeighbors = new HashSet<GeneCluster>();
		resetFitnesses();

		// in the beginning, all exc-nodes are possible entry points
		for (GeneCluster node : getVertices())
			if (!node.isValid())
				currentExceptionNeighbors.add(node);

		for (int t = 1; t <= k; t++) {
			GeneCluster newNode = null;
			double totalProbability = 0;
			for (GeneCluster node : currentExceptionNeighbors) {
				assert !solution.contains(node);
				totalProbability += node.getProbability();
			}

			double hit = kpmSettings.R.nextDouble();

			// choose a neighbor randomly
			if (currentExceptionNeighbors.isEmpty())
				return solution;

			for (GeneCluster node : currentExceptionNeighbors) {
				if (hit < (node.getProbability() / totalProbability)) {
					newNode = node;
					break;
				} else {
					hit -= (node.getProbability() / totalProbability);
				}
			}
			if (newNode == null){
                break;
                //TODO: Figure out what to do here instead of throwing the exception:
                //throw new IllegalStateException("We did not pick a node...");
            }

			solution.add(newNode);

			updateExceptionNeighbors(solution, currentExceptionNeighbors,
					newNode);
		}
		return solution;
	}

	/**
	 * Updates the exception neighborhood of the current solution. Given the
	 * newNode, it checks the new neighbors acquired by this new node and either
	 * adds them to the solution, if they are valid; or adds them to the
	 * exception neighbors, if they are exception nodes.
	 * 
	 * @param solution
	 *            the solution whose neighborhood is to be updated; the newNode
	 *            should already be added
	 * @param currentExceptionNeighbors
	 *            the neighborhood of the old solution
	 * @param newNode
	 *            the newNode that was just added
	 */
	private void updateExceptionNeighbors(LCGSubgraph solution,
			Set<GeneCluster> currentExceptionNeighbors, GeneCluster newNode) {
		if (newNode.isValid() || !solution.contains(newNode))
			throw new IllegalArgumentException(
					"Wanted to update exception neighbors when given an invalid new node.");

		// In case this is our first node we empty the neighbor list
		if (solution.size() == 1)
			currentExceptionNeighbors.clear();

		// Update the neighborhood to be considered.
		currentExceptionNeighbors.remove(newNode);

		for (GeneCluster candidateNeighbor : getNeighbors(newNode)) {
			if (!candidateNeighbor.isValid()
					&& !solution.contains(candidateNeighbor))
				currentExceptionNeighbors.add(candidateNeighbor);

			else if (candidateNeighbor.isValid()
					&& !solution.contains(candidateNeighbor)) {
				for (GeneCluster candidateNeighbor2 : getNeighbors(candidateNeighbor)) {

					// Should not decrease fitness of newNode, but we don't
					// need its fitness anyhow since it already is in the
					// solution
					int oldfitness = candidateNeighbor2.getFitness();
					int weightToRemove = candidateNeighbor.getWeight();
					candidateNeighbor2.setFitness(oldfitness - weightToRemove);

					// don't add newNode the currentNeighbors!
					if (!newNode.equals(candidateNeighbor2))
						currentExceptionNeighbors.add(candidateNeighbor2);

				}

				// add valid neighbor nodes to solution
				solution.add(candidateNeighbor);
			}
		}
	}

	/**
	 * Reverts the changes updateExceptionNeighbor() does to the fitnesses of
	 * the exception nodes of the graph.
	 * 
	 * @param solution
	 *            The (connected) solution that does NOT contain oldNode, i.e.
	 *            the solution does not contain valid nodes that were added due
	 *            to oldNode
	 * @param oldNode
	 *            the exception node that we are throwing out of the solution
	 */
	private void revertExceptionNeighbors(LCGSubgraph solution,
			GeneCluster oldNode) {
		for (GeneCluster validNeighbor : getNeighbors(oldNode))
			if (validNeighbor.isValid() && !solution.contains(validNeighbor))
				for (GeneCluster excNeighbor : getNeighbors(validNeighbor))
					// should not increase fitness of oldNode, but it's okay
					// since we decreased it in the method
					// updateExceptionNeighbor()
					excNeighbor.setFitness(excNeighbor.getFitness()
							+ validNeighbor.getWeight());
	}

	private void resetFitnesses() {
		for (GeneCluster node : getVertices())
			if (!node.isValid())
				node.setFitness(node.getWeight());
	}

	/**
	 * Runs the ACO Algorithm. It constructs per iteration m solutions, and
	 * updates the pheromones according to the solutions before jumping into the
	 * next iteration.
	 * 
	 * @return the top solutions, ordered by their fitness
	 */
	public List<Result> ACO() {
		List<Result> toReturn = new ArrayList<Result>();

		if (k == 0)
			return biggestValidClusters();

		LCGSubgraph best = new LCGSubgraph(kpmSettings);
		int bestFitness = best.getFitness();
		int roundsWithoutChange = 0;
		int iterations = 0;

		if (kpmSettings.MAX_RUNS_WITHOUT_CHANGE == Integer.MAX_VALUE
				&& kpmSettings.MAX_ITERATIONS == Integer.MAX_VALUE) {
			System.out
			.println("Either maxrunswithoutchange or iterations must be set to a value != 0.");
			System.exit(-1);
		}

		ExecutorService pool = Executors.newFixedThreadPool(kpmSettings.NUMBER_OF_PROCESSORS);

		while (roundsWithoutChange <= kpmSettings.MAX_RUNS_WITHOUT_CHANGE
				&& iterations <= kpmSettings.MAX_ITERATIONS && !isCancelled()) {
			LCGSubgraph[] solutions = new LCGSubgraph[kpmSettings.NUMBER_OF_SOLUTIONS_PER_ITERATION];
			List<Future<LCGSubgraph>> futures = new LinkedList<Future<LCGSubgraph>>();

			for (int j = 0; j < solutions.length; j++)
				futures.add(pool.submit(new Callable<LCGSubgraph>() {

					@Override
					public LCGSubgraph call() throws Exception {
						return constructSingleSolution(k);
					}
				}));

			int j = 0;
			for (Future<LCGSubgraph> f : futures) {

				// compute the solutions concurrently
				try {
					solutions[j] = f.get();
				} catch (Exception e) {
					e.printStackTrace();
					continue;
				}

				if (solutions[j].getFitness() > bestFitness) {
					best = solutions[j];
					bestFitness = best.getFitness();
					roundsWithoutChange = 0;
				}
				j++;
			}
			roundsWithoutChange++;
			iterations++;

			toReturn.addAll(Arrays.asList(solutions));

			updatePheromones(solutions);
			if (! kpmSettings.IS_BATCH_RUN) {
				double completed = (double)iterations / (double) kpmSettings.MAX_ITERATIONS;
				taskMonitor.setProgress(completed);
			}
		}

		pool.shutdown();

		return toReturn;
	}

	private List<Result> biggestValidClusters() {
		List<LCGSubgraph> solutions = new ArrayList<LCGSubgraph>();

		for (GeneCluster node : getVertices())
			if (node.isValid()) {
				LCGSubgraph sol = new LCGSubgraph(kpmSettings);
				sol.add(node);
				solutions.add(sol);
			}

		Collections.sort(solutions);

		List<Result> toReturn = new ArrayList<Result>();
		toReturn.addAll(solutions);
		return toReturn;
	}

	/**
	 * Runs the ACO Algorithm until the desired fitness is reached. It
	 * constructs per iteration m solutions, and updates the pheromones
	 * according to the solutions before jumping into the next iteration.
	 * 
	 * @param k
	 *            the number of exception nodes allowed in a solution
	 * 
	 * @param fitness
	 *            desired fitness
	 * @return the number of iterations until reached
	 */
	public int ACOForFitness(int k, int fitness) {
		List<LCGSubgraph> toReturn = new ArrayList<LCGSubgraph>();

		resetPheromones();

		int i = 0;
		int currentFitness = 0;
		do {
			i++;
			LCGSubgraph[] solutions = new LCGSubgraph[kpmSettings.NUMBER_OF_SOLUTIONS_PER_ITERATION];
			for (int j = 0; j < solutions.length; j++) {
				solutions[j] = constructSingleSolution(k);
			}
			updatePheromones(solutions);

			toReturn.addAll(Arrays.asList(solutions));

			currentFitness = toReturn.get(0).getFitness();
		} while (i < kpmSettings.MAX_ITERATIONS && currentFitness < fitness);

		return i;
	}

	/**
	 * Runs a greedy version of the ACO algorithm. Might be used as
	 * initialization or as a measure for comparing solutions. The greedy
	 * algorithm adds in each step the exception node that brings the highest
	 * number of nodes to the subgraph. It tries every exception node as start
	 * node.
	 * 
	 * @return
	 */
	public List<Result> greedy() {

		double exceptionVertices = (double)getExceptionVertexCount();
		double nodesComputed = 0.0;

		if (k == 0) {
			// TODO for k=0 we don't return multiple solutions.
			if (! kpmSettings.IS_BATCH_RUN) {
				taskMonitor.setProgress(1.0);                        
			}
			return biggestValidClusters();
		}

		List<Result> toReturn = new ArrayList<Result>();

        ExecutorService pool = Executors.newFixedThreadPool(kpmSettings.NUMBER_OF_PROCESSORS);

		List<Future<LCGSubgraph>> futures = new LinkedList<Future<LCGSubgraph>>();

		// submit all threads
		for (final GeneCluster node : getVertices())
			// all exc-nodes are possible entry points
			if (!node.isValid()) {

				futures.add(pool.submit(new Callable<LCGSubgraph>() {

					@Override
					public LCGSubgraph call() throws Exception {

						return greedyFromStartingNode(node);
					}
				}));

			}

		// poll all threads
		for (Future<LCGSubgraph> f : futures)
			try {
				toReturn.add(f.get());
				nodesComputed++;
				if (! kpmSettings.IS_BATCH_RUN) {
					double completed = (double)nodesComputed / (double)exceptionVertices;
					taskMonitor.setProgress(completed);
				}
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		pool.shutdown();
		return toReturn;
	}

	private LCGSubgraph greedyFromStartingNode(GeneCluster startingNode) {
		if (startingNode.isValid())
			throw new IllegalArgumentException(
					"Greedy Solution must start with an exception-node.");

		resetFitnesses();
		LCGSubgraph solution = new LCGSubgraph(kpmSettings);
		solution.add(startingNode);

		HashSet<GeneCluster> currentExceptionNeighbors = new HashSet<GeneCluster>();
		updateExceptionNeighbors(solution, currentExceptionNeighbors,
				startingNode);

		for (int t = 1; t < k; t++) {
			List<GeneCluster> newNodes = new ArrayList<GeneCluster>();
			int maxFitness = -1;
			if (currentExceptionNeighbors.isEmpty())
				break;

			// choose the neighbor with the best fitness. If there are several
			// neighbors with the same best fitness, choose one uniformly at
			// random
			for (GeneCluster node : currentExceptionNeighbors)
				if (node.getFitness() > maxFitness) {
					maxFitness = node.getFitness();
					newNodes.clear();
					newNodes.add(node);
				} else if (node.getFitness() == maxFitness)
					newNodes.add(node);

			if (newNodes.size() == 0) {
                break;
                //TODO: Figure out what to do here instead of throwing the exception:
                //throw new IllegalStateException("We did not pick a node...");
            }
			GeneCluster newNode = newNodes.get(kpmSettings.R.nextInt(newNodes
					.size()));

			solution.add(newNode);

			updateExceptionNeighbors(solution, currentExceptionNeighbors,
					newNode);
		}

		return solution;
	}

	/**
	 * This algorithm calculates the optimal solution. It employs a
	 * branch-and-bound-method. As a lower bound, the best current solution is
	 * used. To compute the upper bound, the algorithm proceeds as follows:
	 * Assume you are allowed to have l gene-exc. in your solution, and you
	 * already have l-x in your working subgraph. Then, the algorithm determines
	 * all exception nodes that can be reached from the current subgraph in x
	 * steps, takes the x nodes with the highest weights and adds those
	 * together. This is an upper bound for the fitness this subgraph can
	 * achieve.
	 * 
	 * For k<4, greedy already yields the optimal solution, so it is referred to
	 * the greedy method.
	 * 
	 * @return The optimal solution.
	 */
	public List<Result> optimal() {
		// if (k < 4)
		// return greedy();
		if (! kpmSettings.IS_BATCH_RUN) {
			taskMonitor.setStatusMessage("Performing preprocessing...");
		}

		LCGSubgraph opt = (LCGSubgraph) greedy().get(0);
		if (! kpmSettings.IS_BATCH_RUN) {
			taskMonitor.setStatusMessage("Extracting pathways...");
		}
		resetFitnesses();
		int lowerBound = opt.getFitness();

		int count = 0;
		int numSteps = getExceptionVertexCount();
		GeneCluster startingVertex = getExceptionVertex();

		while (startingVertex != null && !isCancelled()) {
			// System.out.println("Step " + ++count + " of " + numSteps);
			assert !startingVertex.isValid();

			LCGSubgraph init = new LCGSubgraph(kpmSettings);

			LCGSubgraph best = branchSolution(init, startingVertex,
					new HashSet<GeneCluster>(), lowerBound);

			if (best != null && best.getFitness() > lowerBound) {
				opt = best;
				lowerBound = best.getFitness();
			}

			removeVertex(startingVertex);

			startingVertex = getExceptionVertex();
			count++;
			if (! kpmSettings.IS_BATCH_RUN) {
				double completed = (double)count / (double)numSteps;
				taskMonitor.setProgress(completed);
			}
		}
		if (! kpmSettings.IS_BATCH_RUN) {
			taskMonitor.setProgress(0.99);
		}
		return Collections.singletonList((Result) opt);
	}

	/**
	 * Takes an existing solution with x<=k exception nodes. Branches the
	 * solution to add more exception nodes.
	 * 
	 * @param init
	 *            What solution to start with
	 * @return false, if the solution's upper bound fitness does not match the
	 *         current lower bound fitness. True otherwise.
	 */
	private LCGSubgraph branchSolution(LCGSubgraph init, GeneCluster newNode,
			Set<GeneCluster> currentExceptionNeighbors, int lowerBound) {
		if (init.getNumExceptionNodes() >= k) {
			return init;
		}
		// make a deep copy
		LCGSubgraph branch = new LCGSubgraph(kpmSettings);
		branch.addAll(init);
		branch.add(newNode);
		currentExceptionNeighbors = new HashSet<GeneCluster>(
				currentExceptionNeighbors);

		updateExceptionNeighbors(branch, currentExceptionNeighbors, newNode);
		assert allExceptionNodes(currentExceptionNeighbors);

		// Bound.
		if (boundSolution(branch, currentExceptionNeighbors) < lowerBound) {
			// System.out.println("Depth: " + branch.getNumExceptionNodes());
			revertExceptionNeighbors(init, newNode);
			return null;
		}

		LCGSubgraph bestSolution = null;

		// Branch.
		for (GeneCluster node : currentExceptionNeighbors) {
			if (node.isValid())
				throw new IllegalStateException(
						"Current Exception Neighbors is in an illegal state.");
			assert (!branch.contains(node));

			LCGSubgraph newSol = branchSolution(branch, node,
					currentExceptionNeighbors, lowerBound);

			if (newSol != null && newSol.getFitness() > lowerBound) {
				lowerBound = newSol.getFitness();
				bestSolution = newSol;
			}

		}

		// updateExceptionNeighbors() messes up the fitnesses. Be sure to
		// revert the changes after closing a branch.
		revertExceptionNeighbors(init, newNode);

		return bestSolution;
	}

	/**
	 * Takes a solution with x<k exception nodes. Upper bounds the maximal
	 * fitness this solution can obtain.
	 * 
	 * @param toBound
	 *            the solution to upper bound
	 * @return the maximal fitness this solution can get when adding k-x more
	 *         exception nodes (does not have to be tight)
	 */
	private int boundSolution(LCGSubgraph toBound,
			Set<GeneCluster> currentExceptionNeighbors) {
		int x = toBound.getNumExceptionNodes();
		if (x >= k)
			return toBound.getFitness();

		// First, we have to identify all nodes that can be reached from this
		// solution via k-x steps over other exception nodes.

		Set<GeneCluster> neighbors = new HashSet<GeneCluster>();
		Set<GeneCluster> stepsFromSolution = new HashSet<GeneCluster>();
		Queue<GeneCluster> neighborRing = new PriorityQueue<GeneCluster>();
		Queue<GeneCluster> nextNeighborRing = new PriorityQueue<GeneCluster>();

		neighborRing.addAll(currentExceptionNeighbors);
		stepsFromSolution.addAll(toBound);

		for (int i = x; i < k; i++) {
			stepsFromSolution.addAll(neighborRing);
			neighbors.addAll(neighborRing);

			while (!neighborRing.isEmpty() && !isCancelled()) {
				for (GeneCluster possNextNeighbor : getNeighbors(neighborRing
						.poll())) {

                    if (isCancelled()) {
                        i = k;
                        break;
                    }

                    if (possNextNeighbor.isValid()
                            && !stepsFromSolution.contains(possNextNeighbor)) {
                        neighborRing.add(possNextNeighbor);
                        stepsFromSolution.add(possNextNeighbor);
                        neighbors.add(possNextNeighbor);
                    } else if (!stepsFromSolution.contains(possNextNeighbor)) {
                        nextNeighborRing.add(possNextNeighbor);
                    }
                }

			}

			neighborRing = nextNeighborRing;
			nextNeighborRing = new PriorityQueue<GeneCluster>();
		}

		// Now, we check the all the valid nodes that can still be added.
		int bestPossibleFitness1 = toBound.getFitness();
		int numExcNodes = 0;

		for (GeneCluster vn : neighbors)
			if (vn.isValid())
				bestPossibleFitness1 += vn.getWeight();
			else
				numExcNodes++;

		// account for the exception nodes that can be added
		bestPossibleFitness1 += Math.min(k - x, numExcNodes);

		// ***** We also check for the x-k best exception nodes that can be
		// added, since you can't beat their combined fitnesses.
		int bestPossibleFitness2 = toBound.getFitness();

		List<GeneCluster> neighborList = new ArrayList<GeneCluster>();
		neighborList.addAll(neighbors);
		Collections.sort(neighborList);

		for (int i = Math.max(neighborList.size() - k + x, 0); i < neighborList
				.size(); i++)
			if (!neighborList.get(i).isValid())
				bestPossibleFitness2 += neighborList.get(i).getFitness();

		return Math.min(bestPossibleFitness1, bestPossibleFitness2);
	}

	/**
	 * Decreases the fitnesses of all exception nodes around this valid node by
	 * the weight of this node.
	 * 
	 * @param validNode
	 *            the node whose weight is to be subtracted from all neighbors
	 */
	private void decreaseFitnessesAround(GeneCluster validNode) {
		for (GeneCluster excNeighbor : getNeighbors(validNode))
			excNeighbor.setFitness(excNeighbor.getFitness()
					- validNode.getWeight());
	}

	/**
	 * Increases the fitnesses of all exception nodes around this valid node by
	 * the weight of this node.
	 * 
	 * @param validNode
	 *            the node whose weight is to be subtracted from all neighbors
	 */
	private void increaseFitnessesAround(GeneCluster validNode) {
		for (GeneCluster excNeighbor : getNeighbors(validNode))
			excNeighbor.setFitness(excNeighbor.getFitness()
					+ validNode.getWeight());
	}

	private int boundSolutionBad(LCGSubgraph s) {
		return Integer.MAX_VALUE;
	}

	private boolean allExceptionNodes(Set<GeneCluster> nodes) {
		for (GeneCluster n : nodes)
			if (n.isValid())
				return false;
		return true;
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

package dk.sdu.kpm.algo.ines;

import dk.sdu.kpm.KPMSettings;
import dk.sdu.kpm.graph.GeneNode;
import dk.sdu.kpm.graph.KPMGraph;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


public class GeneCluster implements Comparable<GeneCluster>, Serializable {
	private Set<GeneNode> nodesInCluster = new HashSet<GeneNode>();

	private Stack<GeneNode> validNeighbors = new Stack<GeneNode>();

	private Stack<GeneNode> exceptionNeighbors = new Stack<GeneNode>();

	// This map is used to avoid creating several clusters that contain the same
	// node.
	private static Map<GeneNode, GeneCluster> excNodeToExcCluster = new HashMap<GeneNode, GeneCluster>();

	private boolean valid = false;

	// The weight of an exception node displays the sum of all the weights of
	// non-exc. nodes around it
	private int weight = -1;

	// The fitness of an exception node displays the sum of all the weights of
	// non-exc. nodes around it BUT the ones that are already in the current
	// solution. Equals the weight when the solution is empty.
	// We need to keep track of different Fitnesses for different threads...
	private Map<Thread, Integer> fitness = new ConcurrentHashMap<Thread, Integer>();

	private double pheromone;

	private int hashCode = -1;
	private boolean finalizedHashCode = false;

    private boolean MULTIPLICATIVE_TRADEOFF = false;
    private double ALPHA = 0;
    private double BETA = 0;

	private GeneCluster() {
	}

	/**
	 * Constructs a new GeneCluster that contains the given node. The cluster
	 * contains only non-exception nodes, consequently, all neighbors of the
	 * cluster are exception nodes.
	 * 
	 * @param node
	 *            the node that is to be in the cluster
	 * @param g
	 *            the graph from which we construct the clusters
	 * @return The cluster as described
	 */
	public static GeneCluster fromValidNode(GeneNode node, KPMGraph g, KPMSettings settings) {
		if (!node.isValid())
			throw new IllegalArgumentException("Given node has to be valid.");
		GeneCluster newCluster = new GeneCluster();

        newCluster.MULTIPLICATIVE_TRADEOFF = settings.MULTIPLICATIVE_TRADEOFF;
        newCluster.ALPHA = settings.ALPHA;
        newCluster.BETA = settings.BETA;

		newCluster.pheromone = settings.N / ((double) 2);
		newCluster.setValid(true);
		newCluster.addNode(node);
		GeneNode neighbor = newCluster.nextValidNeighbor(g);

		// check the neighborhood recursively to expand the current
		// valid cluster
		while (neighbor != null) {
			newCluster.addNode(neighbor);
			neighbor = newCluster.nextValidNeighbor(g);
		}
		newCluster.setWeight(newCluster.getNodesInCluster().size());
		return newCluster;
	}

	public void setWeight(int weight) {
		if (weight < 1)
			throw new IllegalArgumentException(
					"Tried to set a negative weight.");
		this.weight = weight;
	}

	/**
	 * Constructs a GeneCluster only containing one node, and it has to be an
	 * exception node.
	 * 
	 * @param node
	 * @return
	 */
	public static GeneCluster fromExceptionNode(GeneNode node) {
		if (node.isValid())
			throw new IllegalArgumentException("Node has to be exceptional.");

		GeneCluster excCluster = excNodeToExcCluster.get(node);

		if (excCluster == null) {
			excCluster = new GeneCluster();
			excCluster.addNode(node);
			excCluster.setValid(false);
			excNodeToExcCluster.put(node, excCluster);
		}
		return excCluster;
	}

	private void addNode(GeneNode n) {
		nodesInCluster.add(n);
	}

	/**
	 * Used to construct the Cluster. Searches for a neighbor node of the
	 * cluster that is not an exception node. Should only be called when the
	 * graph is not yet complete.
	 * 
	 * @param g
	 *            the network from which the cluster has to be build
	 * @return a neighbor not yet
	 */
	private GeneNode nextValidNeighbor(KPMGraph g) {
		if (!validNeighbors.empty())
			return validNeighbors.pop();

		// check for new neighbors and update stack
		for (GeneNode inhabitant : nodesInCluster)
			for (GeneNode potentialNeighbor : g.getNeighbors(inhabitant)) {
				if (!nodesInCluster.contains(potentialNeighbor))
					if (potentialNeighbor.isValid())
						validNeighbors.push(potentialNeighbor);
					else
						exceptionNeighbors.push(potentialNeighbor);
			}

		// in case there are no neighbors at all
		if (validNeighbors.empty()) {
			return null;
		} else
			return validNeighbors.pop();

	}

	private void setValid(boolean b) {
		valid = b;
	}

	public boolean isValid() {
		return valid;
	}

	public Set<GeneNode> getNodesInCluster() {
		return nodesInCluster;
	}

	public int getWeight() {
		return weight;
	}

	/**
	 * Returns the neighbors. Should only be called on a cluster containing
	 * valid nodes.
	 * 
	 * @return All exception neighbors of this cluster
	 */
	public Collection<GeneNode> getExceptionNeighbors() {
		if (!valid)
			throw new IllegalStateException(
					"The cluster that should return exception neighbors is an exception node itself.");

		return exceptionNeighbors;
	}

	// Profiler reveals => high cost of re-computing hashcode each time. Save
	// hashCode once the Cluster is complete.
	@Override
	public int hashCode() {
		if (finalizedHashCode)
			return hashCode;

		final int prime = 31;
		hashCode = 1;
		hashCode = prime * hashCode
				+ ((nodesInCluster == null) ? 0 : nodesInCluster.hashCode());
		return hashCode;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		GeneCluster other = (GeneCluster) obj;
		if (nodesInCluster == null) {
			if (other.nodesInCluster != null)
				return false;
		} else if (!nodesInCluster.equals(other.nodesInCluster))
			return false;
		return true;
	}

	public double getPheromone() {
		if (isValid())
			throw new IllegalStateException(
					"Called GetPheromone on a non-exception node.");

		return pheromone;
	}

	/**
	 * Sets the pheromone for this exception-vertex. Does NOT check the bounds.
	 * 
	 * @param d
	 */
	public void setPheromone(double d) {
		if (isValid())
			throw new IllegalStateException(
					"Called SetPheromone on a non-exception node.");
		pheromone = d;
	}

	/**
	 * Returns the (unnormalized) probability for this vertex to be picked.
	 * Basically consists of tradeoff(pheromone, fitness), where the tradeoff
	 * function is given in the KPMParameters parameters.
	 * 
	 * @return the unnormalized probability for this node to be picked. Has to
	 *         be normalized against the sum of probabilities of all nodes.
	 */
	public double getProbability() {
		if (getFitness() <= -1)
			throw new IllegalStateException("Fitness was -1!");

		return tradeOff(getPheromone(), getFitness());
	}

	private double tradeOff(double pheromone, int weight) {
		if (MULTIPLICATIVE_TRADEOFF) {
			// we don't want the whole term to be 0, so...
			if (weight == 0)
				weight = 1;

			return Math.pow(pheromone, ALPHA)
					* Math.pow(weight, BETA);
		}

		else
			return ALPHA * pheromone + BETA * weight;
	}

	public void setFitness(int fitness) {
		if (valid)
			throw new IllegalStateException(
					"Wanted to set fitness on valid node.");
		if (fitness < 1)
			throw new IllegalArgumentException(
					"Wanted to decrease fitness below 1: " + fitness);
		if (fitness > weight)
			throw new IllegalArgumentException(
					"Wanted to increase fitness above its weight.");

		this.fitness.put(Thread.currentThread(), fitness);
	}

	public int getFitness() {
		if (valid) {
			throw new IllegalStateException(
					"Wanted to get fitness on valid node.");
		}

		if (!fitness.containsKey(Thread.currentThread())) {
			System.out.println("Shouldn't happen!");
			fitness.put(Thread.currentThread(), weight);
		}

		return fitness.get(Thread.currentThread());
	}

	/**
	 * This method is called when the creation of the GeneCluster is (basically)
	 * done. We use this mechanism such that the HashCode doesn't have to be
	 * recomputed each time which is costly according to a profiler.
	 */
	public void finalizeHashCode() {
		finalizedHashCode = true;

		final int prime = 31;
		hashCode = 1;
		hashCode = prime * hashCode
				+ ((nodesInCluster == null) ? 0 : nodesInCluster.hashCode());
	}

	@Override
	public int compareTo(GeneCluster o) {
		if (valid && o.valid)
			return 0;
		else if (valid)
			return -1;
		else if (o.valid)
			return 1;

		else
			return (new Integer(getFitness()).compareTo(o.getFitness()));
	}
}

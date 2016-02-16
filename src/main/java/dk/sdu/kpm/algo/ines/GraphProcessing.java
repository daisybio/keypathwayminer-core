package dk.sdu.kpm.algo.ines;

import dk.sdu.kpm.taskmonitors.IKPMTaskMonitor;
import dk.sdu.kpm.KPMSettings;
import dk.sdu.kpm.graph.GeneEdge;
import dk.sdu.kpm.graph.GeneNode;
import dk.sdu.kpm.graph.KPMGraph;

import java.util.HashSet;
import java.util.Set;

public class GraphProcessing {
	private GraphProcessing() {
	}

	/**
	 * Implements Algorithm 2.1, which is defined as follows: Given a graph G,
	 * we derive the l-component Graph by contracting all components that are
	 * not exception nodes. I.e., for all v in a contracted component has to
	 * hold: the number of differentially not expressed cases is at most l. We
	 * label each vertex corresponding to a contracted component with the size
	 * of the original component. We call all vertices with more than l
	 * differentially not expressed nodes exception vertices. We label each
	 * exception vertex by the sum of the labels of the non-exception adjacent
	 * vertices.
	 * 
	 * @param g
	 *            The graph to be contracted
	 */
	public static LComponentGraph componentGraph(KPMGraph g, IKPMTaskMonitor taskMonitor, KPMSettings kpmSettings) {
		// Note that the parameter l is intrinsically given by the KPMNetwork
		// that already flagged its vertices to be exceptional or not.

		LComponentGraph lg = new LComponentGraph(taskMonitor, kpmSettings);
		Set<GeneNode> processedNodes = new HashSet<GeneNode>();

		for (GeneNode current : g.getVertices()) {
			if (processedNodes.contains(current))
				continue;

			if (current.isValid()) {
				GeneCluster newCluster = GeneCluster.fromValidNode(current, g, kpmSettings);
				processedNodes.addAll(newCluster.getNodesInCluster());

				lg.addVertex(newCluster);

				for (GeneNode exceptionNeighbor : newCluster
						.getExceptionNeighbors()) {

					GeneCluster excNode = GeneCluster
							.fromExceptionNode(exceptionNeighbor);

					lg.addEdge(new GeneEdge(), newCluster, excNode);

				}

			} else {
				// check up all the exception neighbors of that node.
				// Non-exceptional neighbors are added in the other case.
				GeneCluster excNode = GeneCluster.fromExceptionNode(current);
				processedNodes.add(current);

				lg.addVertex(excNode);
				for (GeneNode excNeighbor : g.getNeighbors(current)) {
					if (!excNeighbor.isValid()) {
						GeneCluster excNeighborCluster = GeneCluster
								.fromExceptionNode(excNeighbor);

						lg.addVertex(excNeighborCluster);
						if (lg.isNeighbor(excNeighborCluster, excNode)) {
							continue;
						}
                        if (!lg.addEdge(new GeneEdge(), excNode,
                                excNeighborCluster))
                            throw new IllegalStateException(
                                    "Adding an edge did not change the graph.");
					}
				}
			}
		}

		// Now that the graph is done, update with the weights of the exc. nodes
		for (GeneCluster gl : lg.getVertices()) {
			gl.finalizeHashCode();

			if (!gl.isValid()) {
				int weight = 1;
				for (GeneCluster neighbor : lg.getNeighbors(gl)) {
					if (neighbor.isValid())
						weight += neighbor.getWeight();
				}
				gl.setWeight(weight);
			}
		}
		return lg;
	}
}

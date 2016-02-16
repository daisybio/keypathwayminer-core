package dk.sdu.kpm.gui.tree;

import edu.uci.ics.jung.graph.DirectedSparseGraph;
import edu.uci.ics.jung.graph.util.EdgeType;

import java.util.*;

/**
 * Forest implementation which allows to add an edge (v_1, v_2) although v2 is
 * already part of the forest.
 * 
 * @author ajunge
 * 
 * @param <V>
 *            the class of the vertices
 * @param <E>
 *            the class of the edges in this forest
 */
@SuppressWarnings("serial")
public class MyForest extends DirectedSparseGraph<TreeNode, TreeEdge> {

	/**
	 * The objects observing this graph.
	 */
	private List<Observer> observers = new LinkedList<Observer>();

	/**
	 * Adds an edge e connecting vertex v1 and v2 of type edgeType to the graph.
	 * Performs some sanity checks before the edge is added in order to maintain
	 * the forest's consistency.
	 */
	public boolean addEdge(TreeEdge e, TreeNode v1, TreeNode v2,
			EdgeType edgeType) {
		if (!containsVertex(v1)) {
			throw new IllegalArgumentException("Tree must already contain "
					+ v1);
		}
		if (!containsVertex(v2)) {
			throw new IllegalArgumentException("Tree must already contain "
					+ v2);
		}

		// Sanity checks start
		// No self-edges
		if (v1.equals(v2)) {
			return false;
		}

		// No outgoing edge to vertices which already have an outgoing edge
		if (getSuccessorCount(v1) > 0) {
			return false;
		}

		// No incoming edges to data set nodes.
		if (v2 instanceof DataSetNode) {
			return false;
		}

		// Each binary operator vertex only has two incoming edges.
		if (v2 instanceof BinaryOperatorNode) {
			if (getPredecessorCount(v2) >= 2) {
				return false;
			}
		}

		boolean result = super.addEdge(e, v1, v2, edgeType);
		if (result) {
			notifyObservers();
		}
		return result;

	}

	@Override
	public boolean addVertex(TreeNode vertex) {
		boolean res = super.addVertex(vertex);
		notifyObservers();
		return res;
	}

	public void clean() {
		// IDEA Removes BinOp nodes which are not linked to two data sets.
	}

	/**
	 * Checks if the forest is connected and returns the root if this is the
	 * case. In the trees like occurring in KPM, the root is well-defined: It's
	 * the last operator node which is reached when starting to follow the
	 * successors from an arbitrary data set node.
	 * 
	 * @return Returns the root node of forest and returns <code>null</code> if
	 *         the tree is not connected.
	 */
	public TreeNode isConnected() {
		// Start at a data set node and follow its successors to the last
		// operator node. Do this for all data set nodes.
		// When the graph is connected, the root is the same for all data sets.
		TreeNode root = null;
		boolean isFirstDataSetNode = true;
		for (TreeNode node : getVertices()) {
			if (node instanceof DataSetNode) {
				TreeNode currentParent = node;
				int successors = getSuccessorCount(currentParent);
				// Search for the successor which itself has no successor. This
				// is
				// the root of the connected component, the current data set
				// node is in.
				Set<TreeNode> visited = new HashSet<TreeNode>();
				while (successors > 0) {
					if (successors > 1) {
						// This should never happen.
						throw new IllegalStateException("Node " + currentParent
								+ " has more than 1 successor.");
					}
					currentParent = getSuccessors(currentParent).iterator()
							.next();
					if (visited.contains(currentParent)) {
						return null;
					}
					visited.add(currentParent);
					successors = getSuccessorCount(currentParent);
				}
				if (isFirstDataSetNode) {
					root = currentParent;
					isFirstDataSetNode = false;
				} else if (!root.equals(currentParent)) {
					return null;
				}
			}
		}
		// // Create no artificial ORs if the tree is already connected.
		// if (roots.size() > 1) {
		// TreeNode last = roots.get(0);
		// TreeNode next;
		// int i = 1;
		// do {
		// next = roots.get(i);
		// // Create new BinaryOperator between the last and next vertices.
		// BinaryOperatorNode binOpNode = new BinaryOperatorNode(
		// BinaryOperatorType.OR);
		// addVertex(binOpNode);
		// layout.placeBetween(last, next, binOpNode);
		// super.addEdge(new TreeEdge(), last, binOpNode,
		// EdgeType.DIRECTED);
		// super.addEdge(new TreeEdge(), next, binOpNode,
		// EdgeType.DIRECTED);
		//
		// last = binOpNode;
		// } while (++i < roots.size());
		// return roots.get(i-1);
		// } else {
		// return roots.get(0);
		// } return false;
		return root;
	}

	/**
	 * Removes all vertices from this graph, according to the JUNG API manual
	 * this automatically removes all edges as well.
	 */
	public void removeAllVertices() {
		// Kind of odd construct to avoid ConcurrentModificationExceptions
		List<TreeNode> vertices = new LinkedList<TreeNode>(getVertices());
		for (int i = 0; i < vertices.size(); i++) {
			TreeNode v = vertices.get(i);
			removeVertex(v);
		}
		notifyObservers();
	}

	/**
	 * Removes all edges from this graph. The nodes are left untouched.
	 */
	public void removeAllEdges() {
		// Kind of odd construct to avoid ConcurrentModificationExceptions
		List<TreeEdge> edges = new LinkedList<TreeEdge>(getEdges());
		for (int i = 0; i < edges.size(); i++) {
			TreeEdge e = edges.get(i);
			removeEdge(e);
		}
		notifyObservers();
	}

	/**
	 * Removes all nodes which represent logical operators from the tree.
	 */
	public void removeOperatorNodes() {
		// Kind of odd construct to avoid ConcurrentModificationExceptions
		List<TreeNode> vertices = new LinkedList<TreeNode>(getVertices());
		for (int i = 0; i < vertices.size(); i++) {
			TreeNode v = vertices.get(i);
			if (v instanceof BinaryOperatorNode) {
				removeVertex(v);
			}
		}
		notifyObservers();
	}

	/**
	 * Adds an observer to this graph.
	 * 
	 * @param ob
	 *            - The object observing this graph.
	 */
	public void addObserver(Observer ob) {
		observers.add(ob);
	}

	/**
	 * Removes an observer from this graph.
	 * 
	 * @param ob
	 *            - The object observing this graph.
	 */
	public void removeObserver(Observer ob) {
		observers.remove(ob);
	}

	/**
	 * Notifies all objects observing this graph. Observers are typically
	 * notified when an edge or node is added or removed.
	 */
	private void notifyObservers() {
		for (Observer o : observers) {
			o.update(null, null);
		}
	}

	/**
	 * 
	 * @return The number of data sets which are contained in this graph.
	 */
	public int getDataSetCount() {
		int noDataSets = 0;
		for (TreeNode n : getVertices()) {
			if (n instanceof DataSetNode) {
				noDataSets++;
			}
		}
		return noDataSets;
	}

	/**
	 * Returns a map which maps the identifier of each data set to the
	 * respective indicator matrix.
	 * 
	 * @return A map: data set identifier -> file path
	 */
	public Map<String, Map<String, int[]>> getDataSetFileMap() {
		Map<String, Map<String, int[]>> filePathMap = new HashMap<String, Map<String, int[]>>();
		for (TreeNode n : getVertices()) {
			if (n instanceof DataSetNode) {
				DataSetNode dsn = (DataSetNode) n;
				filePathMap.put(dsn.getInternalName(),
						dsn.getDysregulationMatrix());
			}
		}
		return filePathMap;
	}

	/**
	 * Removes the given data set node and notifies all observers.
	 * 
	 * @param dataSetNode
	 *            - The node to be removed
	 */
	public void removeDataSetNode(DataSetNode dataSetNode) {
		removeVertex(dataSetNode);
		notifyObservers();
	}

	public List<DataSetNode> getDataSetNodes() {
		List<DataSetNode> list = new LinkedList<DataSetNode>();
		for (TreeNode n : getVertices()) {
			if (n instanceof DataSetNode) {
				DataSetNode dsn = (DataSetNode) n;
				list.add(dsn);
			}
		}
		return list;
	}

}

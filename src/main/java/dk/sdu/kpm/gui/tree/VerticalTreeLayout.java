package dk.sdu.kpm.gui.tree;

import com.google.common.base.Function;
import edu.uci.ics.jung.algorithms.layout.Layout;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import org.apache.commons.collections15.Transformer;

import java.awt.*;
import java.awt.geom.Point2D;
import java.util.*;
import java.util.List;

/**
 * Layout used to position the nodes of the TreePanel.
 * 
 * @author ajunge
 * 
 * @param <V>
 *            The class of the vertices in the graph.
 * @param <E>
 *            The class of the edges in the graph.
 */
public class VerticalTreeLayout implements Layout<TreeNode, TreeEdge> {

	/**
	 * Maps a vertex to its spatial position.
	 */
	private Map<TreeNode, Point2D> transformer;

	/**
	 * The graph that this Layout refers to.
	 */
	private Graph<TreeNode, TreeEdge> graph;

	/**
	 * Maps each vertex to an boolean flag which indicates whether or not the
	 * position of the vertex is locked.
	 */
	private Map<TreeNode, Boolean> lockMap;

	/**
	 * The range of the Tree in the x dimension (horizontal).
	 */
	private int[] xRange;

	/**
	 * The range of the Tree in the y dimension (vertical).
	 */
	private int[] yRange;

	/**
	 * Top, bottom, left and right margin which is left empty.
	 */
	private int margin = 50;

	/**
	 * The space which is left empty between two old nodes and a new one which
	 * is created after connecting the old nodes by an edge.
	 */
	private int horizontalShift = 50;

	/**
	 * The current size of the layout.
	 */
	private Dimension size;

	/**
	 * The visualization viewer for this graph.
	 */
	private VisualizationViewer<TreeNode, TreeEdge> visView;

	public VerticalTreeLayout(MyForest g, int xMin, int xMax, int yMin, int yMax) {
		xRange = new int[] { xMin + margin, xMax - margin };
		yRange = new int[] { yMin + margin, yMax - margin };
		size = new Dimension(xMax - xMin, yMax - yMin);

		graph = g;
		transformer = new HashMap<TreeNode, Point2D>();
		lockMap = new HashMap<TreeNode, Boolean>();
		initialize();
	}

	//@Override
	public Point2D transform(TreeNode node) {
		if (!transformer.containsKey(node)) {
			throw new IllegalArgumentException(
					"There is no position known for this node.");
		} else {
			return transformer.get(node);
		}
	}

	@Override
	public Graph<TreeNode, TreeEdge> getGraph() {
		return graph;
	}

	@Override
	public Dimension getSize() {
		return size;
	}

	@Override
	public void initialize() {
		reset();
		List<TreeNode> nodes = new ArrayList<TreeNode>(graph.getVertices());

		// Sort nodes according to their IDs in increasing order. Hence,
		// nodes added early to the graph should appear at the top of the graph
		// layout.
		Collections.sort(nodes);

		for (TreeNode n : nodes) {
			lockMap.put(n, false);
		}

		int noNodes = nodes.size();
		// Using the same spacing for a zero node and a one node graph as it is
		// used for a two node graph.
		// Note: Normally the TreePanel should not be displayed when there is
		// only one data set or no data set at all.
		if (noNodes < 2) {
			noNodes = 2;
		}

		// The vertical node distance.
		int nodeDistance = (yRange[1] - yRange[0]) / (noNodes - 1);

		// Position all nodes below each other.
		int xPos = xRange[0];

		for (int i = 0; i < nodes.size(); i++) {
			int yPos = yRange[0] + (nodeDistance * i);
			setLocation(nodes.get(i), new Point(xPos, yPos));
		}
	}

	@Override
	public void setInitializer(Function<TreeNode, Point2D> function) {

	}

	@Override
	public boolean isLocked(TreeNode node) {
		return lockMap.get(node);
	}

	@Override
	public void lock(TreeNode node, boolean lockFlag) {
		lockMap.put(node, lockFlag);
	}

	@Override
	public void reset() {
		lockMap.clear();
		transformer.clear();
	}

	@Override
	public void setGraph(Graph<TreeNode, TreeEdge> g) {
		graph = g;
	}

	@Override
	public void setLocation(TreeNode node, Point2D pos) {
		transformer.put(node, pos);
	}

	@Override
	public void setSize(Dimension size) {
		throw new UnsupportedOperationException(
				"The size of the layout is determined by the initial"
						+ " size and cannot be changed.");
	}

	//@Override
	public void setInitializer(Transformer<TreeNode, Point2D> arg0) {
		throw new UnsupportedOperationException(
				"Initializers are not supported.");

	}

	/**
	 * Places the newNode vertically between node1 and node2 and horizontally
	 * shifted.
	 * 
	 * @param node1
	 *            - First node
	 * @param node2
	 *            - Second node
	 * @param newNode
	 *            - The new node to be placed between the first and second node.
	 */
	public void placeBetween(TreeNode node1, TreeNode node2, TreeNode newNode) {
		Point2D pos1 = transform(node1);
		Point2D pos2 = transform(node2);
		int verticalPos = (int) (pos1.getY() + ((pos2.getY() - pos1.getY()) / 2));
		int horizontalPos = (int) (Math.max(pos1.getX(), pos2.getX()) + horizontalShift);
		setLocation(newNode, new Point(horizontalPos, verticalPos));
	}

	/**
	 * Aligns the data set nodes in the graph and places them vertically aligned
	 * in the middle of the graph.
	 */
	public void alignDataSetNodes() {
		List<TreeNode> dataSetNodes = new LinkedList<TreeNode>();
		for (TreeNode n : graph.getVertices()) {
			if (n instanceof DataSetNode) {
				dataSetNodes.add(n);
			}
		}

		// Compute the graph's center in x direction.
		int xPosition = xRange[0];

		int noNodes = dataSetNodes.size();
		// Return if the graph contains no node.
		// If the graph contains a single node, place it at the x-direction
		// center
		// at its current y position.
		if (noNodes == 0) {
			return;
		} else if (noNodes == 1) {
			TreeNode t = dataSetNodes.get(0);
			setLocation(t, new Point(xPosition, (int) transform(t).getY()));
		} else {

			// Compute distance between two nodes in y direction.
			int interNodeDist = (yRange[1] - yRange[0]) / (noNodes - 1);

			// Sort all data set nodes by increasing y Position.
			Collections.sort(dataSetNodes, new Comparator<TreeNode>() {

				@Override
				public int compare(TreeNode o1, TreeNode o2) {
					double firstY = transform(o1).getY();
					double secY = transform(o2).getY();
					double diff = firstY - secY;
					if (diff == 0) {
						return 0;
					} else if (diff < 0) {
						return -1;
					} else {
						return 1;
					}
				}
			});

			// Reposition the nodes.
			for (int i = 0; i < dataSetNodes.size(); i++) {
				TreeNode t = dataSetNodes.get(i);
				setLocation(t, new Point(xPosition, interNodeDist * i
						+ yRange[0]));
			}
		}
		// Update visualization component.
		visView.updateUI();

		// IDEA Allow vertical and horizontal alignment -> add button to switch
		// between the two alignment modes
		// IDEA Each node as a depth attribute which indicates at what depth the
		// node is to be placed after the align method is invoked.

	}

	/**
	 * 
	 * @param vv
	 *            The visualization viewer for the current tree.
	 */
	public void setVisualizationViewer(
			VisualizationViewer<TreeNode, TreeEdge> vv) {
		visView = vv;
	}

	@Override
	public Point2D apply(TreeNode treeNode) {
		return null;
	}
}

package dk.sdu.kpm.gui.tree;


/**
 * Represents a node as part of the tree in {@link LinksPanel}.
 * 
 * @author ajunge
 * 
 */
public abstract class TreeNode implements Comparable<TreeNode> {

	private static int NODECOUNT = 0;

	private int id;

	public TreeNode() {
		this.id = NODECOUNT++;
	}

	public abstract String toString();

	public boolean equals(Object o) {
		if (o instanceof TreeNode) {
			TreeNode n = (TreeNode) o;
			if (n.id == id) {
				return true;
			} else {
				return false;
			}
		} else {
			return false;
		}
	}

	public int hashCode() {
		return id;
	}

	public int compareTo(TreeNode n) {
		return this.id - n.id;
	}

}

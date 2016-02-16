package dk.sdu.kpm.gui.tree;


/**
 * Represents an edge as part of the tree in {@link LinksPanel}.
 * 
 * @author ajunge
 * 
 */
public class TreeEdge {

	private static int EDGECOUNT = 0;

	private int id;

	public TreeEdge() {
		this.id = EDGECOUNT++;
	}

	public String toString() {
		return new Integer(id).toString();
	}

	public int hashCode() {
		return id;
	}

	public boolean equals(Object o) {
		if (o instanceof TreeEdge) {
			TreeEdge e = (TreeEdge) o;
			if (e.id == id) {
				return true;
			} else {
				return false;
			}
		} else {
			return false;
		}
	}

}

package dk.sdu.kpm.gui.tree;

import dk.sdu.kpm.KPMSettings;

import java.util.Map;


/**
 * Represents a data set vertex as displayed in the TreePanel.
 * 
 * @author ajunge
 * 
 */
public class DataSetNode extends TreeNode {

	/**
	 * The name of the represented data set given by the user and which is
	 * visible in the GUI.
	 */
	private String externalName;

	/**
	 * The name of the represented data set which is used internally.
	 */
	private String internalName;

	/**
	 * The dysregulation matrix represented by this data set.
	 */
	private Map<String, int[]> dysregMap;

	/**
	 * The number of case exceptions, L, associated with this node.
	 */
	private int l;

	/**
	 * Creates a new node representing the data set with the given internal and
	 * external name.
	 * 
	 * @param externalName
	 *            - The name of the represented data set specified by the user
	 *            and used in the GUI.
	 * @param internalName
	 *            - The name of the represented data set specified by the user
	 *            and used in the GUI.
	 * @param dysregMap
	 *            - The data set represented by this node.
	 */
	public DataSetNode(String externalName, String internalName, Map<String, int[]> dysregMap, int case_exceptions_default) {
		this.externalName = externalName;
		this.internalName = internalName;
		this.dysregMap = dysregMap;
		this.l = case_exceptions_default;
	}

	@Override
	public String toString() {
		return externalName;
	}

	/**
	 * 
	 * @return The internal name of the data set represented by this node.
	 */
	public String getInternalName() {
		return internalName;
	}

	/**
	 * 
	 * @return The external name of the data set represented by this node.
	 */
	public String getExternalName() {
		return externalName;
	}

	/**
	 * 
	 * @return The dysregulationMatrix corresponding to this node.
	 */
	public Map<String, int[]> getDysregulationMatrix() {
		return dysregMap;
	}

	/**
	 * 
	 * @param name
	 *            - The new external name of this node, as given by the user.
	 */
	public void setExternalName(String externalName) {
		this.externalName = externalName;
	}

	/**
	 * 
	 * @return The number of case exceptions, L, associated with this node.
	 */
	public int getL() {
		return l;
	}

	/**
	 * Sets this node's L parameter.
	 * 
	 * @param l
	 *            - The number of case exceptions, L, associated with this node.
	 */
	public void setL(int l) {
		this.l = l;
	}

}

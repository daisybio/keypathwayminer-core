package dk.sdu.kpm.gui.tree;

import dk.sdu.kpm.gui.clause.BinaryOperatorType;

/**
 * Represents a binary operator vertex as displayed in the TreePanel.
 * 
 * @author ajunge
 * 
 */
public class BinaryOperatorNode extends TreeNode {

	private BinaryOperatorType op;

	public BinaryOperatorNode(BinaryOperatorType op) {
		this.op = op;
	}

	@Override
	public String toString() {
		return op.toString();
	}

	/**
	 * 
	 * @return The binary operator which is part of this clause.
	 */
	public BinaryOperatorType getBinaryOperator() {
		return op;
	}

	/**
	 * Sets the binary operator of this node.
	 * 
	 * @param bot
	 */
	public void setBinaryOperator(BinaryOperatorType bot) {
		op = bot;
	}

}

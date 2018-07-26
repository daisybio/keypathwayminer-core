package dk.sdu.kpm.gui.tree;

import dk.sdu.kpm.gui.clause.BinaryOperatorType;
import org.apache.commons.collections15.Factory;

/**
 * Used to create logical operator nodes after the user adds them to the
 * {@link TreePanel}.
 * 
 * @author ajunge
 * 
 */
public class NodeFactory implements Factory<TreeNode> {

	//@Override
	public TreeNode create() {
		return new BinaryOperatorNode(BinaryOperatorType.OR);
	}

}

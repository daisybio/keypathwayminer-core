package dk.sdu.kpm.gui.tree;

import org.apache.commons.collections15.Factory;

/**
 * Creates edges which are added by the user to connect data set nodes ande
 * logical operator nodes.
 * 
 * @author ajunge
 * 
 */
public class EdgeFactory implements Factory<TreeEdge> {

	@Override
	public TreeEdge create() {
		return new TreeEdge();
	}

}

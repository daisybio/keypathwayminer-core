package dk.sdu.kpm.gui.clause;

import dk.sdu.kpm.KPMSettings;
import dk.sdu.kpm.gui.tree.BinaryOperatorNode;
import dk.sdu.kpm.gui.tree.DataSetNode;
import dk.sdu.kpm.gui.tree.MyForest;
import dk.sdu.kpm.gui.tree.TreeNode;

/**
 * This class is a factory for the {@link Clause} which is specified in the
 * TreePanel of the GUI. It creates an appropriate Clause object given a
 * {@link MyForest} object and a root node to start the creation from.
 * 
 * @author ajunge
 * 
 */
public class ClauseFactory {

	/**
	 * 
	 * @param f
	 *            - A MyForest object containing the given TreeNode.
	 * @param n
	 *            - The root node which is part of {@code f}.
	 * @return The logical clause which is encoded by the subtree of f which has
	 *         n as the root node.
	 */
	public static Clause create(MyForest f, TreeNode n)
			throws IllegalStateException {
		Clause c;
		if (n instanceof BinaryOperatorNode) {
			c = createBinOpClause(f, (BinaryOperatorNode) n);
		} else {
			c = createLiteralClause(f, (DataSetNode) n);
		}
		return c;
	}

	/**
	 * 
	 * @param f
	 *            - A MyForest containing the given BinaryOperatorNode.
	 * @param n
	 *            - The root node of type BinaryOperatorNode which is part of f.
	 * @return The logical clause which is encoded by the subtree of f which has
	 *         n as the root node.
	 */
	private static Clause createBinOpClause(MyForest f, BinaryOperatorNode n)
			throws IllegalStateException {
		// Get the left and right child node of the current node.
		int noChildren = f.getPredecessorCount(n);
		if (noChildren != 2) {
			throw new IllegalStateException(
					"Wrong number of predecessors: A BinaryOperatorNode must have exactly two predecessors.");
		}

		// Create clauses from the two child nodes.
		Clause[] children = new Clause[2];
		int i = 0;
		for (TreeNode suc : f.getPredecessors(n)) {
			children[i] = create(f, suc);
			i++;
		}

		// Add those clauses as sub-clauses to the current clause.
		Clause c = new BinaryOperatorClause(children[0], children[1],
				n.getBinaryOperator());
		return c;
	}

	/**
	 * 
	 * @param f
	 *            - A MyForest containing the given DataSetNode.
	 * @param n
	 *            - The root node of type DataSetNode which is part of f.
	 * @return The logical clause which is encoded by the subtree of f which has
	 *         n as the root node.
	 */
	private static Clause createLiteralClause(MyForest f, DataSetNode n)
			throws IllegalStateException {
		// The current node should not have any child nodes.
		int noChildren = f.getPredecessorCount(n);
		if (noChildren != 0) {
			throw new IllegalStateException(
					"Wrong number of predecessors: A DataSetNode is not allowed to have any predecessor.");
		}

		Clause c = new LiteralClause(n.getInternalName(), n.getDysregulationMatrix(), n.getL());
		return c;
	}
        
	/**
	 * Returns a string representation of the given logical clause. The
	 * following operators appear in the formula: ( ) -- parenthesis && -- AND
	 * operator || -- OR operator ! - negation
	 * 
	 * Examples: Let L1 and L2 be the identifiers of two data sets. OR: L1 || L2
	 * AND: L1 && L2 XOR: ( !L1 && L2) || ( L1 && !L2)
	 * 
	 * IMPORTANT: The variable names must be equal to the data set identifiers,
	 * and these MUST NOT be numbers.
	 * 
	 * @param clause
	 *            - The logical clause to be represented as a String object.
	 * @return The string representation of the logical clause
	 */
	public static String getLogicalFormula(Clause clause) {
		// Return the data set identifier if the clause is a literal.
		if (clause instanceof LiteralClause) {
			return ((LiteralClause) clause).getInternalIdentifier();
		} else if (clause instanceof BinaryOperatorClause) {
			BinaryOperatorClause c = (BinaryOperatorClause) clause;

			BinaryOperatorType op = c.getBinaryOperator();
			String leftChild = getLogicalFormula(c.getLeftChild());
			String rightChild = getLogicalFormula(c.getRightChild());
			return op.getLogicalFormula(leftChild, rightChild);
		} else {
			throw new UnsupportedOperationException(
					"Cannot compute data values for clauses of class "
							+ clause.getClass());
		}
	}
        
	/**
	 * NOTE: The String returned here is not suited for further processing in
	 * the algorithms but only to be shown in the GUI.
	 * 
	 * Returns a string representation of the given logical clause.
	 * 
	 * @param clause
	 *            - The logical clause to be represented as a String object.
	 * @return The string representation of the logical clause
	 */
	public static String getLogicalStringRepresentation(Clause clause, KPMSettings kpmSettings) {
		// Return the data set identifier if the clause is a literal.
		if (clause instanceof LiteralClause) {
			String internalIdentifier = ((LiteralClause) clause)
					.getInternalIdentifier();
			return kpmSettings.externalToInternalIDManager
					.getExternalIdentifier(internalIdentifier);
		} else if (clause instanceof BinaryOperatorClause) {
			BinaryOperatorClause c = (BinaryOperatorClause) clause;

			BinaryOperatorType op = c.getBinaryOperator();
			String leftChild = getLogicalStringRepresentation(c.getLeftChild(), kpmSettings);
			String rightChild = getLogicalStringRepresentation(c
					.getRightChild(), kpmSettings);
			return op.getStringRepresentation(leftChild, rightChild);
		} else {
			throw new UnsupportedOperationException(
					"Cannot compute data values for clauses of class "
							+ clause.getClass());
		}
	}        

}

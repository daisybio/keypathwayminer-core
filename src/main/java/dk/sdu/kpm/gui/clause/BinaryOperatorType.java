package dk.sdu.kpm.gui.clause;

/**
 * Represents a binary operator as used in connecting for connecting two input
 * data sets.
 * 
 */
public enum BinaryOperatorType {
	AND, OR, XOR;

	/**
	 * Returns a String representation of this binary operator and the String
	 * representations of its children
	 * 
	 * @param leftChild
	 *            - String representation of the left child of this operator
	 * @param rightChild
	 *            - String representation of the right child of this operator
	 * @return a String representation of both children connected by a binary
	 *         logical operator
	 */
	public String getLogicalFormula(String leftChild, String rightChild) {
		switch (this) {
		case AND:
			return " ( " + leftChild + " && " + rightChild + " ) ";
		case OR:
			return " ( " + leftChild + " || " + rightChild + " ) ";
		case XOR:
			return " ( " + " ( " + " ! " + leftChild + " && " + rightChild
					+ " ) " + " || " + " ( " + leftChild + " && " + " ! "
					+ rightChild + " ) " + " ) ";
		default:
			System.out.println("Unsupported binary operator");
			return null;
		}
	}

	/**
	 * NOTE: The String returned here is not suited for further processing in
	 * the algorithms but only to be shown in the GUI.
	 * 
	 * Returns a String representation of this binary operator and the String
	 * representations of its children
	 * 
	 * @param leftChild
	 *            - String representation of the left child of this operator
	 * @param rightChild
	 *            - String representation of the right child of this operator
	 * @return a String representation of both children connected by a binary
	 *         logical operator
	 */
	public String getStringRepresentation(String leftChild, String rightChild) {
		switch (this) {
		case AND:
			return " ( " + leftChild + " AND " + rightChild + " ) ";
		case OR:
			return " ( " + leftChild + " OR " + rightChild + " ) ";
		case XOR:
			return " ( " + leftChild + " XOR " + rightChild + " ) ";
		default:
			System.out.println("Unsupported binary operator");
			return null;
		}
	}

}

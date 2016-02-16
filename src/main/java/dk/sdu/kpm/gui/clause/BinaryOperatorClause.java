package dk.sdu.kpm.gui.clause;

/**
 * Represents a logical clause involving a binary operator, e.g. the clause 'A
 * OR B'.
 * 
 * @author ajunge
 * 
 */
public class BinaryOperatorClause extends Clause {

	/**
	 * The binary operator which is part of this clause.
	 */
	private BinaryOperatorType op;

	/**
	 * Constructs a new BinaryOperatorClause with the given subclauses and a
	 * binary operator.
	 * 
	 * @param left
	 *            - The first subclause.
	 * @param right
	 *            - The second subclause.
	 * @param op
	 *            - The binary operator which is part of this Clause.
	 */
	public BinaryOperatorClause(Clause left, Clause right, BinaryOperatorType op) {
		super(left, right);
		this.op = op;
	}

	/**
	 * 
	 * @return The left child of this binary operator clause.
	 */
	public Clause getLeftChild() {
		return getChildren().get(0);
	}

	/**
	 * 
	 * @return The right child of this binary operator clause.
	 */
	public Clause getRightChild() {
		return getChildren().get(1);
	}

	/**
	 * 
	 * @return The binary operator which is part of this clause.
	 */
	public BinaryOperatorType getBinaryOperator() {
		return op;
	}

}

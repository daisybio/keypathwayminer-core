package dk.sdu.kpm.gui.clause;

import java.util.LinkedList;
import java.util.List;

/**
 * A logical clause which consists of data sets and logical operators connecting
 * them. The user can specify such clauses in the TreePanel.
 * 
 * @author ajunge
 * 
 */
public abstract class Clause {

	/**
	 * The child clauses of this clause.
	 */
	private List<Clause> children;

	/**
	 * Constructs a new {@link Clause} with the given children.
	 * 
	 * @param childClauses
	 */
	public Clause(Clause... childClauses) {
		children = new LinkedList<Clause>();
		for (Clause c : childClauses) {
			children.add(c);
		}
	}

	/**
	 * 
	 * @return The sub-clauses of this clause, i.e. two sub-clauses of a binary
	 *         operator, the only sub-clause of a unary operator and an empty
	 *         list if the clause represents a literal.
	 */
	public List<Clause> getChildren() {
		return children;
	}

	/**
	 * 
	 * @return The number of children of this clause.
	 */
	public int getNoChildren() {
		return children.size();
	}
}

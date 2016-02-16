package dk.sdu.kpm.gui.clause;

import java.util.Map;

/**
 * Represents a single literal in a logical clause, i.e. a data set.
 * 
 * @author ajunge
 * 
 */
public class LiteralClause extends Clause {

	/**
	 * The name of the data set represented by this clause.
	 */
	private String internalIdentifier;

	/**
	 * The data set represented by this clause.
	 */
	private Map<String, int[]> dysregMap;

	/**
	 * Number of case exceptions allowed for this data set.
	 */
	private int caseExceptions;

	/**
	 * Constructs a new literal which represents the data set with the given
	 * internal identifier.
	 * 
	 * @param internalIdentifier
	 */
	public LiteralClause(String internalIdentifier, Map<String, int[]> dysregMap,
			int caseExceptions) {
		this.internalIdentifier = internalIdentifier;
		this.dysregMap = dysregMap;
		this.caseExceptions = caseExceptions;
	}

	/**
	 * 
	 * @return The data file represented by this literal.
	 */
	public Map<String, int[]> getDysregMap() {
		return dysregMap;
	}

	/**
	 * Returns the identifier of the data set which is represented by this
	 * literal.
	 */
	public String getInternalIdentifier() {
		return internalIdentifier;
	}

}

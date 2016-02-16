package dk.sdu.kpm.graph;

import java.util.Map;

public interface Result extends Comparable<Result> {

	/**
	 * Relict from older implementation -- only needed for final statistics
	 * printout.
	 * 
	 * @return a map that maps node ids to the respective GeneNode object; this
	 *         map contains only nodes from the original, uncontracted graph
	 *         that are in this solution.
	 */
	public Map<String, GeneNode> getVisitedNodes();

	/**
	 * Relict from older implementation -- only needed for final statistics
	 * printout.
	 * 
	 * @return the average number of differential expressed cases for this
	 *         solution.
	 */
	public double getAverageDiffExpressedCases();

	/**
	 * Relict from older implementation -- only needed for final statistics
	 * printout.
	 * 
	 * @return I have no clue what this returns
	 */
	public double getInformationGainExpressed();

	public int getFitness();
        
        public int getInstances();
        
        public void setInstances(int instances);

	public int getNumExceptionNodes();

	/**
	 * Return the total number of non-differentially expressed cases; which is
	 * the sum over all of those cases of the nodes in this subgraph. This works
	 * kinda to tell how "bad" a solution is (as opposed to the number of
	 * exception nodes in the simpler model).
	 * 
	 * Pay attention: This method excludes the k most expensive nodes.
	 * 
	 * @return
	 */
	public int getNonDifferentiallyExpressedCases();

	public void flagExceptionNodes();
        
        public boolean haveOverlap(Object o);
}

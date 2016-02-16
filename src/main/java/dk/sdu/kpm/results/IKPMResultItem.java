package dk.sdu.kpm.results;

import java.util.List;
import java.util.Map;

import dk.sdu.kpm.graph.GeneNode;

public interface IKPMResultItem {
	
	/**
	 * The allowed amount of node exceptions.
	 * @return
	 */
	int getK();
	
	/**
	 * The allowed amount of case exceptions.
	 * @return
	 */
	int getL();

    /***
     * Returns information about each subnetwork found in a table-like manner, with a header.
     * @return
     */
    Object[][] getResultsInfoTable();

    /***
     * Returns information about each subnetwork found in a table-like manner, with a header.
     * @return
     */
    String[] getResultsInfoTableHeaders();

    /**
     * Contains the union node set resulting from running KPM with the given K/L combination.
     * @return
     */
    Map<String, GeneNode> getUnionNodeSet();

    /**
     * Contains the union node set count resulting from running KPM with the given K/L combination.
     * @return
     */
    Map<String, Integer> getUnionNodeSetCounts();


    /**
     * Contains the node set resulting from running KPM with the given K/L combination.
     * @return
     */
    List<Map<String, GeneNode>> getAllComputedNodeSets();
}

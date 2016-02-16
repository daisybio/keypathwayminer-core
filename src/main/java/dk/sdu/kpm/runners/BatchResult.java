package dk.sdu.kpm.runners;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import dk.sdu.kpm.graph.GeneNode;
import dk.sdu.kpm.results.IKPMResultItem;

/**
 * Class used to hold information regarding a kpm result.
 */
public class BatchResult implements IKPMResultItem, Serializable{
	
	private int k;
	
	private int l;
	
	private Map<String, GeneNode> unionNodeSet;

    private List<Map<String, GeneNode>> allComputedSets;

    private Map<String, Integer> unionNodeSetCounts;

    private String[] cols = {"Rank", "Nodes", "Edges", "Avg. Exp.", "Info. Content"};

    private Object[][] resultsInfoTable;

    public BatchResult(int k, int l, Map<String, GeneNode> unionNodeSet, List<Map<String, GeneNode>> allComputedSets, Map<String, Integer> unionNodeSetCounts, Object[][] resultsInfoTable){
		this.k = k;
		this.l = l;
		this.unionNodeSet = unionNodeSet;
        this.allComputedSets = allComputedSets;
        this.unionNodeSetCounts = unionNodeSetCounts;
        this.resultsInfoTable = resultsInfoTable;
	}

    public BatchResult(Map<String, GeneNode> unionNodeSet, List<Map<String, GeneNode>> allComputedSets, Map<String, Integer> unionNodeSetCounts, Object[][] resultsInfoTable){
        this(0,0,unionNodeSet,allComputedSets,unionNodeSetCounts, resultsInfoTable);
    }


	@Override
	public int getK() {
		return this.k;
	}

    public void setK(int k){
        this.k = k;
    }

	@Override
	public int getL() {
		return this.l;
	}

    @Override
    public Object[][] getResultsInfoTable() {
        return this.resultsInfoTable;
    }

    @Override
    public String[] getResultsInfoTableHeaders() {
        return cols;
    }

    public void setL(int l){
        this.l = l;
    }

	@Override
	public Map<String, GeneNode> getUnionNodeSet() {
		return this.unionNodeSet;
	}

    @Override
    public Map<String, Integer> getUnionNodeSetCounts() {
        return this.unionNodeSetCounts;
    }

    @Override
    public List<Map<String, GeneNode>> getAllComputedNodeSets() {
        return this.allComputedSets;
    }
}

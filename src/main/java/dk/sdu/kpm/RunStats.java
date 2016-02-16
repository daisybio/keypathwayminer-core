package dk.sdu.kpm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

import dk.sdu.kpm.graph.GeneEdge;
import dk.sdu.kpm.graph.GeneNode;
import dk.sdu.kpm.graph.Result;
import dk.sdu.kpm.logging.KpmLogger;
import dk.sdu.kpm.utils.DeepCopy;
import dk.sdu.kpm.validation.NodeOverlapCounter;
import dk.sdu.kpm.validation.ValidationOverlapResultItem;

/**
 *
 * @author nalcaraz
 */
public class RunStats {

	private List<Result> results;

	private Map<String, Integer> nodeCount;

	private Map<String, Integer> edgeCount;

	private Map<String, Double> nodeCountNorm;

	private Map<String, Double> edgeCountNorm;


	private Map<Integer, Integer> index2numEdges;

	private Object[][] values;

	private String[] cols = {"Rank", "Nodes", "Edges", "Avg. Exp.", "Info. Content"};

	private long runtime;

	private String runId;

	private int minNodeHits;

	private int maxNodeHits;

	private int minEdgeHits;

	private int maxEdgeHits;

	private Map<String, Boolean> exceptionMap;

	public RunStats(String runId, List<Result> results,
			long runtime, Map<String, Boolean> exceptionMap, KPMSettings settings) {
		this.runId = runId;
        if(results != null){
            this.results = results;
        }else{
            this.results =  new ArrayList<Result>();
        }

		this.runtime = runtime;

        if(exceptionMap != null) {
            this.exceptionMap = exceptionMap;
        }else{
            this.exceptionMap = new HashMap<String, Boolean>();
        }
		nodeCountNorm = new HashMap<String, Double>();
		edgeCountNorm = new HashMap<String, Double>();

		index2numEdges = new HashMap<Integer, Integer>();

        if(settings.containsGoldStandardNodes()){
            String[] cols2 = new String[cols.length + 2];

            for(int i = 0; i < cols.length; i++){
                cols2[i] = cols[i];
            }

            cols2[cols.length-2] = "overlap";
            cols2[cols.length-1] = "jaccard";
            cols = cols2;
        }

		values = new Object[this.results.size()][cols.length];
		minNodeHits = Integer.MAX_VALUE;
		minEdgeHits = Integer.MAX_VALUE; 
		maxNodeHits = 0; 
		maxEdgeHits = 0;
		computeCounts(settings);
	}

	public RunStats(RunStats rs){
		nodeCount = new HashMap<String, Integer>(rs.nodeCount);
		edgeCount = new HashMap<String, Integer>(rs.edgeCount);
		nodeCountNorm = new HashMap<String, Double>(rs.nodeCountNorm);
		edgeCountNorm = new HashMap<String, Double>(rs.edgeCountNorm);
		index2numEdges = new HashMap<Integer, Integer>(rs.index2numEdges);
		this.values = deepCopy(rs.values);
		runtime = rs.runtime;
		runId = rs.runId;
		minNodeHits = rs.minNodeHits;
		maxNodeHits = rs.maxNodeHits;
		minEdgeHits = rs.minEdgeHits;
		maxEdgeHits = rs.maxEdgeHits;

        if(rs.exceptionMap != null) {
            exceptionMap = new HashMap<String, Boolean>(rs.exceptionMap);
        }else{
            exceptionMap = new HashMap<String, Boolean>();
        }

        if(rs.getResults() != null && rs.getResults().size() > 0){
            this.results = new ArrayList<Result>();
            for (Result res : rs.getResults()){
                this.results.add(res);
            }
        }
	}

	private Object[][] deepCopy(Object[][] original) {
		if (original == null) {
			return null;
		}

		final Object[][] result = new Object[original.length][];
		for (int i = 0; i < original.length; i++) {
			result[i] = Arrays.copyOf(original[i], original[i].length);
		}
		return result;
	}

	public List<Result> getResults() {
		return results;
	}

	public int getNumPathways() {
		return results.size();
	}

	public int getNumNodes(int index) {
		return results.get(index).getVisitedNodes().size();
	}

	public int getNumEdges(int index) {
		return index2numEdges.get(index);
	}

	public long getRuntime() {
		return runtime;
	}

	public String getRunId() {
		return runId;
	}

	public int getMinNodeHits() {
		return minNodeHits;
	}

	public int getMaxNodeHits() {
		return maxNodeHits;
	}

	public int getMinEdgeHits() {
		return minEdgeHits;
	}

	public int getMaxEdgeHits() {
		return maxEdgeHits;
	}

	public Map<String, Boolean> getExceptionMap() {
		return exceptionMap;
	}

	public Object[][] getValues() {
		return values;
	}

	private double getAvgExpCases(int index) {
		return results.get(index).getAverageDiffExpressedCases();
	}

	private double getInfoGain(int index) {
		return results.get(index).getInformationGainExpressed();
	}

	public Map<String, Integer> getNodeHits() {
		return nodeCount;
	}

	public Map<String, Integer> getEdgeHits() {
		return edgeCount;
	}

	public Map<String, Double> getNodeHitsNorm() {
		return nodeCountNorm;
	}

	public Map<String, Double> getEdgeHitsNorm() {
		return edgeCountNorm;
	}


	private void computeCounts(KPMSettings kpmSettings) {
        try {
            if (kpmSettings.TOTAL_NODE_HITS == null) {
                kpmSettings.TOTAL_NODE_HITS = new HashMap<String, Integer>();
            }
            if (kpmSettings.TOTAL_NODE_HITS.isEmpty()) {
                for (GeneNode node : kpmSettings.MAIN_GRAPH.getVertices()) {
                    kpmSettings.TOTAL_NODE_HITS.put(node.getNodeId(), 0);
                }
            }

            if (kpmSettings.TOTAL_EDGE_HITS == null) {
                kpmSettings.TOTAL_EDGE_HITS = new HashMap<String, Integer>();
            }
            if (kpmSettings.TOTAL_EDGE_HITS.isEmpty()) {
                for (GeneEdge edge : kpmSettings.MAIN_GRAPH.getEdges()) {
                    kpmSettings.TOTAL_EDGE_HITS.put(edge.getEdgeId(), 0);
                }
            }

            nodeCount = new HashMap<String, Integer>();
            for (GeneNode node : kpmSettings.MAIN_GRAPH.getVertices()) {
                nodeCount.put(node.getNodeId(), 0);
            }

            edgeCount = new HashMap<String, Integer>();
            for (GeneEdge edge : kpmSettings.MAIN_GRAPH.getEdges()) {
                edgeCount.put(edge.getEdgeId(), 0);
            }

            int index = 0;
            for (Result result : results) {
                Map<String, GeneNode> nodes = result.getVisitedNodes();
                for (String nodeId : nodes.keySet()) {
                    int curr = nodeCount.get(nodeId) + 1;
                    if (curr < minNodeHits) {
                        minNodeHits = curr;
                    }
                    if (curr > maxNodeHits) {
                        maxNodeHits = curr;
                    }

                    nodeCount.put(nodeId, curr);

                    if (kpmSettings.TOTAL_NODE_HITS.containsKey(nodeId)) {
                        kpmSettings.TOTAL_NODE_HITS.put(nodeId, kpmSettings.TOTAL_NODE_HITS.get(nodeId) + 1);
                    } else {
                        kpmSettings.TOTAL_NODE_HITS.put(nodeId, 1);
                    }
                }

                List<GeneEdge> edges = kpmSettings.MAIN_GRAPH.getConnectingEdges(nodes.values());
                index2numEdges.put(index, edges.size());
                for (GeneEdge edge : edges) {
                    String edgeId = edge.getEdgeId();
                    int curr = edgeCount.get(edgeId) + 1;
                    if (curr < minEdgeHits) {
                        minEdgeHits = curr;
                    }
                    if (curr > maxEdgeHits) {
                        maxEdgeHits = curr;
                    }

                    edgeCount.put(edgeId, curr);

                    if (kpmSettings.TOTAL_EDGE_HITS.containsKey(edgeId)) {
                        kpmSettings.TOTAL_EDGE_HITS.put(edgeId, kpmSettings.TOTAL_EDGE_HITS.get(edgeId) + 1);
                    } else {
                        kpmSettings.TOTAL_EDGE_HITS.put(edgeId, 1);
                    }
                }

                values[index][0] = new Integer(index + 1);
                values[index][1] = new Integer(getNumNodes(index));
                values[index][2] = new Integer(getNumEdges(index));
                double avg = getAvgExpCases(index);
                values[index][3] = new Double(Math.floor(avg * 100 + 0.5) / 100);
                double infoGain = getInfoGain(index);
                values[index][4] = new Double(Math.floor(infoGain * 100 + 0.5) / 100);


                if(kpmSettings.containsGoldStandardNodes() && values.length > 5){
                    Map<String, GeneNode> visitedNodes = results.get(index).getVisitedNodes();
                    List<ValidationOverlapResultItem> overlap = NodeOverlapCounter.countOverlap(kpmSettings.VALIDATION_GOLDSTANDARD_NODES, visitedNodes);
                    int overlapSize = overlap.size();
                    values[index][5] = overlapSize;
                    values[index][6] = 0;
                    if(overlapSize > 0){
                        double intersectionSize = (double) overlapSize;
                        double visitedNodesSize = (double) visitedNodes.size();
                        if(visitedNodesSize > 0){
                            values[index][6] = intersectionSize / visitedNodesSize;
                        }
                    }
                }
                index++;


                for (String nodeId : nodeCount.keySet()) {
                    nodeCountNorm.put(nodeId, ((double) nodeCount.get(nodeId) / (double) maxNodeHits));
                }

                for (String edgeId : edgeCount.keySet()) {
                    edgeCountNorm.put(edgeId, ((double) edgeCount.get(edgeId) / (double) maxEdgeHits));
                }
            }

            if (maxNodeHits > kpmSettings.TOTAL_NODE_HITS_MAX) {
                kpmSettings.TOTAL_NODE_HITS_MAX = maxNodeHits;
            }

            if (minNodeHits < kpmSettings.TOTAL_NODE_HITS_MIN) {
                kpmSettings.TOTAL_NODE_HITS_MIN = minNodeHits;
            }

            if (maxEdgeHits > kpmSettings.TOTAL_EDGE_HITS_MAX) {
                kpmSettings.TOTAL_EDGE_HITS_MAX = maxEdgeHits;
            }

            if (minEdgeHits < kpmSettings.TOTAL_EDGE_HITS_MIN) {
                kpmSettings.TOTAL_EDGE_HITS_MIN = minEdgeHits;
            }
        }catch(Exception e){
            KpmLogger.log(Level.SEVERE, e);
        }
	}


}
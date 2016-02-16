package dk.sdu.kpm.graph;

import dk.sdu.kpm.Combine;
import dk.sdu.kpm.KPMSettings;
import edu.uci.ics.jung.graph.SparseGraph;

import org.mvel2.MVEL;

import java.io.Serializable;
import java.util.*;

/**
 * Class that constructs the network given the biological interactions and the
 * expression vectors
 *
 * @author nalcaraz
 */
public class KPMGraph extends SparseGraph<GeneNode, GeneEdge> implements Serializable {

    /**
     * Mapping of node id's to their expression vector
     */
    private Map<String, Map<String, int[]>> expressionIdToNodeMap;
    private Map<String, Map<String, int[]>> expressionIdToNodeOriginal;
    /**
     * Mapping of node id's to gene id's
     */
    private Map<String, String> nodeIdToSymbol;
    /**
     * List of edges (pairs of node id's)
     */
    private LinkedList<String[]> edgeList;
    /**
     * Mapping from node id's to node structures for quick access
     */
    private Map<String, GeneNode> nodeIdToGeneNode;
    /**
     * List of predefined genes that will be treated as valid nodes with a full
     * vector of diff. exp. cases independent of their true expression values.
     */
    public Set<String> positiveList;
    /**
     * List of predefined genes that will be treated as invalid nodes with an
     * empty vector of diff. exp. cases independent of their true expression
     * values.
     */
    public Set<String> negativeList;
    /**
     * A map from the expression Ids to sets of Nodes in the graph that do not
     * map to any gene ID in the expression studies
     */
    public Map<String, Set<String>> backNodesMap;
    /**
     * A map from the expression Ids to a set of genes in the expression studies
     * that were not mapped to any node in the graph
     */
    public Map<String, Set<String>> backGenesMap;
    public Map<String, Integer> numCasesMap;
    public static final char IN_POSITIVE = 'p';
    public static final char IN_NEGATIVE = 'n';
    public static final char REWIRE = 'r';
    public static final String OR_NODE_VALID_CONDITION = "OR";
    public static final String AND_NODE_VALID_CONDITION = "AND";
    public char treatBackNodes;

    // private Map<String, Set> edgeIdToPaths;
    /**
     * Constructs a KPM-Network.
     *
     * @param expressionIdToNodeMap contains the p-values for each node
     * @param edgeList Contains the edges; an element elem in the linked list
     * has an edge from elem[0] to elem[1]
     * @param nodeIdToSymbol Contains the name of the gene corresponding to that
     * node, as for instance "ATXN2"
     */
    public KPMGraph(Map<String, Map<String, int[]>> expressionIdToNodeMap,
            LinkedList<String[]> edgeList, Map<String, String> nodeIdToSymbol, Map<String, Integer> num_cases_map) {
        super();
        this.expressionIdToNodeMap = expressionIdToNodeMap;
        this.expressionIdToNodeOriginal = expressionIdToNodeMap;
        this.nodeIdToSymbol = nodeIdToSymbol;
        this.edgeList = edgeList;
        nodeIdToGeneNode = new HashMap<String, GeneNode>();
        positiveList = new HashSet<String>();
        negativeList = new HashSet<String>();
        backNodesMap = new HashMap<String, Set<String>>();
        backGenesMap = new HashMap<String, Set<String>>();
        for (String nodeId : expressionIdToNodeMap.keySet()) {
            backNodesMap.put(nodeId, new HashSet<String>());
        }
        numCasesMap = num_cases_map;
        for (String expId : num_cases_map.keySet()) {
            backGenesMap.put(expId, new HashSet<String>());
        }

        treatBackNodes = 'n';
        createGraph();
    }

    public KPMGraph(Map<String, Map<String, int[]>> expressionIdToNodeMap,
            LinkedList<String[]> edgeList, Map<String, String> nodeIdToSymbol,
            Map<String, Set<String>> backNodesMap, Map<String, Set<String>> backGenesMap, Map<String, Integer> num_cases_map) {
        super();
        this.expressionIdToNodeMap = expressionIdToNodeMap;
        this.expressionIdToNodeOriginal = expressionIdToNodeMap;
        this.nodeIdToSymbol = nodeIdToSymbol;
        this.edgeList = edgeList;
        nodeIdToGeneNode = new HashMap<String, GeneNode>();
        positiveList = new HashSet<String>();
        negativeList = new HashSet<String>();
        this.backNodesMap = backNodesMap;
        this.backGenesMap = backGenesMap;
        numCasesMap = num_cases_map;
        treatBackNodes = 'n';
        createGraph();
    }

    /**
     * Copy constructor.
     *
     */
    public KPMGraph(KPMGraph g) {
        super();
        
    	expressionIdToNodeMap = new HashMap<String, Map<String, int[]>>(g.expressionIdToNodeMap);        
        this.expressionIdToNodeOriginal = expressionIdToNodeMap;
        nodeIdToSymbol = new HashMap<String, String>(g.nodeIdToSymbol);
        edgeList = new LinkedList<String[]>(g.edgeList);
        nodeIdToGeneNode = new HashMap<String, GeneNode>();
        backNodesMap = new HashMap<String, Set<String>>(g.backNodesMap);
        backGenesMap = new HashMap<String, Set<String>>(g.backGenesMap);
        treatBackNodes = g.treatBackNodes;
        numCasesMap = new HashMap<String, Integer>(g.numCasesMap);
        positiveList = new HashSet<String>(g.positiveList);
        negativeList = new HashSet<String>(g.negativeList);
        
        // Ensure we copy the vertices.
        for (GeneNode node : g.getVertices()) {
        	GeneNode newNode = new GeneNode(node);
        	this.nodeIdToGeneNode.put(newNode.nodeId, newNode);
        	this.addVertex(newNode);
        }
        
        for (String[] pair : edgeList) {
            String from = pair[0];
            String to = pair[1];
            // Modified ID to be more compatible with cytoscape id's
            String edgeId = to + " (pp) " + from;
            GeneEdge edge = new GeneEdge(edgeId);
            
            if(!nodeIdToGeneNode.containsKey(from) || !nodeIdToGeneNode.containsKey(to)){
            	//Edge removed, don't add.
            	continue;
            }
            
            this.addEdge(edge, nodeIdToGeneNode.get(to), nodeIdToGeneNode.get(from));
        }
    } 
    
    private void createGraph() {
        processBackNodes(treatBackNodes);

        for (String nodeId : expressionIdToNodeMap.keySet()) {
            Map<String, int[]> exprVectors = expressionIdToNodeMap.get(nodeId);
            nodeIdToGeneNode.put(nodeId, new GeneNode(nodeId, nodeIdToSymbol.get(nodeId),
                    expressionIdToNodeMap.get(nodeId)));
        }

        for (String[] pair : edgeList) {
            String from = pair[0];
            String to = pair[1];
            // Modified ID to be more compatible with cytoscape id's
            String edgeId = to + " (pp) " + from;
            GeneEdge edge = new GeneEdge(edgeId);
            this.addEdge(edge, nodeIdToGeneNode.get(to), nodeIdToGeneNode.get(from));
        }



//        for (String nodeId: expressionIdToNodeMap.keySet()) {
//            Map<String, int[]> nodeIdToExpression = expressionIdToNodeMap.get(nodeId);
//            for (String expId: nodeIdToExpression.keySet()) {
//                if (! nodeIdToGeneNode.containsKey(nodeId)) {
//                    backGenesMap.get(expId).add(nodeId);
//                }
//            }                       
//        }


    }

    public LinkedList<String[]> getEdgeList() {
        return edgeList;
    }

    public Map<String, Map<String, int[]>> getExpressionIdToNodeMap() {
        return expressionIdToNodeMap;
    }

    public Map<String, String> getNodeIdToGeneId() {
        return nodeIdToSymbol;
    }

    public Map<String, GeneNode> getNodeIdToGeneNode() {
        return nodeIdToGeneNode;
    }

   

//    private void updateNodesInPositiveList() {
//       int[] differenceArray = new int[numCases]; 
//       for (String posId: positiveList) {
//           nodeIdToGeneNode.get(posId).setDifferenceArray(differenceArray);
//       } 
//    }
//    
//    private void updateNodesInNegativeList() {
//        
//    }
    public double getAverageDegree() {
        return ((double) (getEdgeCount() * 2)) / getVertexCount();
    }

    /**
     * Call this is you want to run a second algorithm on the graph and want all
     * the algorithm-dependent variables to be re-initialized
     */
    public void refreshGraph(KPMSettings kpmSettings) {

        for (String nodeId : positiveList) {
            if (nodeIdToGeneNode.containsKey(nodeId)) {
                GeneNode node = nodeIdToGeneNode.get(nodeId);
                Map<String, int[]> expMap = node.getDifferenceIntMap();
                for (String expId : expMap.keySet()) {
                    int[] exp = expMap.get(expId);
                    Arrays.fill(exp, 1);
                    expMap.put(expId, exp);
                }
                node.setDifferenceIntMap(expMap);
                nodeIdToGeneNode.put(nodeId, node);
            }

            if (expressionIdToNodeMap.containsKey(nodeId)) {
                Map<String, int[]> expMap = expressionIdToNodeMap.get(nodeId);
                for (String expId : expMap.keySet()) {
                    int[] exp = expMap.get(expId);
                    Arrays.fill(exp, 1);
                    expMap.put(expId, exp);
                }
                expressionIdToNodeMap.put(nodeId, expMap);
            }
        }

        for (String nodeId : negativeList) {
            if (nodeIdToGeneNode.containsKey(nodeId)) {
                GeneNode node = nodeIdToGeneNode.get(nodeId);
                Map<String, int[]> expMap = node.getDifferenceIntMap();
                for (String expId : expMap.keySet()) {
                    int[] exp = expMap.get(expId);
                    Arrays.fill(exp, 0);
                    expMap.put(expId, exp);
                }
                node.setDifferenceIntMap(expMap);
                nodeIdToGeneNode.put(nodeId, node);
            }

            if (expressionIdToNodeMap.containsKey(nodeId)) {
                Map<String, int[]> expMap = expressionIdToNodeMap.get(nodeId);
                for (String expId : expMap.keySet()) {
                    int[] exp = expMap.get(expId);
                    Arrays.fill(exp, 0);
                    expMap.put(expId, exp);
                }
                expressionIdToNodeMap.put(nodeId, expMap);
            }
        }

        if (kpmSettings.COMBINE_OPERATOR == Combine.OR) {

            for (GeneNode node : getVertices()) {
                boolean isValid = false;
                for (String expId : kpmSettings.CASE_EXCEPTIONS_MAP.keySet()) {
                    if (node.getNumDiffExpressedCases(expId) >= node.getNumCases(expId) - kpmSettings.CASE_EXCEPTIONS_MAP.get(expId)) {
                        isValid = true;
                        break;
                    }
                }
                node.setIsValid(isValid);
                node.setPheromone(0.5, kpmSettings.TAU_MIN);
                node.setLastIterationPheromoneUpdated(0);
            }
        } else if (kpmSettings.COMBINE_OPERATOR == Combine.AND) {
            for (GeneNode node : getVertices()) {
                boolean isValid = true;
                for (String expId : kpmSettings.CASE_EXCEPTIONS_MAP.keySet()) {
                    if (node.getNumDiffExpressedCases(expId) < node.getNumCases(expId) - kpmSettings.CASE_EXCEPTIONS_MAP.get(expId)) {
                        isValid = false;
                        break;
                    }
                }
                node.setIsValid(isValid);
                node.setPheromone(0.5, kpmSettings.TAU_MIN);
                node.setLastIterationPheromoneUpdated(0);
            }
        } else if (kpmSettings.COMBINE_OPERATOR == Combine.CUSTOM) {
            for (GeneNode node : getVertices()) {
                Map<String, Object> context = new HashMap<String, Object>();

                for (String expId : kpmSettings.CASE_EXCEPTIONS_MAP.keySet()) {
                    boolean aux =
                            (node.getNumCases(expId) - node.getNumDiffExpressedCases(expId))
                            <= kpmSettings.CASE_EXCEPTIONS_MAP.get(expId);
                    context.put(expId, aux);
                }
                
                boolean val = false;
                if (!context.isEmpty()) {
                    try {
                        val = MVEL.evalToBoolean(kpmSettings.COMBINE_FORMULA, context);
                    } catch (Exception e) {
                        String msg = e.getMessage();
                        if (msg.contains("unresolvable property or identifier")) {
                            continue;
                        }
                    }
                }
                node.setIsValid(val);
                node.setPheromone(0.5, kpmSettings.TAU_MIN);
                node.setLastIterationPheromoneUpdated(0);
            }
        }


    }

    public void setAllNodesValid() {
        for (GeneNode n : getVertices()) {
            n.setIsValid(true);
        }
    }

    public Set<String> getNegativeList() {
        return negativeList;
    }

    public void setNegativeList(Set<String> negativeList) {
        this.negativeList = negativeList;
//		CyAttributes nodeAtts = Cytoscape.getNodeAttributes();
//		for (String ID : negativeList) {
//			nodeAtts.setAttribute(ID, "Pos/Neg", "Neg");
//		}
    }

    public Set<String> getPositiveList() {
        return positiveList;
    }

    public void setPositiveList(Set<String> positiveList) {
        this.positiveList = positiveList;
//		CyAttributes nodeAtts = Cytoscape.getNodeAttributes();
//		for (String ID : positiveList) {
//			nodeAtts.setAttribute(ID, "Pos/Neg", "Pos");
//		}
    }

    public char getTreatBackNodes() {
        return treatBackNodes;
    }

    public void setTreatBackNodes(char treatBackNodes) {
        this.treatBackNodes = treatBackNodes;
    }

    public Map<String, Set<String>> getBackNodesMap() {
        return backNodesMap;
    }

    public Map<String, Set<String>> getBackGenesMap() {
        return backGenesMap;
    }

//    public KPMGraph computeListCopy() {
//        HashMap<String, GeneNode> nodeId2GeneNodeCopy = new HashMap<String, GeneNode>(this.nodeIdToGeneNode);
//        HashMap<String, Map<String, int[]>> expressionIdToNodeMapCopy = new HashMap<String, Map<String, int[]>>(this.expressionIdToNodeMap);
//        LinkedList<String[]> edgeListCopy = new LinkedList<String[]>(this.edgeList);
//        
//        
//        for (String nodeId: nodeId2GeneNodeCopy.keySet()) {
//            int[] exp = nodeId2ExpressionCopy.get(nodeId);
//            boolean inPos = positiveList.contains(nodeId);
//            boolean inNeg = negativeList.contains(nodeId);
//            if (inPos && inNeg) {                
//                 Throw Exception ??
//            } else if (inPos) {
//                for (int i = 0; i < exp.length; i++) {
//                    exp[i] = 1;
//                }
//                nodeId2ExpressionCopy.put(nodeId, exp);
//            } else if (inNeg) {
//                for (int i = 0; i < exp.length; i++) {
//                    exp[i] = 0;
//                }
//                nodeId2ExpressionCopy.put(nodeId, exp);
//            }
//        }
//        
//        KPMGraph ans = new KPMGraph(nodeId2ExpressionCopy, edgeListCopy, nodeIdToSymbol);
//        ans.setNegativeList(this.negativeList);
//        ans.setPositiveList(this.positiveList);
//        
//        return ans;
//    }
    public void resetGraph() {
        this.expressionIdToNodeMap = this.expressionIdToNodeOriginal;
        positiveList.clear();
        negativeList.clear();
        createGraph();
    }

    public void processBackNodes(char treat) {
        int fill = 0;
        if (treat == IN_POSITIVE) {
            fill = 1;
        }
        for (String nodeId : backNodesMap.keySet()) {
            Set<String> expSet = backNodesMap.get(nodeId);
            Map<String, int[]> expVectors = new HashMap<String, int[]>();
            if (expressionIdToNodeMap.containsKey(nodeId)) {
                expVectors = expressionIdToNodeMap.get(nodeId);
            }

            for (String expId : expSet) {
                int[] exp = new int[numCasesMap.get(expId)];
                Arrays.fill(exp, fill);
                expVectors.put(expId, exp);
            }

            expressionIdToNodeMap.put(nodeId, expVectors);
            if (!nodeIdToGeneNode.containsKey(nodeId)) {
                nodeIdToGeneNode.put(nodeId, new GeneNode(nodeId,
                        nodeIdToSymbol.get(nodeId), expVectors));
            } else {
                nodeIdToGeneNode.get(nodeId).setDifferenceIntMap(expVectors);
            }
        }


    }

//    public KPMGraph processSpecialNodes(char treat, Set<String> positiveList, Set<String> negativeList) {
//        HashMap<String, GeneNode> nodeId2GeneNodeCopy = new HashMap<String, GeneNode>(this.nodeIdToGeneNode);
//        HashMap<String, Map<String, int[]>> expressionIdToNodeMapCopy = new HashMap<String, Map<String,int[]>>(this.expressionIdToNodeMap);
//        LinkedList<String[]> edgeListCopy = new LinkedList<String[]>(this.edgeList);
//        
//        
//       
//        
//        if (treat == IN_POSITIVE) {
//            for (String expId: backNodesMap.keySet()) {
//                for (String nodeId: backNodesMap.get(expId)) {
//                    positiveList.add(nodeId);
//                }
//            }
//        }
//        
//        if (treat == IN_NEGATIVE) {
//            for (String expId: backNodesMap.keySet()) {
//                for (String nodeId: backNodesMap.get(expId)) {
//                    negativeList.add(nodeId);
//                }
//            }
//        }
//        
//        for (String nodeId: positiveList) {
//            Map<String, int[]> expMap = new HashMap<String, int[]>();
//            for (String expId: expressionIdToNodeMapCopy.keySet()) {
//                Map<String, int[]> nodeId2Expression = expressionIdToNodeMapCopy.get(expId);
//                int[] exp = nodeId2Expression.get(nodeId);
//                Arrays.fill(exp, 1);
//                expMap.put(expId, exp);
//                expressionIdToNodeMapCopy.get(expId).put(nodeId,exp);
//            }
//            nodeId2GeneNodeCopy.get(nodeId).setDifferenceMap(expMap);
//            
//        }
//        
//        for (String nodeId: negativeList) {
//            Map<String, int[]> expMap = new HashMap<String, int[]>();
//            for (String expId: expressionIdToNodeMapCopy.keySet()) {
//                Map<String, int[]> nodeId2Expression = expressionIdToNodeMapCopy.get(expId);
//                int[] exp = nodeId2Expression.get(nodeId);
//                Arrays.fill(exp, 0);
//                expMap.put(expId, exp);
//                expressionIdToNodeMapCopy.get(expId).put(nodeId,exp);
//            }
//            nodeId2GeneNodeCopy.get(nodeId).setDifferenceMap(expMap);
//            
//        }
//        
//        KPMGraph ans = new KPMGraph(expressionIdToNodeMapCopy, edgeListCopy, nodeIdToSymbol);
//        ans.setNegativeList(negativeList);
//        ans.setPositiveList(positiveList);
//        
//        if (treat == REWIRE) {
//            System.out.println("BACK NODES: " + backNodes.size());
//            for (GeneNode node : this.getVertices()) {
//                if (backNodes.contains(node.getNodeId())) {
//                    LinkedList<GeneNode> neighbors = new LinkedList<GeneNode>(this.getNeighbors(node));
//                    for (int i = 0; i < neighbors.size() - 1; i++) {
//                        for (int j = i + 1; j < neighbors.size(); j++) {
//                            GeneNode n1 = neighbors.get(i);
//                            GeneNode n2 = neighbors.get(j);
//                            if (ans.containsVertex(n1) && ans.containsVertex(n2)) {
//                                if (!ans.isNeighbor(n1, n2)) {
//                                    String edgeId = n1.getNodeId() + " - " + n2.getNodeId();
//                                    GeneEdge edge = new GeneEdge(edgeId);
//                                    ans.addEdge(edge, n1, n2, EdgeType.UNDIRECTED);
//                                }
//                            }
//                        }
//                    }
//                    
//                    ans.removeGeneNode(node);
//                }
//
//
//            }
//        }
//        
//        return ans;
//    }
    public boolean removeGeneNode(GeneNode node) {
        String nodeId = node.getNodeId();
        for (Map<String, int[]> nodeIdToExpression : expressionIdToNodeMap.values()) {
            nodeIdToExpression.remove(nodeId);
        }

        LinkedList<String[]> newList = new LinkedList<String[]>();
        for (String[] edge : edgeList) {
            if (!(edge[0].equals(nodeId) || edge[1].equals(nodeId))) {
                newList.add(edge);
            }
        }

        edgeList = newList;
        return this.removeVertex(node);
    }

    /**
     * Returns a copy of this graph with randomized node labels (expression
     * values)
     *
     * @return
     */
    public KPMGraph randomize() {

        Map<String, Map<String, int[]>> newNode2Exp = randomizeExpression();
        HashMap<String, String> newNodeIdToSymbol = new HashMap<String, String>(nodeIdToSymbol);
        LinkedList<String[]> newEdgeList = new LinkedList<String[]>(edgeList);
        // HashSet<String> newBackNodes = new HashSet<String>(backNodes);
        KPMGraph ans = new KPMGraph(newNode2Exp, newEdgeList, newNodeIdToSymbol, numCasesMap);
        return ans;
    }

    private Map<String, Map<String, int[]>> randomizeExpression() {
        Map<String, Map<String, int[]>> newMap = new HashMap<String, Map<String, int[]>>();
        for (String expId : expressionIdToNodeMap.keySet()) {
            Map<String, int[]> nodeIdToExpression = expressionIdToNodeMap.get(expId);
            List<int[]> valueList = new ArrayList<int[]>(nodeIdToExpression.values());
            Collections.shuffle(valueList);
            Iterator<int[]> valueIt = valueList.iterator();
            HashMap<String, int[]> aux = new HashMap<String, int[]>();
            for (String key : nodeIdToExpression.keySet()) {
                aux.put(key, valueIt.next());
            }
            newMap.put(expId, aux);
        }


        return newMap;
    }

    /*
     * Returns the list of all edges connecting the nodes of the given set
     *
     * Does not work for directed graphs
     */
    public List<String[]> getEdgesConnecting(Collection<GeneNode> nodes) {
        List<String[]> resList = new LinkedList<String[]>();

        LinkedList<GeneNode> nodeList = new LinkedList<GeneNode>(nodes);

        for (int i = 0; i < nodeList.size() - 1; i++) {
            for (int j = i + 1; j < nodeList.size(); j++) {
                GeneNode node1 = nodeList.get(i);
                GeneNode node2 = nodeList.get(j);
                if (isNeighbor(node1, node2)) {
                    String[] toAdd = new String[2];
                    toAdd[0] = node1.getNodeId();
                    toAdd[1] = node2.getNodeId();
                    resList.add(toAdd);
                }
            }
        }

        return resList;
    }

    public List<GeneEdge> getConnectingEdges(Collection<GeneNode> nodes) {
        List<GeneEdge> resList = new LinkedList<GeneEdge>();
        LinkedList<GeneNode> nodeList = new LinkedList<GeneNode>(nodes);

        for (int i = 0; i < nodeList.size() - 1; i++) {
            for (int j = i + 1; j < nodeList.size(); j++) {
                GeneNode node1 = nodeList.get(i);
                GeneNode node2 = nodeList.get(j);
                if (isNeighbor(node1, node2)) {
                    Set<GeneEdge> set1 = new HashSet<GeneEdge>(this.getIncidentEdges(node1));
                    Set<GeneEdge> set2 = new HashSet<GeneEdge>(this.getIncidentEdges(node2));
                    set1.retainAll(set2);
                    resList.add(set1.iterator().next());
                }
            }
        }
        return resList;
    }
    
    public Collection<String> getNodeIdSet() {
        return nodeIdToGeneNode.keySet();
    }
    
    public void SaveToFile(String filePath){
    	KPMGraphSaver.SaveGraphToFile(this, filePath);
    }
}

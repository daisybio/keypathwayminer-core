package dk.sdu.kpm.runners;

import dk.sdu.kpm.graph.*;
import edu.uci.ics.jung.algorithms.shortestpath.BFSDistanceLabeler;
import edu.uci.ics.jung.graph.SparseGraph;
import edu.uci.ics.jung.graph.util.EdgeType;
import edu.uci.ics.jung.graph.util.Pair;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
*
* @author nalcaraz
*/
public class BENRemover implements Serializable {
   
   public Set<String> exceptionNodes;
   
   public Set<String> nodesToCheck;
   
   public KPMGraph kpmGraph;
   
   public SparseGraph<String, String> workingGraph;
   
   public Set<String> benNodes;
   
   // We need to know the whole graph to 
   // get the edges of a result. 
   public BENRemover(KPMGraph kpmGraph) {
       this.kpmGraph = kpmGraph;
       exceptionNodes = new HashSet<String>();
       nodesToCheck = new HashSet<String>();
       workingGraph = new SparseGraph<String, String>();
       benNodes = new HashSet<String>();        
   }
   
   public Set<String> getBENNodes() {
       return benNodes;
   }
   
   // Convert a Jung graph to a result
   private Result toResult(SparseGraph<String, String> g) {
       GenericResult result = new GenericResult();
       for (String nodeId: g.getVertices()) {
           result.add(kpmGraph.getNodeIdToGeneNode().get(nodeId));
       }
       return result;
   }

   public Result filterBENs(Result result) {
       // Convert a result to a JUNG graph (easier to work with)
       Collection<GeneNode> nodes = result.getVisitedNodes().values();
       List<String[]> edges = kpmGraph.getEdgesConnecting(nodes);
       SparseGraph<String, String> ret = new SparseGraph<String, String>();
       for (GeneNode node: nodes) {
           String nodeId = node.getNodeId();
           if (!node.isValid()) {
               exceptionNodes.add(nodeId);
               nodesToCheck.add(nodeId);
           }
           workingGraph.addVertex(nodeId);
       }       
       for (String[] edge: edges) {
           String nodeId1 = edge[0];
           String nodeId2 = edge[1];
           ret.addEdge(nodeId1 + "-" + nodeId2, nodeId1, nodeId2, 
                   EdgeType.UNDIRECTED);                        
       }       
       return toResult(removeBENs(ret));
   }
   
   /**
    * THe main method for removing BEN's
    * 
    * @param graph the network to remove BEN's from
    * @return the filtered subnetwork
    */
   private SparseGraph<String, String> removeBENs(
           SparseGraph<String, String> graph) {
       // If the subgraph is empty or we don't have exceptions to check
       // we return the same graph
       if (graph.getVertices().isEmpty() || nodesToCheck.isEmpty()) {
           return graph;
       }


       while (!nodesToCheck.isEmpty()) {
           String node = nodesToCheck.iterator().next();
           nodesToCheck.remove(node);

           // Try remove the node from the graph
           Collection<String> tryRemoveEdges = graph.getIncidentEdges(node);
           HashMap<String, Pair<String>> edgeEndPoints = new HashMap<String, Pair<String>>();
           for (String edge : tryRemoveEdges){
               Pair<String> endpoints = graph.getEndpoints(edge);
               if(!edgeEndPoints.containsKey(edge)){
                   edgeEndPoints.put(edge, endpoints);
               }
           }

           graph.removeVertex(node);
           
           // We compute the connected components
           List<SparseGraph<String, String>> ccs = getConnectedComponents(graph);

           int validCC = 0;
           
           for (SparseGraph<String, String> cc : ccs) {
               // We mark all nodes in connected components
               // containing only exception nodes, marked them 
               // as BEN's and remove them
               if (!hasValidNodes(cc)) {
                   for (String nodeId : cc.getVertices()) {
                       benNodes.add(nodeId);
                       nodesToCheck.remove(nodeId);
                       graph.removeVertex(nodeId);
                   }
               } else {
                   validCC++;
               }
           }
           // If only one connected component with non-exception nodes, 
           // then we just mark
           // the exception node es BEN and remove it
           if (validCC < 2) {
               benNodes.add(node);
           }else{

               // Optimization, instead of creating a deep copy of the graph every time.
               // We re-add the vertex + incident edges again.
               graph.addVertex(node);
               for(String edge : tryRemoveEdges){
                   if(!edgeEndPoints.containsKey(edge)){
                       continue;
                   }

                   Pair<String> endpoints = edgeEndPoints.get(edge);
                   String v1 = endpoints.getFirst();
                   String v2 = endpoints.getSecond();
                   graph.addEdge(edge, v1, v2);
               }

           }
           
       }
       
       return graph;
   }
   
   // To check if a connected component has at least one non-exception node
   private boolean hasValidNodes(SparseGraph<String, String> graph) {
       for (String node: graph.getVertices()) {
           if (!exceptionNodes.contains(node)) {
               return true;
           }
       }
       return false;
   }
   
   // Get a list of connected components given a JUNG graph
   private List<SparseGraph<String, String>> getConnectedComponents(
           SparseGraph<String, String> graph) {
       
       List<SparseGraph<String, String>> ccs = 
               new ArrayList<SparseGraph<String, String>>();
       Set<String> toVisit = new HashSet<String>(graph.getVertices());
       // Use the BFS distance labeler in JUNG
       BFSDistanceLabeler<String, String> bfs = 
               new BFSDistanceLabeler<String, String>();
       while (!toVisit.isEmpty()) {
           String startNode = toVisit.iterator().next();
           SparseGraph<String, String> cc = new SparseGraph<String, String>();
           bfs.labelDistances(graph, startNode);
           Collection<String> visitedVertices = bfs.getVerticesInOrderVisited();
           Map<String, Pair<String>> connectingEdges = getConnectingEdges(graph, 
                   visitedVertices);
           for (String visited: bfs.getVerticesInOrderVisited()) {
               toVisit.remove(visited);
               cc.addVertex(visited);                
           }
           for (String edge: connectingEdges.keySet()) {
               cc.addEdge(edge, connectingEdges.get(edge), EdgeType.UNDIRECTED);
           }
           ccs.add(cc);
       }
       
       return ccs;
       
   }
   
   private Map<String, Pair<String>> getConnectingEdges(
           SparseGraph<String, String> graph, Collection<String> nodes) {
       Map<String, Pair<String>> map = new HashMap<String, Pair<String>>();
       List<String> nodeList = new ArrayList<String>(nodes);
       for (int i = 0; i < nodeList.size() - 1; i++) {
           for (int j = i + 1; j < nodeList.size(); j++) {
               String n1 = nodeList.get(i);
               String n2 = nodeList.get(j);
               String edge = "";
               if ((edge = graph.findEdge(n1, n2)) != null) {
                   map.put(edge, new Pair(n1, n2));
               }
           }
       }                                
       return map;        
   }
   
   private SparseGraph<String, String> copyGraph(
           SparseGraph<String, String> graph) {
       SparseGraph<String, String> g = new SparseGraph<String, String>();
       
       for (String node: graph.getVertices()) {
           g.addVertex(node);
       }
       
       for (String edge: graph.getEdges()) {
           String[] pair = edge.split("-");
           if(pair.length != 2){
        	   continue;
           }
           String node1 = pair[0].trim();
           String node2 = pair[1].trim();
           g.addEdge(edge, node1, node2, EdgeType.UNDIRECTED);
       }
       return g;
   }
}

/*class BENRemover {
	private HashSet<String[]> edgeList;
	private IKPMTaskMonitor taskMonitor;

	// Variables that changes per iteration
	private HashMap<String, GeneNode> currentDFStree; 
	private HashMap<String, Boolean> nodeVisited; 
	private Map<String, GeneNode> currentPathWay; 
	private HashSet<String[]> currentEdgeList;


	public BENRemover(){
		this.edgeList = new HashSet<String[]>();
		this.taskMonitor = new KPMDummyTaskMonitor();
	}

	public BENRemover(IKPMTaskMonitor monitor){
		this();
		if(monitor != null){
			this.taskMonitor = monitor;
		}
	}
	
	public void executeWithKPMResults(){
		
		
		this.edgeList = getEdgeListCopy();

		// Iterating over all solutions
		for(List<Integer> params : KPMParameters.STATS_MAP.keySet()){
			RunStats rs = KPMParameters.STATS_MAP.get(params);
			int k = params.get(0);
			int indx = 1;

			for (String lid : KPMParameters.externalToInternalIDManager.getInternalIdentifiers()) {
				if(KPMParameters.MIN_L.get(lid) == KPMParameters.MAX_L.get(lid)){
					// Not a range, don't do charting
					indx++;
					continue;
				}

				int l = params.get(indx);		

				this.taskMonitor.setTitle(String.format("Key Pathway Miner - Removing BEN's for K = %d, L = %d", k, l));
				for(Result res: rs.getResults()){
					HashMap<String, GeneNode> bens = filterBENs(res);		
				}
			}
		}
	}

	*//**
	 * Gets a copy of the edge list, containing only edges that are contained within the results
	 * @return
	 *//*
	private HashSet<String[]> getEdgeListCopy(){

		// Resulting list of edges 
		HashSet<String[]> edges = new HashSet<String[]>();

		// List of found unique node IDs that are part of a solution
		HashSet<String> solutionNodeIDs = new HashSet<String>();

		// Iterate through all the results, to get the unique nodeIDs
		for(List<Integer> params : KPMParameters.STATS_MAP.keySet()){
			RunStats rs = KPMParameters.STATS_MAP.get(params);
			for(Result res: rs.getResults()){
				Map<String, GeneNode> keyPathWay = res.getVisitedNodes();
				for(String nodeID : keyPathWay.keySet()){
					if(!solutionNodeIDs.contains(nodeID)){
						solutionNodeIDs.add(nodeID);
					}
				}
			}
		}

		//Go through the edge list once, HashSet makes lookup in constant time, so its effective
		for(String[] edge: KPMParameters.MAIN_GRAPH.getEdgeList()){

			// Only see edges with length 2
			if(edge.length != 2){
				continue;
			}

			// Only find edges that contains nodes that is part of a solution
			if(solutionNodeIDs.contains(edge[0]) && solutionNodeIDs.contains(edge[1])){
				edges.add(edge);
			}
		}

		return edges;
	}

	*//**
	 * For each result, given the edge list, find exception nodes that are not BENs
	 * @param result
	 * @return
	 *//*
	private HashMap<String, GeneNode> filterBENs(Result result){
		Map<String, GeneNode> keyPathWay = result.getVisitedNodes();
		this.taskMonitor.setStatusMessage("Started filtering on new result.");

		// Put all EN's in a list
		LinkedList<GeneNode> exceptionNodes = new LinkedList<GeneNode>();
		for(GeneNode node : result.getVisitedNodes().values()){
			if(!node.isValid()){
				exceptionNodes.addLast(node);
			}
		}

		// Create empty BEN list
		HashMap<String, GeneNode> BENs = new HashMap<String, GeneNode>();

		int count = 1;
		String currentNodeID = "";
		// While ENs in L 
		while(!exceptionNodes.isEmpty()){

			// Current Error Node
			GeneNode errorNode = exceptionNodes.getFirst();
			this.taskMonitor.setStatusMessage(String.format("Currently working on node '%s', count %d", errorNode.getNodeId(), count));
			if(!currentNodeID.equals(errorNode.getNodeId())){
				currentNodeID = errorNode.getNodeId();
				count = 0;
			}
			count++;
			// test-remove the EN from pathway K			
			Map<String, GeneNode> testPathway = getPathwayWithoutEN(keyPathWay, errorNode);
			HashSet<String[]> testEdgeList = getEdgeListWithoutEN(errorNode);

			// DFS for finding Connected Components:
			HashSet<HashMap<String, GeneNode>> ccs = getConnectedComponentsDFS(testPathway, testEdgeList);

			// count the number of resulting connected components
			int nrCCs = ccs.size();

			System.out.println("nrCCs = " + nrCCs);
			
			boolean foundCase = false;
			
			// case 1: one CC 
			if(nrCCs == 1){
				BENs.put(errorNode.getNodeId(), errorNode);
				exceptionNodes.remove(errorNode);
				foundCase = true;
				this.taskMonitor.setStatusMessage("Found Case 1");

				// case 2: two CCs and one of the CCs consists only of ENs 
			}else if(nrCCs == 2){
				for(HashMap<String, GeneNode> connectedComponent : ccs){
					if(allErrorNodes(connectedComponent)){
						this.taskMonitor.setStatusMessage("Found Case 2");

						// Add error node to BEN list, and remove from exception nodes list
						BENs.put(errorNode.getNodeId(), errorNode);
						exceptionNodes.remove(errorNode.getNodeId());

						// Add all the elements of that connected component to BEN list, and remove from exception nodes list
						for(GeneNode ccNode : connectedComponent.values()){
							BENs.put(ccNode.getNodeId(), ccNode);
							exceptionNodes.remove(ccNode.getNodeId());
						}

						// We assume only one of them can consist of all error nodes, so might as well break.
						foundCase = true;
						break;
					}
				}

				// case 3: >2 CCs and one of the CCs consists only of ENs 	
			}else if(nrCCs > 2){
				for(HashMap<String, GeneNode> connectedComponent : ccs){
					if(allErrorNodes(connectedComponent)){
						this.taskMonitor.setStatusMessage("Found Case 3");

						// Add all the elements of that connected component to BEN list, and remove from exception nodes list
						for(GeneNode ccNode : connectedComponent.values()){
							BENs.put(ccNode.getNodeId(), ccNode);
							exceptionNodes.remove(ccNode.getNodeId());
						}

						// We assume only one of them can consist of all error nodes, so might as well break.
						foundCase = true;
						break;
					}
				}
			}

			if(!foundCase)
				exceptionNodes.remove(errorNode);
			}
	

		this.taskMonitor.setStatusMessage("Finished working on result set.");

		return BENs;
	}

	*//**
	 * Gets the edge list, where the errorNode has been removed.
	 * @param errorNode
	 * @return
	 *//*
	private HashSet<String[]> getEdgeListWithoutEN(GeneNode errorNode){
		HashSet<String[]> copyEdgeList = new HashSet<String[]>();

		String errorNodeID = errorNode.getNodeId();

		for(String[] edge: edgeList){
			if(edge.length != 2){
				continue;
			}

			// Only add the edge if neither of the two edges it connect, is the error node  
			if(!errorNodeID.equals(edge[0]) && !errorNodeID.equals(edge[1])){
				copyEdgeList.add(edge);
			}
		}

		return copyEdgeList;
	}

	*//**
	 * Returns a copy of a pathway, without the error node we are working on.
	 * @param keyPathWay
	 * @param errorNode
	 * @return
	 *//*
	private HashMap<String, GeneNode> getPathwayWithoutEN(Map<String, GeneNode> keyPathWay, GeneNode errorNode){
		HashMap<String, GeneNode> copyPathway = new HashMap<String, GeneNode>();

		// Iterate through the pathway
		for(String nodeID : keyPathWay.keySet()){

			// All but the error node is added to the list
			if(!errorNode.getNodeId().equals(nodeID)){
				copyPathway.put(nodeID, keyPathWay.get(nodeID));
			}
		}

		return copyPathway;
	}

	*//**
	 * Get connected components via depth first search
	 * @return
	 *//*
	private HashSet<HashMap<String, GeneNode>> getConnectedComponentsDFS(Map<String, GeneNode> testPathWay, HashSet<String[]> testEdgeList){
		HashSet<HashMap<String, GeneNode>> connectedComponents = new HashSet<HashMap<String, GeneNode>>();

		// Initially, no nodes has been visited.
		this.nodeVisited = new HashMap<String, Boolean>();
		for(String nodeID : testPathWay.keySet()){
			nodeVisited.put(nodeID, false);
		}

		// Initially get the first unvisited node from the pathway.
		this.currentPathWay = testPathWay;
		this.currentEdgeList = testEdgeList;
		GeneNode currentNode = getNextUnvisitedNode();

		int count = 1;
		String currentNodeID = currentNode.getNodeId();
		// While there are still unvisited nodes in the pathway
		while(currentNode != null){

			this.taskMonitor.setStatusMessage(String.format("Currently working on DFS cc's for node '%s', count %d", currentNode.getNodeId(), count));
			if(!currentNodeID.equals(currentNode.getNodeId())){
				currentNodeID = currentNode.getNodeId();
				count = 0;
			}
			count++;

			// The current DFS tree.
			this.currentDFStree = new HashMap<String, GeneNode>();

			// Recursive method, which visits all nodes of the current node, 
			// and recursively calls itself with them as parameter.
			// (DFS part)
			visitNeighBors(currentNode.getNodeId());

			// The resulting DFS tree is a connected component of the split path way.
			if(currentDFStree.size() > 0){
				connectedComponents.add(currentDFStree);
			}

			// Get the next unvisited node.
			currentNode = getNextUnvisitedNode();
		}



		return connectedComponents;
	}

	*//**
	 * Gets the next node from the KeyPathWay that has yet to be visited
	 * @param nodeVisited
	 * @param currentPathWay
	 * @return
	 *//*
	private GeneNode getNextUnvisitedNode(){
		GeneNode nextNode = null;
		
		// Go through all nodes, see if they have been visited
		for(String nodeID: this.nodeVisited.keySet()){

			// if we find one which has not been visited
			if(!this.nodeVisited.get(nodeID)){
				return this.currentPathWay.get(nodeID);
			}
		}

		return nextNode;
	}

	*//**
	 * Recursively visit all neighbors of the node with ID nodeID. Does recursive DFS for single source node.
	 * @param nodeID
	 * @param currentDFStree
	 * @param nodeVisited
	 * @param currentPathWay
	 *//*
	private void visitNeighBors(String nodeID){
		
		// Remember to set own node as visited
		this.nodeVisited.put(nodeID, true);
		
		// for each edge, visit the neighbor of the node with the given ID.
		for(String[] edge : currentEdgeList){
			String neighborID = null;
			
			// Only look at edges with two components
			if(edge.length != 2){
				continue;
			}
			
			
			if(nodeID.equals(edge[0])){
				neighborID = edge[1];

			}else if(nodeID.equals(edge[1])){
				neighborID = edge[0];
			}

			// If the node equals the first part, visit the other, if it has not already been visited
			if(neighborID != null && (!nodeVisited.containsKey(neighborID) || !nodeVisited.get(neighborID))){
				// Make sure we mark the node as visited.
				nodeVisited.put(neighborID, true);

				// Add the new node to the DFS tree.
				currentDFStree.put(neighborID, currentPathWay.get(neighborID));

				// Recursively call the 
				visitNeighBors(neighborID);
			}
		}
	}

	*//**
	 * Returns whether the connected component consists of all error nodes.
	 * @param connectedComponent
	 * @return
	 *//*
	private boolean allErrorNodes(HashMap<String, GeneNode> connectedComponent){
		if(connectedComponent == null){
			return false;
		}
		
		if(connectedComponent.size() == 0){
			return false;
		}
		
		System.out.println("Connected components length = " + connectedComponent.size());
		for(GeneNode node : connectedComponent.values()){
			if(node == null){
				// This is a weird special case!
				return false;
			}
			
			if(node.isValid()){
				return false;
			}
		}

		return true;
	}
}*/



/* *
 EN: exception node
non-EN: non-exception node
CC: connected component
K: keypathway
BEN: border exception nodes

put all ENs in a list L
create empty BEN list B

while ENs in L {
   get first EN from L
   test-remove the EN from K
   check if K falls into CCs

   case 1: one CC {
     add EN to B
     remove EN from L
   }

   case 2: two CCs and one of the CCs consists only of ENs {
       add EN to B
       add all nodes from that CC to B
       remove EN from L
       remove all nodes from that CC from L
     }

   case 3: >2 CCs and one of the CCs consists only of ENs {
       add all nodes from that CC to B
       remove all nodes from that CC from L
     }

   else:
     remove EN from L

}

 * */

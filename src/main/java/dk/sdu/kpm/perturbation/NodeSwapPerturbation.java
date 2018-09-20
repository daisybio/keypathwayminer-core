package dk.sdu.kpm.perturbation;

import java.util.HashSet;

import dk.sdu.kpm.KPMSettings;
import dk.sdu.kpm.taskmonitors.IKPMTaskMonitor;
import dk.sdu.kpm.taskmonitors.KPMDummyTaskMonitor;
import dk.sdu.kpm.graph.GeneNode;
import dk.sdu.kpm.graph.KPMGraph;

/**
 * Used for permuting a graph, by swapping nodes, until the parameter percentage permutation degree has been reached. 
 * @author Martin
 *
 */
class NodeSwapPerturbation extends BasePerturbation<KPMGraph>{

    public NodeSwapPerturbation(KPMSettings kpmSettings){
        super(kpmSettings);
    }

	@Override
	public String getDescription() {
		return "Permutation of graph by way of node swap.";
	}

	@Override
	public String getName() {
		return "Node label permutation";
	}


	@Override
	public PerturbationTags getTag() {
		return PerturbationTags.NodeSwap;
	}
	
	/**
	 * Method for permuting
	 * @param percentageToPermute
	 * @param currentGraph
	 * @return
	 */
	@Override
	public KPMGraph execute(int percentageToPermute, KPMGraph currentGraph, IKPMTaskMonitor taskMonitor){
		if(taskMonitor == null){
			taskMonitor = new KPMDummyTaskMonitor();
		}

		taskMonitor.setTitle(String.format("Permuting graph, using '%s'.", this.getName()));
		taskMonitor.setStatusMessage("Permuting...");
		
		KPMGraph graph = new KPMGraph(currentGraph);
		HashSet<GeneNode> swappedNodes = new HashSet<GeneNode>();
		GeneNode[] nodes = graph.getVertices().toArray(new GeneNode[graph.getVertices().size()]);
		
		int nrNodesToShift = (int)Math.ceil(((double)nodes.length / 100) * percentageToPermute);
		int total = (int)Math.ceil(((double)nodes.length / 100) * percentageToPermute);

        // Fill the list with edges indexes. Will be used to draw nodes to be removed.
        this.initIndexRandomizer(nodes.length);


        while(nrNodesToShift > 1){
        	taskMonitor.setProgress((double)((double)1-((double) nrNodesToShift)/((double)total)));
        	
        	GeneNode node1 = nodes[getNextRandomIndex()];
        	GeneNode node2 = nodes[getNextRandomIndex()];
        	
        	// If it's the same ID, or either nodes have been swapped already, then we don't want to swap.
        	if(node1.nodeId.equals(node2.nodeId) || swappedNodes.contains(node1) || swappedNodes.contains(node2)){
        		continue;
        	}
        	
        	// Shifting all edges between them, assuming we're iterating by reference
            for (String[] edge : graph.getEdgeList()) {            	
            	
                if (edge[0].equals(node1.nodeId)) {
                    edge[0] = node2.nodeId;
                }
                
                if (edge[1].equals(node1.nodeId)) {
                    edge[1] = node2.nodeId;
                }
                
                if (edge[0].equals(node2.nodeId)) {
                    edge[0] = node1.nodeId;
                }
                
                if (edge[1].equals(node2.nodeId)) {
                    edge[1] = node1.nodeId;
                }
            }            
        	
            swappedNodes.add(node1);
            swappedNodes.add(node2);
            
        	nrNodesToShift -= 2;
        }
		
		return graph;
	}

}

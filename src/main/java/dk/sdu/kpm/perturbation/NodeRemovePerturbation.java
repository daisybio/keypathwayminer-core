package dk.sdu.kpm.perturbation;

import java.util.HashSet;

import dk.sdu.kpm.taskmonitors.IKPMTaskMonitor;
import dk.sdu.kpm.taskmonitors.KPMDummyTaskMonitor;
import dk.sdu.kpm.graph.GeneNode;
import dk.sdu.kpm.graph.KPMGraph;

class NodeRemovePerturbation extends BasePerturbation<KPMGraph>{

    public NodeRemovePerturbation(){
        super();
    }

	@Override
	public String getDescription() {
		return "Permutation of graph by way of edge removal.";
	}

	@Override
	public String getName() {
		return "Node-removal";
	}
	
	@Override
	public PerturbationTags getTag() {
		return PerturbationTags.NodeRemoval;
	}

	@Override
	public KPMGraph execute(int percentageToPermute, KPMGraph currentGraph, IKPMTaskMonitor taskMonitor){
		if(taskMonitor == null){
			taskMonitor = new KPMDummyTaskMonitor();
		}

		taskMonitor.setTitle(String.format("Permuting graph, using '%s'.", this.getName()));
		taskMonitor.setStatusMessage("Permuting...");
		
		KPMGraph graph = new KPMGraph(currentGraph);
		HashSet<GeneNode> removedNodes = new HashSet<GeneNode>();
		GeneNode[] nodes = graph.getVertices().toArray(new GeneNode[graph.getVertices().size()]);
		
		int nrNodesToRemove = (int)Math.ceil(((double)nodes.length / 100) * percentageToPermute);
		
		// We want at least one node left
		if(nrNodesToRemove >= nodes.length){
			nrNodesToRemove = nodes.length - 1;
		}

        // Fill the list with edges indexes. Will be used to draw nodes to be removed.
        this.initIndexRandomizer(nodes.length);

        
        while(nrNodesToRemove > 0){
        	taskMonitor.setProgress((double)((double)1-((double) nrNodesToRemove)/((double)(nodes.length - 1))));
        	GeneNode node = nodes[getNextRandomIndex()];
        	
        	// If it's the same ID, or either nodes have been swapped already, then we don't want to swap.
        	if(removedNodes.contains(node)){
        		continue;
        	}

        	graph.removeGeneNode(node);
            removedNodes.add(node);
            
        	nrNodesToRemove -= 1;
        }
		
		return graph;
	}

}

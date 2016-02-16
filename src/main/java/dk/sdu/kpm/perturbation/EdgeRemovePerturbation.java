package dk.sdu.kpm.perturbation;

import java.util.HashSet;

import dk.sdu.kpm.taskmonitors.IKPMTaskMonitor;
import dk.sdu.kpm.taskmonitors.KPMDummyTaskMonitor;
import dk.sdu.kpm.graph.GeneEdge;
import dk.sdu.kpm.graph.KPMGraph;

/**
 * Used for perturbing a graph, by removing edges, until the parameter percentage perturbation degree has been reached. 
 * @author Martin
 *
 */
class EdgeRemovePerturbation extends BasePerturbation<KPMGraph>{
    public EdgeRemovePerturbation(){
        super();
    }


	@Override
	public String getDescription() {
		return "Permutation of graph by way of edge removal.";
	}

	@Override
	public String getName() {
		return "Edge-removal";
	}
	
	@Override
	public PerturbationTags getTag() {
		return PerturbationTags.EdgeRemoval;
	}

	@Override
	public KPMGraph execute(int percentageToPermute, KPMGraph currentGraph, IKPMTaskMonitor taskMonitor){
		if(taskMonitor == null){
			taskMonitor = new KPMDummyTaskMonitor();
		}
		taskMonitor.setTitle(String.format("Permuting graph, using '%s'.", this.getName()));
		taskMonitor.setStatusMessage("Permuting...");
		
		KPMGraph graph = new KPMGraph(currentGraph);
		HashSet<GeneEdge> removedEdges = new HashSet<GeneEdge>();
		GeneEdge[] edges = graph.getEdges().toArray(new GeneEdge[graph.getEdges().size()]);

		int nrEdgesToRemove = (int)Math.ceil(((double)edges.length / 100) * percentageToPermute);
		
		// We want at least one edge left
		if(nrEdgesToRemove >= edges.length){
			nrEdgesToRemove = edges.length - 1;
		}

        // Fill the list with edges indexes. Will be used to draw edges to be removed.
        this.initIndexRandomizer(edges.length);

        while(nrEdgesToRemove > 0){
        	taskMonitor.setProgress((double)((double)1-((double) nrEdgesToRemove)/((double)(edges.length - 1))));

        	GeneEdge edge = edges[getNextRandomIndex()];
        	
        	// If it's the same ID, or either nodes have been swapped already, then we don't want to swap.
        	if(removedEdges.contains(edge)){
        		continue;
        	}
        	
        	graph.removeEdge(edge);
            removedEdges.add(edge);
            
        	nrEdgesToRemove -= 1;
        }
	
		return graph;
	}

	@Override
    public String toString() {
        return this.getName();
    }
}

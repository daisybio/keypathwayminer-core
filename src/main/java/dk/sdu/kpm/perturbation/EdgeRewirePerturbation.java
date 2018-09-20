package dk.sdu.kpm.perturbation;

import java.util.HashSet;
import java.util.LinkedList;

import dk.sdu.kpm.KPMSettings;
import dk.sdu.kpm.taskmonitors.IKPMTaskMonitor;
import dk.sdu.kpm.taskmonitors.KPMDummyTaskMonitor;
import dk.sdu.kpm.graph.KPMGraph;
import java.util.Calendar;
import java.util.Random;

class EdgeRewirePerturbation extends BasePerturbation<KPMGraph>{

    public EdgeRewirePerturbation(KPMSettings kpmSettings){
        super(kpmSettings);
    }

	private HashSet<String> edgeAsNodeIDs;
	
	@Override
	public String getDescription() {
		return "Permutation of graph by way of edge rewiring.";
	}

	@Override
	public String getName() {
		return "Degree preserving rewiring";
	}

	@Override
	public KPMGraph execute(int percentageToPermute, KPMGraph currentGraph, IKPMTaskMonitor taskMonitor){
		KPMGraph graph = new KPMGraph(currentGraph);
		if(taskMonitor == null){
			taskMonitor = new KPMDummyTaskMonitor();
		}
		taskMonitor.setTitle(String.format("Permuting graph, using '%s'.", this.getName()));
		taskMonitor.setStatusMessage("Permuting...");
		
		LinkedList<String[]> edges = graph.getEdgeList();

		// For quick lookup to check if an edge already exists
		this.edgeAsNodeIDs = new HashSet<String>();
		for(String[] edge : edges){
			addWiring(edge);
		}

        // Fill the list with edges indexes. Will be used to draw edges to be rewired.
        this.initIndexRandomizer(edges.size());



        int nrEdgesToRewire = (int)Math.ceil(((double)edges.size() / 100) * percentageToPermute);
		int total = (int)Math.ceil(((double)edges.size() / 100) * percentageToPermute);
                Random random = this.kpmSettings.R;
                // seed set by general settings.
                //random.setSeed(Calendar.getInstance().getTimeInMillis());
        // Starting the algorithm
		while(nrEdgesToRewire > 1){
			taskMonitor.setProgress((double)((double)1-((double) nrEdgesToRewire)/((double)total)));
			
			// Draw to random 
			String[] v = edges.get(random.nextInt(edges.size())); //edges.get(getNextRandomIndex());
			String[] u = edges.get(random.nextInt(edges.size()));
			// If edges share at least one vertex in common we do nothing and jump to the next iteration
			// IF (v1 == u1) OR (v1 == u2) OR (v2 == u1) OR (v2 == u2):
			if(v[0].equals(u[0]) || v[0].equals(u[1]) || v[1].equals(u[0]) || v[1].equals(u[1])){
				continue;
			}
			
		    // If at least one of the the rewired edges already exists we do nothing and jump to the next iteration
			// IF (v1, u2) in E OR (v2, u1) in E:
			if(wiringExists(v[0], u[1]) || wiringExists(v[1], u[0])){
				continue;
			}
			
			// Otherwise we swap the endpoints of both edges
			// First remove old wirings:
			removeWiring(v);
			removeWiring(u);
			graph.getEdgeList().remove(v);
			graph.getEdgeList().remove(u);
			
			// Add new (v1, u2) 
			String[] vu = new String[2];
			vu[0] = v[0];
			vu[1] = u[1];
			graph.getEdgeList().add(vu);
			
			// Add new (v2, u1)
			String[] uv = new String[2];
			uv[0] = v[1];
			uv[1] = u[0];
			graph.getEdgeList().add(uv);
			
			nrEdgesToRewire -= 2;
		}
		
		return graph;
	}

	@Override
	public PerturbationTags getTag() {
		return PerturbationTags.EdgeRewire;
	}

	private boolean wiringExists(String v, String u){
		
		if(edgeAsNodeIDs.contains(String.format("%s|%s", v, u))){
			return true;
		}
		
		if(edgeAsNodeIDs.contains(String.format("%s|%s", u, v))){
			return true;
		}
		
		return false;
	}
	
	private void addWiring(String[] edge){
		edgeAsNodeIDs.add(String.format("%s|%s", edge[0], edge[1]));
		
		// For now, assume undirected
		edgeAsNodeIDs.add(String.format("%s|%s", edge[1], edge[0]));
	}
	
	private void removeWiring(String[] edge){
		
		String vu = String.format("%s|%s", edge[0], edge[1]);
		if(edgeAsNodeIDs.contains(vu)){
			edgeAsNodeIDs.remove(vu);
		}
		
		String uv = String.format("%s|%s", edge[1], edge[0]);
		if(edgeAsNodeIDs.contains(uv)){
			edgeAsNodeIDs.remove(uv);
		}
	}
	
}

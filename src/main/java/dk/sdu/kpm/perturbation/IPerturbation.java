package dk.sdu.kpm.perturbation;

import dk.sdu.kpm.taskmonitors.IKPMTaskMonitor;
import dk.sdu.kpm.graph.KPMGraph;

/**
 * Interface for accessing permutations. Also allow for async methods.
 * @author Martin
 *
 */
public interface IPerturbation<T> {
	
	public String getDescription();
	
	public String getName();
	
	public T execute(int percentageToPermute, T input, IKPMTaskMonitor taskMonitor);

	public PerturbationTags getTag();
	
	public enum PerturbationTags{
		// Graph perturbations:
        NodeSwap, EdgeRemoval, NodeRemoval, EdgeRewire, DegreeAwareNodeSwap,

        // Dataset perturbations:
        ColumnWiseShuffle
	}
	
	@Override
    public String toString();
}

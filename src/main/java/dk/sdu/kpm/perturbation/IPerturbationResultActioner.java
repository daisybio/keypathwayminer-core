package dk.sdu.kpm.perturbation;

import dk.sdu.kpm.graph.KPMGraph;

/**
 * Interface for callbacks of async perturbations.  
 * @author Martin
 *
 */
public interface IPerturbationResultActioner {
	public void Callbackmethod(KPMGraph graph, int percentagePermuted);
}

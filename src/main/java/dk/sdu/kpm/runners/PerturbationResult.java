package dk.sdu.kpm.runners;

import java.io.Serializable;
import java.util.List;

import dk.sdu.kpm.results.IKPMResultItem;
import dk.sdu.kpm.validation.ValidationOverlapResult;

public class PerturbationResult implements Serializable {
	public int PercentagePerturbed;
	public int GraphNr;
	public List<BatchResult> Results;
	public List<ValidationOverlapResult> ValidationOverlapResults;
	
	public PerturbationResult(int percentagePerturbed, int graphNr, List<BatchResult> results){
		this.PercentagePerturbed = percentagePerturbed;
		this.GraphNr = graphNr;
		this.Results = results;
		this.ValidationOverlapResults = null;
	}

	public PerturbationResult(int percentagePerturbed, int graphNr, List<BatchResult> results, List<ValidationOverlapResult> validationOverlapResults){
		this(percentagePerturbed, graphNr, results);
		this.ValidationOverlapResults = validationOverlapResults;
	}
}

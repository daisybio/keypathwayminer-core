package dk.sdu.kpm.perturbation;

import java.util.ArrayList;
import java.util.List;

import dk.sdu.kpm.KPMSettings;
import dk.sdu.kpm.perturbation.IPerturbation.PerturbationTags;

public class PerturbationService {
	public static IPerturbation getPerturbation(PerturbationTags tag, KPMSettings kpmSettings){
		switch (tag) {
		case NodeSwap:
			return new NodeSwapPerturbation(kpmSettings);
		case EdgeRemoval:
			return new EdgeRemovePerturbation(kpmSettings);
		case NodeRemoval:
			return new NodeRemovePerturbation(kpmSettings);
		case EdgeRewire:
			return new EdgeRewirePerturbation(kpmSettings);
		case DegreeAwareNodeSwap:
			return new DegreeAwareNodeSwap(kpmSettings);
		default:
			break;
		}
		
		// Default is just NodeSwap
		return new NodeSwapPerturbation(kpmSettings);
	}
	
	public static List<PerturbationTags> getPerturbationTags(){
		List<PerturbationTags> returnList = new ArrayList<PerturbationTags>();

		returnList.add(PerturbationTags.NodeSwap);
		returnList.add(PerturbationTags.NodeRemoval);
		returnList.add(PerturbationTags.EdgeRemoval);
		returnList.add(PerturbationTags.EdgeRewire);
		returnList.add(PerturbationTags.DegreeAwareNodeSwap);
		
		return returnList;
	}
}

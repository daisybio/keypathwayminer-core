package dk.sdu.kpm.perturbation;

import java.util.ArrayList;
import java.util.List;

import dk.sdu.kpm.perturbation.IPerturbation.PerturbationTags;

public class PerturbationService {
	public static IPerturbation getPerturbation(PerturbationTags tag){
		switch (tag) {
		case NodeSwap:
			return new NodeSwapPerturbation();
		case EdgeRemoval:
			return new EdgeRemovePerturbation();
		case NodeRemoval:
			return new NodeRemovePerturbation();
		case EdgeRewire:
			return new EdgeRewirePerturbation();
		default:
			break;
		}
		
		// Default is just NodeSwap
		return new NodeSwapPerturbation();
	}
	
	public static List<PerturbationTags> getPerturbationTags(){
		List<PerturbationTags> returnList = new ArrayList<PerturbationTags>();

		returnList.add(PerturbationTags.NodeSwap);
		returnList.add(PerturbationTags.NodeRemoval);
		returnList.add(PerturbationTags.EdgeRemoval);
		returnList.add(PerturbationTags.EdgeRewire);
		
		return returnList;
	}
}

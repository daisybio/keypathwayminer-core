package dk.sdu.kpm.validation;

import dk.sdu.kpm.graph.GeneNode;

import java.io.Serializable;

public class ValidationOverlapResultItem implements Serializable {
	public String geneNodeID;
	public GeneNode geneNode;
	
	public ValidationOverlapResultItem(String geneNodeId, GeneNode geneNode){
		this.geneNodeID = geneNodeId;
		this.geneNode = geneNode;
	}
	
}

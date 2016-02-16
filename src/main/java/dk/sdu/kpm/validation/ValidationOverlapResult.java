package dk.sdu.kpm.validation;

import java.io.Serializable;
import java.util.List;

public class ValidationOverlapResult implements Serializable{

	/**
	 * List of result items, showing the overlap.
	 */
	public List<ValidationOverlapResultItem> overlapResultItems;
	
	/**
	 * The K
	 */
	public int K;
	
	/**
	 * The L
	 */
	public int L; 
	
	public double percentageOverlap; 
	
	public ValidationOverlapResult(int k, int l, List<ValidationOverlapResultItem> overlapResultItems, double percentageOverlap){
		this.overlapResultItems = overlapResultItems;
		this.K = k;
		this.L = l;
		this.percentageOverlap = percentageOverlap;
	}
}

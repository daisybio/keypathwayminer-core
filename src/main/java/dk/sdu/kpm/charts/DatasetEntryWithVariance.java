package dk.sdu.kpm.charts;

import java.io.Serializable;

public class DatasetEntryWithVariance extends DatasetEntry implements Serializable {
	private double lowY;
	private double highY;
	
	public DatasetEntryWithVariance(double x, double y, double lowY, double highY){
		super(x, y);
		this.lowY = lowY;
		this.highY = highY;
	}
	
	public double getLowY(){
		return this.lowY;
	}
	
	public double getHighY(){
		return this.highY;
	}
}

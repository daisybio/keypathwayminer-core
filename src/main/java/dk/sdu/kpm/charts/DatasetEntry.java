package dk.sdu.kpm.charts;

import java.io.Serializable;

/**
 * Wrapper class for a dataset entry, defining a point in the xy-space.
 * @author Martin
 *
 */
public class DatasetEntry  implements Serializable {
	private double x;
	private double y;
	
	public DatasetEntry(){
		x = 0;
		y = 0;
	}
	
	public DatasetEntry(double x, double y){
		this.x = x;
		this.y = y;
	}
	
	public double getX(){
		return this.x;
	}
	
	public double getY(){
		return this.y;
	}
}

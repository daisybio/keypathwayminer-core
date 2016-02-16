package dk.sdu.kpm.charts;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * Wrapper class, defining a dataset for the charts
 * @author Martin
 *
 */
public class Dataset implements Serializable {
	private String title;
	
	private ArrayList<DatasetEntry> data;
	
	public Dataset(String title){
		this.title = title;
		data = new ArrayList<DatasetEntry>();
	}
	
	public void add(DatasetEntry entry){
		data.add(entry);
	}
	
	public void remove(DatasetEntry entry){
		if(data.contains(entry)){
			data.remove(entry);
		}
	}
	
	public String getTitle(){
		return this.title;
	}
	
	public Iterator<DatasetEntry> getDataIterator(){
		return this.data.iterator();
	}
}

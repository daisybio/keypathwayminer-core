package dk.sdu.kpm.results;

public interface IKPMRunListener {

	/**
	 * Last thing being called by the worker. Returning the resultset, if any. 
	 */
	void runFinished(IKPMResultSet results);
	
	void runCancelled(String reason, String runID);
}

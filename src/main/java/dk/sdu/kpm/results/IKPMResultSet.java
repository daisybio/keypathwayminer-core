package dk.sdu.kpm.results;

import java.util.List;
import java.util.Map;

import dk.sdu.kpm.KPMSettings;
import dk.sdu.kpm.charts.IChart;
import dk.sdu.kpm.validation.ValidationOverlapResult;

public interface IKPMResultSet {

	/**
	 * Return the unique identifier for the specific KPM run for these results.
	 * @return
	 */
	String getKpmID();

    /**
     * Return the KPM run settings for this specific run.
     * @return
     */
    KPMSettings getKpmSettings();

    void setKpmSettings(KPMSettings settings);
	
	/**
	 * Returns the list of results from a run which has not been perturbed.
	 * @return
	 */
	List<IKPMResultItem> getResults();


	/**
	 * Return a map containing the title of the chart as key, and the chart itself as value.
	 * This is currently the only statistics we need for the perturbed parts. 
	 * @return
	 */
	Map<String, IChart> getCharts();

	/**
	 * Return a list containing the name of the validation file as first parameter, and the overlap results as the value
	 * @return
	 */
    List<ValidationOverlapResult> getOverlapResults();
}

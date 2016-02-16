package dk.sdu.kpm.statistics;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import dk.sdu.kpm.charts.IChart;
import dk.sdu.kpm.runners.PerturbationResult;
import dk.sdu.kpm.runners.BatchResult;

public interface IStatistics {
	/**
	 * Return the unique identifier for the specific KPM run for these statistics.
	 * @return
	 */
	String getKpmID();

	/**
	 * Return a thread-safe hashmap containing the title of the chart as key, and the chart itself as value.
	 * @return
	 */
    Map<String, IChart> getCharts();

	/**
	 * Returns the percentage overlap.
	 * x-axis = permutation %, y-axis = overlap between results from standard run and perturbed graphs.
	 * @return
	 */
    Map<Integer, List<Double>> getPercentageOverlap();
	
	/**
	 * For counting overlaps, fixed permutation percentage, varying K and L.
	 * <Percent, <L, overlap>
	 * x-axis = L, fixed permutation percentage, y-axis = same as above
	 * @return
	 */
    Map<Integer, Map<Pair<Integer, Integer>, List<Double>>> getFixedPercentOverlap();
	 
	 /**
	  * Returns the overlap between the result and the gold standard.
	  * <Percent Perturbed, Overlap Percentages>
	  * @return
	  */
	// For counting overlap with gold standards:
	//  Percent    Overlap
     Map<Integer, List<Double>> getGoldOverlap();
			
	/**
	 * Returns the overlap for a given perturbation percent.
	 * <Percent Perturbed, <L, Overlap count>>
	 * @return
	 */
    Map<Integer, Map<Pair<Integer, Integer>, List<Double>>>  getFixedPercentGoldOverlap();
}

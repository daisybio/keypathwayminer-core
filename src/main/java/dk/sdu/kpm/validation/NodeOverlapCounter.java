package dk.sdu.kpm.validation;

import dk.sdu.kpm.KPMSettings;
import dk.sdu.kpm.RunStats;
import dk.sdu.kpm.graph.GeneNode;
import dk.sdu.kpm.graph.Result;
import dk.sdu.kpm.results.PercentageParameters;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Count the overlap
 *
 * @author Martin
 *
 */
public class NodeOverlapCounter implements Serializable {

    public static List<ValidationOverlapResult> compareResultsToGoldStandard(KPMSettings kpmSettings) {
        List<ValidationOverlapResult> results = new ArrayList<ValidationOverlapResult>();

        if (kpmSettings.VALIDATION_GOLDSTANDARD_NODES == null
                || kpmSettings.VALIDATION_GOLDSTANDARD_NODES.size() == 0) {
            return results;
        }

        if (!(kpmSettings.STATS_MAP == null || kpmSettings.STATS_MAP.isEmpty())) {
            for (List<Integer> params : kpmSettings.STATS_MAP.keySet()) {
                RunStats rs = kpmSettings.STATS_MAP.get(params);
                int k = params.get(0);
                List<String> identifiers = kpmSettings.externalToInternalIDManager.getInternalIdentifiers();

                int l1, l2;
                l1 = params.get(1);
                l2 = 0;
                if (kpmSettings.VARYING_L_ID.size() == 1) {
                    l1 = params.get(identifiers.indexOf(kpmSettings.VARYING_L_ID.get(0)) + 1);
                } else if (kpmSettings.VARYING_L_ID.size() == 2) {
                    l1 = params.get(identifiers.indexOf(kpmSettings.VARYING_L_ID.get(1)) + 1);
                    l2 = params.get(identifiers.indexOf(kpmSettings.VARYING_L_ID.get(0)) + 1);
                }

                // Only get the first result. Later, maybe the others will also be counted as an average
                Result firstResult = rs.getResults().get(0);

                // The list of visited nodes
                Map<String, GeneNode> visitedNodes = firstResult.getVisitedNodes();
                List<ValidationOverlapResultItem> overlaps = countOverlap(kpmSettings.VALIDATION_GOLDSTANDARD_NODES, visitedNodes);

                // Add the percentage of overlapping genes.
                // Validation against gold standard set:  Percentage of overlapping genes =  | A intersection G | / | A |
                double percentageOverlap = 0;
                double intersectionSize = (double) overlaps.size();
                double visitedNodesSize = (double) visitedNodes.size();
                if (visitedNodesSize > 0) {
                    percentageOverlap = intersectionSize / visitedNodesSize;
                }

                // Adding the validation overlap results.
                //ValidationOverlapResult result = null;
                if (kpmSettings.VARYING_L_ID.size() == 2) {

                    ValidationOverlapResult result = new ValidationOverlapResult(l1, l2, overlaps, percentageOverlap);
                    results.add(result);
                    //return (results);
                } else {

                    ValidationOverlapResult result = new ValidationOverlapResult(k, l1, overlaps, percentageOverlap);

                    results.add(result);
                }
            }

        } else if (!(kpmSettings.STATS_MAP_PER == null || kpmSettings.STATS_MAP_PER.isEmpty())) {
            for (PercentageParameters params : kpmSettings.STATS_MAP_PER.keySet()) {
                RunStats rs = kpmSettings.STATS_MAP_PER.get(params);

                int k = params.getK();
                List<String> identifiers = kpmSettings.externalToInternalIDManager.getInternalIdentifiers();

                int l = (int) params.getlPer();

                // Only get the first result. Later, maybe the others will also be counted as an average
                Result firstResult = rs.getResults().get(0);

                // The list of visited nodes
                Map<String, GeneNode> visitedNodes = firstResult.getVisitedNodes();
                List<ValidationOverlapResultItem> overlaps = countOverlap(kpmSettings.VALIDATION_GOLDSTANDARD_NODES, visitedNodes);

                // Add the percentage of overlapping genes.
                // Validation against gold standard set:  Percentage of overlapping genes =  | A intersection G | / | A |
                double percentageOverlap = 0;
                double intersectionSize = (double) overlaps.size();
                double visitedNodesSize = (double) visitedNodes.size();
                if (visitedNodesSize > 0) {
                    percentageOverlap = intersectionSize / visitedNodesSize;
                }

            // Adding the validation overlap results.
                //ValidationOverlapResult result = null;
                ValidationOverlapResult result = new ValidationOverlapResult(k, l, overlaps, percentageOverlap);

                results.add(result);
            }

        }

        return results;
    }

    /**
     * *
     * Compares the overlap between a list of gold standard nodes, and a map of
     * the result (visited nodes).
     *
     * @param goldStandardNodes
     * @param resultNodes
     * @return
     */
    public static List<ValidationOverlapResultItem> countOverlap(List<String> goldStandardNodes, Map<String, GeneNode> resultNodes) {
        List<ValidationOverlapResultItem> overlaps = new ArrayList<ValidationOverlapResultItem>();

        for (String goldStandardNode : goldStandardNodes) {
            if (resultNodes.containsKey(goldStandardNode)) {
                overlaps.add(new ValidationOverlapResultItem(goldStandardNode, resultNodes.get(goldStandardNode)));
            }
        }

        return overlaps;
    }
}

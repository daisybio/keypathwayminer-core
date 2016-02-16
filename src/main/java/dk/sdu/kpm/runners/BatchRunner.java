package dk.sdu.kpm.runners;

import dk.sdu.kpm.AlgoComputations;
import dk.sdu.kpm.KPMSettings;
import dk.sdu.kpm.RunStats;
import dk.sdu.kpm.charts.ChartInput;
import dk.sdu.kpm.charts.IChart;
import dk.sdu.kpm.charts.StandardCharts;
import dk.sdu.kpm.graph.GeneNode;
import dk.sdu.kpm.graph.Result;
import dk.sdu.kpm.logging.KpmLogger;
import dk.sdu.kpm.results.IKPMResultItem;
import dk.sdu.kpm.results.IKPMRunListener;
import dk.sdu.kpm.results.PercentageParameters;
import dk.sdu.kpm.results.StandardResultSet;
import dk.sdu.kpm.taskmonitors.IKPMTaskMonitor;
import dk.sdu.kpm.validation.NodeOverlapCounter;
import dk.sdu.kpm.validation.ValidationOverlapResult;

import java.text.DecimalFormat;
import java.util.*;
import java.util.logging.Level;

public class BatchRunner implements Runnable {

    private String runId;
    private IKPMTaskMonitor taskMonitor;
    private IKPMRunListener listener;
    private int totalPathways;
    private List<Double> percentages;
    private volatile boolean cancelled;
    private volatile boolean copyKPMSettings;

    private volatile KPMSettings kpmSettings;

    public BatchRunner(String runId, IKPMTaskMonitor taskMonitor,
            IKPMRunListener listener, KPMSettings settings) {
        this.runId = runId;
        this.taskMonitor = taskMonitor;
        this.listener = listener;
        this.totalPathways = 0;
        this.cancelled = false;
        this.kpmSettings = settings;
        this.copyKPMSettings = false;
        taskMonitor.setTitle("Key Pathway Miner");
    }

    public void setCopyKPMSettings(boolean copy) {
        this.copyKPMSettings = copy;
    }

    synchronized private boolean isCancelled() {
        return this.cancelled;
    }

    private void runSingle() {
        kpmSettings.INDEX_L_MAP = new HashMap<Integer, String>();
        if (kpmSettings.STATS_MAP == null) {
            kpmSettings.STATS_MAP = new HashMap<List<Integer>, RunStats>();
        }
        if (kpmSettings.STATS_MAP_PER == null) {
            kpmSettings.STATS_MAP_PER = new HashMap<PercentageParameters, RunStats>();
        }

        List<Integer> params = new ArrayList<Integer>(kpmSettings.CASE_EXCEPTIONS_MAP.size() + 1);
        for (int i = 0; i <= kpmSettings.CASE_EXCEPTIONS_MAP.size(); i++) {
            params.add(0);
        }

        if (kpmSettings.USE_INES) {
            params.set(0, kpmSettings.GENE_EXCEPTIONS);
            kpmSettings.MIN_K = kpmSettings.MAX_K = kpmSettings.GENE_EXCEPTIONS;
        } else {
            params.set(0, 0);
        }

        int l = 0;
        for (String lid : kpmSettings.externalToInternalIDManager.getInternalIdentifiers()) {
            kpmSettings.INDEX_L_MAP.put(l, lid);
            int caseL = kpmSettings.CASE_EXCEPTIONS_MAP.get(lid);
            kpmSettings.MIN_L.put(lid, caseL);
            kpmSettings.MAX_L.put(lid, caseL);
            params.set(l + 1, caseL);
            l++;
        }

        taskMonitor.setStatusMessage("Refreshing graph...");
        kpmSettings.MAIN_GRAPH.refreshGraph(kpmSettings);
        taskMonitor.setStatusMessage("Searching and extracting pathways...");
        long start = System.currentTimeMillis();

        List<Result> results = new AlgoComputations().run(kpmSettings.ALGO, kpmSettings.MAIN_GRAPH, taskMonitor, kpmSettings);

        long end = System.currentTimeMillis();
        kpmSettings.TOTAL_RUNNING_TIME = (end - start) / 1000;

        taskMonitor.setStatusMessage("Computing statistics...");
        taskMonitor.setProgress(0.3);
        if (results == null) {
            return;
        } else if (results.isEmpty()) {
            return;
        }
        if (results.size() > 1) {
            Collections.sort(results);
            taskMonitor.setProgress(0.7);
            if (!kpmSettings.DOUBLE_SOLUTIONS_ALLOWED) {
                results = removeDoubleSolutions(results);
            }

            if (kpmSettings.NUM_SOLUTIONS >= 0) {
                List<Result> filter = new ArrayList<Result>();
                int toAdd = 1;
                for (Result result : results) {
                    if (toAdd > kpmSettings.NUM_SOLUTIONS) {
                        break;
                    } else {
                        filter.add(result);
                        toAdd++;
                    }
                }
                results = filter;
            }
        }
        totalPathways = results.size();
        taskMonitor.setProgress(0.9);
        Map<String, Boolean> exceptionMap = new HashMap<String, Boolean>();
        for (GeneNode geneNode : kpmSettings.MAIN_GRAPH.getVertices()) {
            exceptionMap.put(geneNode.getNodeId(), !geneNode.isValid());
        }

        String message = "Computing statistics for exceptions:\n ";
        taskMonitor.setStatusMessage(message + "Iterating through vertices.");

        if (results != null) {
            if (!results.isEmpty()) {

                taskMonitor.setStatusMessage(message + "Computing run stats.");
                RunStats rs = new RunStats(this.runId, FilterBENs(results), kpmSettings.TOTAL_RUNNING_TIME, exceptionMap, kpmSettings);

                if (kpmSettings.CALCULATE_ONLY_SAME_L_VALUES && params.size() > 0) {
                    Map<String, List<Integer>> lRanges = getRangeLPer();
                    for (String lid: lRanges.keySet()) {
                        kpmSettings.CASE_EXCEPTIONS_MAP.put(lid, lRanges.get(lid).get(0));
                    }
                    
                    PercentageParameters param = new PercentageParameters(params.get(0), percentages.get(0));
                    kpmSettings.STATS_MAP_PER.put(param, rs);
                } else {
                    kpmSettings.STATS_MAP.put(params, rs);
                }

                taskMonitor.setStatusMessage(message + "Finished computing run stats.");
            }
        }

        taskMonitor.setStatusMessage("Finalizing and performing post-processing tasks...");
    }

    private void runIndividualL() {
        List<Integer> kList = new ArrayList<Integer>();
        kpmSettings.INDEX_L_MAP = new HashMap<Integer, String>();
 //       kpmSettings.VARYING_L_ID = new ArrayList<String>(2);
 //       kpmSettings.VARYING_L_ID_IN_PERCENTAGE = new HashMap<String, Boolean>(2);
        if (kpmSettings.USE_INES) {
            if (kpmSettings.MIN_K == kpmSettings.MAX_K) {
                kList.add(kpmSettings.MIN_K);
            } else {
                kList.addAll(range(kpmSettings.MIN_K, kpmSettings.MAX_K, kpmSettings.INC_K));
            }
        } else {
            kList.add(0);
        }

        List<List<Integer>> ranges = new ArrayList<List<Integer>>();
        ranges.add(kList);

        int[] indSize = new int[kpmSettings.MIN_L.size() + 1];
        indSize[0] = kList.size();
        int totalRuns = kList.size();
        int l = 0;
        int maxRunK = 0;
        Map<String, Integer> maxRunL = new HashMap<String, Integer>();
        for (String lid : kpmSettings.externalToInternalIDManager.getInternalIdentifiers()) {
            maxRunL.put(lid, 0);
            int minL = 0;
            try {
                minL = kpmSettings.MIN_L.get(lid);
            } catch (NullPointerException e) {
                KpmLogger.log(Level.SEVERE, e);
            }
            int maxL = kpmSettings.MAX_L.get(lid);
            int incL = kpmSettings.INC_L.get(lid);

            List<Integer> lList = new ArrayList<Integer>();
            if (minL == maxL) {
                lList.add(minL);
            } else {
                lList = range(minL, maxL, incL);
            }
            ranges.add(lList);
            kpmSettings.INDEX_L_MAP.put(l, lid);
            indSize[l + 1] = lList.size();
            l++;
            totalRuns *= lList.size();
        }

        CartesianProduct cp = new BatchRunner.CartesianProduct(indSize);

        int runs = 0;
        int prog = runs;
        int fullBar = 2 * totalRuns;
        kpmSettings.STATS_MAP = new HashMap<List<Integer>, RunStats>();
        cp.hasNext();

        try {
            DecimalFormat runFormat = getFormatedInt(totalRuns);
            String currentGraphTitle = runId;
            while (cp.hasNext() && !isCancelled()) {
                int[] indices = cp.next();
                int entries = indices.length;
                List<Integer> params = new ArrayList<Integer>(entries);
                for (int i = 0; i < entries; i++) {
                    params.add(0);
                }
                kpmSettings.CASE_EXCEPTIONS_MAP.clear();
                params.set(0, ranges.get(0).get(indices[0]));
                int currK = params.get(0);
                kpmSettings.GENE_EXCEPTIONS = currK;
                String message = "Extracting pathways for exceptions:\n";
                String stats = "";
                if (kpmSettings.USE_INES) {
                    stats = "K=" + currK;
                }
                if (currK > maxRunK) {
                    maxRunK = currK;
                }
                for (int i = 1; i < entries; i++) {
                    int indi = indices[i];
                    List<Integer> rList = ranges.get(i);
                    params.set(i, rList.get(indi));
                    String internalId = kpmSettings.INDEX_L_MAP.get(i - 1);
                    int currL = params.get(i);
                    kpmSettings.CASE_EXCEPTIONS_MAP.put(internalId, currL);
                    stats += ", L=" + currL;
                    if (currL > maxRunL.get(internalId)) {
                        maxRunL.put(internalId, currL);
                    }
                }

                if (kpmSettings.GENE_EXCEPTIONS > maxRunK) {
                    maxRunK = kpmSettings.GENE_EXCEPTIONS;
                }

                kpmSettings.MAIN_GRAPH.refreshGraph(kpmSettings);
                taskMonitor.setStatusMessage(message + stats);
                long start = System.currentTimeMillis();

                // An interface mapper is needed, to isolate the library completely from Cytoscape
                // Should not affect performance
                List<Result> results = new AlgoComputations().run(kpmSettings.ALGO, kpmSettings.MAIN_GRAPH, taskMonitor, kpmSettings);
                long end = System.currentTimeMillis();
                String runId = currentGraphTitle + "-R" + runFormat.format(runs + 1);
                runs++;
                prog++;
                double completed = (double) prog / (double) fullBar;
                taskMonitor.setProgress(completed - 0.01);
                message = "Computing statistics for exceptions:\n ";
                taskMonitor.setStatusMessage(message + stats);

                if (results.size() > 1) {
                    Collections.sort(results);
                    if (!kpmSettings.DOUBLE_SOLUTIONS_ALLOWED) {
                        results = removeDoubleSolutions(results);
                    }

                    if (kpmSettings.NUM_SOLUTIONS >= 0) {
                        List<Result> filter = new ArrayList<Result>();
                        int toAdd = 1;
                        for (Result result : results) {
                            if (toAdd > kpmSettings.NUM_SOLUTIONS) {
                                break;
                            } else {
                                filter.add(result);
                                toAdd++;
                            }
                        }
                        results = filter;
                    }
                }
                totalPathways += results.size();

                taskMonitor.setStatusMessage(message + "Iterating through vertices.");
                Map<String, Boolean> exceptionMap = new HashMap<String, Boolean>();

                if (kpmSettings.MAIN_GRAPH != null) {
                    for (GeneNode geneNode : kpmSettings.MAIN_GRAPH.getVertices()) {
                        exceptionMap.put(geneNode.getNodeId(), !geneNode.isValid());
                    }
                }

                if (results != null) {
                    if (!results.isEmpty()) {

                        taskMonitor.setStatusMessage(message + "Computing run stats.");
                        RunStats rs = new RunStats(runId,
                                FilterBENs(results), kpmSettings.TOTAL_RUNNING_TIME, exceptionMap, kpmSettings);
                        kpmSettings.STATS_MAP.put(params, rs);
                        taskMonitor.setStatusMessage(message + "Finished computing run stats.");
                    }
                }

                taskMonitor.setStatusMessage(message + "Finished computing run stats.");
                prog++;

            }
        } catch (Exception e) {
            KpmLogger.log(Level.SEVERE, e);
        }
        if (cancelled) {
            kpmSettings.MAX_K = maxRunK;
            for (String lid : maxRunL.keySet()) {
                kpmSettings.MAX_L.put(lid, maxRunL.get(lid));
            }
        }
        taskMonitor.setStatusMessage("Finalizing and performing post-processing tasks...");
    }

    private void runPackL() {
        List<Integer> kList = new ArrayList<Integer>();
        kpmSettings.INDEX_L_MAP = new HashMap<Integer, String>();
//        kpmSettings.VARYING_L_ID = new ArrayList<String>(2);
//        kpmSettings.VARYING_L_ID_IN_PERCENTAGE = new HashMap<String, Boolean>(2);
        if (kpmSettings.USE_INES) {
            if (kpmSettings.MIN_K == kpmSettings.MAX_K) {
                kList.add(kpmSettings.MIN_K);
            } else {
                kList.addAll(range(kpmSettings.MIN_K, kpmSettings.MAX_K, kpmSettings.INC_K));
            }
        } else {
            kList.add(0);
        }

        List<List<Integer>> ranges = new ArrayList<List<Integer>>();
        ranges.add(kList);
        Map<String, List<Integer>> lRanges = getRangeLPer();
        int lRuns = percentages.size();
        int[] indSize = new int[2];
        indSize[0] = kList.size();
        indSize[1] = lRuns;
        int totalRuns = kList.size() * lRuns;

        int maxRunK = 0;
        Map<String, Integer> maxRunL = new HashMap<String, Integer>();

        int l = 0;
        for (String lid : kpmSettings.externalToInternalIDManager.getInternalIdentifiers()) {
            ranges.add(lRanges.get(lid));
            maxRunL.put(lid, 0);
            kpmSettings.INDEX_L_MAP.put(l, lid);
            l++;
        }

        CartesianProduct cp = new BatchRunner.CartesianProduct(indSize);

        int runs = 0;
        int prog = runs;
        int fullBar = 2 * totalRuns;
        kpmSettings.STATS_MAP_PER = new HashMap<PercentageParameters, RunStats>();
        cp.hasNext();

        try {
            DecimalFormat runFormat = getFormatedInt(totalRuns);
            String currentGraphTitle = runId;

            while (cp.hasNext() && !isCancelled()) {
                int[] indices = cp.next();
                int entries = lRuns + 1;
//                List<Integer> params = new ArrayList<Integer>(entries);
//                for (int i = 0; i < entries; i++) {
//                    params.add(0);
//                }
                kpmSettings.CASE_EXCEPTIONS_MAP.clear();
//                params.set(0, ranges.get(0).get(indices[0]));
                int currK = ranges.get(0).get(indices[0]);
                kpmSettings.GENE_EXCEPTIONS = currK;
                String message = "Extracting pathways for exceptions:\n";
                String stats = "";
                if (kpmSettings.USE_INES) {
                    stats = "K=" + currK;
                }
                if (currK > maxRunK) {
                    maxRunK = currK;
                }
                int indi = indices[1];

                PercentageParameters perParam
                        = new PercentageParameters(currK, percentages.get(indi));
                for (int i = 1; i < ranges.size(); i++) {

                    List<Integer> rList = ranges.get(i);
                    //params.set(i, rList.get(indi));
                    String internalId = kpmSettings.INDEX_L_MAP.get(i - 1);
                    int currL = rList.get(indi);
                    kpmSettings.CASE_EXCEPTIONS_MAP.put(internalId, currL);
                    stats += ", L=" + currL;
                    if (currL > maxRunL.get(internalId)) {
                        maxRunL.put(internalId, currL);
                    }
                }

//                kpmSettings.VARYING_L_ID = new ArrayList<String>(2);
//                kpmSettings.VARYING_L_ID_IN_PERCENTAGE = new HashMap<String, Boolean>(2);
                if (kpmSettings.GENE_EXCEPTIONS > maxRunK) {
                    maxRunK = kpmSettings.GENE_EXCEPTIONS;
                }

                kpmSettings.MAIN_GRAPH.refreshGraph(kpmSettings);
                taskMonitor.setStatusMessage(message + stats);
                long start = System.currentTimeMillis();

                // An interface mapper is needed, to isolate the library completely from Cytoscape
                // Should not affect performance
                List<Result> results = new AlgoComputations().run(kpmSettings.ALGO, kpmSettings.MAIN_GRAPH, taskMonitor, kpmSettings);
                long end = System.currentTimeMillis();
                String runId = currentGraphTitle + "-R" + runFormat.format(runs + 1);
                runs++;
                prog++;
                double completed = (double) prog / (double) fullBar;
                taskMonitor.setProgress(completed - 0.01);
                message = "Computing statistics for exceptions:\n ";
                taskMonitor.setStatusMessage(message + stats);

                if (results.size() > 1) {
                    Collections.sort(results);
                    if (!kpmSettings.DOUBLE_SOLUTIONS_ALLOWED) {
                        results = removeDoubleSolutions(results);
                    }

                    if (kpmSettings.NUM_SOLUTIONS >= 0) {
                        List<Result> filter = new ArrayList<Result>();
                        int toAdd = 1;
                        for (Result result : results) {
                            if (toAdd > kpmSettings.NUM_SOLUTIONS) {
                                break;
                            } else {
                                filter.add(result);
                                toAdd++;
                            }
                        }
                        results = filter;
                    }
                }
                totalPathways += results.size();

                taskMonitor.setStatusMessage(message + "Iterating through vertices.");
                Map<String, Boolean> exceptionMap = new HashMap<String, Boolean>();

                if (kpmSettings.MAIN_GRAPH != null) {
                    for (GeneNode geneNode : kpmSettings.MAIN_GRAPH.getVertices()) {
                        exceptionMap.put(geneNode.getNodeId(), !geneNode.isValid());
                    }
                }

                if (results != null) {
                    if (!results.isEmpty()) {

                        taskMonitor.setStatusMessage(message + "Computing run stats.");
                        RunStats rs = new RunStats(runId,
                                FilterBENs(results), kpmSettings.TOTAL_RUNNING_TIME, exceptionMap, kpmSettings);
                        //kpmSettings.STATS_MAP.put(params, rs);
                        kpmSettings.STATS_MAP_PER.put(perParam, rs);
                        taskMonitor.setStatusMessage(message + "Finished computing run stats.");
                    }
                }

                taskMonitor.setStatusMessage(message + "Finished computing run stats.");
                prog++;

            }
        } catch (Exception e) {
            KpmLogger.log(Level.SEVERE, e);
        }
        if (cancelled) {
            kpmSettings.MAX_K = maxRunK;
            for (String lid : maxRunL.keySet()) {
                kpmSettings.MAX_L.put(lid, maxRunL.get(lid));
            }
        }
        taskMonitor.setStatusMessage("Finalizing and performing post-processing tasks...");
    }

    private void runBatch() {
        if (kpmSettings.CALCULATE_ONLY_SAME_L_VALUES) {
            runPackL();
        } else {
            runIndividualL();
        }
    }

    /**
     * For filtering BENs
     *
     * @param results
     * @return The list of results, filtered for BENs.
     */
    private List<Result> FilterBENs(List<Result> results) {

        // Only filter if it set to true.
        if (kpmSettings.REMOVE_BENs) {
            BENRemover benKiller = new BENRemover(kpmSettings.MAIN_GRAPH);
            List<Result> resultsFilteredForBens = new ArrayList<Result>();

            int size = results.size();
            for (int i = 0; i < size; i++) {
                taskMonitor.setStatusMessage(String.format("Filtering BENs for result %d/%d", (i + 1), size));
                resultsFilteredForBens.add(benKiller.filterBENs(results.get(i)));
            }

            return resultsFilteredForBens;

        } else {
            return results;
        }
    }

    private Map<String, List<Integer>> getRangeLPer() {
        // kpmSettings.PER_TO_L_VALS_MAP = new HashMap<Double, List<Integer>>();
        percentages = new ArrayList<Double>();

        double increment = kpmSettings.INC_PER;
        if (increment == 0) {
            increment = 1;
        }

        for (double i = kpmSettings.MIN_PER; i <= kpmSettings.MAX_PER; i += increment) {
            percentages.add(i);
        }

        Map<String, List<Integer>> ret
                = new HashMap<String, List<Integer>>(kpmSettings.NUM_STUDIES);
        for (String lid : kpmSettings.externalToInternalIDManager.getInternalIdentifiers()) {
            List<Integer> exceptionList = new ArrayList<Integer>();
            double cases = 0;
            if (kpmSettings.NUM_CASES_MAP.containsKey(lid)) {
                cases = (double) kpmSettings.NUM_CASES_MAP.get(lid);
            } else if (kpmSettings.MIN_L.containsKey(lid)) {
                cases = (double) kpmSettings.MIN_L.get(lid);
            }

            for (double i = kpmSettings.MIN_PER; i <= kpmSettings.MAX_PER; i += increment) {
                int excp = (int) Math.round((i / 100.0) * (double) cases);
                exceptionList.add(excp);
            }
            ret.put(lid, exceptionList);
        }
        return ret;
    }

    private List<Integer> range(int min, int max, int inc) {
        List<Integer> ret = new ArrayList<Integer>();
        for (int i = min; i <= max; i += inc) {
            ret.add(i);
        }
        return ret;
    }

    private List<Result> removeDoubleSolutions(List<Result> results) {
        List<Result> toReturn = new ArrayList<Result>();
        int i = 0;

        while (i < results.size()) {
            Result r = results.get(i);
            if (!toReturn.contains(r)) {
                toReturn.add(r);
            }
            i++;
        }
        return toReturn;
    }

    private DecimalFormat getFormatedInt(int maxValue) {
        String ret = "";
        int zeros = (int) Math.floor(Math.log10(maxValue)) + 1;
        for (int i = 0; i < zeros; i++) {
            ret += "0";
        }
        return new DecimalFormat(ret);
    }

    synchronized public void cancel() {
        this.cancelled = true;
        if (kpmSettings.ALGO != null) {
            //kpmSettings.ALGO.cancel();
        }
        this.listener.runCancelled("Not known.", kpmSettings.RunID);
    }

    private int percentageToActual(int total, int percentageAsInt) {
        double percentage = (double) ((double) percentageAsInt / (double) 100);
        return (int) (total * percentage);
    }

    @Override
    public void run() {
        try {
            taskMonitor.setProgress(0.01);
            taskMonitor.setStatusMessage("Performing pre-processing operations...");

            if (kpmSettings.IS_BATCH_RUN) {
                runBatch();

//            } else if (kpmSettings.CALCULATE_ONLY_SAME_L_VALUES) {
//                runPackL();
            } else {
                runSingle();
            }
            taskMonitor.setProgress(1.0);
            if (kpmSettings.STATS_MAP == null) {
                kpmSettings.STATS_MAP = new HashMap<List<Integer>, RunStats>();
                taskMonitor.setStatusMessage("No pathways founds");

            } else if (kpmSettings.STATS_MAP.isEmpty()) {
                taskMonitor.setStatusMessage("No pathways founds");
            }

            // Preparing the results:
            StandardResultSet resultSet;
            
            if (this.copyKPMSettings) {
                resultSet = new StandardResultSet(new KPMSettings(kpmSettings), getResults(), getStandardCharts(), getOverlapResultsOrNull());
            } else {
                resultSet = new StandardResultSet(kpmSettings, getResults(), getStandardCharts(), getOverlapResultsOrNull());
            }

            this.listener.runFinished(resultSet);

        } catch (Exception e) {
            // Ensure we catch all errors
            KpmLogger.log(Level.SEVERE, e);
            cancel();
        }
    }

    /**
     * Runs the overlap counter, and fetches the results, cleaning any old
     * results.
     *
     * @return
     */
    private List<ValidationOverlapResult> getOverlapResultsOrNull() {
        List<ValidationOverlapResult> res = null;
        
        if (kpmSettings.VALIDATION_GOLDSTANDARD_NODES != null && kpmSettings.VALIDATION_GOLDSTANDARD_NODES.size() > 0) {
            this.taskMonitor.setStatusMessage("Calculating overlap results.");
            // Clear old results
            res = NodeOverlapCounter.compareResultsToGoldStandard(kpmSettings);
            kpmSettings.ValidationOverlapResults = res;

            this.taskMonitor.setStatusMessage("Finishing overlap results.");
        }
        
        return res;
    }

    private Map<String, IChart> getStandardCharts() {

        this.taskMonitor.setStatusMessage("Creating standard charts.");
        HashMap<String, IChart> standardCharts = new HashMap<String, IChart>();

        ArrayList<ChartInput> chartInputList = new ArrayList<ChartInput>();

        if (kpmSettings.CALCULATE_ONLY_SAME_L_VALUES) {
            if (kpmSettings.STATS_MAP_PER == null) {
                return new HashMap<String, IChart>();
            }

            if (kpmSettings.STATS_MAP_PER.size() == 0) {
                return new HashMap<String, IChart>();
            }

            for (PercentageParameters perpar : kpmSettings.STATS_MAP_PER.keySet()) {
                RunStats rs = kpmSettings.STATS_MAP_PER.get(perpar);
                chartInputList.add(new ChartInput(perpar.getK(), (int) perpar.getlPer(), rs.getResults()));
            }
        } else if (kpmSettings.VARYING_L_ID.size() == 1) {
            List<String> identifiers = kpmSettings.externalToInternalIDManager.getInternalIdentifiers();
            for (List<Integer> params : kpmSettings.STATS_MAP.keySet()) {
                RunStats rs = kpmSettings.STATS_MAP.get(params);

                int k = params.get(0);

                int l = 0;
                String varLID = kpmSettings.VARYING_L_ID.get(0);
                if (identifiers.contains(varLID)) {
                    l = params.get(identifiers.indexOf(varLID) + 1);
                } else if (kpmSettings.CASE_EXCEPTIONS_MAP != null && kpmSettings.CASE_EXCEPTIONS_MAP.size() > 0) {
                    for (String lid : kpmSettings.CASE_EXCEPTIONS_MAP.keySet()) {
                        l = kpmSettings.CASE_EXCEPTIONS_MAP.get(lid);
                    }
                }
                chartInputList.add(new ChartInput(k, l, rs.getResults()));
            }
        } else if (kpmSettings.VARYING_L_ID.size() == 2) {
            
            List<String> identifiers = kpmSettings.externalToInternalIDManager.getInternalIdentifiers();
            for (List<Integer> params : kpmSettings.STATS_MAP.keySet()) {
                RunStats rs = kpmSettings.STATS_MAP.get(params);

                int l1 = 0;
                int l2 = 0;
                String varLID1 = kpmSettings.VARYING_L_ID.get(0);
                String varLID2 = kpmSettings.VARYING_L_ID.get(1);
                if (identifiers.contains(varLID1)) {
                    l1 = params.get(identifiers.indexOf(varLID1) + 1);
                } else if (kpmSettings.CASE_EXCEPTIONS_MAP != null && kpmSettings.CASE_EXCEPTIONS_MAP.size() > 0) {
                    for (String lid : kpmSettings.CASE_EXCEPTIONS_MAP.keySet()) {
                        l1 = kpmSettings.CASE_EXCEPTIONS_MAP.get(lid);
                    }
                }

                if (identifiers.contains(varLID2)) {
                    l2 = params.get(identifiers.indexOf(varLID2) + 1);
                } else if (kpmSettings.CASE_EXCEPTIONS_MAP != null && kpmSettings.CASE_EXCEPTIONS_MAP.size() > 0) {
                    for (String lid : kpmSettings.CASE_EXCEPTIONS_MAP.keySet()) {
                        l2 = kpmSettings.CASE_EXCEPTIONS_MAP.get(lid);
                    }
                }

                chartInputList.add(new ChartInput(l1, l2, rs.getResults()));
            }
        } else {
            List<String> identifiers = kpmSettings.externalToInternalIDManager.getInternalIdentifiers();
            for (List<Integer> params : kpmSettings.STATS_MAP.keySet()) {
                RunStats rs = kpmSettings.STATS_MAP.get(params);

                int k = params.get(0);

                int l = 0;

                if (kpmSettings.CASE_EXCEPTIONS_MAP != null && kpmSettings.CASE_EXCEPTIONS_MAP.size() > 0) {
                    for (String lid : kpmSettings.CASE_EXCEPTIONS_MAP.keySet()) {
                        l = kpmSettings.CASE_EXCEPTIONS_MAP.get(lid);
                    }
                }
                chartInputList.add(new ChartInput(k, l, rs.getResults()));
            }
        }

        boolean allSameK = true;
        boolean first = true;
        int lastK = -1;
        if (kpmSettings.VARYING_L_ID.size() != 2) {
            for (ChartInput inp : chartInputList) {
                if (first == false && lastK != inp.VAR1) {
                    allSameK = false;
                    break;
                }

                lastK = inp.VAR1;
                first = false;
            }
        }

        boolean L_inPercentage = false;
        if (kpmSettings.CALCULATE_ONLY_SAME_L_VALUES) {
            L_inPercentage = true;
        }

        boolean l_fixed = true;
        for (String lid : kpmSettings.MIN_L.keySet()) {
            if (!kpmSettings.MIN_L.get(lid).equals(kpmSettings.MAX_L.get(lid))) {
                l_fixed = false;
                break;
            }
        }

        if (!allSameK) {
            IChart knChart = StandardCharts.K_vs_Nodes(chartInputList,
                    L_inPercentage, kpmSettings.VARYING_L_ID.isEmpty());
            if (knChart != null) {
                String knTitle = "K vs Nodes";
                knChart.setTitle(knTitle);
                standardCharts.put(knTitle, knChart);
            }

            IChart knavgChart = StandardCharts.K_vs_Nodes_averaged(chartInputList, L_inPercentage,
                    kpmSettings.VARYING_L_ID.isEmpty());
            if (knavgChart != null) {
                String knavgTitle = "K vs Nodes (averaged)";
                knavgChart.setTitle(knavgTitle);
                standardCharts.put(knavgTitle, knavgChart);
            }
        }

        if (kpmSettings.VARYING_L_ID.size() == 1) {
            String varL = kpmSettings.VARYING_L_ID.get(0);
            boolean allSameL = true;
            first = true;
            int lastL = -1;
            for (ChartInput inp : chartInputList) {
                if (first == false && lastL != inp.VAR2) {
                    allSameL = false;
                    break;
                }

                lastL = inp.VAR2;
                first = false;
            }
            if (!allSameL) {
                IChart lnChart = StandardCharts.L_vs_Nodes(chartInputList, L_inPercentage, "K", true);
                    
                if (!kpmSettings.USE_INES) {
                    lnChart = StandardCharts.L_vs_Nodes(chartInputList, L_inPercentage, "L", false);
                }
                if (lnChart != null) {
                    String lnTitle = varL + " vs Nodes";
                    lnChart.setTitle(lnTitle);
                    standardCharts.put(lnTitle, lnChart);
                }

                IChart lnavgChart = StandardCharts.L_vs_Nodes_averaged(chartInputList, L_inPercentage, "K", true);
                if (!kpmSettings.USE_INES) {
                    lnavgChart = StandardCharts.L_vs_Nodes_averaged(chartInputList, L_inPercentage, "L", false);
                }
                if (lnavgChart != null) {
                    String lnavgTitle = varL + " vs Nodes (averaged)";
                    lnavgChart.setTitle(lnavgTitle);
                    standardCharts.put(lnavgTitle, lnavgChart);
                }
            }
        } else if (kpmSettings.VARYING_L_ID.size() == 2) {
            String varL1 = kpmSettings.VARYING_L_ID.get(0);
            String varL2 = kpmSettings.VARYING_L_ID.get(1);
            boolean var1InPer = false;
            if (kpmSettings.VARYING_L_ID_IN_PERCENTAGE.containsKey(varL1)) {
                var1InPer = kpmSettings.VARYING_L_ID_IN_PERCENTAGE.get(varL1);
            } else {
    //            System.out.println(varL1 + " NOT FOUND IN PER");
            }

            boolean var2InPer = false;
            if (kpmSettings.VARYING_L_ID_IN_PERCENTAGE.containsKey(varL2)) {
                var2InPer = kpmSettings.VARYING_L_ID_IN_PERCENTAGE.get(varL2);
            } else {
    //            System.out.println(varL2 + " NOT FOUND IN PER");
            }
            boolean allSameL1 = true;
            first = true;
            int lastL = -1;
            for (ChartInput inp : chartInputList) {
                if (first == false && lastL != inp.VAR1) {
                    allSameL1 = false;
                    break;
                }

                lastL = inp.VAR1;
                first = false;
            }

            if (!allSameL1) {

                IChart lnChart = StandardCharts.L1_vs_Nodes(chartInputList,
                        false, false,
                        varL1, varL2);
                if (lnChart != null) {
                    String lnTitle = varL1 + " vs Nodes";
                    lnChart.setTitle(lnTitle);
                    standardCharts.put(lnTitle, lnChart);
                }

                IChart lnavgChart = StandardCharts.L1_vs_Nodes_averaged(chartInputList,
                        false, false,
                        varL1, varL2);
                if (lnavgChart != null) {
                    String lnavgTitle = varL1 + " vs Nodes (averaged)";
                    lnavgChart.setTitle(lnavgTitle);
                    standardCharts.put(lnavgTitle, lnavgChart);
                }
            }

            boolean allSameL2 = true;
            first = true;
            lastL = -1;
            for (ChartInput inp : chartInputList) {
                if (first == false && lastL != inp.VAR2) {
                    allSameL2 = false;
                    break;
                }

                lastL = inp.VAR2;
                first = false;
            }

            if (!allSameL2) {
                IChart lnChart = StandardCharts.L2_vs_Nodes(chartInputList,
                        false, false,
                        varL1, varL2);
                if (lnChart != null) {
                    String lnTitle = varL2 + " vs Nodes";
                    lnChart.setTitle(lnTitle);
                    standardCharts.put(lnTitle, lnChart);
                }

                IChart lnavgChart = StandardCharts.L2_vs_Nodes_averaged(chartInputList,
                        false, false,
                        varL1, varL2);
                if (lnavgChart != null) {
                    String lnavgTitle = varL2 + " vs Nodes (averaged)";
                    lnavgChart.setTitle(lnavgTitle);
                    standardCharts.put(lnavgTitle, lnavgChart);
                }
            }

        } else if (kpmSettings.VARYING_L_ID.size() == 3) {
            boolean allSameL = true;
            first = true;
            int lastL = -1;
            for (ChartInput inp : chartInputList) {
                if (first == false && lastL != inp.VAR2) {
                    allSameL = false;
                    break;
                }

                lastL = inp.VAR2;
                first = false;
            }
            if (!allSameL) {
                IChart lnChart = StandardCharts.L_vs_Nodes(chartInputList, Boolean.TRUE, "K", true);
                if (!kpmSettings.USE_INES) {
                    lnChart = StandardCharts.L_vs_Nodes(chartInputList, Boolean.TRUE, "L", false);
                }
                if (lnChart != null) {
                    String lnTitle = "L vs Nodes";
                    lnChart.setTitle(lnTitle);
                    standardCharts.put(lnTitle, lnChart);
                }

                IChart lnavgChart = StandardCharts.L_vs_Nodes_averaged(chartInputList, Boolean.TRUE, "K", true);
                if (!kpmSettings.USE_INES) {
                    lnavgChart = StandardCharts.L_vs_Nodes_averaged(chartInputList, Boolean.TRUE, "L", false);
                }
                if (lnavgChart != null) {
                    String lnavgTitle = "L vs Nodes (averaged)";
                    lnavgChart.setTitle(lnavgTitle);
                    standardCharts.put(lnavgTitle, lnavgChart);
                }
            }
        }

        this.taskMonitor.setStatusMessage("Finishing standard charts.");
        return standardCharts;
    }

    private List<IKPMResultItem> getResults() {
        this.taskMonitor.setStatusMessage("Fetching results.");
        List<IKPMResultItem> results = new ArrayList<IKPMResultItem>();
        try {
            if ((kpmSettings.STATS_MAP == null || kpmSettings.STATS_MAP.isEmpty()) &&
                    (kpmSettings.STATS_MAP_PER == null || kpmSettings.STATS_MAP_PER.isEmpty())) {
                this.taskMonitor.setStatusMessage("Finished fetching results: But none was found.");
                return results;
            }

            if (kpmSettings.CALCULATE_ONLY_SAME_L_VALUES) {
                for (PercentageParameters params : kpmSettings.STATS_MAP_PER.keySet()) {
                    RunStats rs = kpmSettings.STATS_MAP_PER.get(params);

                    BatchResult br = GetResultFromRunStats(rs);
                    br.setK(params.getK());
                    br.setL((int) params.getlPer());

                    // We just add all the results.
                    results.add(br);
                }
                this.taskMonitor.setStatusMessage("Finished fetching results.");
                return results;
            }

            List<String> identifiers = kpmSettings.externalToInternalIDManager.getInternalIdentifiers();
            for (List<Integer> params : kpmSettings.STATS_MAP.keySet()) {
                RunStats rs = kpmSettings.STATS_MAP.get(params);
                BatchResult br = GetResultFromRunStats(rs);
                int k = params.get(0);
                int l1 = params.get(1);
                int l2 = 0;
                br.setK(k);
                br.setL(l1);
                if (kpmSettings.VARYING_L_ID.size() == 1) {
                    l1 = params.get(identifiers.indexOf(kpmSettings.VARYING_L_ID.get(0)) + 1);
                    br.setK(k);
                    br.setL(l1);
                } else if (kpmSettings.VARYING_L_ID.size() == 2) {
                    l1 = params.get(identifiers.indexOf(kpmSettings.VARYING_L_ID.get(1)) + 1);
                    l2 = params.get(identifiers.indexOf(kpmSettings.VARYING_L_ID.get(0)) + 1);
                    br.setK(l1);
                    br.setL(l2);
                }
  
//                if (!identifiers.contains(kpmSettings.VARYING_L_ID)) {
//                    break;
//                }
//
//                int l = params.get(identifiers.indexOf(kpmSettings.VARYING_L_ID) + 1);

                results.add(br);
            }

            // If this case is true, we either do not have a varying L, or we are not using INES
//            if (!identifiers.contains(kpmSettings.VARYING_L_ID) && results.isEmpty()) {
//                System.out.println("5. getResults() BATCH RUNNER");
//                for (List<Integer> params : kpmSettings.STATS_MAP.keySet()) {
//                    System.out.println("5.1 loop getResults() BATCH RUNNER");
//                    RunStats rs = kpmSettings.STATS_MAP.get(params);
//
//                    BatchResult br = GetResultFromRunStats(rs);
//                    int k = params.get(0);
//                    br.setK(k);
//
//                    if (params.size() > 1) {
//                        int l = params.get(1);
//                        br.setL(l);
//                    }
//                    // We just add all the results.
//                    results.add(br);
//                }
//            }
        } catch (Exception e) {
            KpmLogger.log(Level.SEVERE, e);
        }

        this.taskMonitor.setStatusMessage("Finished fetching results.");

        return results;
    }

    private BatchResult GetResultFromRunStats(RunStats rs) {
        Map<String, GeneNode> unionResultNodes = new HashMap<String, GeneNode>();
        Map<String, Integer> unionResultNodeCounts = new HashMap<String, Integer>();
        List<Map<String, GeneNode>> allComputedNodeSets = new ArrayList<Map<String, GeneNode>>();

        // Iterate over all results
        for (Result res : rs.getResults()) {
            Map<String, GeneNode> visitedNodes = res.getVisitedNodes();

            // For each key, only add if we haven't added it already.
            for (String nodeID : visitedNodes.keySet()) {
                if (!unionResultNodes.containsKey(nodeID)) {
                    unionResultNodes.put(nodeID, visitedNodes.get(nodeID));
                }

                int count = unionResultNodeCounts.containsKey(nodeID) ? unionResultNodeCounts.get(nodeID) : 0;
                unionResultNodeCounts.put(nodeID, count + 1);
            }

            allComputedNodeSets.add(visitedNodes);
        }

        return new BatchResult(unionResultNodes, allComputedNodeSets, unionResultNodeCounts, rs.getValues());
    }

    private class CartesianProduct implements Iterable<int[]>, Iterator<int[]> {

        private final int[] lengths;
        private final int[] indices;
        private boolean hasNext = true;

        public CartesianProduct(int[] lengths) {
            this.lengths = lengths;
            indices = new int[lengths.length];
        }

        @Override
        public boolean hasNext() {
            return hasNext;
        }

        @Override
        public int[] next() {
            int[] result = Arrays.copyOf(indices, indices.length);
            for (int i = indices.length - 1; i >= 0; i--) {
                if (indices[i] == lengths[i] - 1) {
                    indices[i] = 0;
                    if (i == 0) {
                        hasNext = false;
                    }
                } else {
                    indices[i]++;
                    break;
                }
            }
            return result;
        }

        @Override
        public Iterator<int[]> iterator() {
            return this;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
}

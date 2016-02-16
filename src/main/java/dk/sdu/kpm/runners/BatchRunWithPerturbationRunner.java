package dk.sdu.kpm.runners;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;

import dk.sdu.kpm.RunStats;
import dk.sdu.kpm.perturbation.IPerturbation;
import dk.sdu.kpm.results.PercentageParameters;
import dk.sdu.kpm.taskmonitors.IKPMTaskMonitor;
import dk.sdu.kpm.KPMSettings;
import dk.sdu.kpm.logging.KpmLogger;
import dk.sdu.kpm.results.IKPMResultSet;
import dk.sdu.kpm.results.IKPMRunListener;
import dk.sdu.kpm.charts.IChart;
import dk.sdu.kpm.graph.KPMGraph;
import dk.sdu.kpm.perturbation.InvalidParametersException;
import dk.sdu.kpm.statistics.BatchStatistics;

/***
 * For running KPM on random perturbated graphs 
 * @author Martin
 *
 */
public class BatchRunWithPerturbationRunner implements Runnable, IKPMTaskMonitor, IKPMRunListener{

    private volatile boolean cancelled;

    private BatchRunWithPerturbationParameters parameters;

    private double currentStep;

    private double nrSteps;

    private IKPMResultSet originalKPMrunResultSet;

    private volatile KPMSettings kpmSettings;

    private List<PerturbationResult> otherKPMResults;

    private volatile boolean isUnperturbedRun;

    private volatile int currentPerturbationPercent;

    private volatile int currentGraphNr;

    public volatile HashMap<List<Integer>, RunStats> ORIG_STATS_MAP;

    public volatile HashMap<PercentageParameters, RunStats> ORIG_STATS_MAP_PER;
    /**
     * The original settings, as they were after the original, unperturbed run.
     */
    private volatile KPMSettings originalKpmSettings;

    private volatile BatchRunner currentRunner;

    public BatchRunWithPerturbationRunner(BatchRunWithPerturbationParameters parameters, KPMSettings settings){
        this.parameters = parameters;
        this.otherKPMResults = new ArrayList<PerturbationResult>();
        this.kpmSettings = settings;

        this.cancelled = false;

        // We always start with the first run.
        this.isUnperturbedRun = true;
    }



    @Override
    public void run(){
        try{
            runWithPerturbation();
        }catch(Exception e){
            // Ensure we always log errors.
            KpmLogger.log(Level.SEVERE, e);
            this.cancel();
        }
    }

    private void runWithPerturbation(){

        // Check input.
        if(		   parameters.minPercentage < 0
                || parameters.maxPercentage < 0
                || parameters.minPercentage > parameters.maxPercentage
                || parameters.stepPercentage < 0){
            try {
                throw new InvalidParametersException("Invalid percentages.");
            } catch (InvalidParametersException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            return;
        }

        if(parameters.graphsPerStep < 1){
            parameters.graphsPerStep = 1;
        }

        if(parameters.permuter == null){
            try {
                throw new InvalidParametersException("No pertubation technique set.");
            } catch (InvalidParametersException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            return;
        }


        // Backup the unpermuted graph.
        kpmSettings.MAIN_GRAPH_BACKUP = new KPMGraph(kpmSettings.MAIN_GRAPH);
        kpmSettings.USE_INES = parameters.isINEs;

        if(parameters.stepPercentage < 1){
            parameters.stepPercentage = 1;
        }

        nrSteps = 1;
        for(int i = parameters.minPercentage; i <= parameters.maxPercentage; i += parameters.stepPercentage){
            for(int j = 1; j <= parameters.graphsPerStep; j++) {
                nrSteps++;
            }
        }
        parameters.taskMonitor.setTitle("Key Pathway Miner");

        // Run KPM once without perturbation:
        parameters.taskMonitor.setTitle(String.format("No perturbation (Step %d/%d)", 1, (int) nrSteps));


        // prepare the BatchRunner, ensuring that this class consumes all responses for being both IBatchListener and IKPMTaskMonitor.
        clearOldResults();
        
        
        currentRunner = new BatchRunner(parameters.runId, this, this, kpmSettings);

        
        // Copy the KPMSettings, such that we know the results are saved for later as they were after the original run
        currentRunner.setCopyKPMSettings(true);
        
        // Start the run.
        currentRunner.run();
        
        this.isUnperturbedRun = false;
        
        // add standard graphs for default run, if needed.
        currentStep = 1;

        // Start doing iterations with different permuted graphs
        for (int i = parameters.minPercentage; i <= parameters.maxPercentage; i += parameters.stepPercentage) {
            for (int j = 1; j <= parameters.graphsPerStep; j++) {
                if (isCancelled()) {
                    this.parameters.listener.runCancelled("The run was cancelled.", kpmSettings.RunID);
                    clearOldResults();
                    break;
                }

                currentPerturbationPercent = i;
                currentGraphNr = j;

                parameters.taskMonitor.setTitle(String.format("Perturbation %d%%, Graph %d/%d (Step %d/%d)", i, j, parameters.graphsPerStep, (int) currentStep + 1, (int) nrSteps));

                // Clear any old results.
                clearOldResults();

                // Set the current graph to be the graph which was permuted from the backup.

                IPerturbation<KPMGraph> perturber = (IPerturbation<KPMGraph>) parameters.permuter;
                 System.out.println("NUMBER OF EDGES GRAPH: " +kpmSettings.MAIN_GRAPH_BACKUP.getEdgeCount());
                kpmSettings.MAIN_GRAPH = perturber.execute(i, kpmSettings.MAIN_GRAPH_BACKUP, null);
               
                kpmSettings.MAIN_GRAPH.refreshGraph(kpmSettings);

                // Initialize new one:
                currentRunner = new BatchRunner(parameters.runId, this, this, kpmSettings);

                // Run KPM --> will call runFinished() method in this class once done.
                currentRunner.run();

                currentStep++;
            }
        
            if (isCancelled()) {
                break;
            }

        }


        if (parameters.listener != null) {
            parameters.taskMonitor.setStatusMessage("Finishing: Notifying listeners");
            this.originalKPMrunResultSet.setKpmSettings(kpmSettings);
            BatchStatistics stats = new BatchStatistics(this.originalKPMrunResultSet, this.parameters, kpmSettings.getKpmRunID());
            stats.calculate(this.otherKPMResults);
            

            // Basing our final result on the original, adding to the results there:
            IKPMResultSet set = this.originalKPMrunResultSet;

            try {
                KpmLogger.log(Level.WARNING, "ORIG_STATS_MAP.size() = " + ORIG_STATS_MAP.size());
                set.getKpmSettings().STATS_MAP = ORIG_STATS_MAP;
            }catch (Exception e){
                KpmLogger.log(Level.SEVERE, e);
            }

            try {
                KpmLogger.log(Level.WARNING, "ORIG_STATS_MAP_PER.size() = " + ORIG_STATS_MAP_PER.size());
                set.getKpmSettings().STATS_MAP_PER = ORIG_STATS_MAP_PER;
            }catch (Exception e){
                KpmLogger.log(Level.SEVERE, e);
            }

            for(String chartName: stats.getCharts().keySet()){
                IChart chart = stats.getCharts().get(chartName);

                if(!chart.containsTag(IChart.TagEnum.STANDARD)){
                    set.getCharts().put(chartName, chart);
                }
            }

            parameters.listener.runFinished(set);
        } else {
            parameters.taskMonitor.setStatusMessage("Finishing: No listeners?");
        }
    }

    private void clearOldResults(){

        kpmSettings.MAIN_GRAPH_BACKUP.refreshGraph(kpmSettings);

        if(kpmSettings.STATS_MAP != null){
            kpmSettings.STATS_MAP.clear();
        }

        if(kpmSettings.TOTAL_NODE_HITS != null){
            kpmSettings.TOTAL_NODE_HITS.clear();
        }

        if(kpmSettings.TOTAL_EDGE_HITS != null){
            kpmSettings.TOTAL_EDGE_HITS.clear();
        }

        kpmSettings.TOTAL_NODE_HITS_MAX = kpmSettings.TOTAL_EDGE_HITS_MAX = 0;
        kpmSettings.TOTAL_NODE_HITS_MIN = kpmSettings.TOTAL_EDGE_HITS_MIN = Integer.MAX_VALUE;
    }

    synchronized public void cancel(){
        this.cancelled = true;

        // Remember to cancel the current run.
        if(this.currentRunner != null){
            currentRunner.cancel();
        }
    }

    synchronized private boolean isCancelled(){
        return this.cancelled;
    }

    // ------------------------------------------------------
    // Methods from IKPMTaskMonitor. To handle multiple iterations of a run, without confusing the actual monitor:
    // ------------------------------------------------------

    @Override
    public void setTitle(String title) {
        // TODO Auto-generated method stub

    }

    @Override
    public void setProgress(double progress) {
        if(parameters.taskMonitor != null){
            if(progress > 1){
                progress = 1;
            }

            double stepSize = ((double)1)/((double)nrSteps);
            double currentProgress = progress*stepSize;
            double progPercent = (double)((double)currentStep+currentProgress) / (double)nrSteps;
            parameters.taskMonitor.setProgress(progPercent);
        }
    }

    @Override
    public void setStatusMessage(String statusMessage) {
        if(parameters.taskMonitor != null){
            parameters.taskMonitor.setStatusMessage(statusMessage);
        }
    }



    // ------------------------------------------------------
    // Methods from IKPMRunListener, picking up results from iteratively executing the BatchRunner:
    // ------------------------------------------------------

    @Override
    public void runFinished(IKPMResultSet results) { 
        if(isUnperturbedRun){
            this.originalKPMrunResultSet = results;
            ORIG_STATS_MAP = new HashMap<List<Integer>, RunStats>();
            if(results.getKpmSettings().STATS_MAP != null){
                for(List<Integer> key : results.getKpmSettings().STATS_MAP.keySet()){
                    ORIG_STATS_MAP.put(key, new RunStats(results.getKpmSettings().STATS_MAP.get(key)));
                    KpmLogger.log(Level.WARNING, "saving orig_stats_map");
                }
            }
            ORIG_STATS_MAP_PER = new HashMap<PercentageParameters, RunStats>();
            if(results.getKpmSettings().STATS_MAP_PER != null){
                for(PercentageParameters key : results.getKpmSettings().STATS_MAP_PER.keySet()){
                    ORIG_STATS_MAP_PER.put(key, new RunStats(results.getKpmSettings().STATS_MAP_PER.get(key)));
                    KpmLogger.log(Level.WARNING, "saving orig_stats_map_per");
                }
            }
            return;
        }

        PerturbationResult res = new PerturbationResult(currentPerturbationPercent, currentGraphNr, results.getResults(), results.getOverlapResults());
        this.otherKPMResults.add(res);
    }

    @Override
    public void runCancelled(String reason, String runID) {
        // TODO Auto-generated method stub
    }
}

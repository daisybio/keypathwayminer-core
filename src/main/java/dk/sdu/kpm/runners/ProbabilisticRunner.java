package dk.sdu.kpm.runners;

import dk.sdu.kpm.AlgoComputations;
import dk.sdu.kpm.KPMSettings;
import dk.sdu.kpm.RunStats;
import dk.sdu.kpm.algo.fdr.DistributionGenerator;
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
import java.util.List;

public class ProbabilisticRunner implements Runnable {

    private String runId;
    private IKPMTaskMonitor taskMonitor;
    private IKPMRunListener listener;
    private int totalPathways;
    private List<Double> percentages;
    private volatile boolean cancelled;
    private volatile boolean copyKPMSettings;

    private volatile KPMSettings kpmSettings;

    public ProbabilisticRunner(String runId, IKPMTaskMonitor taskMonitor,
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

    @Override
    public void run() {
        //Awesome stuff TODO

        // STEP 1: Generate the distribbution you will need to assess your result.
       // DistributionGenerator dg = new DistributionGenerator(this.kpmSettings.MAIN_GRAPH, 1000, 1,100, true);
        //dg.createBackgroundDistribution();
        //dg.writeDistributionToFile("/home/anne/Documents/Master/MA/Testing/out/dist/distribution.txt", dg.getDistribution());
       // dg.writeDistributionToFile("/home/anne/Documents/Master/MA/Testing/out/dist/pdistribution.txt", dg.getPdist());
        DistributionGenerator dg1 = new DistributionGenerator(this.kpmSettings.MAIN_GRAPH, 1000, 1,100, false);
        dg1.createBackgroundDistribution();
        dg1.writeDistributionToFile("/home/anne/Documents/Master/MA/Testing/out_new/dist/distributionFall.txt", dg1.getDistribution());
        dg1.writeDistributionToFile("/home/anne/Documents/Master/MA/Testing/out_new/dist/pdistributionFall.txt", dg1.getPdist());
    }

}

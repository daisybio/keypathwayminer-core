package dk.sdu.kpm.runners;

import dk.sdu.kpm.AlgoComputations;
import dk.sdu.kpm.KPMSettings;
import dk.sdu.kpm.RunStats;
import dk.sdu.kpm.algo.fdr.DistributionGenerator;
import dk.sdu.kpm.algo.fdr.RandomSubgraph;
import dk.sdu.kpm.algo.fdr.Testcase;
import dk.sdu.kpm.charts.ChartInput;
import dk.sdu.kpm.charts.IChart;
import dk.sdu.kpm.charts.StandardCharts;
import dk.sdu.kpm.graph.GeneEdge;
import dk.sdu.kpm.graph.GeneNode;
import dk.sdu.kpm.graph.KPMGraph;
import dk.sdu.kpm.graph.Result;
import dk.sdu.kpm.logging.KpmLogger;
import dk.sdu.kpm.results.IKPMResultItem;
import dk.sdu.kpm.results.IKPMRunListener;
import dk.sdu.kpm.results.PercentageParameters;
import dk.sdu.kpm.results.StandardResultSet;
import dk.sdu.kpm.taskmonitors.IKPMTaskMonitor;
import dk.sdu.kpm.taskmonitors.KPMDummyTaskMonitor;
import dk.sdu.kpm.validation.NodeOverlapCounter;
import dk.sdu.kpm.validation.ValidationOverlapResult;

import java.io.*;
//import de.mpg.mpiinf.ag1.kpm.utils.*;
import dk.sdu.kpm.perturbation.*;

import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.List;

import static dk.sdu.kpm.perturbation.IPerturbation.PerturbationTags.EdgeRewire;
import static java.lang.System.exit;

public class ProbabilisticRunner implements Runnable {

    private String runId;
    private IKPMTaskMonitor taskMonitor;
    private IKPMRunListener listener;
    private int totalPathways;
    private List<Double> percentages;
    private volatile boolean cancelled;
    private volatile boolean copyKPMSettings;
    private KPMGraph graph2;
    private String outdir;

    private volatile KPMSettings kpmSettings;
    private IPerturbation.PerturbationTags tag;

    public ProbabilisticRunner(String runId, IKPMTaskMonitor taskMonitor,
                               IKPMRunListener listener, KPMSettings settings, KPMGraph graph2,
                               String outdir, IPerturbation.PerturbationTags tag) {
        this.runId = runId;
        this.taskMonitor = taskMonitor;
        this.listener = listener;
        this.totalPathways = 0;
        this.cancelled = false;
        this.kpmSettings = settings;
        this.copyKPMSettings = false;
        this.graph2 = graph2;
        this.outdir = outdir;
        this.tag = tag;
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
        try {
            // Starting time
            long start = System.currentTimeMillis();
            this.taskMonitor.setStatusMessage(this.kpmSettings.toString());

            boolean general = true;

            // create output directory if it does not exist yet
            Files.createDirectories(Paths.get(outdir));

            //Testcase tc = new Testcase(this.kpmSettings.MAIN_GRAPH, this.kpmSettings, "/home/anne/Documents/Master/MA/pipeline_new/sample_networks/StringDB/");
            //tc.createTestcases();
            //tc.createRandomDistribution(10000);
            //exit(0);

            // STEP 1: Generate the distribbution
            IPerturbation<KPMGraph> ps = PerturbationService.getPerturbation(tag, kpmSettings);
            this.taskMonitor.setStatusMessage("Permuting graph");
            this.graph2 = ps.execute(kpmSettings.PERC_PERTURBATION, kpmSettings.MAIN_GRAPH, this.taskMonitor);
            this.taskMonitor.setStatusMessage("Finished permuting graph");
            this.taskMonitor.setStatusMessage("Started generating background score distribution");
            DistributionGenerator dg1 = new DistributionGenerator(this.graph2, kpmSettings.NR_SAMPLES_BACKROUND, 400, kpmSettings);
            dg1.createBackgroundDistribution(outdir + "/distribution_", general);
            this.taskMonitor.setStatusMessage("Finished generating background score distribution");

            taskMonitor.setStatusMessage("Refreshing graph...");
            kpmSettings.MAIN_GRAPH.refreshGraph(kpmSettings);
            taskMonitor.setStatusMessage("Searching and extracting pathways...");

            try (BufferedWriter bw = new BufferedWriter(new FileWriter(outdir + "/thresholds.txt"))) {
                for (int d = 0; d < dg1.getThresholds().length; d++) {
                    bw.write(d + "\t" + dg1.getThresholds()[d] + "\n");
                }


            } catch (IOException e) {
                e.printStackTrace();
            }
            try (BufferedWriter bw = new BufferedWriter(new FileWriter(outdir + "/teststats.txt"))) {
                for (int d = 0; d < dg1.getMeanTeststats().length; d++) {
                    bw.write(d + "\t" + dg1.getMeanTeststats()[d] + "\n");
                }


            } catch (IOException e) {
                e.printStackTrace();
            }

            // STEP 2: Search pathways
            List<Result> results = new AlgoComputations().run(kpmSettings.ALGO, kpmSettings.MAIN_GRAPH, taskMonitor, kpmSettings, dg1, general);

            long end = System.currentTimeMillis();
            kpmSettings.TOTAL_RUNNING_TIME = (end - start) / 1000;


            // STEP 3: Write output to files
            int counter = 0;
            for (Result res : results) {
                if (res instanceof RandomSubgraph && ((RandomSubgraph) res).getVertices().size() > 1) {
                    ((RandomSubgraph) res).writeGraphToFile(outdir + "/file", "graph" + counter, general);
                    counter++;
                }
            }

            // Write an R markdown report. May fail.
            //String command = "Rscript " + "~/Masterarbeit/R/reports/create_report_greedy.R" + "" +" -f " + outdir+ " -g ~/Masterarbeit/data/toydata/expression_type.tsv";
            //Process process = Runtime.getRuntime().exec(command);


            StandardResultSet resultSet = null;
            this.listener.runFinished(resultSet);

        } catch (Exception e) {
            // Ensure we catch all errors
            KpmLogger.log(Level.SEVERE, e);
            cancel();
        }


    }

    public void getResults() {

    }

    public void getStandardCharts() {

    }

    public void getOverlapResultsOrNull() {

    }


    synchronized public void cancel() {
        this.cancelled = true;
        if (kpmSettings.ALGO != null) {
            //kpmSettings.ALGO.cancel();
        }
        this.listener.runCancelled("Not known.", kpmSettings.RunID);
    }

}

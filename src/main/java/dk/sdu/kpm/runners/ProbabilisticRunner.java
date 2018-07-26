package dk.sdu.kpm.runners;

import dk.sdu.kpm.AlgoComputations;
import dk.sdu.kpm.KPMSettings;
import dk.sdu.kpm.RunStats;
import dk.sdu.kpm.algo.fdr.DistributionGenerator;
import dk.sdu.kpm.algo.fdr.RandomSubgraph;
import dk.sdu.kpm.charts.ChartInput;
import dk.sdu.kpm.charts.IChart;
import dk.sdu.kpm.charts.StandardCharts;
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

    public ProbabilisticRunner(String runId, IKPMTaskMonitor taskMonitor,
                               IKPMRunListener listener, KPMSettings settings, KPMGraph graph2,
                               String outdir) {
        this.runId = runId;
        this.taskMonitor = taskMonitor;
        this.listener = listener;
        this.totalPathways = 0;
        this.cancelled = false;
        this.kpmSettings = settings;
        this.copyKPMSettings = false;
        this.graph2 = graph2;
        this.outdir = outdir;
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
        try{
        // STEP 1: Generate the distribbution you will need to assess your result.
       // DistributionGenerator dg = new DistributionGenerator(this.kpmSettings.MAIN_GRAPH, 1000, 1,100, true);
        //dg.createBackgroundDistribution();
        //dg.writeDistributionToFile("/home/anne/Documents/Master/MA/Testing/out/dist/distribution.txt", dg.getDistribution());
       // dg.writeDistributionToFile("/home/anne/Documents/Master/MA/Testing/out/dist/pdistribution.txt", dg.getPdist());
            long now = System.currentTimeMillis();

            boolean general = true;
            // String newpath =
            Files.createDirectories(Paths.get(outdir));

            IPerturbation<KPMGraph> ps = PerturbationService.getPerturbation(IPerturbation.PerturbationTags.NodeSwap);

            this.graph2 = ps.execute(10, kpmSettings.MAIN_GRAPH, new KPMDummyTaskMonitor());
            DistributionGenerator dg1 = new DistributionGenerator(this.graph2, 100, 1,250, false,
                    kpmSettings);
        dg1.createBackgroundDistribution(outdir+"/distribution_", general);
        //dg1.writeDistributionToFile("/home/anne/Documents/Master/MA/Testing/toydata/dist/distributionFall.txt", dg1.getDistribution());
        //dg1.writeDistributionToFile("/home/anne/Documents/Master/MA/Testing/toydata/dist/pdistributionFall.txt", dg1.getPdist());
        taskMonitor.setStatusMessage("Refreshing graph...");
        kpmSettings.MAIN_GRAPH.refreshGraph(kpmSettings);
        taskMonitor.setStatusMessage("Searching and extracting pathways...");
        long start = System.currentTimeMillis();

        try(BufferedWriter bw = new BufferedWriter(new FileWriter(outdir+"/thresholds.txt"))) {
            for (int d = 0; d<dg1.getThresholds().length; d++) {
                bw.write(d+"\t"+dg1.getThresholds()[d]+"\n");
            }


        }
        catch(IOException e){
            e.printStackTrace();
        }
            try(BufferedWriter bw = new BufferedWriter(new FileWriter(outdir+"/teststats.txt"))) {
                for (int d = 0; d<dg1.getMeanTeststats().length; d++) {
                    bw.write(d+"\t"+dg1.getMeanTeststats()[d]+"\n");
                }


            }
            catch(IOException e){
                e.printStackTrace();
            }
        System.out.print(this.kpmSettings.toString());
        List<Result> results = new AlgoComputations().run(kpmSettings.ALGO, kpmSettings.MAIN_GRAPH, taskMonitor, kpmSettings, dg1, general);

        long end = System.currentTimeMillis();
        kpmSettings.TOTAL_RUNNING_TIME = (end - start) / 1000;
        int counter = 0;

        for(Result res: results){
            if(res instanceof RandomSubgraph && ((RandomSubgraph) res).getVertices().size()>1){
                ((RandomSubgraph) res).writeGraphToFile(outdir+"/file", "graph"+counter, general);
                counter++;
            }
        }
        // Write an R markdown report. May fail.
            String command = "Rscript "+ "~/Masterarbeit/R/reports/create_report_greedy.R" +"" +
                    " -f "+outdir
                    + " -g ~/Masterarbeit/data/toydata/expression_type.tsv";
        System.out.println(command);
            Process process = Runtime.getRuntime().exec(command);


        StandardResultSet resultSet = null;

       /* if (this.copyKPMSettings) {
            resultSet = new StandardResultSet(new KPMSettings(kpmSettings), getResults(), getStandardCharts(), getOverlapResultsOrNull());
        } else {
            resultSet = new StandardResultSet(kpmSettings, getResults(), getStandardCharts(), getOverlapResultsOrNull());
        }
*/
        this.listener.runFinished(resultSet);

    } catch (Exception e) {
        // Ensure we catch all errors
        KpmLogger.log(Level.SEVERE, e);
        cancel();
    }


    }
    public void getResults(){

    }
    public void getStandardCharts(){

    }
    public void getOverlapResultsOrNull(){

    }


    synchronized public void cancel() {
        this.cancelled = true;
        if (kpmSettings.ALGO != null) {
            //kpmSettings.ALGO.cancel();
        }
        this.listener.runCancelled("Not known.", kpmSettings.RunID);
    }

}

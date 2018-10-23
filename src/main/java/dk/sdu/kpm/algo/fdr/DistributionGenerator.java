package dk.sdu.kpm.algo.fdr;

import dk.sdu.kpm.KPMSettings;
import dk.sdu.kpm.graph.KPMGraph;
import dk.sdu.kpm.graph.Result;
import dk.sdu.kpm.taskmonitors.KPMDummyTaskMonitor;
import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.interpolation.*;
import org.apache.commons.math3.stat.Frequency;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.ResultSet;
import java.util.*;

public class DistributionGenerator {

    private KPMGraph kpmGraph;

    public HashMap<Integer, double[]> getDistribution() {
        return distribution;
    }

    public HashMap<Integer, double[]> getPdist() {
        return pdist;
    }

    private HashMap<Integer, double[]> distribution;
    private HashMap<Integer, double[]> pdist;
    private int nrSamples;
    private int sizeOfLargest;
    //legacy paramter was to lazy to refactor code. Network growth always start growing from size 1,
    // does not make sense to start distribution at a higher number.
    private int sizeOfSmallest;
    private double fdr;
    protected double[] thresholds;
    protected double[] meanTeststats;
    Integer[] sizes;
    KPMSettings kpmSettings;

    public DistributionGenerator(KPMGraph kpmGraph, int nrSamples, int sizeOfLargest,
                                 KPMSettings kpmSettings) {
        this.kpmGraph = kpmGraph;
        this.nrSamples = nrSamples;
        this.sizeOfLargest = sizeOfLargest;
        this.sizeOfSmallest = 1;
        this.distribution = new HashMap<Integer, double[]>();
        this.pdist = new HashMap<Integer, double[]>();
        this.kpmSettings = kpmSettings;
        this.fdr = kpmSettings.FDR_CUTOFF;
    }

    public double[] getMeanTeststats() {
        return meanTeststats;
    }

    public void createBackgroundDistribution(String filename, boolean general) {
        int running = sizeOfLargest - sizeOfSmallest;
        int j = sizeOfSmallest;
        int increment = 1;
        int counter = 0;

        while (j <= running + sizeOfSmallest) {
            this.distribution.put(j, new double[this.nrSamples]);
            this.pdist.put(j, new double[this.nrSamples]);

            List<Result> res;
            if (kpmSettings.BACKGROUND.equals("greedy")) {
                FDRGreedy greedyN = new FDRGreedy(kpmGraph, new KPMDummyTaskMonitor(), kpmSettings, general, j, nrSamples);
                res = greedyN.runGreedy();
            } else {
                res = new ArrayList<Result>();
                for (int k = 0; k < nrSamples; k++) {
                    RandomSubgraph rs = new RandomSubgraph(this.kpmSettings.MAIN_GRAPH, j, false, filename, this.kpmSettings);
                    rs.calculateNetworkScore(this.kpmSettings.AGGREGATION_METHOD, kpmGraph);
                    res.add(rs);
                }
            }
            int i = 0;
            for (Result rs : res) {
                RandomSubgraph r = (RandomSubgraph) rs;
                ((RandomSubgraph) rs).writeToFile(filename + "pvalsSamplewise.txt", filename + "nodeDist.txt", filename + "pvalsGeneral.txt");
                distribution.get(j)[i] = r.getGeneralTeststat();
                pdist.get(j)[i] = r.getGeneralPval();
                i++;
            }
            for (int k = i; k < distribution.get(j).length; k++) {
                distribution.get(j)[k] = distribution.get(j)[kpmSettings.R.nextInt(i)];
                pdist.get(j)[k] = pdist.get(j)[kpmSettings.R.nextInt(i)];
            }
            increment = stepSize(j);
            j += increment;

        }

        for (int i : this.distribution.keySet()) {
            Arrays.sort(distribution.get(i));

        }

        for (int i : this.pdist.keySet()) {
            Arrays.sort(pdist.get(i));

        }
        allThresholds();
    }

    /*
    This function determines the increment for the size of the network that is to be sampled.
    Networks up to size 30 are all sampled, then the size is incremented by 5 up till networks of size 100;
    After Size 100 the increment is set to 10
     */
    public static int stepSize(int j) {
        int stepSize = 1;
        if (j <= 30) {
            stepSize = 1;
        } else if (j <= 100) {
            stepSize = 5;
        } else if (j <= 150) {
            stepSize = 10;
        } else if (j <= 200) {
            stepSize = 25;
        } else {
            stepSize = 100;
        }
        return stepSize;
    }

    public static int stepSize() {
        return 1;
    }


    public void writeDistributionToFile(String filename, HashMap<Integer, double[]> distribution) {
        // use append to write different distributions to the same file
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(filename))) {
            //bw.write(nrSamples+"\t"+distribution.length+"\t");
            int counter = 0;
            for (int j : distribution.keySet()) {
                int size = this.sizeOfSmallest + counter;
                bw.write(size + "\t");
                counter += stepSize(j);
                for (int i = 0; i < distribution.get(j).length; i++) {
                    bw.write(distribution.get(j)[i] + "\t");
                }
                bw.write("\n");
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    protected double getThreshold(int networkSize) {
        // Arrays are already sorted
        if (networkSize <= thresholds.length) {
            return thresholds[networkSize - 1];
        } else {
            return 1.0;
        }
    }

    protected double getMeanTS(int networkSize) {
        // Arrays are already sorted
        if (networkSize <= meanTeststats.length) {
            return meanTeststats[networkSize - 1];
        } else {
            return 0.0;
        }
    }


    private void allThresholds() {
        meanTeststats = new double[this.distribution.size()];
        double[] thresholds = new double[this.pdist.size()];
        // index = % of all random networks
        int index = (int) Math.floor(this.fdr * nrSamples);
        int counter = 0;
        Integer[] indices = this.pdist.keySet().toArray(new Integer[this.pdist.size()]);
        Arrays.sort(indices);
        for (Integer i : indices) {
            double thresh = this.pdist.get(i)[index];
            double t = this.distribution.get(i)[index];
            thresholds[counter] = thresh;
            meanTeststats[counter] = t;
            counter++;
        }

        this.thresholds = thresholds;
        this.sizes = indices;
        interpolateThreshold();

    }

    public void interpolateThreshold() {
        double[] result = new double[sizes[sizes.length - 1] - 1];
        double[] resultT = new double[sizes[sizes.length - 1] - 1];
        double[] resultP = new double[sizes[sizes.length - 1] - 1];

        double[] x = new double[thresholds.length];
        for (int i = 0; i < x.length; i++) {
            x[i] = (double) this.sizes[i].intValue();

        }
        int counter = 0;
        for (int i = 0; i < sizes.length - 1; i++) {
            int distance = sizes[i + 1] - sizes[i];
            if (distance == 1) {
                result[counter] = this.thresholds[i];
                resultT[counter] = meanTeststats[i];
                counter++;
            } else {
                int tmp = 1;
                while (distance >= tmp) {
                    result[counter] = ((distance - tmp * 1.0) / distance * 1.0) * this.thresholds[i] + (tmp * 1.0 / distance * 1.0) * this.thresholds[i + 1];
                    resultT[counter] = ((distance - tmp * 1.0) / distance * 1.0) * this.meanTeststats[i] + (tmp * 1.0 / distance * 1.0) * this.meanTeststats[i + 1];
                    tmp++;
                    counter++;
                }
            }
        }

        this.thresholds = result;
        this.meanTeststats = resultT;
    }

    public double[] getThresholds() {
        return this.thresholds;
    }

    public Integer[] getSizes() {
        return this.sizes;
    }

}

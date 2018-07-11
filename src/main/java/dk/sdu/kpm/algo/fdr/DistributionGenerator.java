package dk.sdu.kpm.algo.fdr;

import dk.sdu.kpm.graph.KPMGraph;
import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.interpolation.SplineInterpolator;
import org.apache.commons.math3.analysis.interpolation.UnivariateInterpolator;
import org.apache.commons.math3.stat.Frequency;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class DistributionGenerator {

    private KPMGraph kpmGraph;

    public HashMap<Integer,double[]> getDistribution() {
        return distribution;
    }

    public HashMap<Integer,double[]> getPdist() {
        return pdist;
    }

    private HashMap<Integer,double[]> distribution;
    private HashMap<Integer,double[]> pdist;
    private int nrSamples;
    private int sizeOfLargest;
    private int sizeOfSmallest;
    private boolean includeBackground = true;
    private double fdr = 0.05;
    protected double[] thresholds;
    Integer[] sizes;

    public DistributionGenerator(KPMGraph kpmGraph, int nrSamples, int sizeOfSmallest, int sizeOfLargest, boolean includeBackground){
        this.kpmGraph = kpmGraph;
        this.nrSamples = nrSamples;
        this.sizeOfLargest = sizeOfLargest;
        this.sizeOfSmallest = sizeOfSmallest;
        this.distribution = new HashMap<Integer, double[]>();
        this.pdist = new HashMap<Integer,double[]>();
        this.includeBackground = includeBackground;

    }

    public void createBackgroundDistribution(){
        int running = sizeOfLargest-sizeOfSmallest;
        int j = sizeOfSmallest;
        int increment = 1;
        int counter = 0;
        while(j<=running+sizeOfSmallest){
            this.distribution.put(j, new double[this.nrSamples]);
            this.pdist.put(j,new double[this.nrSamples]);
        for(int i = 0; i<this.nrSamples; i++){
            RandomSubgraph rs = new RandomSubgraph(this.kpmGraph, j, this.includeBackground );
            distribution.get(j)[i] = rs.getTestStatistics();
            pdist.get(j)[i] = rs.getPval();
            increment = stepSize(j);
            //increment = stepSize();

        }
        j+=increment;
        counter++;
        }
        for(int i :this.distribution.keySet()){
            Arrays.sort(distribution.get(i));

        }
        for(int i :this.pdist.keySet()){
            Arrays.sort(pdist.get(i));

        }
        allThresholds();
    }

    /*
    This function determines the increment for the size of the network that is to be sampled.
    Networks up to size 30 are all sampled, then the size is incremented by 5 up till networks of size 100;
    After Size 100 the increment is set to 10
     */
    public static int stepSize(int j){
        int stepSize = 1;
        if(j<=30){
            stepSize = 1;
        }
        else if(j <= 100){
            stepSize = 5;
        }
        else{
            stepSize = 10;
        }
        return stepSize;
    }
    public static int stepSize(){
        return 1;
    }


    public void writeDistributionToFile(String filename, HashMap<Integer,double[]> distribution){
        // use append to write different distributions to the same file
        try(BufferedWriter bw = new BufferedWriter(new FileWriter(filename))){
            //bw.write(nrSamples+"\t"+distribution.length+"\t");
            int counter = 0;
            for (int j : distribution.keySet()) {
                int size = this.sizeOfSmallest+counter;
                bw.write(size+"\t");
                counter+=stepSize(j);
                for (int i = 0; i < distribution.get(j).length; i++) {
                    bw.write(distribution.get(j)[i] + "\t");
                }
                bw.write("\n");
            }
        }
        catch (IOException ioe){
            ioe.printStackTrace();
        }
    }
    protected double getThreshold(int networkSize){
        // Arrays are already sorted
        if(networkSize<=thresholds.length) {
            return thresholds[networkSize-1];
        }
        else{
            return 1.0;
        }
    }


    private void allThresholds(){
        double[] thresholds = new double[this.pdist.size()];
        // index = % of all random networks
        int index = (int)Math.floor(this.fdr*nrSamples);
        int counter = 0;
        Integer[] indices = this.pdist.keySet().toArray(new Integer[this.pdist.size()]);
        Arrays.sort(indices);
        for(Integer i : indices){
            double thresh = this.pdist.get(i)[index];
            thresholds[counter] = thresh;
            counter ++;
        }

            this.thresholds = thresholds;
            this.sizes = indices;
            interpolateThreshold();

    }

    public void interpolateThreshold(){
        double[] result = new double[sizes[sizes.length-1]];
        double[] x = new double[thresholds.length];
        for(int i = 0; i<x.length; i++){
            x[i]=(double)this.sizes[i].intValue();

        }
        UnivariateInterpolator interpolator = new SplineInterpolator();
        UnivariateFunction function = interpolator.interpolate(x, thresholds);

        double last = 1.0;
        for (int i = 0; i<x.length; i++) {
            while(x[i]-last>0) {
                double interpolatedY = function.value(last);
                result[(int)last-1] = interpolatedY;
                last++;
            }
            result[(int)last-1] = thresholds[i];
            last = last+1.0;
        }
        this.thresholds = result;
    }
    public double[] getThresholds(){
        return this.thresholds;
    }
    public Integer[] getSizes(){
        return this.sizes;
    }

}

package dk.sdu.kpm.algo.fdr;

import dk.sdu.kpm.graph.KPMGraph;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

public class DistributionGenerator {

    private KPMGraph kpmGraph;

    public ArrayList<double[]> getDistribution() {
        return distribution;
    }

    public ArrayList<double[]> getPdist() {
        return pdist;
    }

    private ArrayList<double[]> distribution;
    private ArrayList<double []> pdist;
    private int nrSamples;
    private int sizeOfLargest;
    private int sizeOfSmallest;
    private boolean includeBackground = true;

    public DistributionGenerator(KPMGraph kpmGraph, int nrSamples, int sizeOfSmallest, int sizeOfLargest, boolean includeBackground){
        this.kpmGraph = kpmGraph;
        this.nrSamples = nrSamples;
        this.sizeOfLargest = sizeOfLargest;
        this.sizeOfSmallest = sizeOfSmallest;
        this.distribution = new ArrayList<double[]>();
        this.pdist = new ArrayList<double[]>();
        this.includeBackground = includeBackground;

    }

    public void createBackgroundDistribution(){
        int running = sizeOfLargest-sizeOfSmallest;
        int j = sizeOfSmallest;
        int increment = 1;
        int counter = 0;
        while(j<=running+sizeOfSmallest){
            this.distribution.add(new double[this.nrSamples]);
            this.pdist.add(new double[this.nrSamples]);
        for(int i = 0; i<this.nrSamples; i++){
            RandomSubgraph rs = new RandomSubgraph(this.kpmGraph, j, this.includeBackground );
            distribution.get(counter)[i] = rs.getTestStatistics();
            pdist.get(counter)[i] = rs.getPval();
            increment = stepSize(j);

        }
        j+=increment;
        counter++;
        }
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


    public void writeDistributionToFile(String filename, ArrayList<double[]> distribution){
        // use append to write different distributions to the same file
        try(BufferedWriter bw = new BufferedWriter(new FileWriter(filename))){
            //bw.write(nrSamples+"\t"+distribution.length+"\t");
            int counter = 0;
            for (int j = 0; j< distribution.size(); j++) {
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



}

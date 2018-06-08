package dk.sdu.kpm.algo.fdr;

import dk.sdu.kpm.graph.KPMGraph;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;

public class DistributionGenerator {

    private KPMGraph kpmGraph;
    private double[][] distribution;
    private int nrSamples;
    private int sizeOfLargest;
    private int sizeOfSmallest;

    public DistributionGenerator(KPMGraph kpmGraph, int nrSamples, int sizeOfSmallest, int sizeOfLargest){
        this.kpmGraph = kpmGraph;
        this.nrSamples = nrSamples;
        this.sizeOfLargest = sizeOfLargest;
        this.sizeOfSmallest = sizeOfSmallest;

    }

    public void createBackgroundDistribution(){
        int running = sizeOfLargest-sizeOfSmallest;
        this.distribution = new double[running+1][this.nrSamples];
        for (int j = 0; j<=running; j++){
        for(int i = 0; i<this.nrSamples; i++){
            RandomSubgraph rs = new RandomSubgraph(this.kpmGraph, j );
            distribution[j][i] = rs.getTestStatistics();
        }
    }
    }


    public void writeDistributionToFile(String filename){
        // use append to write different distributions to the same file
        try(BufferedWriter bw = new BufferedWriter(new FileWriter(filename))){
            //bw.write(nrSamples+"\t"+distribution.length+"\t");
            int counter = 0;
            for (int j = 0; j< distribution.length; j++) {
                int size = sizeOfSmallest+counter;
                bw.write(size+"\t");
                counter++;
                for (int i = 0; i < distribution[j].length; i++) {
                    bw.write(distribution[j][i] + "\t");
                }
                bw.write("\n");
            }
        }
        catch (IOException ioe){
            ioe.printStackTrace();
        }
    }



}

package dk.sdu.kpm.algo.fdr;

import dk.sdu.kpm.graph.KPMGraph;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;

public class DistributionGenerator {

    private KPMGraph kpmGraph;
    private double[] distribution;

    public DistributionGenerator(KPMGraph kpmGraph){
        this.kpmGraph = kpmGraph;

    }

    private void createBackgroundDistribution(int nrSamples, int sizeOfNetwork){
        this.distribution = new double[nrSamples];
        for(int i = 0; i<nrSamples; i++){
            RandomSubgraph rs = new RandomSubgraph(this.kpmGraph, sizeOfNetwork);
            distribution[i] = rs.getTestStatistics();
        }
    }

    public void writeDistributionToFile(String filename, int nrSamples){
        // use append to write different distributions to the same file
        try(BufferedWriter bw = new BufferedWriter(new FileWriter(filename, true))){
            bw.write(nrSamples+"\t"+distribution.length+"\t");
            for (int i = 0; i<distribution.length; i++){
                bw.write(distribution[i]+"\t");
            }
            bw.write("\n");
        }
        catch (IOException ioe){
            ioe.printStackTrace();
        }
    }
}

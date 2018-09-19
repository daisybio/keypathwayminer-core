package dk.sdu.kpm.algo.fdr;

import dk.sdu.kpm.KPMSettings;
import dk.sdu.kpm.graph.KPMGraph;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

public class Testcase {

    KPMGraph kpmGraph;
    KPMSettings kpmSettings;


    public Testcase(KPMGraph kpmGraph, KPMSettings kpmSettings){
        this.kpmGraph = kpmGraph;
        this.kpmSettings = kpmSettings;

    }

    public void createTestcases(){
        Random r = new Random(1235);
        ArrayList<RandomSubgraph> al = new ArrayList<RandomSubgraph>();
        for(int i =0; i<20; i++) {
            int j = r.nextInt(100);
       // int j = 5000;
            boolean overlap = false;
            RandomSubgraph rs = new RandomSubgraph(kpmGraph, j, false, "/home/anne/Masterarbeit/Test_pipeline/sample_networks/StringDB/"+j , this.kpmSettings);
            for(RandomSubgraph rr: al){
                if(rs.getVertices().containsAll(rr.getVertices())){
                    i--;

                    break;
                }
            }
            if(overlap){
                continue;
            }
            else {
                al.add(rs);
                rs.writeGraphToFile("/home/anne/Masterarbeit/Test_pipeline/sample_networks/StringDB" +
                        "/" + j, j + "", false);
            }
        }
    }

    public void createRandomDistribution(){
        double[] s = new double[250];
        for(int i = 0; i<250; i++){
            double stats = 0.0;
            for(int j = 0; j<=25; j++){
                RandomSubgraph rs = new RandomSubgraph(kpmGraph, i, false, "/home/anne/Masterarbeit/Testing/distribution/"+i+j, this.kpmSettings);
                rs.calculateNetworkScore("sum");
                stats+=rs.getGeneralTeststat();
            }
            stats = stats/25.0;
            s[i] = stats;
        }
        try(BufferedWriter bw = new BufferedWriter(new FileWriter("/home/anne/Masterarbeit/Testing/distribution/random_scores.txt"))){
            for(double s1: s){
                bw.write(s1+"\n");
            }
        }
        catch (IOException e){
            e.printStackTrace();
        }

    }
}

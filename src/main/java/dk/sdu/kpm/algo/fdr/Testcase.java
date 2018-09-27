package dk.sdu.kpm.algo.fdr;

import dk.sdu.kpm.KPMSettings;
import dk.sdu.kpm.graph.GeneNode;
import dk.sdu.kpm.graph.KPMGraph;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Random;

public class Testcase {

    KPMGraph kpmGraph;
    KPMSettings kpmSettings;
    String directory;


    public Testcase(KPMGraph kpmGraph, KPMSettings kpmSettings, String directory){
        this.kpmGraph = kpmGraph;
        this.kpmSettings = kpmSettings;
        this.directory = directory;
    }

    public void createTestcases(){
        Random r = kpmSettings.R;
        ArrayList<RandomSubgraph> al = new ArrayList<RandomSubgraph>();
        // Make sure that no network size is drawn twice.
        ArrayList<Integer> visited = new ArrayList<Integer>();
        for(int i =0; i<20; i++) {
            int j = r.nextInt(100);
            if(visited.contains(j)){
                i--;
                continue;
            }
            visited.add(j);
       // int j = 5000;
            boolean overlap = false;
            try {
                Files.createDirectories(Paths.get(directory));
            } catch (IOException e) {
                e.printStackTrace();
            }
            RandomSubgraph rs = new RandomSubgraph(kpmGraph, j, false, directory+j , this.kpmSettings);
            for(RandomSubgraph rr: al){
                boolean cond = false;
                for(GeneNode n: rr.getVertices()) {
                    if (rs.getVertices().contains(n)) {
                        i--;
                        cond = true;
                        overlap=true;
                        break;
                    }
                    if(cond){
                        break;
                    }
                }
            }
            if(overlap){
                continue;
            }
            else {
                al.add(rs);
                rs.writeGraphToFile(directory +
                        File.separator + j, j + "", false);
            }
        }
    }

    public void createRandomDistribution(){
        double[] s = new double[250];
        for(int i = 0; i<250; i++){
            double stats = 0.0;
            for(int j = 0; j<=25; j++){
                RandomSubgraph rs = new RandomSubgraph(kpmGraph, i, false, "/home/anne/Masterarbeit/Testing/distribution/"+i+j, this.kpmSettings);
                // TODO: dummy
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

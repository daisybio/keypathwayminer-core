package dk.sdu.kpm.algo.fdr;

import dk.sdu.kpm.KPMSettings;
import dk.sdu.kpm.graph.KPMGraph;

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
            RandomSubgraph rs = new RandomSubgraph(kpmGraph, j, false, "/home/anne/Masterarbeit/Testing/sampleGraphs2/"+j , this.kpmSettings);
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
                rs.writeGraphToFile("/home/anne/Masterarbeit/Testing/sampleGraphs2/" + j, j + "", false);
            }
        }
    }
}

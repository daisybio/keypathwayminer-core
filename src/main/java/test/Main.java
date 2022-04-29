package test;

import dk.sdu.kpm.Algo;
import dk.sdu.kpm.KPMSettings;
import dk.sdu.kpm.charts.*;
import dk.sdu.kpm.graph.KPMGraph;
import dk.sdu.kpm.results.IKPMResultItem;
import dk.sdu.kpm.results.IKPMResultSet;
import dk.sdu.kpm.runners.BatchRunner;
import dk.sdu.kpm.taskmonitors.KPMDummyTaskMonitor;
import dk.sdu.kpm.results.IKPMRunListener;

import javax.swing.*;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class Main {
    private static final String pathToResources = "KPM-5/resources";

	public static void main(String[] args) {
        final KPMSettings kpmSettings = new KPMSettings();
        // Change this to run the different algorithms
        kpmSettings.ALGO = Algo.LCG;
        // Change the L parameter here
        kpmSettings.CASE_EXCEPTIONS_MAP.put("L1", 15);
        // Change the K parameter here
        kpmSettings.GENE_EXCEPTIONS = 5;
        kpmSettings.MATRIX_FILES_MAP.put("L1", pathToResources +
                "/datasets/indicator-matrices/huntington-gene-expression-down-p005.txt");
        kpmSettings.SEED = 1234;
        kpmSettings.R = new Random(kpmSettings.SEED);
        kpmSettings.MAIN_GRAPH = createGraph(kpmSettings, pathToResources + "/sampleNetwork.sif");
        kpmSettings.COMBINE_FORMULA = "L1 || L2";
        kpmSettings.ITERATION_BASED = false;
        System.out.println("NUMBER OF NODES: " + kpmSettings.MAIN_GRAPH.getVertexCount());
        System.out.println("NUMBER OF INTERACTIONS: " + kpmSettings.MAIN_GRAPH.getEdgeCount());
        System.out.println("AVERAGE NODE DEGREE: " + kpmSettings.MAIN_GRAPH.getAverageDegree());

        KPMDummyTaskMonitor dummyTaskMonitor = new KPMDummyTaskMonitor();
        IKPMRunListener listener = new IKPMRunListener() {
            @Override
            public void runFinished(IKPMResultSet results) {
                System.out.println("The run was finished!");
                printSummary(results, kpmSettings);
            }

            @Override
            public void runCancelled(String reason, String runID) {
                System.out.println("The run was cancelled. \n" + reason);
            }
        };

        BatchRunner batcher = new BatchRunner("core", dummyTaskMonitor, listener, kpmSettings);
        batcher.run();
//        try {
//            displayKPMXY();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        ValidationOverlapResultItem i1 = new ValidationOverlapResultItem("123412", new GeneNode("123", "entrez", new HashMap<String, int[]>()));
//        ArrayList<ValidationOverlapResultItem> list = new ArrayList<ValidationOverlapResultItem>();
//        list.add(i1);
//
//        KPMSettings settings = new KPMSettings();
//        settings.ValidationOverlapResults.add(new ValidationOverlapResult(1, 1, list, 10));
//        KPMSettings test = new DeepCopy<KPMSettings>().copy(settings);
//
//        System.out.println(test.ValidationOverlapResults.size());
//        test.ValidationOverlapResults.get(0).K = 10;
//        test.ValidationOverlapResults.get(0).overlapResultItems.get(0).geneNodeID = "424242";
//
//
//        System.out.println(settings.ValidationOverlapResults.get(0).K);
//        System.out.println(settings.ValidationOverlapResults.get(0).overlapResultItems.get(0).geneNodeID);

//        try {
//            //displayKPMXY();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

//        System.out.println("Trying to generate classes, see if something breaks...");
//		KPMSettings settings = new KPMSettings();
//		settings.CASE_EXCEPTIONS_MAP = new HashMap<String, Integer>();
//		settings.CASE_EXCEPTIONS_MAP.put("Something", 42);
//		if(settings.CASE_EXCEPTIONS_MAP.containsKey("Something")){
//			System.out.println("Found 42. :)");
//		}else{
//			System.out.println("Did not find 42. :(");
//		}
//
//        try{
//            // will fail
//           int res = settings.CASE_EXCEPTIONS_MAP.get("24");
//        }catch (Exception e){
//            KpmLogger.log(Level.SEVERE, e);
//        }
//
//		System.out.println("\n\nDONE.");
	}
    private static void displayKPMXY() throws IOException{
        Dataset series = new Dataset("k=35");
        Dataset series2 = new Dataset("k=4");
        Dataset series3 = new Dataset("k=2");
        Dataset series4 = new Dataset("k=10");


        for (int i = 0; i <= 0; i++) {
            series.add(new DatasetEntry(i, i*1));
            series.add(new DatasetEntryWithVariance(i-0.3, Math.pow(2, i), Math.pow(2, i)-10, Math.pow(2, i)+10));
            series2.add(new DatasetEntryWithVariance(i-0.6, Math.pow(1.5, i), Math.pow(1.5, i)-50, Math.pow(1.5, i)+50));
            series3.add(new DatasetEntryWithVariance(i-0.7, Math.sin(i)+10, Math.sin(i)+10-25,Math.sin(i)+10+42));
            series4.add(new DatasetEntryWithVariance(i, Math.cos(i)+10, Math.cos(i)+10-25,Math.cos(i)+10+42));
        }

        XYMultiChart chart = new XYMultiChart("Test-Title", "X-axis title", "Y-axis title", true);
        //chart.addDataset(series);
        chart.addDataset(series2);
//        chart.addDataset(series3);
//        chart.addDataset(series4);

        JFrame frame = new JFrame("Test XY Chart");
        frame.add(chart.getChartPanel());


        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private static void displayBoxPlot(){
        BoxplotChart chart = new BoxplotChart("Test-Title", "X-axis title", "Y-axis title");
        List<Double> series0 = new ArrayList<Double>();
        List<Double> series2 = new ArrayList<Double>();
        List<Double> valueList3 = new ArrayList<Double>();
        List<Double> valueList4 = new ArrayList<Double>();
        series0.add((double) 0);

        Random random = new Random(Calendar.getInstance().getTimeInMillis());
        for(int i = 10; i < 100; i ++){
            series0.add((double) i * (1 + random.nextInt(10)));
            series2.add((double) i * (1 + random.nextInt(10)));
            valueList3.add((double)i*(1+random.nextInt(10)));
            valueList4.add((double)i*(1+random.nextInt(10)));
        }

        chart.addDataset("", "0%", series0);
        chart.addDataset("", "2%", series2);
        chart.addDataset("", "4%", valueList3);
        chart.addDataset("", "6%", valueList4);

        File resultsFolder = new File("C:\\Users\\Martin\\Desktop");
        String absPath = "";
        if(resultsFolder.isDirectory()){
            absPath = resultsFolder.getAbsolutePath();
        }else{
            System.out.println("No path found, could not save charts.");
        }
        int counter = 1;
        String formattedChartTime = new SimpleDateFormat("HH.mm.ss").format(Calendar.getInstance().getTime());
        String chartFileName = String.format("%s\\box%d_%s.png", absPath, counter, formattedChartTime);

        System.out.println("Tyring to save: " + chartFileName);
        File chartsFile = new File(chartFileName);
        try {
            chart.saveAsPng(chartsFile, 800, 600);
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("EROROR");
        }


        JFrame frame = new JFrame("Test Boxplot Chart");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(chart.getChartPanel());
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        //DialogUtils.showNonModalDialog("Error", "Error", DialogUtils.MessageTypes.Error);
        //DialogUtils.showNonModalDialog("Info", "Info", DialogUtils.MessageTypes.Info);
        //DialogUtils.showNonModalDialog("Warn", "Warn", DialogUtils.MessageTypes.Warn);
    }

    public static KPMGraph createGraph(KPMSettings kpmSettings, String graphFile) {
        HashMap<String, String> nodeId2Symbol = new HashMap<String, String>();
        Map<String, Map<String, int[]>> expressionMap = new HashMap<String, Map<String, int[]>>();
        LinkedList<String[]> edgeList = new LinkedList<String[]>();
        HashMap<String, Integer> without_exp = new HashMap<String, Integer>();
        HashSet<String> inNetwork = new HashSet<String>();
        HashMap<String, String> expressionFiles = new HashMap<>(kpmSettings.MATRIX_FILES_MAP);
        HashMap<String, Integer> numCasesMap = new HashMap<>();
        HashMap<String, Integer> numGenesMap = new HashMap<>();
        HashMap<String, Double> avgExpressedCasesMap = new HashMap<>();
        HashMap<String, Double> avgExpressedGenesMap = new HashMap<>();
        HashMap<String, Integer> totalExpressedMap = new HashMap<>();
        HashMap<String, Set<String>> backNodesMap = new HashMap<>();
        HashMap<String, Set<String>> backNodesByExpMap = new HashMap<>();
        HashMap<String, Set<String>> backGenesMap = new HashMap<>();

        for (String fileId : expressionFiles.keySet()) {
            numCasesMap.put(fileId, 0);
            without_exp.put(fileId, 0);
        }
        try {

            String line = "";
            BufferedReader graphReader = new BufferedReader(new FileReader(graphFile));
            int cont = 0;
            while ((line = graphReader.readLine()) != null) {
                String[] fields = line.split("\t");
                String id1 = fields[0].trim();
                nodeId2Symbol.put(id1, id1);
                cont++;
                if (fields.length < 3) {
                    System.out.println("LINE NUMBER:" + cont);
                    System.out.print(line);
                }
                String id2 = fields[2].trim();
                nodeId2Symbol.put(id2, id2);

                String[] edge = {id1, id2};
                edgeList.add(edge);
                inNetwork.add(id1);
                inNetwork.add(id2);
            }
            for (String fileId : expressionFiles.keySet()) {
                int totalExp = 0;
                int numCases = 0;
                int numGenes = 0;
                HashMap<String, int[]> nodeId2Expression = new HashMap<String, int[]>();
                Set<String> inExp = new HashSet<String>();

                BufferedReader expressionReader =
                        new BufferedReader(new FileReader(expressionFiles.get(fileId)));



                while ((line = expressionReader.readLine()) != null) {
                    numGenes++;
                    String[] fields = line.split("\t");
                    String nodeId = fields[0].trim();
                    inExp.add(nodeId);

                    int[] exp = new int[fields.length - 1];

                    for (int i = 1; i < fields.length; i++) {
                        String val = fields[i].trim();
                        if (val.equals("1")) {
                            exp[i - 1] = 1;
                            totalExp++;
                        } else if (val.equals("-1")) {
                            exp[i - 1] = -1;
                            totalExp++;
                        } else if (val.equals("0")) {
                            exp[i - 1] = 0;
                        } else {
                            exp[i - 1] = 0;
                        }
                    }

                    if (expressionMap.containsKey(nodeId)) {
                        expressionMap.get(nodeId).put(fileId, exp);
                    } else {
                        Map<String, int[]> aux = new HashMap<String, int[]>();
                        aux.put(fileId, exp);
                        expressionMap.put(nodeId, aux);
                    }
                    numCases = exp.length;
                    numCasesMap.put(fileId, numCases);
                }
                totalExpressedMap.put(fileId, totalExp);
                double avgExpCases = 0;
                double avgExpGenes = 0;
                if (totalExp > 0) {
                    avgExpCases = (double)numCases / (double)totalExp;
                    avgExpGenes = (double)numGenes / (double)totalExp;
                }
                numGenesMap.put(fileId, inExp.size());
                avgExpressedCasesMap.put(fileId, avgExpCases);
                avgExpressedGenesMap.put(fileId, avgExpGenes);
                Set<String> bckN = new HashSet(inNetwork);
                Set<String> bckG = new HashSet(inExp);
                for (String id: inNetwork) {
                    if (inExp.contains(id)) {
                        bckN.remove(id);
                    }
                }
                for (String id: inExp) {
                    if (inNetwork.contains(id)) {
                        bckG.remove(id);
                    }
                }

                backNodesByExpMap.put(fileId, bckN);
                backGenesMap.put(fileId, bckG);
                expressionReader.close();
            }



            graphReader.close();


        } catch (FileNotFoundException ex) {
            //Logger.getLogger(Parser.class.getName()).log(Level.SEVERE, null, ex);
            ex.printStackTrace();
        } catch (IOException ioe) {
            //Logger.getLogger(Parser.class.getName()).log(Level.SEVERE, null, ioe);
            ioe.printStackTrace();
        }

        for (String nodeId : inNetwork) {
            if (expressionMap.containsKey(nodeId)) {
                Map<String, int[]> expMap = expressionMap.get(nodeId);
                for (String expId : expressionFiles.keySet()) {
                    if (!expMap.containsKey(expId)) {
                        if (backNodesMap.containsKey(nodeId)) {
                            backNodesMap.get(nodeId).add(expId);
                        } else {
                            HashSet<String> aux = new HashSet<String>();
                            aux.add(expId);
                            backNodesMap.put(nodeId, aux);
                        }

                    }
                }
            } else {
                if (backNodesMap.containsKey(nodeId)) {
                    backNodesMap.get(nodeId).addAll(expressionFiles.keySet());
                } else {
                    HashSet<String> aux = new HashSet<String>();
                    aux.addAll(expressionFiles.keySet());
                    backNodesMap.put(nodeId, aux);
                }
            }
        }

        kpmSettings.NUM_CASES_MAP = numCasesMap;
        kpmSettings.NUM_STUDIES = numCasesMap.size();
        return new KPMGraph(expressionMap, edgeList, nodeId2Symbol, backNodesMap, backGenesMap, kpmSettings.NUM_CASES_MAP);
    }

    public static void printSummary(IKPMResultSet results, KPMSettings kpmSettings) {
        boolean isInes = kpmSettings.ALGO == Algo.GREEDY || kpmSettings.ALGO == Algo.LCG
                || kpmSettings.ALGO == Algo.OPTIMAL;

	    System.out.println("GENE EXCEPTIONS: " + kpmSettings.GENE_EXCEPTIONS);
        System.out.println("CASE EXCEPTIONS" + "\t" + "MATRIX");

        for (String expId : kpmSettings.CASE_EXCEPTIONS_MAP.keySet()) {
            System.out.println("Case exceptions: " + kpmSettings.CASE_EXCEPTIONS_MAP.get(expId)
                    + "\tMatrix: " + kpmSettings.MATRIX_FILES_MAP.get(expId));
        }

        if (kpmSettings.ALGO == Algo.GREEDY) {
            System.out.println("STRATEGY: " + "\t" + "INES");
            System.out.println("ALGORITHM: " + "\t" + "GREEDY");
        } else if (kpmSettings.ALGO == Algo.LCG) {
            System.out.println("STRATEGY: " + "\t" + "INES");
            System.out.println("ALGORITHM: " + "\t" + "ACO");
        } else if (kpmSettings.ALGO == Algo.OPTIMAL) {
            System.out.println("STRATEGY: " + "\t" + "INES");
            System.out.println("ALGORITHM: " + "\t" + "OPTIMAL");
        } else if (kpmSettings.ALGO == Algo.EXCEPTIONSUMGREEDY) {
            System.out.println("STRATEGY: " + "\t" + "GLONE");
            System.out.println("ALGORITHM: " + "\t" + "GREEDY");
        } else if (kpmSettings.ALGO == Algo.EXCEPTIONSUMACO) {
            System.out.println("STRATEGY: " + "\t" + "GLONE");
            System.out.println("ALGORITHM: " + "\t" + "ACO");
        } else if (kpmSettings.ALGO == Algo.EXCEPTIONSUMOPTIMAL) {
            System.out.println("STRATEGY: " + "\t" + "GLONE");
            System.out.println("ALGORITHM: " + "\t" + "OPTIMAL");
        }
        IKPMResultItem best = results.getResults().get(0);
        System.out.println("RANK: " + "\t" + best.getResultsInfoTable()[0][0]);
        System.out.println("#NODES BEST RESULT: " + "\t" + best.getResultsInfoTable()[0][1]);
        System.out.println("#EDGES BEST RESULT: " + "\t" + best.getResultsInfoTable()[0][2]);
        System.out.println("AVG. EXP. BEST RESULT: " + "\t" + best.getResultsInfoTable()[0][3]);
        System.out.println("INFO. CONTENT BEST RESULT: " + "\t" + best.getResultsInfoTable()[0][4]);
        System.out.println("TOTAL RUNNING TIME: " + "\t" + kpmSettings.TOTAL_RUNNING_TIME);

        validateResult(best, kpmSettings);

    }

    private static void validateResult(IKPMResultItem best, KPMSettings kpmSettings){
        if (kpmSettings.ALGO == Algo.LCG &&
                kpmSettings.SEED == 1234 &&
                kpmSettings.MATRIX_FILES_MAP.get("L1").equals(pathToResources + "/datasets/indicator-matrices/huntington-gene-expression-down-p005.txt") &&
                kpmSettings.CASE_EXCEPTIONS_MAP.get("L1") == 15 &&
                kpmSettings.GENE_EXCEPTIONS == 5
        ) {
            System.out.println("Validating ACO/INES results ... ");
            if((int) best.getResultsInfoTable()[0][0] != 1) {
                throw new AssertionError("Rank is supposed to be 1 instead of " +
                        (int) best.getResultsInfoTable()[0][0]);
            }
            if((int) best.getResultsInfoTable()[0][1] != 126){
                throw new AssertionError("# Nodes in best result is supposed to be 126 instead of " +
                        (int) best.getResultsInfoTable()[0][1]);
            }
            if((int) best.getResultsInfoTable()[0][2] != 149){
                throw new AssertionError("# Edges in best result is supposed to be 149 instead of " +
                        (int) best.getResultsInfoTable()[0][2]);
            }
            if((double) best.getResultsInfoTable()[0][3] != 93.39){
                throw new AssertionError("Avg. exp. of best result is supposed to be 93.39 instead of " +
                        (double) best.getResultsInfoTable()[0][3]);
            }
            if((double) best.getResultsInfoTable()[0][4] != 0.28) {
                throw new AssertionError("Info. Content of best results is supposed to be 0.28 instead of " +
                        (double) best.getResultsInfoTable()[0][4]);
            }
            System.out.println("All good!");
        }else if (kpmSettings.ALGO == Algo.GREEDY &&
                kpmSettings.SEED == 1234 &&
                kpmSettings.MATRIX_FILES_MAP.get("L1").equals(pathToResources + "/datasets/indicator-matrices/huntington-gene-expression-down-p005.txt") &&
                kpmSettings.CASE_EXCEPTIONS_MAP.get("L1") == 15 &&
                kpmSettings.GENE_EXCEPTIONS == 5
        ) {
            System.out.println("Validating GREEDY/INES results ... ");
            if((int) best.getResultsInfoTable()[0][0] != 1) {
                throw new AssertionError("Rank is supposed to be 1 instead of " +
                        (int) best.getResultsInfoTable()[0][0]);
            }
            if((int) best.getResultsInfoTable()[0][1] != 128){
                throw new AssertionError("# Nodes in best result is supposed to be 128 instead of " +
                        (int) best.getResultsInfoTable()[0][1]);
            }
            if((int) best.getResultsInfoTable()[0][2] != 156){
                throw new AssertionError("# Edges in best result is supposed to be 156 instead of " +
                        (int) best.getResultsInfoTable()[0][2]);
            }
            if((double) best.getResultsInfoTable()[0][3] != 94.79){
                throw new AssertionError("Avg. exp. of best result is supposed to be 94.79 instead of " +
                        (double) best.getResultsInfoTable()[0][3]);
            }
            if((double) best.getResultsInfoTable()[0][4] != 0.28) {
                throw new AssertionError("Info. Content of best results is supposed to be 0.28 instead of " +
                        (double) best.getResultsInfoTable()[0][4]);
            }
            System.out.println("All good!");
        }
    }


}
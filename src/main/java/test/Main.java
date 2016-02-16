package test;

import dk.sdu.kpm.charts.*;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

public class Main {
	public static void main(String[] args) {

        double original = 38;
        int L = 2;

        System.out.println("(int) Math.floor((original / (double)100) * L) = " + (int) Math.floor((original / (double)100) * L));
        System.out.println("(int) Math.floor((original / L) * (double)100) = " + (int) Math.floor((original / L)));
        System.out.println("(int) Math.floor(((double)100 / L) * original) = " + (int) Math.floor(((double)100 / L) * original));

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

}

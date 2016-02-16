package dk.sdu.kpm.perturbation;

import dk.sdu.kpm.logging.KpmLogger;
import dk.sdu.kpm.taskmonitors.IKPMTaskMonitor;
import dk.sdu.kpm.taskmonitors.KPMDummyTaskMonitor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.logging.Level;

/**
 * Created by Martin on 24-01-2015.
 */
public class ColumnWiseShufflePerturbation extends BasePerturbation<String> {
    public ColumnWiseShufflePerturbation(){
        super();

        this.nodeIDs = new ArrayList<String>();
    }

    @Override
    public String getDescription() {
        return "Permutation of graph by way of edge removal.";
    }

    @Override
    public String getName() {
        return "Edge-removal";
    }

    @Override
    public PerturbationTags getTag() {
        return PerturbationTags.ColumnWiseShuffle;
    }

    /***
     * Presumes no header in the dataset.
     * @param percentageToPermute
     * @param dataset
     * @param taskMonitor
     * @return
     */
    @Override
    public String execute(int percentageToPermute, String dataset, IKPMTaskMonitor taskMonitor){
        if(taskMonitor == null){
            taskMonitor = new KPMDummyTaskMonitor();
        }
        taskMonitor.setTitle(String.format("Permuting dataset, using '%s'.", this.getName()));
        taskMonitor.setStatusMessage("Permuting...");

        int[][] datasetMatrix = this.convertToMatrix(dataset);

        int nrColumnsToShuffle = (int) Math.ceil((datasetMatrix.length / 100) * percentageToPermute);

        this.initIndexRandomizer(datasetMatrix.length);

        while(nrColumnsToShuffle > 0){
            int columnIndex = getNextRandomIndex();

            int[] column = datasetMatrix[columnIndex];

            int amountOnes = 0;
            for(int i = 0; i < column.length; i++){
                if(column[i] == 1){
                    amountOnes++;
                }
            }

            if(amountOnes == 0){
                continue;
            }

            IndexRandomizer cellRandomizer = new IndexRandomizer(column.length);

            for(int i = 0; i < column.length; i++){
                datasetMatrix[columnIndex][i] = 0;
            }

            for(int i = 0; i < amountOnes; i++){
                int cellIndex = cellRandomizer.getNextRandomIndex();
                datasetMatrix[columnIndex][cellIndex] = 1;
            }

            nrColumnsToShuffle--;
        }

        String newDataset = this.convertToString(datasetMatrix);
        return newDataset;
    }

    private int[][] convertToMatrix(String dataset){
        BufferedReader expressionReader = new BufferedReader(new StringReader(dataset));
        ArrayList<int[]> columnsList = new ArrayList<int[]>();

        String line = "";
        try {
            while ((line = expressionReader.readLine()) != null) {
                String[] fields = line.split("\t");
                String nodeId = fields[0].trim();
                nodeIDs.add(nodeId);

                int[] column = new int[fields.length - 1];

                for (int i = 1; i < fields.length; i++) {
                    String val = fields[i].trim();
                    if (val.equals("1")) {
                        column[i - 1] = 1;
                    } else if (val.equals("-1")) {
                        column[i - 1] = -1;
                    } else {
                        column[i - 1] = 0;
                    }
                }
                columnsList.add(column);
            }
        } catch (IOException e) {
            KpmLogger.log(Level.SEVERE, e);
        }

        int[][] columns = new int[columnsList.size() - 1][];
        for(int i = 0; i < columnsList.size(); i++){
            columns[i] = columnsList.get(i);
        }

        return columns;
    }

    private ArrayList<String> nodeIDs;

    private String convertToString(int[][] datasetMatrix){
        String dataset = "";

        for(int i = 0; i < datasetMatrix.length; i++){
            dataset += nodeIDs.get(i);

            for(int j = 0; j < datasetMatrix[i].length; j++){
                dataset += "\t" + datasetMatrix[i][j];
            }

            dataset += System.lineSeparator();
        }

        return dataset;
    }

    @Override
    public String toString() {
        return this.getName();
    }
}

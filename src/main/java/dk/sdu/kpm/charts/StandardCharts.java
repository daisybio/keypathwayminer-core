package dk.sdu.kpm.charts;

import dk.sdu.kpm.graph.Result;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;

/**
 * Class for modeling charts after batch run
 *
 * @author Martin
 *
 */
public class StandardCharts implements Serializable {

    public static IChart K_vs_Nodes(List<ChartInput> chartInputList,
            boolean L_in_Percentage, boolean fixed_L) {
        XYMultiChart chart = new XYMultiChart("", "K", "# Nodes", true);

        chart.getTags().add(IChart.TagEnum.STANDARD);
        chart.getTags().add(IChart.TagEnum.K_VS_NODES);

        if (chartInputList == null) {
            return chart;
        }

        HashMap<Integer, Dataset> datasetMap = new HashMap<Integer, Dataset>();

        for (ChartInput chartInput : chartInputList) {
            int nrNodes = chartInput.Results.get(0).getFitness();
            Dataset set;
            if (datasetMap.containsKey(chartInput.VAR2)) {
                set = datasetMap.get(chartInput.VAR2);
            } else {

                if (fixed_L) {
                    set = new Dataset("L");
                } else if (L_in_Percentage) {
                    set = new Dataset(String.format("L=%d%%", chartInput.VAR2));
                } else {
                    set = new Dataset(String.format("L=%d", chartInput.VAR2));
                }
                datasetMap.put(chartInput.VAR2, set);
            }

            set.add(new DatasetEntry(chartInput.VAR1, nrNodes));
        }
        for (int key : datasetMap.keySet()) {
            chart.addDataset(datasetMap.get(key));
        }
        return chart;
    }

    public static IChart L_vs_Nodes(List<ChartInput> chartInputList, boolean L_in_Percentage, 
            String legendVar, boolean addLegend) {
        XYMultiChart chart = new XYMultiChart("", "L", "# Nodes", addLegend);
        if (L_in_Percentage) {
            chart = new XYMultiChart("", "L%", "# Nodes", addLegend);
        }

        chart.getTags().add(IChart.TagEnum.STANDARD);
        chart.getTags().add(IChart.TagEnum.L_VS_NODES);

        if (chartInputList == null) {
            return chart;
        }

        HashMap<Integer, Dataset> datasetMap = new HashMap<Integer, Dataset>();

        for (ChartInput chartInput : chartInputList) {
            int nrNodes = chartInput.Results.get(0).getFitness();
            Dataset set;
            if (datasetMap.containsKey(chartInput.VAR1)) {
                set = datasetMap.get(chartInput.VAR1);
            } else {
                set = new Dataset(String.format(legendVar + "=%d", chartInput.VAR1));
                datasetMap.put(chartInput.VAR1, set);
            }
            set.add(new DatasetEntry(chartInput.VAR2, nrNodes));
        }
        for (int key : datasetMap.keySet()) {
            chart.addDataset(datasetMap.get(key));
        }
        return chart;
    }

    public static IChart L_vs_Nodes_averaged(List<ChartInput> chartInputList, boolean L_in_Percentage,
            String legendVar, boolean addLegend) {
        XYMultiChart chart = new XYMultiChart("", "L", "Averaged # Nodes", addLegend);
        if (L_in_Percentage) {
            chart = new XYMultiChart("", "L%", "Averaged # Nodes", addLegend);
        }

        chart.getTags().add(IChart.TagEnum.STANDARD);
        chart.getTags().add(IChart.TagEnum.L_VS_NODES);
        chart.getTags().add(IChart.TagEnum.AVG);

        if (chartInputList == null) {
            return chart;
        }

        HashMap<Integer, Dataset> datasetMap = new HashMap<Integer, Dataset>();

        for (ChartInput chartInput : chartInputList) {
            double mean = 0;
            double variance = 0;
            for (Result res : chartInput.Results) {
                mean += res.getFitness();
            }

            mean = mean / chartInput.Results.size();
            for (Result res : chartInput.Results) {
                variance += Math.pow(res.getFitness() - mean, 2);
            }
            variance = variance / chartInput.Results.size();

            Dataset set;
            if (datasetMap.containsKey(chartInput.VAR1)) {
                set = datasetMap.get(chartInput.VAR1);
            } else {
                set = new Dataset(String.format(legendVar + "=%d", chartInput.VAR1));
                datasetMap.put(chartInput.VAR1, set);
            }
            set.add(new DatasetEntryWithVariance(chartInput.VAR2, mean, mean - variance, mean + variance));
        }
        for (int key : datasetMap.keySet()) {
            chart.addDataset(datasetMap.get(key));
        }
        return chart;
    }

    public static IChart K_vs_Nodes_averaged(List<ChartInput> chartInputList, boolean L_in_Percentage,
            boolean fixed_L) {
        XYMultiChart chart = new XYMultiChart("", "K", "Averaged # Nodes", true);

        chart.getTags().add(IChart.TagEnum.STANDARD);
        chart.getTags().add(IChart.TagEnum.K_VS_NODES);
        chart.getTags().add(IChart.TagEnum.AVG);

        if (chartInputList == null) {
            return chart;
        }

        HashMap<Integer, Dataset> datasetMap = new HashMap<Integer, Dataset>();

        for (ChartInput chartInput : chartInputList) {
            double mean = 0;
            double variance = 0;
            for (Result res : chartInput.Results) {
                mean += res.getFitness();
            }

            mean = mean / chartInput.Results.size();
            for (Result res : chartInput.Results) {
                variance += Math.pow(res.getFitness() - mean, 2);
            }
            variance = variance / chartInput.Results.size();

            Dataset set;
            if (datasetMap.containsKey(chartInput.VAR2)) {
                set = datasetMap.get(chartInput.VAR2);
            } else {
                
                if (fixed_L) {
                    set = new Dataset("L");
                } else if (L_in_Percentage) {
                    set = new Dataset(String.format("L=%d%%", chartInput.VAR2));
                } else {
                    set = new Dataset(String.format("L=%d", chartInput.VAR2));
                }
                datasetMap.put(chartInput.VAR2, set);
            }
            set.add(new DatasetEntryWithVariance(chartInput.VAR1, mean, mean - variance, mean + variance));
        }
        for (int key : datasetMap.keySet()) {
            chart.addDataset(datasetMap.get(key));
        }
        return chart;
    }

    public static IChart L1_vs_Nodes(List<ChartInput> chartInputList,
            boolean L1_in_Percentage, boolean L2_in_Percentage, String L1_ID,
            String L2_ID) {
        XYMultiChart chart = new XYMultiChart("", L1_ID, "# Nodes", true);
        if (L1_in_Percentage) {
            chart = new XYMultiChart("", L1_ID + "%", "# Nodes", true);
        }

        chart.getTags().add(IChart.TagEnum.STANDARD);
        chart.getTags().add(IChart.TagEnum.L_VS_NODES);

        if (chartInputList == null) {
            return chart;
        }

        HashMap<Integer, Dataset> datasetMap = new HashMap<Integer, Dataset>();

        for (ChartInput chartInput : chartInputList) {
            int nrNodes = chartInput.Results.get(0).getFitness();
            Dataset set;
            if (datasetMap.containsKey(chartInput.VAR2)) {
                set = datasetMap.get(chartInput.VAR2);
            } else {

//                if (fixed_L) {
//                    set = new Dataset(String.format("",chartInput.L));
//                } else 
//                    
                if (L2_in_Percentage) {
                    set = new Dataset(String.format(L2_ID + "=%d%%", chartInput.VAR2));
                } else {
                    set = new Dataset(String.format(L2_ID + "=%d", chartInput.VAR2));
                }
                datasetMap.put(chartInput.VAR2, set);
            }

            set.add(new DatasetEntry(chartInput.VAR1, nrNodes));
        }
        for (int key : datasetMap.keySet()) {
            chart.addDataset(datasetMap.get(key));
        }
        return chart;
    }

    public static IChart L1_vs_Nodes_averaged(List<ChartInput> chartInputList,
            boolean L1_in_Percentage, boolean L2_in_Percentage, String L1_ID,
            String L2_ID) {
        XYMultiChart chart = new XYMultiChart("", L1_ID, "# Nodes", true);
        if (L1_in_Percentage) {
            chart = new XYMultiChart("", L1_ID + "%", "# Nodes", true);
        }

        chart.getTags().add(IChart.TagEnum.STANDARD);
        chart.getTags().add(IChart.TagEnum.L_VS_NODES);
        chart.getTags().add(IChart.TagEnum.AVG);

        if (chartInputList == null) {
            return chart;
        }

        HashMap<Integer, Dataset> datasetMap = new HashMap<Integer, Dataset>();

        for (ChartInput chartInput : chartInputList) {
            double mean = 0;
            double variance = 0;
            for (Result res : chartInput.Results) {
                mean += res.getFitness();
            }

            mean = mean / chartInput.Results.size();
            for (Result res : chartInput.Results) {
                variance += Math.pow(res.getFitness() - mean, 2);
            }
            variance = variance / chartInput.Results.size();
            Dataset set;
            if (datasetMap.containsKey(chartInput.VAR2)) {
                set = datasetMap.get(chartInput.VAR2);
            } else {

//                if (fixed_L) {
//                    set = new Dataset(String.format("",chartInput.L));
//                } else 
//                    
                if (L2_in_Percentage) {
                    set = new Dataset(String.format(L2_ID + "=%d%%", chartInput.VAR2));
                } else {
                    set = new Dataset(String.format(L2_ID + "=%d", chartInput.VAR2));
                }
                datasetMap.put(chartInput.VAR2, set);
            }

            set.add(new DatasetEntryWithVariance(chartInput.VAR1, mean, mean - variance, mean + variance));
        }
        for (int key : datasetMap.keySet()) {
            chart.addDataset(datasetMap.get(key));
        }
        return chart;
    }

    public static IChart L2_vs_Nodes(List<ChartInput> chartInputList,
            boolean L1_in_Percentage, boolean L2_in_Percentage, String L1_ID,
            String L2_ID) {
        XYMultiChart chart = new XYMultiChart("", L2_ID, "# Nodes", true);
        if (L2_in_Percentage) {
            chart = new XYMultiChart("", L2_ID + "%", "# Nodes", true);
        }

        chart.getTags().add(IChart.TagEnum.STANDARD);
        chart.getTags().add(IChart.TagEnum.L_VS_NODES);

        if (chartInputList == null) {
            return chart;
        }

        HashMap<Integer, Dataset> datasetMap = new HashMap<Integer, Dataset>();

        for (ChartInput chartInput : chartInputList) {
            int nrNodes = chartInput.Results.get(0).getFitness();
            Dataset set;
            if (datasetMap.containsKey(chartInput.VAR1)) {
                set = datasetMap.get(chartInput.VAR1);
            } else {

                if (L1_in_Percentage) {
                    set = new Dataset(String.format(L1_ID + "=%d%%", chartInput.VAR1));
                } else {
                    set = new Dataset(String.format(L1_ID + "=%d", chartInput.VAR1));
                }
                datasetMap.put(chartInput.VAR1, set);
            }

            set.add(new DatasetEntry(chartInput.VAR2, nrNodes));
        }
        for (int key : datasetMap.keySet()) {
            chart.addDataset(datasetMap.get(key));
        }
        return chart;
    }

    public static IChart L2_vs_Nodes_averaged(List<ChartInput> chartInputList,
            boolean L1_in_Percentage, boolean L2_in_Percentage, String L1_ID,
            String L2_ID) {
        XYMultiChart chart = new XYMultiChart("", L2_ID, "# Nodes", true);
        if (L2_in_Percentage) {
            chart = new XYMultiChart("", L2_ID + "%", "# Nodes", true);
        }

        chart.getTags().add(IChart.TagEnum.STANDARD);
        chart.getTags().add(IChart.TagEnum.L_VS_NODES);
        chart.getTags().add(IChart.TagEnum.AVG);

        if (chartInputList == null) {
            return chart;
        }

        HashMap<Integer, Dataset> datasetMap = new HashMap<Integer, Dataset>();

        for (ChartInput chartInput : chartInputList) {
            double mean = 0;
            double variance = 0;
            for (Result res : chartInput.Results) {
                mean += res.getFitness();
            }

            mean = mean / chartInput.Results.size();
            for (Result res : chartInput.Results) {
                variance += Math.pow(res.getFitness() - mean, 2);
            }
            variance = variance / chartInput.Results.size();
            Dataset set;
            if (datasetMap.containsKey(chartInput.VAR1)) {
                set = datasetMap.get(chartInput.VAR1);
            } else {

                if (L1_in_Percentage) {
                    set = new Dataset(String.format(L1_ID + "=%d%%", chartInput.VAR1));
                } else {
                    set = new Dataset(String.format(L1_ID + "=%d", chartInput.VAR1));
                }
                datasetMap.put(chartInput.VAR1, set);
            }

            set.add(new DatasetEntryWithVariance(chartInput.VAR2, mean, mean - variance, mean + variance));
        }
        for (int key : datasetMap.keySet()) {
            chart.addDataset(datasetMap.get(key));
        }
        return chart;
    }
}

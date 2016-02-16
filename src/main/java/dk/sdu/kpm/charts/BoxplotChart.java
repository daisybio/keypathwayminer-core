package dk.sdu.kpm.charts;

import java.awt.Color;
import java.awt.Font;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.LegendItem;
import org.jfree.chart.LegendItemCollection;
import org.jfree.chart.LegendItemSource;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.renderer.category.BoxAndWhiskerRenderer;
import org.jfree.chart.title.LegendTitle;
import org.jfree.data.statistics.DefaultBoxAndWhiskerCategoryDataset;
import org.jfree.ui.RectangleEdge;
import org.jfree.util.StringUtils;

public class BoxplotChart extends BaseChart implements Serializable {
	private DefaultBoxAndWhiskerCategoryDataset dataset;

	private ColorGenerator colorGenerator;
	private int datasetIndex;

	public BoxplotChart(String title, String xAxisTitle, String yAxisTitle){
        super();
		dataset = new DefaultBoxAndWhiskerCategoryDataset ();	

		CategoryAxis xAxis = new CategoryAxis("Type");
		NumberAxis yAxis = new NumberAxis("Value");
		BoxAndWhiskerRenderer renderer = new BoxAndWhiskerRenderer();
		renderer.setFillBox(false);
		CategoryPlot plot = new CategoryPlot(dataset, xAxis, yAxis, renderer);
		chart = new JFreeChart(title, plot); 

		Font font = new Font("Helvetica", Font.BOLD, 12);
		xAxis.setLabelFont(font);
		xAxis.setLabel(xAxisTitle);
		yAxis.setLabelFont(font);
		yAxis.setLabel(yAxisTitle);

		// Making it pretty
		plot.setBackgroundPaint(Color.white);
		plot.setDomainGridlinePaint(Color.lightGray);
		plot.setRangeGridlinePaint(Color.lightGray);
		chart.setBackgroundPaint(Color.white);
        chart.setTextAntiAlias(true);
		yAxis.setAutoRange(true);
		colorGenerator = new ColorGenerator();
		datasetIndex = 0;

        chart.getLegend().setPosition(RectangleEdge.BOTTOM);
        chart.getLegend().setBorder(1,1,1,1);
	}

	public void addDataset(String type, String series, List<Double> values){
		if(values == null){
			values = new ArrayList<Double>();
		}

        if(values.size() == 0){
            values.add(0.0);
        }

        this.dataset.add(values, series, type);
		Color color = colorGenerator.getNext();		
		CategoryPlot plot = (CategoryPlot)chart.getPlot();		
		BoxAndWhiskerRenderer renderer = new BoxAndWhiskerRenderer();
		renderer.setSeriesPaint(0, color);
		renderer.setMeanVisible(false);
		plot.setRenderer(datasetIndex, renderer);
		datasetIndex ++;
		
		// Update legends, remove those from variance.
		LegendItemCollection legendItemsOld = plot.getLegendItems();
		this.updateAndSortLegend(legendItemsOld);
	}

    public void updateAndSortLegend(LegendItemCollection legendItemsOld){
        HashMap<String, LegendItem> oldIndexes = new HashMap<String, LegendItem>();

        // Finding old indexes and saving them for sorting (not including those starting with "var"
        for(int i = 0; i< legendItemsOld.getItemCount(); i++){
            LegendItem item = legendItemsOld.get(i);
            if(!item.getLabel().equals("var")){
                oldIndexes.put(trimLabel(item.getLabel()), item);
            }
        }

        // Sorting the new legend
        final LegendItemCollection legendItemsNew = new LegendItemCollection();
        Object[] keys = oldIndexes.keySet().toArray();
        Arrays.sort(keys);
        for(Object key : keys) {
            legendItemsNew.add(oldIndexes.get(key));
        }


        LegendItemSource source = new LegendItemSource() {
            LegendItemCollection lic = new LegendItemCollection();
            {lic.addAll(legendItemsNew);}
            public LegendItemCollection getLegendItems() {
                return lic;
            }
        };

        // Forcing legend to be in bottom
        LegendTitle legend = new LegendTitle(source);
        legend.setPosition(RectangleEdge.BOTTOM);
        legend.setBorder(1,1,1,1);

        chart.removeLegend();
        chart.addLegend(legend);
    }

    private String trimLabel(String label){
        String padding = "00000000000000000000000000000000000000";
        String trimmed = padding.substring(label.length()) + label;

        return trimmed;
    }

}

package dk.sdu.kpm.charts;

import java.awt.Color;
import java.awt.Font;
import java.io.Serializable;
import java.util.Random;
import java.util.Set;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.LegendItemCollection;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.category.StandardBarPainter;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.ui.RectangleEdge;

public class BarChart extends BaseChart implements Serializable {

	private DefaultCategoryDataset dataset;
		
	public BarChart(String title, String xAxisTitle, String yAxisTitle){
        super();
		dataset = new DefaultCategoryDataset();		
		chart = ChartFactory.createBarChart(title, xAxisTitle, yAxisTitle,
				dataset, PlotOrientation.VERTICAL, true, false, false);
		setChartStyle();
	}
	
	private void setChartStyle(){
		chart.setBackgroundPaint(Color.white);
		chart.setTextAntiAlias(true);

		CategoryPlot plot = chart.getCategoryPlot();
		CategoryAxis xAxis = plot.getDomainAxis();
		ValueAxis yAxis = plot.getRangeAxis();

		Font font = new Font("Helvetica", Font.BOLD, 12);
		xAxis.setLabelFont(font);
		yAxis.setLabelFont(font);
		
		// Making it pretty
		plot.setBackgroundPaint(Color.white);
		plot.setDomainGridlinePaint(Color.lightGray);
		plot.setRangeGridlinePaint(Color.lightGray);
		
		// disable bar outlines...  
        BarRenderer renderer = (BarRenderer) plot.getRenderer();   
        chart.getCategoryPlot().setRenderer(renderer);  
        ((BarRenderer)renderer).setBarPainter(new StandardBarPainter());

        chart.getLegend().setPosition(RectangleEdge.BOTTOM);
        chart.getLegend().setBorder(1,1,1,1);
        
        // Choose pastel colors for bars
        Random random = new Random(3);
        for(int i = 0; i < 2; i++){
        	
        	int[] colorBytes = new int[3];
            colorBytes[0] = random.nextInt(128) + 127;
            colorBytes[1] = random.nextInt(128) + 127;
            colorBytes[2] = random.nextInt(128) + 127;
                        
            Color color = new Color(colorBytes[0], colorBytes[1], colorBytes[2], 255);
            renderer.setSeriesPaint(i, color);
         }		
	}
	
	/**
	 * Adds a Dataset to the chart
	 */
	public void addDataset(String category, String series, Set<Double> values){
		for (double value : values) {
            this.dataset.addValue(value, series, category);
        }

		// Update legends, remove those from variance.
		LegendItemCollection legendItemsOld = chart.getPlot().getLegendItems();
		this.updateAndSortLegend(legendItemsOld);
	}
	
}

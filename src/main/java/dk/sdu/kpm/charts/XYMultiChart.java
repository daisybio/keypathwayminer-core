package dk.sdu.kpm.charts;

import java.awt.Color;
import java.awt.Font;
import java.awt.Shape;
import java.awt.geom.Line2D;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Iterator;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.LegendItemCollection;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYSplineRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.RectangleEdge;
import org.jfree.util.ShapeUtilities;

public class XYMultiChart extends BaseChart implements Serializable {

	private int datasetIndex;
	
	private ColorGenerator colorGenerator;
        
        private boolean addLegend;

	public XYMultiChart(String title, String xAxisTitle, String yAxisTitle, 
                boolean addLegend){
        super();
                this.addLegend = addLegend;
 		datasetIndex = 0;
		XYSeriesCollection dataset = new XYSeriesCollection(new XYSeries(""));
		chart = ChartFactory.createXYLineChart(title, xAxisTitle, yAxisTitle,
				dataset, PlotOrientation.VERTICAL, this.addLegend, false, false);

		chart.setBackgroundPaint(Color.white);
		//chart.setTextAntiAlias(true);

		XYPlot xyplot = chart.getXYPlot();
		ValueAxis xAxis = xyplot.getDomainAxis();
		ValueAxis yAxis = xyplot.getRangeAxis();
		xAxis.setAutoRange(true);
		yAxis.setAutoRange(true);
        yAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
        xAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());

		Font font = new Font("Helvetica", Font.BOLD, 12);
		xAxis.setLabelFont(font);
		yAxis.setLabelFont(font);

		// Making it pretty
		xyplot.setBackgroundPaint(Color.white);
		xyplot.setDomainMinorGridlinePaint(Color.lightGray);
		xyplot.setDomainGridlinePaint(Color.lightGray);
		xyplot.setRangeGridlinePaint(Color.lightGray);
        chart.setTextAntiAlias(true);
		
		colorGenerator = new ColorGenerator();
                
           if(addLegend) {
            chart.getLegend().setPosition(RectangleEdge.BOTTOM);
            chart.getLegend().setBorder(1,1,1,1);   
           }            
	}


	/**
	 * Adds a Dataset to the chart
	 * @param set
	 */
	public void addDataset(Dataset set){
		XYSeries series = new XYSeries(set.getTitle());
		Iterator<DatasetEntry> iterator = set.getDataIterator();	

		// find out if there are any data in the set
		boolean any = iterator.hasNext();
		
		boolean hasVariance = false;
		
		while (iterator.hasNext()) {
			DatasetEntry entry = iterator.next();
			
			// If the Dataset contains variance, it must be added
			if(entry instanceof DatasetEntryWithVariance && !hasVariance){
				hasVariance = true;
			}
			series.add(entry.getX(), entry.getY());
		}

		// if the series contain any data, add it to the chart
		if(any){
			Color color = colorGenerator.getNext();			
			XYPlot xyplot = chart.getXYPlot();
			xyplot.setDataset(datasetIndex, new XYSeriesCollection(series));
			XYSplineRenderer renderer = new XYSplineRenderer();
			renderer.setSeriesPaint(0, color);
			xyplot.setRenderer(datasetIndex, renderer);
			
			// Increment the index
			datasetIndex++;
			
			
			if(hasVariance){
				addVarianceSeries(set.getDataIterator(), color);
			}else if(addLegend) {
                // Update legends, remove those from variance.
                LegendItemCollection legendItemsOld = xyplot.getLegendItems();
                this.updateAndSortLegend(legendItemsOld);
            }
		}

	}

    private boolean allSameX = true;

    private boolean firstVal = true;

    private double lastVal = 0;

    private double minVal = 0;

    private double maxVal = 0;

	// Add variance to a series, giving it the same color
	private void addVarianceSeries(Iterator<DatasetEntry> iterator, Color color){
		HashSet<XYSeries> varianceCollections = new HashSet<XYSeries>();

		while(iterator.hasNext()) {
			DatasetEntry entry = iterator.next();
			
			// if it is not an instance of the 
			if(!(entry instanceof DatasetEntryWithVariance)){
				continue;
			}
			
			DatasetEntryWithVariance dewv = (DatasetEntryWithVariance)entry;
			
			XYSeries series = new XYSeries("var");	
			series.add(dewv.getX(), dewv.getLowY());
			series.add(dewv.getX()+0.0001, dewv.getHighY());
			varianceCollections.add(series);
            if(firstVal == false && lastVal != dewv.getX()){
                allSameX = false;
            }

            if(minVal > dewv.getLowY()){
                minVal = dewv.getLowY();
            }

            if(maxVal < dewv.getHighY()){
                maxVal = dewv.getHighY();
            }

            lastVal = dewv.getX();
            firstVal = false;
		}

        updateAxis();

		XYPlot xyplot = chart.getXYPlot();
		Line2D line2d = new Line2D.Float(3f, 0, -3f, 0);
		Shape line = ShapeUtilities.createLineRegion(line2d, 0.1f);

		// if the series contain any data, add it to the chart
		for(XYSeries series : varianceCollections){
			xyplot.setDataset(datasetIndex, new XYSeriesCollection(series));
			XYItemRenderer renderer = new XYSplineRenderer();
			renderer.setSeriesPaint(0, color);
			renderer.setSeriesShape(0, line);
			xyplot.setRenderer(datasetIndex, renderer);


			// Increment the index
			datasetIndex++;
		}

		// Update legends, remove those from variance.
                if (addLegend) {
                    	LegendItemCollection legendItemsOld = xyplot.getLegendItems();
		this.updateAndSortLegend(legendItemsOld);
                }
	
	}

    private void updateAxis(){
        XYPlot xyplot = chart.getXYPlot();
        ValueAxis xAxis = xyplot.getDomainAxis();
        if(allSameX){
            xAxis.centerRange(lastVal);
            xAxis.setRange(lastVal - 1, lastVal + 1);
        }else{
            xAxis.setAutoRange(true);
        }

        ValueAxis yAxis = xyplot.getRangeAxis();
        yAxis.setRange(minVal - 1, maxVal + 1);
    }

	
}

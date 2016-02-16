package dk.sdu.kpm.charts;

import java.awt.Dimension;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.nio.file.FileAlreadyExistsException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import javax.swing.JPanel;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.LegendItem;
import org.jfree.chart.LegendItemCollection;
import org.jfree.chart.LegendItemSource;
import org.jfree.chart.title.LegendTitle;
import org.jfree.ui.RectangleEdge;

public class BaseChart implements IChart, Serializable, Comparable {
	
	protected JFreeChart chart;

    protected ArrayList<TagEnum> tags;

    protected String sortName;

    protected String sortTitle;

    protected BaseChart()
    {
        sortTitle = "";
        sortName = "";
        tags = new ArrayList<TagEnum>();
    }
	
	@Override
	public String getTitle() {
		if(this.chart == null){
			return "";
		}
		
		return this.chart.getTitle().getText();
	}

	@Override
	public void setTitle(String newTitle) {
		if(this.chart == null){
			return;
		}
		
		this.chart.setTitle(newTitle);
	}
	
	/**
	 * Get the panel displaying the chart, wrapped as a JPanel.
	 * @return
	 */
	public JPanel getChartPanel(){
		if(this.chart == null){
			return new JPanel();
		}

		ChartPanel chartPanel = new ChartPanel(chart){
			@Override
			public Dimension getPreferredSize() {
				return new Dimension(480, 360);
			}
		};

		return chartPanel;
	}

    @Override
    public List<TagEnum> getTags() {
        return tags;
    }

    @Override
    public boolean containsTag(TagEnum tag) {
        if(tags == null || tags.size() == 0){
            return false;
        }

        return tags.contains(tag);
    }

    @Override
    public String getSortName() {
        return this.sortName;
    }

    @Override
    public String getSortTitle() {
        return sortTitle;
    }

    public void setSortName(String name) {
        this.sortName = name;
    }

    public void setSortTitle(String title) {
        this.sortTitle = title;
    }

    /**
	 * Save the chart as a .png file with the given width and height.
	 * @param file
	 * @param width
	 * @param height
	 * @throws IOException
	 */
	public void saveAsPng(java.io.File file, int width, int height) throws IOException{
			
		if(file.exists()){
			throw new FileAlreadyExistsException(file.toString());
		}

		if(this.chart == null){
			System.out.println("No chart for file '"+file.getName()+"' found. Not saving.");
			return;
		}
		
		ChartUtilities.saveChartAsPNG(file, this.chart, width, height);
	}
	
	public void writeChartAsJpeg(OutputStream out, int width, int height) throws IOException{
		ChartUtilities.writeChartAsJPEG(out, 1.0f, chart, width, height);		
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
        label = label.trim();
        int lastEqual = label.lastIndexOf("=");
        if(lastEqual == -1){
            return label;
        }

        if(label.length() > lastEqual + 2){
            return label;
        }

        return label.replace("=", "=0");
    }

    public int compareTo(Object o) {
        BaseChart bc = (BaseChart)o;
        return(this.getSortTitle().compareTo(bc.getSortTitle()));
    }

    

}

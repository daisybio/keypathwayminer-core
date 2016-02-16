package dk.sdu.kpm.charts;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import javax.swing.JPanel;

public interface IChart {
	public void setTitle(String newTitle);
	public String getTitle();
	public JPanel getChartPanel();
    public List<TagEnum> getTags();
    public boolean containsTag(TagEnum tag);
    public String getSortName();
    public String getSortTitle();
	public void saveAsPng(java.io.File file, int width, int height) throws IOException;
	public void writeChartAsJpeg(OutputStream out, int width, int height) throws IOException;

    public enum TagEnum{
        PERTURB_LEVEL_FIXED, K_FIXED, L_FIXED, STANDARD, K_VS_NODES, L_VS_NODES, ORIGINAL_RESULT, GOLD_STANDARD, AVG
    }
}

package dk.sdu.kpm.graph;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Paint;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import javax.imageio.ImageIO;

import org.apache.commons.collections15.Transformer;

import edu.uci.ics.jung.algorithms.layout.FRLayout;
import edu.uci.ics.jung.algorithms.layout.Layout;
import edu.uci.ics.jung.visualization.VisualizationImageServer;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.decorators.EdgeShape;
import edu.uci.ics.jung.visualization.decorators.ToStringLabeller;
import edu.uci.ics.jung.visualization.renderers.DefaultVertexLabelRenderer;
import edu.uci.ics.jung.visualization.renderers.Renderer;

class KPMGraphSaver {
	public static void SaveGraphToFile(KPMGraph graph, String filePath){
		FRLayout<GeneNode, GeneEdge> visualizer;
		VisualizationViewer<GeneNode, GeneEdge> visViewer;

		visualizer = new FRLayout<GeneNode, GeneEdge>(graph);

		visViewer = new VisualizationViewer<GeneNode, GeneEdge>(visualizer);

		visViewer.setBackground(Color.WHITE);
		visViewer.getRenderer().getVertexLabelRenderer().setPosition(Renderer.VertexLabel.Position.CNTR);
		visViewer.getRenderContext().setVertexLabelTransformer(new ToStringLabeller<GeneNode>());
		visViewer.setForeground(Color.BLACK);

		visViewer.setBackground(Color.WHITE);
		visViewer.getRenderContext().setEdgeLabelTransformer(new ToStringLabeller<GeneEdge>());
		visViewer.getRenderContext().setEdgeShapeTransformer(new EdgeShape.Line<GeneNode, GeneEdge>());
		visViewer.getRenderContext().setVertexLabelTransformer(new ToStringLabeller<GeneNode>());
		visViewer.getRenderContext().setVertexLabelRenderer(new DefaultVertexLabelRenderer(Color.LIGHT_GRAY));

		Transformer<GeneNode, Paint> vertexPaint = new Transformer<GeneNode, Paint>() {public Paint transform(GeneNode n) {return Color.LIGHT_GRAY;}};

		visViewer.getRenderContext().setVertexFillPaintTransformer(vertexPaint);
		visViewer.getRenderer().getVertexLabelRenderer()
		.setPosition(Renderer.VertexLabel.Position.CNTR);

		// Create the VisualizationImageServer
		// vv is the VisualizationViewer containing my graph
		VisualizationImageServer<GeneNode, GeneEdge> vis =
				new VisualizationImageServer<GeneNode, GeneEdge>(visViewer.getGraphLayout(),
						visViewer.getGraphLayout().getSize());





		// Create the buffered image
		BufferedImage image = (BufferedImage) vis.getImage(
				new Point2D.Double(visViewer.getGraphLayout().getSize().getWidth() / 2,
						visViewer.getGraphLayout().getSize().getHeight() / 2),
						new Dimension(visViewer.getGraphLayout().getSize()));

		// Ensure directory endings are correct
		if(!filePath.endsWith("\\")){
			filePath += "\\";
		}

		String formattedTime = new SimpleDateFormat("HH.mm.ss").format(Calendar.getInstance().getTime());

		File outputfile = new File(String.format("%sgraph_%s.png",filePath, formattedTime));

		try {
			ImageIO.write(image, "png", outputfile);
		} catch (IOException e) {
			// Exception handling
			System.out.println("fail!!");
		}
	}
}

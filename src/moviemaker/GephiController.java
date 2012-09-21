/**
 * Written by André Panisson <panisson@gmail.com>  
 * Copyright (C) 2012 Istituto per l'Interscambio Scientifico I.S.I.  
 * You can contact us by email (isi@isi.it) or write to:  
 * ISI Foundation, Via Alassio 11/c, 10126 Torino, Italy.  
 */

package moviemaker;

import java.awt.Color;
import java.awt.Font;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Properties;

import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.JPEGTranscoder;
import org.gephi.filters.api.FilterController;
import org.gephi.filters.api.Query;
import org.gephi.filters.api.Range;
import org.gephi.filters.plugin.dynamic.DynamicRangeBuilder;
import org.gephi.filters.plugin.dynamic.DynamicRangeBuilder.DynamicRangeFilter;
import org.gephi.filters.spi.FilterBuilder;
import org.gephi.graph.api.GraphController;
import org.gephi.graph.api.GraphModel;
import org.gephi.graph.api.GraphView;
import org.gephi.io.exporter.preview.SVGExporter;
import org.gephi.io.importer.api.Container;
import org.gephi.io.importer.api.EdgeDefault;
import org.gephi.io.importer.api.ImportController;
import org.gephi.io.processor.plugin.DefaultProcessor;
import org.gephi.preview.api.PreviewController;
import org.gephi.preview.api.PreviewModel;
import org.gephi.preview.api.PreviewProperty;
import org.gephi.preview.types.EdgeColor;
import org.gephi.project.api.ProjectController;
import org.gephi.project.api.Workspace;
import org.openide.util.Lookup;

/**
 *
 * @author panisson
 */
public class GephiController {

    private Properties properties;

    private Workspace workspace;
    private GraphModel graphModel;
    private FilterController filterController = null;
    private DynamicRangeFilter dynamicRangeFilter;
    private Query dynamicQuery;

    public GephiController(Properties properties) {
	this.properties = properties;
    }

    public void importGEXF(File gexf) throws FileNotFoundException {

	/*
	 * The NetBeans masterfs library creates a file watcher Thread that is not a daemon thread.
	 * This line disables the creation of such thread that is not useful in this case.
	 */
	System.setProperty("org.netbeans.modules.masterfs.watcher.disable", "true");

	// get controller
	ImportController importController = Lookup.getDefault().lookup(ImportController.class);

	//Import file
	Container container = importController.importFile(gexf);
	container.getLoader().setEdgeDefault(EdgeDefault.UNDIRECTED);
	container.setAutoScale(false);
	container.setAllowAutoNode(false);  //Don't create missing nodes

	//Append imported data to GraphAPI
	importController.process(container, new DefaultProcessor(), workspace);
    }

    private void initFilters() {
	//Create a dynamic range filter query
	filterController = Lookup.getDefault().lookup(FilterController.class);
	FilterBuilder[] builders = Lookup.getDefault().lookup(DynamicRangeBuilder.class).getBuilders();
	dynamicRangeFilter = (DynamicRangeFilter) builders[0].getFilter();     //There is only one TIME_INTERVAL column, so it's always the [0] builder
	dynamicQuery = filterController.createQuery(dynamicRangeFilter);
	filterController.add(dynamicQuery);
    }

    public void init() {
	//Init a project - and therefore a workspace
	ProjectController pc = Lookup.getDefault().lookup(ProjectController.class);
	pc.newProject();
	workspace = pc.getCurrentWorkspace();

	//Get a graph model - it exists because we have a workspace
	graphModel = Lookup.getDefault().lookup(GraphController.class).getModel();

	//Preview
	PreviewController previewController = Lookup.getDefault().lookup(PreviewController.class);
	PreviewModel model = previewController.getModel();
	model.getProperties().putValue(PreviewProperty.BACKGROUND_COLOR, Color.WHITE);

	model.getProperties().putValue(PreviewProperty.NODE_BORDER_WIDTH, 
		Float.valueOf(properties.getProperty("nodeBorderWidth")));
	model.getProperties().putValue(PreviewProperty.SHOW_NODE_LABELS, 
		Boolean.valueOf(properties.getProperty("showNodeLabels")));
	model.getProperties().putValue(PreviewProperty.NODE_LABEL_PROPORTIONAL_SIZE, Boolean.TRUE);
	model.getProperties().putValue(PreviewProperty.SHOW_NODE_LABELS, Boolean.TRUE);

	//TODO: nodeLabelFontSize = properties.getProperty("nodeLabelFontSize")
	Font font = model.getProperties().getFontValue(PreviewProperty.NODE_LABEL_FONT);
	font = font.deriveFont(Float.valueOf(properties.getProperty("nodeLabelFontSize")));
	model.getProperties().putValue(PreviewProperty.NODE_LABEL_FONT, font);

	model.getProperties().putValue(PreviewProperty.EDGE_COLOR,
		new EdgeColor(Color.decode(properties.getProperty("edgeColor"))));
	model.getProperties().putValue(PreviewProperty.EDGE_CURVED, Boolean.FALSE);

	// TODO: edgeScale = properties.getProperty("edgeScale")
	model.getProperties().putValue(PreviewProperty.EDGE_THICKNESS, 
		Float.valueOf(properties.getProperty("edgeScale")));

    }

    public BufferedImage createImage(double width, double height) {
	BufferedImage image = null;

	try {

	    /*
	     * We first export to SVG, transform it to a JPEG
	     * and finally read it as a BufferedImage.
	     * It would be preferable to directly export the preview
	     * as a BufferedImage.
	     */

	    SVGExporter exporter = new SVGExporter();
	    exporter.setWorkspace(workspace);
	    StringWriter writer = new StringWriter();
	    exporter.setWriter(writer);
	    exporter.execute();
	    writer.flush();
	    writer.close();

	    // Create a JPEG transcoder
	    StringReader reader = new StringReader(writer.toString());
	    JPEGTranscoder t = new JPEGTranscoder();
	    t.addTranscodingHint(JPEGTranscoder.KEY_QUALITY, new Float(1.0));
	    t.addTranscodingHint(JPEGTranscoder.KEY_WIDTH, Float.valueOf((float)width));
	    t.addTranscodingHint(JPEGTranscoder.KEY_HEIGHT, Float.valueOf((float)height));


	    TranscoderInput input = new TranscoderInput(reader);
	    ByteArrayOutputStream ostream = new ByteArrayOutputStream();
	    TranscoderOutput output = new TranscoderOutput(ostream);
	    t.transcode(input, output);

	    // Flush and close the stream.
	    ostream.flush();
	    ostream.close();

	    image = javax.imageio.ImageIO.read(new ByteArrayInputStream(ostream.toByteArray()));

	} catch (IOException ex) {
	    ex.printStackTrace();
	    return null;
	} catch (TranscoderException e) {
	    e.printStackTrace();
	    System.exit(0);
	}

	return image;
    }

    public BufferedImage generateFrame(double start, double end, double width, double height) {

	if (filterController == null) initFilters();

	GraphView previousView = graphModel.getVisibleView();

	dynamicRangeFilter.setRange(new Range(start, end));
	GraphView view = filterController.filter(dynamicQuery);

	graphModel.setVisibleView(view);

	BufferedImage image = createImage(width, height);

	graphModel.setVisibleView(previousView);
	graphModel.destroyView(view);

	return image;
    }

    public Workspace getWorkspace() {
	return workspace;
    }

}

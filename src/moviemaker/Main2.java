/**
 * Written by André Panisson <panisson@gmail.com>  
 * Copyright (C) 2012 Istituto per l'Interscambio Scientifico I.S.I.  
 * You can contact us by email (isi@isi.it) or write to:  
 * ISI Foundation, Via Alassio 11/c, 10126 Torino, Italy.  
 */

package moviemaker;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.TimeUnit;


import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.gephi.dynamic.api.DynamicController;
import org.gephi.dynamic.api.DynamicModel;
import org.gephi.filters.api.FilterController;
import org.gephi.filters.api.Query;
import org.gephi.filters.api.Range;
import org.gephi.filters.plugin.dynamic.DynamicRangeBuilder;
import org.gephi.filters.plugin.dynamic.DynamicRangeBuilder.DynamicRangeFilter;
import org.gephi.filters.spi.FilterBuilder;
import org.gephi.graph.api.DirectedGraph;
import org.gephi.graph.api.Graph;
import org.gephi.graph.api.GraphController;
import org.gephi.graph.api.GraphFactory;
import org.gephi.graph.api.GraphModel;
import org.gephi.graph.api.GraphView;
import org.gephi.graph.api.Node;
import org.gephi.layout.plugin.AutoLayout;
import org.gephi.layout.plugin.forceAtlas.ForceAtlas;
import org.gephi.layout.plugin.forceAtlas.ForceAtlasLayout;
import org.gephi.layout.spi.Layout;
import org.gephi.project.api.Workspace;
import org.openide.util.Lookup;

public class Main2 {
	
    private Workspace workspace;
    private GraphModel graphModel;
    private FilterController filterController;
    private DynamicRangeFilter dynamicRangeFilter;
    private Query dynamicQuery;
    private Layout layout;
    private Rectangle aoi = new Rectangle(800, 600);
    private VideoWriter videoWriter;
    private GephiController controller;

    public void doIt(Properties properties, String gexfPath, String outPath) {
    	
    	controller = new GephiController(properties);
        controller.init();
        
        // import gexf file
        File gexfFile = new File(gexfPath);
        if (!gexfFile.exists()) {
            throw new RuntimeException("GEXF file not found");
        }
        try {
			controller.importGEXF(gexfFile);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.exit(0);
		}
    	
        workspace = controller.getWorkspace();
        
        //Get a graph model - it exists because we have a workspace
        graphModel = Lookup.getDefault().lookup(GraphController.class).getModel();
        DirectedGraph graph = graphModel.getDirectedGraph();

        // add some fake fixed nodes to delimit the view window
        GraphFactory factory = graphModel.factory();

        // upper left
        Node fake1 = factory.newNode("fake1");
        fake1.getNodeData().setX(-500.0f);
        fake1.getNodeData().setY(-500.0f);
        fake1.getNodeData().setSize(0.0f);
        fake1.getNodeData().setFixed(true);
        graph.addNode(fake1);

        // lower right
        Node fake2 = factory.newNode("fake2");
        fake2.getNodeData().setX(500.0f);
        fake2.getNodeData().setY(500.0f);
        fake2.getNodeData().setSize(0.0f);
        fake2.getNodeData().setFixed(true);
        graph.addNode(fake2);

        // get min and max
        double min = 0.0;
        double max = Double.POSITIVE_INFINITY;
        DynamicController dynamicController = Lookup.getDefault().lookup(DynamicController.class);
        if (dynamicController != null && workspace != null) {
            DynamicModel dynamicModel = dynamicController.getModel(workspace);
            min = dynamicModel.getMin();
            max = dynamicModel.getMax();
        }
        
        //Create a dynamic range filter query
        filterController = Lookup.getDefault().lookup(FilterController.class);
        FilterBuilder[] builders = Lookup.getDefault().lookup(DynamicRangeBuilder.class).getBuilders();
        dynamicRangeFilter = (DynamicRangeFilter) builders[0].getFilter();     //There is only one TIME_INTERVAL column, so it's always the [0] builder
        dynamicQuery = filterController.createQuery(dynamicRangeFilter);
        filterController.add(dynamicQuery);
        
		ForceAtlas forceAtlas = new ForceAtlas();
		ForceAtlasLayout layout = forceAtlas.buildLayout();
		layout.setGraphModel(graphModel);
        
        AutoLayout autoLayout = new AutoLayout(1, TimeUnit.MILLISECONDS);
        autoLayout.addLayout(layout, 1.0f, new AutoLayout.DynamicProperty[]{});
        autoLayout.setGraphModel(graphModel);
        autoLayout.execute();
        
        layout.setRepulsionStrength(200.0);
        layout.setAttractionStrength(10.0);
        layout.setGravity(30.0);
        layout.setSpeed(10.0);
        layout.setAdjustSizes(true);
        layout.setMaxDisplacement(10.0);
        // auto stabilize function
        layout.setFreezeBalance(Boolean.TRUE);
        layout.setFreezeStrength(80.0);
        layout.setFreezeInertia(0.2);
        
        layout.initAlgo();
        
//        ForceAtlas2Builder layoutBuilder = new ForceAtlas2Builder();
//        ForceAtlas2 layout = layoutBuilder.buildLayout();
//        layout.setGraphModel(graphModel);
//        
//        layout.setOutboundAttractionDistribution(Boolean.FALSE);
//        layout.setLinLogMode(Boolean.FALSE);
//        layout.setAdjustSizes(Boolean.FALSE);
//        layout.setScalingRatio(10.0); // repulsion
//        layout.setGravity(10.0);
//        layout.setJitterTolerance(0.2); // speed
//        layout.setBarnesHutOptimize(Boolean.TRUE);
//        layout.setBarnesHutTheta(1.5);
//        
//        layout.initAlgo();
//        layout.goAlgo();
        
        this.layout = layout;
        
        videoWriter = new VideoWriter(outPath, aoi.width, aoi.height, 50);
        videoWriter.init();
		
        int timeWindow = 20; // seconds
		try {
				
	        for (int i=0; min+i*1000<=max; i++) {
	        	
	        	double start = min+i*1000;
	        	double end = start+timeWindow*1000;
	        	generateFrame(start, end);
	            
	            System.out.println(i);
	            
	            if (i>1000) break;
	            
	        }
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			videoWriter.close();
        }
    }
    
    private void updateNodeSize(Graph graph) {
    	
    	for (Node node: graph.getNodes()) {
    		int degree = graph.getDegree(node);
    		float size = (float)Math.pow( degree, 0.8);
    		size *= 4.;
    		node.getNodeData().setSize(size);
    	}
    	
    }
    
    private void generateFrame(double start, double end) {
    	
    	GraphView previousView = graphModel.getVisibleView();
    	
        dynamicRangeFilter.setRange(new Range(start, end));
        GraphView view = filterController.filter(dynamicQuery);
        graphModel.setVisibleView(view);
        graphModel.destroyView(previousView);
        
        updateNodeSize(graphModel.getGraphVisible());

        //for (int j = 0; j < 2 && layout.canAlgo(); j++) {

        	layout.goAlgo();
        	
        	
        	BufferedImage image = controller.createImage(Float.valueOf(aoi.width), Float.valueOf(aoi.height));
        	
        	videoWriter.addFrame(image);
  
        //}
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
    	
        Option gexfOption = new Option("g", "gexf", true, "gexf file to import");
        gexfOption.setRequired(true);

        Option outOption = new Option("o", "out", true, "output file for the movie");
        outOption.setRequired(true);

//        Option startOption = new Option("s", "start", true, "start time value - double format");
//        startOption.setRequired(true);
//
//        Option endOption = new Option("e", "end", true, "end time value - double format");
//        startOption.setRequired(true);
//
//        Option intervalOption = new Option("i", "interval", true, "filtered interval in seconds");
//        startOption.setRequired(true);
//
//        Option stepOption = new Option("t", "step", true, "step between frames in seconds. Default 10");

        Options options = new Options();
        options.addOption(gexfOption);
        options.addOption(outOption);
//        options.addOption(startOption);
//        options.addOption(endOption);
//        options.addOption(intervalOption);
//        options.addOption(stepOption);

        CommandLineParser parser = new PosixParser();
        try {
            CommandLine cmd = parser.parse(options, args);
            String gexf = cmd.getOptionValue("g");
            System.out.println("Using gexf file "+gexf);
            String out = cmd.getOptionValue("o");
            System.out.println("Output to folder "+out);

//            Double start = Double.valueOf(cmd.getOptionValue("s"));
//            System.out.println("Start at "+start);
//            Double end = Double.valueOf(cmd.getOptionValue("e"));
//            System.out.println("End at "+end);
//            int interval = Integer.valueOf(cmd.getOptionValue("i"));
//            System.out.println("Interval of "+interval+" seconds");
//            int step = Integer.valueOf(cmd.getOptionValue("t", "10"));
//            System.out.println("Step of "+step+" seconds");

            Properties prop = new Properties();
            prop.load(new FileReader("preview.properties"));
//            prop.load(prop.getClass().getResourceAsStream("/preview.properties"));
	    	
	        new Main2().doIt(prop, gexf, out);
        
        } catch (ParseException ex) {
            // automatically generate the help statement
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp( "moviemaker", options );
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}

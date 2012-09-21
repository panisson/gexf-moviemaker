/**
 * Written by André Panisson <panisson@gmail.com>  
 * Copyright (C) 2012 Istituto per l'Interscambio Scientifico I.S.I.  
 * You can contact us by email (isi@isi.it) or write to:  
 * ISI Foundation, Via Alassio 11/c, 10126 Torino, Italy.  
 */

package moviemaker;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

/**
 *
 * @author panisson
 */
public class Main {
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {

        Option gexfOption = new Option("g", "gexf", true, "gexf file to import");
        gexfOption.setRequired(true);

        Option outOption = new Option("o", "out", true, "output directory for svg frames");
        outOption.setRequired(true);

        Option startOption = new Option("s", "start", true, "start time value - double format");
        startOption.setRequired(true);

        Option endOption = new Option("e", "end", true, "end time value - double format");
        startOption.setRequired(true);

        Option intervalOption = new Option("i", "interval", true, "filtered interval in seconds");
        startOption.setRequired(true);

        Option stepOption = new Option("t", "step", true, "step between frames in seconds. Default 10");

        Options options = new Options();
        options.addOption(gexfOption);
        options.addOption(outOption);
        options.addOption(startOption);
        options.addOption(endOption);
        options.addOption(intervalOption);
        options.addOption(stepOption);

        CommandLineParser parser = new PosixParser();
        try {
            CommandLine cmd = parser.parse(options, args);
            String gexf = cmd.getOptionValue("g");
            System.out.println("Using gexf file "+gexf);
            String out = cmd.getOptionValue("o");
            System.out.println("Output to folder "+out);

            Double start = Double.valueOf(cmd.getOptionValue("s"));
            System.out.println("Start at "+start);
            Double end = Double.valueOf(cmd.getOptionValue("e"));
            System.out.println("End at "+end);
            int interval = Integer.valueOf(cmd.getOptionValue("i"));
            System.out.println("Interval of "+interval+" seconds");
            int step = Integer.valueOf(cmd.getOptionValue("t", "10"));
            System.out.println("Step of "+step+" seconds");

            Properties prop = new Properties();
            prop.load(new FileReader("preview.properties"));
            //prop.load(prop.getClass().getResourceAsStream("/preview.properties"));
//            prop.setProperty("nodeBorderWidth", "0.0");
//            prop.setProperty("showNodeLabels", "true");
//            prop.setProperty("nodeLabelFontSize", "8");
//            prop.setProperty("edgeScale", "1.0");
//            prop.setProperty("edgeColor", "#ff0000");
            
            GephiController c = new GephiController(prop);
            c.init();
            
            // import gexf file
            File gexfFile = new File(gexf);
            if (!gexfFile.exists()) {
                throw new RuntimeException("GEXF file not found");
            }
            c.importGEXF(gexfFile);
            
            VideoWriter videoWriter = new VideoWriter(out, 800, 600, 50);
            videoWriter.init();

            doIt(c, videoWriter, start, end, interval, step);
            
            videoWriter.close();
            
        } catch (ParseException ex) {
            // automatically generate the help statement
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp( "moviemaker", options );
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public static void doIt(GephiController c, VideoWriter videoWriter, double start, double end, int interval, int step) {

        int min = (int)(start*3600);
        int max = (int)(end*3600);

        for (int i=min; i<=max; i+=step) {
            System.out.println(i/3600.);
            BufferedImage image = c.generateFrame(i/3600., (i+interval)/3600., 800., 600.);
            videoWriter.addFrame(image);

        }
    }
}

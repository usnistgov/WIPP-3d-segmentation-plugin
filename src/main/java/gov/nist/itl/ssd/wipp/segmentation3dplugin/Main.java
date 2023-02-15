/*
 * This software was developed at the National Institute of Standards and
 * Technology by employees of the Federal Government in the course of
 * their official duties. Pursuant to title 17 Section 105 of the United
 * States Code this software is not subject to copyright protection and is
 * in the public domain. This software is an experimental system. NIST assumes
 * no responsibility whatsoever for its use by other parties, and makes no
 * guarantees, expressed or implied, about its quality, reliability, or
 * any other characteristic. We would appreciate acknowledgement if the
 * software is used.
 */
package gov.nist.itl.ssd.wipp.segmentation3dplugin;

import java.io.File;
import java.util.logging.Logger;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PatternOptionBuilder;

/**
 * @author Mylene Simon <mylene.simon at nist.gov>
 *
 */
public class Main {
	
	private static final Logger LOG = Logger.getLogger(
            Main.class.getName());

	public static void main(String[] args) {
		
		Options options = new Options();
		   
		Option helpOption = new Option("h", "help", false,
           "Display this help message and exit.");
		options.addOption(helpOption);
		
		Option inputOption = new Option("i", "inputImages", true,
                "Input images folder - images to segment.");
        inputOption.setRequired(true);
        options.addOption(inputOption);

		Option filenameOption = new Option("f", "filenameFilter", true,
				"Filename filter, enter file extension/suffix (ex: .ome.tif, _ch00.ome.tif).");
		filenameOption.setRequired(false);
		options.addOption(filenameOption);

        Option outputOption = new Option("o", "output", true,
                "Output folder where the segmented images will be saved.");
        outputOption.setRequired(true);
        options.addOption(outputOption);
        
        Option tileSizeOption = new Option("ts", "tileSize", true,
                "Tile size (default 1024).");
        tileSizeOption.setType(PatternOptionBuilder.NUMBER_VALUE);
        options.addOption(tileSizeOption);

        Option thresholdOption = new Option("th", "threshold", true,
                "Thresholding technique.");
		thresholdOption.setRequired(false);
        options.addOption(thresholdOption);

		Option smoothingOption = new Option("s", "smoothing", true,
				"Smooth image before segmentation (default false).");
		smoothingOption.setRequired(false);
		options.addOption(smoothingOption);

		Option morphOpOption = new Option("m", "morphOperations", true,
				"Morphological operations to apply (optional): NO_MORPHOLOGICAL_OPERATIONS = 0, " +
						"CLOSING_FIRST_MORPHOLOGICAL_OPERATIONS = 1, " +
						"OPENING_FIRST_MORPHOLOGICAL_OPERATIONS = 2, " +
						"OPENING_MORPHOLOGICAL_OPERATIONS = 3, " +
						"CLOSING_MORPHOLOGICAL_OPERATIONS = 4, " +
						"DILATE_MORPHOLOGICAL_OPERATIONS = 5, " +
						"ERODE_MORPHOLOGICAL_OPERATIONS = 6.");
		morphOpOption.setRequired(false);
		morphOpOption.setType(PatternOptionBuilder.NUMBER_VALUE);
		options.addOption(morphOpOption);

		Option removeEdgeComponentsOption = new Option("re", "removeEdgeComponents", true,
				"Remove Edge Components (default false).");
		removeEdgeComponentsOption.setRequired(false);
		options.addOption(removeEdgeComponentsOption);

		Option fillHolesOption = new Option("fh", "fillHoles", true,
				"Fill Holes (default false).");
		fillHolesOption.setRequired(false);
		options.addOption(fillHolesOption);

		Option makeSingleComponentOption = new Option("ms", "makeSingleComponent", true,
				"Make Single Component (default false).");
		makeSingleComponentOption.setRequired(false);
		options.addOption(makeSingleComponentOption);
		
		CommandLineParser parser = new DefaultParser();
	       try {
	           CommandLine commandLine = parser.parse(options, args);

	           if (commandLine.hasOption(helpOption.getOpt())) {
	               printHelp(options);
	               return;
	           }
	           
	           String inputImages = commandLine.getOptionValue(inputOption.getOpt());
	
	           String outputFolder = commandLine.getOptionValue(outputOption.getOpt());

	            Number tileSizeNumber = (Number) commandLine.getParsedOptionValue(
	                    tileSizeOption.getOpt());
	            int tileSize = tileSizeNumber == null
	                    ? 1024 : tileSizeNumber.intValue();

			   String filenameFilterValue = commandLine.getOptionValue(
					   filenameOption.getOpt());
			   String filenameFilter = filenameFilterValue == null
					   ? ".ome.tif" : filenameFilterValue;

				String thresholdValue = commandLine.getOptionValue(
						thresholdOption.getOpt());
	            String thresholding = thresholdValue == null
	            		? "Otsu" : thresholdValue;

			   String smoothingValue = commandLine.getOptionValue(
					   smoothingOption.getOpt());
			   boolean smoothing = smoothingValue == null
					   ? false : Boolean.valueOf(smoothingValue).booleanValue();

			   Number morphOpValue = (Number) commandLine.getParsedOptionValue(
					   morphOpOption.getOpt());
			   int morphOp = morphOpValue == null
					   ? 0 : morphOpValue.intValue();

			   String removeEdgeComponentsValue = commandLine.getOptionValue(
					   removeEdgeComponentsOption.getOpt());
			   boolean removeEdgeComponents = removeEdgeComponentsValue == null
					   ? false : Boolean.valueOf(removeEdgeComponentsValue).booleanValue();

			   String fillHolesValue = commandLine.getOptionValue(
					   fillHolesOption.getOpt());
			   boolean fillHolesComponents = fillHolesValue == null
					   ? false : Boolean.valueOf(fillHolesValue).booleanValue();

			   String makeSingleComponentValue = commandLine.getOptionValue(
					   makeSingleComponentOption.getOpt());
			   boolean makeSingleComponent = makeSingleComponentValue == null
					   ? false : Boolean.valueOf(makeSingleComponentValue).booleanValue();
	            
	            try {
	                long start = System.currentTimeMillis();

					Image3DProcessingPipeline pipeline = new Image3DProcessingPipeline();
					pipeline.processImages(inputImages, filenameFilter,
							outputFolder, 1,
							65535, 1, 0, 0,
							0, null, thresholding, 1024, smoothing,
							morphOp, removeEdgeComponents, fillHolesComponents, makeSingleComponent);

	                float duration = (System.currentTimeMillis() - start) / 1000F;
	                LOG.info("Segmentation done in " + duration + "s.");
	            } catch (Exception ex) {
	            	String errorMessage = "Error while segmenting images.";
	                LOG.severe(errorMessage);
					System.exit(1);
					return;
	            }
	            

	       } catch (ParseException ex) {
	    	   LOG.severe(ex.getMessage());
	           printHelp(options);
			   System.exit(1);
			   return;
	       }

	}
	
	/**
     * Print help
     * @param options
     */
    private static void printHelp(Options options) {
        new HelpFormatter().printHelp("wipp-3d-segmentation-plugin", options);
    }

}

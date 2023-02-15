/*
 * This software was developed by employees of the National Institute of 
 * Standards and Technology (NIST), an agency of the Federal Government. 
 * Pursuant to title 17 United States Code Section 105, works of NIST employees 
 * are not subject to copyright protection in the United States and are considered 
 * to be in the public domain. Permission to freely use, copy, modify, and distribute 
 * this software and its documentation without fee is hereby granted, provided that 
 * this notice and disclaimer of warranty appears in all copies.
 * THE SOFTWARE IS PROVIDED 'AS IS' WITHOUT ANY WARRANTY OF ANY KIND, EITHER EXPRESSED, 
 * IMPLIED, OR STATUTORY, INCLUDING, BUT NOT LIMITED TO, ANY WARRANTY THAT THE SOFTWARE 
 * WILL CONFORM TO SPECIFICATIONS, ANY IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS 
 * FOR A PARTICULAR PURPOSE, AND FREEDOM FROM INFRINGEMENT, AND ANY WARRANTY THAT THE 
 * DOCUMENTATION WILL CONFORM TO THE SOFTWARE, OR ANY WARRANTY THAT THE SOFTWARE WILL 
 * BE ERROR FREE. IN NO EVENT SHALL NIST BE LIABLE FOR ANY DAMAGES, INCLUDING, BUT NOT 
 * LIMITED TO, DIRECT, INDIRECT, SPECIAL OR CONSEQUENTIAL DAMAGES, ARISING OUT OF, 
 * RESULTING FROM, OR IN ANY WAY CONNECTED WITH THIS SOFTWARE, WHETHER OR NOT BASED 
 * UPON WARRANTY, CONTRACT, TORT, OR OTHERWISE, WHETHER OR NOT INJURY WAS SUSTAINED BY 
 * PERSONS OR PROPERTY OR OTHERWISE, AND WHETHER OR NOT LOSS WAS SUSTAINED FROM, OR 
 * AROSE OUT OF THE RESULTS OF, OR USE OF, THE SOFTWARE OR SERVICES PROVIDED HEREUNDER.
 */
package gov.nist.itl.ssd.wipp.segmentation3dplugin;

import gov.nist.itl.ssd.wipp.segmentation3dplugin.threshold3D.*;
import gov.nist.itl.ssd.wipp.segmentation3dplugin.utils.BioFormatsUtils;
import ij.ImagePlus;
import ij.measure.Calibration;

import java.io.*;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.logging.Logger;

import ij.process.ImageProcessor;
import loci.formats.FormatException;
import loci.formats.codec.CompressionType;
import loci.formats.in.MetadataOptions;
import loci.formats.ome.OMEXMLMetadata;
import loci.formats.out.OMETiffWriter;
import ome.xml.model.enums.PixelType;
import ome.xml.model.primitives.PositiveInteger;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import gov.nist.itl.ssd.wipp.segmentation3dplugin.segment3D.Image3DCropping;
import gov.nist.itl.ssd.wipp.segmentation3dplugin.segment3D.Image3DSmoothing;
import gov.nist.itl.ssd.wipp.segmentation3dplugin.segment3D.Segment3DImage;


/**
 * @author Mylene Simon <mylene.simon at nist.gov>
 * 
 */
public class Image3DProcessingPipeline {

	private static final Logger LOGGER = Logger.getLogger(Image3DProcessingPipeline.class.getName());

	public Image3DProcessingPipeline() {
	}

	public void processImages(String inputImagesFolder,
			String imagesFileNameExtension, String outputDirectory,
			double thresholdMinimumValue,
			double thresholdMaximumValue, double thresholdStep,
			double voxelDimX, double voxelDimY, double voxelDimZ,
			String voxelDimUnit, String method, int tileSize,
		  	boolean smoothImage,
		  	int morphologicalOperations, boolean removeEdgeComponents,
		  	boolean fillHoles, boolean makeSingleComponent) throws IOException {

		File inputFolder = new File(inputImagesFolder);
		if (inputFolder == null) {
			throw new NullPointerException("Input folder is null");
		}

		File[] images =  inputFolder.listFiles(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return name.toLowerCase().endsWith(imagesFileNameExtension);
			}
		});

		if (images == null || images.length == 0) {
			throw new NullPointerException("Input folder is empty or no images were found.");
		}

		File outputFolder = new File(outputDirectory);
		boolean created = outputFolder.mkdirs();
		if (!created && !outputFolder.exists()) {
			throw new IOException("Can not create folder " + outputFolder);
		}

		try {

			// Starting logs
			LOGGER.info("Starting processing images in the Image3DProcessingPipeline, arguments are:");
			LOGGER.info("inputImagesFolder: " + inputImagesFolder);
			LOGGER.info("imagesFileNameExtension: " + imagesFileNameExtension);
			LOGGER.info("outputDirectory: " + outputDirectory);
			LOGGER.info("thresholdMinimumValue: " + thresholdMinimumValue);
			LOGGER.info("thresholdMaximumValue: " + thresholdMaximumValue);
			LOGGER.info("thresholdStep: " + thresholdStep);
			LOGGER.info("voxelDimX: " + voxelDimX);
			LOGGER.info("voxelDimY: " + voxelDimY);
			LOGGER.info("voxelDimZ: " + voxelDimZ);
			LOGGER.info("voxelDimUnit: " + voxelDimUnit);
			LOGGER.info("smoothImage: " + smoothImage);
			LOGGER.info("morphologicalOperations: " + morphologicalOperations);
			LOGGER.info("removeEdgeComponents: " + removeEdgeComponents);
			LOGGER.info("fillHoles: " + fillHoles);
			LOGGER.info("makeSingleComponent: " + makeSingleComponent);
			LOGGER.info(images.length + " images to process");

			// start time for benchmark
			long startTime = System.currentTimeMillis();

			String inputFilename = new String();
			for(File image : images){
				try {
					// Open ImagePlus object from image sequence and set calibration
					ImagePlus img3D = BioFormatsUtils.readImage(image.getAbsolutePath());
					if(voxelDimUnit != null) {
						Calibration imgCalibration = img3D.getCalibration();
						imgCalibration.pixelWidth = voxelDimX;
						imgCalibration.pixelHeight = voxelDimY;
						imgCalibration.pixelDepth = voxelDimZ;
						imgCalibration.setXUnit(voxelDimUnit);
						imgCalibration.setYUnit(voxelDimUnit);
						imgCalibration.setZUnit(voxelDimUnit);
					}
					
					String shortImageName = image.getName();
					LOGGER.info("Starting processing stack " + shortImageName
							+ " at time: " + new Date().toString());
					
					// smooth image
					if (smoothImage) {
						LOGGER.info("Smoothing image...");
						Image3DSmoothing.grayscaleFlatErosion(img3D, 1, 1, 0);
						Image3DSmoothing.grayscaleFlatDilation(img3D, 1, 1, 0);
					}

					double optThresh = 0.0;
					
					// find opt Threshold (min error)
					if(method.equals("MinError")) {
						MinErrorThresh minErrorThresholding = new MinErrorThresh();
						LOGGER.info("Looking for optimal threshold...");
						optThresh = minErrorThresholding.findThresh(img3D,
								thresholdMinimumValue, thresholdMaximumValue,
								thresholdStep);
						LOGGER.info("Optimal threshold is: " + optThresh);
					}

					else if(method.equals("MaxEntropy")) {
						MaxEntropyThresh maxEntropyThresholding = new MaxEntropyThresh();
						LOGGER.info("Looking for optimal threshold...");
						optThresh = maxEntropyThresholding.findThresh(img3D,
								thresholdMinimumValue, thresholdMaximumValue,
								thresholdStep);
						LOGGER.info("Optimal threshold is: " + optThresh);
					}

						// find opt Threshold (Otsu)
					else if(method.equals("Otsu")) {
						OtsuThresh otsuThresholding = new OtsuThresh();
						LOGGER.info("Looking for optimal threshold...");
						optThresh = otsuThresholding.findThresh(img3D,
								thresholdMinimumValue, thresholdMaximumValue,
								thresholdStep);
						LOGGER.info("Optimal threshold from Otsu is: " + optThresh);
					}
					
					// find opt Threshold (EGT 2DSobel)
					else if(method.equals("EGTSobel2D")) {
						EGTThresh egtThresholding = new EGTThresh();
						LOGGER.info("Looking for optimal threshold...");
						optThresh = egtThresholding.getEGTThrehold(img3D,
								-13);

						LOGGER.info("Optimal threshold from EGT is: " + optThresh);
					}
					
					// find opt Threshold (EGT 3DSobel)
					else if(method.equals("EGTSobel3D")) {
						EGTThresh egtThresholding = new EGTThresh();
						LOGGER.info("Looking for optimal threshold...");
						optThresh = egtThresholding.findThresh(img3D,
								-13, true);

						LOGGER.info("Optimal threshold from EGT is: " + optThresh);
					}
					
					// find opt Threshold (Triangle)
					else if(method.equals("Triangle")) {
						TriangleThresh triangleThresholding = new TriangleThresh();
						LOGGER.info("Looking for optimal threshold...");
						optThresh = triangleThresholding.findThresh(img3D);

						LOGGER.info("Optimal threshold from Triangle is: " + optThresh);
					}
					
					// find opt Threshold (DarkFrames)
					else if(method.equals("DarkFrames")) {
						OtsuThresh otsuThresholding = new OtsuThresh();
						LOGGER.info("Looking for optimal threshold...");
						optThresh = otsuThresholding.findThresh(img3D,
								thresholdMinimumValue, thresholdMaximumValue,
								thresholdStep);
						LOGGER.info("Optimal threshold from Otsu is: " + optThresh);
					}
					else {
						LOGGER.severe("Thresholding method not found.");
						return;
					}
						
					
//					// remove useless frames at beginning and end of stack
//					LOGGER.info("Removing useless frames at beginning and end of stack...");
//					Image3DCropping image3DCropping = new Image3DCropping();
//					int[] framesRange = image3DCropping.removeMeaninglessFrames16bits(img3D, (int)optThresh);
//					LOGGER.info("Starting frame is " + framesRange[0] + " ending frame is " + framesRange[1]);
//					double meanBlackFrames = image3DCropping.getMeanBKGFrames();
//					double stdevBlackFrames = image3DCropping.getStdevBKGFrames();
//					LOGGER.info("Mean intensity of black frames is " + meanBlackFrames + ", stdev intensity of black frames is " + stdevBlackFrames);
//
//					// find opt Threshold (DarkFrames)
//					if(method.equals("DarkFrames")) {
//						double thresholdFromBlackFrames = meanBlackFrames + 4.0 * stdevBlackFrames;
//						if(thresholdFromBlackFrames > 0)
//							optThresh = thresholdFromBlackFrames;
//						LOGGER.info("Optimal threshold from black frames is: " + optThresh);
//					}
					
					// create segment3DImage object with the image
					Segment3DImage segment3DImage = new Segment3DImage(img3D);
					// try to call GC to free unused memory
					img3D = null;
					System.gc();

					// segment image with threshold
					LOGGER.info("Segmenting image (T-E-L)...");
					ImagePlus segmentedImage = segment3DImage
							.segmentImage(
									(int) optThresh,
									morphologicalOperations,
									3,
									removeEdgeComponents,
									fillHoles,
									makeSingleComponent);
					
					// save segmented image in a FITS file
					//Fits3DWriter.write(outputDirectory + File.separatorChar + shortImageName + ".fits", segmentedImage.);
					//Writing the output tiled tiff
					OMEXMLMetadata metadata = BioFormatsUtils.getMetadata(image);
					File outputFile = new File(outputFolder, image.getName());
					try (OMETiffWriter imageWriter = new OMETiffWriter()) {
						metadata.setPixelsType(PixelType.UINT8, 0);
						metadata.setPixelsSignificantBits(PositiveInteger.valueOf("8"), 0);
						imageWriter.setMetadataRetrieve(metadata);
						imageWriter.setTileSizeX(tileSize);
						imageWriter.setTileSizeY(tileSize);
						imageWriter.setInterleaved(metadata.getPixelsInterleaved(0));
						imageWriter.setCompression(CompressionType.LZW.getCompression());
						imageWriter.setId(outputFile.getPath());
						for(int z = 1; z <= segmentedImage.getImageStackSize(); ++ z) {
							ImageProcessor imgProc = segmentedImage.getImageStack().getProcessor(z);
							//metadata.setPixelsType(PixelType.UINT8, z-1);
							//metadata.setPixelsSignificantBits(PositiveInteger.valueOf("8"), z-1);
							imageWriter.saveBytes(z-1, (byte[]) imgProc.getPixels());
						}

					} catch (FormatException | IOException ex) {
						throw new RuntimeException("Unable to write file "
								+ outputFile + ": " + ex.getMessage(), ex);
					}

					// get number of foreground voxels after segmentation
					long frgVoxelCount = segment3DImage.getFRGCount();
					LOGGER.info("Foreground voxel count after segmentation: "
							+ frgVoxelCount);

				} catch (Exception e) {
					LOGGER.severe(e.getMessage());
					throw new RuntimeException(e);
				}

			}

			// end time for benchmark
			long endTime = System.currentTimeMillis();
			LOGGER.info("Image3DProcessingPipeline execution time : "
					+ (endTime - startTime) + " millisecond.");
			System.out.println();

		} catch (Exception e) {
			LOGGER.severe(e.getMessage());
			throw new RuntimeException(e);
		}
	}

}

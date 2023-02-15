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
package gov.nist.itl.ssd.wipp.segmentation3dplugin.threshold3D;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import gov.nist.itl.ssd.wipp.segmentation3dplugin.segment3D.Image3DSmoothing;
import gov.nist.itl.ssd.wipp.segmentation3dplugin.segment3D.Segment3DImage;
import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ImageProcessor;


/**
 * @author peter bajcsy
 * 
 * based on the ImageJ/Fiji implementation
 * 
 * * Automatic thresholding technique based on the entopy of the histogram.
* See: P.K. Sahoo, S. Soltani, K.C. Wong and, Y.C. Chen "A Survey of
* Thresholding Techniques", Computer Vision, Graphics, and Image
* Processing, Vol. 41, pp.233-260, 1988.
*
*
 *
 */
public class MaxEntropyThresh extends Threshold3DImage {

	private static Log _logger = LogFactory
			.getLog(MaxEntropyThresh.class);

	public int [] findThreshPerSlice(ImagePlus img3D, double min, double max, double delta) {
		   
		
/*		int[] hist = imageProcessor.getHistogram();
		int threshold = entropySplit(hist);
		imageProcessor.threshold(threshold);
*/		
		
		//this.segmentedImagePlus = img3d.createImagePlus();
		
		// sanity check
		if (img3D == null) {
			_logger.error("Input image is null, no threshold to be found.");
			return null;
		}
		int numrows = img3D.getHeight();
		int numcols = img3D.getWidth();
		int numzs = img3D.getNSlices();
		int bitDepth = img3D.getBitDepth();
		int numberOfVoxels = numrows * numcols * numzs;
		
		int [] optThresh = new int[numzs];
		
		ImageStack imgStack = img3D.getStack();
		int numberGreyValues = (int) Math.pow(2, bitDepth);
		
		int[] histogram = new int[numberGreyValues];
		
		// compute histogram
		for (int z = 0; z < numzs; z++) {
			ImageProcessor imgProc = imgStack.getProcessor(z+1);
			for(int idx=0;idx<numberGreyValues; idx++){
				histogram[idx]=0;
			}
			for(int x = 0; x < numcols; ++ x) {
				for(int y = 0; y < numrows; ++ y) {
					int temp =imgProc.getPixel(x, y);
					histogram[temp] ++;
				}
			}
			optThresh[z] = entropySplit(histogram);

		}
		
		return optThresh;
	}
	@Override
	public double findThresh(ImagePlus img3D, double min, double max, double delta) {
		   
		
/*		int[] hist = imageProcessor.getHistogram();
		int threshold = entropySplit(hist);
		imageProcessor.threshold(threshold);
*/		
		
		//this.segmentedImagePlus = img3d.createImagePlus();
		
		// sanity check
		if (img3D == null) {
			_logger.error("Input image is null, no threshold to be found.");
			return -1.0;
		}
		int numrows = img3D.getHeight();
		int numcols = img3D.getWidth();
		int numzs = img3D.getNSlices();
		int bitDepth = img3D.getBitDepth();
		//int numberOfVoxels = numrows * numcols * numzs;
		
		double optThresh = 0.0;
		
		ImageStack imgStack = img3D.getStack();
		int numberGreyValues = (int) Math.pow(2, bitDepth);
		
		int[] histogram = new int[numberGreyValues];
		//double[] probabilities = new double[numberGreyValues];
		
		// compute histogram
		for (int z = 0; z < numzs; z++) {
			ImageProcessor imgProc = imgStack.getProcessor(z+1);
			
			for(int x = 0; x < numcols; ++ x) {
				for(int y = 0; y < numrows; ++ y) {
					int temp =imgProc.getPixel(x, y);
					// ignore the background values after a cell mask has been applied
					// consider only values from the foreground of the cell mask
					if(temp != 0)
						histogram[temp] ++;
				}
			}
		}
		
		optThresh = entropySplit(histogram);

		return optThresh;
	}
	
	/**
	  * Calculate maximum entropy split of a histogram.
	  *
	  * @param hist histogram to be thresholded.
	  *
	  * @return index of the maximum entropy split.`
	  */
	 private int entropySplit(int[] hist) {

	   // Normalize histogram, that is makes the sum of all bins equal to 1.
	   double sum = 0;
	   for (int i = 0; i < hist.length; ++i) {
	     sum += hist[i];
	   }
	   if (sum == 0) {
	     // This should not normally happen, but...
	     throw new IllegalArgumentException("Empty histogram: sum of all bins is zero.");
	   }

	   double[] normalizedHist = new double[hist.length];
	   for (int i = 0; i < hist.length; i++) {
	     normalizedHist[i] = hist[i] / sum;
	   }

	   //
	   double[] pT = new double[hist.length];
	   pT[0] = normalizedHist[0];
	   for (int i = 1; i < hist.length; i++) {
	     pT[i] = pT[i - 1] + normalizedHist[i];
	   }

	   // Entropy for black and white parts of the histogram
	   final double epsilon = Double.MIN_VALUE;
	   double[] hB = new double[hist.length];
	   double[] hW = new double[hist.length];
	   for (int t = 0; t < hist.length; t++) {
	     // Black entropy
	     if (pT[t] > epsilon) {
	       double hhB = 0;
	       for (int i = 0; i <= t; i++) {
	         if (normalizedHist[i] > epsilon) {
	           hhB -= normalizedHist[i] / pT[t] * Math.log(normalizedHist[i] / pT[t]);
	         }
	       }
	       hB[t] = hhB;
	     } else {
	       hB[t] = 0;
	     }

	     // White  entropy
	     double pTW = 1 - pT[t];
	     if (pTW > epsilon) {
	       double hhW = 0;
	       for (int i = t + 1; i < hist.length; ++i) {
	         if (normalizedHist[i] > epsilon) {
	           hhW -= normalizedHist[i] / pTW * Math.log(normalizedHist[i] / pTW);
	         }
	       }
	       hW[t] = hhW;
	     } else {
	       hW[t] = 0;
	     }
	   }

	   // Find histogram index with maximum entropy
	   double jMax = hB[0] + hW[0];
	   int tMax = 0;
	   for (int t = 1; t < hist.length; ++t) {
	     double j = hB[t] + hW[t];
	     if (j > jMax) {
	       jMax = j;
	       tMax = t;
	     }
	   }

	   return tMax;
	 }
	
}

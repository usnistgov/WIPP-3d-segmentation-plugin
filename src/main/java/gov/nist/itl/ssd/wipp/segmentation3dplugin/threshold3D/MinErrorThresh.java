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

import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ImageProcessor;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Clustering based model: Minimum error thresholding ��� Ranked #1 according to
 * the thresholding survey paper M. Sezgin and B. Sankur, ���Survey over image
 * thresholding techniques and quantitative performance evaluation,��� Journal of
 * Electronic Imaging 13(1), 146���165 (January 2004).
 * 
 * Reference: Kittler 1986, Cho 1989, Kittler 1985 J. Kittler, J. Illingworth,
 * Minimum Error Thresholding, Pattern Recognition, 19 (1986) 41-47. S. Cho, R.
 * Haralick, S. Yi, Improvement of Kittler and Illingworths���s Minimum Error
 * Thresholding, Pattern Recognition, 22 (1989) 609-617. J. Kittler, J.
 * Illingworth, On Threshold Selection Using Clustering Criteria, IEEE Trans.
 * Systems, Man and Cybernetics, SMC-15 (1985) 652-655.
 * 
 * @author peter bajcsy
 * 
 */
public class MinErrorThresh extends Threshold3DImage {
	private static Log _logger = LogFactory.getLog(MinErrorThresh.class);

	/**
	 * This method finds the optimal threshold following the min error criterion
	 * over a range of thresholds
	 * 
	 * 
	 * @param img3D
	 *            - input 3D image
	 * @param min
	 *            - minimum threshold
	 * @param max
	 *            - maximum threshold (included)
	 * @param delta
	 *            - delta threshold increment
	 * @return - double threshold value
	 * @throws ImageException
	 */
	public double findThresh(ImagePlus img3D, double min, double max, double delta) {

		// sanity check
		if (img3D == null) {
			_logger.error("Missing array of input images");
			return -1.0;
		}
		int numrows = img3D.getHeight();
		int numcols = img3D.getWidth();
		int numzs = img3D.getNSlices();
		//int numbands = img3D.getNChannels();
		//int type = img3D.getBitDepth();
		int bitDepth = img3D.getBitDepth();
		int numberGreyValues = (int) Math.pow(2, bitDepth);

		double thresh = 0.0;
		int numIter = 1 + (int) ((max - min) / delta);
		long[] countBlack = new long[numIter];
		long[] countWhite = new long[numIter];
		int iter = 0;
		double sumW;
		double sumW2;
		double sumB;
		double sumB2;
		long totalNumPixels = numrows * numcols * numzs;
		double p_t, sigmaFRG_t, sigmaBKG_t;
		double[] score = new double[numIter];
		
		ImageStack imgStack = img3D.getStack();
		int[] histogram = new int[numberGreyValues];
		
		// compute histogram
		for (int z = 0; z < numzs; z++) {
			ImageProcessor imgProc = imgStack.getProcessor(z+1);
			
			for(int x = 0; x < numcols; ++ x) {
				for(int y = 0; y < numrows; ++ y) {
					int temp =imgProc.getPixel(x, y);
					histogram[temp] ++;
				}
			}
		}
		
		System.out.println("THRESHOLD,P,SIGMA FRG, SIGMA BKG,SCORE");

		for (thresh = min; thresh <= max; thresh += delta) {
			
			countBlack[iter] = 0;
			countWhite[iter] = 0;
			sumW = 0.0;
			sumW2 = 0.0;
			sumB = 0.0;
			sumB2 = 0.0;
			
			// ImageObject [] intermediate = Threshold3DImage.threshold(img3D,
			// thresh);
			
//			for (int z = 0; z < numzs; z++) {
//				ImageProcessor imgProc = imgStack.getProcessor(z+1);
//				
//				for(int x = 0; x < numcols; ++ x) {
//					for(int y = 0; y < numrows; ++ y) {
//						int temp =imgProc.getPixel(x, y);
//						
//						// FRG pixels are those above the threshold
//						if (temp > thresh) {
//							// white pixels
//							sumW += temp;
//							sumW2 += Math.pow(
//									temp, 2);
//							countWhite[iter]++;
//						} else {
//							// black pixels
//							sumB += temp;
//							sumB2 += Math.pow(
//									temp, 2);
//							countBlack[iter]++;
//						}
//					}
//				}
//			}
			
			for(int i=0; i<= thresh; i++) {
				sumB += histogram[i] * i;
				sumB2 += histogram[i] * Math.pow(i, 2);
				countBlack[iter] += histogram[i];
			}
			
			for(int i=(int)thresh + 1; i< numberGreyValues; i++) {
				sumW += histogram[i] * i;
				sumW2 += histogram[i] * Math.pow(i, 2);
				countWhite[iter] += histogram[i];
			}

			// compute ratio of background/all pixels = P(T)
			// compute sigma of BKG and sigma of FRG
			double epsilon = 0.000001;
			
			p_t = (double) countBlack[iter] / totalNumPixels;
			if (countBlack[iter] > 0 && countWhite[iter] > 0) {
				sigmaBKG_t = Math.sqrt(sumB2 * countBlack[iter]
						- sumB * sumB)
						/ (double) countBlack[iter];
				sigmaFRG_t = Math.sqrt(sumW2 * countWhite[iter]
						- sumW * sumW)
						/ (double) (countWhite[iter]);
				/*System.out.print("P[" + thresh + "]=" + p_t);
				System.out.print(" , ");
				System.out.print("sigmaFRG[" + thresh + "]=" + sigmaFRG_t);
				System.out.print(" , ");
				System.out.print("sigmaBKG[" + thresh + "]=" + sigmaBKG_t);
				System.out.print(" , ");*/
			

				if ((totalNumPixels - countBlack[iter]) != countWhite[iter]) {
					System.out.println("error? "
							+ (totalNumPixels - countBlack[iter])
							+ ", " + countWhite[iter]);
				}
				
				if (sigmaBKG_t < epsilon || sigmaFRG_t < epsilon
						|| p_t < epsilon || (1 - p_t) < epsilon) {

					score[iter] = Double.MAX_VALUE;
				} else {

					score[iter]= p_t * Math.log10(sigmaBKG_t)
							+ (1 - p_t) * Math.log10(sigmaFRG_t) - p_t
							* Math.log10(p_t) - (1 - p_t)
							* Math.log10(1 - p_t);
				}
				/*System.out.println("score[" + iter + "]="
						+ score[iter]);*/
				
				System.out.println(thresh + "," + p_t + "," + sigmaFRG_t + "," + sigmaBKG_t + "," + score[iter]);
			} else {
				score[iter] = Double.MAX_VALUE;
			}

			

			iter++;
		}

		// find global minimum
		// Note: this minScore should be an array of length numbands.
		// however, in the current implementation the min is found over all
		// bands
		double optGlobalThresh = -1.0;
		double minGlobalScore = Double.MAX_VALUE;
		for (iter = 0; iter < numIter; iter++) {
			
			if (minGlobalScore > score[iter]) {
				minGlobalScore = score[iter];
				optGlobalThresh = min + iter * delta;
			}
			System.out.print("Global score[" + iter + "]="
					+ score[iter]);
			
			System.out.println();
		}
		double optLocalThresh = -1.0;
		double val1, val2, val3;
		int j;
		// find min local score
		double minLocalScore = Double.MAX_VALUE;
		for (iter = 1; iter < numIter-1; iter++) {
			if(score[iter-1] == Double.MAX_VALUE || score[iter] == Double.MAX_VALUE || score[iter+1] == Double.MAX_VALUE){
				continue;
			}
			val1 = score[iter-1];
			val2 = score[iter];
			val3 = score[iter+1];
			j=iter;
			if(val1 == val2 ){
				while(j<numIter-2 && val1==val2){
					j++;
					val2 = score[j];					
				}
				iter = j;
				val3 = score[j+1];
			}
			if(val2 == val3 ){
				j++;
				while(j<numIter-1 && val2==val3){
					j++;
					val3 = score[j];					
				}
			}
	
			if (minLocalScore > val2 && val2< val1 && val2 < val3) {
				minLocalScore = score[iter];
				optLocalThresh = min + iter * delta;
			}
		}
		double optThresh = -1;
		if(optLocalThresh <0){
			// CASE: it did not find local threshold
			if(optGlobalThresh<0){
				// CASE: it did not find neither local nor global threshold
				// this occurs with noise free images
				optGlobalThresh = 0;
			}
			optThresh = optGlobalThresh;
			System.out.println("optGlobalThresh=" + optThresh);
		}else{
			//CASE: it did find local minima
			optThresh = optLocalThresh;
			System.out.println("optLocalThresh=" + optThresh);
		}

		return optThresh;
	}

}

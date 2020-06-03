import ij.plugin.Filters3D;
import ij.plugin.GaussianBlur3D;
import ij.plugin.HyperStackConverter;
import ij.plugin.PlugIn;
import ij.plugin.filter.GaussianBlur;
import ij.plugin.filter.RankFilters;
import ij.process.*;
import ij.gui.GenericDialog;
import ij.gui.Roi;
import ij.io.OpenDialog;
import ij.IJ;

import java.util.Arrays;
import ij.ImageStack;
import ij.ImagePlus;

/**
 * The Canny-Edge detector part of this plug-in is largely inspired from Tom Gibara's algorithm available at :
 *  http://www.tomgibara.com/computer-vision/canny-edge-detector
 *
 */



public class Preprocessing_2D implements PlugIn {
	
		
	// Variables declaration
	private int height;
	private int width;
	private int NFrames;
	private int NSlices;
	private int bitdepth;
	private int picsize;
	private String dir;
	private int[] data;
	private int[] magnitude;
	private ImagePlus sourceImage;
	
	
	private float gaussianKernelRadius;
	private float lowThreshold ;
	private float highThreshold;
	private int gaussianKernelWidth;
	private boolean contrastNormalized;
	
	private float[] xConv;
	private float[] yConv;
	private float[] xGradient;
	private float[] yGradient;
	
	private final static float GAUSSIAN_CUT_OFF = 0.005f;
	private final static float MAGNITUDE_SCALE = 100F;
	private final static float MAGNITUDE_LIMIT = 1000F;
	private final static int MAGNITUDE_MAX = (int) (MAGNITUDE_SCALE * MAGNITUDE_LIMIT);

	public void run(String arg) {
		
		// Loading the original image
		ImagePlus in = IJ.getImage().duplicate();
		
		// Getting the directory to store the intermediate images with the original one
		OpenDialog od = new OpenDialog("Choose the original image", null);  
	    dir = od.getDirectory();
	    
	    // Getting the dimensions of the images we are working with
		width = in.getWidth();
		height = in.getHeight();
		NFrames = in.getNFrames();
		NSlices = in.getNSlices();
		bitdepth = in.getBitDepth();
		
		
		// Denoising the original image and saving the denoised image
		IJ.log("Denoising...");
		ImagePlus Denoise = median2D (in);
		Denoise = gaussian2D(Denoise);
		IJ.saveAs(Denoise, "Tiff", dir +"/denoised.tif");
		Denoise.show();
		
				
		// Detecting the shell of the embryo, creating a mask and saving it
		IJ.log("Shell detection...");
		ImagePlus Shell = shell (in);
		IJ.saveAs(Shell, "Tiff", dir +"/shell.tif");
		Shell.show();
		
		
		
		// Canny-Edges detection 
		IJ.log("Canny-Edges detection...");
		ImagePlus CEdges = CannyEdges (in);
		IJ.saveAs(CEdges, "Tiff", dir +"/edges.tif");
		CEdges.show();
		IJ.log("All done !");
				
				
		
	}
		
	
	public  ImagePlus CannyEdges (ImagePlus in) {
		ImageStack output = new ImageStack();
		if (!showDialog())
			return in;
		for (int t=1; t<=NFrames; t++) {
			ImagePlus sourceImage = makesubstack(in,t);					
			process(sourceImage);
			ImageProcessor ip = new FloatProcessor(width, height, data);
			ip = ip.convertToByte(false);				
			sourceImage.setProcessor(ip);
			output.addSlice(ip);
			}
		ImagePlus out = new ImagePlus();		
		out.setStack("Edges", output);
		out = HyperStackConverter.toHyperStack(out, 1, 1, NFrames, "Color");
		return out;
	}
	
	public ImagePlus process(ImagePlus imp) {
		sourceImage = imp;
		process();
		return new ImagePlus("Edges_"+imp.getTitle());
	}
	
	private boolean showDialog() {
		GenericDialog gd = new GenericDialog("Canny Edge Detector");
		gd.addNumericField("Gaussian kernel radius:", 2, 1);
		gd.addNumericField("Low threshold:", 4, 1);
		gd.addNumericField("High threshold:", 8, 1);
		gd.addNumericField("Gaussian Kernel width:", 8, 0);
		gd.addCheckbox("Normalize contrast ", contrastNormalized);
		gd.showDialog();
		if (gd.wasCanceled())
			return false;
		gaussianKernelRadius = (float)gd.getNextNumber();
		if (gaussianKernelRadius<0.1f)
			gaussianKernelRadius = 0.1f;
		lowThreshold = (float)gd.getNextNumber();
		if (lowThreshold<0.1f)
			lowThreshold = 0.1f;
		highThreshold = (float)gd.getNextNumber();
		if (highThreshold<0.1f)
			highThreshold = 0.1f;
		gaussianKernelWidth = (int)gd.getNextNumber();
		contrastNormalized = gd.getNextBoolean();
		return true;
	}
	
	public void setLowThreshold(float threshold) {
		if (threshold < 0) throw new IllegalArgumentException();
		lowThreshold = threshold;
	}
	
	public void setHighThreshold(float threshold) {
		if (threshold < 0) throw new IllegalArgumentException();
		highThreshold = threshold;
	}

	public void setGaussianKernelWidth(int gaussianKernelWidth) {
		if (gaussianKernelWidth < 2) throw new IllegalArgumentException();
		this.gaussianKernelWidth = gaussianKernelWidth;
	}
	
	public void setGaussianKernelRadius(float gaussianKernelRadius) {
		if (gaussianKernelRadius < 0.1f) throw new IllegalArgumentException();
		this.gaussianKernelRadius = gaussianKernelRadius;
	}
	
	public void setContrastNormalized(boolean contrastNormalized) {
		this.contrastNormalized = contrastNormalized;
	}
	
	public void process() {
		picsize = width * height;
		initArrays();
		readLuminance();
		if (contrastNormalized) normalizeContrast();
		computeGradients(gaussianKernelRadius, gaussianKernelWidth);
		int low = Math.round(lowThreshold * MAGNITUDE_SCALE);
		int high = Math.round( highThreshold * MAGNITUDE_SCALE);
		performHysteresis(low, high);
		thresholdEdges();
	}
	
	private void initArrays() {
		if (data == null || picsize != data.length) {
			data = new int[picsize];
			magnitude = new int[picsize];

			xConv = new float[picsize];
			yConv = new float[picsize];
			xGradient = new float[picsize];
			yGradient = new float[picsize];
		}
	}
	
	private void computeGradients(float kernelRadius, int kernelWidth) {
		
		//generate the gaussian convolution masks
		float kernel[] = new float[kernelWidth];
		float diffKernel[] = new float[kernelWidth];
		int kwidth;
		for (kwidth = 0; kwidth < kernelWidth; kwidth++) {
			float g1 = gaussian(kwidth, kernelRadius);
			if (g1 <= GAUSSIAN_CUT_OFF && kwidth >= 2) break;
			float g2 = gaussian(kwidth - 0.5f, kernelRadius);
			float g3 = gaussian(kwidth + 0.5f, kernelRadius);
			kernel[kwidth] = (g1 + g2 + g3) / 3f / (2f * (float) Math.PI * kernelRadius * kernelRadius);
			diffKernel[kwidth] = g3 - g2;
		}

		int initX = kwidth - 1;
		int maxX = width - (kwidth - 1);
		int initY = width * (kwidth - 1);
		int maxY = width * (height - (kwidth - 1));
		
		//perform convolution in x and y directions
		for (int x = initX; x < maxX; x++) {
			for (int y = initY; y < maxY; y += width) {
				int index = x + y;
				float sumX = data[index] * kernel[0];
				float sumY = sumX;
				int xOffset = 1;
				int yOffset = width;
				for(; xOffset < kwidth ;) {
					sumY += kernel[xOffset] * (data[index - yOffset] + data[index + yOffset]);
					sumX += kernel[xOffset] * (data[index - xOffset] + data[index + xOffset]);
					yOffset += width;
					xOffset++;
				}				
				yConv[index] = sumY;
				xConv[index] = sumX;
			} 
		}
		
		// Calculating the gradient in x and y directions
		for (int x = initX; x < maxX; x++) {
			for (int y = initY; y < maxY; y += width) {
				float sum = 0f;
				int index = x + y;
				for (int i = 1; i < kwidth; i++)
					sum += diffKernel[i] * (yConv[index - i] - yConv[index + i]);
 
				xGradient[index] = sum;
			}
 
		}
		for (int x = kwidth; x < width - kwidth; x++) {
			for (int y = initY; y < maxY; y += width) {
				float sum = 0.0f;
				int index = x + y;
				int yOffset = width;
				for (int i = 1; i < kwidth; i++) {
					sum += diffKernel[i] * (xConv[index - yOffset] - xConv[index + yOffset]);
					yOffset += width;
				}
 
				yGradient[index] = sum;
			}
 
		}

 
		initX = kwidth;
		maxX = width - kwidth;
		initY = width * kwidth;
		maxY = width * (height - kwidth);
		for (int x = initX; x < maxX; x++) {
			for (int y = initY; y < maxY; y += width) {
				int index = x + y;
				int indexN = index - width;
				int indexS = index + width;
				int indexW = index - 1;
				int indexE = index + 1;
				int indexNW = indexN - 1;
				int indexNE = indexN + 1;
				int indexSW = indexS - 1;
				int indexSE = indexS + 1;
				
				float xGrad = xGradient[index];
				float yGrad = yGradient[index];
				float gradMag = hypot(xGrad, yGrad);

				//perform non-maximal supression
				float nMag = hypot(xGradient[indexN], yGradient[indexN]);
				float sMag = hypot(xGradient[indexS], yGradient[indexS]);
				float wMag = hypot(xGradient[indexW], yGradient[indexW]);
				float eMag = hypot(xGradient[indexE], yGradient[indexE]);
				float neMag = hypot(xGradient[indexNE], yGradient[indexNE]);
				float seMag = hypot(xGradient[indexSE], yGradient[indexSE]);
				float swMag = hypot(xGradient[indexSW], yGradient[indexSW]);
				float nwMag = hypot(xGradient[indexNW], yGradient[indexNW]);
				float tmp;
				/* Here we perform the non-maximum suppression : we only keep 
				 * a given point as an edge if it is a local maximum of the gradient 
				 * magnitude in its direction.
				 * 
				 * 
				 * We need to break the comparison into a number of different
				 * cases depending on the gradient direction so that the
				 * appropriate values can be used. To avoid computing the
				 * gradient direction, we use two simple comparisons: first we
				 * check that the partial derivatives have the same sign (1)
				 * and then we check which is larger (2). As a consequence, we
				 * have reduced the problem to one of four identical cases that
				 * each test the central gradient magnitude against the values at
				 * two points with 'identical support'; what this means is that
				 * the geometry required to accurately interpolate the magnitude
				 * of gradient function at those points has an identical
				 * geometry (upto right-angled-rotation/reflection).
				 * 
				 * When comparing the central gradient to the two interpolated
				 * values, we avoid performing any divisions by multiplying both
				 * sides of each inequality by the greater of the two partial
				 * derivatives. The common comparand is stored in a temporary
				 * variable (3) and reused in the mirror case (4).
				 * 
				 */
				if (xGrad * yGrad <= (float) 0 /*(1)*/
					? Math.abs(xGrad) >= Math.abs(yGrad) /*(2)*/
						? (tmp = Math.abs(xGrad * gradMag)) >= Math.abs(yGrad * neMag - (xGrad + yGrad) * eMag) /*(3)*/
							&& tmp > Math.abs(yGrad * swMag - (xGrad + yGrad) * wMag) /*(4)*/
						: (tmp = Math.abs(yGrad * gradMag)) >= Math.abs(xGrad * neMag - (yGrad + xGrad) * nMag) /*(3)*/
							&& tmp > Math.abs(xGrad * swMag - (yGrad + xGrad) * sMag) /*(4)*/
					: Math.abs(xGrad) >= Math.abs(yGrad) /*(2)*/
						? (tmp = Math.abs(xGrad * gradMag)) >= Math.abs(yGrad * seMag + (xGrad - yGrad) * eMag) /*(3)*/
							&& tmp > Math.abs(yGrad * nwMag + (xGrad - yGrad) * wMag) /*(4)*/
						: (tmp = Math.abs(yGrad * gradMag)) >= Math.abs(xGrad * seMag + (yGrad - xGrad) * sMag) /*(3)*/
							&& tmp > Math.abs(xGrad * nwMag + (yGrad - xGrad) * nMag) /*(4)*/
					) {
					magnitude[index] = gradMag >= MAGNITUDE_LIMIT ? MAGNITUDE_MAX : (int) (MAGNITUDE_SCALE * gradMag);
					//Here, we don't take the orientation into account
				} else {
					magnitude[index] = 0;
				}
			}
		}
	}
	
	private float hypot(float x, float y) {
		return (float) Math.hypot(x, y);
	}

	private float gaussian(float x, float sigma) {
		return (float) Math.exp(-(x * x) / (2f * sigma * sigma));
	}

	private void performHysteresis(int low, int high) {
		Arrays.fill(data, 0); 
		int offset = 0;
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				if (data[offset] == 0 && magnitude[offset] >= high) {
					follow(x, y, offset, low);
				}
				offset++;
			}
		}
 	}

	private void follow(int x1, int y1, int i1, int threshold) {
		int x0 = x1 == 0 ? x1 : x1 - 1;
		int x2 = x1 == width - 1 ? x1 : x1 + 1;
		int y0 = y1 == 0 ? y1 : y1 - 1;
		int y2 = y1 == height -1 ? y1 : y1 + 1;
		
		data[i1] = magnitude[i1];
		for (int x = x0; x <= x2; x++) {
			for (int y = y0; y <= y2; y++) {
				int i2 = x + y * width;
				if ((y != y1 || x != x1)
					&& data[i2] == 0 
					&& magnitude[i2] >= threshold) {
					follow(x, y, i2, threshold);
					return;
				}
			}
		}
	}

	private void thresholdEdges() {
		for (int i = 0; i < picsize; i++) {
			data[i] = data[i] > 0 ? 255 : 0;
			}
	}

	
	private void readLuminance() {
		ImageProcessor ip = sourceImage.getProcessor();
		ip = ip.convertToByte(true);
		for (int i=0; i<ip.getPixelCount(); i++)
			data[i] = ip.get(i);
	}

	private void normalizeContrast() {
		int[] histogram = new int[256];
		for (int i = 0; i < data.length; i++) {
			histogram[data[i]]++;
		}
		int[] remap = new int[256];
		int sum = 0;
		int j = 0;
		for (int i = 0; i < histogram.length; i++) {
			sum += histogram[i];
			int target = sum*255/picsize;
			for (int k = j+1; k <=target; k++) {
				remap[k] = i;
			}
			j = target;
		}		
		for (int i = 0; i < data.length; i++) {
			data[i] = remap[data[i]];
		}		
	}

	public ImagePlus makesubstack (ImagePlus in, int t) {
		ImageStack stack = in.getStack();
		ImageStack stack2 = null;
		boolean virtualStack = stack.isVirtual();
		double min = in.getDisplayRangeMin();
		double max = in.getDisplayRangeMax();
		Roi roi = in.getRoi();
		int currSlice = t;
			ImageProcessor ip2 = stack.getProcessor(currSlice);
			ip2.setRoi(roi);
			ip2 = ip2.crop();
			if (stack2==null)
				stack2 = new ImageStack(width, height);
			stack2.addSlice(stack.getSliceLabel(currSlice), ip2);
			in.setStack(stack);		
		ImagePlus impSubstack = in.createImagePlus();
		impSubstack.setStack("", stack2);
		if (virtualStack)
			impSubstack.setDisplayRange(min, max);
		return impSubstack;
	}
	
	public double[][] neighborhood (ImagePlus in, int x, int y, int t, int L){
		double b [][]  = new double [L][L];
		int l= (L-1)/2;
		in.setPosition(1,1,t);
		ImageProcessor ip = in.getProcessor();
		if (x>l && x<(width-l)) {
			if( y>l && y<(height-l)) {
			for (int i=0; i<L; i++) {
				for (int j=0; j<L; j++) {					
						b[i][j] = ip.getPixelValue(x-l+i, y-l+j);
						}
						
					}					
				}
			}
				
		return b;
	}
	
	

	public boolean[][] ball(int size) {
		 boolean mask[][] = new boolean[size][size];
		 int r = size/2;
		 for(int i =0; i<size; i++) {
			 for (int j=0; j<size; j++) {
					int x = i-r;
					int y = j-r;
					if (x*x+y*y<=r*r) {
						mask[i][j]=true;
					}
					else {
						mask[i][j]=false;
					}
				 }				 
		 }
		 return mask;
	}
	
	public ImagePlus dilation (ImagePlus in, boolean mask[][]) {
		 int size = mask.length;
		 ImagePlus output = IJ.createHyperStack("Out", width, height, 1, NSlices, NFrames, bitdepth);
		 for (int t=1; t<=NFrames; t++) {
			 for (int x=0; x<width; x++) {
				 for (int y=0; y<height; y++) {
						 double [][] block = neighborhood (in, x, y, t, size);
						 double max = -Double.MAX_VALUE;
						 for (int i=0; i<size; i++) {
							 for (int j=0; j<size; j++) {
									 if (mask[size-1-i][size-1-j]== true) {
										 max = Math.max(block[i][j], max);
									 }
							 }
						 }
						 output.setPosition(1,1,t);
						 ImageProcessor ip = output.getProcessor();
						 ip.putPixelValue(x,y,max);		
						 }					 
				 }					 
		}
		 return output;
	}

	public ImagePlus erosion(ImagePlus in, boolean mask[][]) {
		 int size = mask.length;
		 ImagePlus output = IJ.createHyperStack("Out", width, height, 1, 1, NFrames, bitdepth);
		 for (int t=1; t<=NFrames; t++) {
			 for (int x=0; x<width; x++) {
				 for (int y=0; y<height; y++) {
					  double [][] block = neighborhood (in, x, y, t, size);
					  double min = Double.MAX_VALUE;
						 for (int i=0; i<size; i++) {
							 for (int j=0; j<size; j++) {
								 if (mask[i][j]==true) {
										 min = Math.min(block[i][j], min);
									 }
								 }
							 }
					  output.setPosition(1,1,t);
					  ImageProcessor ip = output.getProcessor();
					  ip.putPixelValue(x,y,min);	
					 }
				 }				
		 }
		 return output;
	}

	public ImagePlus close(ImagePlus in, boolean mask[][]) {
		ImagePlus temp = dilation (in, mask);
		ImagePlus out = erosion (temp, mask);
		 return out;
		 }

	public ImagePlus shell (ImagePlus in ) {
		ImagePlus temp = median2D (in);
		IJ.setAutoThreshold(temp, "Huang dark"); 
		IJ.run(temp, "Convert to Mask", "method=Huang background=Dark calculate black");
		ImagePlus output = median2D(temp);
		//output = HyperStackConverter.toHyperStack(output, 1, 1, NFrames, "Color");
		output = close (output, ball(7)); // Change the size of the closing to adapt the shape of the shell
		return output;
		
	}
	
	public ImagePlus median2D (ImagePlus in) {
		ImagePlus out = in.duplicate();
		for(int t=1; t<=out.getNFrames(); t++) {
			out.setPosition(1,1,t);
			ImageProcessor ip = out.getProcessor();
			RankFilters rf = new RankFilters();
			rf.rank(ip,2,RankFilters.MEDIAN);
		}		
		return out;
	}
	
	public ImagePlus gaussian2D (ImagePlus in) {
		ImagePlus out = in.duplicate();
		for(int t=1; t<=out.getNFrames(); t++) {
			out.setPosition(1,1,t);
			ImageProcessor ip = out.getProcessor();
			GaussianBlur gb = new GaussianBlur();
			gb.blurGaussian(ip, 1.25, 1.25, 0.01);
		}		
		return out;
	}

}

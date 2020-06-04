import java.awt.Color;
import java.awt.Polygon;
import java.util.ArrayList;
import java.util.Random;
import java.lang.Math;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.gui.Plot;
import ij.gui.Roi;
import ij.measure.ResultsTable;
import ij.plugin.PlugIn;
import ij.plugin.filter.GaussianBlur;
import ij.plugin.filter.RankFilters;
import ij.process.ImageProcessor;

public class Region_Growing2D implements PlugIn {
	
	public void run(String arg) {
			
		// --- ----------- ---
		// --->INPUT IMAGE<---
		// --- ----------- ---
		
		// Fetch input image
		ImagePlus in = IJ.getImage();
		
		// Get image dimensions 
		int nx = in.getWidth();
		int ny = in.getHeight();
		int nt = in.getNFrames();
		int nz = in.getNSlices();
		int b = in.getBitDepth();
		
		// --- ------------ ---
		// --->INPUT DIALOG<---
		// --- ------------ ---
		
		// Generate Dialog for Region Growing parameter input
		GenericDialog dlg = new GenericDialog("Region Growing");
		
		dlg.addNumericField("Max number of iterations", 50, 1);
		dlg.addNumericField("Max Mean Difference (graylevel)", 175, 1);
		dlg.addNumericField("Threshold (graylevel)", 4500, 1);
		dlg.addNumericField("Max Distance (um)", 35, 1);
		
		dlg.showDialog();
		
		if (dlg.wasCanceled()) return;
		
		int itermax = (int) dlg.getNextNumber();
		double tol = dlg.getNextNumber();
		double thr = dlg.getNextNumber();
		double dis = dlg.getNextNumber();
		dis = dis * dis;  // squared here to prevent slow square root in distance function
		
		IJ.log("Chosen parameters: tol=" + Double.toString(tol) + ", T=" + Double.toString(thr) + ", d=" + Double.toString(dis));;;
		
	
		// --- --------- ---
		// --->DENOISING<---
		// --- --------- ---
		IJ.log("Denoising the original image...");
		ImagePlus denoised = median2D(in, 2);
		
		
		// --- ------------ ---
		// --->OUTPUT IMAGE<---
		// --- ------------ ---
		
		// Create output segmentation image
		ImagePlus areas = IJ.createImage("Regions", nx, ny, nt, b);
		
		Roi mroi = in.getRoi();
		if (mroi == null) {
			IJ.log("Roi is null. Please try again.");
			return;
		}
		
		Polygon up = mroi.getPolygon();
		
		in = denoised;
		
		// Number of initial regions
		int nri = up.xpoints.length;
		
		IJ.log("Retrieving " + nri + " selected seeds.");
		
		int slice = in.getSlice();
		int frame = in.getFrame();
		
		IJ.log("Current slice is: " + slice);
		IJ.log("Current frame is: " + frame);
		
		ImageProcessor imp = in.getProcessor();
		ImageProcessor oup = areas.getProcessor();
		
		ArrayList<Point2D> initialSeeds = new ArrayList<Point2D>();
		
		for (int i = 0; i < nri; i++){
			
			Point2D seed = new Point2D(up.xpoints[i], up.ypoints[i], frame, imp.getPixelValue(up.xpoints[i], up.ypoints[i]));
			initialSeeds.add(seed);
			
		}
		
		// --- ------------------ ---
		// --->REGION CALCULATION<---
		// --- ------------------ ---
		
		ArrayList<TimeFrame> timeFrames = new ArrayList<TimeFrame>();
		
		// To suppress error of uninitialized local variable
		ArrayList<Point2D> seeds = new ArrayList<Point2D>();
	
		// Descending through time
		for (int t = frame; t > 0; t--) {
				
			// Get seeds
			seeds = (t == frame) ? initialSeeds : getNextSeeds(seeds, in, 
					timeFrames.get(timeFrames.size() - 1), slice, t, thr, true);
			
			IJ.log('\n' + "Frame " + t + ": " + seeds.size() + " seeds.");
			
			// Set time frame
			in.setPosition(1, slice, t);
			areas.setPosition(1, t, 1);
			
			imp =    in.getProcessor();
			oup = areas.getProcessor();
			
			// Get Regions 
			TimeFrame currentTimeFrame = new TimeFrame(
					getRegions(seeds, imp, oup, itermax, thr, tol, dis, t)
			);
			
			timeFrames.add(currentTimeFrame);
			
		}
		
		areas.setPosition(1, slice, nt);
		areas.getProcessor().resetMinAndMax();
		areas.show();
		
		surfaceCalculation(areas);

		
	}
	
	/*!
     * @brief Region Growing algorithm in 2D
     *
     * @param seeds
     * @param imp input image
     * @param oup output image
     * @param itermax maximum number of iterations
     * @param thr threshold
     * @param tol mean difference tolerance
     * @param dis distance tolerance factor
     * @param frame
     * 
     * @return newSeeds list of new seeds
     * 
     * @note runs getRegions() with verbose=false
     */
	public ArrayList<Region2D> getRegions(
			ArrayList<Point2D> seeds, ImageProcessor imp, ImageProcessor oup, 
			int itermax, double thr, double tol, double dis,
			int frame) {
		return this.getRegions(seeds, imp, oup, itermax, thr, tol, dis, frame, false);
	}
	
	
	/*!
     * @brief Region Growing algorithm in 2D
     *
     * @param seeds
     * @param imp input image
     * @param oup output image
     * @param itermax maximum number of iterations
     * @param thr threshold
     * @param tol mean difference tolerance
     * @param dis distance tolerance factor
     * @param frame
     * @param verbose output some details
     * 
     * @return newSeeds list of new seeds
     */
	public ArrayList<Region2D> getRegions(
			ArrayList<Point2D> seeds, ImageProcessor imp, ImageProcessor oup, 
			int itermax, double thr, double tol, double dis,
			int frame, boolean verbose) {
		
		ArrayList<Region2D> regions = new ArrayList<Region2D>();
		
		for (int s = 0; s < seeds.size(); s++) {
			
			Region2D region = new Region2D(seeds.get(s));
			regions.add(region);
			
			oup.putPixelValue(seeds.get(s).getX(), seeds.get(s).getY(), 55 + (200.0 / seeds.size()) * s);
			
			if (verbose)
				IJ.log("Region created with seed: " + region.getSeed().toString());
			
		}
		
		
		// While loop for number of iterations
		int iter = 0;
		while (iter < itermax) {
			
			boolean nothingAdded = true;
			
			if (verbose)
				IJ.log("Iteration " + Integer.toString(iter));
			
			// Loop over each Region
			for (int i = 0; i < regions.size(); i++) {
				
				if (verbose)
					IJ.log("Looping over region " + Integer.toString(i));
				
				Region2D region = regions.get(i);
				int s = region.size();
				
				// Iterate over all points in region
				for (int p = 0; p < s; p++) {
					
					if (verbose)
						IJ.log("Looping over point " + region.getPoint(p).toString());
					
					int x = (int) region.getPoint(p).getX();
					int y = (int) region.getPoint(p).getY();		
					
					// Loop over x and y neighbors subsequently
					for (int m = - 1; m <= 1; m++)
					for (int n = - 1; n <= 1; n++) {
						
						if (m == 0 && n == 0)
							continue;
										
						// get comparison pixel
						double pixel = imp.getPixelValue(x + m, y + n);
						
						Point2D eval = new Point2D(x + m, y + n, frame, pixel);
						
						if (verbose)
							IJ.log("Evaluating pixel: " + eval.toString());
						
						/* HARD CONDITIONS 
						 * (1) Check that newly evaluated pixel does not overlap with other regions
						 *     |— done by checking value of output image (if black - not written upon)
						 * (2) Check that pixel value is under user threshold
						 * (3) Check vicinity for growing spherically
						 *     |- TODO includes Star convex with respect to seed
						 * (4) Pixel is in shell (done 17 lines above)
						 */	
						int amount = (iter < itermax / 2) ? 1 : 3;
						
						if (oup.getPixelValue(x + m, y + n) != 0.0 || pixel >= thr || !region.isInVicinity(eval, amount))
							continue;
						
						/* SOFT CONDITIONS
						 * (1) Distance to seed
						 * (2) Pixel value compared to region's mean value
						 */
						double cost = 0.5 * sigmoid(region.getDistanceToSeed(eval), dis, dis / 5.0) + 
									  0.5 * sigmoid(Math.abs(pixel - region.getMean()), tol, tol / 5.0);
						
						if (verbose)
							IJ.log("Cost: " + Double.toString(cost));
						
						
						if (cost > 0.5) {
							
							nothingAdded = false;
						
							if (verbose)
								IJ.log("Point added: " + eval.toString());
							
							region.addPoint(eval);
							
							// [!!] Putting the output pixel just for overlap checking
							oup.putPixelValue(x + m, y + n, 55 + (200.0 / regions.size()) * i);
							
						}
					}
				}	
				
			}
			
			if (nothingAdded) {
				iter = 10000;
				if (verbose)
					IJ.log("Nothing added. Stopping algorithm.");
			}
			
			iter++;
			
		}
		
		return regions;
		
	}
	
	/*!
     * @brief Fetches seeds in frame based on previous seeds
     *
     * @param seeds previous seeds
     * @param in input image
     * @param timeFrame current time frame containing previous regions
     * @param slice 
     * @param frame
     * @param thr threshold
     * 
     * @return newSeeds list of new seeds
     * 
     * @note calls fuller function with verbose=false
     */
	public ArrayList<Point2D> getNextSeeds(
			ArrayList<Point2D> seeds, ImagePlus in, TimeFrame timeFrame,
			int slice, int frame, double thr) {
		return getNextSeeds(seeds, in, timeFrame, slice, frame, thr, false);
	}
	
	/*!
     * @brief Fetches seeds in frame based on previous seeds
     *
     * @param seeds previous seeds
     * @param in input image
     * @param timeFrame current time frame containing previous regions
     * @param slice 
     * @param frame
     * @param thr threshold
     * @param verbose output some details
     * 
     * @return newSeeds list of new seeds
     */
	public ArrayList<Point2D> getNextSeeds(
			ArrayList<Point2D> seeds, ImagePlus in, TimeFrame timeFrame,
			int slice, int frame, double thr, boolean verbose) {
	
		in.setPosition(1, slice, frame);
		ImageProcessor imp = in.getProcessor();
		
		ArrayList<Point2D> newSeeds = new ArrayList<Point2D>();
		boolean[][] merge = new boolean[seeds.size()][seeds.size()];
		
		// Hack to get equivalent of itertools.combinations from Python (luckily only pairwise)
		for (int indexSeedOne = 0; indexSeedOne < seeds.size(); indexSeedOne++) {
			
			if (verbose)
				IJ.log('\n' + "Checking Seed " + indexSeedOne + ": " + seeds.get(indexSeedOne));
						
			for (int indexSeedTwo = indexSeedOne + 1; indexSeedTwo < seeds.size(); indexSeedTwo++) {
				
				merge[indexSeedOne][indexSeedTwo] = true;
				
				if (verbose)
					IJ.log("Neighbor " + indexSeedTwo + " "+ seeds.get(indexSeedTwo));
				
				// To avoid division by 0
				boolean byX = (seeds.get(indexSeedOne).getX() - seeds.get(indexSeedTwo).getX()) == 0.0;
				
				if (byX && (seeds.get(indexSeedOne).getY() - seeds.get(indexSeedTwo).getY()) == 0.0)
					break;
				
				// Get equation of line between points y = a * x + b
				double a = !byX ?
						   (seeds.get(indexSeedOne).getY() - seeds.get(indexSeedTwo).getY()) / 
						   (seeds.get(indexSeedOne).getX() - seeds.get(indexSeedTwo).getX()) :
						   (seeds.get(indexSeedOne).getX() - seeds.get(indexSeedTwo).getX()) / 
						   (seeds.get(indexSeedOne).getY() - seeds.get(indexSeedTwo).getY());
							   
				double b = !byX ?
						   seeds.get(indexSeedOne).getY() - a * seeds.get(indexSeedOne).getX() :
						   seeds.get(indexSeedOne).getX() - a * seeds.get(indexSeedOne).getY();
				
				// Define min and max x values
				double min = !byX ?
							 Math.min(seeds.get(indexSeedOne).getX(), seeds.get(indexSeedTwo).getX()) :
						     Math.min(seeds.get(indexSeedOne).getY(), seeds.get(indexSeedTwo).getY());
				double max = !byX ?
						 	 Math.max(seeds.get(indexSeedOne).getX(), seeds.get(indexSeedTwo).getX()) :
							 Math.max(seeds.get(indexSeedOne).getY(), seeds.get(indexSeedTwo).getY());	
				
				// Create empty array to store all values
				ArrayList<Double> pixels = new ArrayList<Double>();
				
				// Get interpolated pixels between the two points
				for (double dl = min; dl < max; dl += 0.1) {
					
					double dx = !byX ? dl : (a * dl + b);
					double dy = !byX ? (a * dl + b) : dl;
					
					double eval = imp.getInterpolatedValue(dx, dy);

					pixels.add(eval);
					
				}
				
				if (merge[indexSeedOne][indexSeedTwo])
					for (Double value : derive(pixels)) {
						
						if (value > 25) {
							if (verbose)
								IJ.log("Edge detected using gradient!");
							
							merge[indexSeedOne][indexSeedTwo] = false;
							break;
						}
						
					}
			}
		}
		
		if (verbose) {
			IJ.log('\n' + "SUMMARY OF SEED MODIFICATIONS:");
		}
			
		ArrayList<Integer> mergedIdx = new ArrayList<Integer>();
		
		for (int idxSeedOne = 0; idxSeedOne < seeds.size(); idxSeedOne++) {
			
			boolean merged = false;
			
			if (mergedIdx.contains(idxSeedOne))
				continue;
			
			Region2D regionOne = timeFrame.getRegion(idxSeedOne);
			
			for (int idxSeedTwo = idxSeedOne + 1; idxSeedTwo < seeds.size(); idxSeedTwo++) {
				
				if (mergedIdx.contains(idxSeedTwo) || merged)
					continue;
			
				if (merge[idxSeedOne][idxSeedTwo]) {
					
					Region2D regionTwo = timeFrame.getRegion(idxSeedTwo);
					
					// Get new barycenter
					double x = regionOne.getBarycenter()[0] * regionOne.size() + regionTwo.getBarycenter()[0] * regionTwo.size();
					x /= (regionOne.size() + regionTwo.size());
					
					double y = regionOne.getBarycenter()[1] * regionOne.size() + regionTwo.getBarycenter()[1] * regionTwo.size();
					y /= (regionOne.size() + regionTwo.size());
					
					Point2D newSeed = new Point2D((int) x, (int) y, frame,
							(seeds.get(idxSeedOne).getValue() + seeds.get(idxSeedTwo).getValue()) / 2.0); 
					
					newSeeds.add(newSeed);
					
					mergedIdx.add(idxSeedOne);
					mergedIdx.add(idxSeedTwo);
					
					merged = true;
					
					
					if (verbose) {
						IJ.log("Added merged seed " + newSeed + " from " + idxSeedOne + " & " + idxSeedTwo);
					}
				}
			}
			
			if (!merged) {
				
				Point2D updatedSeed = new Point2D(
					(int) regionOne.getBarycenter()[0], (int) regionOne.getBarycenter()[1],
					frame, seeds.get(idxSeedOne).getValue()
				); 
				
				newSeeds.add(updatedSeed);
				
				if (verbose)
					IJ.log("Kept lone seed " + idxSeedOne + ": " + updatedSeed);
			}
		}
		
		return newSeeds;
		
	}
	
	
	/*
	 * @brief Surface Calculation and results
	 * 
	 * @param areas results image with regions
	 * 
	 */
	public void surfaceCalculation(ImagePlus areas) {
			
			int nx = areas.getWidth();
			int ny = areas.getHeight();
			int nt = areas.getNSlices();
			
			IJ.log("Size: " + " [" + nx + " " + ny + "]");
			IJ.log("Time frames " + nt);
			
			double[][] results = new double [1000][nt];
			
			int cellnb = 0;
			
			ArrayList<Double> color = new ArrayList<Double>();

			// Get Cell surface in number of pixels
			ArrayList<Double> cellsurfaces = new ArrayList<Double>();
			
			// Label is uncalibrated, enter voxel size [cubic cm]
			double vsize = 0.0032 * 0.0032;
			
			// Loop over time
			for (int t = 0; t < nt; t++) {
			
				double pixel = 0.0;
				
				//Initializing the volume of the cells to 0
				for(int l = 0; l < cellsurfaces.size(); l++) {
					cellsurfaces.set(l, 0.0);
				}		
			
				areas.setPosition(1, t, 1);
				ImageProcessor ipin = areas.getProcessor();
				
				for (int x = 0; x < nx; x++) {
						for (int y = 0; y < ny; y++) {
							
							pixel = ipin.getPixelValue(x, y);
							
							if (pixel != 0.0) {
							
								if (color.contains(pixel)) {
									
									int nb = color.indexOf(pixel);
									double surf = cellsurfaces.get(nb);
									cellsurfaces.set(nb, surf + vsize);
									
								} else {
								
									cellnb = cellnb + 1;
									color.add(pixel);
									cellsurfaces.add(vsize);
									
								}
							}
						}
					
					}
			
				for (int k = 0; k < cellnb; k++) {
					results[k][t] = cellsurfaces.get(k);
				}
			}
			
			IJ.log("Total number of cells " + cellnb);
			ResultsTable rt = new ResultsTable();
			
			
			for (int t = 0; t < nt; t++) {
				
				rt.incrementCounter();
				
				for (int c = 0; c < cellnb; c++) {
					rt.addValue("Cell " + c, results[c][t]);
				}
			}
			
			rt.show("Results");
			
			Plot plot = new Plot("Surface", "Time", "Cell volume");
			for (int c = 0; c < cellnb; c++) {
				
				Random rand = new Random();
				
				float r = rand.nextFloat();
				float g = rand.nextFloat();
				float b = rand.nextFloat();
				
				Color randomColor = new Color(r, g, b);
				
				plot.setColor(randomColor);
				plot.add("line", rt.getColumnAsDoubles(c));
			}
			
			plot.show();
			
	}
	
	/*!
     * @brief Median 2D filter
     *
     * @param in input image
     * @param sigma filter size
     * 
     * @return out output image
     *
     */
	public ImagePlus median2D (ImagePlus in, int sigma) {
		
		ImagePlus out = in.duplicate();
		
		for(int t = 1; t <= out.getNFrames(); t++) {
			
			out.setPosition(1, 1, t);
			
			ImageProcessor ip = out.getProcessor();
			
			RankFilters rf = new RankFilters();
			
			rf.rank(ip, sigma, RankFilters.MEDIAN);
		}		
		
		return out;
	}
	
	/*!
     * @brief Gaussian 2D filter
     *
     * @param in input image
     * @param sigmaX filter size in x direction
     * @param sigmaY filter size in y direction
     * @param accuracy of kernel
     * 
     * @return out output image
     *
     */
	public ImagePlus gaussian2D (ImagePlus in, double sigmaX, double sigmaY, double accuracy) {
		
		ImagePlus out = in.duplicate();
		
		for(int t = 1; t <= out.getNFrames(); t++) {
			
			out.setPosition(1, 1, t);
			
			ImageProcessor ip = out.getProcessor();
			
			GaussianBlur gb = new GaussianBlur();
			
			gb.blurGaussian(ip, sigmaX, sigmaY, accuracy);
		}		
		
		return out;
	}
	
	/*!
     * @brief Derivative a list of values
     *
     * @param f original list of values
     * 
     * @return dervied list of derived values
     *
     * @note uses ( f(x + h) - f(x - h) ) / (2 * h)
     */
    private static ArrayList<Double> derive(ArrayList<Double> f) {
    	
    	ArrayList<Double> derived = new ArrayList<Double>(); 
    	
    	for (int i = 1; i < f.size() - 1; i++) {
    		
    		derived.add( (f.get(i + 1) - f.get(i - 1)) / 2.0 );

    	}
    	
        return derived;
        
    }
    
	/*!
     * @brief Sigmoid function
     *
     * @param val x value
     * @param middle x value for y = 1/2
     * @param zoom zoom of descent
     * 
     * @return evaluated value
     */
	public double sigmoid(double val, double middle, double zoom) {
		
		return 1 / (1 + Math.exp( (val - middle) / zoom ));
		
	}
	
	
}

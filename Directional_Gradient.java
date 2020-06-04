import java.awt.Polygon;
import java.util.ArrayList;
import java.lang.Math;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.gui.Roi;
import ij.io.OpenDialog;
import ij.plugin.PlugIn;
import ij.process.ImageProcessor;


public class Directional_Gradient implements PlugIn {
		
	
	public void run(String arg) {
		
		// --- ----------- ---
		// --->INPUT IMAGE<---
		// --- ----------- ---
		
		// Duplicate input image
		ImagePlus in = IJ.getImage();
		
		// --- ------------ ---
		// --->OUTPUT IMAGE<---
		// --- ------------ ---
		
		// Create output segmentation image
		ImagePlus areas = IJ.createImage("Regions", nx, ny, 1, b);
		areas.show();
		
		Roi mroi = in.getRoi();
		if (mroi == null) {
			IJ.log("Roi is null. Please try again.");
			return;
		}
		
		Polygon up = mroi.getPolygon();
		
		// Number of initial regions
		int nri = up.xpoints.length;
		
		IJ.log("Retrieving " + nri + " selected seeds.");
		
		ImageProcessor imp = in.getProcessor();
		
		ArrayList<Region2D> regions = new ArrayList<Region2D>();
		
		for (int i = 0; i < nri; i++){
			
			Point2D seed = new Point2D(up.xpoints[i], up.ypoints[i], frame, imp.getPixelValue(up.xpoints[i], up.ypoints[i]));
			
			IJ.log("Seed added: " + seed.toString());
			
			Region2D region = new Region2D(seed);
			regions.add(region);
			
			oup.putPixelValue((int) seed.getX(), (int) seed.getY(), 55 + (200.0 / nri) * i);
			
			IJ.log("Region created with seed: " + region.getSeed().toString());
			
		}
		
		// --- ------------------ ---
		// --->REGION CALCULATION<---
		// --- ------------------ ---
	
		// While loop for number of iterations
		int iter = 0;
		while (iter < itermax) {
			
			boolean nothingAdded = true;
			
			IJ.log("Iteration " + Integer.toString(iter));
			
			// Loop over each Region
			for (int i = 0; i < regions.size(); i++) {
				
				IJ.log("Looping over region " + Integer.toString(i));
				
				Region2D region = regions.get(i);
				int s = region.size();
				
				// Iterate over all points in region
				for (int p = 0; p < s; p++) {
					
//					IJ.log("Looping over region " + Integer.toString(i) + " point " + region.getPoint(p).toString());
					
					int x = (int) region.getPoint(p).getX();
					int y = (int) region.getPoint(p).getY();		
					
					// Loop over x and y neighbors subsequently
					for (int m = - 1; m <= 1; m++)
					for (int n = - 1; n <= 1; n++) {
						
						// Check 20-connected instead of 26-connected
						if (Math.abs(m) + Math.abs(n) == 2)
							continue;
						
						// Check shell
//							if (tshl.getPixelValue(x + m, y + n) == 0.0) 
//								continue;
										
						// get comparison pixel
						double pixel = imp.getPixelValue(x + m, y + n);
						Point2D eval = new Point2D(x + m, y + n, frame, pixel);
						
//							IJ.log("Evaluating pixel: " + eval.toString());
						
						/* HARD CONDITIONS 
						 * (1) Check that newly evaluated pixel does not overlap with other regions
						 *     |— done by checking value of output image (if black - not written upon)
						 * (2) Check that pixel value is under user threshold
						 * (3) Check vicinity for growing spherically
						 *     |- TODO includes Star convex with respect to seed
						 * (4) Pixel is in shell (done 17 lines above)
						 */	
						int amount = (iter < itermax / 3) ? 1 : 3;
						
						if (oup.getPixelValue(x + m, y + n) != 0.0 || pixel >= thr || !region.isInVicinity(eval, amount))
							continue;
						
						/* SOFT CONDITIONS
						 * (1) Distance to seed
						 * (2) Pixel value compared to region's mean value
						 */
						double cost = 0.3 * sigmoid(region.getDistanceToSeed(eval), dis, dis / 5.0) + 
									  0.7 * sigmoid(Math.abs(pixel - region.getMean()), tol, tol / 5.0);
						

//						IJ.log("Cost: " + Double.toString(cost));
						
						
						if (cost > 0.5) {
							
							nothingAdded = false;
							
//							IJ.log("Point added: " + eval.toString());
							
							region.addPoint(eval);
							
							oup.putPixelValue(x + m, y + n, 55 + (200.0 / regions.size()) * i);
							
						}
					}
				}	
				
			}
			
			if (nothingAdded) {
				iter = 10000;
				IJ.log("Nothing added. Stopping algorithm.");
			}
			
			iter++;
			
		}
		
		areas.getProcessor().resetMinAndMax();

		
	}
	
}
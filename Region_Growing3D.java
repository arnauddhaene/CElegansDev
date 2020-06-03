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


public class Region_Growing3D implements PlugIn {
	
	public double sigmoid(double val, double middle, double zoom) {
		
		return 1 / (1 + Math.exp( (val - middle) / zoom ));
		
	}
	
	
	public void run(String arg) {
		
		// --- ----------- ---
		// --->INPUT IMAGE<---
		// --- ----------- ---
		
		// Duplicate input image
		ImagePlus in = IJ.getImage();
		
		// Get image dimensions 
		int nx = in.getWidth();
		int ny = in.getHeight();
		int nt = in.getNFrames();
		int nz = in.getNSlices();
		int b = in.getBitDepth();
		
		// --- ----------- ---
		// --->INPUT SHELL<---
		// --- ----------- ---
//		ImagePlus shell =  IJ.openImage();
		
		// --- ------------ ---
		// --->INPUT DIALOG<---
		// --- ------------ ---
		
		// Generate Dialog for Region Growing parameter input
		GenericDialog dlg = new GenericDialog("Region Growing");
		
		dlg.addNumericField("Max number of iterations", 30, 1);
		dlg.addNumericField("Max Mean Difference (graylevel)", 100, 1);
		dlg.addNumericField("Threshold (graylevel)", 4000, 1);
		dlg.addNumericField("Max Distance (um)", 25, 1);
//		dlg.addNumericField("Maximum Number of regions", 4, 1);
		
		dlg.showDialog();
		
		if (dlg.wasCanceled()) return;
		
		int itermax = (int) dlg.getNextNumber();
		double tol = dlg.getNextNumber();
		double thr = dlg.getNextNumber();
		double dis = dlg.getNextNumber();
		dis = dis * dis;  // squared here to prevent slow square root in distance function
//		int nr = (int) dlg.getNextNumber();
		
		IJ.log("Chosen parameters: tol=" + Double.toString(tol) + ", T=" + Double.toString(thr) + ", d=" + Double.toString(dis));;;
		
		// --- ------------ ---
		// --->OUTPUT IMAGE<---
		// --- ------------ ---
		
		// Create output segmentation image
		ImagePlus areas = IJ.createImage("Regions", nx, ny, nz, b);
		areas.show();
		
		Roi mroi = in.getRoi();
		if (mroi == null) {
			IJ.log("Roi is null. Please try again.");
			return;
		}
		
		Polygon up = mroi.getPolygon();
		
		// Number of initial regions
		int nri = up.xpoints.length;
		
		IJ.log("Retrieving " + Integer.toString(nri) + " selected seeds.");
		
		int slice = in.getSlice();
		int frame = in.getFrame();
		
		IJ.log("Current slice is: " + slice);
		IJ.log("Current frame is: " + frame);
		
		areas.setPositionWithoutUpdate(1, slice, 1);
		
		ImageProcessor imp = in.getProcessor();
		ImageProcessor oup = areas.getProcessor();
		
		ArrayList<Region3D> regions = new ArrayList<Region3D>();
		
		for (int i = 0; i < nri; i++){
			
			Point3D seed = new Point3D(up.xpoints[i], up.ypoints[i], slice, frame, imp.getPixelValue(up.xpoints[i], up.ypoints[i]));
			
			IJ.log("Seed added: " + seed.toString());
			
			Region3D region = new Region3D(seed);
			regions.add(region);
			
			oup.putPixelValue((int) seed.getX(), (int) seed.getY(), 55 + (200.0 / nri) * i);
			
			IJ.log("Region created with seed: " + region.seed.toString());
			
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
				
				Region3D region = regions.get(i);
				int s = region.size();
				
				// Iterate over all points in region
				for (int p = 0; p < s; p++) {
					
//					IJ.log("Looping over region " + Integer.toString(i) + " point " + region.getPoint(p).toString());
					
					int x = (int) region.getPoint(p).getX();
					int y = (int) region.getPoint(p).getY();
					int z = (int) region.getPoint(p).getZ();
					
					// Loop over z first to optimize processor access
					for (int o = - 1; o <= 1; o++) {
						
						if (z + o < 0 || z + o > nz)
							continue;
						
						in.setPositionWithoutUpdate(1, z + o, frame);
						areas.setPositionWithoutUpdate(1, z + o, 1);
//						shell.setPositionWithoutUpdate(1, z + o, frame);
						
						ImageProcessor timp = in.getProcessor();
						ImageProcessor toup = areas.getProcessor();
//						ImageProcessor tshl = shell.getProcessor();
						
						
						// Loop over x and y neighbors subsequently
						for (int m = - 1; m <= 1; m++)
						for (int n = - 1; n <= 1; n++) {
							
							// Check 20-connected instead of 26-connected
							if (Math.abs(m) + Math.abs(n) + Math.abs(o) == 3)
								continue;
							
							// Check shell
//							if (tshl.getPixelValue(x + m, y + n) == 0.0) 
//								continue;
											
							// get comparison pixel
							double pixel = timp.getPixelValue(x + m, y + n);
							Point3D eval = new Point3D(x + m, y + n, z + o, frame, pixel);
							
//							IJ.log("Evaluating pixel: " + eval.toString());
							
							/* HARD CONDITIONS 
							 * (1) Check that newly evaluated pixel does not overlap with other regions
							 *     |— done by checking value of output image (if black - not written upon)
							 * (2) Check that pixel value is under user threshold
							 * (3) Check vicinity for growing spherically
							 *     |- TODO includes Star convex with respect to seed
							 * (4) Pixel is in shell (done 17 lines above)
							 */	
							int amount = (iter < itermax / 3) ? 1 : 4;
							
							if (toup.getPixelValue(x + m, y + n) != 0.0 || pixel >= thr || !region.isInVicinity(eval, amount))
								continue;
							
							/* SOFT CONDITIONS
							 * (1) Distance to seed
							 * (2) Pixel value compared to region's mean value
							 */
							double cost = 0.3 * sigmoid(region.getDistanceToSeed(eval), dis, dis / 5.0) + 
										  0.7 * sigmoid(Math.abs(pixel - region.getMean()), tol, tol / 5.0);
							
	
//							IJ.log("Cost: " + Double.toString(cost));
							
							
							if (cost > 0.5) {
								
								nothingAdded = false;
								
//								IJ.log("Point added: " + eval.toString());
								
								region.addPoint(eval);
								
								oup.putPixelValue(x + m, y + n, 55 + (200.0 / regions.size()) * i);
								
							}
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
		
	}
	
}
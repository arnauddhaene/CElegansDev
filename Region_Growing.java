import java.awt.Polygon;
import java.util.ArrayList;


import ij.IJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.gui.Roi;
import ij.plugin.PlugIn;
import ij.process.ImageProcessor;


public class Region_Growing implements PlugIn {
	
	
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
		
		// --- ------------ ---
		// --->INPUT DIALOG<---
		// --- ------------ ---
		
		// Generate Dialog for Region Growing parameter input
		GenericDialog dlg = new GenericDialog("Region Growing");
		
		dlg.addNumericField("Max number of iterations", 100, 1);
		dlg.addNumericField ("Tolerance", 20, 1);
		dlg.addNumericField ("Threshold", 200, 1);
		dlg.addNumericField ("Maximum distance", 100, 1);
//		dlg.addNumericField ("Maximum Number of regions", 4, 1);
		
		dlg.showDialog();
		
		if (dlg.wasCanceled()) return;
		
		int itermax = (int) dlg.getNextNumber();
		double tol = dlg.getNextNumber();
		double T = dlg.getNextNumber();
		double distmax = dlg.getNextNumber();
//		int nr = (int) dlg.getNextNumber();
		
		IJ.log("Chosen parameters: tol=" + Double.toString(tol) + ", T=" + Double.toString(T) + ", d=" + Double.toString(distmax));;;
		
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
			
			Point3D seed = new Point3D(up.xpoints[i], up.ypoints[i], slice, imp.getPixelValue(up.xpoints[i], up.ypoints[i]));
			
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
				
					// Get 26-connected neighbors
					// Loop over z first to optimize processor access
					for (int o = - 1; o <= 1; o++) {
						
						if (z + o < 6 || z + o > 16)
							continue;
						
						in.setPositionWithoutUpdate(1, z + o, frame);
						areas.setPositionWithoutUpdate(1, z + o, 1);
						
						ImageProcessor timp = in.getProcessor();
						ImageProcessor toup = areas.getProcessor();
						
						for (int m = - 1; m <= 1; m++)
						for (int n = - 1; n <= 1; n++) {
							
	//						IJ.log("Getting 26-connected neighbors.");
							
							
							// Using value = 0.0 as is it not used in Point3D::equals for comparison
							if (region.overlaps(new Point3D(x + m, y + n, z + o, 0.0)))
								continue;
							
							// get comparison pixel
							double pixel = timp.getPixelValue(x + m, y + n);
							
							// Check that newly evaluated pixel does not overlap with others
							boolean overlaps = false;
							
							for (int r = 0; r < regions.size(); r++) {
								if (r != i) {
									// Using value = 0.0 as is it not used in Point3D::equals for comparison
									if (regions.get(r).overlaps(new Point3D(x, y, z, 0.0))) {
										overlaps = true;
										continue;
									}	
								}
							}
							
	//						IJ.log("Pixel value of " + new Point(x + m, y + n).toString() + ": " + Double.toString(pixel));
	//						IJ.log("Overlaps: " + String.valueOf(overlaps));
	//						IJ.log("Tolerance: " + String.valueOf(Math.abs(pixel - region.getMean()) < tol));
	//						IJ.log("Threshold: " + String.valueOf(pixel < T));
	//						IJ.log("Distance: " + String.valueOf(region.getDistanceToSeed(new Point(x, y)) < distmax));
							
							
							/* Test conditions
							 * (1) p is inside the image
							 *  |— Not sure how this is handled TODO
							 * (2) p doesn't overlap with another region
							 * (3) | f(p) - region_mean | < tol
							 * (4) f(p) < T
							 * (5) dist(seed, p) < distmin 
							 * 	|— Using value = 0.0 as is it not used in Point3D::equals for comparison
							 */
							
							if (!overlaps &&
								Math.abs(pixel - region.getMean()) < tol &&
								pixel < T &&
								region.getDistanceToSeed(new Point3D(x, y, z, 0.0)) < distmax) {
								
								nothingAdded = false;
								
								Point3D addition = new Point3D(x + m, y + n, z + o, pixel); 
								
								IJ.log("Point: " + addition.toString() + " added w/ value " + pixel);
								
								region.addPoint(addition);
								
								oup.putPixelValue(x + m, y + n, 55 + (200.0 / regions.size()) * i);
							}
						}
					}
				}	
				
			}
			
			if (nothingAdded)
				iter = 10000;
			
			iter++;
			
		}
		
	}
	
}
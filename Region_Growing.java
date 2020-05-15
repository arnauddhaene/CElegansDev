import java.awt.Point;
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
		
		// Duplicate input image
		ImagePlus in = IJ.getImage();
		
		// Get image dimensions 
		int nx = in.getWidth();
		int ny = in.getHeight();
		int nt = in.getNFrames();
		int nz = in.getSlice();
		int b = in.getBitDepth();
		
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
		
		// Loop over all z
//		for (int z = 0; z < nz; z++) {
		
			IJ.log("Retrieving " + Integer.toString(nri) + " selected seeds.");
			
//			in.setPosition(1, 1, z + 1);
//			areas.setPositionWithoutUpdate(1, z + 1, 1);
			ImageProcessor imp = in.getProcessor();
			ImageProcessor oup = areas.getProcessor();
			
			ArrayList<Region2D> regions = new ArrayList<Region2D>();
			
			for (int i = 0; i < nri; i++){
				
				Point seed = new Point(up.xpoints[i], up.ypoints[i]);
				
				IJ.log("Seed added: " + seed.toString());
				
				Region2D region = new Region2D(seed, imp.getPixelValue((int) seed.getX(), (int) seed.getY()));
				regions.add(region);
				
				oup.putPixelValue((int) seed.getX(), (int) seed.getY(), 180);
				
				IJ.log("Region created with seed: " + region.seed.toString());
				
			}
		
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
					
						// Get 8-connected neighbors
						for (int m = - 1; m <= 1; m++)
						for (int n = - 1; n <= 1; n++) {
							
	//						IJ.log("Getting 8-connected neighbors.");
							
							if (region.overlaps(new Point(x + m, y + n)))
								continue;
							
							// get comparison pixel
							double pixel = imp.getPixelValue(x + m, y + n);
							
							boolean overlaps = false;
							
							for (int o = 0; o < regions.size(); o++) {
								if (o != i) {
									if (regions.get(o).overlaps(new Point(x, y))) {
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
							
//							double dtocenter = Math.abs(z - nz / 2.0); 
							
							
							/* Test conditions
							 * (1) p is inside the image
							 * (2) p doesn't overlap with another region
							 * (3) | f(p) - region_mean | < tol
							 * (4) f(p) < T
							 * (5) dist(seed, p) < distmin 
							 */
							if (!overlaps &&
								Math.abs(pixel - region.getMean()) < tol &&
								pixel < T &&
								region.getDistanceToSeed(new Point(x, y)) < distmax /* / dtocenter */) {
								
								nothingAdded = false;
								
								Point addition = new Point(x + m, y + n); 
								
								IJ.log("Point: " + addition.toString() + " added w/ value " + pixel);
								
								region.addPoint(addition, pixel);
								
								oup.putPixelValue(x + m, y + n, 55 + (200.0 / regions.size()) * i);
							}
						}
					}	
					
				}
				
				if (nothingAdded)
					iter = 10000;
				
				iter++;
				
			}
//		}
		
	}
	
}
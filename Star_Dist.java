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


public class Star_Dist implements PlugIn {
	
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
		
		// --- ------------ ---
		// --->INPUT DIALOG<---
		// --- ------------ ---
		
		// Generate Dialog for Region Growing parameter input
		GenericDialog dlg = new GenericDialog("Region Growing");
		
		dlg.addNumericField("Max number of iterations", 30, 1);
		dlg.addNumericField("Number of Arms", 25);
//		dlg.addNumericField ("Max Mean Difference (graylevel)", 300, 1);
//		dlg.addNumericField ("Threshold (graylevel)", 4000, 1);
//		dlg.addNumericField ("Max Distance (um)", 100, 1);
		
		dlg.showDialog();
		
		if (dlg.wasCanceled()) return;
		
		int itermax = (int) dlg.getNextNumber();
		int arms = (int) dlg.getNextNumber();
		
//		double tol = dlg.getNextNumber();
//		double thr = dlg.getNextNumber();
//		double dis = dlg.getNextNumber();
//		dis = dis * dis;  // squared here to prevent slow square root in distance function
//		int nr = (int) dlg.getNextNumber();
		
		IJ.log("Chosen parameters: itermax=" + itermax + " arms=" + arms);

		
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
		
		ArrayList<StarRegion> regions = new ArrayList<StarRegion>();
		
		for (int i = 0; i < nri; i++){
			
			Point3D seed = new Point3D(up.xpoints[i], up.ypoints[i], slice, frame, imp.getPixelValue(up.xpoints[i], up.ypoints[i]));
			
			IJ.log("Seed added: " + seed.toString());
			
			StarRegion region = new StarRegion(seed, arms);
			
			for (StarArm arm : region.arms) {
				for (int l = 0; l <= arm.getLength(); l++) {
					
					double[] coords = arm.getCoordinates(l);
					
					areas.setSliceWithoutUpdate(seed.getZ() + (int) coords[2]);
					ImageProcessor temp = areas.getProcessor();
					
					IJ.log("Fetched point of coordinates " + coords[0] + " " + coords[1] + " " + coords[2]);;
					
					temp.putPixelValue(seed.getX() + (int) coords[0], seed.getY() + (int) coords[1], 255.0);
					
				}
			}			
			
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
				
				StarRegion region = regions.get(i);
				int s = region.size();
				
				// Iterate over all arms in region
				for (int a = 0; a < s; a++) {
					
					StarArm arm = region.getArm(a);
					
					
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

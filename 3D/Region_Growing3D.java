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

/**
 * Class implementing a 3D Region Growing Plug In for ImageJ
 *
 * Created by Arnaud Dhaene (EPFL), Audrey Menaesse (EPFL)
 */
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
		in.hide();

		// Get image dimensions
		int nx = in.getWidth();
		int ny = in.getHeight();
		int nt = in.getNFrames();
		int nz = in.getNSlices();
		int b = in.getBitDepth();

		// --- ----------------------------- ---
		// ---> OPENING PREPROCESSED IMAGES <---
		// --- ----------------------------- ---
		OpenDialog od = new OpenDialog("Choose the original image", null);
		String dir = od.getDirectory();
		IJ.open(dir+"edges.tif");
		ImagePlus edges = IJ.getImage();
		edges.hide();
		IJ.open(dir+"shell.tif");
		ImagePlus shell = IJ.getImage();
		shell.hide();


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
		ImagePlus areas = IJ.createHyperStack("Regions", nx, ny, 1, nz, nt, b);


		// Checking that the seeds are selected from the last time point
		int slice = in.getSlice();
		int frame = in.getFrame();
		if (frame < nt) {
			IJ.log("The initial seeds must be selected from the last frame");
			return;
		}

		Roi mroi = in.getRoi();
		if (mroi == null) {
			IJ.log("Roi is null. Please try again.");
			return;
		}

		Polygon up = mroi.getPolygon();

		// Number of initial regions
		int nri = up.xpoints.length;

		IJ.log("Retrieving " + Integer.toString(nri) + " selected seeds.");


		IJ.log("Working slice is: " + slice);


		ArrayList<Point3D> seeds = new ArrayList<Point3D>();
		for (int t=0; t<nt; t++) {
			frame = nt-t;
			IJ.log("Current frame is: " + frame);
			areas.setPositionWithoutUpdate(1, slice, frame);
			ImageProcessor imp = in.getProcessor();
			ImageProcessor oup = areas.getProcessor();
			ArrayList<Region3D> regions = new ArrayList<Region3D>();

		// --- ---------------------- ---
		// --->SEEDS DETECTION/UPDATE<---
		// --- ---------------------- ---

		if (frame == nt) {
			for (int i = 0; i < nri; i++){
				double color = 55 + (200.0 / nri) * i;
				// Reading the seed points from manual input
				Point3D seed = new Point3D(up.xpoints[i], up.ypoints[i], slice, frame, color, imp.getPixelValue(up.xpoints[i], up.ypoints[i]));
				seeds.add(seed);
				IJ.log("Seed added: " + seed.toString());

				Region3D region = new Region3D(seed);
				regions.add(region);

				oup.putPixelValue((int) seed.getX(), (int) seed.getY(), color);

				IJ.log("Region created with seed: " + region.seed.toString());
		}
		}
		else {
			// Deducing the seed points from the previously analyzed frame
			seeds= getSeeds (edges,  in, seeds, slice, frame);
			int nbr=seeds.size();
			for (int i=0; i<nbr; i++) {
				Point3D seed = seeds.get(i);
				double color = seed.getColor();
				Region3D region = new Region3D(seed);
				regions.add(region);
				oup.putPixelValue((int) seed.getX(), (int) seed.getY(), color);
				IJ.log("Region created with seed: " + region.seed.toString());
			}
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
				double color = seeds.get(i).getColor();
				int s = region.size();

				// Iterate over all points in region
				for (int p = 0; p < s; p++) {

//					IJ.log("Looping over region " + Integer.toString(i) + " point " + region.getPoint(p).toString());

					int x = (int) region.getPoint(p).getX();
					int y = (int) region.getPoint(p).getY();
					int z = (int) region.getPoint(p).getZ();

					// Get 26-connected neighbors
//					IJ.log("Getting 26-connected neighbors.");

					// Loop over z first to optimize processor access
					for (int o = - 1; o <= 1; o++) {

						if (z + o < 0 || z + o > 24)
							continue;

						in.setPositionWithoutUpdate(1, z + o, frame);
						areas.setPositionWithoutUpdate(1, z + o, frame);
						shell.setPositionWithoutUpdate(1, z + o, frame);

						ImageProcessor timp = in.getProcessor();
						ImageProcessor toup = areas.getProcessor();
						ImageProcessor tshl = shell.getProcessor();


						// Loop over x and y neighbors subsequently
						for (int m = - 1; m <= 1; m++)
						for (int n = - 1; n <= 1; n++) {

							// Check 20-connected instead of 26-connected
							if (Math.abs(m) + Math.abs(n) + Math.abs(o) == 3)
								continue;

							// Check shell
							if (tshl.getPixelValue(x + m, y + n) == 0.0)
								continue;

							// get comparison pixel
							double pixel = timp.getPixelValue(x + m, y + n);
							Point3D eval = new Point3D(x + m, y + n, z + o, frame, color, pixel);

//							IJ.log("Evaluating pixel: " + eval.toString());

							/* HARD CONDITIONS
							 * (1) Check that newly evaluated pixel does not overlap with other regions
							 *     |� done by checking value of output image (if black - not written upon)
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

							// TODO check if edge in region of point w/ gradient
							// |� take into account neighborhood to prevent region holes
							// |� agglomeration as a region=

//							IJ.log("Cost: " + Double.toString(cost));


							if (cost > 0.5) {

								nothingAdded = false;

//								IJ.log("Point added: " + eval.toString());

								region.addPoint(eval);

								oup.putPixelValue(x + m, y + n, color);

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

		IJ.log("Closing");
		areas = close (areas, frame, ball(3));

		// Repositioning the seeds at the centroid of the central slice of their region
		seeds = repositionSeeds (regions, in, seeds, slice, frame);


		}

		IJ.saveAs(areas, "Tiff", dir +"/areas.tif");

		in.show();
		areas.show();

	}



	public ArrayList<Point3D> getSeeds (ImagePlus edges, ImagePlus in, ArrayList<Point3D> seeds, int slice, int frame) {
		in.setPosition(1, slice, frame);
		ImageProcessor ip = in.getProcessor();
		edges.setPosition(1,slice, frame);
		ImageProcessor ipedges = edges.getProcessor();
		ArrayList<Point3D> newSeeds =  (ArrayList<Point3D>) seeds.clone();

		int nri = seeds.size();
		ArrayList<Integer> discarded = new ArrayList<Integer>();
		for (int i=0; i<nri; i++) {
			if (discarded.contains(i)==false) {
			// Checking if the seed is separated from the other seed points by a membrane on the edges file.
			int loc = isAlone(ipedges, seeds, discarded, i);
			// If the seed is independent, the value of the associated pixel is update to the current time frame
			if (loc==Integer.MAX_VALUE) {
				Point3D A = seeds.get(i);
				Point3D C = new Point3D (A.getX(), A.getY(), slice, frame, A.getColor(), ip.getPixelValue(A.getX(), A.getY()));
				newSeeds.set(i, C);
				IJ.log("Seed updated : " + C.getX() + "  "  + C.getY() + " for frame " + frame);
			}
			// When 2 seed points are in the same region, they are replaced by a seed point situated in the middle of the segment defined by these two points
			else {
				Point3D A = seeds.get(i);
				Point3D B = seeds.get(loc);
				int x = (int) Math.floor((A.getX() + B.getX())/2);
				int y = (int) Math.floor((A.getY() + B.getY())/2);
				double color = A.getColor() + 1;
				Point3D C = new Point3D (x, y, slice, frame, color, ip.getPixelValue(x, y));
				discarded.add(i);
				discarded.add(loc);
				newSeeds.add(C);
				IJ.log("Seed created : " + C.getX() + "  "  + C.getY() + " for frame " + frame);
				IJ.log("2 seeds removed");
			}
			}
		}

		ArrayList<Point3D> NS =  new ArrayList<Point3D>();
		for (int k=0; k<newSeeds.size(); k++) {
			if (discarded.contains(k)==false) {
				Point3D A = newSeeds.get(k);
				NS.add(A);
			}
		}

		return NS;

	}

	public int isAlone (ImageProcessor ipedges, ArrayList<Point3D> seeds, ArrayList<Integer> removed, int i) {
		int nri = seeds.size();
		int loc = Integer.MAX_VALUE;
		Point3D A = seeds.get(i);
		for (int k=0; k<nri; k++) {
			//Checking if the point has already been removed
			if (removed.contains(k)==false && k!=i) {
				Point3D B = seeds.get(k);
				int dx = Math.abs(A.getX()-B.getX());
				int dy = Math.abs(A.getY()-B.getY());
				int lth = (int) Math.sqrt(dx*dx+dy*dy);
				double intensity = 0;
				int xmin=Math.min(A.getX(), B.getX());
				int ymin;
				int xmax;
				int ymax;
				if (xmin==A.getX()) {
					ymin = A.getY();
					xmax = B.getX();
					ymax = B.getY();
				}
				else{
					ymin = B.getY();
					xmax = A.getX();
					ymax = A.getY();
				}
				boolean dypositive = true;
				if((ymax-ymin)<0) {
					dypositive = false;
				}

				int x=xmin;
				int y=ymin;
		// Detecting membrane along the line between the 2 points
				for (int n=1; n <=lth; n++) {

					if (dx!=0) {
						x = xmin + n*dx/lth;
						//x = (int) (xmin + Math.abs((n+1)*Math.cos(angle)/lth));
					}
					if(dy!=0) {
						if (dypositive==true) {
							y= ymin + n*dy/lth;
							//y = (int) (ymin + Math.abs((n+1)*Math.sin(angle)/lth));
						}
						else {
							y= ymin - n*dy/lth;
							//y = (int) (ymin - Math.abs((n+1)*Math.sin(angle)/lth));
						}
					}
					intensity+= ipedges.getPixelValue(x, y);
					intensity+= ipedges.getPixelValue(x, y-1);
					intensity+= ipedges.getPixelValue(x, y+1);
					intensity+= ipedges.getPixelValue(x-1, y);
					intensity+= ipedges.getPixelValue(x-1, y-1);
					intensity+= ipedges.getPixelValue(x-1, y+1);
					intensity+= ipedges.getPixelValue(x+1, y);
					intensity+= ipedges.getPixelValue(x+1, y-1);
					intensity+= ipedges.getPixelValue(x+1, y+1);
				}

			if (intensity == 0) {
				loc=k;
			}

		}
		}

	return loc;

}



	public ArrayList<Point3D> repositionSeeds (ArrayList<Region3D> regions, ImagePlus in, ArrayList<Point3D> seeds, int slice, int frame) {
		int nri = seeds.size();
		in.setPosition(1, slice, frame);
		ImageProcessor ip = in.getProcessor();
		for (int s=0; s<nri; s++) {
			Region3D region = regions.get(s);
			double color = seeds.get(s).getColor();
			int x = region.getXCentroid();
			int y = region.getYCentroid();
			Point3D C = new Point3D (x, y, slice, frame, color, ip.getPixelValue(x,y));
			seeds.set(s, C);
		}
	return seeds;

	}

	public boolean[][][] ball(int size) {
		 boolean mask[][][] = new boolean[size][size][size];
		 int r = size/2;
		 for(int i =0; i<size; i++) {
			 for (int j=0; j<size; j++) {
				 for(int k=0; k<size; k++) {
					int x = i-r;
					int y = j-r;
					int z = k-r;
					if (x*x+y*y+z*z<=r*r) {
						mask[i][j][k]=true;
					}
					else {
						mask[i][j][k]=false;
					}
				 }
			 }
		 }
		 return mask;
	}

	public double[][][] neighborhood (ImagePlus in, int x, int y, int z, int t, int Lxy, int Lz){
		double b [][][]  = new double [Lxy][Lxy][Lz];
		int lxy= (Lxy-1)/2;
		int lz= (Lz-1)/2;

		if (x>lxy && x<(in.getWidth()-lxy)) {
			if( y>lxy && y<(in.getHeight()-lxy)) {
			for (int i=0; i<Lxy; i++) {
				for (int j=0; j<Lxy; j++) {
					for (int k=0; k<Lz; k++) {
						if ((z-lz+k)>0 && (z-lz+k)<=in.getNSlices()) {
						in.setPosition(1,(z-lz+k), t);
						ImageProcessor ip = in.getProcessor();
						b[i][j][k] = ip.getPixelValue(x-lxy+i, y-lxy+j);
						}
						else {
							b[i][j][k] = 0;
						}
					}
				}
			}
			}
		}
		return b;
	}

	public ImagePlus dilation (ImagePlus in, int frame, boolean mask[][][]) {
		 int size = mask.length;
		 ImagePlus output = in.duplicate();
		 for (int x=0; x<in.getWidth(); x++) {
			for (int y=0; y<in.getHeight(); y++) {
				for (int z=1; z<=in.getNSlices(); z++) {
					double [][][] block = neighborhood (in, x, y, z, frame, size, size);
						 double max = -Double.MAX_VALUE;
						 for (int i=0; i<size; i++) {
							 for (int j=0; j<size; j++) {
								 for (int k=0; k<size; k++) {
									 if (mask[size-1-i][size-1-j][size-1-k] == true) {
										 max = Math.max(block[i][j][k], max);
									 }
								 }
							 }
						 }
						 output.setPosition(1,z,frame);
						 ImageProcessor ip = output.getProcessor();
						 ip.putPixelValue(x,y,max);
						 }
				 }
			 }
		 return output;
	}

	public ImagePlus erosion(ImagePlus in, int frame, boolean mask[][][]) {
		 int size = mask.length;
		 ImagePlus output = in.duplicate();
		 for (int x=0; x<in.getWidth(); x++) {
			for (int y=0; y<in.getHeight(); y++) {
				for (int z=1; z<=in.getNSlices(); z++) {
					double [][][] block = neighborhood (in, x, y, z, frame, size, size);
					double min = Double.MAX_VALUE;
					for (int i=0; i<size; i++) {
					 for (int j=0; j<size; j++) {
						 for (int k=0; k<size; k++) {
							 if (mask[i][j][k]==true) {
								 min = Math.min(block[i][j][k], min);
							 }
						 }
					 }
					 }
					output.setPosition(1,z,frame);
					ImageProcessor ip = output.getProcessor();
					ip.putPixelValue(x,y,min);
					 }
				 }
			 }

		 return output;
	}

	public ImagePlus close(ImagePlus in, int frame, boolean mask[][][]) {
		ImagePlus temp = dilation (in, frame, mask);
		ImagePlus out = erosion (temp, frame, mask);
		 return out;
		 }

}

import java.awt.Polygon;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;

import com.sun.tools.javac.util.ArrayUtils;


import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.GenericDialog;
import ij.gui.Roi;
import ij.io.FileInfo;
import ij.io.FileOpener;
import ij.io.OpenDialog;
import ij.plugin.PlugIn;
import ij.process.ImageProcessor;


public class Seed_Select implements PlugIn {
	
	
	public void run(String arg) {
		
		// Opening the original image
		ImagePlus in = IJ.getImage();
		int nx = in.getWidth();
		int ny = in.getHeight();
		int nt = in.getNFrames();
		int nz = in.getNSlices();
		int b = in.getBitDepth();
		
		// Opening the edges image
		OpenDialog od = new OpenDialog("Choose a .tif file", null);  
		String path = od.getPath();
		IJ.open(path);
		ImagePlus edges = IJ.getImage();
			
		// Checking that the seeds are selected from the last time point		
		int slice = in.getSlice();
		int frame = in.getFrame();
		if (frame < nt) {
			IJ.log("The initial seeds must be selected from the last frame");
			return;
		}
		
		
		// Selection of the initial seed points
		Roi mroi = in.getRoi();
		if (mroi == null) {
			IJ.log("Roi is null. Please try again.");
			return;
		}
		
		Polygon up = mroi.getPolygon();
		
		// Number of initial regions
		int nri = up.xpoints.length;
		ArrayList<Point3D> seeds = new ArrayList<Point3D>();
		
		// Creating the list of seed points for the last frame
		ImageProcessor imp = in.getProcessor();
		for (int i = 0; i < nri; i++){			
			Point3D seed = new Point3D(up.xpoints[i], up.ypoints[i], slice, frame, imp.getPixelValue(up.xpoints[i], up.ypoints[i]));
			seeds.add(seed);
			IJ.log("Seed added: " + seed.toString() + "for frame "+ nt);
		}		
		IJ.log(seeds.size()+" seeds for frame " + (nt));
		
		// Recursively defining the list of seed points for each frame
		for (int i=1; i<nt; i++) {			
			seeds = getSeeds (edges, in, seeds, slice, nt-i);
			IJ.log(seeds.size()+" seeds for frame " + (nt-i));
		}
		
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
				Point3D C = new Point3D (A.getX(), A.getY(), slice, frame, ip.getPixelValue(A.getX(), A.getY()));
				newSeeds.set(i, C);
				IJ.log("Seed updated : " + C.getX() + "  "  + C.getY() + " for frame " + frame);		
			}
			// When 2 seed points are in the same region, they are replaced by a seed point situated in the middle of the segment defined by these two points 
			else {
				Point3D A = seeds.get(i);
				Point3D B = seeds.get(loc);
				int x = (int) Math.floor((A.getX() + B.getX())/2);
				int y = (int) Math.floor((A.getY() + B.getY())/2);
				Point3D C = new Point3D (x, y, slice, frame, ip.getPixelValue(x, y));
				discarded.add(i);
				discarded.add(loc);
				newSeeds.add(C);
				IJ.log("Seed created : " + C.getX() + "  "  + C.getY() + " for frame " + frame);
				IJ.log("2 seeds removed");
			}
			}
		}
		// Creating a new list to only keep the isolated seeds 
		int s= discarded.size();
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
		int x=Math.min(A.getX(), B.getX());
		int y=Math.min(A.getY(), B.getY());
		
		// Detecting membrane along the line between the 2 points
		for (int n=0; n <lth; n++) {
			
			if (dx!=0) {
			x = x + 1/dx ;
			}
			if(dy!=0) {
			y = y + 1/dy;
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
		
			if (intensity != 0) {
				loc=k;
			}
			
		}
		}
	
	return loc; 
	
}
}
	
	
	
	
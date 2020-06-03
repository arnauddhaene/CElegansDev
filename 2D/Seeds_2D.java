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


public class Seeds_2D implements PlugIn {
	
	
	public void run(String arg) {
		
		// Opening the original image
		ImagePlus in = IJ.getImage();
		int nx = in.getWidth();
		int ny = in.getHeight();
		int nt = in.getNFrames();
		int b = in.getBitDepth();
		
		// Opening the edges image
		OpenDialog od = new OpenDialog("Choose a .tif file", null);  
		String path = od.getPath();
		IJ.open(path);
		ImagePlus edges = IJ.getImage();
			
		// Checking that the seeds are selected from the last time point		
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
		ArrayList<Point2D> seeds = new ArrayList<Point2D>();
		
		// Creating the list of seed points for the last frame
		ImageProcessor imp = in.getProcessor();
		for (int i = 0; i < nri; i++){
			double color = 55 + (200.0 / nri) * i;
			Point2D seed = new Point2D(up.xpoints[i], up.ypoints[i], frame, color, imp.getPixelValue(up.xpoints[i], up.ypoints[i]));
			seeds.add(seed);
			IJ.log("Seed added: " + seed.toString() + "for frame "+ nt);
		}		
		IJ.log(seeds.size()+" seeds for frame " + (nt));
		
		// Recursively defining the list of seed points for each frame
		for (int i=1; i<nt; i++) {			
			seeds = getSeeds (edges, in, seeds, nt-i);
			IJ.log(seeds.size()+" seeds for frame " + (nt-i));
		}
		
	}
	
	
	
	public ArrayList<Point2D> getSeeds (ImagePlus edges, ImagePlus in, ArrayList<Point2D> seeds, int frame) { 
		in.setPosition(1, 1, frame);
		ImageProcessor ip = in.getProcessor();
		edges.setPosition(1, 1, frame);
		ImageProcessor ipedges = edges.getProcessor();
		ArrayList<Point2D> newSeeds =  (ArrayList<Point2D>) seeds.clone();
		
		int nri = seeds.size();
		int ncolor = nri;
		ArrayList<Integer> discarded = new ArrayList<Integer>();
		for (int i=0; i<nri; i++) {
			if (discarded.contains(i)==false) {
			// Checking if the seed is separated from the other seed points by a membrane on the edges file. 
			int loc = isAlone(ipedges, seeds, discarded, i);
			// If the seed is independent, the value of the associated pixel is update to the current time frame
			if (loc==Integer.MAX_VALUE) {
				Point2D A = seeds.get(i);
				Point2D C = new Point2D (A.getX(), A.getY(), frame, A.getColor(), ip.getPixelValue(A.getX(), A.getY()));
				newSeeds.set(i, C);
				//IJ.log("Seed updated : " + C.getX() + "  "  + C.getY() + " for frame " + frame);		
			}
			// When 2 seed points are in the same region, they are replaced by a seed point situated in the middle of the segment defined by these two points 
			else {
				Point2D A = seeds.get(i);
				Point2D B = seeds.get(loc);
				ncolor+=1;
				int x = (int) Math.floor((A.getX() + B.getX())/2);
				int y = (int) Math.floor((A.getY() + B.getY())/2);
				double color = 55 + (200.0 / ncolor) * i;
				Point2D C = new Point2D (x, y, frame, color, ip.getPixelValue(x, y));
				discarded.add(i);
				discarded.add(loc);
				newSeeds.add(C);
				//IJ.log("Seed created : " + C.getX() + "  "  + C.getY() + " for frame " + frame);
				//IJ.log("2 seeds removed");
			}
			}
		}
		// Creating a new list to only keep the isolated seeds 
		int s= discarded.size();
		ArrayList<Point2D> NS =  new ArrayList<Point2D>();
		for (int k=0; k<newSeeds.size(); k++) {
			if (discarded.contains(k)==false) {
				Point2D A = newSeeds.get(k);
				NS.add(A);
			}
		}
		
		return NS;
		
	}
	
	public int isAlone (ImageProcessor ipedges, ArrayList<Point2D> seeds, ArrayList<Integer> removed, int i) {
		int nri = seeds.size();
		int loc = Integer.MAX_VALUE;
		Point2D A = seeds.get(i);
		for (int k=0; k<nri; k++) {
			//Checking if the point has already been removed 
			if (removed.contains(k)==false && k!=i) {
				Point2D B = seeds.get(k);				
				int dx = Math.abs(A.getX()-B.getX());
				int dy = Math.abs(A.getY()-B.getY());
				int lth = (int) Math.sqrt(dx*dx+dy*dy);
				double angle = Math.atan((double) dy/dx);
				//IJ.log("Angle"+angle);
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
	

}
	
	
	

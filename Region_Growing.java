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
		ImagePlus in = IJ.getImage().duplicate();
		int nx = in.getWidth();
		int ny = in.getHeight();
		int nt = in.getNFrames();
		int nz = in.getSlice();
		int b = in.getBitDepth();
		ImagePlus areas = IJ.createImage("Regions", nx, ny, nz, b);
		
		
		GenericDialog dlg = new GenericDialog("Region Growing");
		dlg.addNumericField("Max number of iterations", 100, 1);
		dlg.addNumericField ("Tolerance", 20, 1);
		dlg.addNumericField ("Threshold", 200, 1);
		dlg.addNumericField ("Maximum distance", 100,1);
		dlg.addNumericField ("Maximum Number of regions", 4,1);
		dlg.showDialog();
		if (dlg.wasCanceled()) return;
		int itermax = (int) dlg.getNextNumber();
		double tol = dlg.getNextNumber();
		double T = dlg.getNextNumber();
		double distmax = dlg.getNextNumber();
		int nr = (int) dlg.getNextNumber();
		
		
		//Roi mroi = in.getRoi();
		//if (mroi == null) 
			//return;
		//Polygon up = mroi.getPolygon();
		
		//ArrayList<Point> seeds = new ArrayList<Point>();
		//for (int i=0; i<nr; i++){
			//Point seed = new Point(up.xpoints[i], up.ypoints[i]);
			//seeds.add(seed);
		//}
		
		//System.out.println(seeds);
		
		//ArrayList<Region2D> regions = new ArrayList<Region2D>();
		
		
		
		
	}
	
}
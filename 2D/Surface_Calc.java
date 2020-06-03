
import java.awt.Color;
import java.util.ArrayList;
import java.util.Random;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.gui.Plot;
import ij.measure.ResultsTable;
import ij.plugin.PlugIn;
import ij.process.ImageProcessor;


public class Surface_Calc implements PlugIn {

	public void run(String arg) {
		
		ImagePlus in = IJ.getImage().duplicate();
		int nx = in.getWidth();
		int ny = in.getHeight();
		int nt = in.getNFrames();
		IJ.log("Size: " + " [" + nx + " " + ny + "]");
		IJ.log("Time frames " + nt);
		double[][] results = new double [1000][nt];
		int cellnb =0;
		ArrayList<Double> color = new ArrayList ();
		ArrayList<Double> cellsurface = new ArrayList (); // in number of voxels
		
		// As label is uncalibrated, so I enter the values by hand		
		double vsize = 0.0032*0.0032; //in cm^3
		for (int t=0; t<nt; t++) {
		
			double pixel = 0.0;
		//Initializing the volume of the cells to 0
			for(int l=0; l<cellsurface.size(); l++) {
				cellsurface.set(l, 0.0);
			}		
		
			in.setPosition(1, 1, t);
			ImageProcessor ipin = in.getProcessor();
				for (int x=0; x<nx; x++) {
						for (int y=0; y<ny; y++) {
							pixel = ipin.getPixelValue(x, y);
							if (pixel !=0) {
								if (color.contains(pixel)){
									int nb = color.indexOf(pixel);
									double surf = cellsurface.get(nb);
									cellsurface.set(nb, surf+vsize);
								}
								else {
									cellnb = cellnb +1;
									color.add(pixel);
									cellsurface.add(vsize);						
								}
							}
						}
				
		}
		
		for (int k=0; k<cellnb; k++) {
			results[k][t] = cellsurface.get(k);
					}
				}
		
		IJ.log("Total number of cells " + cellnb);
		ResultsTable rt = new ResultsTable();
		
		
		for (int t=0; t<nt; t++) {
			rt.incrementCounter();
			for (int c=0; c<cellnb; c++) {
				rt.addValue("Cell " + c, results[c][t]);
			}
		}
		
		rt.show("Results");
		
		Plot plot = new Plot("Surface", "Time", "Cell volume");
		for (int c=0; c<cellnb; c++) {
			plot.add("-", rt.getColumnAsDoubles(c));
			Random rand = new Random();
			float r = rand.nextFloat();
			float g = rand.nextFloat();
			float b = rand.nextFloat();
			Color randomColor = new Color(r, g, b);
			plot.setColor(randomColor);
		}
		plot.show();
		
}
	
}

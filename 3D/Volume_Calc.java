import java.awt.Color;
import java.util.ArrayList;
import java.util.Random;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.Plot;
import ij.measure.ResultsTable;
import ij.plugin.PlugIn;
import ij.process.ImageProcessor;


public class Volume_Calc implements PlugIn {

	public void run(String arg) {
		
		ImagePlus in = IJ.getImage().duplicate();
		int nx = in.getWidth();
		int ny = in.getHeight();
		int nt = in.getNFrames();
		int nz = in.getNSlices();
		IJ.log("Size: " + " [" + nx + " " + ny + "]");
		IJ.log("Z stack " + nz);
		IJ.log("Time frames " + nt);
		double[][] results = new double [100][nt];
		int cellnb =0;
		ArrayList<Double> color = new ArrayList ();
		ArrayList<Double> cellvolume = new ArrayList (); // in number of voxels
		
		// As label is uncalibrated, so I enter the values by hand		
		double vsize = 0.2752 * 0.2752; //in µm^3
		for (int t=0; t<nt; t++) {
			// Initializing the volume of the cells
			double pixel = 0.0;
			for(int l=0; l<cellvolume.size(); l++) {
				cellvolume.set(l, 0.0);
			}		
		
			// Swiping all images of the stack and counting the pixels of each color
			for (int z = 0; z < nz; z++) {
			in.setPosition(1, z+1, t+1);
			ImageProcessor ipin = in.getProcessor();
				for (int x=0; x<nx; x++) {
						for (int y=0; y<ny; y++) {
							pixel = ipin.getPixelValue(x, y);
							if (pixel !=0) {
								if (color.contains(pixel)){
									int nb = color.indexOf(pixel);
									double vol = cellvolume.get(nb);
									cellvolume.set(nb, vol+vsize);
								}
								else {
									cellnb = cellnb +1;
									color.add(pixel);
									cellvolume.add(vsize);						
								}
							}
						}
				}
		}
		// Turning the results into a result table for readability
			for (int k=0; k<cellnb; k++) {
				results[k][t] = cellvolume.get(k);
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
			
			// Plotting the results in a graph
			Plot plot = new Plot("Cells Volume over time", "Time", "Cell volume");
			for (int c=0; c<cellnb; c++) {
				plot.add("line", rt.getColumnAsDoubles(c));
				Random rand = new Random();
				float r = rand.nextFloat();
				float g = rand.nextFloat();
				float b = rand.nextFloat();
				Color randomColor = new Color(r, g, b);
				plot.setColor(randomColor);
			}
			
			plot.show();
			//IJ.saveAs(plot, "Tiff", dir + "/Cells Volume.tif");
					
		}
}

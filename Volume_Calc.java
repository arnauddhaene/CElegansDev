import java.util.ArrayList;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
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
		double vsize = 0.0032*0.0032; //in cm^3
		for (int t=0; t<nt; t++) {
		
			double pixel = 0.0;
		//Initializing the volume of the cells to 0
			for(int l=0; l<cellvolume.size(); l++) {
				cellvolume.set(l, 0.0);
			}		
		
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
		
		for (int k=0; k<cellnb; k++) {
			results[k][t] = cellvolume.get(k);
					}
				}
		
		IJ.log("Total number of cells " + cellnb);
		ResultsTable rt = new ResultsTable();
		
		
		for (int c=0; c<cellnb; c++) {
			rt.incrementCounter();
			for (int l=0; l<nt; l++) {
				//IJ.log(""+ results[c][l]);
				rt.addValue("T " + l, results[c][l]);
			}
		}
		
		rt.show("Results");
		//in.setPosition(1, nz, nt);
		//in.show();
}}

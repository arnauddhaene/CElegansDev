import java.util.ArrayList;
import java.util.Arrays;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.measure.ResultsTable;
import ij.plugin.PlugIn;
import ij.process.ImageProcessor;


public class Morpho_3D implements PlugIn {
	
	public void run(String arg) {
		
		ImagePlus in = IJ.getImage().duplicate();
		GenericDialog dlg = new GenericDialog("Morphological Operators Parameters");
		dlg.addNumericField("Size of 3D circular structuring element (odd)", 3, 1);
		dlg.showDialog();
		if (dlg.wasCanceled()) return;
		int size = (int) dlg.getNextNumber();
		boolean[][][] b = ball(size);
		ImagePlus out = close (in, b);
		out.show();
	
	}
	
	public double[][][] neighborhood (ImagePlus in, int x, int y, int z, int t, int L){
		double [][][] b = new double [L][L][L];
		//Arrays.fill(b, 0);
		int l= (L-1)/2;
		
		if (z>l && z<(in.getNSlices()-l)) {
		if (x>l && x<(in.getWidth()-l) && y>l && y<(in.getHeight()-l)) {
			for (int i=0; i<L; i++) {
				for (int j=0; j<L; j++) {
					for (int k=0; k<L; k++) {
						in.setPosition(1, z-l+k, t);
						ImageProcessor ip = in.getProcessor();
						b[i][j][k] = ip.getPixelValue(x-l+i, y-l+j);
					}
					
				}
			}
		}
		}
		
		return b;
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
		 
	public ImagePlus dilation (ImagePlus in, boolean mask[][][]) {
		 int size = mask.length;
		 int nx = in.getWidth();
		 int ny = in.getHeight();
		 int nt = in.getNFrames();
		 int nz = in.getNSlices();
		 int bitdepth = in.getBitDepth();
		 ImagePlus output = IJ.createHyperStack("Out", nx, ny, 1, nz, nt, bitdepth);
		 for (int t=0; t<nt; t++) {
			 for (int x=0; x<nx; x++) {
				 for (int y=0; y<ny; y++) {
					 for (int z=0; z<nz; z++) {
						 double [][][] block = neighborhood (in, x, y, z, t, size);
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
						 output.setPosition(1,z,t);
						 ImageProcessor ip = output.getProcessor();
						 ip.putPixelValue(x,y,max);						 
					 }
				 }				 
			 }
		 }
		 return output;
	}
		 
	public ImagePlus erosion(ImagePlus in, boolean mask[][][]) {
		 int size = mask.length;
		 int nx = in.getWidth();
		 int ny = in.getHeight();
		 int nt = in.getNFrames();
		 int nz = in.getNSlices();
		 int bitdepth = in.getBitDepth();
		 ImagePlus output = IJ.createHyperStack("Out", nx, ny, 1, nz, nt, bitdepth);
		 for (int t=0; t<nt; t++) {
			 for (int x=0; x<nx; x++) {
				 for (int y=0; y<ny; y++) {
					 for (int z=0; z<nz; z++) {
						 double [][][] block = neighborhood (in, x, y, z, t, size);
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
						 output.setPosition(1,z,t);
						 ImageProcessor ip = output.getProcessor();
						 ip.putPixelValue(x,y,min);	
					 }
				 }				 
			 }
		 }
		 return output;
	}
		 
	public ImagePlus close(ImagePlus in, boolean mask[][][]) {
		 return erosion(dilation(in,mask),mask);
		 }

		 

}

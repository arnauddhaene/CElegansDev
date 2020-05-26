

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.plugin.Filters3D;
import ij.plugin.GaussianBlur3D;
import ij.plugin.PlugIn;
import ij.process.ImageProcessor;


public class Morpho_3D implements PlugIn {
	
	public void run(String arg) {
		
		ImagePlus input = IJ.getImage().duplicate();
		
		ImageStack is = input.getImageStack();
		ImageStack iss = Filters3D.filter(is, 11, 2, 2, 2);
		
		ImagePlus in = new ImagePlus("in",  iss);
		
//		GaussianBlur3D.blur(input, 1.25, 1.25, 1.2);
		
//		ImagePlus out = gaussian3D (in, 1.5, 1.5);
		
		
		//GenericDialog dlg = new GenericDialog("Morphological Operators Parameters");
		//dlg.addNumericField("Size of 3D circular structuring element (odd)", 3, 1);
		//dlg.showDialog();
		//if (dlg.wasCanceled()) return;
		//int size = (int) dlg.getNextNumber();
		//boolean[][][] b = ball(size);
		//ImagePlus out = close (in, b);
		
		in.show();
	
	}
	
	public double[][][] neighborhood (ImagePlus in, int x, int y, int z, int t, int Lxy, int Lz){
		double [][][] b = new double [Lxy][Lxy][Lz];
		//Arrays.fill(b, 0);
		int lxy= (Lxy-1)/2;
		int lz= (Lz-1)/2;
		
		if (z>lz && z<(in.getNSlices()-lz)) {
		if (x>lxy && x<(in.getWidth()-lxy) && y>lxy && y<(in.getHeight()-lxy)) {
			for (int i=0; i<Lxy; i++) {
				for (int j=0; j<Lxy; j++) {
					for (int k=0; k<Lz; k++) {
						in.setPosition(1, z-lz+k, t);
						ImageProcessor ip = in.getProcessor();
						b[i][j][k] = ip.getPixelValue(x-lxy+i, y-lxy+j);
					}
					
				}
			}
		}
		}
		if (z<=lz) {
			if (x>lxy && x<(in.getWidth()-lxy) && y>lxy && y<(in.getHeight()-lxy)) {
				for (int i=0; i<Lxy; i++) {
					for (int j=0; j<Lxy; j++) {
						for (int k=0; k<Lz; k++) {
							while(z-lz+k<0) {
								b[i][j][k] = 0;
							}
							in.setPosition(1, z-lz+k, t);
							ImageProcessor ip = in.getProcessor();
							b[i][j][k] = ip.getPixelValue(x-lxy+i, y-lxy+j);
						}
					}}
			}
			
			if (z>=(in.getNSlices()-lz)) {
				if (x>lxy && x<(in.getWidth()-lxy) && y>lxy && y<(in.getHeight()-lxy)) {
					for (int i=0; i<Lxy; i++) {
						for (int j=0; j<Lxy; j++) {
							for (int k=0; k<Lz; k++) {
								while(z-lz+k<=in.getNSlices()) {
									in.setPosition(1, z-lz+k, t);
									ImageProcessor ip = in.getProcessor();
									b[i][j][k] = ip.getPixelValue(x-lxy+i, y-lxy+j);
								}
								b[i][j][k]=0;
							}
						}}
				}
			
		}}
		
		
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
						 IJ.log(""+z);
						 double [][][] block = neighborhood (in, x, y, z, t, size, size);
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
						 double [][][] block = neighborhood (in, x, y, z, t, size, size);
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

	public ImagePlus gaussian3D (ImagePlus in, double sigmaxy, double sigmaz) {
		int nx = in.getWidth();
		 int ny = in.getHeight();
		 int nt = in.getNFrames();
		 int nz = in.getNSlices();
		 int bitdepth = in.getBitDepth();
		 ImagePlus output = IJ.createHyperStack("Out", nx, ny, 1, nz, nt, bitdepth);
		 int lxy = (int) (2 * Math.ceil(3 * sigmaxy) + 1);
		 int lz = (int) (2*Math.ceil(3*sigmaz)+1);
		 		 
		 double[][][] g3D = new double [lxy][lxy][lz];
		 for (int x=0; x<lxy; x++) {
			 for(int y=0; y<lxy;y++) {
				 for (int z=0; z<lz; z++) {			 
				 g3D[x][y][z] = Math.exp(-(Math.pow(x-(lxy-1)/2,2)+Math.pow(y-(lxy-1)/2,2))/(2*sigmaxy*sigmaxy)-Math.pow(z-(lz-1)/2,2)/(2*sigmaz*sigmaz))/(2*Math.PI*sigmaxy*sigmaxy*sigmaz*Math.sqrt(2*Math.PI));
				 }}}
		 for (int t=0; t<nt; t++) {
			 for(int x=0; x<nx; x++) {
				 for(int y=0; y<ny; y++) {
					 for (int z=0; z<nz; z++){
						 double[][][] b = neighborhood(in, x, y, z, t, lxy, lz);
						 double pixel = 0.0;
			 			 for (int i=0; i<lxy; i++) {
			 				 for (int j=0; j<lxy; j++) {
			 					 for (int k=0; k<lz; k++) {
			 						pixel=pixel + g3D[i][j][k]*b[i][j][k];
			 					 }
			 				 }
			 			 }
			 			 output.setPosition(1,z,t);
						 ImageProcessor ip = output.getProcessor();
						 ip.putPixelValue(x,y,pixel);	
					 }}
			 			 
			 
			 }
			 }

		 
		 return output;
	}
		 

}

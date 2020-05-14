import ij.IJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.plugin.PlugIn;
import ij.plugin.filter.GaussianBlur;
import ij.process.ImageProcessor;

public class Canny_Edges implements PlugIn {

	public void run(String arg) {
		
		// Noise reduction for a single image
			ImagePlus in = IJ.getImage().duplicate();
			int nx = in.getWidth();
			int ny = in.getHeight();
			int nt = in.getNFrames();
			int nz = in.getSlice();
			GenericDialog dlg = new GenericDialog("Canny Edge");
			dlg.addNumericField("Gaussian Denoising", 1.5, 1);
			dlg.addNumericField ("X gradient sigma", 0.7, 1);
			dlg.addNumericField ("Y gradient sigma", 0.7, 1);
			dlg.showDialog();
			if (dlg.wasCanceled()) return;
			double sigma = dlg.getNextNumber();
			double sigmax = dlg.getNextNumber();
			double sigmay = dlg.getNextNumber();
			
			GaussianBlur gf = new GaussianBlur();
			
			for (int t = 0; t < nt; t++) {
				for (int z = 0; z < nz; z++) {
					in.setPosition(1, z+1,  t + 1);
					ImageProcessor ip = in.getProcessor();
					gf.blurGaussian(ip, sigma, sigma, 0.001);
				}
			}
			in.setPosition(1, 1, 1);
			//in.show();
		
	
	// Gradient calculation in X
		ImagePlus indx = in.duplicate();
		ImageProcessor ipin = in.getProcessor();
		ImageProcessor ipoutx = indx.getProcessor();
		int Lx = (int)(2*Math.ceil(3*sigmax)+1);
		int n = Lx-1-(Lx-1)/2;
		double [][] gradientx = new double [Lx][Lx];
		 	for (int x=0; x<Lx;x++) {
		 		for (int y=0; y<Lx;y++) {
		 gradientx[x][y]=-(2*(x-n)/Math.PI)*Math.exp(-(Math.pow(x-n, 2)+Math.pow(y-n, 2)));
		 }
		 }

		 //Convolution with the filter
		 for (int x=0; x<nx;x++) {
		 for (int y=0; y<ny;y++) {
		 double [][] b = getNeighborhood(in, x, y, Lx, Lx);
		 double pixel = 0.0;
		 for (int i=0; i<Lx; i++) {
		 for(int j=0; j<Lx; j++) {
		 pixel = pixel +gradientx[i][j]*b[i][j];
		 }
		 }
		 ipoutx.putPixelValue(x, y, Math.abs(pixel));
		 }
		 }

		// Gradient calculation in Y
			ImagePlus indy = in.duplicate();
			ImageProcessor ipouty = indy.getProcessor();
			int Ly = (int)(2*Math.ceil(3*sigmay)+1);
			n = Ly-1-(Ly-1)/2;
			double [][] gradienty = new double [Ly][Ly];
			 	for (int x=0; x<Ly;x++) {
			 		for (int y=0; y<Ly;y++) {
			 gradienty[x][y]=-(2*(y-n)/Math.PI)*Math.exp(-(Math.pow(y-n, 2)+Math.pow(x-n, 2)));
			 }
			 }

			 //Convolution with the filter
			 for (int x=0; x<nx;x++) {
			 for (int y=0; y<ny;y++) {
			 double [][] b = getNeighborhood(in, x, y, Ly, Ly);
			 double pixel = 0.0;
			 for (int i=0; i<Ly; i++) {
			 for(int j=0; j<Ly; j++) {
			 pixel = pixel + gradienty[i][j]*b[i][j];
			 }
			 }
			 ipouty.putPixelValue(x, y, Math.abs(pixel));
			 }
			 }

			 // Creation of the gradient map
			 ImagePlus gradientmap = in.duplicate();
			 ImagePlus orientmap = in.duplicate();
			 ImageProcessor ipgrad = gradientmap.getProcessor();
			 ImageProcessor iporient = orientmap.getProcessor();
			 double pix = 0.0;
			 double pixel = 0.0;
			 for (int x=0; x<nx;x++) {
				 for (int y=0; y<ny;y++) {
					 pix = Math.sqrt(Math.pow(ipoutx.getPixelValue(x, y),2)+Math.pow(ipouty.getPixelValue(x, y),2));
					 pixel = 128+ 2*255*Math.atan2(ipouty.getPixelValue(x,y), ipoutx.getPixelValue(x,y))/Math.PI;
					 ipgrad.putPixelValue(x,y,pix);
					 iporient.putPixelValue(x, y, pixel);
				 }
				 }
			 gradientmap.show();
			 orientmap.show();
			 
			 // Non maximum suppression
			 ImagePlus maxsup = in.duplicate();
			 ImageProcessor ipsup = maxsup.getProcessor();
			 for (int x=0; x<nx;x++) {
				 for (int y=0; y<ny;y++) {
				 double[][] nbh = getNeighborhood(gradientmap, x,y, 3,3);
				 	pixel = ipgrad.getPixelValue(x, y);
				 	for (int i = 0; i<3; i++) {
				 		for (int j=0; j<3; j++) {
				 			if (nbh [j][i] > pixel) {
				 				ipsup.putPixelValue(x, y, 0);
				 			}
				 			
				 		}
				 	}
				 }
				 }
			 maxsup.show();
	
	}

public double[][] getNeighborhood(ImagePlus in, int x, int y, int mx, int my) {
	int nx = in.getWidth();
	int ny= in.getHeight();
	ImageProcessor ipin= in.getProcessor();
	double neigh[][] = new double[mx][my];
	int bx = neigh.length;
	int by = neigh[0].length;
	int bx2 = (bx - 1) / 2;
	int by2 = (by - 1) / 2;
	if (x >= bx2)
		if (y >= by2)
			if (x < nx - bx2 - 1)
				if (y < ny - by2 - 1) {
					//int index = (y - by2) * nx + (x - bx2);
					for (int j = 0; j < by; j++) {
						for (int i = 0; i < bx; i++) {
							neigh[i][j] = ipin.getPixelValue(i+x-bx,j+y-by);
						}
						//index += (nx - bx);
					}
					return neigh;
				}
	int xt[] = new int[bx];
	for (int k = 0; k < bx; k++) {
		int xa = x + k - bx2;
		int periodx = 2 * nx - 2;
		while (xa < 0)
			xa += periodx; // Periodize
		while (xa >= nx) {
			xa = periodx - xa; // Symmetrize
			if (xa < 0)
				xa = -xa;
		}
		xt[k] = xa;
	}
	int yt[] = new int[by];
	for (int k = 0; k < by; k++) {
		int ya = y + k - by2;
		int periody = 2 * ny - 2;
		while (ya < 0)
			ya += periody; // Periodize
		while (ya >= ny) {
			ya = periody - ya; // Symmetrize
			if (ya < 0)
				ya = -ya;
		}
		yt[k] = ya;
	}
	
	return neigh;
}
}
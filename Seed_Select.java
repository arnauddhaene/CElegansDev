import java.awt.Polygon;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;


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
		
		OpenDialog od = new OpenDialog("Choose a .tif file", null);  
		String path = od.getPath();
		IJ.open(path);
		ImagePlus edges = IJ.getImage();
		
		
		/*
		// Opening the edges file
		String path = getPath(arg);  
        if (null == path) return; 
        ImagePlus edges = get_open(path, nx, ny, nz, nt);
        edges.show();
		*/
		
		
		
				
		int slice = in.getSlice();
		int frame = in.getFrame();
		if (frame < nt-1) {
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
		
		ImageProcessor imp = in.getProcessor();
		for (int i = 0; i < nri; i++){			
			Point3D seed = new Point3D(up.xpoints[i], up.ypoints[i], slice, frame, imp.getPixelValue(up.xpoints[i], up.ypoints[i]));
			seeds.add(seed);
			IJ.log("Seed added: " + seed.toString() + "for frame "+ nt);
		}
		
		IJ.log(seeds.size()+" seeds for frame " + (nt));
		// Looping over the frames
		for (int i=1; i<nt; i++) {			
			seeds = getSeed (edges, in, seeds, slice, nt-i);
			IJ.log(seeds.size()+" seeds for frame " + (nt-i));
		}
		
	}
	
	
	
	
	
	
	public ArrayList<Point3D> getSeed (ImagePlus edges, ImagePlus in, ArrayList<Point3D> seeds, int slice, int frame) { 
		in.setPosition(1, slice, frame);
		ImageProcessor ip = in.getProcessor();
		edges.setPosition(1,slice, frame);
		ImageProcessor ipedges = edges.getProcessor();
		ArrayList<Point3D> newSeeds = new ArrayList<Point3D>();
				
		int nri = seeds.size();
		while(nri>0) {
			Point3D A = seeds.get(0);
			int separation = 0; 
						
				for (int j=1; j<nri; j++) {
					Point3D B = seeds.get(j);
					if (isSeparated(ipedges,A,B)==false) {
						separation =j;											
					}
				}
				if(separation ==0) {
					Point3D C = new Point3D (A.getX(), A.getY(), slice, frame, ip.getPixelValue(A.getX(), A.getY()));
					newSeeds.add(C);
					seeds.remove(0);
					//IJ.log("Seed updated : " + C.getX() + "  "  + C.getY() + " for frame " + frame);				
				}
				else {
					Point3D B = seeds.get(separation);
					int x = (int) Math.floor((A.getX() + B.getX())/2);
					int y = (int) Math.floor((A.getY() + B.getY())/2);
					Point3D C = new Point3D (x, y, slice, frame, ip.getPixelValue(x, y));
					newSeeds.add(C);
					seeds.remove(separation);
					seeds.remove(0);
					//IJ.log("Seed created : " + C.getX() + "  "  + C.getY() + " for frame " + frame);	
				}
				nri = seeds.size();
			}
	
			
			
		return newSeeds;
		
	}
	
	
	public boolean isSeparated (ImageProcessor ipedges, Point3D A, Point3D B) {
		int xmin = Math.min(A.getX(), B.getX());
		int xmax;
		int ymin;
		int ymax;
		if (xmin==A.getX()) {
			xmax = B.getX();
			ymin = A.getY();
			ymax = B.getY();
		}
		else {
			xmax = A.getX();
			ymin = B.getY();
			ymax = A.getY();
		}
		double slope=0;
		if (xmax!=xmin) {
		slope = (ymax-ymin)/(xmax-xmin);
		}
		
		double intercept = ymax - slope*xmax;
		double intensity = 0;
		for (int i=0; i <(xmax-xmin); i++) {
			int x = xmin + i ;
			int y = (int) ((int) slope*x + intercept);
			intensity = intensity + ipedges.getPixelValue(x, y);
		}
		
		if (intensity ==0) {
			return false;
		}
		else {
			return true;
		}
		}
	
}
	
	
	/* Garbage Code to open the second image
	
	public String getPath(String arg) {  
        OpenDialog od = new OpenDialog("Choose a .tif file", null);  
        String dir = od.getDirectory();  
        if (null == dir) return null; // dialog was canceled  
        dir = dir.replace('\\', '/'); // Windows safe  
        if (!dir.endsWith("/")) dir += "/";  
        return dir + od.getFileName();  
    }  
	
	public InputStream open(String path) throws Exception {  
	       return new FileInputStream(path);  
	    }  
	
	 public ImagePlus get_open(String path, int nx, int ny, int nz, int nt) {  
	            
	         
	        // Build a new FileInfo object with all file format parameters and file data  
	        FileInfo fi = new FileInfo();  
	        fi.fileFormat = fi.RAW;  
	        int islash = path.lastIndexOf('/');  
	        if (0 == path.indexOf("http://")) {  
	            fi.url = path;  
	        } else {  
	            fi.directory = path.substring(0, islash+1);  
	        }  
	        fi.fileName = path.substring(islash+1);  
	        fi.width = nx;  
	        fi.height = ny;  
	        fi.nImages = nz*nt;
	        fi.gapBetweenImages = 0;  
	        fi.intelByteOrder = true; // little endian  
	        fi.whiteIsZero = false; // no inverted LUT  
	        //fi.longOffset = fi.offset = 512; // header size, in bytes  
	  
	        // Now make a new ImagePlus out of the FileInfo  
	        // and integrate its data into this PlugIn, which is also an ImagePlus  
	         
	            FileOpener fo = new FileOpener(fi);  
	            ImagePlus imp = fo.open(false);  
	            imp.setStack(imp.getTitle(), imp.getStack());  
	            imp.setCalibration(imp.getCalibration());  
	            Object obinfo = imp.getProperty("Info");  
	            if (null != obinfo) imp.setProperty("Info", obinfo);  
	            imp.setFileInfo(imp.getOriginalFileInfo());    
	        return imp ; 
	    }
	    
	    public final int readIntLittleEndian(byte[] buf, int start) {  
	        return (buf[start]) + (buf[start+1]<<8) + (buf[start+2]<<16) + (buf[start+3]<<24);  
	    }  
	    
	
	

	}

*/


import java.util.ArrayList;

import ij.IJ;
import ij.ImagePlus;
import ij.process.ImageProcessor;

public class StarRegion {
	
	public StarRegion(Point3D seed, int arms) {
		
		this.seed = seed;
		this.arms = new ArrayList<StarArm>();
		
		double goldenRatio = (1 + Math.sqrt(5)) / 2.0;
		
		for (int a = 0; a < arms; a++) {
			
			double i = 0.5 + a;
					
			double phi = (2 * Math.PI * i) / goldenRatio;
			double theta = Math.acos(1 - 2 * i / arms);
			
			this.arms.add(new StarArm(phi, theta));
			
			IJ.log("Arm added with phi=" + phi + " and theta=" + theta);
			
		}
		
	}
	
	public Point3D seed;
	
	public ArrayList<StarArm> arms;
	
	public Point3D getSeed() {
		return this.seed;
	}
	
	public StarArm getArm(int index) {
		return this.arms.get(index);
	}
	
	public int size() {
		return this.arms.size();
	}

}

import java.util.ArrayList;

import ij.IJ;

public class Region3D {
	
	public Point3D seed;
	
	public ArrayList<Point3D> points;

	private double mean;
	public double Xcentroid;
	public double Ycentroid;
	public int centralsize;
	
	
	// Something to do with dimensionality ;)
	static double vicinity = 3;

	public Region3D() {
		
		// Initialize with seed (0, 0)
		this(new Point3D());
	}
	
	public Region3D(Point3D seed) {
		
		// Initialize
		this.points = new ArrayList<Point3D>();
		
		// Add seed
		this.seed = seed;
		this.points.add(seed);
		this.mean = seed.getValue();
		this.Xcentroid = (double) seed.getX();
		this.Ycentroid = (double) seed.getY();
		this.centralsize = 1 ; 
		this.addPoint(seed);
		
		
	}
	
	public int size() {
		return this.points.size();
	}

	public Point3D getSeed() {
		return this.seed;
	}
	
	public boolean isInVicinity(Point3D point, int amount) {
	
		int number = 0;
		double xdiff = this.getSeed().getX() - point.getX();
		double ydiff = this.getSeed().getY() - point.getY();
		double zdiff = this.getSeed().getZ() - point.getZ();
		
		int xdir = (int) Math.signum(xdiff);
		int ydir = (int) Math.signum(ydiff);
		int zdir = (int) Math.signum(zdiff);

		boolean radiallyConvex = false;
		
		for (int i = this.points.size() - 1; i  >=  0; i--) {		
			if (point.getRasterDistanceFrom(this.points.get(i)) <= vicinity)
				number++;
			
//			if (this.points.get(i).getX() == point.getX() + xdir &&
//				this.points.get(i).getY() == point.getY() + ydir &&
//				this.points.get(i).getZ() == point.getZ() + zdir)
//					radiallyConvex = true;
			
			if (number >= amount) // && radiallyConvex)
				return true;
		}
		
//		IJ.log(point.toString() + " has " + number + " neighbors.");
			
		return false;
		
	}
	
	
	public boolean overlaps(Region3D other) {
		
		for (int i = 0; i < this.size(); i++) {
			
			if (other.points.contains(this.points.get(i)))
				return true;
			
		}
		
		return false;
		
	}
	
	public boolean overlaps(Point3D other) {
		return this.points.contains(other);
	}
	
	public Point3D getPoint(int index) {
		return this.points.get(index);
	}
	
	public void addPoint(Point3D point) {
		
		// Add to mean incrementally
		double total = this.mean * this.points.size();
		
		this.points.add(point);
		
		
		
		// Adjust the centroid only on the central slice
		if (this.seed.getZ()== point.getZ()) {
			double xtot = this.Xcentroid * this.centralsize;		
			double ytot = this.Ycentroid * this.centralsize;	
			xtot  += point.getX();
			ytot  += point.getY();
			this.centralsize +=1;
			this.Xcentroid = xtot / (double) this.centralsize;
			this.Ycentroid = ytot / (double) this.centralsize;
		//this.Xcentroid = (int) (this.Xcentroid*this.centralsize+point.getX())/(this.centralsize +1);
		//this.Ycentroid = (int) (this.Ycentroid*this.centralsize+point.getY())/(this.centralsize +1);
		//this.centralsize+=1;
		}
		total += point.getValue();
		
		this.mean = total / (double) this.points.size();
		
	}
	
	public double getDistanceToSeed(Point3D point) {
		return this.seed.getDistanceFrom(point);
	}
	
	public double getMean() {
		return this.mean; 
	}
	
	public int getXCentroid() {
		if ((this.Xcentroid - Math.floor(this.Xcentroid))<0.5){
			return (int) Math.floor(this.Xcentroid);
		}
		return (int) Math.ceil(this.Xcentroid);
		
	}
	
	public int getYCentroid() {
		if ((this.Ycentroid - Math.floor(this.Ycentroid))<0.5){
			return (int) Math.floor(this.Ycentroid);
		}
		return (int) Math.ceil(this.Ycentroid);
	}
	
	
}

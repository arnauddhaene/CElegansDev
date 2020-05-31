import java.util.ArrayList;

import ij.IJ;

public class Region3D {
	
	public Point3D seed;
	
	public ArrayList<Point3D> points;

	private double mean;
	
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
		
	}
	
	public int size() {
		return this.points.size();
	}

	public Point3D getSeed() {
		return this.seed;
	}
	
	public boolean isInVicinity(Point3D point, int amount) {
	
		int number = 0;
		int xdir = (int) Math.signum(this.getSeed().getX() - point.getX());
		int ydir = (int) Math.signum(this.getSeed().getY() - point.getY());
		int zdir = (int) Math.signum(this.getSeed().getZ() - point.getZ());
		boolean radiallyConvex = false;
		
		for (int i = this.points.size() - 1; i  >=  0; i--) {		
			if (point.getRasterDistanceFrom(this.points.get(i)) <= vicinity)
				number++;
			
			if (this.points.get(i).getX() == point.getX() + xdir &&
				this.points.get(i).getY() == point.getY() + ydir &&
				this.points.get(i).getZ() == point.getZ() + zdir)
				radiallyConvex = true;
			
			if (number >= amount && radiallyConvex)
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
		
		total += point.getValue();
		
		this.mean = total / (double) this.points.size();
		
	}
	
	public double getDistanceToSeed(Point3D point) {
		return this.seed.getDistanceFrom(point);
	}
	
	public double getMean() {
		return this.mean; 
	}
	
	
	
}


import java.util.ArrayList;

public class Region3D {
	
	public Point3D seed;
	
	public ArrayList<Point3D> points;

	private double mean;

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
		
		// x and y have 0.2752 um while z has 1 um
		double d = 0.2752 * 0.2752;
		
		return  (this.seed.getX() - point.getX()) * (this.seed.getX() - point.getX()) / d +
				(this.seed.getY() - point.getY()) * (this.seed.getY() - point.getY()) / d +
				(this.seed.getZ() - point.getZ()) * (this.seed.getZ() - point.getZ());
	}
	
	public double getMean() {
		return this.mean; 
	}
	
	
	
}


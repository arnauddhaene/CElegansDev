import java.util.ArrayList;

public class Region3D {
	
	public Point3D seed;
	
	public ArrayList<Point3D> points;

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
		this.points.add(point);
	}
	
	public double getDistanceToSeed(Point3D point) {
		return Math.sqrt(
				Math.pow(this.seed.getX() - point.getX(), 2) +
				Math.pow(this.seed.getY() - point.getY(), 2) +
				Math.pow(this.seed.getZ() - point.getZ(), 2));
	}
	
	public double getMean() {
		
		double sum = 0;
		
		if(!this.points.isEmpty()) {
			
			for (Point3D pt : this.points) {
				
				sum += pt.getValue();
			}
			return sum / this.points.size();
		}
		return sum; 
	}
	
	
	
}


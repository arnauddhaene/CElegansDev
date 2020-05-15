import java.util.ArrayList;
import java.awt.Point;

public class Region2D {
	
	public Point seed;
	
	public ArrayList<Point> points;
	
	public ArrayList<Double> values;
	
	public ArrayList<Point> fronts;

	public Region2D() {
		
		// Initialize with seed (0, 0)
		this(new Point(), 0.0);
	}
	
	public Region2D(Point seed, double value) {
		
		// Initialize
		this.points = new ArrayList<Point>();
		this.fronts = new ArrayList<Point>();
		this.values = new ArrayList<Double>();
		
		// Add seed
		this.seed = seed;
		this.points.add(seed);
		this.values.add(value);
		
	}
	
	public int size() {
		return this.points.size();
	}
	
	
	public boolean overlaps(Region2D other) {
		
		for (int i = 0; i < this.size(); i++) {
			
			if (other.points.contains(this.points.get(i)))
				return true;
			
		}
		
		return false;
		
	}
	
	public boolean overlaps(Point other) {
		return this.points.contains(other);
	}
	
	public Point getPoint(int index) {
		return this.points.get(index);
	}
	
	public void addPoint(Point point, double value) {
		this.points.add(point);
		this.values.add(value);
	}
	
	public double getDistanceToSeed(Point point) {
		return Math.hypot(this.seed.getX() - point.getX(), this.seed.getY() - point.getY());
	}
	
	public double getMean() {
		
		double sum = 0;
		
		if(!this.values.isEmpty()) {
			
			for (double value : this.values) {
				
				sum += value;
			}
			return sum / this.values.size();
		}
		return sum; 
	}
	
	
	
}

import java.util.ArrayList;
import java.awt.Point;

public class Region2D {
	
	// Attributes
	
	private Point2D seed;
	
	private double mean;
	
	private ArrayList<Point2D> points;
	
	private double[] barycenter;
	
	static double vicinity = 2;
	
	// Constructor
	
	public Region2D(Point2D seed) {
		
		// Initialize
		this.points = new ArrayList<Point2D>();
		this.barycenter = new double[2];
		
		// Add seed
		this.seed = seed;
		this.addPoint(seed);
		
	}
	
	// Methods
	
	public int size() {
		return this.points.size();
	}
	
	public boolean overlaps(Point2D other) {
		return this.points.contains(other);
	}
	
	public Point2D getPoint(int index) {
		return this.points.get(index);
	}
	
	public double getDistanceToSeed(Point2D point) {
		return this.seed.getDistanceFrom(point);
	}
	
	public double getMean() {
		return this.mean;
	}
	
	public Point2D getSeed() {
		return this.seed;
	}
	
	public boolean isInVicinity(Point2D point, int amount) {
		
		int number = 0;
		
		for (int i = this.points.size() - 1; i  >=  0; i--) {		
			if (point.getDistanceFrom(this.points.get(i)) <= vicinity)
				number++;
			
			if (number >= amount)
				return true;
		}
		
//		IJ.log(point.toString() + " has " + number + " neighbors.");
			
		return false;
		
	}
	
	public void addPoint(Point2D point) {
		
		// Add to mean incrementally
		double total = this.mean * this.size();
		
		double xtot = (barycenter != null) ? this.barycenter[0] * this.size() : 0;
		double ytot = (barycenter != null) ? this.barycenter[1] * this.size() : 0;
		
		this.points.add(point);
		
		total += point.getValue();
		xtot  += point.getX();
		ytot  += point.getY();
		
		this.barycenter[0] = xtot / (double) this.size();
		this.barycenter[1] = ytot / (double) this.size();
		
		this.mean = total / (double) this.size();
		
	}
	
	
}

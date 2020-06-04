/**
 * Class representing a point in 2D
 *
 * Created by Arnaud Dhaene (EPFL)
 */
public class Point2D implements java.io.Serializable {

	public int x;
	public int y;
	public int t;
	public double color;
	public double value;


	public Point2D() {
		this(0, 0, 0, 0, 0.0);
	}

	public Point2D(int x, int y, int t, double color, double value) {
		this.x = x;
		this.y = y;
		this.t = t;
		this.color = color;
		this.value = value;
	}

	public Point2D(int x, int y, int t, double value) {
		this.x = x;
		this.y = y;
		this.t = t;
		this.value = value;
	}

	public int getX() {
		return this.x;
	}

	public int getY() {
		return this.y;
	}

	public int getT() {
		return this.t;
	}

	public double getColor() {
		return this.color;
	}

	public double getValue() {
		return this.value;
	}

	public double getDistanceFrom(Point2D other) {
		// Doesn't include square root for optimization purposes

		return  (this.getX() - other.getX()) * (this.getX() - other.getX()) +
				(this.getY() - other.getY()) * (this.getY() - other.getY());
	}

	public boolean equals(Object obj) {

		// We selectively do not take into account the value for comparison

		if (obj instanceof Point2D) {
            Point2D pt = (Point2D) obj;
            return (this.x == pt.x) && (this.y == pt.y);
        }
        return super.equals(obj);

	}

	public String toString() {
		return getClass().getName() + "[x=" + x + ", y=" + y + ", t=" + t + "]";
	}

}

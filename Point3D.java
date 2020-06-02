public class Point3D implements java.io.Serializable {

	public int x;
	public int y;
	public int z;
	public int t;
	public double color;
	public double value;


	public Point3D() {
		this(0, 0, 0, 0, 0.0, 0.0);
	}

	public Point3D(int x, int y, int z, int t, double color, double value) {
		this.x = x;
		this.y = y;
		this.z = z;
		this.t = t;
		this.color = color;
		this.value = value;
	}

	public int getX() {
		return this.x;
	}

	public int getY() {
		return this.y;
	}

	public int getZ() {
		return this.z;
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
	
	public double getDistanceFrom(Point3D other) {
		// Doesn't include square root for optimization purposes
		
		// isotropy in the lateral view
		// x and y have 0.2752 um while z has 1 um
		double d = 0.2752 * 0.2752;
		
		return  (this.getX() - other.getX()) * (this.getX() - other.getX()) * d +
				(this.getY() - other.getY()) * (this.getY() - other.getY()) * d +
				(this.getZ() - other.getZ()) * (this.getZ() - other.getZ());
	}
	
	public double getRasterDistanceFrom(Point3D other) {
		return  (this.getX() - other.getX()) * (this.getX() - other.getX()) +
				(this.getY() - other.getY()) * (this.getY() - other.getY()) +
				(this.getZ() - other.getZ()) * (this.getZ() - other.getZ());
	}

	public boolean equals(Object obj) {

		// We selectively do not take into account the value for comparison

		if (obj instanceof Point3D) {
            Point3D pt = (Point3D) obj;
            return (this.x == pt.x) && (this.y == pt.y) && (this.z == pt.z);
        }
        return super.equals(obj);

	}

	public String toString() {
		return getClass().getName() + "[x=" + x + ", y=" + y + ", z=" + z + ", t=" + t + "]";	
	}

}
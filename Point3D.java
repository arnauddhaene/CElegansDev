
public class Point3D implements java.io.Serializable {
	
	public int x;
	public int y;
	public int z;
	public double value;
	
	public Point3D() {
		this(0, 0, 0, 0.0);
	}
	
	public Point3D(int x, int y, int z, double value) {
		this.x = x;
		this.y = y;
		this.z = z;
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
	
	public double getValue() {
		return this.value;
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
		return getClass().getName() + "[x=" + x + ", y=" + y + ", z=" + z + "]";	
	}
	
	
}

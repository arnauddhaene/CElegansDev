
public class StarArm {
	
	public StarArm(double phi, double theta) {
		this.phi = phi;
		this.theta = theta;
		this.length = 50.0;
	}
	
	public double length;
	
	public double phi;
	
	public double theta;
	
	public double getTheta() {
		return this.theta;
	}
	
	public double getPhi() {
		return this.phi;
	}
	
	public double getLength() {
		return this.length;
	}
	
	public double getX() {
		return this.length * Math.sin(this.theta) * Math.cos(this.phi);
	}
	
	public double getY() {
		return this.length * Math.sin(this.theta) * Math.sin(this.phi);
	}
	
	public double getZ() {
		return this.length * Math.cos(this.theta);
	}
	
	public double[] getCoordinates(double length) {
		return new double[]{ 
				length * Math.sin(this.theta) * Math.cos(this.phi), 
				length * Math.sin(this.theta) * Math.sin(this.phi),
				length * Math.cos(this.theta)};
	}
}

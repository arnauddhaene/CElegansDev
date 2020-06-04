import java.util.ArrayList;

/**
 * Class representing a time-frame
 *
 * Created by Arnaud Dhaene (EPFL)
 */
public class TimeFrame {

	// Attributes

	private ArrayList<Region2D> regions;

	// Constructor

	public TimeFrame(ArrayList<Region2D> regions) {
		this.regions = regions;
	}

	// Methods

	public int size() {
		return this.regions.size();
	}

	public Region2D get(int index) {
		return this.regions.get(index);
	}

	public ArrayList<Region2D> getRegions() {
		return this.regions;
	}

	public Region2D getRegion(int index) {
		return this.regions.get(index);
	}
}

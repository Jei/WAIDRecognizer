/**
 * 
 */
package it.unibo.cs.jonus.waidrec;

/**
 * @author jei
 * 
 */
public class VehicleInstance {

	private long timestamp;
	private String category;
	private MagnitudeFeatures accelFeatures;
	private MagnitudeFeatures gyroFeatures;

	/**
	 * 
	 */
	public VehicleInstance() {
		// TODO Auto-generated constructor stub
	}

	/**
	 * @return the timestamp
	 */
	public long getTimestamp() {
		return timestamp;
	}

	/**
	 * @param timestamp
	 *            the timestamp to set
	 */
	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}

	/**
	 * @return the category
	 */
	public String getCategory() {
		return category;
	}

	/**
	 * @param category
	 *            the category to set
	 */
	public void setCategory(String category) {
		this.category = category;
	}

	/**
	 * @return the accelFeatures
	 */
	public MagnitudeFeatures getAccelFeatures() {
		return accelFeatures;
	}

	/**
	 * @param accelFeatures
	 *            the accelFeatures to set
	 */
	public void setAccelFeatures(MagnitudeFeatures accelFeatures) {
		this.accelFeatures = accelFeatures;
	}

	/**
	 * @return the gyroFeatures
	 */
	public MagnitudeFeatures getGyroFeatures() {
		return gyroFeatures;
	}

	/**
	 * @param gyroFeatures
	 *            the gyroFeatures to set
	 */
	public void setGyroFeatures(MagnitudeFeatures gyroFeatures) {
		this.gyroFeatures = gyroFeatures;
	}

}

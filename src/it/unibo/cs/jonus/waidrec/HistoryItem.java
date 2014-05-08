/**
 * 
 */
package it.unibo.cs.jonus.waidrec;

/**
 * @author jei
 * 
 */
public class HistoryItem {

	private long timestamp;
	private String category;
	private MagnitudeFeatures accelFeatures;
	private MagnitudeFeatures gyroFeatures;

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
	 * @return the accelerometer features object
	 */
	public MagnitudeFeatures getAccelFeatures() {
		return accelFeatures;
	}

	/**
	 * @param accelFeatures
	 *            the accelerometer features to set
	 */
	public void setAccelFeatures(MagnitudeFeatures accelFeatures) {
		this.accelFeatures = accelFeatures;
	}

	/**
	 * @return the gyroscope features object
	 */
	public MagnitudeFeatures getGyroFeatures() {
		return gyroFeatures;
	}

	/**
	 * @param gyroFeatures
	 *            the gyroscope features to set
	 */
	public void setGyroFeatures(MagnitudeFeatures gyroFeatures) {
		this.gyroFeatures = gyroFeatures;
	}

}

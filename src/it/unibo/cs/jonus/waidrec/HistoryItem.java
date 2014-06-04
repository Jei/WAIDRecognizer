/**
 * 
 */
package it.unibo.cs.jonus.waidrec;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * @author jei
 * 
 */
public class HistoryItem implements Parcelable {

	private long timestamp;
	private String category;
	private MagnitudeFeatures accelFeatures;
	private MagnitudeFeatures gyroFeatures;
	
	public HistoryItem() {
		this.timestamp = 0;
		this.category = "";
		this.accelFeatures = new MagnitudeFeatures();
		this.gyroFeatures = new MagnitudeFeatures();
	}
	
	public HistoryItem(Parcel in) {
		this.timestamp = in.readLong();
		this.category = in.readString();
		this.accelFeatures = in.readParcelable(MagnitudeFeatures.class.getClassLoader());
		this.gyroFeatures = in.readParcelable(MagnitudeFeatures.class.getClassLoader());
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

	@Override
	public int describeContents() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeLong(timestamp);
		dest.writeString(category);
		dest.writeParcelable(accelFeatures, 0);
		dest.writeParcelable(gyroFeatures, 0);
	}

	public static final Parcelable.Creator<HistoryItem> CREATOR = new Parcelable.Creator<HistoryItem>() {

		@Override
		public HistoryItem createFromParcel(Parcel source) {
			return new HistoryItem(source);
		}

		@Override
		public HistoryItem[] newArray(int size) {
			// TODO Auto-generated method stub
			return new HistoryItem[size];
		}

	};

}

package it.unibo.cs.jonus.waidrec;

import android.os.Parcel;
import android.os.Parcelable;

public class MagnitudeFeatures implements Parcelable {

	private Double average;
	private Double maximum;
	private Double minimum;
	private Double standardDeviation;
	
	public MagnitudeFeatures() {
		this.average = (double) 0;
		this.maximum = (double) 0;
		this.minimum = (double) 0;
		this.standardDeviation = (double) 0;
	}
	
	public MagnitudeFeatures(Parcel in) {
		this.average = in.readDouble();
		this.maximum = in.readDouble();
		this.minimum = in.readDouble();
		this.standardDeviation = in.readDouble();
	}

	/**
	 * @return the average
	 */
	public Double getAverage() {
		return average;
	}

	/**
	 * @param average
	 *            the average to set
	 */
	public void setAverage(Double average) {
		this.average = average;
	}

	/**
	 * @return the maximum
	 */
	public Double getMaximum() {
		return maximum;
	}

	/**
	 * @param maximum
	 *            the maximum to set
	 */
	public void setMaximum(Double maximum) {
		this.maximum = maximum;
	}

	/**
	 * @return the minimum
	 */
	public Double getMinimum() {
		return minimum;
	}

	/**
	 * @param minimum
	 *            the minimum to set
	 */
	public void setMinimum(Double minimum) {
		this.minimum = minimum;
	}

	/**
	 * @return the standard deviation
	 */
	public Double getStandardDeviation() {
		return standardDeviation;
	}

	/**
	 * @param standard
	 *            deviation the standard deviation to set
	 */
	public void setStandardDeviation(Double standardDeviation) {
		this.standardDeviation = standardDeviation;
	}

	@Override
	public int describeContents() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeDouble(average);
		dest.writeDouble(maximum);
		dest.writeDouble(minimum);
		dest.writeDouble(standardDeviation);
	}

	public static final Parcelable.Creator<MagnitudeFeatures> CREATOR = new Parcelable.Creator<MagnitudeFeatures>() {

		@Override
		public MagnitudeFeatures createFromParcel(Parcel source) {
			return new MagnitudeFeatures(source);
		}

		@Override
		public MagnitudeFeatures[] newArray(int size) {
			return new MagnitudeFeatures[size];
		}
	};

}

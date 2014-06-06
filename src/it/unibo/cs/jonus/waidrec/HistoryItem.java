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
public class HistoryItem extends VehicleInstance implements Parcelable {

	public HistoryItem() {
		this.setTimestamp(0);
		this.setCategory("");
		this.setAccelFeatures(new MagnitudeFeatures());
		this.setGyroFeatures(new MagnitudeFeatures());
	}

	public HistoryItem(Parcel in) {
		this.setTimestamp(in.readLong());
		this.setCategory(in.readString());
		this.setAccelFeatures((MagnitudeFeatures) in.readParcelable(MagnitudeFeatures.class
				.getClassLoader()));
		this.setGyroFeatures((MagnitudeFeatures) in.readParcelable(MagnitudeFeatures.class
				.getClassLoader()));
	}

	@Override
	public int describeContents() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeLong(this.getTimestamp());
		dest.writeString(this.getCategory());
		dest.writeParcelable(this.getAccelFeatures(), 0);
		dest.writeParcelable(this.getGyroFeatures(), 0);
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

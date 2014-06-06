/**
 * 
 */
package it.unibo.cs.jonus.waidrec;

import java.util.ArrayList;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.os.SystemClock;

/**
 * @author jei
 * 
 */
public class MagnitudeListener implements SensorEventListener {

	public static final int READING_DELAY_NORMAL = 100;

	private int readingDelay = Integer.MAX_VALUE;
	private long lastReading = Long.MAX_VALUE;

	public ArrayList<Double> magnitudeArray = new ArrayList<Double>();

	public MagnitudeListener(int delay) {
		readingDelay = delay;
	}

	/**
	 * Tells the listener to start generating magnitudes
	 */
	public void startGenerating() {
		lastReading = SystemClock.elapsedRealtime();
	}

	/**
	 * Tells the listener to stop generating magnitudes
	 */
	public void stopGenerating() {
		lastReading = Long.MAX_VALUE;
	}

	/**
	 * Resets the arrays of magnitudes
	 */
	public void clearMagnitudes() {
		magnitudeArray.clear();
	}

	/**
	 * Generate a new set of features from the generated magnitudes
	 * 
	 * @return a MagnitudeFeatures object, or null if there were no sensor
	 *         readings
	 */
	public MagnitudeFeatures getFeatures() {
		MagnitudeFeatures mf = new MagnitudeFeatures();
		// check array size
		if (magnitudeArray.size() > 0) {
			// generate the set of features from the array of magnitudes
			double sum = 0;
			double max = Double.MIN_VALUE;
			double min = Double.MAX_VALUE;
			for (int i = 0; i < magnitudeArray.size(); i++) {
				sum += magnitudeArray.get(i);
				if (magnitudeArray.get(i) > max)
					max = magnitudeArray.get(i);
				if (magnitudeArray.get(i) < min)
					min = magnitudeArray.get(i);
			}
			double avg = sum / magnitudeArray.size();
			double std = Math.sqrt(sum - (avg * avg));

			mf.setMaximum(max);
			mf.setMinimum(min);
			mf.setAverage(avg);
			mf.setStandardDeviation(std);

		}

		return mf;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * android.hardware.SensorEventListener#onAccuracyChanged(android.hardware
	 * .Sensor, int)
	 */
	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * android.hardware.SensorEventListener#onSensorChanged(android.hardware
	 * .SensorEvent)
	 */
	@Override
	public void onSensorChanged(SensorEvent event) {
		if (SystemClock.elapsedRealtime() > lastReading + readingDelay) {
			// generate the new magnitude
			lastReading = SystemClock.elapsedRealtime();
			double magnitude = Math.sqrt(event.values[0] * event.values[0]
					+ event.values[1] * event.values[1] + event.values[2]
					* event.values[2]);
			magnitudeArray.add(magnitude);
		}
	}

}

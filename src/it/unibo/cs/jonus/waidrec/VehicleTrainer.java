/**
 * 
 */
package it.unibo.cs.jonus.waidrec;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Handler;

/**
 * @author jei
 * 
 */
public class VehicleTrainer extends VehicleManager {

	private String mTrainingVehicle = null;
	private ArrayList<VehicleObserver> mObservers = new ArrayList<VehicleObserver>();
	private Handler mTrainingHandler = new Handler();

	private MagnitudeListener accelListener = new MagnitudeListener(
			MagnitudeListener.READING_DELAY_NORMAL);
	private MagnitudeListener gyroListener = new MagnitudeListener(
			MagnitudeListener.READING_DELAY_NORMAL);

	// Runnable for vehicle classification
	private Runnable classificationRunnable = new Runnable() {
		@Override
		public void run() {
			// Generate the sets of features
			MagnitudeFeatures accelFeatures = accelListener.getFeatures();
			MagnitudeFeatures gyroFeatures = gyroListener.getFeatures();

			// Create the instance and classify it
			VehicleInstance instance = new VehicleInstance();
			instance.setTimestamp(System.currentTimeMillis());
			instance.setAccelFeatures(accelFeatures);
			instance.setGyroFeatures(gyroFeatures);
			instance.setCategory(mTrainingVehicle);

			// Send the new instance to the observers
			for (VehicleObserver o : mObservers) {
				o.onNewInstance(instance);
			}

			// clear readings arrays
			gyroListener.clearMagnitudes();
			accelListener.clearMagnitudes();

			// Reset delayed runnable
			mTrainingHandler.postDelayed(classificationRunnable,
					samplingDelay);
		}
	};

	/**
	 * @param context
	 * @param modelManager
	 * @param delay
	 * @param vehicle
	 */
	public VehicleTrainer(Context context, ModelManager modelManager,
			long delay, String vehicle) {
		super(context, modelManager, delay);

		this.mTrainingVehicle = vehicle;
	}

	/**
	 * @param context
	 * @param modelManager
	 * @param vehicle
	 */
	public VehicleTrainer(Context context, ModelManager modelManager,
			String vehicle) {
		super(context, modelManager);

		this.mTrainingVehicle = vehicle;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * it.unibo.cs.jonus.waidrec.VehicleManager#registerVehicleObserver(it.unibo
	 * .cs.jonus.waidrec.VehicleObserver)
	 */
	@Override
	public boolean registerVehicleObserver(VehicleObserver observer) {
		boolean result = false;

		if (!mObservers.contains(observer)) {
			int initialSize = mObservers.size();
			mObservers.add(observer);

			if (initialSize == 0) {
				startReading();
			}

			result = true;
		}

		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * it.unibo.cs.jonus.waidrec.VehicleManager#unregisterVehicleObserver(it
	 * .unibo.cs.jonus.waidrec.VehicleObserver)
	 */
	@Override
	public boolean unregisterVehicleObserver(VehicleObserver observer) {
		boolean result = false;

		if (!mObservers.contains(observer)) {
			mObservers.remove(observer);
			int currentSize = mObservers.size();

			if (currentSize == 0) {
				stopReading();
			}

			result = true;
		}

		return result;
	}

	/**
	 * Set the vehicle used to generate the vehicle instances
	 * 
	 * @param vehicle
	 *            the vehicle name
	 */
	public void setTrainingVehicle(String vehicle) {
		this.mTrainingVehicle = vehicle;
	}

	/**
	 * Return the vehicle used to generate the vehicle instances
	 * 
	 * @return the vehicle name
	 */
	public String getTrainingVehicle() {
		return mTrainingVehicle;
	}

	private void startReading() {
		// Register the sensors and start the MagnitudeListeners
		if (mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null) {
			List<Sensor> ls = mSensorManager
					.getSensorList(Sensor.TYPE_ACCELEROMETER);
			for (int i = 0; i < ls.size(); i++) {
				Sensor s_i = ls.get(i);
				mSensorManager.registerListener(accelListener, s_i,
						SensorManager.SENSOR_DELAY_NORMAL);
			}
		}
		accelListener.startGenerating();
		if (mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE) != null) {
			List<Sensor> ls = mSensorManager
					.getSensorList(Sensor.TYPE_GYROSCOPE);
			for (int i = 0; i < ls.size(); i++) {
				Sensor s_i = ls.get(i);
				mSensorManager.registerListener(gyroListener, s_i,
						SensorManager.SENSOR_DELAY_NORMAL);
			}
		}
		gyroListener.startGenerating();

		// Launch the delayed classification task
		mTrainingHandler.removeCallbacks(classificationRunnable);
		mTrainingHandler
				.postDelayed(classificationRunnable, samplingDelay);
	}

	private void stopReading() {
		// Unregister the sensors and stop the MagnitudeListeners
		mSensorManager.unregisterListener(accelListener);
		mSensorManager.unregisterListener(gyroListener);
		accelListener.stopGenerating();
		gyroListener.stopGenerating();
		// Remove the classification task
		mTrainingHandler.removeCallbacks(classificationRunnable);
	}

}

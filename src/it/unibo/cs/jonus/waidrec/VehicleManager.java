/**
 * 
 */
package it.unibo.cs.jonus.waidrec;

import android.content.Context;
import android.hardware.SensorManager;

/**
 * @author jei
 * 
 */
public abstract class VehicleManager {

	public static final int DEFAULT_SAMPLING_DELAY = 5000;

	private Context mContext = null;
	protected ModelManager mModelManager = null;
	protected SensorManager mSensorManager = null;
	protected long samplingDelay = DEFAULT_SAMPLING_DELAY;

	/**
	 * Constructor method for VehicleManager
	 * 
	 * @param context
	 *            the context to use for this VehicleManager
	 */
	public VehicleManager(Context context, ModelManager modelManager, long delay) {
		mContext = context;
		mModelManager = modelManager;
		mSensorManager = (SensorManager) mContext
				.getSystemService(Context.SENSOR_SERVICE);
		samplingDelay = delay;
	}
	
	/**
	 * Constructor method for VehicleManager
	 * 
	 * @param context
	 *            the context to use for this VehicleManager
	 */
	public VehicleManager(Context context, ModelManager modelManager) {
		this(context, modelManager, DEFAULT_SAMPLING_DELAY);
	}

	/**
	 * Set the classification delay
	 * 
	 * @param milliseconds
	 *            the sampling delay in milliseconds
	 */
	public void setSamplingDelay(long milliseconds) {
		samplingDelay = milliseconds;
	}

	/**
	 * Get the classification delay
	 * 
	 * @return the sampling delay in milliseconds
	 */
	public long getSamplingDelay() {
		return samplingDelay;
	}
	
	/**
	 * Set the model manager of this VehicleManager
	 * 
	 * @param modelManager the ModelManager for this VehicleManager
	 */
	public void setModelManager(ModelManager modelManager) {
		this.mModelManager = modelManager;
	}
	
	/**
	 * Get the model manager used by this VehicleManager
	 * @return the ModelManager
	 */
	public ModelManager getModelManager() {
		return mModelManager;
	}

	/**
	 * Register a vehicle observer for this vehicle manager and start generating
	 * vehicle instances
	 * 
	 * @param observer
	 *            the VehicleObserver to use for callbacks
	 */
	public abstract boolean registerVehicleObserver(VehicleObserver observer);

	/**
	 * Unregister the vehicle observer for this vehicle manager and stop
	 * generating vehicle instances
	 * 
	 */
	public abstract boolean unregisterVehicleObserver(VehicleObserver observer);

}

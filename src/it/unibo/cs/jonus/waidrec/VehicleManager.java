/**
 * 
 */
package it.unibo.cs.jonus.waidrec;

import java.util.List;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Handler;
import weka.classifiers.trees.RandomForest;
import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;

/**
 * @author jei
 * 
 */
public class VehicleManager {

	private static final int DEFAULT_SAMPLING_DELAY = 5000;

	private Context mContext = null;
	private VehicleObserver mObserver = null;
	private ModelManager mModelManager = null;
	private SensorManager mSensorManager = null;
	private long samplingDelay = DEFAULT_SAMPLING_DELAY;
	private String trainingVehicle = null;
	private Handler classificationHandler;

	// Weka variables
	private Instances classificationSet;
	private FastVector fvWekaAttributes;
	private RandomForest mClassifier;

	private MagnitudeListener accelListener = new MagnitudeListener(
			MagnitudeListener.READING_DELAY_NORMAL);
	private MagnitudeListener gyroListener = new MagnitudeListener(
			MagnitudeListener.READING_DELAY_NORMAL);

	// Runnable for model deserialization
	private Runnable deserializationRunnable = new Runnable() {

		public void run() {
			try {
				mClassifier = mModelManager.getRandomForestClassifier();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	};

	// Runnable for vehicle classification
	private Runnable classificationRunnable = new Runnable() {
		@Override
		public void run() {
			// Generate the sets of features
			MagnitudeFeatures accelFeatures = accelListener.getFeatures();
			MagnitudeFeatures gyroFeatures = gyroListener.getFeatures();

			// Create the instance
			Instance inst = new Instance(9);
			for (int i = 0; i < 9; i++) {
				inst.setMissing((Attribute) fvWekaAttributes.elementAt(i));
			}

			if (accelFeatures != null) {
				inst.setValue((Attribute) fvWekaAttributes.elementAt(0),
						accelFeatures.getAverage());
				inst.setValue((Attribute) fvWekaAttributes.elementAt(2),
						accelFeatures.getMaximum());
				inst.setValue((Attribute) fvWekaAttributes.elementAt(4),
						accelFeatures.getMinimum());
				inst.setValue((Attribute) fvWekaAttributes.elementAt(6),
						accelFeatures.getStandardDeviation());
			}
			if (gyroFeatures != null) {
				inst.setValue((Attribute) fvWekaAttributes.elementAt(1),
						gyroFeatures.getAverage());
				inst.setValue((Attribute) fvWekaAttributes.elementAt(3),
						gyroFeatures.getMaximum());
				inst.setValue((Attribute) fvWekaAttributes.elementAt(5),
						gyroFeatures.getMinimum());
				inst.setValue((Attribute) fvWekaAttributes.elementAt(7),
						gyroFeatures.getStandardDeviation());
			}

			// add the instance
			classificationSet.add(inst);

			// do classification or specify the vehicle
			if (trainingVehicle == null) {
				double clsLabel = 0;
				try {
					if (mClassifier != null) {
						clsLabel = mClassifier
								.classifyInstance(classificationSet.instance(0));
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
				classificationSet.instance(0).setClassValue(clsLabel);
			} else {
				classificationSet.instance(0).setValue(
						(Attribute) fvWekaAttributes.elementAt(8),
						trainingVehicle);
			}

			// Send the new instance to the observer
			Instance instance = classificationSet.firstInstance();
			VehicleInstance newInstance = new VehicleInstance();
			newInstance.setTimestamp(System.currentTimeMillis());
			newInstance.setCategory(instance.stringValue(8));
			newInstance.setAccelFeatures(accelFeatures);
			newInstance.setGyroFeatures(gyroFeatures);
			mObserver.onNewInstance(newInstance);

			// clear readings arrays
			gyroListener.clearMagnitudes();
			accelListener.clearMagnitudes();

			// erase testing set
			classificationSet.delete();

			// Reset delayed runnable
			classificationHandler.postDelayed(classificationRunnable,
					samplingDelay);
		}
	};

	/**
	 * Constructor method for VehicleManager
	 * 
	 * @param context
	 *            the context to use for this VehicleManager
	 * @param vehicle
	 *            the vehicle used to generate the vehicle instances
	 */
	public VehicleManager(Context context, long delay, String vehicle) {
		this(context, delay);
		trainingVehicle = vehicle;
	}

	/**
	 * Constructor method for VehicleManager
	 * 
	 * @param context
	 *            the context to use for this VehicleManager
	 */
	public VehicleManager(Context context, long delay) {
		mContext = context;
		mModelManager = new ModelManager();
		mSensorManager = (SensorManager) mContext
				.getSystemService(Context.SENSOR_SERVICE);
		samplingDelay = delay;
		classificationHandler = new Handler();

		// Declare the numeric attributes
		Attribute Attribute1 = new Attribute("avga");
		Attribute Attribute2 = new Attribute("avgg");
		Attribute Attribute3 = new Attribute("maxa");
		Attribute Attribute4 = new Attribute("maxg");
		Attribute Attribute5 = new Attribute("mina");
		Attribute Attribute6 = new Attribute("ming");
		Attribute Attribute7 = new Attribute("stda");
		Attribute Attribute8 = new Attribute("stdg");

		// Declare the class attribute along with its values
		// TODO get the classes from ModelManager
		FastVector fvClassVal = new FastVector(4);
		fvClassVal.addElement("walking");
		fvClassVal.addElement("car");
		fvClassVal.addElement("train");
		fvClassVal.addElement("idle");
		Attribute ClassAttribute = new Attribute("theClass", fvClassVal);

		// Declare the feature vector
		fvWekaAttributes = new FastVector(9);
		fvWekaAttributes.addElement(Attribute1);
		fvWekaAttributes.addElement(Attribute2);
		fvWekaAttributes.addElement(Attribute3);
		fvWekaAttributes.addElement(Attribute4);
		fvWekaAttributes.addElement(Attribute5);
		fvWekaAttributes.addElement(Attribute6);
		fvWekaAttributes.addElement(Attribute7);
		fvWekaAttributes.addElement(Attribute8);
		fvWekaAttributes.addElement(ClassAttribute);

		// create testing set
		classificationSet = new Instances("Rel", fvWekaAttributes, 1);
		classificationSet.setClassIndex(8);

		// Model deserialization
		// WARNING! Custom stack size may not be considered depending on the
		// platform
		Thread deserializationThread = new Thread(null,
				deserializationRunnable, "modelOpener", 204800);
		deserializationThread.start();
	}

	/**
	 * Set the vehicle used to generate the vehicle instances
	 * 
	 * @param vehicle
	 *            the vehicle name
	 */
	public void setTrainingVehicle(String vehicle) {
		trainingVehicle = vehicle;
	}

	/**
	 * Return the vehicle used to generate the vehicle instances
	 * 
	 * @return the vehicle name
	 */
	public String getTrainingVehicle() {
		return trainingVehicle;
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
	 * Register a vehicle observer for this vehicle manager and start generating
	 * vehicle instances
	 * 
	 * @param observer
	 *            the VehicleObserver to use for callbacks
	 */
	public void registerVehicleObserver(VehicleObserver observer) {
		mObserver = observer;
		startReading();
	}

	/**
	 * Unregister the vehicle observer for this vehicle manager and stop
	 * generating vehicle instances
	 * 
	 */
	public void unregisterVehicleObserver() {
		mObserver = null;
		stopReading();
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
		classificationHandler.removeCallbacks(classificationRunnable);
		classificationHandler
				.postDelayed(classificationRunnable, samplingDelay);
	}

	private void stopReading() {
		// Unregister the sensors and stop the MagnitudeListeners
		mSensorManager.unregisterListener(accelListener);
		mSensorManager.unregisterListener(gyroListener);
		accelListener.stopGenerating();
		gyroListener.stopGenerating();
		// Remove the classification task
		classificationHandler.removeCallbacks(classificationRunnable);
	}

}

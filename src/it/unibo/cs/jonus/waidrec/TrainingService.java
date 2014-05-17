package it.unibo.cs.jonus.waidrec;

import java.io.IOException;
import java.util.List;

import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import android.app.AlertDialog;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.preference.PreferenceManager;
import android.util.Log;

public class TrainingService extends Service {

	private SharedPreferences sharedPrefs;

	private Context context;

	private final IBinder mBinder = new MyBinder();

	private ModelManager modelManager;

	private Instances trainingSet;
	private FastVector fvWekaAttributes;

	private SensorManager sm = null;

	private int sensorStartDelay = 10;
	private int samplingRate;
	private String vehicleClass;
	private boolean append;

	private MagnitudeListener accelListener = new MagnitudeListener(
			MagnitudeListener.READING_DELAY_NORMAL);
	private MagnitudeListener gyroListener = new MagnitudeListener(
			MagnitudeListener.READING_DELAY_NORMAL);

	private PowerManager powerManager;
	private WakeLock wakeLock;
	private static final int SCREEN_OFF_RECEIVER_DELAY = 500;

	private Handler trainingHandler;
	private Handler startHandler;

	// Runnable for training
	private Runnable trainingRunnable = new Runnable() {
		@Override
		public void run() {
			// Generate the sets of features
			MagnitudeFeatures accelFeatures = accelListener.getFeatures();
			MagnitudeFeatures gyroFeatures = gyroListener.getFeatures();

			// Create the instance
			Instance inst = new Instance(9);
			for (int i = 0; i < 8; i++) {
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

			inst.setValue((Attribute) fvWekaAttributes.elementAt(8),
					vehicleClass);

			// add the instance
			trainingSet.add(inst);

			// Write instance to temp file
			try {
				modelManager.writeInstance(trainingSet.firstInstance(),
						getFilesDir());
			} catch (IOException e) {
				new AlertDialog.Builder(context).setTitle("Error")
						.setMessage("Error while writing to temp Arff file")
						.show();
				stopSelf();
			}

			// clear readings arrays
			gyroListener.clearMagnitudes();
			accelListener.clearMagnitudes();

			// erase testing set
			trainingSet.delete();

			// Reset delayed runnable
			trainingHandler.postDelayed(trainingRunnable, samplingRate);
		}
	};

	// Broadcast receiver used to listen for "screen off" event
	public BroadcastReceiver screenOffReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {

			if (!intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
				return;
			}

			Runnable runnable = new Runnable() {
				public void run() {
					unregisterSensors();
					registerSensors();
				}
			};

			new Handler().postDelayed(runnable, SCREEN_OFF_RECEIVER_DELAY);
		}
	};

	// UncaughtException handler. Useful for sudden Service crashes.
	private Thread.UncaughtExceptionHandler androidDefaultUEH;

	private Thread.UncaughtExceptionHandler UEHandler = new Thread.UncaughtExceptionHandler() {

		@Override
		public void uncaughtException(Thread thread, Throwable ex) {
			// Write current service status to shared preferences
			if (sharedPrefs != null) {
				sharedPrefs.edit().putBoolean("training_isrunning", false)
						.commit();
			}
			
			androidDefaultUEH.uncaughtException(thread, ex);
		}

	};

	// TODO move start delay handling to TrainingActivity
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Bundle bundle = intent.getExtras();
		if (bundle == null) {
			this.stopSelf();
		}
		vehicleClass = bundle.getString("class");
		samplingRate = bundle.getInt("frequency");
		append = bundle.getBoolean("append");

		// Reset temp file according to write mode
		try {
			modelManager.resetTempFile(getFilesDir(), append);
		} catch (IOException e) {
			Log.v("TrainingService", "Error while resetting temp file");
			e.printStackTrace();
			stopSelf();
		}

		// start sensor reading after several seconds
		startHandler.postDelayed(new Runnable() {
			@Override
			public void run() {
				// launch the execution of the delayed classification task
				trainingHandler.postDelayed(trainingRunnable, samplingRate);
				accelListener.startGenerating();
				gyroListener.startGenerating();
			}
		}, sensorStartDelay * 1000);

		return START_STICKY;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		
		// Get the UncaughtException handlers
		androidDefaultUEH = Thread.getDefaultUncaughtExceptionHandler();
		Thread.setDefaultUncaughtExceptionHandler(UEHandler);
		
		// Get shared preferences
		sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);

		// Write current service status to shared preferences
		sharedPrefs.edit().putBoolean("training_isrunning", true).commit();

		modelManager = new ModelManager();

		context = this;

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
		trainingSet = new Instances("Rel", fvWekaAttributes, 1);
		trainingSet.setClassIndex(8);

		// Get power manager and partial wake lock
		powerManager = (PowerManager) getSystemService(POWER_SERVICE);
		wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
				"WaidProviderWakeLock");

		// Register our receiver for the ACTION_SCREEN_OFF action. This will
		// make our receiver
		// code be called whenever the phone enters standby mode.
		IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_OFF);
		registerReceiver(screenOffReceiver, filter);

		// Acquire partial wake lock
		wakeLock.acquire();

		// Register sensor listeners
		registerSensors();

		trainingHandler = new Handler();
		startHandler = new Handler();

	}

	@Override
	public void onDestroy() {
		// Write current service status to shared preferences
		sharedPrefs.edit().putBoolean("training_isrunning", false).commit();

		// Unregister screen off event listener
		unregisterReceiver(screenOffReceiver);

		unregisterSensors();

		// Stop classification Handler
		trainingHandler.removeCallbacks(trainingRunnable);

		// Release partial wake lock
		if (wakeLock.isHeld()) {
			wakeLock.release();
		}
	}

	@Override
	public IBinder onBind(Intent intent) {

		return mBinder;
	}

	public class MyBinder extends Binder {
		TrainingService getService() {
			return TrainingService.this;
		}
	}

	private void registerSensors() {
		sm = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		if (sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null) {
			List<Sensor> ls = sm.getSensorList(Sensor.TYPE_ACCELEROMETER);
			for (int i = 0; i < ls.size(); i++) {
				Sensor s_i = ls.get(i);
				sm.registerListener(accelListener, s_i,
						SensorManager.SENSOR_DELAY_NORMAL);
			}
		}
		if (sm.getDefaultSensor(Sensor.TYPE_GYROSCOPE) != null) {
			List<Sensor> ls = sm.getSensorList(Sensor.TYPE_GYROSCOPE);
			for (int i = 0; i < ls.size(); i++) {
				Sensor s_i = ls.get(i);
				sm.registerListener(gyroListener, s_i,
						SensorManager.SENSOR_DELAY_NORMAL);
			}
		}
	}

	private void unregisterSensors() {
		sm.unregisterListener(accelListener);
		sm.unregisterListener(gyroListener);
		accelListener.stopGenerating();
		gyroListener.stopGenerating();
	}

}

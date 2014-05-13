package it.unibo.cs.jonus.waidrec;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import weka.classifiers.trees.RandomForest;
import android.app.AlertDialog;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Toast;

public class RecognizerService extends Service {
	
	// Static variable to check if the service is running
	public static boolean isRunning = false;
	
	// Evaluation insertion test objects
	TimerTask testEvaluationTask;
	Timer testEvaluationTimer;

	// sampling rate is set to max until the service is bound to the activity
	private int samplingRate = Integer.MAX_VALUE;
	private Boolean firstStart = true;

	private ModelManager modelManager;

	private final IBinder mBinder = new MyBinder();

	private PowerManager powerManager;
	private WakeLock wakeLock;
	private static final int SCREEN_OFF_RECEIVER_DELAY = 500;

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

	long lastWindowExpire;

	private Instances isTestingSet;
	private FastVector fvWekaAttributes;
	private Context context;

	// Runnable for model deserialization
	private Runnable deserializationRunnable = new Runnable() {

		public void run() {
			try {
				classifier = (RandomForest) weka.core.SerializationHelper
						.read(context.openFileInput("randomforest.model"));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	};

	private SensorManager sm;

	RandomForest classifier;

	private MagnitudeListener accelListener = new MagnitudeListener(
			MagnitudeListener.READING_DELAY_NORMAL);
	private MagnitudeListener gyroListener = new MagnitudeListener(
			MagnitudeListener.READING_DELAY_NORMAL);

	private Handler classificationHandler;

	private HistoryManager historyManager;

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
			isTestingSet.add(inst);

			// do classification
			double clsLabel = 0;
			try {
				clsLabel = classifier
						.classifyInstance(isTestingSet.instance(0));
			} catch (Exception e) {
				e.printStackTrace();
			}
			isTestingSet.instance(0).setClassValue(clsLabel);

			// clear readings arrays
			gyroListener.clearMagnitudes();
			accelListener.clearMagnitudes();

			String classification = isTestingSet.instance(0).stringValue(8);

			Long currentTime = System.currentTimeMillis();

			// send evaluation to content provider
			sendEvaluation(classification, currentTime);

			// Write evaluation to history
			HistoryItem newItem = new HistoryItem();
			newItem.setTimestamp(currentTime);
			newItem.setCategory(classification);
			newItem.setAccelFeatures(accelFeatures);
			newItem.setGyroFeatures(gyroFeatures);
			historyManager.writeHistoryItem(newItem);

			// Write instance to temp file
			try {
				modelManager.writeInstance(isTestingSet.firstInstance(),
						getFilesDir());
			} catch (IOException e) {
				new AlertDialog.Builder(context).setTitle("Error")
						.setMessage("Error while writing to temp Arff file")
						.show();
				stopSelf();
			}

			// erase testing set
			isTestingSet.delete();

			// Reset delayed runnable
			classificationHandler.postDelayed(classificationRunnable,
					samplingRate * 1000);
		}
	};

	@Override
	public void onCreate() {
		isRunning = true;
		
		modelManager = new ModelManager();

		historyManager = new HistoryManager(getFilesDir());

		classifier = null;

		// Get power manager and partial wake lock
		powerManager = (PowerManager) getSystemService(POWER_SERVICE);
		wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
				"WaidProviderWakeLock");

		// Register our receiver for the ACTION_SCREEN_OFF action. This will
		// make our receiver
		// code be called whenever the phone enters standby mode.
		IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_OFF);
		registerReceiver(screenOffReceiver, filter);

		Log.v("ProviderService", "start");

		Toast.makeText(this, R.string.recognizer_service_loading,
				Toast.LENGTH_SHORT).show();

		// Get context
		context = getApplicationContext();

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

		// WARNING! Custom stack size may not be considered depending on the
		// platform
		Thread thread = new Thread(null, deserializationRunnable,
				"modelOpener", 204800);
		Log.v("ProviderService", "launching thread");
		thread.start();
		try {
			thread.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		// create testing set
		isTestingSet = new Instances("Rel", fvWekaAttributes, 1);
		isTestingSet.setClassIndex(8);

		lastWindowExpire = SystemClock.elapsedRealtime();

		// start sensor reading
		registerSensors();

		// Acquire partial wake lock
		wakeLock.acquire();

	}

	@Override
	public void onDestroy() {
		// Write the history to file
		try {
			historyManager.writeSession();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// Tell the user we stopped.
		Toast.makeText(this, R.string.recognizer_service_stopped,
				Toast.LENGTH_SHORT).show();
		Log.v("ProviderService", "stop");

		// Unregister screen off event listener
		unregisterReceiver(screenOffReceiver);

		// Unregister sensor listeners
		unregisterSensors();

		// Stop classification Handler
		classificationHandler.removeCallbacks(classificationRunnable);

		// Evaluation insertion test cancel
		// testEvaluationTimer.cancel();

		// Release partial wake lock
		if (wakeLock.isHeld()) {
			wakeLock.release();
		}
		
		isRunning = false;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		// Set sampling rate
		Bundle bundle = intent.getExtras();
		if (bundle == null) {
			this.stopSelf();
		}
		if (firstStart) {
			// start history session
			try {
				historyManager.newSession();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			// launch the execution of the delayed classification task
			samplingRate = bundle.getInt("sampling");
			classificationHandler = new Handler();
			classificationHandler.postDelayed(classificationRunnable,
					samplingRate * 1000);
			firstStart = false;
		}

		Toast.makeText(this, R.string.recognizer_service_loaded,
				Toast.LENGTH_SHORT).show();

		// Reset temp file
		try {
			modelManager.resetTempFile(getFilesDir(), false);
		} catch (IOException e) {
			Log.v("TrainingService", "Error while resetting temp file");
			e.printStackTrace();
			stopSelf();
		}

		return START_STICKY;
	}

	@Override
	public IBinder onBind(Intent intent) {

		return mBinder;
	}

	public class MyBinder extends Binder {
		RecognizerService getService() {
			return RecognizerService.this;
		}
	}

	private void sendEvaluation(String classification, long timestamp) {
		ContentValues values = new ContentValues();
		values.put(DatabaseOpenHelper.COLUMN_TIMESTAMP, timestamp);
		values.put(DatabaseOpenHelper.COLUMN_CATEGORY, classification);

		getContentResolver().insert(EvaluationsProvider.CONTENT_URI, values);
		Log.v("ProviderService", "evaluation sent");
	}

	// Evaluation insertion test method
	@SuppressWarnings("unused")
	private void testEvaluation() {
		// Retrieve random element from a list of the classes
		Random randomGenerator = new Random();
		ArrayList<String> classList = new ArrayList<String>();
		classList.add("walking");
		classList.add("car");
		classList.add("train");
		classList.add("idle");
		int randomIndex = randomGenerator.nextInt(classList.size());
		String classToSend = classList.get(randomIndex);

		ContentValues values = new ContentValues();
		values.put(DatabaseOpenHelper.COLUMN_TIMESTAMP,
				System.currentTimeMillis());

		values.put(DatabaseOpenHelper.COLUMN_CATEGORY, classToSend);

		Uri evaluationUri = getContentResolver().insert(
				EvaluationsProvider.CONTENT_URI, values);

		Log.v("MainActivity", "new id:" + evaluationUri.getLastPathSegment());

		if (evaluationUri.getLastPathSegment() != "0") {
			Log.v("MainActivity", "Insert Evaluation Successful");
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
		accelListener.startGenerating();
		if (sm.getDefaultSensor(Sensor.TYPE_GYROSCOPE) != null) {
			List<Sensor> ls = sm.getSensorList(Sensor.TYPE_GYROSCOPE);
			for (int i = 0; i < ls.size(); i++) {
				Sensor s_i = ls.get(i);
				sm.registerListener(gyroListener, s_i,
						SensorManager.SENSOR_DELAY_NORMAL);
			}
		}
		gyroListener.startGenerating();
	}

	private void unregisterSensors() {
		sm.unregisterListener(accelListener);
		sm.unregisterListener(gyroListener);
		accelListener.stopGenerating();
		gyroListener.stopGenerating();
	}

}

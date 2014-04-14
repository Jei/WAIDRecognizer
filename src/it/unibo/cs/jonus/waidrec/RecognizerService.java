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
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
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

public class RecognizerService extends Service implements SensorEventListener {
	// public class ProviderService extends Service {

	// private SensorEventListener mEventListenerAccelerometer;
	// private SensorEventListener mEventListenerGyroscope;

	// Evaluation insertion test objects
	TimerTask testEvaluationTask;
	Timer testEvaluationTimer;

	private int samplingRate;

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

	ArrayList<Double> arrayAccelerometer;
	ArrayList<Double> arrayGyroscope;

	long lastWindowExpire;
	long lastSensorInput;

	private Instances isTestingSet;
	private FastVector fvWekaAttributes;
	private Context context;
	private Runnable runnable;

	// sensorsimulator things
	// private SensorManagerSimulator sm;

	private SensorManager sm;

	RandomForest classifier;

	@Override
	public void onCreate() {
		modelManager = new ModelManager();

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

		Toast.makeText(this, R.string.recognizer_service_loading, Toast.LENGTH_SHORT)
				.show();

		// Thread for model deserialization
		runnable = new Runnable() {

			public void run() {
				try {
					classifier = (RandomForest) weka.core.SerializationHelper
							.read(context.openFileInput("randomforest.model"));
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		};

		// start the arrays of readings
		arrayAccelerometer = new ArrayList<Double>();
		arrayGyroscope = new ArrayList<Double>();

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
		Thread thread = new Thread(null, runnable, "modelOpener", 204800);
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
		lastSensorInput = SystemClock.elapsedRealtime();

		// start sensor reading
		registerSensors();

		// Acquire partial wake lock
		wakeLock.acquire();

	}

	@Override
	public void onDestroy() {

		// Tell the user we stopped.
		Toast.makeText(this, R.string.recognizer_service_stopped, Toast.LENGTH_SHORT)
				.show();
		Log.v("ProviderService", "stop");

		// Unregister screen off event listener
		unregisterReceiver(screenOffReceiver);

		// Unregister sensor listeners
		unregisterSensors();

		// Evaluation insertion test cancel
		// testEvaluationTimer.cancel();

		// Release partial wake lock
		if (wakeLock.isHeld()) {
			wakeLock.release();
		}
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {

		return START_STICKY;
	}

	@Override
	public IBinder onBind(Intent intent) {
		// Set sampling rate
		Bundle bundle = intent.getExtras();
		if (bundle == null) {
			this.stopSelf();
		}
		samplingRate = bundle.getInt("sampling");

		Toast.makeText(this, R.string.recognizer_service_loaded, Toast.LENGTH_SHORT)
				.show();

		// Reset temp file
		try {
			modelManager.resetTempFile(getFilesDir(), false);
		} catch (IOException e) {
			Log.v("TrainingService", "Error while resetting temp file");
			e.printStackTrace();
			stopSelf();
		}

		return mBinder;
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		// public void evaluateEvent(SensorEvent event) {
		// int type = event.type;
		int type = event.sensor.getType();

		// get readings at 100 ms intervals
		if (SystemClock.elapsedRealtime() > lastSensorInput + 100) {
			lastSensorInput = SystemClock.elapsedRealtime();
			double magnitude = Math.sqrt(event.values[0] * event.values[0]
					+ event.values[1] * event.values[1] + event.values[2]
					* event.values[2]);
			if (type == Sensor.TYPE_GYROSCOPE) {
				arrayGyroscope.add(magnitude);
			} else {
				arrayAccelerometer.add(magnitude);
			}
		}

		// if window has expired, do classification
		if (SystemClock.elapsedRealtime() > lastWindowExpire
				+ (samplingRate * 1000)) { // use samplingRate SECONDS windows
			lastWindowExpire = SystemClock.elapsedRealtime();

			double suma = 0;
			double sumg = 0;
			double maxa = Double.MIN_VALUE;
			double mina = Double.MAX_VALUE;
			double maxg = Double.MIN_VALUE;
			double ming = Double.MAX_VALUE;
			for (int i = 0; i < arrayAccelerometer.size(); i++) {
				suma += arrayAccelerometer.get(i);
				if (arrayAccelerometer.get(i) > maxa)
					maxa = arrayAccelerometer.get(i);
				if (arrayAccelerometer.get(i) < mina)
					mina = arrayAccelerometer.get(i);
			}
			for (int i = 0; i < arrayGyroscope.size(); i++) {
				sumg += arrayGyroscope.get(i);
				if (arrayGyroscope.get(i) > maxg)
					maxg = arrayGyroscope.get(i);
				if (arrayGyroscope.get(i) < ming)
					ming = arrayGyroscope.get(i);
			}
			double avga = suma / arrayAccelerometer.size();
			double avgg = sumg / arrayGyroscope.size();
			double stda = Math.sqrt(suma - (avga * avga));
			double stdg = Math.sqrt(sumg - (avgg * avgg));

			// Toast.makeText(this, "maxa:" + maxa + " mina:" + mina + " maxg:"
			// + maxg + " ming:" + ming + " avga:" + avga + " avgg:" + avgg +
			// " stda:" + stda + " stdg:" + stdg, Toast.LENGTH_SHORT).show();

			// Create the instance
			Instance inst = new Instance(9);

			inst.setValue((Attribute) fvWekaAttributes.elementAt(0), avga);
			inst.setValue((Attribute) fvWekaAttributes.elementAt(1), avgg);
			inst.setValue((Attribute) fvWekaAttributes.elementAt(2), maxa);
			inst.setValue((Attribute) fvWekaAttributes.elementAt(3), maxg);
			inst.setValue((Attribute) fvWekaAttributes.elementAt(4), mina);
			inst.setValue((Attribute) fvWekaAttributes.elementAt(5), ming);
			inst.setValue((Attribute) fvWekaAttributes.elementAt(6), stda);
			inst.setValue((Attribute) fvWekaAttributes.elementAt(7), stdg);
			inst.setMissing((Attribute) fvWekaAttributes.elementAt(8));

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
			arrayGyroscope.clear();
			arrayAccelerometer.clear();

			String classification = isTestingSet.instance(0).stringValue(8);

			// send evaluation to content provider
			sendEvaluation(classification, System.currentTimeMillis());

			// Write instance to temp file
			try {
				modelManager.writeInstance(isTestingSet.firstInstance(),
						getFilesDir());
			} catch (IOException e) {
				new AlertDialog.Builder(this).setTitle("Error")
						.setMessage("Error while writing to temp Arff file")
						.show();
				stopSelf();
			}

			// erase testing set
			isTestingSet.delete();
		}

	}

	@Override
	public void onAccuracyChanged(Sensor arg0, int arg1) {

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

		getContentResolver().insert(
				EvaluationsProvider.CONTENT_URI, values);
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
				sm.registerListener(this, s_i,
						SensorManager.SENSOR_DELAY_NORMAL);
			}
		}
		if (sm.getDefaultSensor(Sensor.TYPE_GYROSCOPE) != null) {
			List<Sensor> ls = sm.getSensorList(Sensor.TYPE_GYROSCOPE);
			for (int i = 0; i < ls.size(); i++) {
				Sensor s_i = ls.get(i);
				sm.registerListener(this, s_i,
						SensorManager.SENSOR_DELAY_NORMAL);
			}
		}
	}

	private void unregisterSensors() {
		sm.unregisterListener(this);
	}

}

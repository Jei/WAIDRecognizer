package it.unibo.cs.jonus.waidrec;

import java.util.ArrayList;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

public class RecognizerService extends Service {

	private Thread.UncaughtExceptionHandler androidDefaultUEH;

	private Thread.UncaughtExceptionHandler ueHandler = new Thread.UncaughtExceptionHandler() {
		public void uncaughtException(Thread thread, Throwable ex) {
			sharedPrefs.edit().putBoolean("recognizer_isrunning", false)
					.commit();

			androidDefaultUEH.uncaughtException(thread, ex);
		}
	};

	private SharedPreferences sharedPrefs;

	// Evaluation insertion test objects
	TimerTask testEvaluationTask;
	Timer testEvaluationTimer;

	private static final String KEY_REC_SAMPLING_DELAY = "pref_rec_sampling_delay";

	private final IBinder mBinder = new MyBinder();

	private PowerManager powerManager;
	private WakeLock wakeLock;
	private static final int SCREEN_OFF_RECEIVER_DELAY = 500;

	private VehicleManager mVehicleManager;
	private ModelManager mModelManager;

	// Broadcast receiver used to listen for "screen off" event
	private BroadcastReceiver screenOffReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {

			if (!intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
				return;
			}

			Runnable runnable = new Runnable() {
				public void run() {
					mVehicleManager.unregisterVehicleObserver(vehicleObserver);
					mVehicleManager.registerVehicleObserver(vehicleObserver);
				}
			};

			new Handler().postDelayed(runnable, SCREEN_OFF_RECEIVER_DELAY);
		}
	};

	private VehicleObserver vehicleObserver = new VehicleObserver() {

		@Override
		public void onNewInstance(VehicleInstance instance) {
			// Send evaluation to content provider
			sendInstance(instance);
		}
	};

	@Override
	public void onCreate() {
		// Set default UE handler
		androidDefaultUEH = Thread.getDefaultUncaughtExceptionHandler();
		Thread.setDefaultUncaughtExceptionHandler(ueHandler);

		// Get shared preferences
		sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);

		// Write current service status to shared preferences
		sharedPrefs.edit().putBoolean("recognizer_isrunning", true).commit();

		// Get the sampling delay from shared preferences
		String sdString = sharedPrefs.getString(KEY_REC_SAMPLING_DELAY, "5");
		int samplingDelay = Integer.parseInt(sdString);

		// Get the model and vehicle managers
		mModelManager = new ModelManager(getApplicationContext());
		mVehicleManager = new VehicleRecognizer(this, mModelManager,
				samplingDelay * 1000);

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

		// Acquire partial wake lock
		wakeLock.acquire();

		// Start reading from the vehicle manager
		mVehicleManager.registerVehicleObserver(vehicleObserver);

	}

	@Override
	public void onDestroy() {
		// Tell the user we stopped.
		Toast.makeText(this, R.string.recognizer_service_stopped,
				Toast.LENGTH_SHORT).show();
		Log.v("ProviderService", "stop");

		// Stop reading from the vehicle manager
		mVehicleManager.unregisterVehicleObserver(vehicleObserver);

		// Unregister screen off event listener
		unregisterReceiver(screenOffReceiver);

		// Evaluation insertion test cancel
		// testEvaluationTimer.cancel();

		// Release partial wake lock
		if (wakeLock.isHeld()) {
			wakeLock.release();
		}

		// Write current service status to shared preferences
		sharedPrefs.edit().putBoolean("recognizer_isrunning", false).commit();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {

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

	private void sendInstance(VehicleInstance instance) {
		ContentValues values = EvaluationsProvider
				.vehicleInstanceToContentValues(instance);

		getContentResolver().insert(EvaluationsProvider.CONTENT_URI, values);
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

}

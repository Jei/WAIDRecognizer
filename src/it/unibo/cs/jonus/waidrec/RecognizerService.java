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

	private SharedPreferences sharedPrefs;

	// Evaluation insertion test objects
	TimerTask testEvaluationTask;
	Timer testEvaluationTimer;

	private final IBinder mBinder = new MyBinder();

	private PowerManager powerManager;
	private WakeLock wakeLock;
	private static final int SCREEN_OFF_RECEIVER_DELAY = 500;

	private VehicleManager vehicleManager;

	// Broadcast receiver used to listen for "screen off" event
	private BroadcastReceiver screenOffReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {

			if (!intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
				return;
			}

			Runnable runnable = new Runnable() {
				public void run() {
					vehicleManager.unregisterVehicleObserver();
					vehicleManager.registerVehicleObserver(vehicleObserver);
				}
			};

			new Handler().postDelayed(runnable, SCREEN_OFF_RECEIVER_DELAY);
		}
	};

	private VehicleObserver vehicleObserver = new VehicleObserver() {

		@Override
		public void onNewInstance(VehicleInstance instance) {
			// Send evaluation to content provider
			sendEvaluation(instance.getCategory(), instance.getTimestamp());

			// Write evaluation to history
			HistoryItem newItem = new HistoryItem();
			newItem.setTimestamp(instance.getTimestamp());
			newItem.setCategory(instance.getCategory());
			newItem.setAccelFeatures(instance.getAccelFeatures());
			newItem.setGyroFeatures(instance.getGyroFeatures());
			historyManager.writeHistoryItem(newItem);
		}
	};

	private HistoryManager historyManager;

	@Override
	public void onCreate() {
		// Get shared preferences
		sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);

		// Write current service status to shared preferences
		sharedPrefs.edit().putBoolean("recognizer_isrunning", true).commit();

		// Get the sampling delay from shared preferences
		String sdString = sharedPrefs.getString(
				RecognizerSettingsActivity.KEY_REC_SAMPLING_DELAY, "5");
		int samplingDelay = Integer.parseInt(sdString);

		// Get the vehicle manager
		vehicleManager = new VehicleManager(this, samplingDelay * 1000);

		historyManager = new HistoryManager(this);

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
		vehicleManager.registerVehicleObserver(vehicleObserver);

	}

	@Override
	public void onDestroy() {
		// Tell the user we stopped.
		Toast.makeText(this, R.string.recognizer_service_stopped,
				Toast.LENGTH_SHORT).show();
		Log.v("ProviderService", "stop");

		// Stop reading from the vehicle manager
		vehicleManager.unregisterVehicleObserver();

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

}

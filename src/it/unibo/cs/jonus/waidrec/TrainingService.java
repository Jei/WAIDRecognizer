package it.unibo.cs.jonus.waidrec;

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

public class TrainingService extends Service {

	private static final String KEY_TRN_SAMPLING_DELAY = "pref_training_sampling_delay";

	private SharedPreferences sharedPrefs;

	private final IBinder mBinder = new MyBinder();

	private ModelManager modelManager;
	private VehicleManager vehicleManager;
	private VehicleObserver vehicleObserver = new VehicleObserver() {

		@Override
		public void onNewInstance(VehicleInstance instance) {
			Uri uri = Uri.parse(EvaluationsProvider.TRAINING_DATA_URI
					+ EvaluationsProvider.PATH_INSERT_TRAINING_ITEM);

			ContentValues values = EvaluationsProvider
					.vehicleInstanceToContentValues(instance);

			getContentResolver().insert(uri, values);
		}
	};

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
					vehicleManager.unregisterVehicleObserver(vehicleObserver);
					vehicleManager.registerVehicleObserver(vehicleObserver);
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

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {

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
		String vehicle = sharedPrefs.getString("training_current_vehicle",
				"idle");
		String sd = sharedPrefs.getString(KEY_TRN_SAMPLING_DELAY, "5");
		int samplingDelay = Integer.parseInt(sd);

		modelManager = new ModelManager(this);
		vehicleManager = new VehicleTrainer(this, modelManager,
				samplingDelay * 1000, vehicle);

		// Write current service status to shared preferences
		sharedPrefs.edit().putBoolean("training_isrunning", true).commit();

		vehicleManager.registerVehicleObserver(vehicleObserver);

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

	}

	@Override
	public void onDestroy() {
		// Write current service status to shared preferences
		sharedPrefs.edit().putBoolean("training_isrunning", false).commit();

		// Unregister screen off event listener
		unregisterReceiver(screenOffReceiver);

		// Unregister the vehicle observer
		vehicleManager.unregisterVehicleObserver(vehicleObserver);

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

}

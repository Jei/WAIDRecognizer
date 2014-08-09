package it.unibo.cs.jonus.waidrec;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Locale;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.FragmentTransaction;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.NavUtils;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

public class MainActivity extends FragmentActivity implements
		ActionBar.TabListener {

	/**
	 * The {@link android.support.v4.view.PagerAdapter} that will provide
	 * fragments for each of the sections. We use a
	 * {@link android.support.v4.app.FragmentPagerAdapter} derivative, which
	 * will keep every loaded fragment in memory. If this becomes too memory
	 * intensive, it may be best to switch to a
	 * {@link android.support.v4.app.FragmentStatePagerAdapter}.
	 */
	SectionsPagerAdapter mSectionsPagerAdapter;

	/**
	 * The {@link ViewPager} that will host the section contents.
	 */
	ViewPager mViewPager;

	private SharedPreferences mSharedPrefs;
	private NotificationManager mNotificationManager;

	private static final int MODIFY_PREFERENCES = 1; // The request code
	private static final int ERROR_MODEL_GENERATION_IOEXCEPTION = 10;
	private static final int ERROR_MODEL_GENERATION_EXCEPTION = 11;
	public static final int NOTIFICATION_MODEL_GENERATION_IOEXCEPTION = R.string.model_generation_ioexception;
	public static final int NOTIFICATION_MODEL_GENERATION_EXCEPTION = R.string.model_generation_exception;
	public static final int NOTIFICATION_TRAINING_RUNNING = R.string.training_service_running;
	public static final int NOTIFICATION_GENERATING_MODEL = R.string.model_generating;
	public static final int NOTIFICATION_MODEL_GENERATED = R.string.model_generated;
	public static final int NOTIFICATION_MODEL_GENERATION_ERROR = R.string.model_generation_error;
	public static final int NOTIFICATION_RECOGNIZER_STARTED = R.string.recognizer_started;
	public static final int NOTIFICATION_RECOGNIZER_STOPPED = R.string.recognizer_stopped;
	public static final String[] allColumnsProjection = {
			DatabaseOpenHelper.COLUMN_TIMESTAMP,
			DatabaseOpenHelper.COLUMN_CATEGORY, DatabaseOpenHelper.COLUMN_AVGA,
			DatabaseOpenHelper.COLUMN_MINA, DatabaseOpenHelper.COLUMN_MAXA,
			DatabaseOpenHelper.COLUMN_STDA, DatabaseOpenHelper.COLUMN_AVGG,
			DatabaseOpenHelper.COLUMN_MING, DatabaseOpenHelper.COLUMN_MAXG,
			DatabaseOpenHelper.COLUMN_STDG };
	public static final String[] vehicleColumnsProjection = {
			DatabaseOpenHelper.COLUMN_CATEGORY, DatabaseOpenHelper.COLUMN_ICON };

	// Thread messages handler
	private static class ThreadHandler extends Handler {
		private final WeakReference<MainActivity> mActivity;

		public ThreadHandler(MainActivity context) {
			mActivity = new WeakReference<MainActivity>((MainActivity) context);
		}

		@Override
		public void handleMessage(Message msg) {
			MainActivity activity = mActivity.get();
			int code = msg.arg1;
			ProgressDialog dialog = (ProgressDialog) msg.obj;

			// Dismiss the progress dialog
			if (dialog != null) {
				dialog.dismiss();
			}

			switch (code) {
			case 0:
				Toast.makeText(activity, R.string.model_generated,
						Toast.LENGTH_SHORT).show();
				activity.hideNotification(NOTIFICATION_GENERATING_MODEL);
				activity.showNotification(NOTIFICATION_MODEL_GENERATED, true,
						false);
				break;
			case ERROR_MODEL_GENERATION_IOEXCEPTION:
				new AlertDialog.Builder(activity)
						.setTitle(activity.getText(R.string.error))
						.setMessage(
								activity.getText(NOTIFICATION_MODEL_GENERATION_IOEXCEPTION))
						.show();
				activity.hideNotification(NOTIFICATION_GENERATING_MODEL);
				activity.showNotification(
						NOTIFICATION_MODEL_GENERATION_IOEXCEPTION, true, false);

				break;
			case ERROR_MODEL_GENERATION_EXCEPTION:
				new AlertDialog.Builder(activity)
						.setTitle(activity.getText(R.string.error))
						.setMessage(
								activity.getText(NOTIFICATION_MODEL_GENERATION_EXCEPTION))
						.show();
				activity.hideNotification(NOTIFICATION_GENERATING_MODEL);
				activity.showNotification(
						NOTIFICATION_MODEL_GENERATION_EXCEPTION, true, false);

				break;
			}
		}
	};

	private ThreadHandler mThreadHandler = new ThreadHandler(this);

	// Runnable for model reset thread
	public static class ModelResetRunnable implements Runnable {
		private final WeakReference<MainActivity> mActivity;
		private ProgressDialog mDialog;

		public ModelResetRunnable(MainActivity context, ProgressDialog pd) {
			mActivity = new WeakReference<MainActivity>((MainActivity) context);
			mDialog = pd;
		}

		public void run() {
			MainActivity activity = mActivity.get();
			// Prepare message for thread completion
			Message message = new Message();
			// Reset original arff files
			try {
				// Create new ModelManager
				ModelManager modelManager = new ModelManager(activity);

				// Reset the training data
				activity.resetFromAssets();

				// Get the VehicleInstances from the Content Provider
				Uri uri = Uri.parse(EvaluationsProvider.TRAINING_DATA_URI
						+ EvaluationsProvider.PATH_ALL_TRAINING_DATA);
				Cursor cursor = activity.getContentResolver().query(uri,
						allColumnsProjection, null, null, null);
				ArrayList<VehicleInstance> instances = EvaluationsProvider
						.cursorToVehicleInstanceArray(cursor);

				// Generate the new model from the database data
				modelManager.generateModel(instances);
				Log.v("resetModel", "model generated");
				// Operations completed correctly, return 0
				message.arg1 = 0;
			} catch (IOException e) {
				message.arg1 = ERROR_MODEL_GENERATION_IOEXCEPTION;
				e.printStackTrace();
			} catch (Exception e) {
				message.arg1 = ERROR_MODEL_GENERATION_EXCEPTION;
				e.printStackTrace();
			}
			if (!Thread.interrupted() && activity.mThreadHandler != null) {
				// Notify thread completion
				message.obj = mDialog;
				activity.mThreadHandler.sendMessage(message);
			}
		}
	};

	public static class ModelGenRunnable implements Runnable {
		private final WeakReference<MainActivity> mActivity;
		ProgressDialog dialog;

		public ModelGenRunnable(MainActivity context, ProgressDialog pd) {
			mActivity = new WeakReference<MainActivity>((MainActivity) context);
			dialog = pd;
		}

		public void run() {
			MainActivity activity = mActivity.get();

			// Prepare message for thread completion
			Message message = new Message();
			// Generate new model
			try {
				ModelManager modelManager = new ModelManager(activity);

				// Get the VehicleInstances from the Content Provider
				Uri uri = Uri.parse(EvaluationsProvider.TRAINING_DATA_URI
						+ EvaluationsProvider.PATH_ALL_TRAINING_DATA);
				Cursor cursor = activity.getContentResolver().query(uri,
						allColumnsProjection, null, null, null);
				ArrayList<VehicleInstance> instances = EvaluationsProvider
						.cursorToVehicleInstanceArray(cursor);
				modelManager.generateModel(instances);
				Log.v("resetModel", "model generated");
				// Operations completed correctly, return 0
				message.arg1 = 0;
			} catch (IOException e) {
				message.arg1 = ERROR_MODEL_GENERATION_IOEXCEPTION;
				e.printStackTrace();
			} catch (Exception e) {
				message.arg1 = ERROR_MODEL_GENERATION_EXCEPTION;
				e.printStackTrace();
			}
			if (!Thread.interrupted() && activity.mThreadHandler != null) {
				// Notify thread completion
				message.obj = dialog;
				activity.mThreadHandler.sendMessage(message);
			}

		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		// Set up the action bar.
		final ActionBar actionBar = getActionBar();
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

		// Create the adapter that will return a fragment for each of the three
		// primary sections of the app.
		mSectionsPagerAdapter = new SectionsPagerAdapter(
				getSupportFragmentManager());

		// Set up the ViewPager with the sections adapter.
		mViewPager = (ViewPager) findViewById(R.id.pager);
		mViewPager.setAdapter(mSectionsPagerAdapter);

		// When swiping between different sections, select the corresponding
		// tab. We can also use ActionBar.Tab#select() to do this if we have
		// a reference to the Tab.
		mViewPager
				.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
					@Override
					public void onPageSelected(int position) {
						actionBar.setSelectedNavigationItem(position);
					}
				});

		// For each of the sections in the app, add a tab to the action bar.
		for (int i = 0; i < mSectionsPagerAdapter.getCount(); i++) {
			// Create a tab with text corresponding to the page title defined by
			// the adapter. Also specify this Activity object, which implements
			// the TabListener interface, as the callback (listener) for when
			// this tab is selected.
			actionBar.addTab(actionBar.newTab()
					.setText(mSectionsPagerAdapter.getPageTitle(i))
					.setTabListener(this));
		}

		// Get shared preferences
		mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);

		// Set default preference values
		PreferenceManager.setDefaultValues(this, R.xml.waidrec_settings, false);

		// Get notification manager
		mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

		// Check if the activity is being run for the first time
		if (firstRun()) {
			// Create progress dialog
			final ProgressDialog progressDialog = new ProgressDialog(this);
			progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
			progressDialog.setMessage("Generating new model...");
			progressDialog.setIndeterminate(true);
			progressDialog.setCancelable(false);
			progressDialog.show();

			// Run model reset thread
			ModelResetRunnable runnable = new ModelResetRunnable(this,
					progressDialog);
			Thread asyncThread = new Thread(null, runnable, "ModelReset",
					204800);
			asyncThread.start();
			showNotification(NOTIFICATION_GENERATING_MODEL, false, true);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	// Start activities if selected from the menu
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.action_settings:
			Intent prefsActivity = new Intent(this, MainPrefsActivity.class);
			startActivityForResult(prefsActivity, MODIFY_PREFERENCES);
			break;
		default:
			return false;
		}

		return true;
	}

	@Override
	public void onTabSelected(ActionBar.Tab tab,
			FragmentTransaction fragmentTransaction) {
		// When the given tab is selected, switch to the corresponding page in
		// the ViewPager.
		mViewPager.setCurrentItem(tab.getPosition());
	}

	@Override
	public void onTabUnselected(ActionBar.Tab tab,
			FragmentTransaction fragmentTransaction) {
	}

	@Override
	public void onTabReselected(ActionBar.Tab tab,
			FragmentTransaction fragmentTransaction) {
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// If we called MainPrefsActivity
		if (requestCode == MODIFY_PREFERENCES) {
			// If we have to reset the model
			if (resultCode == MainPrefsActivity.RESULT_RESET) {
				// Create progress dialog
				final ProgressDialog progressDialog = new ProgressDialog(this);
				progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
				progressDialog.setMessage("Generating new model...");
				progressDialog.setIndeterminate(true);
				progressDialog.setCancelable(false);
				progressDialog.show();

				// Run model reset thread
				ModelResetRunnable runnable = new ModelResetRunnable(this,
						progressDialog);
				Thread asyncThread = new Thread(null, runnable, "ModelReset",
						204800);
				asyncThread.start();
				showNotification(NOTIFICATION_GENERATING_MODEL, false, true);
			}
			// If we have to generate the model
			if (resultCode == MainPrefsActivity.RESULT_MODIFIED) {
				// Create progress dialog
				final ProgressDialog progressDialog = new ProgressDialog(this);
				progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
				progressDialog.setMessage(getResources().getString(
						MainActivity.NOTIFICATION_GENERATING_MODEL));
				progressDialog.setIndeterminate(true);
				progressDialog.setCancelable(false);
				progressDialog.show();

				// Run model generation thread
				ModelGenRunnable runnable = new ModelGenRunnable(this,
						progressDialog);
				Thread asyncThread = new Thread(null, runnable, "ModelGen",
						204800);
				asyncThread.start();
				showNotification(MainActivity.NOTIFICATION_GENERATING_MODEL,
						false, true);
			}
		}
	}

	/**
	 * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
	 * one of the sections/tabs/pages.
	 */
	public class SectionsPagerAdapter extends FragmentPagerAdapter {

		public SectionsPagerAdapter(FragmentManager fm) {
			super(fm);
		}

		@Override
		public Fragment getItem(int position) {
			// getItem is called to instantiate the fragment for the given page.
			// Return a DummySectionFragment (defined as a static inner class
			// below) with the page number as its lone argument.
			Fragment fragment = null;
			switch (position) {
			case 0:
				fragment = new RecognizerFragment();
				break;
			case 1:
				fragment = new TrainingFragment();
				break;
			default:
				fragment = new HistoryFragment();
				break;
			}

			return fragment;
		}

		@Override
		public int getCount() {
			// Show 3 total pages.
			return 3;
		}

		@Override
		public CharSequence getPageTitle(int position) {
			Locale l = Locale.getDefault();
			switch (position) {
			case 0:
				return getString(R.string.recognizer).toUpperCase(l);
			case 1:
				return getString(R.string.training).toUpperCase(l);
			case 2:
				return getString(R.string.history).toUpperCase(l);
			}
			return null;
		}
	}

	// TODO create method for model generation

	private void resetFromAssets() {
		AssetManager assets = getAssets();
		File filesDir = getFilesDir();
		FileInputStream walkingAsset;
		FileInputStream carAsset;
		FileInputStream trainAsset;
		FileInputStream idleAsset;
		Bitmap walkingIcon;
		Bitmap carIcon;
		Bitmap trainIcon;
		Bitmap idleIcon;

		try {
			// Get assets
			walkingAsset = assets.openFd("walking.gif").createInputStream();
			carAsset = assets.openFd("car.gif").createInputStream();
			trainAsset = assets.openFd("train.gif").createInputStream();
			idleAsset = assets.openFd("idle.gif").createInputStream();
			walkingIcon = getBitmapFromAsset("walking_00b0b0.png");
			carIcon = getBitmapFromAsset("car_00b0b0.png");
			trainIcon = getBitmapFromAsset("train_00b0b0.png");
			idleIcon = getBitmapFromAsset("idle_00b0b0.png");

			// Copy the arff files to app files directory
			File newWalkingFile = new File(filesDir.getPath() + "/walking.arff");
			File newCarFile = new File(filesDir.getPath() + "/car.arff");
			File newTrainFile = new File(filesDir.getPath() + "/train.arff");
			File newIdleFile = new File(filesDir.getPath() + "/idle.arff");

			FileInputStream[] streams = { walkingAsset, carAsset, trainAsset,
					idleAsset };
			File[] newFiles = { newWalkingFile, newCarFile, newTrainFile,
					newIdleFile };
			for (int i = 0; i < newFiles.length; i++) {
				OutputStream os = null;
				try {
					os = new FileOutputStream(newFiles[i]);
					byte[] buffer = new byte[1024];
					int length;
					while ((length = streams[i].read(buffer)) > 0) {
						os.write(buffer, 0, length);
					}
				} finally {
					streams[i].close();
					os.close();
				}
			}

			// Erase training data from the database
			Uri uri = Uri.parse(EvaluationsProvider.TRAINING_DATA_URI
					+ EvaluationsProvider.PATH_ERASE_TRAINING_DATA);
			getContentResolver().delete(uri, null, null);

			// Delete all the vehicles
			uri = Uri.parse(EvaluationsProvider.VEHICLES_URI
					+ EvaluationsProvider.PATH_ERASE_VEHICLES);
			getContentResolver().delete(uri, null, null);

			// Insert the data from the files into the database
			uri = Uri.parse(EvaluationsProvider.TRAINING_DATA_URI
					+ EvaluationsProvider.PATH_INSERT_TRAINING_ITEM);
			for (File f : newFiles) {
				ArrayList<VehicleInstance> instances = ModelManager
						.readArffFile(f);

				for (VehicleInstance i : instances) {
					ContentValues values = EvaluationsProvider
							.vehicleInstanceToContentValues(i);

					getContentResolver().insert(uri, values);
				}
			}

			// Create the VehicleItem objects and insert them in the db
			String[] vehicles = { "walking", "car", "train", "idle" };
			Bitmap[] icons = { walkingIcon, carIcon, trainIcon, idleIcon };
			uri = Uri.parse(EvaluationsProvider.VEHICLES_URI
					+ EvaluationsProvider.PATH_ADD_VEHICLE);
			for (int i = 0; i < 4; i++) {
				VehicleItem item = new VehicleItem();
				item.setCategory(vehicles[i]);
				item.setIcon(icons[i]);

				ContentValues values = EvaluationsProvider
						.vehicleItemToContentValues(item);

				getContentResolver().insert(uri, values);
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void showNotification(int textId, boolean autoCancel, boolean onGoing) {
		CharSequence text = getText(textId);
		CharSequence title = getText(R.string.recognizer_service_name);

		// Create a pending intent to open the activity
		Intent recognizerIntent = new Intent(this, MainActivity.class);
		recognizerIntent.setAction(Intent.ACTION_MAIN);
		recognizerIntent.addCategory(Intent.CATEGORY_LAUNCHER);
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
				recognizerIntent, 0);

		// Build the notification
		@SuppressWarnings("deprecation")
		Notification notification = new Notification.Builder(this)
				.setSmallIcon(R.drawable.ic_launcher).setContentText(text)
				.setContentTitle(title).setContentIntent(contentIntent)
				.setAutoCancel(autoCancel).setOngoing(onGoing)
				.getNotification();

		// Send the notification
		mNotificationManager.notify(textId, notification);
	}

	public void hideNotification(int textId) {
		mNotificationManager.cancel(textId);
	}

	// First application run test & set
	private boolean firstRun() {
		boolean currentValue;
		currentValue = mSharedPrefs.getBoolean("first_run", true);
		mSharedPrefs.edit().putBoolean("first_run", false).commit();
		return currentValue;
	}

	private Bitmap getBitmapFromAsset(String fileName) throws IOException {
		AssetManager assetManager = getAssets();

		InputStream istr = assetManager.open(fileName);
		Bitmap bitmap = BitmapFactory.decodeStream(istr);

		return bitmap;
	}

}

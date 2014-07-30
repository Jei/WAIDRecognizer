package it.unibo.cs.jonus.waidrec;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;

import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.database.ContentObserver;
import android.database.Cursor;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

public class RecognizerActivity extends Activity {

	private Context context;

	private SharedPreferences sharedPrefs;

	private Boolean generating = false;
	private Thread asyncThread = null;

	private Handler contentProviderHandler = new Handler();
	private ListView historyView;
	private ArrayList<VehicleInstance> evaluationsList;

	private ImageView noneView;
	private ImageView walkingView;
	private ImageView carView;
	private ImageView trainView;
	private ImageView idleView;

	private Button startServiceButton;
	private Button stopServiceButton;

	private String currentClassification;

	private EvaluationsContentObserver evaluationsObserver = null;

	private NotificationManager notificationManager;
	private static final int NOTIFICATION_GENERATING_MODEL = R.string.model_generating;
	private static final int NOTIFICATION_MODEL_GENERATED = R.string.model_generated;
	private static final int NOTIFICATION_MODEL_GENERATION_ERROR = R.string.model_generation_error;
	private static final int NOTIFICATION_RECOGNIZER_STARTED = R.string.recognizer_started;

	private static final String[] allColumnsProjection = {
			DatabaseOpenHelper.COLUMN_TIMESTAMP,
			DatabaseOpenHelper.COLUMN_CATEGORY, DatabaseOpenHelper.COLUMN_AVGA,
			DatabaseOpenHelper.COLUMN_MINA, DatabaseOpenHelper.COLUMN_MAXA,
			DatabaseOpenHelper.COLUMN_STDA, DatabaseOpenHelper.COLUMN_AVGG,
			DatabaseOpenHelper.COLUMN_MING, DatabaseOpenHelper.COLUMN_MAXG,
			DatabaseOpenHelper.COLUMN_STDG };

	@SuppressLint("HandlerLeak")
	private Handler threadHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			int code = msg.arg1;
			ProgressDialog dialog = (ProgressDialog) msg.obj;

			// Dismiss the progress dialog
			if (dialog != null) {
				dialog.dismiss();
			}

			if (code == 0) {
				Toast.makeText(RecognizerActivity.this,
						R.string.model_generated, Toast.LENGTH_SHORT).show();
				hideNotification(NOTIFICATION_GENERATING_MODEL);
				showNotification(NOTIFICATION_MODEL_GENERATED, true, false);

			} else {
				new AlertDialog.Builder(context)
						.setTitle(getText(R.string.error))
						.setMessage(
								getText(NOTIFICATION_MODEL_GENERATION_ERROR))
						.show();
				hideNotification(NOTIFICATION_GENERATING_MODEL);
				showNotification(NOTIFICATION_MODEL_GENERATION_ERROR, true,
						false);

			}

			generating = false;
		}
	};

	// Class used to listen to changes on the provider
	class EvaluationsContentObserver extends ContentObserver {

		public EvaluationsContentObserver(Handler handler) {
			super(handler);
		}

		public void onChange(boolean selfChange) {
			super.onChange(selfChange);

			updateUI();
		}

	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_recognizer);

		// Get shared preferences
		sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);

		context = this;

		// Set default preference values
		PreferenceManager.setDefaultValues(this, R.xml.waidrec_settings, false);

		// Get notification manager
		notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

		// Runnable for model generation thread
		class ModelResetRunnable implements Runnable {
			ProgressDialog dialog;

			public ModelResetRunnable(ProgressDialog pd) {
				dialog = pd;
			}

			public void run() {
				// Prepare message for thread completion
				Message message = new Message();
				// Reset original arff files
				try {
					// Create new ModelManager
					ModelManager modelManager = new ModelManager(
							getApplicationContext());

					// Reset the training data
					resetFromAssets();

					// Get the VehicleInstances from the Content Provider
					Uri uri = Uri.parse(EvaluationsProvider.TRAINING_DATA_URI
							+ EvaluationsProvider.PATH_ALL_TRAINING_DATA);
					Cursor cursor = getContentResolver().query(uri,
							allColumnsProjection, null, null, null);
					ArrayList<VehicleInstance> instances = EvaluationsProvider
							.cursorToVehicleInstanceArray(cursor);

					// Generate the new model from the database data
					modelManager.generateModel(instances);
					Log.v("resetModel", "model generated");
					// Operations completed correctly, return 0
					message.arg1 = 0;
				} catch (IOException e) {
					message.arg1 = 1;
					e.printStackTrace();
				} catch (Exception e) {
					message.arg1 = 1;
					e.printStackTrace();
				}
				if (!Thread.interrupted() && threadHandler != null) {
					// Notify thread completion
					message.obj = dialog;
					threadHandler.sendMessage(message);
				}
			}
		}
		;

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
			ModelResetRunnable runnable = new ModelResetRunnable(progressDialog);
			asyncThread = new Thread(null, runnable, "ModelReset", 204800);
			asyncThread.start();
			generating = true;
			showNotification(NOTIFICATION_GENERATING_MODEL, false, true);
		}

		// Set initial classification
		currentClassification = "none";

		// Set classifications images
		noneView = (ImageView) findViewById(R.id.noneView);
		walkingView = (ImageView) findViewById(R.id.walkingView);
		carView = (ImageView) findViewById(R.id.carView);
		trainView = (ImageView) findViewById(R.id.trainView);
		idleView = (ImageView) findViewById(R.id.idleView);

		// Set none view visible
		noneView.setVisibility(View.VISIBLE);

		// Prepare history view
		Context context = getApplicationContext();
		historyView = (ListView) findViewById(R.id.historyView);
		evaluationsList = new ArrayList<VehicleInstance>();
		historyView
				.setAdapter(new HistoryListAdapter(context, evaluationsList));
		historyView.setEmptyView(findViewById(R.id.emptyView));
		historyView.setTranscriptMode(ListView.TRANSCRIPT_MODE_ALWAYS_SCROLL);

		// Set start/stop service buttons
		startServiceButton = (Button) findViewById(R.id.serviceStart);
		stopServiceButton = (Button) findViewById(R.id.serviceStop);
		startServiceButton.setEnabled(true);
		stopServiceButton.setEnabled(false);

		// Check if RecognizerService is already running
		if (sharedPrefs.getBoolean("recognizer_isrunning", false)) {
			startProvider(startServiceButton);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.recognizer, menu);

		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {

		return true;
	}

	// Start activities if selected from the menu
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.action_magnitude:
			Intent magnitudeActivity = new Intent(context, Magnitude.class);
			startActivity(magnitudeActivity);
			break;
		case R.id.action_training:
			if (!generating) {
				Intent trainingActivity = new Intent(context,
						TrainingActivity.class);
				startActivity(trainingActivity);
			}
			break;
		case R.id.action_history:
			Intent historyActivity = new Intent(context, HistoryActivity.class);
			startActivity(historyActivity);
			break;
		case R.id.action_settings:
			Intent settingsActivity = new Intent(context,
					RecognizerSettingsActivity.class);
			startActivity(settingsActivity);
			break;
		default:
			break;
		}

		return true;
	}

	private void showNotification(int textId, boolean autoCancel,
			boolean onGoing) {
		CharSequence text = getText(textId);
		CharSequence title = getText(R.string.recognizer_service_name);

		// Create a pending intent to open the activity
		Intent recognizerIntent = new Intent(this, RecognizerActivity.class);
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
		notificationManager.notify(textId, notification);
	}

	private void hideNotification(int textId) {
		notificationManager.cancel(textId);
	}

	// First application run test & set
	private boolean firstRun() {
		boolean currentValue;
		currentValue = sharedPrefs.getBoolean("first_run", true);
		sharedPrefs.edit().putBoolean("first_run", false).commit();
		return currentValue;
	}

	private void registerContentObserver() {
		ContentResolver cr = getContentResolver();
		if (evaluationsObserver == null) {
			evaluationsObserver = new EvaluationsContentObserver(
					contentProviderHandler);
			cr.registerContentObserver(EvaluationsProvider.CONTENT_URI, true,
					evaluationsObserver);
		}
	}

	private void unregisterContentObserver() {
		ContentResolver cr = getContentResolver();
		if (evaluationsObserver != null) {
			cr.unregisterContentObserver(evaluationsObserver);
			evaluationsObserver = null;
		}
	}

	// Button handler for "Start Provider"
	public void startProvider(View view) {

		if (!generating) {
			// Clear long term history
			evaluationsList.clear();
			@SuppressWarnings("unchecked")
			ArrayAdapter<VehicleInstance> adapter = (ArrayAdapter<VehicleInstance>) historyView
					.getAdapter();
			adapter.notifyDataSetChanged();

			// Start service with startService to keep it running
			// independently
			// startService does nothing if the service is already running
			Intent intent = new Intent(RecognizerActivity.this,
					RecognizerService.class);
			startService(intent);

			// Start listening for changes to the provider
			registerContentObserver();

			// Set start/stop buttons
			startServiceButton.setEnabled(false);
			stopServiceButton.setEnabled(true);

			// Show persistent notification
			showNotification(NOTIFICATION_RECOGNIZER_STARTED, false, true);
		} else {
			Toast.makeText(RecognizerActivity.this,
					R.string.error_isgenerating, Toast.LENGTH_SHORT).show();
		}

	}

	// Button handler for "Stop Provider"
	public void stopProvider(View view) {
		unregisterContentObserver();
		Context context = view.getContext();

		Intent service = new Intent(context, RecognizerService.class);
		stopService(service);

		// View neutral classification
		noneView.setVisibility(View.VISIBLE);
		walkingView.setVisibility(View.INVISIBLE);
		carView.setVisibility(View.INVISIBLE);
		trainView.setVisibility(View.INVISIBLE);
		idleView.setVisibility(View.INVISIBLE);

		// Set current classification to none
		currentClassification = "none";

		// Set start/stop buttons
		startServiceButton.setEnabled(true);
		stopServiceButton.setEnabled(false);

		// Hide persistent notification
		hideNotification(NOTIFICATION_RECOGNIZER_STARTED);
	}

	private void updateUI() {

		// Get last evaluation from content provider
		Uri uri = Uri.parse(EvaluationsProvider.CONTENT_URI + "/last");
		Cursor cursor = getContentResolver().query(uri, allColumnsProjection,
				null, null, null);
		if (cursor == null) {
			return;
		}
		cursor.moveToFirst();
		VehicleInstance lastInstance = EvaluationsProvider
				.cursorToVehicleInstance(cursor);

		// Update long history
		evaluationsList.add(lastInstance);
		@SuppressWarnings("unchecked")
		ArrayAdapter<VehicleInstance> adapter = (ArrayAdapter<VehicleInstance>) historyView
				.getAdapter();
		adapter.notifyDataSetChanged();

		// If classification changed, switch image
		if (currentClassification != lastInstance.getCategory()) {

			currentClassification = lastInstance.getCategory();

			// switch not working with JRE under 1.7
			if (currentClassification.equals("car")) {
				carView.setVisibility(View.VISIBLE);
				noneView.setVisibility(View.INVISIBLE);
				walkingView.setVisibility(View.INVISIBLE);
				trainView.setVisibility(View.INVISIBLE);
				idleView.setVisibility(View.INVISIBLE);

			}

			if (currentClassification.equals("walking")) {
				walkingView.setVisibility(View.VISIBLE);
				noneView.setVisibility(View.INVISIBLE);
				carView.setVisibility(View.INVISIBLE);
				trainView.setVisibility(View.INVISIBLE);
				idleView.setVisibility(View.INVISIBLE);

			}

			if (currentClassification.equals("train")) {
				trainView.setVisibility(View.VISIBLE);
				noneView.setVisibility(View.INVISIBLE);
				carView.setVisibility(View.INVISIBLE);
				walkingView.setVisibility(View.INVISIBLE);
				idleView.setVisibility(View.INVISIBLE);

			}

			if (currentClassification.equals("idle")) {
				trainView.setVisibility(View.INVISIBLE);
				noneView.setVisibility(View.INVISIBLE);
				carView.setVisibility(View.INVISIBLE);
				walkingView.setVisibility(View.INVISIBLE);
				idleView.setVisibility(View.VISIBLE);

			}

		}

	}

	@Override
	public void onRestart() {
		super.onRestart();

		registerContentObserver();
	}

	@Override
	public void onPause() {
		unregisterContentObserver();

		super.onPause();
	}

	private void resetFromAssets() {
		AssetManager assets = getAssets();
		File filesDir = getFilesDir();
		FileInputStream walkingAsset;
		FileInputStream carAsset;
		FileInputStream trainAsset;
		FileInputStream idleAsset;
		FileInputStream vehiclesAsset;

		try {
			// Get assets
			walkingAsset = assets.openFd("walking_1000lines.gif")
					.createInputStream();
			carAsset = assets.openFd("car_1000lines.gif").createInputStream();
			trainAsset = assets.openFd("train_1000lines.gif")
					.createInputStream();
			idleAsset = assets.openFd("idle_1000lines.gif").createInputStream();
			vehiclesAsset = assets.openFd("vehicles").createInputStream();

			// Copy the arff files to app files directory
			File newWalkingFile = new File(filesDir.getPath() + "/walking.arff");
			File newCarFile = new File(filesDir.getPath() + "/car.arff");
			File newTrainFile = new File(filesDir.getPath() + "/train.arff");
			File newIdleFile = new File(filesDir.getPath() + "/idle.arff");
			File newVehiclesFile = new File(filesDir.getPath() + "/vehicles");

			FileInputStream[] streams = { walkingAsset, carAsset, trainAsset,
					idleAsset, vehiclesAsset };
			File[] newFiles = { newWalkingFile, newCarFile, newTrainFile,
					newIdleFile, newVehiclesFile };
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

			File[] vehicleFiles = { newWalkingFile, newCarFile, newTrainFile,
					newIdleFile };
			
			// Erase training data from the database
			Uri uri = Uri.parse(EvaluationsProvider.TRAINING_DATA_URI
					+ EvaluationsProvider.PATH_ERASE_TRAINING_DATA);
			getContentResolver().delete(uri, null, null);
			
			// Insert the data from the files into the database
			uri = Uri.parse(EvaluationsProvider.TRAINING_DATA_URI
					+ EvaluationsProvider.PATH_INSERT_TRAINING_ITEM);
			getContentResolver().delete(uri, null, null);
			for (File f : vehicleFiles) {
				ArrayList<VehicleInstance> instances = ModelManager
						.readArffFile(f);

				for (VehicleInstance i : instances) {
					ContentValues values = EvaluationsProvider
							.vehicleInstanceToContentValues(i);

					getContentResolver().insert(
							EvaluationsProvider.TRAINING_DATA_URI, values);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}

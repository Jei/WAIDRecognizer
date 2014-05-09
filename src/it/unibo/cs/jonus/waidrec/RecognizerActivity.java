package it.unibo.cs.jonus.waidrec;

import java.io.File;
import java.io.IOException;
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
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.database.Cursor;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

public class RecognizerActivity extends Activity {

	private Context context;

	private SharedPreferences sharedPrefs;

	private Boolean generating = false;
	private ModelManager modelManager;
	private Thread asyncThread = null;

	private Handler contentProviderHandler = new Handler();
	private EditText samplingRateInput;
	private ListView historyView;
	private ArrayList<Evaluation> evaluationsList;

	private ImageView noneView;
	private ImageView walkingView;
	private ImageView carView;
	private ImageView trainView;
	private ImageView idleView;

	private String currentClassification;

	private EvaluationsContentObserver evaluationsObserver = null;

	private NotificationManager notificationManager;
	private static final int NOTIFICATION_GENERATING_MODEL = R.string.model_generating;
	private static final int NOTIFICATION_MODEL_GENERATED = R.string.model_generated;
	private static final int NOTIFICATION_MODEL_GENERATION_ERROR = R.string.model_generation_error;
	private static final int NOTIFICATION_RECOGNIZER_STARTED = R.string.recognizer_started;

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
				showNotification(NOTIFICATION_MODEL_GENERATED, true);

				generating = false;
			} else {
				new AlertDialog.Builder(context)
						.setTitle(getText(R.string.error))
						.setMessage(
								getText(NOTIFICATION_MODEL_GENERATION_ERROR))
						.show();
				hideNotification(NOTIFICATION_GENERATING_MODEL);
				showNotification(NOTIFICATION_MODEL_GENERATION_ERROR, true);

				generating = false;
			}
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

		context = this;

		// Set default preference values
		PreferenceManager.setDefaultValues(this, R.xml.waidrec_settings, false);

		// Get notification manager
		notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

		// Create new ModelManager
		modelManager = new ModelManager();

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
					modelManager.resetFromAssets(getAssets(), getFilesDir());
					Log.v("resetModel", "assets reset");
					modelManager.generateModel(getFilesDir());
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
			showNotification(NOTIFICATION_GENERATING_MODEL, false);
		}

		// Set initial classification
		currentClassification = "none";

		// Set input fields
		samplingRateInput = (EditText) findViewById(R.id.samplingRateInput);

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
		evaluationsList = new ArrayList<Evaluation>();
		historyView
				.setAdapter(new HistoryListAdapter(context, evaluationsList));
		historyView.setEmptyView(findViewById(R.id.emptyView));
		historyView.setTranscriptMode(ListView.TRANSCRIPT_MODE_ALWAYS_SCROLL);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.recognizer, menu);
		
		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		// Update the Training menu item if we are generating a new model
		menu.findItem(R.id.action_training).setEnabled(!generating);
		
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
		default:
			break;
		}

		return true;
	}

	private void showNotification(int textId, boolean autoCancel) {
		// In this sample, we'll use the same text for the ticker and the
		// expanded notification
		CharSequence text = getText(textId);

		// Set the icon, scrolling text and timestamp
		Notification notification = new Notification(R.drawable.ic_launcher,
				text, System.currentTimeMillis());

		// The PendingIntent to launch our activity if the user selects this
		// notification
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
				new Intent(this, RecognizerActivity.class), 0);

		// Set the info for the views that show in the notification panel.
		notification.setLatestEventInfo(this,
				getText(R.string.recognizer_service_name), text, contentIntent);

		if (autoCancel) {
			notification.flags |= Notification.FLAG_AUTO_CANCEL;
		}

		// Send the notification.
		notificationManager.notify(textId, notification);
	}

	private void hideNotification(int textId) {
		notificationManager.cancel(textId);
	}

	// First application run test & set
	private boolean firstRun() {
		boolean currentValue;
		sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
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

		if (samplingRateInput.getText().length() != 0) {
			if (!generating) {
				// Get sampling rate and history length from input
				int sampling = Integer.parseInt(samplingRateInput.getText()
						.toString());

				// Clear long term history
				evaluationsList.clear();
				@SuppressWarnings("unchecked")
				ArrayAdapter<Evaluation> adapter = (ArrayAdapter<Evaluation>) historyView
						.getAdapter();
				adapter.notifyDataSetChanged();

				samplingRateInput.setEnabled(false);
				samplingRateInput.setInputType(InputType.TYPE_NULL);

				// Start service with startService to keep it running
				// independently
				// startService does nothing if the service is already running
				Intent intent = new Intent(RecognizerActivity.this,
						RecognizerService.class);
				intent.putExtra("sampling", sampling);
				startService(intent);

				// Start listening for changes to the provider
				registerContentObserver();

				// Show persistent notification
				showNotification(NOTIFICATION_RECOGNIZER_STARTED, false);
			} else {
				Toast.makeText(RecognizerActivity.this,
						R.string.error_isgenerating, Toast.LENGTH_SHORT).show();
			}
		} else {
			Toast.makeText(RecognizerActivity.this, R.string.error_empty_field,
					Toast.LENGTH_SHORT).show();
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

		// Set input values editable
		samplingRateInput.setEnabled(true);
		samplingRateInput.setInputType(InputType.TYPE_CLASS_NUMBER);

		// Set current classification to none
		currentClassification = "none";

		// Append temp data to history
		String tempFileName = getFilesDir() + File.separator
				+ ModelManager.TEMP_FILE_NAME;
		try {
			File historyFile = new File(getFilesDir() + File.separator
					+ ModelManager.HISTORY_FILE_NAME);
			modelManager.appendToArffFile(new File(tempFileName), historyFile);
		} catch (IOException e) {
			Log.v("TrainingService", "Error while overwriting vehicle file");
			e.printStackTrace();
		}

		// Hide persistent notification
		hideNotification(NOTIFICATION_RECOGNIZER_STARTED);
	}

	private void updateUI() {

		// Get last evaluation from content provider
		String[] projection = { DatabaseOpenHelper.COLUMN_ID,
				DatabaseOpenHelper.COLUMN_TIMESTAMP,
				DatabaseOpenHelper.COLUMN_CATEGORY };
		Uri uri = Uri.parse(EvaluationsProvider.CONTENT_URI + "/last");
		Cursor cursor = getContentResolver().query(uri, projection, null, null,
				null);
		if (cursor == null) {
			return;
		}
		cursor.moveToFirst();
		long id = cursor.getLong(cursor
				.getColumnIndexOrThrow(DatabaseOpenHelper.COLUMN_ID));
		String category = cursor.getString(cursor
				.getColumnIndexOrThrow(DatabaseOpenHelper.COLUMN_CATEGORY));
		long timestamp = cursor.getLong(cursor
				.getColumnIndexOrThrow(DatabaseOpenHelper.COLUMN_TIMESTAMP));

		// Update long history
		evaluationsList.add(new Evaluation(id, timestamp, category));
		@SuppressWarnings("unchecked")
		ArrayAdapter<Evaluation> adapter = (ArrayAdapter<Evaluation>) historyView
				.getAdapter();
		adapter.notifyDataSetChanged();

		// If classification changed, switch image
		if (currentClassification != category) {

			currentClassification = category;

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

}

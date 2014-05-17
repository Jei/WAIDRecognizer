package it.unibo.cs.jonus.waidrec;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

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
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;

public class TrainingActivity extends Activity {

	private SharedPreferences sharedPrefs;
	private boolean generating = false;

	private RadioGroup writeModeGroup;
	private Spinner vehicleSpinner;
	private EditText trainingFrequencyInput;
	private RadioButton overwriteRadio;
	private RadioButton appendRadio;
	private Button startButton;
	private Button stopButton;
	private Button modelResetButton;

	private ProgressDialog progressDialog = null;

	private Context context;

	private Thread asyncThread = null;

	private ModelManager modelManager;

	private NotificationManager notificationManager;
	private static final int NOTIFICATION_GENERATING_MODEL = R.string.model_generating;
	private static final int NOTIFICATION_MODEL_GENERATED = R.string.model_generated;
	private static final int NOTIFICATION_MODEL_GENERATION_ERROR = R.string.model_generation_error;
	private static final int NOTIFICATION_MODEL_GENERATION_IOEXCEPTION = R.string.model_generation_ioexception;
	private static final int NOTIFICATION_MODEL_GENERATION_EXCEPTION = R.string.model_generation_exception;
	private static final int NOTIFICATION_TRAINING_RUNNING = R.string.training_service_running;
	private static final int ERROR_MODEL_GENERATION_IOEXCEPTION = 10;
	private static final int ERROR_MODEL_GENERATION_EXCEPTION = 11;
	private static final int MODE_AVAILABLE = 0;
	private static final int MODE_TRAINING = 1;
	private static final int MODE_GENERATING = 2;
	private static final String KEY_TRAINING_ISRUNNING = "training_isrunning";
	private static final String KEY_TRAINING_CURRENT_VEHICLE = "training_current_vehicle";
	private static final String KEY_TRAINING_CURRENT_WRITE_MODE = "training_current_write_mode";

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

			switch (code) {
			case 0:
				Toast.makeText(TrainingActivity.this, R.string.model_generated,
						Toast.LENGTH_SHORT).show();
				hideNotification(NOTIFICATION_GENERATING_MODEL);
				showNotification(NOTIFICATION_MODEL_GENERATED, true);

				break;
			case ERROR_MODEL_GENERATION_IOEXCEPTION:
				new AlertDialog.Builder(context)
						.setTitle(getText(R.string.error))
						.setMessage(
								getText(NOTIFICATION_MODEL_GENERATION_IOEXCEPTION))
						.show();
				hideNotification(NOTIFICATION_GENERATING_MODEL);
				showNotification(NOTIFICATION_MODEL_GENERATION_IOEXCEPTION,
						true);

				break;
			case ERROR_MODEL_GENERATION_EXCEPTION:
				new AlertDialog.Builder(context)
						.setTitle(getText(R.string.error))
						.setMessage(
								getText(NOTIFICATION_MODEL_GENERATION_EXCEPTION))
						.show();
				hideNotification(NOTIFICATION_GENERATING_MODEL);
				showNotification(NOTIFICATION_MODEL_GENERATION_EXCEPTION, true);

				break;
			default:
				new AlertDialog.Builder(context)
						.setTitle(getText(R.string.error))
						.setMessage(
								getText(NOTIFICATION_MODEL_GENERATION_ERROR))
						.show();
				hideNotification(NOTIFICATION_GENERATING_MODEL);
				showNotification(NOTIFICATION_MODEL_GENERATION_ERROR, true);

				break;
			}

			// Update the UI
			setUIMode(MODE_AVAILABLE);

			generating = false;
		}
	};

	// Runnable for model generation thread
	private class ModelGenRunnable implements Runnable {
		ProgressDialog dialog;

		public ModelGenRunnable(ProgressDialog pd) {
			dialog = pd;
		}

		public void run() {
			// Prepare message for thread completion
			Message message = new Message();
			// Generate new model
			try {
				modelManager.generateModel(getFilesDir());
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
			if (!Thread.interrupted() && threadHandler != null) {
				// Notify thread completion
				message.obj = dialog;
				threadHandler.sendMessage(message);
			}

		}
	};

	// Runnable for model reset thread
	private class ModelResetRunnable implements Runnable {
		ProgressDialog dialog;

		public ModelResetRunnable(ProgressDialog pd) {
			dialog = pd;
		}

		public void run() {
			// Prepare message for thread completion
			Message message = new Message();
			// Reset original arff files and regenerate model
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
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_training);

		// Get shared preferences
		sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);

		writeModeGroup = (RadioGroup) findViewById(R.id.modeRadioGroup);
		trainingFrequencyInput = (EditText) findViewById(R.id.trainingFreqInput);
		vehicleSpinner = (Spinner) findViewById(R.id.classesSpinner);
		overwriteRadio = (RadioButton) findViewById(R.id.overwriteRadioButton);
		appendRadio = (RadioButton) findViewById(R.id.appendRadioButton);
		startButton = (Button) findViewById(R.id.startTrainingButton);
		stopButton = (Button) findViewById(R.id.stopTrainingButton);
		modelResetButton = (Button) findViewById(R.id.resetModelButton);
		stopButton.setEnabled(false);

		// Get context for general use
		context = this;

		// Get model manager
		modelManager = new ModelManager();

		// Get notification manager
		notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

		// Populate the vehicles spinner
		ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
				this, R.array.default_classes_names,
				android.R.layout.simple_spinner_item);
		// Specify the layout to use when the list of choices appears
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		// Apply the adapter to the spinner
		vehicleSpinner.setAdapter(adapter);

		// Restore UI elements from the saved state
		if (savedInstanceState != null) {
			vehicleSpinner.setSelection(savedInstanceState
					.getInt("selectedVehicle"));
			writeModeGroup
					.check(savedInstanceState.getInt("selectedWriteMode"));
		}

	}

	@Override
	public void onResume() {
		super.onResume();

		// If the training service is already running, update the UI
		boolean isServiceRunning = sharedPrefs.getBoolean(
				KEY_TRAINING_ISRUNNING, false);
		if (isServiceRunning) {
			setUIMode(MODE_TRAINING);
		} else {
			setUIMode(MODE_AVAILABLE);
		}
		List<String> vehiclesArray = Arrays.asList(getResources()
				.getStringArray(R.array.default_classes_keys));
		vehicleSpinner.setSelection(vehiclesArray.indexOf(sharedPrefs
				.getString(KEY_TRAINING_CURRENT_VEHICLE, "idle")));
		writeModeGroup.check(sharedPrefs.getInt(
				KEY_TRAINING_CURRENT_WRITE_MODE, R.id.appendRadioButton));
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		// getMenuInflater().inflate(R.menu.training, menu);
		return true;
	}

	public void startTraining(View view) {
		boolean isServiceRunning = sharedPrefs.getBoolean(
				KEY_TRAINING_ISRUNNING, false);

		if (!isServiceRunning && !generating
				&& trainingFrequencyInput.getText().length() != 0) {
			// Get the selected radio button for write mode
			int writeMode = ((RadioGroup) findViewById(R.id.modeRadioGroup))
					.getCheckedRadioButtonId();
			// Get confirmation from user
			AlertDialog.Builder confirmDialog = new AlertDialog.Builder(this);
			confirmDialog.setTitle("Warning");
			switch (writeMode) {
			case R.id.overwriteRadioButton:
				confirmDialog
						.setMessage(getText(R.string.training_confirm_overwrite));
				break;
			case R.id.appendRadioButton:
				confirmDialog
						.setMessage(getText(R.string.training_confirm_append));
				break;
			}
			confirmDialog.setPositiveButton(android.R.string.ok,
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							// Get training frequency
							int trainingFrequency = Integer
									.parseInt(trainingFrequencyInput.getText()
											.toString());
							// Get the selected item key from the vehicles
							// spinner
							String vehicle = getResources().getStringArray(
									R.array.default_classes_keys)[vehicleSpinner
									.getSelectedItemPosition()];
							sharedPrefs
									.edit()
									.putString(KEY_TRAINING_CURRENT_VEHICLE,
											vehicle).commit();
							// Get the selected radio button for write mode
							int writeMode = ((RadioGroup) findViewById(R.id.modeRadioGroup))
									.getCheckedRadioButtonId();
							sharedPrefs
									.edit()
									.putInt(KEY_TRAINING_CURRENT_WRITE_MODE,
											writeMode).commit();
							// Create and bind to the new TrainingService
							Intent serviceIntent = new Intent(
									TrainingActivity.this,
									TrainingService.class);
							// Add extra informations for the service
							serviceIntent.putExtra("class", vehicle);
							serviceIntent.putExtra("frequency",
									trainingFrequency);
							switch (writeMode) {
							case R.id.appendRadioButton:
								serviceIntent.putExtra("append", true);
								break;
							case R.id.overwriteRadioButton:
								serviceIntent.putExtra("append", false);
								break;
							}

							startService(serviceIntent);

							// Update the UI
							setUIMode(MODE_TRAINING);

							Toast.makeText(TrainingActivity.this,
									R.string.training_service_started,
									Toast.LENGTH_SHORT).show();

							showNotification(NOTIFICATION_TRAINING_RUNNING,
									false);
						}
					});
			confirmDialog.setNegativeButton(android.R.string.cancel,
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							dialog.dismiss();
						}
					});
			confirmDialog.show();
		}
	}

	public void stopTraining(View view) {
		boolean isServiceRunning = sharedPrefs.getBoolean(
				KEY_TRAINING_ISRUNNING, false);

		if (isServiceRunning) {
			// Create progress dialog
			progressDialog = new ProgressDialog(context);
			progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
			progressDialog.setMessage(getResources().getString(
					NOTIFICATION_GENERATING_MODEL));
			progressDialog.setIndeterminate(true);
			progressDialog.setCancelable(false);
			progressDialog.show();

			// Stop the training service
			Intent serviceIntent = new Intent(TrainingActivity.this,
					TrainingService.class);
			stopService(serviceIntent);

			hideNotification(NOTIFICATION_TRAINING_RUNNING);

			generating = true;

			// Get the write mode and the vehicle from the preferences
			String vehicle = sharedPrefs.getString(
					KEY_TRAINING_CURRENT_VEHICLE, "idle");
			int writeMode = sharedPrefs.getInt(KEY_TRAINING_CURRENT_WRITE_MODE,
					R.id.appendRadioButton);

			// Overwrite old vehicle file with the temp file
			String tempFileName = getFilesDir() + "/temp.arff";
			String vehicleFileName = getFilesDir() + "/" + vehicle + ".arff";
			try {
				switch (writeMode) {
				case R.id.appendRadioButton:
					modelManager.appendToArffFile(new File(tempFileName),
							new File(vehicleFileName));
					break;
				case R.id.overwriteRadioButton:
					modelManager.overwriteArffFile(new File(tempFileName),
							new File(vehicleFileName));
					break;
				}
			} catch (IOException e) {
				Log.v("TrainingService", "Error while overwriting vehicle file");
				e.printStackTrace();
			}

			// TODO write temp file to external storage
			/*
			 * try { File externalStorage = new File(Environment
			 * .getExternalStorageDirectory().toString() + File.separator +
			 * "waidrec"); externalStorage.mkdirs();
			 * modelManager.overwriteArffFile(new File(vehicleFileName), new
			 * File(externalStorage.getPath() + File.separator + vehicle +
			 * ".arff")); } catch (IOException e) { // TODO Auto-generated catch
			 * block e.printStackTrace(); }
			 */

			// Run model generation thread
			ModelGenRunnable runnable = new ModelGenRunnable(progressDialog);
			asyncThread = new Thread(null, runnable, "ModelGen", 204800);
			asyncThread.start();

			showNotification(NOTIFICATION_GENERATING_MODEL, false);
			
			//XXX Set the TrainingService preference to false
			sharedPrefs.edit().putBoolean(KEY_TRAINING_ISRUNNING, false).commit();

		}
	}

	public void resetModel(View view) {
		boolean isServiceRunning = sharedPrefs.getBoolean(
				KEY_TRAINING_ISRUNNING, false);

		if (!isServiceRunning && !generating) {
			// Create progress dialog
			final ProgressDialog progressDialog = new ProgressDialog(this);
			progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
			progressDialog.setMessage(getResources().getString(
					NOTIFICATION_GENERATING_MODEL));
			progressDialog.setIndeterminate(true);
			progressDialog.setCancelable(false);
			progressDialog.show();

			// Run model reset thread
			ModelResetRunnable runnable = new ModelResetRunnable(progressDialog);
			asyncThread = new Thread(null, runnable, "ModelReset", 204800);
			asyncThread.start();

			// Show a notification
			showNotification(NOTIFICATION_GENERATING_MODEL, false);

			// Update the UI
			setUIMode(MODE_GENERATING);

			generating = true;
		}
	}

	// TODO switch to non deprecated methods
	private void showNotification(int textId, boolean autoCancel) {
		// In this sample, we'll use the same text for the ticker and the
		// expanded notification
		CharSequence text = getText(textId);

		// Set the icon, scrolling text and timestamp
		Notification notification = new Notification(R.drawable.ic_launcher,
				text, System.currentTimeMillis());

		// Add a pending intent to open the activity
		Intent trainingIntent = new Intent(this, TrainingActivity.class);
		trainingIntent.setAction(Intent.ACTION_MAIN);
		trainingIntent.addCategory(Intent.CATEGORY_LAUNCHER);
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
				trainingIntent, 0);

		// Set the info for the views that show in the notification panel.
		notification.setLatestEventInfo(this,
				getText(R.string.training_service_label), text, contentIntent);

		if (autoCancel) {
			notification.flags |= Notification.FLAG_AUTO_CANCEL;
		}

		// Send the notification.
		notificationManager.notify(textId, notification);
	}

	private void hideNotification(int textId) {
		notificationManager.cancel(textId);
	}

	// Update the UI elements to reflect the current state of the training
	// service
	private void setUIMode(int mode) {
		switch (mode) {
		case MODE_AVAILABLE:
			// Enable all the elements except for Training Stop
			vehicleSpinner.setEnabled(true);
			trainingFrequencyInput.setEnabled(true);
			overwriteRadio.setEnabled(true);
			appendRadio.setEnabled(true);
			startButton.setEnabled(true);
			modelResetButton.setEnabled(true);
			stopButton.setEnabled(false);
			break;
		case MODE_TRAINING:
			vehicleSpinner.setEnabled(false);
			trainingFrequencyInput.setEnabled(false);
			overwriteRadio.setEnabled(false);
			appendRadio.setEnabled(false);
			startButton.setEnabled(false);
			modelResetButton.setEnabled(false);
			stopButton.setEnabled(true);
			break;
		case MODE_GENERATING:
			// Disable all the elements
			vehicleSpinner.setEnabled(false);
			trainingFrequencyInput.setEnabled(false);
			overwriteRadio.setEnabled(false);
			appendRadio.setEnabled(false);
			startButton.setEnabled(false);
			stopButton.setEnabled(false);
			modelResetButton.setEnabled(false);
			break;
		}

	}

}

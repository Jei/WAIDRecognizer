package it.unibo.cs.jonus.waidrec;

import java.io.File;
import java.io.IOException;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
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

	private boolean serviceIsBound = false;
	private boolean generating = false;

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
	private static final int NOTIFICATION_TRAINING_RUNNING = R.string.training_service_running;

	private ServiceConnection trainingServiceConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder service) {
			Toast.makeText(TrainingActivity.this,
					R.string.training_service_connected, Toast.LENGTH_SHORT)
					.show();
		}

		public void onServiceDisconnected(ComponentName className) {
			Toast.makeText(TrainingActivity.this,
					R.string.training_service_disconnected, Toast.LENGTH_SHORT)
					.show();
		}
	};

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
				Toast.makeText(TrainingActivity.this, R.string.model_generated,
						Toast.LENGTH_SHORT).show();
				hideNotification(NOTIFICATION_GENERATING_MODEL);
				showNotification(NOTIFICATION_MODEL_GENERATED, true);
				
				vehicleSpinner.setEnabled(true);
				trainingFrequencyInput.setEnabled(true);
				overwriteRadio.setEnabled(true);
				appendRadio.setEnabled(true);
				startButton.setEnabled(true);
				modelResetButton.setEnabled(true);
				
				stopButton.setEnabled(false);
				
				generating = false;
			} else {
				new AlertDialog.Builder(context)
						.setTitle(getText(R.string.error))
						.setMessage(
								getText(NOTIFICATION_MODEL_GENERATION_ERROR))
						.show();
				hideNotification(NOTIFICATION_GENERATING_MODEL);
				showNotification(NOTIFICATION_MODEL_GENERATION_ERROR, true);

				vehicleSpinner.setEnabled(true);
				trainingFrequencyInput.setEnabled(true);
				overwriteRadio.setEnabled(true);
				appendRadio.setEnabled(true);
				startButton.setEnabled(true);
				modelResetButton.setEnabled(true);
				
				stopButton.setEnabled(false);
				
				generating = false;
			}
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		setContentView(R.layout.activity_training);
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

		super.onCreate(savedInstanceState);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		// getMenuInflater().inflate(R.menu.training, menu);
		return true;
	}

	public void startTraining(View view) {
		if (!serviceIsBound && !generating && trainingFrequencyInput.getText().length() != 0) {
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
							// Get the selected radio button for write mode
							int writeMode = ((RadioGroup) findViewById(R.id.modeRadioGroup))
									.getCheckedRadioButtonId();
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

							bindService(serviceIntent,
									trainingServiceConnection,
									Context.BIND_AUTO_CREATE);
							serviceIsBound = true;

							vehicleSpinner.setEnabled(false);
							trainingFrequencyInput.setEnabled(false);
							overwriteRadio.setEnabled(false);
							appendRadio.setEnabled(false);
							startButton.setEnabled(false);
							modelResetButton.setEnabled(false);
							
							stopButton.setEnabled(true);

							Toast.makeText(TrainingActivity.this,
									R.string.training_service_started,
									Toast.LENGTH_SHORT).show();
							
							showNotification(NOTIFICATION_TRAINING_RUNNING, false);
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
		// Runnable for model generation thread
		class ModelGenRunnable implements Runnable {
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

		if (serviceIsBound) {

			vehicleSpinner.setEnabled(true);
			trainingFrequencyInput.setEnabled(true);
			overwriteRadio.setEnabled(true);
			appendRadio.setEnabled(true);

			// Create progress dialog
			progressDialog = new ProgressDialog(context);
			progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
			progressDialog.setMessage(getResources().getString(
					NOTIFICATION_GENERATING_MODEL));
			progressDialog.setIndeterminate(true);
			progressDialog.setCancelable(false);
			progressDialog.show();

			// Detach our existing connection.
			unbindService(trainingServiceConnection);
			Intent serviceIntent = new Intent(TrainingActivity.this,
					TrainingService.class);
			stopService(serviceIntent);
			
			hideNotification(NOTIFICATION_TRAINING_RUNNING);
			
			serviceIsBound = false;
			generating = true;

			// Get the selected item key from the vehicles spinner
			String vehicle = getResources().getStringArray(
					R.array.default_classes_keys)[vehicleSpinner
					.getSelectedItemPosition()];
			// Get the selected radio button for write mode
			int writeMode = ((RadioGroup) findViewById(R.id.modeRadioGroup))
					.getCheckedRadioButtonId();

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
			/*try {
				File externalStorage = new File(Environment
						.getExternalStorageDirectory().toString()
						+ File.separator + "waidrec");
				externalStorage.mkdirs();
				modelManager.overwriteArffFile(new File(vehicleFileName),
						new File(externalStorage.getPath() + File.separator
								+ vehicle + ".arff"));
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}*/

			// Run model generation thread
			ModelGenRunnable runnable = new ModelGenRunnable(progressDialog);
			asyncThread = new Thread(null, runnable, "ModelGen", 204800);
			asyncThread.start();
			
			showNotification(NOTIFICATION_GENERATING_MODEL, false);

		}
	}

	public void resetModel(View view) {

		// Runnable for model reset thread
		class ModelResetRunnable implements Runnable {
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
		}
		;

		if (!serviceIsBound && !generating) {
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
			
			generating = true;
			
			// Disable all the buttons and inputs until completion
			vehicleSpinner.setEnabled(false);
			trainingFrequencyInput.setEnabled(false);
			overwriteRadio.setEnabled(false);
			appendRadio.setEnabled(false);
			startButton.setEnabled(false);
			stopButton.setEnabled(false);
			modelResetButton.setEnabled(false);
		}
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
				new Intent(this, TrainingActivity.class), 0);

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

}

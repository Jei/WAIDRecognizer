/**
 * 
 */
package it.unibo.cs.jonus.waidrec;

import it.unibo.cs.jonus.waidrec.MainActivity.ModelGenRunnable;

import java.util.Arrays;
import java.util.List;

import android.support.v4.app.Fragment;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;

/**
 * @author jei
 * 
 */
public class TrainingFragment extends Fragment {

	private static final int MODE_AVAILABLE = 0;
	private static final int MODE_TRAINING = 1;
	private static final String KEY_TRAINING_ISRUNNING = "training_isrunning";
	private static final String KEY_TRAINING_CURRENT_VEHICLE = "training_current_vehicle";
	private static final String KEY_TRAINING_CURRENT_APPEND = "training_current_append";
	private static final String KEY_RECOGNIZER_ISRUNNING = "recognizer_isrunning";
	private static final String KEY_TRN_START_DELAY = "pref_training_start_delay";

	private MainActivity mActivity;

	private SharedPreferences mSharedPrefs;

	private ImageView onView;
	private ImageView offView;
	private RadioGroup writeModeGroup;
	private Spinner vehicleSpinner;
	private RadioButton overwriteRadio;
	private RadioButton appendRadio;

	private ProgressDialog progressDialog = null;

	private Thread asyncThread = null;

	private OnClickListener startTrnListener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			startTraining(v);
		}
	};
	private OnClickListener stopTrnListener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			stopTraining(v);
		}
	};

	public TrainingFragment() {

	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_training, container,
				false);

		return rootView;
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		mActivity = (MainActivity) getActivity();
		// Get shared preferences
		mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(mActivity);

		writeModeGroup = (RadioGroup) view.findViewById(R.id.modeRadioGroup);
		vehicleSpinner = (Spinner) view.findViewById(R.id.classesSpinner);
		overwriteRadio = (RadioButton) view
				.findViewById(R.id.overwriteRadioButton);
		appendRadio = (RadioButton) view.findViewById(R.id.appendRadioButton);

		ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
				mActivity, R.array.default_classes_names,
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

		// Set on/off images
		offView = (ImageView) view.findViewById(R.id.trainingOffView);
		onView = (ImageView) view.findViewById(R.id.trainingOnView);

		// Set click listeners
		offView.setOnClickListener(startTrnListener);
		onView.setOnClickListener(stopTrnListener);

	}

	@Override
	public void onResume() {
		super.onResume();

		// If the training service is already running, update the UI
		boolean isServiceRunning = mSharedPrefs.getBoolean(
				KEY_TRAINING_ISRUNNING, false);
		if (isServiceRunning) {
			setUIMode(MODE_TRAINING);
		} else {
			setUIMode(MODE_AVAILABLE);
		}
		List<String> vehiclesArray = Arrays.asList(getResources()
				.getStringArray(R.array.default_classes_keys));
		vehicleSpinner.setSelection(vehiclesArray.indexOf(mSharedPrefs
				.getString(KEY_TRAINING_CURRENT_VEHICLE, "idle")));
		if (mSharedPrefs.getBoolean(KEY_TRAINING_CURRENT_APPEND, false)) {
			writeModeGroup.check(R.id.appendRadioButton);
		} else {
			writeModeGroup.check(R.id.overwriteRadioButton);
		}
	}

	public void startTraining(View view) {
		boolean isTrainingRunning = mSharedPrefs.getBoolean(
				KEY_TRAINING_ISRUNNING, false);
		boolean isRecognizerRunning = mSharedPrefs.getBoolean(
				KEY_RECOGNIZER_ISRUNNING, false);

		if (!isTrainingRunning && !isRecognizerRunning) {
			// Get the selected radio button for write mode
			int writeMode = ((RadioGroup) getView().findViewById(
					R.id.modeRadioGroup)).getCheckedRadioButtonId();
			// Get confirmation from user
			AlertDialog.Builder confirmDialog = new AlertDialog.Builder(
					mActivity);
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
							trainingDelayedStart();
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

	private void trainingDelayedStart() {
		String st = mSharedPrefs.getString(KEY_TRN_START_DELAY, "10");
		long startingTime = Integer.parseInt(st) * 1000;

		final ProgressDialog countdownDialog = new ProgressDialog(mActivity);
		countdownDialog.setTitle("Starting training");
		countdownDialog.setMessage(st);
		countdownDialog.setCancelable(false);
		countdownDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel",
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
					}
				});
		countdownDialog.show();

		new CountDownTimer(startingTime, 1000) {
			@Override
			public void onTick(long millisUntilFinished) {
				countdownDialog.setMessage("" + (millisUntilFinished / 1000));
			}

			@Override
			public void onFinish() {
				trainingStart();
				countdownDialog.dismiss();
			}
		}.start();
	}

	private void trainingStart() {
		// Get the selected vehicle
		String vehicle = getResources().getStringArray(
				R.array.default_classes_keys)[vehicleSpinner
				.getSelectedItemPosition()];
		mSharedPrefs.edit().putString(KEY_TRAINING_CURRENT_VEHICLE, vehicle)
				.commit();
		// Get the write mode
		int writeMode = ((RadioGroup) getView().findViewById(
				R.id.modeRadioGroup)).getCheckedRadioButtonId();
		switch (writeMode) {
		case R.id.appendRadioButton:
			mSharedPrefs.edit().putBoolean(KEY_TRAINING_CURRENT_APPEND, true)
					.commit();
			break;
		case R.id.overwriteRadioButton:
			mSharedPrefs.edit().putBoolean(KEY_TRAINING_CURRENT_APPEND, false)
					.commit();

			// Delete entries for vehicle from database
			String[] arg = { vehicle };
			Uri uri = Uri.parse(EvaluationsProvider.TRAINING_DATA_URI
					+ EvaluationsProvider.PATH_DELETE_TRAINING_VEHICLE);
			getActivity().getContentResolver().delete(uri, null, arg);
			break;
		}

		// Create and bind to the new TrainingService
		Intent serviceIntent = new Intent(mActivity, TrainingService.class);
		mActivity.startService(serviceIntent);

		// Update the UI
		setUIMode(MODE_TRAINING);

		Toast.makeText(mActivity, R.string.training_service_started,
				Toast.LENGTH_SHORT).show();

		mActivity.showNotification(MainActivity.NOTIFICATION_TRAINING_RUNNING,
				false, true);
	}

	public void stopTraining(View view) {
		boolean isServiceRunning = mSharedPrefs.getBoolean(
				KEY_TRAINING_ISRUNNING, false);

		if (isServiceRunning) {
			// Create progress dialog
			progressDialog = new ProgressDialog(mActivity);
			progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
			progressDialog.setMessage(getResources().getString(
					MainActivity.NOTIFICATION_GENERATING_MODEL));
			progressDialog.setIndeterminate(true);
			progressDialog.setCancelable(false);
			progressDialog.show();

			// Stop the training service
			Intent serviceIntent = new Intent(mActivity, TrainingService.class);
			mActivity.stopService(serviceIntent);

			mActivity
					.hideNotification(MainActivity.NOTIFICATION_TRAINING_RUNNING);

			// Run model generation thread
			ModelGenRunnable runnable = new ModelGenRunnable(mActivity,
					progressDialog);
			asyncThread = new Thread(null, runnable, "ModelGen", 204800);
			asyncThread.start();

			mActivity.showNotification(
					MainActivity.NOTIFICATION_GENERATING_MODEL, false, true);

			setUIMode(MODE_AVAILABLE);

			// XXX Set the TrainingService preference to false
			mSharedPrefs.edit().putBoolean(KEY_TRAINING_ISRUNNING, false)
					.commit();

		}
	}

	// Update the UI to reflect the current state of the training service
	private void setUIMode(int mode) {
		switch (mode) {
		case MODE_AVAILABLE:
			// Enable all the elements except for Training Stop
			vehicleSpinner.setEnabled(true);
			overwriteRadio.setEnabled(true);
			appendRadio.setEnabled(true);
			onView.setVisibility(View.INVISIBLE);
			offView.setVisibility(View.VISIBLE);
			break;
		case MODE_TRAINING:
			vehicleSpinner.setEnabled(false);
			overwriteRadio.setEnabled(false);
			appendRadio.setEnabled(false);
			onView.setVisibility(View.VISIBLE);
			offView.setVisibility(View.INVISIBLE);
			break;
		}

	}

}

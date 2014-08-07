/**
 * 
 */
package it.unibo.cs.jonus.waidrec;

import java.util.ArrayList;
import java.util.HashMap;

import android.support.v4.app.Fragment;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;

/**
 * @author jei
 * 
 */
public class RecognizerFragment extends Fragment {

	private static final String KEY_RECOGNIZER_ISRUNNING = "recognizer_isrunning";
	private static final String KEY_TRAINING_ISRUNNING = "training_isrunning";

	private MainActivity mActivity;

	private SharedPreferences mSharedPrefs;

	private Handler contentProviderHandler = new Handler();
	private ListView historyView;
	private ArrayList<VehicleInstance> evaluationsList;

	private ImageView vehicleView;
	private ImageView onoffView;
	private HashMap<String, Bitmap> mVehicles = new HashMap<String, Bitmap>();

	private String mCurrentVehicle;

	private OnClickListener startRecListener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			startProvider(v);
		}
	};
	private OnClickListener stopRecListener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			stopProvider(v);
		}
	};

	// Class used to listen to changes on the provider
	private class EvaluationsContentObserver extends ContentObserver {

		public EvaluationsContentObserver(Handler handler) {
			super(handler);
		}

		public void onChange(boolean selfChange) {
			super.onChange(selfChange);

			updateUI();
		}

	}

	private EvaluationsContentObserver evaluationsObserver = null;

	public RecognizerFragment() {

	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_recognizer,
				container, false);

		return rootView;
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		mActivity = (MainActivity) getActivity();
		// Get shared preferences
		mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(mActivity);

		// Set classifications images
		vehicleView = (ImageView) view.findViewById(R.id.vehicleView);
		onoffView = (ImageView) view.findViewById(R.id.onoffView);

		// Get vehicles and icons from the db
		Uri uri = Uri.parse(EvaluationsProvider.VEHICLES_URI
				+ EvaluationsProvider.PATH_ALL_VEHICLES);
		Cursor cursor = getActivity().getContentResolver().query(uri,
				MainActivity.vehicleColumnsProjection, null, null, null);
		ArrayList<VehicleItem> items = EvaluationsProvider
				.cursorToVehicleItemArray(cursor);
		for (VehicleItem i : items) {
			mVehicles.put(i.getCategory(), i.getIcon());
		}
		// Get "none" bitmap and add it
		Bitmap noneBmp = BitmapFactory.decodeResource(getResources(),
				R.drawable.none_00b0b0);
		mVehicles.put("none", noneBmp);

		// Set on/off image visible
		onoffView.setVisibility(View.VISIBLE);

		// Set click listeners
		onoffView.setOnClickListener(startRecListener);
		vehicleView.setOnClickListener(stopRecListener);

		mCurrentVehicle = "";

		// Prepare history view
		historyView = (ListView) view.findViewById(R.id.historyView);
		evaluationsList = new ArrayList<VehicleInstance>();
		historyView.setAdapter(new HistoryListAdapter(mActivity,
				evaluationsList));
		historyView.setEmptyView(view.findViewById(R.id.emptyView));
		historyView.setTranscriptMode(ListView.TRANSCRIPT_MODE_ALWAYS_SCROLL);
	}

	@Override
	public void onResume() {
		super.onResume();

		// Check if RecognizerService is already running
		if (mSharedPrefs.getBoolean(KEY_RECOGNIZER_ISRUNNING, false)) {
			startProvider(onoffView);
		}
	}

	@Override
	public void onPause() {
		unregisterContentObserver();

		super.onPause();
	}

	// Button handler for "Start Provider"
	private void startProvider(View view) {
		boolean isTrainingRunning = mSharedPrefs.getBoolean(
				KEY_TRAINING_ISRUNNING, false);

		// Check if TrainingService is running
		if (!isTrainingRunning) {
			// Clear long term history
			evaluationsList.clear();
			@SuppressWarnings("unchecked")
			ArrayAdapter<VehicleInstance> adapter = (ArrayAdapter<VehicleInstance>) historyView
					.getAdapter();
			adapter.notifyDataSetChanged();

			// Use startService to keep it running independently
			Intent intent = new Intent(mActivity, RecognizerService.class);
			mActivity.startService(intent);

			// Start listening for changes to the provider
			registerContentObserver();

			// Hide the on/off image
			onoffView.setVisibility(View.INVISIBLE);
			vehicleView.setVisibility(View.VISIBLE);
			vehicleView.setImageBitmap(null);

			// Show persistent notification
			mActivity.showNotification(
					MainActivity.NOTIFICATION_RECOGNIZER_STARTED, false, true);
		}
	}

	// Button handler for "Stop Provider"
	private void stopProvider(View view) {
		unregisterContentObserver();
		Context context = view.getContext();

		Intent service = new Intent(context, RecognizerService.class);
		mActivity.stopService(service);

		// View neutral classification
		onoffView.setVisibility(View.VISIBLE);
		vehicleView.setVisibility(View.INVISIBLE);
		mCurrentVehicle = "";

		// Hide persistent notification
		mActivity
				.hideNotification(MainActivity.NOTIFICATION_RECOGNIZER_STARTED);
	}

	private void registerContentObserver() {
		ContentResolver cr = mActivity.getContentResolver();
		if (evaluationsObserver == null) {
			evaluationsObserver = new EvaluationsContentObserver(
					contentProviderHandler);
			cr.registerContentObserver(EvaluationsProvider.CONTENT_URI, true,
					evaluationsObserver);
		}
	}

	private void unregisterContentObserver() {
		ContentResolver cr = mActivity.getContentResolver();
		if (evaluationsObserver != null) {
			cr.unregisterContentObserver(evaluationsObserver);
			evaluationsObserver = null;
		}
	}

	private void updateUI() {

		// Get last two evaluations from content provider
		Uri uri = Uri.parse(EvaluationsProvider.CONTENT_URI
				+ EvaluationsProvider.PATH_LAST_EVALUATION);
		Cursor cursor = mActivity.getContentResolver().query(uri,
				MainActivity.allColumnsProjection, null, null, null);
		if (cursor == null) {
			return;
		}
		cursor.moveToFirst();
		VehicleInstance lastInstance = EvaluationsProvider
				.cursorToVehicleInstance(cursor);
		cursor.close();

		// Update long history
		evaluationsList.add(lastInstance);
		@SuppressWarnings("unchecked")
		ArrayAdapter<VehicleInstance> adapter = (ArrayAdapter<VehicleInstance>) historyView
				.getAdapter();
		adapter.notifyDataSetChanged();

		// If classification changed, switch image
		String newVehicle = lastInstance.getCategory();
		if (!newVehicle.equals(mCurrentVehicle)) {
			mCurrentVehicle = newVehicle;

			if (mVehicles.containsKey(newVehicle)) {
				vehicleView.setImageBitmap(mVehicles.get(newVehicle));
			} else {
				vehicleView.setImageBitmap(mVehicles.get("none"));
			}
		}

	}

}

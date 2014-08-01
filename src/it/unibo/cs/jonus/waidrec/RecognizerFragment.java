/**
 * 
 */
package it.unibo.cs.jonus.waidrec;

import java.util.ArrayList;

import android.support.v4.app.Fragment;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
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

	private MainActivity mActivity;

	private SharedPreferences mSharedPrefs;

	private Handler contentProviderHandler = new Handler();
	private ListView historyView;
	private ArrayList<VehicleInstance> evaluationsList;

	private ImageView noneView;
	private ImageView walkingView;
	private ImageView carView;
	private ImageView trainView;
	private ImageView idleView;
	private ImageView onoffView;

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
		noneView = (ImageView) view.findViewById(R.id.noneView);
		walkingView = (ImageView) view.findViewById(R.id.walkingView);
		carView = (ImageView) view.findViewById(R.id.carView);
		trainView = (ImageView) view.findViewById(R.id.trainView);
		idleView = (ImageView) view.findViewById(R.id.idleView);
		onoffView = (ImageView) view.findViewById(R.id.onoffView);

		// Set on/off image visible
		onoffView.setVisibility(View.VISIBLE);
		
		// Set click listeners
		onoffView.setOnClickListener(startRecListener);
		noneView.setOnClickListener(stopRecListener);
		walkingView.setOnClickListener(stopRecListener);
		carView.setOnClickListener(stopRecListener);
		trainView.setOnClickListener(stopRecListener);
		idleView.setOnClickListener(stopRecListener);
		
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
		if (mSharedPrefs.getBoolean("recognizer_isrunning", false)) {
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

		// Show persistent notification
		mActivity.showNotification(
				MainActivity.NOTIFICATION_RECOGNIZER_STARTED, false, true);
	}

	// Button handler for "Stop Provider"
	private void stopProvider(View view) {
		unregisterContentObserver();
		Context context = view.getContext();

		Intent service = new Intent(context, RecognizerService.class);
		mActivity.stopService(service);

		// View neutral classification
		onoffView.setVisibility(View.VISIBLE);
		noneView.setVisibility(View.INVISIBLE);
		walkingView.setVisibility(View.INVISIBLE);
		carView.setVisibility(View.INVISIBLE);
		trainView.setVisibility(View.INVISIBLE);
		idleView.setVisibility(View.INVISIBLE);
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

			// switch not working with JRE under 1.7
			if (newVehicle.equals("car")) {
				carView.setVisibility(View.VISIBLE);
				noneView.setVisibility(View.INVISIBLE);
				walkingView.setVisibility(View.INVISIBLE);
				trainView.setVisibility(View.INVISIBLE);
				idleView.setVisibility(View.INVISIBLE);
			} else if (newVehicle.equals("walking")) {
				walkingView.setVisibility(View.VISIBLE);
				noneView.setVisibility(View.INVISIBLE);
				carView.setVisibility(View.INVISIBLE);
				trainView.setVisibility(View.INVISIBLE);
				idleView.setVisibility(View.INVISIBLE);
			} else if (newVehicle.equals("train")) {
				trainView.setVisibility(View.VISIBLE);
				noneView.setVisibility(View.INVISIBLE);
				carView.setVisibility(View.INVISIBLE);
				walkingView.setVisibility(View.INVISIBLE);
				idleView.setVisibility(View.INVISIBLE);
			} else if (newVehicle.equals("idle")) {
				trainView.setVisibility(View.INVISIBLE);
				noneView.setVisibility(View.INVISIBLE);
				carView.setVisibility(View.INVISIBLE);
				walkingView.setVisibility(View.INVISIBLE);
				idleView.setVisibility(View.VISIBLE);
			} else {
				trainView.setVisibility(View.INVISIBLE);
				noneView.setVisibility(View.VISIBLE);
				carView.setVisibility(View.INVISIBLE);
				walkingView.setVisibility(View.INVISIBLE);
				idleView.setVisibility(View.INVISIBLE);
			}
		}

	}

}

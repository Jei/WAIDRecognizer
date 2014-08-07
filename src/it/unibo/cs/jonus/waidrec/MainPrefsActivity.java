package it.unibo.cs.jonus.waidrec;

import java.util.List;

import android.annotation.TargetApi;
import android.app.NotificationManager;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Build;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.view.MenuItem;
import android.widget.Toast;
import android.support.v4.app.NavUtils;

public class MainPrefsActivity extends PreferenceActivity {

	public static final int RESULT_NO_OP = 0;
	public static final int RESULT_RESET = 1;
	public static final String KEY_REC_SAMPLING_DELAY = "pref_rec_sampling_delay";
	private static final int MAX_SAMPLING_DELAY = 60;
	private static final int MIN_SAMPLING_DELAY = 1;
	public static final String KEY_TRN_START_DELAY = "pref_training_start_delay";
	public static final String KEY_TRN_SAMPLING_DELAY = "pref_training_sampling_delay";
	private static final int MAX_START_DELAY = 60;
	private static final int MIN_START_DELAY = 0;

	NotificationManager mNotificationManager;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		
		// Set the result of this activity
		setResult(RESULT_NO_OP);
	}

	@Override
	public void onBuildHeaders(List<Header> target) {
		loadHeadersFromResource(R.xml.waidrec_preferences_headers, target);
	}

	/**
	 * Set up the {@link android.app.ActionBar}, if the API is available.
	 */
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private void setupActionBar() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			// Show the Up button in the action bar.
			getActionBar().setDisplayHomeAsUpEnabled(true);
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			NavUtils.navigateUpFromSameTask(this);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onHeaderClick(Header header, int position) {
		super.onHeaderClick(header, position);

		if (header.id == R.id.reset_default_model) {
			// TODO add confirmation dialog
			// Return to the parent activity with reset code
			setResult(RESULT_RESET);
			finish();
		}
	}

	@Override
	protected boolean isValidFragment(String fragmentName) {
		if (RecPrefsFragment.class.getName().equals(fragmentName))
			return true;
		if (TrnPrefsFragment.class.getName().equals(fragmentName))
			return true;
		return false;
	}

	public static class RecPrefsFragment extends PreferenceFragment implements
			OnSharedPreferenceChangeListener {
		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);

			addPreferencesFromResource(R.xml.recognizer_preferences);
		}

		@Override
		public void onResume() {
			super.onResume();
			getPreferenceManager().getSharedPreferences()
					.registerOnSharedPreferenceChangeListener(this);
		}

		@Override
		public void onPause() {
			getPreferenceManager().getSharedPreferences()
					.unregisterOnSharedPreferenceChangeListener(this);
			super.onPause();
		}

		@Override
		public void onSharedPreferenceChanged(
				SharedPreferences sharedPreferences, String key) {

			if (key.equals(KEY_REC_SAMPLING_DELAY)) {
				// Get the changed preference
				EditTextPreference preference = (EditTextPreference) findPreference(key);
				String input = sharedPreferences.getString(key, "5");
				int value = Integer.parseInt(input);
				// Check if the input is too high or low
				if (value > MAX_SAMPLING_DELAY) {
					preference.setText("" + MAX_SAMPLING_DELAY);
					Toast.makeText(
							getActivity(),
							" " + getText(R.string.sampling_delay_set_max)
									+ MAX_SAMPLING_DELAY, Toast.LENGTH_SHORT)
							.show();
				} else if (value < MIN_SAMPLING_DELAY) {
					preference.setText("" + MIN_SAMPLING_DELAY);
					Toast.makeText(
							getActivity(),
							" " + getText(R.string.sampling_delay_set_min)
									+ MIN_SAMPLING_DELAY, Toast.LENGTH_SHORT)
							.show();
				}
			}

		}

	}

	public static class TrnPrefsFragment extends PreferenceFragment implements
			OnSharedPreferenceChangeListener {
		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);

			addPreferencesFromResource(R.xml.training_preferences);
		}

		@Override
		public void onResume() {
			super.onResume();
			getPreferenceManager().getSharedPreferences()
					.registerOnSharedPreferenceChangeListener(this);
		}

		@Override
		public void onPause() {
			getPreferenceManager().getSharedPreferences()
					.unregisterOnSharedPreferenceChangeListener(this);
			super.onPause();
		}

		@Override
		public void onSharedPreferenceChanged(
				SharedPreferences sharedPreferences, String key) {

			if (key.equals(KEY_TRN_START_DELAY)) {
				// Get the changed preference
				EditTextPreference preference = (EditTextPreference) findPreference(key);
				String input = sharedPreferences.getString(key, "10");
				int value = Integer.parseInt(input);
				// Check if the input is too high or low
				if (value > MAX_START_DELAY) {
					preference.setText("" + MAX_START_DELAY);
					Toast.makeText(
							getActivity(),
							" " + getText(R.string.start_delay_set_max)
									+ MAX_START_DELAY, Toast.LENGTH_SHORT)
							.show();
				} else if (value < MIN_START_DELAY) {
					preference.setText("" + MIN_START_DELAY);
					Toast.makeText(
							getActivity(),
							" " + getText(R.string.start_delay_set_min)
									+ MIN_START_DELAY, Toast.LENGTH_SHORT)
							.show();
				}
			}
			if (key.equals(KEY_TRN_SAMPLING_DELAY)) {
				// Get the changed preference
				EditTextPreference preference = (EditTextPreference) findPreference(key);
				String input = sharedPreferences.getString(key, "5");
				int value = Integer.parseInt(input);
				// Check if the input is too high or low
				if (value > MAX_SAMPLING_DELAY) {
					preference.setText("" + MAX_SAMPLING_DELAY);
					Toast.makeText(
							getActivity(),
							" " + getText(R.string.sampling_delay_set_max)
									+ MAX_SAMPLING_DELAY, Toast.LENGTH_SHORT)
							.show();
				} else if (value < MIN_SAMPLING_DELAY) {
					preference.setText("" + MIN_SAMPLING_DELAY);
					Toast.makeText(
							getActivity(),
							" " + getText(R.string.sampling_delay_set_min)
									+ MIN_SAMPLING_DELAY, Toast.LENGTH_SHORT)
							.show();
				}
			}

		}

	}

}

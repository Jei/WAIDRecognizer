package it.unibo.cs.jonus.waidrec;

import android.annotation.TargetApi;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Build;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.PreferenceActivity;
import android.view.MenuItem;
import android.widget.Toast;
import android.support.v4.app.NavUtils;

public class RecognizerSettingsActivity extends PreferenceActivity implements
		OnSharedPreferenceChangeListener {
	
	public static final String KEY_REC_SAMPLING_DELAY = "pref_rec_sampling_delay";
	private static final int MAX_SAMPLING_DELAY = 60;
	private static final int MIN_SAMPLING_DELAY = 1;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		addPreferencesFromResource(R.xml.recognizer_preferences);
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
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {

		// Validate the input for history length
		if (key.equals(KEY_REC_SAMPLING_DELAY)) {
			// Get the changed preference
			EditTextPreference preference = (EditTextPreference) findPreference(key);
			String input = sharedPreferences.getString(key, "5");
			int value = Integer.parseInt(input);
			// Check if the input is too high or low
			if (value > MAX_SAMPLING_DELAY) {
				preference.setText("" + MAX_SAMPLING_DELAY);
				Toast.makeText(
						this,
						" " + getText(R.string.sampling_delay_set_max)
								+ MAX_SAMPLING_DELAY, Toast.LENGTH_SHORT)
						.show();
			} else if (value < MIN_SAMPLING_DELAY) {
				preference.setText("" + MIN_SAMPLING_DELAY);
				Toast.makeText(
						this,
						" " + getText(R.string.sampling_delay_set_min)
								+ MIN_SAMPLING_DELAY, Toast.LENGTH_SHORT)
						.show();
			}
		}

	}

}

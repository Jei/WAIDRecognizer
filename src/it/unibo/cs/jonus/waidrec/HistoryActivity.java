package it.unibo.cs.jonus.waidrec;

import java.util.ArrayList;
import java.util.Calendar;

import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.content.Context;
import android.os.Build;
import android.support.v4.app.NavUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;

public class HistoryActivity extends Activity implements
		ActionBar.OnNavigationListener {

	/**
	 * The serialization (saved instance state) Bundle key representing the
	 * current dropdown position.
	 */
	private static final String STATE_SELECTED_NAVIGATION_ITEM = "selected_navigation_item";

	private HistoryManager historyManager;

	private static final String[] historyValues = { "Today", "This Week",
			"This Month", "All Time" };

	private String currentFragmentTag;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_history);

		// Get the list of history files from the history manager
		historyManager = new HistoryManager(this);
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_list_item_1, android.R.id.text1,
				historyValues);

		// Set up the action bar to show a dropdown list.
		final ActionBar actionBar = getActionBar();
		actionBar.setDisplayShowTitleEnabled(false);
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
		// Show the Up button in the action bar.
		actionBar.setDisplayHomeAsUpEnabled(true);

		// Set up the dropdown list navigation in the action bar.
		actionBar.setListNavigationCallbacks(adapter, this);
	}

	/**
	 * Backward-compatible version of {@link ActionBar#getThemedContext()} that
	 * simply returns the {@link android.app.Activity} if
	 * <code>getThemedContext</code> is unavailable.
	 */
	@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
	private Context getActionBarThemedContextCompat() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
			return getActionBar().getThemedContext();
		} else {
			return this;
		}
	}

	@Override
	public void onRestoreInstanceState(Bundle savedInstanceState) {
		// Restore the previously serialized current dropdown position.
		if (savedInstanceState.containsKey(STATE_SELECTED_NAVIGATION_ITEM)) {
			getActionBar().setSelectedNavigationItem(
					savedInstanceState.getInt(STATE_SELECTED_NAVIGATION_ITEM));
		}
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		// Serialize the current dropdown position.
		outState.putInt(STATE_SELECTED_NAVIGATION_ITEM, getActionBar()
				.getSelectedNavigationIndex());
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.history, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			// This ID represents the Home or Up button. In the case of this
			// activity, the Up button is shown. Use NavUtils to allow users
			// to navigate up one level in the application structure. For
			// more details, see the Navigation pattern on Android Design:
			//
			// http://developer.android.com/design/patterns/navigation.html#up-vs-back
			//
			NavUtils.navigateUpFromSameTask(this);
			return true;
		case R.id.action_delete_history_file:
			// Delete the history and reload the fragment
			historyManager.deleteHistory();
			ArrayList<HistoryItem> historyList = (ArrayList<HistoryItem>) historyManager
					.getHistory();
			Fragment currentFragment = getFragmentManager().findFragmentByTag(
					currentFragmentTag);
			Bundle args = new Bundle();
			args.putParcelableArrayList(HistoryGraphFragment.ARG_HISTORY_ITEMS,
					historyList);
			currentFragment.setArguments(args);
			getFragmentManager()
					.beginTransaction()
					.replace(R.id.container, currentFragment,
							currentFragmentTag).commit();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public boolean onNavigationItemSelected(int position, long id) {
		// When the given dropdown item is selected, show its contents in the
		// container view.
		Fragment fragment = new HistoryGraphFragment();
		Bundle args = new Bundle();

		// Get the selected menu item and retrieve a list of items
		ArrayList<HistoryItem> historyList = new ArrayList<HistoryItem>();
		Long endTime = Long.MAX_VALUE;
		Long startTime = null;
		String tag = null;
		switch (position) {
		case 0:
			startTime = getPastDay(0);
			tag = "TODAY";
			break;
		case 1:
			startTime = getPastDay(7);
			tag = "THIS_WEEK";
			break;
		case 2:
			startTime = getPastDay(30);
			tag = "THIS_MONTH";
			break;
		case 3:
			startTime = (long) 0;
			tag = "ALL_TIME";
			break;
		}

		historyList = (ArrayList<HistoryItem>) historyManager.getHistory(
				startTime, endTime);
		Calendar calendar = Calendar.getInstance();
		calendar.setTimeInMillis(startTime);

		// Call the new fragment with the list of history items
		args.putParcelableArrayList(HistoryGraphFragment.ARG_HISTORY_ITEMS,
				historyList);
		fragment.setArguments(args);
		getFragmentManager().beginTransaction()
				.replace(R.id.container, fragment, tag).commit();
		currentFragmentTag = tag;
		return true;
	}

	private long getPastDay(int difference) {
		Calendar calendar = Calendar.getInstance();
		calendar.add(Calendar.DATE, -difference);
		int year = calendar.get(Calendar.YEAR);
		int month = calendar.get(Calendar.MONTH);
		int day = calendar.get(Calendar.DATE);
		calendar.set(year, month, day, 0, 0, 0);
		return calendar.getTime().getTime();
	}
}

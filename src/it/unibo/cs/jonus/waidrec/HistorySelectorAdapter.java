/**
 * 
 */
package it.unibo.cs.jonus.waidrec;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * @author jei
 * 
 */
public class HistorySelectorAdapter extends ArrayAdapter<File> {

	private LayoutInflater inflater;
	private ArrayList<File> items;

	public HistorySelectorAdapter(Context context, ArrayList<File> objects) {
		super(context, R.layout.history_selector_item, objects);

		this.inflater = LayoutInflater.from(context);
		this.items = objects;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		// Create a new view of my layout and inflate it in the row
		convertView = (LinearLayout) inflater.inflate(
				R.layout.history_selector_item, parent, false);

		// Extract file name from path
		File file = getItem(position);
		String fileName = file.getName();

		// Set item's name
		TextView fileNameView = (TextView) convertView
				.findViewById(R.id.historyFileName);
		fileNameView.setText(fileName);

		// Set item's date
		TextView dateView = (TextView) convertView
				.findViewById(R.id.historyFileDate);
		// Get human readable date
		SimpleDateFormat sdfDateTime = new SimpleDateFormat(
				"yyyy-MM-dd HH:mm:ss", Locale.getDefault());
		// FIXME extract timestamp from file name
		String humanDate = sdfDateTime.format(new Date(file.lastModified()));
		dateView.setText(humanDate);

		return convertView;
	}

	@Override
	public View getDropDownView(int position, View convertView, ViewGroup parent) {
		// FIXME different view for dropdown items?
		// Create a new view of my layout and inflate it in the row
		convertView = (LinearLayout) inflater.inflate(
				R.layout.history_selector_item, parent, false);

		// Extract file name from path
		File file = getItem(position);
		String fileName = file.getName();

		// Set item's name
		TextView fileNameView = (TextView) convertView
				.findViewById(R.id.historyFileName);
		fileNameView.setText(fileName);

		// Set item's date
		TextView dateView = (TextView) convertView
				.findViewById(R.id.historyFileDate);
		// Get human readable date
		SimpleDateFormat sdfDateTime = new SimpleDateFormat(
				"yyyy-MM-dd HH:mm:ss", Locale.getDefault());
		// FIXME extract timestamp from file name
		String humanDate = sdfDateTime.format(new Date(file.lastModified()));
		dateView.setText(humanDate);

		return convertView;
	}

	@Override
	public File getItem(int position) {
		return items.get(position);
	}

}

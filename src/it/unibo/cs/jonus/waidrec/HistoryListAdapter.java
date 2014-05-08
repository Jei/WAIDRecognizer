package it.unibo.cs.jonus.waidrec;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

public class HistoryListAdapter extends ArrayAdapter<Evaluation>{
	
    private LayoutInflater inflater;
    private List<Evaluation> items;

	public HistoryListAdapter(Context ctx, ArrayList<Evaluation> objects) {
		super(ctx, R.layout.history_item, objects);
		
		this.inflater = LayoutInflater.from(ctx);
		this.items = objects;
	}
	
	@Override
	public View getView (int position, View convertView, ViewGroup parent) {

		// Create a new view of my layout and inflate it in the row
		convertView = (LinearLayout) inflater.inflate(R.layout.history_item, parent, false);
		
		// Extract evaluation
		Evaluation evaluation = getItem(position);

		// Set item's category
		TextView txtCategory = (TextView) convertView.findViewById(R.id.category);
		txtCategory.setText(evaluation.getCategory());

		// Set item's date
		TextView txtDate = (TextView) convertView.findViewById(R.id.date);
		// Get human readable date
		SimpleDateFormat sdfDateTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
		String humanDate =  sdfDateTime.format(new Date(evaluation.getTimestamp()));
		txtDate.setText(humanDate);
		
		return convertView;
		
    }
	
	@Override
	public Evaluation getItem(int position) {
        return items.get(position);
    }

}

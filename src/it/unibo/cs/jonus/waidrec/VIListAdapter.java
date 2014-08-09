package it.unibo.cs.jonus.waidrec;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.RelativeLayout;

public class VIListAdapter extends ArrayAdapter<Bitmap> {
	private LayoutInflater inflater;
	private List<Bitmap> icons;

	public VIListAdapter(Context ctx, ArrayList<Bitmap> objects) {
		super(ctx, R.layout.vehicles_list_item, R.id.icon_name, objects);

		this.inflater = LayoutInflater.from(ctx);
		this.icons = objects;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		// Create a new view of my layout and inflate it in the row
		convertView = (RelativeLayout) inflater.inflate(R.layout.vi_list_item,
				parent, false);

		// Set the corresponding icon
		Bitmap icon = getItem(position);
		ImageView image = (ImageView) convertView.findViewById(R.id.vi_view);
		image.setImageBitmap(icon);

		return convertView;
	}

	@Override
	public View getDropDownView(int position, View convertView, ViewGroup parent) {
		// Create a new view of my layout and inflate it in the row
		convertView = (RelativeLayout) inflater.inflate(R.layout.vi_list_item,
				parent, false);

		// Set the corresponding icon
		Bitmap icon = getItem(position);
		ImageView image = (ImageView) convertView.findViewById(R.id.vi_view);
		image.setImageBitmap(icon);

		return convertView;
	}

	@Override
	public Bitmap getItem(int position) {
		return icons.get(position);
	}
}

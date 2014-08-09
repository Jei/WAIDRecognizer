package it.unibo.cs.jonus.waidrec;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class VehiclesListAdapter extends ArrayAdapter<VehicleItem> {

	private LayoutInflater inflater;
	private List<VehicleItem> items;

	public VehiclesListAdapter(Context ctx, ArrayList<VehicleItem> objects) {
		super(ctx, R.layout.vehicles_list_item, objects);

		this.inflater = LayoutInflater.from(ctx);
		this.items = objects;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {

		// Create a new view of my layout and inflate it in the row
		convertView = (RelativeLayout) inflater.inflate(
				R.layout.vehicles_list_item, parent, false);

		// Extract evaluation
		VehicleItem vehicle = getItem(position);
		String vehicleName = vehicle.getCategory();

		// Set item's name
		TextView txtName = (TextView) convertView
				.findViewById(R.id.vehicle_name);
		txtName.setText(vehicleName);

		// Get number of instances for this vehicle
		Uri uri = Uri.parse(EvaluationsProvider.TRAINING_DATA_URI
				+ EvaluationsProvider.PATH_VEHICLE_TRAINING_DATA);
		Cursor cursor = getContext().getContentResolver().query(uri,
				MainActivity.allColumnsProjection,
				DatabaseOpenHelper.COLUMN_CATEGORY + " =?",
				new String[] { vehicleName }, null);
		ArrayList<VehicleInstance> instances = EvaluationsProvider
				.cursorToVehicleInstanceArray(cursor);
		cursor.close();
		int instancesCount = instances.size();
		TextView txtInstances = (TextView) convertView
				.findViewById(R.id.vehicle_instances_number);
		txtInstances.setText(instancesCount
				+ " "
				+ getContext().getResources().getString(
						R.string.instances_suffix));

		// Create remove vehicle button
		Button deleteButton = (Button) convertView
				.findViewById(R.id.remove_vehicle_button);
		deleteButton.setTag(vehicle);
		deleteButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// Delete this vehicle from the database
				// TODO add confirmation dialog
				VehicleItem selectedItem = (VehicleItem) v.getTag();
				String vehicleName = selectedItem.getCategory();
				Uri uri = Uri.parse(EvaluationsProvider.VEHICLES_URI
						+ EvaluationsProvider.PATH_REMOVE_VEHICLE);
				int count = getContext().getContentResolver().delete(uri,
						DatabaseOpenHelper.COLUMN_CATEGORY + " =?",
						new String[] { vehicleName });

				if (count > 0) {
					uri = Uri.parse(EvaluationsProvider.TRAINING_DATA_URI
							+ EvaluationsProvider.PATH_DELETE_TRAINING_VEHICLE);
					getContext().getContentResolver().delete(uri,
							DatabaseOpenHelper.COLUMN_CATEGORY + " =?",
							new String[] { vehicleName });

					remove(selectedItem);
					notifyDataSetChanged();
				} else {
					// TODO error?
				}
			}
		});

		return convertView;

	}

	@Override
	public VehicleItem getItem(int position) {
		return items.get(position);
	}

}

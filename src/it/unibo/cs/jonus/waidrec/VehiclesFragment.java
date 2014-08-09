package it.unibo.cs.jonus.waidrec;

import it.unibo.cs.jonus.waidrec.AddVehicleDialog.VehicleDialogListener;

import java.util.ArrayList;

import android.app.DialogFragment;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ListView;

public class VehiclesFragment extends PreferenceFragment {

	private ArrayList<VehicleItem> mItems = new ArrayList<VehicleItem>();
	private VehiclesListAdapter mAdapter;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_vehicles, container,
				false);

		return rootView;
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		// super.onViewCreated(view, savedInstanceState);

		ListView vehiclesListView = (ListView) view
				.findViewById(R.id.vehicles_listview);
		// Get the list of VehicleItem from the db
		Uri uri = Uri.parse(EvaluationsProvider.VEHICLES_URI
				+ EvaluationsProvider.PATH_ALL_VEHICLES);
		Cursor cursor = getActivity().getContentResolver().query(uri,
				MainActivity.vehicleColumnsProjection, null, null, null);
		mItems.addAll(EvaluationsProvider.cursorToVehicleItemArray(cursor));
		cursor.close();
		mAdapter = new VehiclesListAdapter(getActivity(), mItems);
		vehiclesListView.setAdapter(mAdapter);

		// Set the listeners for the buttons
		Button addVehicleBtn = (Button) view
				.findViewById(R.id.add_vehicle_button);
		addVehicleBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				AddVehicleDialog dialog = new AddVehicleDialog();
				dialog.registerListener(new VehicleDialogListener() {

					@Override
					public void onDialogPositiveClick(DialogFragment dialog) {
						// Refresh list of vehicles
						Uri uri = Uri.parse(EvaluationsProvider.VEHICLES_URI
								+ EvaluationsProvider.PATH_ALL_VEHICLES);
						Cursor cursor = getActivity().getContentResolver()
								.query(uri,
										MainActivity.vehicleColumnsProjection,
										null, null, null);
						mItems.clear();
						mItems.addAll(EvaluationsProvider
								.cursorToVehicleItemArray(cursor));
						cursor.close();
						mAdapter.notifyDataSetChanged();
					}
				});
				dialog.show(getFragmentManager(), null);
			}
		});

	}

	@Override
	public void onResume() {
		super.onResume();
	}

	@Override
	public void onPause() {
		super.onPause();
	}

}

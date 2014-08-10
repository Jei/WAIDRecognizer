package it.unibo.cs.jonus.waidrec;

import java.util.ArrayList;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Spinner;

public class AddVehicleDialog extends DialogFragment {
	private ArrayList<Bitmap> mIcons = new ArrayList<Bitmap>();
	private Spinner viSpinner;
	private EditText nameEditText;
	private static int[] sIconsRes = { R.drawable.idle_00b0b0,
			R.drawable.walking_00b0b0, R.drawable.car_00b0b0,
			R.drawable.train_00b0b0, R.drawable.bicycle_00b0b0,
			R.drawable.bus_00b0b0, R.drawable.boat_00b0b0,
			R.drawable.plane_00b0b0, R.drawable.running_00b0b0 };

	public interface VehicleDialogListener {
		public void onDialogPositiveClick(DialogFragment dialog);
	}

	VehicleDialogListener mListener;

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

		LayoutInflater inflater = getActivity().getLayoutInflater();
		View layout = inflater.inflate(R.layout.dialog_add_vehicle, null);

		// Get list of icons
		for (int res : sIconsRes) {
			Bitmap icon = lessResolution(getResources(), res, 100, 100);
			mIcons.add(icon);
		}
		// Build elements
		viSpinner = (Spinner) layout.findViewById(R.id.icons_list);
		viSpinner.setAdapter(new VIListAdapter(getActivity(), mIcons));
		nameEditText = (EditText) layout.findViewById(R.id.vehicle_name);

		// Build the dialog
		builder.setView(layout);
		builder.setPositiveButton(android.R.string.ok,
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						// Get the name and icon
						String name = nameEditText.getText().toString();
						String test = name.replaceAll("\\s+", "");
						if (!test.equals("")) {
							// TODO insert full icon instead of scaled one
							Bitmap icon = (Bitmap) viSpinner.getSelectedItem();
							// Add the vehicle to the database
							VehicleItem item = new VehicleItem();
							item.setCategory(name);
							item.setIcon(icon);
							Uri uri = Uri
									.parse(EvaluationsProvider.VEHICLES_URI
											+ EvaluationsProvider.PATH_ADD_VEHICLE);
							ContentValues values = EvaluationsProvider
									.vehicleItemToContentValues(item);
							getActivity().getContentResolver().insert(uri,
									values);

							if (mListener != null) {
								mListener
										.onDialogPositiveClick(AddVehicleDialog.this);
							}

							dialog.dismiss();
						}
					}
				});
		builder.setNegativeButton(android.R.string.cancel,
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
					}
				});

		return builder.create();
	}

	public void registerListener(VehicleDialogListener listener) {
		mListener = listener;
	}

	public void unregisterListener() {
		mListener = null;
	}

	public static Bitmap lessResolution(Resources res, int resourceId,
			int width, int height) {
		int reqHeight = width;
		int reqWidth = height;
		BitmapFactory.Options options = new BitmapFactory.Options();

		// First decode with inJustDecodeBounds=true to check dimensions
		options.inJustDecodeBounds = true;
		BitmapFactory.decodeResource(res, resourceId, options);

		// Calculate inSampleSize
		options.inSampleSize = calculateInSampleSize(options, reqWidth,
				reqHeight);

		// Decode bitmap with inSampleSize set
		options.inJustDecodeBounds = false;

		return BitmapFactory.decodeResource(res, resourceId, options);
	}

	private static int calculateInSampleSize(BitmapFactory.Options options,
			int reqWidth, int reqHeight) {

		final int height = options.outHeight;
		final int width = options.outWidth;
		int inSampleSize = 1;

		if (height > reqHeight || width > reqWidth) {

			// Calculate ratios of height and width to requested height and
			// width
			final int heightRatio = Math.round((float) height
					/ (float) reqHeight);
			final int widthRatio = Math.round((float) width / (float) reqWidth);

			// Choose the smallest ratio as inSampleSize value, this will
			// guarantee
			// a final image with both dimensions larger than or equal to the
			// requested height and width.
			inSampleSize = heightRatio < widthRatio ? heightRatio : widthRatio;
		}
		return inSampleSize;
	}
}

package it.unibo.cs.jonus.waidrec;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;

public class EvaluationsProvider extends ContentProvider {

	private DatabaseOpenHelper database;
	private static final int EVALUATIONS = 1;
	private static final int LAST_EVALUATION = 2;
	private static final int ALL_EVALUATIONS = 3;
	private static final int SOME_EVALUATIONS = 4;
	private static final int ERASE_EVALUATIONS = 666;
	private static final int INSERT_TRAINING_ITEM = 11;
	private static final int ALL_TRAINING_DATA = 13;
	private static final int DELETE_TRAINING_VEHICLE = 14;
	private static final int VEHICLE_TRAINING_DATA = 12;
	private static final int ERASE_TRAINING_DATA = 999;
	private static final int ALL_VEHICLES = 23;
	private static final int ADD_VEHICLE = 21;
	private static final int REMOVE_VEHICLE = 24;
	private static final int ERASE_VEHICLES = 333;
	private static final String AUTHORITY = "it.unibo.cs.jonus.waidrec.evaluationsprovider";
	private static final String BASE_PATH = "evaluations";
	private static final String TRAINING_PATH = "training_data";
	private static final String VEHICLES_PATH = "vehicles";
	public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY
			+ "/" + BASE_PATH);
	public static final Uri TRAINING_DATA_URI = Uri.parse("content://"
			+ AUTHORITY + "/" + TRAINING_PATH);
	public static final Uri VEHICLES_URI = Uri.parse("content://" + AUTHORITY
			+ "/" + VEHICLES_PATH);
	public static final String PATH_LAST_EVALUATION = "/last";
	public static final String PATH_ALL_EVALUATIONS = "/all";
	public static final String PATH_SOME_EVALUATIONS = "/some";
	public static final String PATH_ERASE_EVALUATIONS = "/erase";
	public static final String PATH_INSERT_TRAINING_ITEM = "/insert";
	public static final String PATH_ALL_TRAINING_DATA = "/all";
	public static final String PATH_VEHICLE_TRAINING_DATA = "/vehicle";
	public static final String PATH_ERASE_TRAINING_DATA = "/erase";
	public static final String PATH_DELETE_TRAINING_VEHICLE = "/delete_vehicle";
	public static final String PATH_ALL_VEHICLES = "/all";
	public static final String PATH_REMOVE_VEHICLE = "/remove";
	public static final String PATH_ERASE_VEHICLES = "/erase";
	public static final String PATH_ADD_VEHICLE = "/add";
	private static final int MAX_EVALUATIONS = 86400;

	private static final UriMatcher sUriMatcher = new UriMatcher(
			UriMatcher.NO_MATCH);
	static {
		sUriMatcher.addURI(AUTHORITY, BASE_PATH, EVALUATIONS);
		sUriMatcher.addURI(AUTHORITY, BASE_PATH + PATH_LAST_EVALUATION,
				LAST_EVALUATION);
		sUriMatcher.addURI(AUTHORITY, BASE_PATH + PATH_ALL_EVALUATIONS,
				ALL_EVALUATIONS);
		sUriMatcher.addURI(AUTHORITY, BASE_PATH + PATH_SOME_EVALUATIONS,
				SOME_EVALUATIONS);
		sUriMatcher.addURI(AUTHORITY, BASE_PATH + PATH_ERASE_EVALUATIONS,
				ERASE_EVALUATIONS);
		sUriMatcher
				.addURI(AUTHORITY, TRAINING_PATH + PATH_INSERT_TRAINING_ITEM,
						INSERT_TRAINING_ITEM);
		sUriMatcher.addURI(AUTHORITY, TRAINING_PATH + PATH_ALL_TRAINING_DATA,
				ALL_TRAINING_DATA);
		sUriMatcher.addURI(AUTHORITY, TRAINING_PATH + PATH_VEHICLE_TRAINING_DATA,
				VEHICLE_TRAINING_DATA);
		sUriMatcher.addURI(AUTHORITY, TRAINING_PATH + PATH_ERASE_TRAINING_DATA,
				ERASE_TRAINING_DATA);
		sUriMatcher.addURI(AUTHORITY, TRAINING_PATH
				+ PATH_DELETE_TRAINING_VEHICLE, DELETE_TRAINING_VEHICLE);
		sUriMatcher.addURI(AUTHORITY, VEHICLES_PATH + PATH_ALL_VEHICLES,
				ALL_VEHICLES);
		sUriMatcher.addURI(AUTHORITY, VEHICLES_PATH + PATH_ADD_VEHICLE,
				ADD_VEHICLE);
		sUriMatcher.addURI(AUTHORITY, VEHICLES_PATH + PATH_REMOVE_VEHICLE,
				REMOVE_VEHICLE);
		sUriMatcher.addURI(AUTHORITY, VEHICLES_PATH + PATH_ERASE_VEHICLES,
				ERASE_VEHICLES);
	}

	public EvaluationsProvider() {
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		int uriType = sUriMatcher.match(uri);
		int count = 0;
		SQLiteDatabase db = database.getWritableDatabase();
		switch (uriType) {
		case ERASE_EVALUATIONS:
			// discard selection values, add sorting
			selection = null;
			count = db.delete(DatabaseOpenHelper.TABLE_EVALUATIONS, "1", null);
			break;
		case ERASE_TRAINING_DATA:
			count = db
					.delete(DatabaseOpenHelper.TABLE_TRAINING_DATA, "1", null);
			break;
		case DELETE_TRAINING_VEHICLE:
			count = db.delete(DatabaseOpenHelper.TABLE_TRAINING_DATA,
					DatabaseOpenHelper.COLUMN_CATEGORY + "=?", selectionArgs);
			break;
		case REMOVE_VEHICLE:
			count = db.delete(DatabaseOpenHelper.TABLE_VEHICLES,
					DatabaseOpenHelper.COLUMN_CATEGORY + "=?", selectionArgs);
			break;
		case ERASE_VEHICLES:
			count = db.delete(DatabaseOpenHelper.TABLE_VEHICLES, "1", null);
			break;
		default:
			throw new IllegalArgumentException("Unknown URI: " + uri);
		}

		return count;
	}

	@Override
	public String getType(Uri uri) {
		// TODO: Implement this to handle requests for the MIME type of the data
		// at the given URI.
		throw new UnsupportedOperationException("Not yet implemented");
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		int uriType = sUriMatcher.match(uri);
		SQLiteDatabase db = database.getWritableDatabase();
		long id = 0;
		Uri newUri = null;
		switch (uriType) {
		case EVALUATIONS:
			id = db.insert(DatabaseOpenHelper.TABLE_EVALUATIONS, null, values);

			// Add rows limit to table
			String table = DatabaseOpenHelper.TABLE_EVALUATIONS;
			String idColumn = DatabaseOpenHelper.COLUMN_ID;
			String timestampColumn = DatabaseOpenHelper.COLUMN_TIMESTAMP;
			String query = idColumn + " NOT IN (SELECT " + idColumn + " from "
					+ table + " ORDER BY " + timestampColumn + " DESC LIMIT "
					+ MAX_EVALUATIONS + ")";
			db.delete(DatabaseOpenHelper.TABLE_EVALUATIONS, query, null);

			newUri = Uri.parse(BASE_PATH + "/" + id);
			break;
		case INSERT_TRAINING_ITEM:
			id = db.insert(DatabaseOpenHelper.TABLE_TRAINING_DATA, null, values);

			newUri = Uri.parse(TRAINING_PATH + "/" + id);
			break;
		case ADD_VEHICLE:
			id = db.insert(DatabaseOpenHelper.TABLE_VEHICLES, null, values);

			newUri = Uri.parse(VEHICLES_PATH + "/" + id);
			break;
		default:
			throw new IllegalArgumentException("Unknown URI: " + uri);
		}
		getContext().getContentResolver().notifyChange(uri, null);

		return newUri;
	}

	@Override
	public boolean onCreate() {
		// Open database
		database = new DatabaseOpenHelper(getContext());

		return false;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {

		SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();

		checkColumns(projection);

		int uriType = sUriMatcher.match(uri);
		String limit = null;
		switch (uriType) {
		case ALL_EVALUATIONS:
			// discard selection values, add sorting
			selection = null;
			sortOrder = "_id DESC";
			queryBuilder.setTables(DatabaseOpenHelper.TABLE_EVALUATIONS);
			break;
		case SOME_EVALUATIONS:
			// add sorting and limit
			if (sortOrder != null) {
				sortOrder = "_id DESC " + sortOrder;
			} else {
				sortOrder = "_id DESC";
			}
			queryBuilder.setTables(DatabaseOpenHelper.TABLE_EVALUATIONS);
			break;
		case LAST_EVALUATION:
			// discard selection, add limit and sorting to get last evaluation
			selection = null;
			sortOrder = "_id DESC";
			queryBuilder.setTables(DatabaseOpenHelper.TABLE_EVALUATIONS);
			limit = "1";
			break;
		case ALL_TRAINING_DATA:
			// discard selection values, add sorting
			selection = null;
			sortOrder = "_id DESC";
			queryBuilder.setTables(DatabaseOpenHelper.TABLE_TRAINING_DATA);
			break;
		case VEHICLE_TRAINING_DATA:
			// add sorting
			sortOrder = "_id DESC";
			queryBuilder.setTables(DatabaseOpenHelper.TABLE_TRAINING_DATA);
			break;
		case ALL_VEHICLES:
			// discard selection values, add sorting
			selection = null;
			sortOrder = "_id DESC";
			queryBuilder.setTables(DatabaseOpenHelper.TABLE_VEHICLES);
			break;
		default:
			throw new IllegalArgumentException("Unknown URI: " + uri);
		}

		SQLiteDatabase db = database.getWritableDatabase();
		Cursor cursor = queryBuilder.query(db, projection, selection,
				selectionArgs, null, null, sortOrder, limit);
		cursor.setNotificationUri(getContext().getContentResolver(), uri);

		return cursor;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		// TODO: Implement this to handle requests to update one or more rows.
		throw new UnsupportedOperationException("Not yet implemented");
	}

	private void checkColumns(String[] projection) {
		String[] available = { DatabaseOpenHelper.COLUMN_ID,
				DatabaseOpenHelper.COLUMN_TIMESTAMP,
				DatabaseOpenHelper.COLUMN_CATEGORY,
				DatabaseOpenHelper.COLUMN_AVGA, DatabaseOpenHelper.COLUMN_MINA,
				DatabaseOpenHelper.COLUMN_MAXA, DatabaseOpenHelper.COLUMN_STDA,
				DatabaseOpenHelper.COLUMN_AVGG, DatabaseOpenHelper.COLUMN_MING,
				DatabaseOpenHelper.COLUMN_MAXG, DatabaseOpenHelper.COLUMN_STDG,
				DatabaseOpenHelper.COLUMN_ICON };
		if (projection != null) {
			HashSet<String> requestedColumns = new HashSet<String>(
					Arrays.asList(projection));
			HashSet<String> availableColumns = new HashSet<String>(
					Arrays.asList(available));
			// Check if all columns which are requested are available
			if (!availableColumns.containsAll(requestedColumns)) {
				throw new IllegalArgumentException(
						"Unknown columns in projection");
			}
		}
	}

	/**
	 * Create a VehicleInstance from a Cursor from EvaluationsProvider
	 * 
	 * @param cursor
	 *            the Cursor to convert
	 * @return a VehicleInstance with the values from the Cursor
	 */
	public static VehicleInstance cursorToVehicleInstance(Cursor cursor) {
		VehicleInstance instance = new VehicleInstance();
		MagnitudeFeatures accelFeatures = new MagnitudeFeatures();
		MagnitudeFeatures gyroFeatures = new MagnitudeFeatures();

		String category = cursor.getString(cursor
				.getColumnIndexOrThrow(DatabaseOpenHelper.COLUMN_CATEGORY));
		long timestamp = cursor.getLong(cursor
				.getColumnIndexOrThrow(DatabaseOpenHelper.COLUMN_TIMESTAMP));
		accelFeatures.setAverage(cursor.getDouble(cursor
				.getColumnIndexOrThrow(DatabaseOpenHelper.COLUMN_AVGA)));
		accelFeatures.setMinimum(cursor.getDouble(cursor
				.getColumnIndexOrThrow(DatabaseOpenHelper.COLUMN_MINA)));
		accelFeatures.setMaximum(cursor.getDouble(cursor
				.getColumnIndexOrThrow(DatabaseOpenHelper.COLUMN_MAXA)));
		accelFeatures.setStandardDeviation(cursor.getDouble(cursor
				.getColumnIndexOrThrow(DatabaseOpenHelper.COLUMN_STDA)));
		gyroFeatures.setAverage(cursor.getDouble(cursor
				.getColumnIndexOrThrow(DatabaseOpenHelper.COLUMN_AVGG)));
		gyroFeatures.setMinimum(cursor.getDouble(cursor
				.getColumnIndexOrThrow(DatabaseOpenHelper.COLUMN_MING)));
		gyroFeatures.setMaximum(cursor.getDouble(cursor
				.getColumnIndexOrThrow(DatabaseOpenHelper.COLUMN_MAXG)));
		gyroFeatures.setStandardDeviation(cursor.getDouble(cursor
				.getColumnIndexOrThrow(DatabaseOpenHelper.COLUMN_STDG)));

		instance.setCategory(category);
		instance.setTimestamp(timestamp);
		instance.setAccelFeatures(accelFeatures);
		instance.setGyroFeatures(gyroFeatures);

		return instance;
	}

	/**
	 * Create a VehicleInstance array from a Cursor from EvaluationsProvider
	 * 
	 * @param cursor
	 *            the Cursor to convert
	 * @return an ArrayList of VehicleInstance with the rows from the Cursor
	 */
	public static ArrayList<VehicleInstance> cursorToVehicleInstanceArray(
			Cursor cursor) {
		ArrayList<VehicleInstance> instanceArray = new ArrayList<VehicleInstance>();
		String[] columns = cursor.getColumnNames();

		VehicleInstance instance;
		cursor.moveToFirst();
		while (!cursor.isAfterLast()) {
			instance = new VehicleInstance();
			MagnitudeFeatures accelFeatures = new MagnitudeFeatures();
			MagnitudeFeatures gyroFeatures = new MagnitudeFeatures();

			for (String column : columns) {
				if (column.equals(DatabaseOpenHelper.COLUMN_CATEGORY)) {
					instance.setCategory(cursor.getString(cursor
							.getColumnIndex(column)));
				}
				if (column.equals(DatabaseOpenHelper.COLUMN_TIMESTAMP)) {
					instance.setTimestamp(cursor.getLong(cursor
							.getColumnIndex(column)));
				}
				if (column.equals(DatabaseOpenHelper.COLUMN_AVGA)) {
					accelFeatures.setAverage(cursor.getDouble(cursor
							.getColumnIndex(column)));
				}
				if (column.equals(DatabaseOpenHelper.COLUMN_MINA)) {
					accelFeatures.setMinimum(cursor.getDouble(cursor
							.getColumnIndex(column)));
				}
				if (column.equals(DatabaseOpenHelper.COLUMN_MAXA)) {
					accelFeatures.setMaximum(cursor.getDouble(cursor
							.getColumnIndex(column)));
				}
				if (column.equals(DatabaseOpenHelper.COLUMN_STDA)) {
					accelFeatures.setStandardDeviation(cursor.getDouble(cursor
							.getColumnIndex(column)));
				}
				if (column.equals(DatabaseOpenHelper.COLUMN_AVGG)) {
					gyroFeatures.setAverage(cursor.getDouble(cursor
							.getColumnIndex(column)));
				}
				if (column.equals(DatabaseOpenHelper.COLUMN_MING)) {
					gyroFeatures.setMinimum(cursor.getDouble(cursor
							.getColumnIndex(column)));
				}
				if (column.equals(DatabaseOpenHelper.COLUMN_MAXG)) {
					gyroFeatures.setMaximum(cursor.getDouble(cursor
							.getColumnIndex(column)));
				}
				if (column.equals(DatabaseOpenHelper.COLUMN_STDG)) {
					gyroFeatures.setStandardDeviation(cursor.getDouble(cursor
							.getColumnIndex(column)));
				}
			}

			instance.setAccelFeatures(accelFeatures);
			instance.setGyroFeatures(gyroFeatures);
			instanceArray.add(instance);

			cursor.moveToNext();
		}

		return instanceArray;
	}

	/**
	 * Create a VehicleItem from a Cursor from EvaluationsProvider
	 * 
	 * @param cursor
	 *            the Cursor to convert
	 * @return a VehicleItem with the values from the Cursor
	 */
	public static VehicleItem cursorToVehicleItem(Cursor cursor) {
		VehicleItem item = new VehicleItem();

		String category = cursor.getString(cursor
				.getColumnIndexOrThrow(DatabaseOpenHelper.COLUMN_CATEGORY));
		byte[] iconByteArray = cursor.getBlob(cursor
				.getColumnIndexOrThrow(DatabaseOpenHelper.COLUMN_ICON));
		Bitmap icon = BitmapFactory.decodeByteArray(iconByteArray, 0,
				iconByteArray.length);

		item.setCategory(category);
		item.setIcon(icon);

		return item;
	}

	/**
	 * Create a VehicleItem array from a Cursor from EvaluationsProvider
	 * 
	 * @param cursor
	 *            the Cursor to convert
	 * @return an ArrayList of VehicleItem with the rows from the Cursor
	 */
	public static ArrayList<VehicleItem> cursorToVehicleItemArray(Cursor cursor) {
		ArrayList<VehicleItem> itemArray = new ArrayList<VehicleItem>();

		VehicleItem item;
		cursor.moveToFirst();
		while (!cursor.isAfterLast()) {
			item = new VehicleItem();

			String category = cursor.getString(cursor
					.getColumnIndexOrThrow(DatabaseOpenHelper.COLUMN_CATEGORY));
			byte[] iconByteArray = cursor.getBlob(cursor
					.getColumnIndexOrThrow(DatabaseOpenHelper.COLUMN_ICON));
			Bitmap icon = BitmapFactory.decodeByteArray(iconByteArray, 0,
					iconByteArray.length);

			item.setCategory(category);
			item.setIcon(icon);

			itemArray.add(item);

			cursor.moveToNext();
		}

		return itemArray;
	}

	/**
	 * Create ContentValues from a VehicleInstance
	 * 
	 * @param instance
	 *            the VehicleInstance to convert to ContentValues
	 * @return ContentValues to use with this EvaluationsProvider
	 */
	public static ContentValues vehicleInstanceToContentValues(
			VehicleInstance instance) {
		ContentValues values = new ContentValues();

		long timestamp = instance.getTimestamp();
		String category = instance.getCategory();
		MagnitudeFeatures accelFeatures = instance.getAccelFeatures();
		MagnitudeFeatures gyroFeatures = instance.getGyroFeatures();

		values.put(DatabaseOpenHelper.COLUMN_TIMESTAMP, timestamp);
		values.put(DatabaseOpenHelper.COLUMN_CATEGORY, category);
		values.put(DatabaseOpenHelper.COLUMN_AVGA, accelFeatures.getAverage());
		values.put(DatabaseOpenHelper.COLUMN_MINA, accelFeatures.getMinimum());
		values.put(DatabaseOpenHelper.COLUMN_MAXA, accelFeatures.getMaximum());
		values.put(DatabaseOpenHelper.COLUMN_STDA,
				accelFeatures.getStandardDeviation());
		values.put(DatabaseOpenHelper.COLUMN_AVGG, gyroFeatures.getAverage());
		values.put(DatabaseOpenHelper.COLUMN_MING, gyroFeatures.getMinimum());
		values.put(DatabaseOpenHelper.COLUMN_MAXG, gyroFeatures.getMaximum());
		values.put(DatabaseOpenHelper.COLUMN_STDG,
				gyroFeatures.getStandardDeviation());

		return values;
	}
	
	/**
	 * Create ContentValues from a VehicleItem
	 * 
	 * @param instance
	 *            the VehicleItem to convert to ContentValues
	 * @return ContentValues to use with this EvaluationsProvider
	 */
	public static ContentValues vehicleItemToContentValues(
			VehicleItem item) {
		ContentValues values = new ContentValues();

		String category = item.getCategory();
		Bitmap icon = item.getIcon();
		// Convert the icon to a byte array
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		icon.compress(Bitmap.CompressFormat.PNG, 100, stream);
		byte[] byteArray = stream.toByteArray();

		values.put(DatabaseOpenHelper.COLUMN_CATEGORY, category);
		values.put(DatabaseOpenHelper.COLUMN_ICON, byteArray);

		return values;
	}

}

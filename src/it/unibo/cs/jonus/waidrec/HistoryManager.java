/**
 * 
 */
package it.unibo.cs.jonus.waidrec;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

/**
 * @author jei
 * 
 */
public class HistoryManager {

	private static final long MAX_HISTORY_SIZE = 86400;

	private static final String[] allColumns = { DatabaseOpenHelper.COLUMN_ID,
			DatabaseOpenHelper.COLUMN_TIMESTAMP,
			DatabaseOpenHelper.COLUMN_CATEGORY,
			DatabaseOpenHelper.COLUMN_HISTORY_AVGA,
			DatabaseOpenHelper.COLUMN_HISTORY_MINA,
			DatabaseOpenHelper.COLUMN_HISTORY_MAXA,
			DatabaseOpenHelper.COLUMN_HISTORY_STDA,
			DatabaseOpenHelper.COLUMN_HISTORY_AVGG,
			DatabaseOpenHelper.COLUMN_HISTORY_MING,
			DatabaseOpenHelper.COLUMN_HISTORY_MAXG,
			DatabaseOpenHelper.COLUMN_HISTORY_STDG };

	private File filesDir;
	private DatabaseOpenHelper databaseOH;

	public HistoryManager(Context context) {
		this.filesDir = context.getFilesDir();
		databaseOH = new DatabaseOpenHelper(context);
	}

	/**
	 * Writes a new history item in the database
	 * 
	 * @param the
	 *            history item to write
	 */
	public void writeHistoryItem(HistoryItem item) {
		// Create a new ContentValues object with the item
		ContentValues values = new ContentValues();
		values.put(DatabaseOpenHelper.COLUMN_TIMESTAMP, item.getTimestamp());
		values.put(DatabaseOpenHelper.COLUMN_CATEGORY, item.getCategory());
		MagnitudeFeatures accelFeatures = item.getAccelFeatures();
		MagnitudeFeatures gyroFeatures = item.getGyroFeatures();
		if (accelFeatures != null) {
			values.put(DatabaseOpenHelper.COLUMN_HISTORY_AVGA, item
					.getAccelFeatures().getAverage());
			values.put(DatabaseOpenHelper.COLUMN_HISTORY_MINA, item
					.getAccelFeatures().getMinimum());
			values.put(DatabaseOpenHelper.COLUMN_HISTORY_MAXA, item
					.getAccelFeatures().getMaximum());
			values.put(DatabaseOpenHelper.COLUMN_HISTORY_STDA, item
					.getAccelFeatures().getStandardDeviation());
		} else {
			values.put(DatabaseOpenHelper.COLUMN_HISTORY_AVGA, (Double) null);
			values.put(DatabaseOpenHelper.COLUMN_HISTORY_MINA, (Double) null);
			values.put(DatabaseOpenHelper.COLUMN_HISTORY_MAXA, (Double) null);
			values.put(DatabaseOpenHelper.COLUMN_HISTORY_STDA, (Double) null);
		}
		if (gyroFeatures != null) {
			values.put(DatabaseOpenHelper.COLUMN_HISTORY_AVGG, item
					.getGyroFeatures().getAverage());
			values.put(DatabaseOpenHelper.COLUMN_HISTORY_MING, item
					.getGyroFeatures().getMinimum());
			values.put(DatabaseOpenHelper.COLUMN_HISTORY_MAXG, item
					.getGyroFeatures().getMaximum());
			values.put(DatabaseOpenHelper.COLUMN_HISTORY_STDG, item
					.getGyroFeatures().getStandardDeviation());
		} else {
			values.put(DatabaseOpenHelper.COLUMN_HISTORY_AVGG, (Double) null);
			values.put(DatabaseOpenHelper.COLUMN_HISTORY_MING, (Double) null);
			values.put(DatabaseOpenHelper.COLUMN_HISTORY_MAXG, (Double) null);
			values.put(DatabaseOpenHelper.COLUMN_HISTORY_STDG, (Double) null);
		}

		// Insert the ContentValues in the history table
		SQLiteDatabase db = databaseOH.getWritableDatabase();
		db.insert(DatabaseOpenHelper.TABLE_HISTORY, null, values);

		// Limit the total number of rows in the table
		String history = DatabaseOpenHelper.TABLE_HISTORY;
		String id = DatabaseOpenHelper.COLUMN_ID;
		String timestamp = DatabaseOpenHelper.COLUMN_TIMESTAMP;
		String deleteQuery = "DELETE FROM " + history + " where " + id
				+ " NOT IN (SELECT " + id + " from " + history + " ORDER BY "
				+ timestamp + " DESC LIMIT " + MAX_HISTORY_SIZE + ")";
		db.rawQuery(deleteQuery, null);
	}

	/**
	 * Returns a list of the JSON history files
	 * 
	 * @return an ArrayList of File objects
	 */
	public ArrayList<File> getFilesList() {
		File[] filesArray = filesDir.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File pathName, String fileName) {
				return fileName.endsWith(".json");
			}
		});

		ArrayList<File> filesList = new ArrayList<File>(
				Arrays.asList(filesArray));

		return filesList;
	}

	/**
	 * Get the JSON array from the specified JSON file
	 * 
	 * @param the
	 *            File object to read
	 * @return a JSONArray of JSONObjects
	 * @throws FileNotFoundException
	 * @throws IOException
	 * @throws ParseException
	 */
	public JSONArray getJSONData(File JSONfile) throws FileNotFoundException,
			IOException, ParseException {
		JSONParser parser = new JSONParser();
		JSONArray array = (JSONArray) parser.parse(new FileReader(JSONfile));

		return array;
	}

	/**
	 * Delete an history file
	 * 
	 * @param the
	 *            File to delete
	 * @return true if the file is succesfully deleted, false otherwise
	 */
	public boolean deleteFile(File file) {
		return file.delete();
	}

	/**
	 * Get all the history items stored in the database
	 * 
	 * @return a list of all the history items stored
	 */
	public List<HistoryItem> getHistory() {
		List<HistoryItem> history = new ArrayList<HistoryItem>();

		SQLiteDatabase db = databaseOH.getReadableDatabase();

		Cursor cursor = db.query(DatabaseOpenHelper.TABLE_HISTORY, allColumns,
				null, null, null, null, null);

		cursor.moveToFirst();
		while (!cursor.isAfterLast()) {
			HistoryItem item = cursorToHistoryItem(cursor);
			history.add(item);
			cursor.moveToNext();
		}
		// Close the cursor!
		cursor.close();

		return history;
	}

	/**
	 * Get all the history items stored in the database in a specified range of
	 * time
	 * 
	 * @param the
	 *            starting time in milliseconds
	 * 
	 * @param the
	 *            finishing time in milliseconds
	 * 
	 * @return a list of the history items more recent than the specified time
	 */
	public List<HistoryItem> getHistory(long start, long finish) {
		List<HistoryItem> history = new ArrayList<HistoryItem>();

		SQLiteDatabase db = databaseOH.getReadableDatabase();

		Cursor cursor = db
				.query(DatabaseOpenHelper.TABLE_HISTORY, allColumns,
						DatabaseOpenHelper.COLUMN_TIMESTAMP + " >= " + start
								+ " AND " + DatabaseOpenHelper.COLUMN_TIMESTAMP
								+ " <= " + finish, null, null, null, null);

		cursor.moveToFirst();
		while (!cursor.isAfterLast()) {
			HistoryItem item = cursorToHistoryItem(cursor);
			history.add(item);
			cursor.moveToNext();
		}
		// Close the cursor!
		cursor.close();

		return history;
	}
	
	/**
	 * Delete all the history items from the database
	 */
	public void deleteHistory() {
		SQLiteDatabase db = databaseOH.getWritableDatabase();
		db.delete(DatabaseOpenHelper.TABLE_HISTORY, null, null);
	}

	private HistoryItem cursorToHistoryItem(Cursor cursor) {
		HistoryItem item = new HistoryItem();

		item.setTimestamp(cursor.getLong(1));
		item.setCategory(cursor.getString(2));
		MagnitudeFeatures accelFeatures = new MagnitudeFeatures();
		MagnitudeFeatures gyroFeatures = new MagnitudeFeatures();
		accelFeatures.setAverage(cursor.getDouble(3));
		accelFeatures.setMinimum(cursor.getDouble(4));
		accelFeatures.setMaximum(cursor.getDouble(5));
		accelFeatures.setStandardDeviation(cursor.getDouble(6));
		gyroFeatures.setAverage(cursor.getDouble(7));
		gyroFeatures.setMinimum(cursor.getDouble(8));
		gyroFeatures.setMaximum(cursor.getDouble(9));
		gyroFeatures.setStandardDeviation(cursor.getDouble(10));
		item.setAccelFeatures(accelFeatures);
		item.setGyroFeatures(gyroFeatures);

		return item;
	}

}

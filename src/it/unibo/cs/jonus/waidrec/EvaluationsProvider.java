package it.unibo.cs.jonus.waidrec;

import java.util.Arrays;
import java.util.HashSet;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.util.Log;

public class EvaluationsProvider extends ContentProvider {
	
	private DatabaseOpenHelper database;
	private static final int EVALUATIONS = 1;
	private static final int LAST_EVALUATION = 2;
	private static final String AUTHORITY = "it.unibo.studio.waid.evaluationsprovider";
	private static final String BASE_PATH = "evaluations";
	public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + BASE_PATH);
	
	private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
	static {
		sUriMatcher.addURI(AUTHORITY, BASE_PATH, EVALUATIONS);
		sUriMatcher.addURI(AUTHORITY, BASE_PATH + "/last", LAST_EVALUATION);
	}
	
	
	public EvaluationsProvider() {
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		// TODO: Implement this to handle requests to delete one or more rows.
		throw new UnsupportedOperationException("Not yet implemented");
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
	    switch (uriType) {
	    case EVALUATIONS:
	    	id = db.insert(DatabaseOpenHelper.TABLE_EVALUATIONS, null, values);
	    	break;
	    default:
	    	throw new IllegalArgumentException("Unknown URI: " + uri);
	    }
	    getContext().getContentResolver().notifyChange(uri, null);
	    
	    return Uri.parse(BASE_PATH + "/" + id);
	}

	@Override
	public boolean onCreate() {
		// Open database
		database = new DatabaseOpenHelper(getContext());
		// Delete all the rows from evaluations table
		SQLiteDatabase db = database.getWritableDatabase();
		int rows = db.delete(DatabaseOpenHelper.TABLE_EVALUATIONS, null, null);
		Log.v("EvaluationProvider", "Deleted " + rows + " rows");
		
		return false;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		
		SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
		
		checkColumns(projection);
		queryBuilder.setTables(DatabaseOpenHelper.TABLE_EVALUATIONS);
		
		int uriType = sUriMatcher.match(uri);
		String limit = null;
		switch (uriType){
		case LAST_EVALUATION:
			// discard selection values, add limit and sorting to get last evaluation
			selection = null;
			sortOrder = "_id DESC";
			limit = "1";
			break;
		default:
			throw new IllegalArgumentException("Unknown URI: " + uri);
		}
		
		SQLiteDatabase db = database.getWritableDatabase();
		Cursor cursor = queryBuilder.query(db, projection, selection, selectionArgs, null, null, sortOrder, limit);
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
	    String[] available = { DatabaseOpenHelper.COLUMN_CATEGORY, DatabaseOpenHelper.COLUMN_TIMESTAMP, DatabaseOpenHelper.COLUMN_ID };
	    if (projection != null) {
	      HashSet<String> requestedColumns = new HashSet<String>(Arrays.asList(projection));
	      HashSet<String> availableColumns = new HashSet<String>(Arrays.asList(available));
	      // Check if all columns which are requested are available
	      if (!availableColumns.containsAll(requestedColumns)) {
	        throw new IllegalArgumentException("Unknown columns in projection");
	      }
	    }
	}

	
}

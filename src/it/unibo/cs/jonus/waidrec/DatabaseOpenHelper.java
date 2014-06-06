package it.unibo.cs.jonus.waidrec;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseOpenHelper extends SQLiteOpenHelper {

	private static final int DATABASE_VERSION = 4;
	public static final String DATABASE_NAME = "waid.db";
	public static final String TABLE_EVALUATIONS = "evaluations";

	public static final String COLUMN_ID = "_id";
	// evaluations table columns
	public static final String COLUMN_CATEGORY = "category";
	public static final String COLUMN_TIMESTAMP = "timestamp";
	public static final String COLUMN_AVGA = "avga";
	public static final String COLUMN_MINA = "mina";
	public static final String COLUMN_MAXA = "maxa";
	public static final String COLUMN_STDA = "stda";
	public static final String COLUMN_AVGG = "avgg";
	public static final String COLUMN_MING = "ming";
	public static final String COLUMN_MAXG = "maxg";
	public static final String COLUMN_STDG = "stdg";

	// history table creation query
	private static final String CREATE_TABLE_EVALUATIONS = "CREATE TABLE IF NOT EXISTS "
			+ TABLE_EVALUATIONS
			+ " ("
			+ COLUMN_ID
			+ " INTEGER PRIMARY KEY AUTOINCREMENT, "
			+ COLUMN_TIMESTAMP
			+ " INT NOT NULL, "
			+ COLUMN_CATEGORY
			+ " TEXT, "
			+ COLUMN_AVGA
			+ " REAL, "
			+ COLUMN_MINA
			+ " REAL, "
			+ COLUMN_MAXA
			+ " REAL, "
			+ COLUMN_STDA
			+ " REAL, "
			+ COLUMN_AVGG
			+ " REAL, "
			+ COLUMN_MING
			+ " REAL, " + COLUMN_MAXG + " REAL, " + COLUMN_STDG + " REAL);";

	public DatabaseOpenHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);

	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(CREATE_TABLE_EVALUATIONS);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_EVALUATIONS);
		onCreate(db);
	}

}

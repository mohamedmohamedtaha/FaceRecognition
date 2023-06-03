package app.statest.camerax;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;


@Singleton
public final class SQLHelper {

	private static final String TAG = "sql";
	private DatabaseHelper mDbHelper;
	private static SQLiteDatabase mDb;
	private static String currentDBVersion = "0.8";
	private static Boolean differentDB = false;
	private static final String passwordDatabase = "1234";
	private static final String DB_PATH = "/storage/emulated/0/" + SessionClass.DBFolderName;

	private static final String DATABASE_NAME = SessionClass.DBName;
	private static final String myPath = DB_PATH + DATABASE_NAME;

	private static final int DATABASE_VERSION = 3;
	private final Context adapterContext;
	File file;

	@Inject
	public SQLHelper(@ApplicationContext Context context) {
		this.adapterContext = context;
		file = new File(DB_PATH);
		if (!file.exists()) {
			file.mkdirs();
		}
	}

	public SQLHelper open() throws SQLException {

		mDbHelper = new DatabaseHelper(adapterContext);
		try {
			mDbHelper.createDataBase();
		} catch (IOException ioe) {
			throw new Error("Unable to create database");
		}
		try {
			mDbHelper.openDataBase();
		} catch (SQLException sqle) {
			throw sqle;
		}
		Cursor mCurr = Select("select version_column from  version_table", null);
		if (mCurr != null && mCurr.getCount() > 0 && mCurr.moveToFirst()) {
			if (mCurr.getString(0).equals(currentDBVersion)) {
				differentDB = false;
			} else {
				differentDB = true;
			}
		} else {
		}
		// disable
		differentDB = false;
		return this;
	}

	public Cursor Select(String query, String[] myVariable) {
		try {
			return mDb.rawQuery(query, myVariable);
		} catch (Exception e) {
			// TODO: handle exception
			e.getMessage();
			return null;
		}
	}

	public String Insert(String command) {
		try {
			mDb.execSQL(command);
		} catch (SQLException sqle) {
			return sqle.getMessage();
		} catch (Exception sqle) {
			return sqle.getMessage();
		}
		return "true";
	}
	public Boolean insert(ContentValues values, String table_name) {
		try {

			long rowID = mDb.insert(table_name, null, values);
			return rowID > -1;
		} catch (Exception e) {
			return false;
		}
	}
	public String Insert(ContentValues values, String table_name) {
		try {
			mDb.insert(table_name, null, values);
			return "true";
		} catch (Exception e) {
			return e.getMessage();
		}
	}
	public Cursor rawQuery(String sql) {
		Cursor mCursor;
		try {
			mCursor = mDb.rawQuery(sql, null);
			return mCursor;
		} catch (Exception e) {
			return null;
		}
	}
	public String Update(String command) {
		try {
			mDb.execSQL(command);
		} catch (Exception e) {
			return e.getMessage();
		}
		return "true";
	}

	public String Delete(String command) {
		try {
			// mDb.execSQL(command, new String[]{ myVariable1, myVariable2});
			mDb.execSQL(command);
			return "true";
		} catch (Exception e) {
			e.getMessage();
			return "false";
		}
	}

	public Cursor RawQuery(String tableName) {
		Cursor mCursor = null;
		try {
			mCursor = mDb.rawQuery("PRAGMA table_info("+tableName+")",null);
			return mCursor;
		} catch (Exception e) {
			return null;
		}
	}

	public void Delete(String TableName, String Condition) {
		try {
			mDb.delete(TableName, Condition, null);
		} catch (Exception e) {
			e.getMessage();
		}
	}

	public void close() {
		mDbHelper.close();
	}

	private static class DatabaseHelper extends android.database.sqlite.SQLiteOpenHelper {

		Context helperContext;

		DatabaseHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
			helperContext = context;
		}

		@Override
		public void onCreate(SQLiteDatabase db) {}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			Log.w(TAG, "Upgrading database!!!!!");
			// db.execSQL("");
			onCreate(db);
		}

		public void createDataBase() throws IOException {
			boolean dbExist = checkDataBase();
			if (dbExist) {
				if (differentDB) {
					this.getReadableDatabase();
					try {
						copyDataBase();
					} catch (IOException e) {
						throw new Error("Error copying database");
					}
				}
			} else {
				this.getReadableDatabase();
				try {
					copyDataBase();
				} catch (IOException e) {
					throw new Error("Error copying database");
				}
			}
		}

		public SQLiteDatabase getDatabase() {
			String myPath = DB_PATH + DATABASE_NAME;
			return SQLiteDatabase.openDatabase(myPath, null,
					SQLiteDatabase.OPEN_READONLY);
		}

		private boolean checkDataBase() {
			SQLiteDatabase checkDB = null;
			try {
				String myPath = DB_PATH + DATABASE_NAME;
				checkDB = SQLiteDatabase.openDatabase(myPath, null,
						SQLiteDatabase.OPEN_READWRITE);
			} catch (SQLiteException e) {
				e.getMessage();
			}
			if (checkDB != null) {
				checkDB.close();
			}
			return checkDB != null ? true : false;
		}

		private void copyDataBase() throws IOException {
			// Open your local db as the input stream
			InputStream myInput = helperContext.getAssets().open(DATABASE_NAME);
			// Path to the just created empty db
			String outFileName = DB_PATH + DATABASE_NAME;
			// Open the empty db as the output stream
			OutputStream myOutput = new FileOutputStream(outFileName);
			// transfer bytes from the inputfile to the outputfile
			byte[] buffer = new byte[1024];
			int length;
			while ((length = myInput.read(buffer)) > 0) {
				myOutput.write(buffer, 0, length);
			}
			// Close the streams
			myOutput.flush();
			myOutput.close();
			myInput.close();
		}

		public void openDataBase() throws SQLException {
			// Open the database
			String myPath = DB_PATH + DATABASE_NAME;
			mDb = SQLiteDatabase.openDatabase(myPath, null,
					SQLiteDatabase.OPEN_READWRITE);
		}

		@Override
		public synchronized void close() {
			if (mDb != null)
				mDb.close();
			super.close();
		}
	}
}

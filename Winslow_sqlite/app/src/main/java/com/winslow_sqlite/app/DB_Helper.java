package com.winslow_sqlite.app;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DB_Helper extends SQLiteOpenHelper {
	private static String TAG = "DB Helper"; //for debug
	private static String DB_PATH = "/data/data/com.winslow_sqlite.app/databases/";
	private static final String DATABASE_NAME = "winslow_app";
	private static final int DATABASE_VERSION = 1;
	private SQLiteDatabase myDataBase;
	private final Context myContext;
	
	public DB_Helper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
		this.myContext = context;
	}

	/** Create empty database and rewrite it */
	
	public void createDataBase() throws IOException {
		boolean dbExist = checkDataBase();
		if(dbExist) {
			//do nothing//
			Log.e(TAG, "okay, db exists!");
			}
		else {
			this.getReadableDatabase();
			try {
				copyDataBase();
				Log.e(TAG, " database created!");
				}
			catch (IOException e) {
				throw new Error("Error copying database");
				}
			}
	}
	
	private boolean checkDataBase(){
		SQLiteDatabase checkDB = null;
		try {
			String myPath = DB_PATH + DATABASE_NAME;
			checkDB = SQLiteDatabase.openDatabase(myPath, null, SQLiteDatabase.OPEN_READONLY);
		    }
		catch(SQLiteException e) {
		    }
		if (checkDB !=null) {
			checkDB.close();
		    }
		return checkDB != null ? true : false;
	}
	
	private void copyDataBase() throws IOException {
		InputStream myInput = myContext.getAssets().open(DATABASE_NAME);
		String outFileName = DB_PATH + DATABASE_NAME;
		OutputStream myOutput = new FileOutputStream(outFileName);
		
		byte[] buffer = new byte[1024];
		int length;
		
		while ((length = myInput.read(buffer))>0) {
			myOutput.write(buffer, 0, length);
			}
		myOutput.flush();
		myOutput.close();
		myInput.close();
	}
	
	public SQLiteDatabase openDataBase() throws SQLException {
		String myPath = DB_PATH + DATABASE_NAME;
        Log.i(TAG, " In openDataBase: " + myPath);
		return myDataBase = SQLiteDatabase.openDatabase(myPath,  null, SQLiteDatabase.OPEN_READONLY);
	}
	
	@Override
	public synchronized void close() {
		if(myDataBase != null) {
			myDataBase.close();
			}
		super.close();
        Log.i(TAG, " DB should be CLOSED!");
	}
	
	@Override
	
	public void onCreate(SQLiteDatabase db) {
		/**
		 * don't need?
		db.execSQL(DATABASE_CREATE);
		*/
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		/**
		 * don't need?
		db.execSQL("drop table if exists " + TABLE_NAME);
		onCreate(db);
		*/
	}
}

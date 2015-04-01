package com.winslow_sqlite.app;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;


public class DictionaryDb {
	
	private static final String TAG = "DictionaryDB";
	public static final String MY_WORD = "WORD"; 
	public static final String TABLE_NAME = "winslow_app";
	public static final String ENTRY_ID = "_id";
	public static final String TAM_HEAD_WORD = "utf8hw";
	public static final String ROM_HEAD_WORD = "romutfhw";
	public static final String ROM_HEAD_WORD_NO_ACC = "romasciihw";
	public static final String DEFINITION = "utf8def";
	public static boolean search_headwords = true;
    public boolean got_tamil;
    public boolean got_accented_rom;
	
	public SQLiteDatabase myDB;
    public Context mContext;
	private final DB_Helper myDicoOpenHelper;

	
	public DictionaryDb(Context context) {
		this.mContext = context;
		myDicoOpenHelper = new DB_Helper(mContext);
	}
	
    // stackoverflow code //
	// http://stackoverflow.com/questions/9109438/how-to-use-an-existing-database-with-an-android-application/9109728#9109728 //
    
    public DictionaryDb createDataBase() throws SQLException {
            
            try {
                    myDicoOpenHelper.createDataBase();
                    Log.e(TAG, " databases created....");
            }
            catch (IOException mIOException) {
            		Log.e(TAG, mIOException.toString() + " unable to create DB!");
                    throw new Error("Unable to create Database!");
            }
            return this;
    }
    
    public DictionaryDb open() throws SQLException {
            try {
                    myDicoOpenHelper.openDataBase();
                    //myDicoOpenHelper.close();
                    myDB = myDicoOpenHelper.getReadableDatabase();
                    Log.e(TAG, " DictionaryDb open worked!");
            }
            catch (SQLException mSQLException) {
            		Log.e(TAG, "open >>"+ mSQLException.toString());
                    throw mSQLException;
            }
            return this;
    }
    
    public void close() {
            myDicoOpenHelper.close();
    }
    
    // end of stackoverflow code //
    
    public boolean setQuerySelector(boolean head_word_search) {
    	String bool_value = toString();
    	
    	if (head_word_search) {
    		Log.e(TAG, "Headword search: " + bool_value);
    		search_headwords = true;
    	} else {
    		Log.e(TAG, "Fulltext search: " + bool_value);
    		search_headwords = false;
    	}
    	return search_headwords;
    }
	
    public Cursor getWordMatches(String query, String[] columns) {
        
    	String selection;
        String[] q_pair = query.split("<cmc>");
        if (q_pair[0].contains("head")){
            search_headwords = true;
            Log.i(TAG, " Doing a headword search");
        }
        else {
            search_headwords = false;
        }
        query = q_pair[1];
        Log.i(TAG, " get word matches for: " + query);

    	if (search_headwords) {
            //query = "%" + query + "%";
            Pattern tamil_chars = Pattern.compile("[\u0b80-\u0bff]");
            Matcher tamil_matcher = tamil_chars.matcher(query);
            Pattern rom_accent_chars = Pattern.compile("[\u0101\u016b\u014d\u00f1\u1e00-\u1eff]");
            Matcher rom_accent_matcher = rom_accent_chars.matcher(query);
    		if (tamil_matcher.find()) {
    			selection = TAM_HEAD_WORD + " like ?";
    			Log.i(TAG, " Got some tamil here! " + selection);
    		}
    		else if (rom_accent_matcher.find()) {
    			selection = ROM_HEAD_WORD + " like ?";
    			Log.i(TAG, " Accented roman: " + selection);
    		}
            else {
                selection = ROM_HEAD_WORD_NO_ACC + " like ?";
                Log.i(TAG, " Plain vanilla ascii: " + selection);
            }
    	}
    	else {
    		//selection = TABLE_NAME + " match ?"; // this should work for fts
    		selection = DEFINITION + " like ?"; // not using fts for winslow
            //query = " %" + query + "% ";
    		Log.i(TAG, " Okay, searching all fields " + selection);
    	}

        query = "%" + query + "%";
    	//selection = TABLE_NAME + " match ?";
        String[] selectionArgs = new String[] {query};
        
        /* This builds a query that looks like:
         *     SELECT <columns> FROM <table> WHERE <KEY_WORD> MATCH HEAD_WORD (I think)
         */

        Log.i(TAG, " term: " + query + " args: ");
        
		return query(selection, selectionArgs, columns);
    }
	
	private Cursor query(String selection, String[] selectionArgs, String[] columns) {
		SQLiteDatabase myDB = myDicoOpenHelper.openDataBase();
		
		
		// columns can remain null if all are to be returned
		Cursor cursor = myDB.query(TABLE_NAME, columns,
				selection, selectionArgs, null, null, null, null);
		
		Log.i(TAG, "From DictionaryDb: " + columns + "-->" + selection + "-->" + selectionArgs);
		if (cursor == null) {
			return null;
		}
		else if (!cursor.moveToFirst()) {
			cursor.close();
			return null;
		}
		return cursor;
	}
	
	
	
}

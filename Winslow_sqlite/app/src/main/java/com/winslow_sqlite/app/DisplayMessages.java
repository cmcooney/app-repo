package com.winslow_sqlite.app;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.util.Log;

public class DisplayMessages {
	
	public Context context;
	
	private static final String TAG = "DisplayMessages";
	public boolean search_headwords;
	
	public DisplayMessages (Context context) {
		this.context = context;
	}
	
	//=================================================//
	
	public void showAlertDialog(Context context, String title, String message, Boolean status) {
		//AlertDialog alertDialog = new AlertDialog.Builder(context).create();
		
		AlertDialog.Builder alertDialog = new AlertDialog.Builder(context);
		alertDialog.setTitle(title);
		alertDialog.setMessage(message);
		alertDialog.setIcon((status) ? android.R.drawable.btn_star_big_on : R.drawable.ic_action_warning);
		
		if (status == true) {
			alertDialog.setPositiveButton("Okay!", new DialogInterface.OnClickListener() {
			
				@Override
				public void onClick(DialogInterface dialog, int which) {
					//System.exit(0);
					// TODO Auto-generated method stub
				}
			});
		}
		else {	
			//alertDialog.setPositiveButton("Piss off!", new DialogInterface.OnClickListener() {
				
			//	@Override
			//	public void onClick(DialogInterface dialog, int which) {
					//System.exit(0);
					// TODO Auto-generated method stub
			//	}
			//});
		}
		
		alertDialog.show();
		
	}
	
	//==========================================================//


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
    
}

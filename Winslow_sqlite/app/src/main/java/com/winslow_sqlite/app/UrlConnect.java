package com.winslow_sqlite.app;

import java.net.HttpURLConnection;
import java.net.URL;

import android.app.AlertDialog;
import android.os.AsyncTask;
import android.util.Log;
import android.content.Context;



public class UrlConnect extends AsyncTask<String, Void, Boolean>{
	private int response_code = 0;
	private static final String TAG = "URLConnect";
    public String server = "http://dsalsrv02.uchicago.edu";
	Boolean live_server;
	DisplayMessages dm;
    AlertDialog alertDialog;
    private Context mContext;
    public Context context;

    public UrlConnect (Context context){
        mContext = context;
    }
	@Override
	protected Boolean doInBackground(String... urls) {
		

		try {
			URL url = new URL(server);
            Log.i(TAG, " Server URL: " + url);
			HttpURLConnection urlc = (HttpURLConnection) url.openConnection();
			urlc.setReadTimeout(10000);
			urlc.setConnectTimeout(15000);
			urlc.setRequestMethod("GET");
			urlc.setDoInput(true);
			
			urlc.connect();
			response_code = urlc.getResponseCode();
			
			Log.i(TAG, "HTTP response: " + response_code);
			//live_server = true;

            if (response_code > 0) {
				live_server = true;
			}
			
			//if (response_code == HttpURLConnection.HTTP_OK) {
			if (response_code > 0) {
				Log.e(TAG, "Boolean live connection condition is true");
				live_server = true;
			}
			
			else {
				Log.e(TAG, "Response code HTTP_OK is NOT okay!");
				live_server=false;
			}
			
			
		}
		catch (Exception exception){
			Log.e(TAG, exception.toString());
			//catch_exception = false;
			return false;
		}
		
		return live_server;
	}

    @Override
    protected void onPostExecute(Boolean result) {

    }

}

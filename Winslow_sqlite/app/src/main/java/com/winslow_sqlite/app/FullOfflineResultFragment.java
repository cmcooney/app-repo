package com.winslow_sqlite.app;

/**
 * Created by cmcooney on 11/13/14.
 */

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FullOfflineResultFragment extends Fragment {

    public String TAG = "In FullOfflineResultFragment";
    ProgressDialog dialog;
    public Context context;
    public Activity activity;
    public String query;
    public int results_count;
    public TextView mTextView;
    public WebView mWebView;
    DictionaryDb db;


    @Override
    public void onAttach(Activity activity){
        super.onAttach(activity);
        context = getActivity().getApplicationContext();
        Log.i(TAG, " onAttach");
    }

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
        Log.i(TAG, " onCreateView");
        View view = inflater.inflate(R.layout.fullresult, container, false);
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState){
        super.onActivityCreated(savedInstanceState);
	    db = new DictionaryDb(context);
        Bundle bundle = this.getArguments();
        String query = bundle.getString("query");

	    Cursor cursor = db.getWordMatches(query, null);
        String out_string = "";
        String results_array = ""; // not an array, *wink*
        int counter = 0;

        String[] display_query = query.split("<cmc>");
        display_query[1] = display_query[1].replaceAll("%20", " ");

        if (getView() == null) {
            View view = LayoutInflater.from(context).inflate(R.layout.fullresult, null);
            Log.i(TAG, " View was null: " + view.toString());
            mTextView = (TextView) view.findViewById(R.id.hit_count);
            mWebView = (WebView) view.findViewById(R.id.full_def);
        } else {
            Log.i(TAG, " The View from here: " + getView().toString());
            mTextView = (TextView) getView().findViewById(R.id.hit_count);
            mWebView = (WebView) getView().findViewById(R.id.full_def);
        }

        if (cursor == null) {
            Log.i(TAG, " No results, cursor null");
            mTextView.setText(getString(R.string.no_results, new Object[] {display_query[1]}));
        }
        else {
            int count = cursor.getCount();
            Log.i(TAG, " No. of results: " + count);
            for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                counter += 1;
                Log.i(TAG, " Tamil Headword: " + cursor.getString(1));
                String hw = cursor.getString(1) + " " + cursor.getString(2);
                String def = cursor.getString(4);
                String page = cursor.getString(5);
                out_string = counter + ") " + hw + " (p. " + page + ") " + def + "\n";
                results_array += "<p>" + out_string;
            }

            mWebView.setBackgroundColor(0x00000000);
            String countString = context.getResources().getQuantityString(R.plurals.search_results, count,
                    new Object[]{count, display_query[1]});
            //String countString = "Full definition for " + query + ":";
            mTextView.setText(countString);


            results_array = results_array.replaceAll("<p2>", "<p>&nbsp;&nbsp;&nbsp;"); //should use css for this
            String html_header = "<html><head><link href=\"sqlite_display.css\" type=\"text/css\" rel=\"stylesheet\">";
            String display_results = html_header + "<body>" + results_array + "</body>";
            Log.i(TAG, " webview results: " + display_results);
            mWebView.getSettings().setBuiltInZoomControls(true);
            mWebView.getSettings().setJavaScriptEnabled(true);

            mWebView.setWebViewClient(new WebViewClient(){

                @Override
                public boolean shouldOverrideUrlLoading(WebView view, String url){
                    Log.i(TAG , " URL looks like: " + url);
                    if (url != null){
                        Log.i(TAG, " Your page no: " + url);
                        String page_number = url;
                        page_number = page_number.replace("file:///android_asset/", "");
                        //getPageResults.getPageResults(page_number);
                        return true;
                    }
                    else {
                        return false;
                    }
                }

            });
            mWebView.loadDataWithBaseURL("file:///android_asset/", display_results, "text/html", "utf-8", "");

        }
    }

}

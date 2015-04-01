package com.winslow_sqlite.app;

/**
 * Created by cmcooney on 11/13/14.
 */

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.res.Configuration;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OfflineResultFragment extends Fragment {

    public String TAG = "In OfflineResultFragment";
    ProgressDialog dialog;
    GetFullOfflineDef getFullOfflineDef;
    public Context context;
    public Activity activity;
    public String query;
    public int results_count;
    DictionaryDb db;


    public interface GetFullOfflineDef {
        public void getFullOfflineDef(String fullOfflineQuery);
    }

    @Override
    public void onAttach(Activity activity){
        super.onAttach(activity);
        context = getActivity().getApplicationContext();
        Log.i(TAG, " onAttach");
        try {
            getFullOfflineDef = (GetFullOfflineDef) activity;
        }
        catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement interface correctly");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
        Log.i(TAG, " onCreateView");
        View view = inflater.inflate(R.layout.result, container, false);
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
        ArrayList<String> all_results = new ArrayList<String>();

        final TextView mTextView;
        final ListView mListView;

        if (getView() == null) {
            View view = LayoutInflater.from(context).inflate(R.layout.result, null);
            Log.i(TAG, " View was null: " + view.toString());
            mTextView = (TextView) view.findViewById(R.id.hit_count);
            mListView = (ListView) view.findViewById(R.id.results_list);
        } else {
            Log.i(TAG, " The View from here: " + getView().toString());
            mTextView = (TextView) getView().findViewById(R.id.hit_count);
            mListView = (ListView) getView().findViewById(R.id.results_list);
        }

        String[] display_query = query.split("<cmc>");
        display_query[1] = display_query[1].replaceAll("%20", " ");

        int counter = 0;
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
                String[] split_def = def.split(" ");
                String short_def = "";
                Log.i(TAG, " Split def length: " + split_def.length);
                if (split_def.length > 25) {
                    Log.i(TAG, " Shortening def");
                    for (int i = 0; i < 25; i++) {
                        short_def = short_def + " " + split_def[i];
                    }
                    def = short_def + " ... ";
                }

                out_string = counter + ") " + hw + " (p. " + page + ") " + def + "\n";

                all_results.add(out_string);
            }


            int results_count = cursor.getCount();
            String countString = context.getResources().getQuantityString(R.plurals.search_results, results_count,
                    new Object[]{results_count, display_query[1]});
            mTextView.setText(countString);

            if (all_results != null && !all_results.isEmpty()) {
                Log.i(TAG, " Now formatting your results");
                DisplayResultsAdapter outAdapter = new DisplayResultsAdapter(context, R.layout.result, all_results);
                mListView.setAdapter(outAdapter);
            }

            mListView.setOnItemClickListener(new AdapterView.OnItemClickListener(){
                @Override

                public void onItemClick(AdapterView<?> parent, View view, int position, long id){
                    String single_result_hit = mListView.getItemAtPosition(position).toString();
                    Log.i(TAG, " Your result: " + single_result_hit);
                    String headword_query_match = "";
                    Pattern headword_regex = Pattern.compile("<tam>([^<]*)</tam>");
                    Matcher headword_match = headword_regex.matcher(single_result_hit);

                    if (headword_match.find()){
                        headword_query_match = headword_match.group(1);
                        Log.i(TAG, " Now a query with this word: " + headword_query_match);
                        headword_query_match = "head<cmc>" + headword_query_match;
                        getFullOfflineDef.getFullOfflineDef(headword_query_match);
                    }

                }
            }); // end click listener
        }

    }

}

package com.winslow_sqlite.app;

/**
 * Created by cmcooney on 11/13/14.
 */

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.res.Configuration;
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

public class SearchResultFragment extends Fragment {

    public String TAG = "In SearchResultFragment";
    GetFullDef getFullDef;
    ProgressDialog dialog;
    public Context context;
    public Activity activity;
    public String query;
    public int results_count;

    public interface GetFullDef {
        public void getFullDef(String fulldef_query_term);
    }
    @Override
    public void onAttach(Activity activity){
        super.onAttach(activity);
        context = getActivity().getApplicationContext();
        Log.i(TAG, " onAttach");
        try {
            getFullDef = (GetFullDef) activity;
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
        Bundle bundle = this.getArguments();
        String query_uri = bundle.getString("query_uri");
        new GetQueryResults().execute(query_uri);
        //final InputMethodManager imm = (InputMethodManager)getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        //imm.hideSoftInputFromWindow(getView().getWindowToken(),0);
        getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        Log.i(TAG, " Keyboard close should have executed?");
    }

    private class GetQueryResults extends AsyncTask<String, Void, ArrayList> {

        public String query = "";

        public GetQueryResults() {
        }

        @Override
        protected void onPreExecute() {
            if (dialog == null) {
                dialog = new ProgressDialog(getActivity());
                dialog.setMessage("Retrieving results.");
                dialog.show();
            }
        }

        @Override
        protected ArrayList doInBackground(String... urls) {
            BufferedReader reader = null;
            ArrayList<String> all_results = new ArrayList<String>();
            int counter;

            try {
                String search_URI = urls[0];
                Log.i(TAG + "  Search URI: ", search_URI);
                Pattern p = Pattern.compile("qs=([^&]*)&");
                Matcher m = p.matcher(search_URI);

                while (m.find()) {
                    query = m.group(1);
                }

                URI search_query = new URI(urls[0]);
                DefaultHttpClient httpclient = new DefaultHttpClient();
                HttpGet httpget = new HttpGet(search_query);

                Log.i(TAG, "HTTP query:  " + query + " " + httpget.toString());

                HttpResponse response = httpclient.execute(httpget);
                HttpEntity entity = response.getEntity();
                InputStream content = entity.getContent();


                // read results into buffer //
                try {
                    reader = new BufferedReader(new InputStreamReader(content));
                    String line = "";
                    String out_string = "";
                    Pattern count_regex = Pattern.compile("<count>([^<]*)</count>");

                    while ((line = reader.readLine()) != null) {
                        Matcher count_match = count_regex.matcher(line);

                        if (count_match.find()) {
                            String incoming_count = count_match.group(1);
                            results_count = Integer.parseInt(incoming_count.toString());
                            Log.i(TAG, "Results count == " + results_count);
                        } else {

                            JSONArray jsonArray = new JSONArray(line);
                            //counter++;
                            for (int i = 0; i < jsonArray.length(); i++) {
                                counter = 1 + i;
                                JSONObject jsonObject = jsonArray.getJSONObject(i);
                                String hw = jsonObject.getString("hw");
                                //hw = hw.toUpperCase(locale);
                                String def = jsonObject.getString("def");
                                String page = jsonObject.getString("page");
                                //Log.i(TAG, " headword: " + hw);
                                //Log.i(TAG, " def: " + def);
                                def.replaceAll("\n", "<br>");
                                out_string = counter + ") " + hw + " (p. " + page + ") " + def + "\n";
                                all_results.add(out_string);
                            }
                        }
                    }

                } // end try BufferedReader

                catch (IOException exception) {
                    Log.e(TAG, "Here? IOException --> " + exception.toString());
                }

                // pro-forma cleanup //
                finally {
                    if (reader != null) {
                        try {
                            reader.close();
                        } catch (IOException exception) {
                            Log.e(TAG, "IOException --> " + exception.toString());
                        }
                    }
                }

            } // end searchURI try

            // Exception for problems with HTTP connection //
            catch (Exception exception) {
                Log.e(TAG, "Trouble connecting -->" + exception.toString());
                return null;
            }

            return all_results;
        } // end doInBackground

        @Override
        protected void onPostExecute(ArrayList all_results) {
            if (dialog != null) {
                dialog.dismiss();
            }

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


            query = query.replaceAll("%20", " ");
            String countString = context.getResources().getQuantityString(R.plurals.search_results, results_count,
                    new Object[]{results_count, query});
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
                        getFullDef.getFullDef(headword_query_match);
                    }

                }
            }); // end click listener

        } // end onPostExecute

    } // end GetQueryResults

}

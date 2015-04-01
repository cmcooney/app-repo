package com.winslow_sqlite.app;

/**
 * Created by cmcooney on 11/13/14.
 */

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.SpannableStringBuilder;
import android.text.style.URLSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageButton;
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

public class GetPageFragment extends Fragment {

    public String TAG = "In GetPageFragment";
    ProgressDialog dialog;
    public Context context;
    GetPageResults getPageResults;
    public String current_page;
    public int results_count;
    public Activity activity;
    public WebView mWebView;
    public float chuck_float = Float.parseFloat(".25");


    public interface GetPageResults {
        public void getPageResults(String page_no);
    }

    @Override
    public void onAttach(Activity activity){
        super.onAttach(activity);
        context = getActivity().getApplicationContext();
        Log.i(TAG, " onAttach");
        try {
            getPageResults = (GetPageResults) activity;
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
        View view = inflater.inflate(R.layout.pageresult, container, false);
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState){
        super.onActivityCreated(savedInstanceState);
        Bundle bundle = this.getArguments();
        String query_uri = bundle.getString("query_uri");
        new GetPage().execute(query_uri);
        final InputMethodManager imm = (InputMethodManager)getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(getView().getWindowToken(),0);
    }

    private class GetPage extends AsyncTask<String, Void, ArrayList> {

        public String query = "";
        public String page = "";

        public GetPage() {
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
                Pattern p = Pattern.compile("page=([^&]*)&");
                Matcher m = p.matcher(search_URI);

                while (m.find()) {
                    current_page = m.group(1);
                }

                URI search_query = new URI(urls[0]);
                DefaultHttpClient httpclient = new DefaultHttpClient();
                HttpGet httpget = new HttpGet(search_query);

                Log.i(TAG, "HTTP query:  " + current_page + " " + httpget.toString());

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
                                page = jsonObject.getString("page");
                                page = "<a href=\"" + page + "\">p. " + page + "</a>";
                                //Log.i(TAG, " headword: " + hw);
                                //Log.i(TAG, " def: " + def);
                                def.replaceAll("\n", "<br>");
                                //out_string = counter + ") " + hw + " " + def + "\n";
                                out_string = hw + " (" + page + ") " + def + "\n";
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

            final WebView mWebView;
            ImageButton next_btn;
            ImageButton prev_btn;
            final TextView mTextView;


            if (getView() == null) {
                View view = LayoutInflater.from(context).inflate(R.layout.fullresult, null);
                Log.i(TAG, " View was null: " + view.toString());
                mWebView = (WebView) view.findViewById(R.id.page_res);
                mTextView = (TextView) view.findViewById(R.id.page_number);
                prev_btn = (ImageButton) view.findViewById(R.id.ll_previous);
                next_btn = (ImageButton) view.findViewById(R.id.ll_next);
            } else {
                Log.i(TAG, " The View from here: " + getView().toString());
                mWebView = (WebView) getView().findViewById(R.id.page_res);
                mTextView = (TextView) getView().findViewById(R.id.page_number);
                prev_btn = (ImageButton) getView().findViewById(R.id.ll_previous);
                next_btn = (ImageButton) getView().findViewById(R.id.ll_next);
            }


            mWebView.setBackgroundColor(0x00000000);
            int next_int = Integer.valueOf(current_page) + 1;
            final String next_page = String.valueOf(next_int);
            int prev_int = Integer.valueOf(current_page) - 1;
            final String prev_page = String.valueOf(prev_int);

            next_btn.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    getPageResults.getPageResults(next_page);
                }
            });

            prev_btn.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    getPageResults.getPageResults(prev_page);
                }
            });

            /*
            if (prev.isEmpty()){
                prev_btn.setAlpha(chuck_float);
                prev_btn.setOnClickListener(null);
            }
            if (next.isEmpty()){
                next_btn.setAlpha(chuck_float);
                next_btn.setOnClickListener(null);
            }*/


            String display_page = " (Page " + current_page + ")";
            mTextView.setText(display_page);
            String results_array = "";

            if (all_results != null && !all_results.isEmpty()) {
                Log.i(TAG, " Your results: " + all_results.toString());
                Log.i(TAG, " Now formatting your results");


                for (int i=0; i< all_results.size(); i++){
                    int counter = i + 1;
                    String tmp_results = counter + ") " + all_results.get(i).toString();
                    results_array += "<p>" + tmp_results;
                }
                results_array = results_array.replaceAll("<p2>", "<p>&nbsp;&nbsp;&nbsp;");
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
                            getPageResults.getPageResults(page_number);
                            return true;
                        }
                        else {
                            return false;
                        }
                    }

                });

                mWebView.loadDataWithBaseURL("file:///android_asset/", display_results, "text/html", "utf-8", "");

            }


        } // end onPostExecute

    } // end GetQueryResults

}

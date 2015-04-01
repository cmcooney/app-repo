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
import android.text.Html;
import android.text.SpannableStringBuilder;
import android.text.method.LinkMovementMethod;
import android.text.method.ScrollingMovementMethod;
import android.text.style.URLSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
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

public class FullDefinitionFragment extends Fragment {

    public String TAG = "In FullDefinitionFragment";
    ProgressDialog dialog;
    public Context context;
    GetPageResults getPageResults;
    public String query;
    public int results_count;
    public Activity activity;
    public WebView mWebView;


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
        View view = inflater.inflate(R.layout.fullresult, container, false);
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState){
        super.onActivityCreated(savedInstanceState);
        Bundle bundle = this.getArguments();
        String query_uri = bundle.getString("query_uri");
        new GetQueryResults().execute(query_uri);
        final InputMethodManager imm = (InputMethodManager)getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(getView().getWindowToken(),0);
    }

    private class GetQueryResults extends AsyncTask<String, Void, ArrayList> {

        public String query = "";
        public String page = "";

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

            final TextView mTextView;
            final TextView defTextView;
            //final WebView mWebView;

            if (getView() == null) {
                View view = LayoutInflater.from(context).inflate(R.layout.fullresult, null);
                Log.i(TAG, " View was null: " + view.toString());
                mTextView = (TextView) view.findViewById(R.id.hit_count);
                mWebView = (WebView) view.findViewById(R.id.full_def);

                //defTextView = (TextView) view.findViewById(R.id.full_def);
            } else {
                Log.i(TAG, " The View from here: " + getView().toString());
                mTextView = (TextView) getView().findViewById(R.id.hit_count);
                mWebView = (WebView) getView().findViewById(R.id.full_def);
                //defTextView = (TextView) getView().findViewById(R.id.full_def);
            }


            mWebView.setBackgroundColor(0x00000000);
            query = query.replaceAll("%20", " ");
            String countString = context.getResources().getQuantityString(R.plurals.search_results, results_count,
                    new Object[]{results_count, query});
            //String countString = "Full definition for " + query + ":";
            mTextView.setText(countString);
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

                /*
                defTextView.setMovementMethod(new ScrollingMovementMethod());
                defTextView.setMovementMethod(LinkMovementMethod.getInstance());
                //defTextView.setLinksClickable(true);
                defTextView.setText(Html.fromHtml(results_array));

                defTextView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (defTextView.getSelectionStart() == -1 && defTextView.getSelectionEnd() == -1){
                            Log.i(TAG, " You didn't touch the link");
                        }
                        else{
                            Log.i(TAG, "You touched the link!" + page);
                        }

                    }
                });*/

                /*
                String results_array = all_results.toString();
                */

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


            /*
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
            */



        } // end onPostExecute

    } // end GetQueryResults

    protected void makeLinkClickable(SpannableStringBuilder strBuilder, final URLSpan span) {
        int start = strBuilder.getSpanStart(span);
        int end = strBuilder.getSpanEnd(span);
        int flags = strBuilder.getSpanFlags(span);



    }
}

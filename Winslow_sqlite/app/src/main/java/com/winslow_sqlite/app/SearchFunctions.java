package com.winslow_sqlite.app;


import android.app.Activity;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewConfiguration;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import java.lang.reflect.Field;
import java.util.concurrent.ExecutionException;

//import android.widget.SearchView;

public class SearchFunctions extends ActionBarActivity implements OnScrollListener,
    SearchResultFragment.GetFullDef,
    FullDefinitionFragment.GetPageResults,
    GetPageFragment.GetPageResults,
    OfflineResultFragment.GetFullOfflineDef {

    public Activity activity;
    private static final String TAG = "Search Functions";
    public boolean online_search = true;
    public boolean latin_display;
    public boolean hide_latin_search = false;
    public MenuItem connectIcon;
    public String display = "utf8def";
    //public String uri_authority = "http://dsalsrv01.uchicago.edu/";
    public String uri_authority = "http://dsalsrv02.uchicago.edu/";
    public String cgi_dir = "cgi-bin/chuck/";
    public String query_script = "winslow_query.py";

    RadioGroup radioSearchGroup;
    RadioButton genericRadioButton;
    private RadioButton headword_search, fulltext_search;
    public String query;

    DisplayMessages dm;
    ConnectionDetector cd;
    UrlConnect url_con;
    GetResults get_res;
    DictionaryDb db;

    Boolean isInternetPresent = false;

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        cd = new ConnectionDetector(getApplicationContext());
        dm = new DisplayMessages(getApplicationContext());
	    db = new DictionaryDb(getApplicationContext());
        url_con = new UrlConnect(getApplicationContext());
	    db.createDataBase();
        isInternetPresent = cd.isConnectingToInternet();

        if (!isInternetPresent) {
            dm.showAlertDialog(this, "No internet connection",
                    "Search using offline mode.", false);
            }
        else {

            url_con.execute();

            try {
                if (!url_con.get()) {
                    dm.showAlertDialog(this, "Remote server is not responding.",
                            "Search using offline mode.", false);
                }
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (ExecutionException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        Log.i(TAG, "Completed network checks.");


        // Keeping this just in case, a hack to force display of
        // the overflow menu at all times. From:
        //
        //http://stackoverflow.com/questions/9286822/how-to-force-use-of-overflow-menu-on-devices-with-menu-button
        //http://stackoverflow.com/questions/20444596/how-to-force-action-bar-overflow-icon-to-show
        //http://stackoverflow.com/questions/9739498/android-action-bar-not-showing-overflow
        //
        /*
        try {
            ViewConfiguration config = ViewConfiguration.get(this);
            Field menuKeyField = ViewConfiguration.class.getDeclaredField("sHasPermanentMenuKey");
            if(menuKeyField != null) {
                menuKeyField.setAccessible(true);
                menuKeyField.setBoolean(config, false);
            }
        } catch (Exception ex) {
            // Ignore
        } */

        addListenerOnButton();
        Log.i(TAG, "Added listener button.");

    }

    @Override
    protected void onNewIntent(Intent intent) {
        handleIntent(intent);
        Log.i(TAG, "In onNewIntent.");
    }

    private void handleIntent(Intent intent) {
        Log.i(TAG, "In the handleIntent.");

        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            // handles a search query
            Log.i(TAG, "Got my search term.");
            String query = intent.getStringExtra(SearchManager.QUERY);
            showResults(query);
        }
        //else if (Intent.ACTION_VIEW.equals(intent.getAction())){
        //    Uri data = intent.getData();
        //    Log.i(TAG, " In Action_View: " + data.toString());
        //}
    }

    public void addListenerOnButton() {

        Log.i(TAG, "In addListenerOnButton.");

        // checking radio button status without click //

        radioSearchGroup = (RadioGroup) findViewById(R.id.search_type);
        int selectedID = radioSearchGroup.getCheckedRadioButtonId();
        genericRadioButton = (RadioButton) findViewById(selectedID);

        String button_value = genericRadioButton.toString();
        Log.e(TAG, "Starting button value is " + button_value);

        if (genericRadioButton.findViewById(R.id.radio_fulltext) != null) {
            //Log.e(TAG, "Fulltext search is checked coming in...");
            dm.setQuerySelector(false);
        }

        else {
            //Log.e(TAG, "Headword search is checked coming in...");
            dm.setQuerySelector(true);
        }

        // code to handle clicks //

        headword_search = (RadioButton) findViewById(R.id.radio_headword);
        fulltext_search = (RadioButton) findViewById(R.id.radio_fulltext);

        fulltext_search.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                //Log.e(TAG, "Made it to fulltext_search");
                Toast.makeText(SearchFunctions.this,fulltext_search.getText(),
                        Toast.LENGTH_SHORT).show();
                dm.setQuerySelector(false);
            }
        });

        headword_search.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                //Log.e(TAG, "Made it to headword_search");
                Toast.makeText(SearchFunctions.this,headword_search.getText(),
                        Toast.LENGTH_SHORT).show();
                dm.setQuerySelector(true);
            }
        });
    }

    public void showResults(String query) {
        //Log.e(TAG, "query from showResults -->" + query);

        // check radio button status //
        addListenerOnButton();
        get_res = new GetResults(this, activity);
        String qs = query;

        qs = qs.replaceAll(" ", "%20");

        Log.i(TAG, "query from showResults -->" + query);

        Boolean fs_boo = dm.search_headwords;
        String fs = fs_boo.toString();

        Log.i(TAG, "Am I getting selector??? -->" + fs);
        Log.i(TAG, " Items selected values: online_search == " + online_search + " latin display == " + latin_display);

        final String query_uri;
        //String query_uri = "http://pantagruel.ci.uchicago.edu/cgi-bin/chuck/littre_query.py?qs=" + qs + "&fs=" + fs;

	    if (online_search) {
            Log.i(TAG, " Querying remote server.");
            if (fs_boo) {
                query_uri = uri_authority + cgi_dir + query_script + "?qs=" + qs + "&display=" + display + "&searchhws=yes&format=json";
            }
            else {
                query_uri = uri_authority + cgi_dir + query_script + "?qs=" + qs + "&display=" + display + "&format=json";
            }

            Log.i(TAG, "Search URI --> " + query_uri);

            // make it all happen right here -->
            Bundle bundle = new Bundle();
            bundle.putString("query_uri", query_uri);
            Fragment fr;
            fr = new SearchResultFragment();
            fr.setArguments(bundle);
            FragmentManager fm = getSupportFragmentManager();
            FragmentTransaction fragTransaction = fm.beginTransaction();
            fragTransaction.addToBackStack(null);
            fragTransaction.replace(R.id.text, fr, "text");
            fragTransaction.commit();
	    }

	    else {
            Log.i(TAG, " Going offline! ");
            if (fs_boo){
                query = "head<cmc>" + query;
            }
            else {
                query = "full<cmc>" + query;
            }
            Log.i(TAG, " Offline query: " + query);
            Bundle bundle = new Bundle();
            bundle.putString("query", query);
            Fragment fr;
            fr = new OfflineResultFragment();
            fr.setArguments(bundle);
            FragmentManager fm = getSupportFragmentManager();
            FragmentTransaction fragTransaction = fm.beginTransaction();
            fragTransaction.addToBackStack(null);
            fragTransaction.replace(R.id.text, fr, "text");
            fragTransaction.commit();
        }
	}

    public void getFullDef(String fulldef_query_term){
        Bundle bundle = new Bundle();
        String query_uri = uri_authority + cgi_dir + query_script + "?qs=" + fulldef_query_term + "&display=" + display +
                "&searchhws=yes&matchtype=exact&getfull=yes&format=json";
        bundle.putString("query_uri", query_uri);
        Fragment fr;
        fr = new FullDefinitionFragment();
        fr.setArguments(bundle);
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction fragTransaction = fm.beginTransaction();
        fragTransaction.addToBackStack(null);
        fragTransaction.replace(R.id.text, fr, "text");
        fragTransaction.commit();
    }

    public void getPageResults(String page_no){
        Bundle bundle = new Bundle();
        String query_uri = uri_authority + cgi_dir + query_script + "?page=" + page_no +
                "&display=" + display + "&format=json";
        bundle.putString("query_uri", query_uri);
        Fragment fr;
        fr = new GetPageFragment();
        fr.setArguments(bundle);
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction fragTransaction = fm.beginTransaction();
        fragTransaction.addToBackStack(null);
        fragTransaction.replace(R.id.text, fr, "text");
        fragTransaction.commit();
    }

    public void getFullOfflineDef(String fullOfflineQuery){
        Log.i(TAG, " Offline query: " + fullOfflineQuery);
        Bundle bundle = new Bundle();
        bundle.putString("query", fullOfflineQuery);
        Fragment fr;
        fr = new FullOfflineResultFragment();
        fr.setArguments(bundle);
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction fragTransaction = fm.beginTransaction();
        fragTransaction.addToBackStack(null);
        fragTransaction.replace(R.id.text, fr, "text");
        fragTransaction.commit();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.options_menu, menu);
        MenuItem searchItem = menu.findItem(R.id.search);
        SearchManager searchManager = (SearchManager)getSystemService(Context.SEARCH_SERVICE);
        Log.i(TAG, " your searchManager:" + searchManager.toString());
        Log.i(TAG, " your menu:" + menu.toString());
        SearchView searchView = (SearchView) MenuItemCompat.getActionView(searchItem);
        //SearchView searchView = (SearchView) MenuItemCompat.getActionView(menu.findItem(R.id.search));
        Log.i(TAG, " your searchView: " + searchView.toString());
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        connectIcon = menu.findItem(R.id.connection_status);
        updateIcon(true);

        return super.onCreateOptionsMenu(menu);
        //return true;
    }


    public void updateIcon(boolean online) {
        if (online){
            //connectIcon.setVisible(true);
            connectIcon.setIcon(R.drawable.ic_action_web_site);
        }
        else {
            //connectIcon.setVisible(false);
            connectIcon.setIcon(R.drawable.ic_action_web_site_dark);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection

        switch (item.getItemId()) {
            case R.id.search:
                String chuck_string = item.toString();
                //String another_string = item.getIntent().toString();

                //handleIntent(getIntent());
                //search = (EditText) item.getActionView();
                //String search_string = search.toString();
                Log.i(TAG, "I'm trying to execute a search. " + chuck_string);
                //handleIntent(getIntent());
                return true;
            case R.id.online_search:
                if (item.isChecked()) {
                    item.setChecked(false);
                    online_search = true;
                    updateIcon(true);
                }
                else {
                    item.setChecked(true);
                    online_search = false;
                    Log.i(TAG, " What's the status?");
                    updateIcon(false);
                }
                return true;
            case R.id.latin_display:
                if (item.isChecked()) {
                    item.setChecked(false);
                    latin_display = false;
                    display = "utf8def";
                } else {
                    item.setChecked(true);
                    latin_display = true;
                    display = "romutfdef";
                }
                return true;
	        case R.id.info:
                displayInfoDialog("about");
                return true;
            }
        //return false;
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu){

        MenuItem online_search = menu.findItem(R.id.online_search);
        MenuItem latin_search = menu.findItem(R.id.latin_display);
        MenuItem connection_icon = menu.findItem(R.id.connection_status);
        if (online_search.isChecked()){
            Log.i(TAG, " Search offline is checked");
            latin_search.setVisible(false);
            updateIcon(false);
            //connection_icon.setVisible(false);
        }
        else {
            Log.i(TAG, " Searching online");
            latin_search.setVisible(true);
            updateIcon(true);
            //connection_icon.setChecked(true);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    public void displayInfoDialog(String info_string){
        Log.i(TAG, "in displayInfoDialog: " + info_string);
        String file_name = info_string + ".html";
        Bundle bundle = new Bundle();
        bundle.putString("file_name", file_name);
        Fragment fr;
        fr = new InfoFragment();
        fr.setArguments(bundle);
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction fragTransaction = fm.beginTransaction();
        fragTransaction.addToBackStack(null);
        fragTransaction.replace(R.id.text, fr, "text");
        fragTransaction.commit();
    }


    @Override
    public void onScroll(AbsListView view, int firstVisibleItem,
                         int visibleItemCount, int totalItemCount) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {
        // TODO Auto-generated method stub

    }
}

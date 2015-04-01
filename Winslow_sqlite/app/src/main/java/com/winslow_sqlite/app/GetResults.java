package com.winslow_sqlite.app;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONObject;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.text.Html;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class GetResults extends AsyncTask<String, Void, File> implements OnScrollListener {
	private static final String TAG = "GetResults";
	public String query = "";
	public String return_results = "";
	
	public ArrayList<String> total_hits;
	public DisplayResultsAdapter outAdapter;
	public int chuck_file_count = 0;
	public int new_file_count = 0;
	int results_count;
	public Activity a;
	private Context context;
	public File[] files;
	public File dir;
	public File [] new_files;
	
	private ListView mListView;
	private TextView mTextView;
	
	public FileWriter f_writer;
	
    //Locale locale = new Locale("fr");
    //Configuration config = new Configuration();
    
	public Exception exception;
	
    ProgressDialog dialog;
    
    int first_hit, last_hit = 0;
	
	public GetResults(Context context, Activity a) {
		this.context = context;
		this.a = a;
		
    	//Locale.setDefault(locale);
    	//config.locale = locale;
	}
	
	@Override
	protected void onPreExecute() {
		dialog = new ProgressDialog(context);
		dialog.setMessage("Retrieving results.");
		dialog.show();
	}
	
	@Override
	protected File doInBackground(String... urls) {
		BufferedReader reader = null;
		File file = null;
		
		try {
			if (context.getCacheDir().exists()) {
				   Log.i(TAG, "In cleanDir. Cleaning up " + context.getCacheDir().toString());
			   }
			   else {
				   Log.i(TAG, "Not finding the cache dir...");
			   }
				File cachedir = context.getCacheDir();
				File[] files = cachedir.listFiles();
				
				for (File existing_file : files) {
					Log.i(TAG, "Cleaning file: " + existing_file.toString());
					existing_file.delete();
					
				}
			}
		
		catch (Exception e) {
			Log.i(TAG, e.toString());
			}
		
		try {
			String search_URI = urls[0];
			Pattern p = Pattern.compile("fs=([^&]*)&");
			Matcher m = p.matcher(search_URI);
			while (m.find()){
				query = m.group(1);
			}
			URI search_query = new URI(urls[0]);
			DefaultHttpClient httpclient = new DefaultHttpClient();
			HttpGet httpget = new HttpGet(search_query);
			
			String get_string = httpget.toString();
			
			Log.i(TAG, "HTTP query:  " + query + " " + get_string);
			
			HttpResponse response = httpclient.execute(httpget);
            HttpEntity entity = response.getEntity();
            InputStream content = entity.getContent();
            
            // read results into buffer //
            try {
            	reader = new BufferedReader(new InputStreamReader(content));
            	String line = "";
            	
            	Pattern count_regex = Pattern.compile("<count>([^<]*)</count>");
            	
            	int ticker = 0;
            	int file_ticker = 0;
            	int counter = 0;
            	file = File.createTempFile("winslow_results", ".tmp", context.getCacheDir());

                String out_string = "";
                ArrayList<String> all_results = new ArrayList<String>();

            	while ((line = reader.readLine()) != null) {
                    //Log.i(TAG, " your results: " + line);
            		ticker++;
            		Matcher count_match = count_regex.matcher(line);
            		if (ticker == 1) {
            			f_writer = new FileWriter(file, true);
            			//Log.i(TAG, "New FileWriter!");
            			}
            		
            		if (count_match.find()) {
            			String incoming_count = count_match.group(1);
                		results_count = Integer.parseInt(incoming_count.toString());
                		Log.i(TAG, "Results count == " + results_count);
            			}
            		else {

                        JSONArray jsonArray = new JSONArray(line.toString());
            			//counter++;
                        for (int i= 0; i< jsonArray.length(); i++) {
                            counter = 1 + i;
                            JSONObject jsonObject = jsonArray.getJSONObject(i);
                            String hw = jsonObject.getString("hw");
                            //hw = hw.toUpperCase(locale);
                            String def = jsonObject.getString("def");
                            //Log.i(TAG, " headword: " + hw);
                            //Log.i(TAG, " def: " + def);
                            def.replaceAll("\n", "<br>");
                            out_string = counter + ") " + hw + " " + def + "\n";
                            all_results.add(out_string);
                        }
	            		//Log.i(TAG, "Convert the json right off the bat: " + out_string);
	            		
	            		f_writer.write(all_results.toString());
                		}
            		
           			if (ticker == 101) {
           				//multi_file = true;
           				//Log.i(TAG, "My string: " + sb.toString());
           				ticker = 0;
           				file_ticker++;
           				String res_extension = ".tmp" + file_ticker;
           				//Log.i(TAG, "Resetting ticker.");
           				f_writer.flush();
           				f_writer.close();
           				file = File.createTempFile("winslow_results", res_extension, context.getCacheDir());
           				}
           		
            		}
            	f_writer.flush();
            	f_writer.close();
            	dialog.dismiss();
            	}
            catch (IOException exception) {
            	Log.i(TAG, "Here? IOException --> " + exception.toString());
            	}
            // pro-forma cleanup //
            finally {
            	if (reader != null) {
            		try {
            			reader.close();
            		}
            		catch (IOException exception) {
            			Log.i(TAG, "IOException --> " + exception.toString());
            		}
            	}
            }
		}
		// Exception for problems with HTTP connection //
		catch (Exception exception) {
            Log.i(TAG, "Trouble connecting -->" + exception.toString());
            return null;
		}
		
		return file;
		
	}
	
	protected void onPostExecute(File file) {
		
		a = (Activity) context;
		
		File dir = context.getCacheDir();
		File[] files = dir.listFiles();
		
		int file_count = files.length;
		//Log.i(TAG, "File count == " + file_count);
		
		// do I even need this for actual devices????
		
		for (int i = 0; i < file_count; i++) {
			for (int j = i+1; j < file_count; j++) {
				if (files[i].length() == files[j].length()) {
					files[j].delete();
				}
				else {
					//Log.i(TAG, "Ha! These files are different: " + files[i].getName() + " & " + files[j].getName());
				}
			}
		}
		
		if (dialog.isShowing()){
			dialog.dismiss();
		}
		new_files = dir.listFiles();
		
		mTextView = (TextView) a.findViewById(R.id.hit_count);
		mListView = (ListView) a.findViewById(R.id.results_list);

		query = query.replaceAll("%20", " ");
		String countString = a.getResources().getQuantityString(R.plurals.search_results, results_count,
				new Object[] {results_count, query});
		mTextView.setText(countString);
		
		try {
			total_hits = getFileContents(0);
            Log.i(TAG, " hits: " + total_hits.toString());
			outAdapter = new DisplayResultsAdapter (context, R.layout.result, total_hits);
			mListView.setOnScrollListener(this);
			mListView.setAdapter(outAdapter);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		mListView.setOnItemClickListener(new OnItemClickListener() {
			@Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            
        // Send the single definition to Walt's display function...//
                                    
                    String single_definition = mListView.getItemAtPosition(position).toString();
                    final Context context = view.getContext();
                    showDefinition(single_definition, context );
                    }
            });
		
	}
	
	// Pop up display //
	
	public void showDefinition ( String single_definition, Context context) {
	    	
		//Log.i(TAG, "In the Toasty activity: " + single_definition);
	    //Typeface tf = Typeface.createFromAsset(context.getAssets(), "fonts/FreeSans.ttf");
	        
	    final Dialog dialog = new Dialog(context);
	    dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

	    dialog.setContentView(R.layout.toasty_dialog);

	    dialog.getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL|WindowManager.LayoutParams.DIM_AMOUNT_CHANGED, WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL);
	    dialog.getWindow().getAttributes().dimAmount = 0;
	        
	    final Button b = (Button) dialog.findViewById(R.id.toasty_button);
	    b.setMovementMethod(new ScrollingMovementMethod()); 
	    b.setText(Html.fromHtml(single_definition));
	    //b.setTypeface(tf);
	        
	    // If you touch the dialog then it will exit
	    b.setOnLongClickListener(new Button.OnLongClickListener() {
	        public boolean onLongClick(final View v) {
	          		b.setBackgroundColor(0xffde5800);
	                dialog.dismiss();
	                return true;
	        } 
	            
	    });

	    dialog.setCanceledOnTouchOutside(true);
	    dialog.show();
		
	}
	
	 
	public ArrayList<String> getFileContents(int file_number) throws IOException {
		String line = "";
		InputStreamReader r = null;
		BufferedReader file_reader = null;
		FileInputStream fis =  null;
		
		ArrayList<String> out_array = new ArrayList<String>();
		
		try {
			fis = new FileInputStream(new_files[file_number]);
			r = new InputStreamReader(fis);
			file_reader = new BufferedReader(r);
				
			while ((line = file_reader.readLine()) != null) {
				out_array.add(new String(line));
				}
					
			file_reader.close();
			fis.close();
				
		} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
		}
		
		return out_array;
		
	}
		
		
	@Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {
            // TODO Auto-generated method stub
            try {
            	dialog.dismiss();
            } catch (Exception e) {
            	// nothing
            }
    }

	@Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
			firstVisibleItem = mListView.getFirstVisiblePosition();
			int action_number = 0;
			
			action_number = ((chuck_file_count + 1) * 100) - 20;
			
			//Log.i(TAG, "Current file == " + chuck_file_count + "; Number of files: " + new_files.length + "; Action number == " + action_number + "; First Visible == " + firstVisibleItem);
			int math_file_number = new_files.length - 1;
			
			if (firstVisibleItem == action_number && chuck_file_count < math_file_number) {	
				/*
				int first_hit = current_file * 100;
				int last_hit = first_hit + 100;
				if (last_hit > results_count) {
					last_hit = results_count;
				}
				Log.i(TAG, "First hit: " + first_hit + "; Last hit: " + last_hit);*/
				Toast.makeText(context.getApplicationContext(), "Loading next set of results.", Toast.LENGTH_LONG).show();
				
				chuck_file_count++;
				try {
					total_hits.addAll(getFileContents(chuck_file_count));
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				outAdapter.notifyDataSetChanged();
			}
          
    }

}

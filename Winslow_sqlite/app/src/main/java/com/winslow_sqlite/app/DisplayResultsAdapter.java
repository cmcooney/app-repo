package com.winslow_sqlite.app;

import java.util.ArrayList;

import android.content.Context;
import android.graphics.Typeface;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class DisplayResultsAdapter extends ArrayAdapter<String>{
	
	private static final String TAG = "Display Results";
	public String out_view;
	private ArrayList<String> def_items;
	//private String[] def_items;
	private Context context;
	private Typeface tf;
	
	//public DisplayResultsAdapter(Context context, int textViewResourceId, String[] def_items){
	public DisplayResultsAdapter(Context context, int textViewResourceId, ArrayList<String> def_items){	
		super(context, textViewResourceId, def_items);
		this.context = context;
		this.def_items = def_items;
	}

	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View view = convertView;
		ViewHolder holder = null;
		
		//String out_view = null;
		//String holder_view = null;
        //Log.i(TAG, " In DisplayResultsAdapter");
		String def_item = def_items.get(position);
        def_item = def_item.replaceFirst(",", "");
        def_item = def_item.replace("<p2>", "<br>");
		//String def_item = def_items[position];
		
		// http://www.ezzylearning.com/tutorial.aspx?tid=1763429 //
		
		
		if (view == null) {
			//Log.e(TAG, "Another null view...");
			tf = Typeface.createFromAsset(context.getAssets(), "FreeSans.ttf");
			LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			view = inflater.inflate(R.layout.word, null);
			holder = new ViewHolder();
			view.setTag(holder);
		}
		
		else{
			holder = (ViewHolder)view.getTag();
			//holder_view = holder.toString();
			//Log.e(TAG, "Have a view == " + holder_view);
			
		}
		
		if (def_item != null) {
			holder.headword_view = (TextView) view.findViewById(R.id.word);
			holder.headword_view.setText(Html.fromHtml(def_item));
			//holder.headword_view.setText(def_item);
			holder.headword_view.setTypeface(tf);
			
			out_view = holder.headword_view.toString();
			//Log.e(TAG, "In def_items, building view with " + out_view);
		}
		
		/*
		if (def != null) {
			viewHolder.def_view = (TextView) view.findViewById(R.id.definition);
			viewHolder.def_view.setText(Html.fromHtml(def));
			viewHolder.def_view.setTypeface(tf);
			out_view.concat(viewHolder.def_view.toString());
		}*/
			//Log.e(TAG, "Headword View == " + out_view);
		
		return view;
	}
	
	static class ViewHolder {
		TextView headword_view;
		//TextView def_view;
	}

}

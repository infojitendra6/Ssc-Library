package com.artifex.mupdfdemo;

import java.util.ArrayList;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import com.kopykitab.ereader.settings.Constants;
import com.kopykitab.ereader.R;

public class OutlineAdapter extends BaseAdapter implements Filterable {
	private ArrayList<OutlineItem> mItems = new ArrayList<OutlineItem>(), mFilteredItems = new ArrayList<OutlineItem>();
	private final LayoutInflater mInflater;
	private boolean isNightMode;
	
	public OutlineAdapter(LayoutInflater inflater, OutlineItem items[], boolean isNightMode) {
		mInflater = inflater;
		for(OutlineItem tempItem : items)	{
			mItems.add(tempItem);
		}
		mFilteredItems = mItems;
		this.isNightMode = isNightMode; 
	}

	public int getCount() {
		return mFilteredItems.size();
	}

	public OutlineItem getItem(int position) {
		return mFilteredItems.get(position);
	}

	public long getItemId(int id) {
		return id;
	}

	public View getView(int position, View convertView, ViewGroup parent) {
		View v = convertView;
		if (convertView == null) {
			v = mInflater.inflate(R.layout.outline_entry, parent, false);
		}
		int level = mFilteredItems.get(position).level;
		if (level > 8) level = 8;
		String space = "";
		for (int i=0; i<level;i++)
			space += "   ";
		TextView titleView = (TextView) v.findViewById(R.id.title);
		titleView.setText(space+mFilteredItems.get(position).title);
		titleView.setTextColor(isNightMode ? Color.WHITE : Color.BLACK);
		
		TextView pageNumberView = (TextView) v.findViewById(R.id.page);
		pageNumberView.setText(String.valueOf(mFilteredItems.get(position).page+1));
		pageNumberView.setTextColor(isNightMode ? Color.WHITE : Color.BLACK);
		
		return v;
	}

	@Override
	public Filter getFilter() {
		// TODO Auto-generated method stub
		return new OutlineFilter();
	}
	
	private class OutlineFilter extends Filter {

		@Override
		protected FilterResults performFiltering(CharSequence searchText) {
			// TODO Auto-generated method stub
			FilterResults filteredResult = new FilterResults();
			int totalItems = mItems.size();
			if (searchText == null || searchText.length() <= 0) {
				filteredResult.values = mItems;
				filteredResult.count = totalItems;
			} else {
				ArrayList<OutlineItem> tempItems = new ArrayList<OutlineItem>();
				for(OutlineItem tempItem : mItems)	{
					if (tempItem.title.toLowerCase().contains(searchText.toString().toLowerCase())) {
						tempItems.add(tempItem);
					}
				}

				filteredResult.values = tempItems;
				filteredResult.count = tempItems.size();
			}

			return filteredResult;
		}

		@Override
		protected void publishResults(CharSequence searchText, FilterResults filteredResult) {
			// TODO Auto-generated method stub
			if(filteredResult.count <= 0)	{
				Constants.showToast("No Index found for keyword '"+searchText.toString()+"'", mInflater.getContext());
			}
			mFilteredItems = (ArrayList<OutlineItem>) filteredResult.values;
			notifyDataSetChanged();
		}
	}
}
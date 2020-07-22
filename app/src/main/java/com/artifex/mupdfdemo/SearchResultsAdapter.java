package com.artifex.mupdfdemo;

import com.kopykitab.ereader.components.ClearableEditText;
import com.kopykitab.ereader.settings.Constants;
import com.kopykitab.ereader.R;

import android.content.Context;
import android.graphics.RectF;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ViewFlipper;

public class SearchResultsAdapter extends BaseAdapter implements OnScrollListener{

	private Context mContext;
	private SparseArray<RectF[]> searchResults;
	private boolean isLoadingMoreResults = false;
	private MuPDFCore mCore;
	private int loadedUptoPage = 0;
	private PopoverView popoverView;
	private ViewFlipper searchPopoverFlip;
	private TextView searchProgressText;
	private LinearLayout searchProgressLayout;

	public SearchResultsAdapter(Context c, PopoverView popoverView, SparseArray<RectF[]> searchResults, MuPDFCore mCore) {
		mContext = c;
		this.searchResults = searchResults;
		this.mCore = mCore;
		this.popoverView = popoverView;
		searchPopoverFlip = (ViewFlipper) popoverView.findViewById(R.id.search_popover_flip);
		searchProgressLayout = (LinearLayout) searchPopoverFlip.findViewById(R.id.search_progress_layout);
		searchProgressText = (TextView) searchProgressLayout.findViewById(R.id.search_progress_text);
		setProgressText();
	}

	@Override
	public int getCount() {
		// TODO Auto-generated method stub
		return searchResults.size();
	}

	@Override
	public RectF[] getItem(int position) {
		// TODO Auto-generated method stub
		return searchResults.get(searchResults.keyAt(position));
	}

	@Override
	public long getItemId(int position) {
		// TODO Auto-generated method stub
		return searchResults.keyAt(position);
	}

	public void clear()	{
		searchResults.clear();
		notifyDataSetChanged();
		loadedUptoPage = -1;
	}

	public void setSearchResults(SparseArray<RectF[]> searchResults)	{
		this.searchResults = searchResults;
		notifyDataSetChanged();
	}

	public SparseArray<RectF[]> getSearchResults()	{
		return searchResults;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		// TODO Auto-generated method stub
		LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		convertView = inflater.inflate(R.layout.search_list, parent, false);
		TextView searchPageNumber = (TextView) convertView.findViewById(R.id.search_page_number);
		searchPageNumber.setText("Page " + (getItemId(position) + 1));

		TextView searchOccurances = (TextView) convertView.findViewById(R.id.search_occurances);
		searchOccurances.setText(getItem(position).length + " Occurances");

		return convertView;
	}
	
	public void setProgressText()	{
		int fromPage = 1;
		if(loadedUptoPage != -1)	{
			fromPage = loadedUptoPage + 1;
		}
		
		int toPage = loadedUptoPage + Constants.FETCH_SEARCH_TEXT;
		if(toPage > mCore.countPages())	{
			toPage = mCore.countPages();
		}
		
		searchProgressText.setText("Searching " + fromPage + " to " + toPage);
	}

	@Override
	public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
		// TODO Auto-generated method stub
		if(loadedUptoPage != -1)	{			
			if(view.getLastVisiblePosition() == (totalItemCount - 2) && !isLoadingMoreResults && loadedUptoPage <= mCore.countPages())	{
				searchProgressLayout.setVisibility(View.VISIBLE);
				view.smoothScrollToPosition(loadedUptoPage);
			}
			if((firstVisibleItem + visibleItemCount) >= totalItemCount && !isLoadingMoreResults && loadedUptoPage <= mCore.countPages())	{
				if(totalItemCount == 0 && searchProgressLayout.getVisibility() == View.GONE)	{
					searchProgressLayout.setVisibility(View.VISIBLE);
				}
				ClearableEditText searchText = (ClearableEditText) popoverView.findViewById(R.id.search_text);

				if(searchText.getText().toString().length() > 0)	{			
					isLoadingMoreResults = true;				
					
					try {
						searchResults = new SearchTextInDocument().execute(searchText.getText().toString()).get();						
						notifyDataSetChanged();						
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}					
				}
			} else if(loadedUptoPage > mCore.countPages() && getCount() == 0)	{
				searchPopoverFlip.setDisplayedChild(1);
			}
		}
	}

	@Override
	public void onScrollStateChanged(AbsListView view, int scrollState) {
		// TODO Auto-generated method stub

	}
	
	private class SearchTextInDocument extends AsyncTask<String, Integer, SparseArray<RectF[]>>	{
		
		@Override
		protected SparseArray<RectF[]> doInBackground(String... params) {
			// TODO Auto-generated method stub
			return mCore.searchTextAll(params[0], loadedUptoPage, searchResults);
		}		

		@Override
		protected void onPostExecute(SparseArray<RectF[]> result) {
			// TODO Auto-generated method stub
			super.onPostExecute(result);
			loadedUptoPage += Constants.FETCH_SEARCH_TEXT;
			isLoadingMoreResults = false;
			if(searchProgressLayout.getVisibility() == View.VISIBLE && result.size() > 0)	{
				searchProgressLayout.setVisibility(View.GONE);
			}
			setProgressText();
		}
	}
}
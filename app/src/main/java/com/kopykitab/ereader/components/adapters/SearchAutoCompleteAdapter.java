package com.kopykitab.ereader.components.adapters;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import com.kopykitab.ereader.models.StoreSearchItem;
import com.kopykitab.ereader.settings.Constants;
import com.kopykitab.ereader.settings.Utils;

import org.json.JSONArray;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

public class SearchAutoCompleteAdapter extends BaseAdapter implements Filterable {

    private List<String> searchResults;
    private List<StoreSearchItem> searchItems, searchSuggestionsdata;
    private String searchResponse = null;
    private StoreItemClickListener mItemClickListener;

    LayoutInflater inflater;

    public SearchAutoCompleteAdapter(Context context, StoreItemClickListener mItemClickListener) {
        inflater = LayoutInflater.from(context);
        this.mItemClickListener = mItemClickListener;
        searchResults = new ArrayList<String>();
    }

    public SearchAutoCompleteAdapter(Context context, List<StoreSearchItem> searchSuggestionsdata, StoreItemClickListener mItemClickListener) {
        inflater = LayoutInflater.from(context);
        searchResults = new ArrayList<String>();
        this.searchSuggestionsdata = searchSuggestionsdata;
        this.mItemClickListener = mItemClickListener;
    }


    @Override
    public Filter getFilter() {
        Filter filter = new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                FilterResults filterResults = new FilterResults();
                if (!TextUtils.isEmpty(constraint) && constraint.length() >= 3) {
                    // Retrieve the autocomplete results.
                    getSearchResults(constraint.toString());

                    // Assign the data to the FilterResults
                    filterResults.values = searchItems;
                    filterResults.count = searchItems.size();
                } else {
                    // Assign the data to the FilterResults
                    if (searchSuggestionsdata != null) {
                        filterResults.values = searchSuggestionsdata;
                        filterResults.count = searchSuggestionsdata.size();
                    }
                }

                return filterResults;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                if (results.values != null) {
                    searchResults = (ArrayList<String>) results.values;
                    notifyDataSetChanged();
                }
            }
        };
        return filter;
    }

    @Override
    public int getCount() {
        return searchResults.size();
    }

    @Override
    public Object getItem(int position) {
        return searchResults.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        MyViewHolder mViewHolder;

        if (convertView == null) {
            convertView = inflater.inflate(android.R.layout.simple_list_item_1, parent, false);
            mViewHolder = new MyViewHolder(convertView);
            convertView.setTag(mViewHolder);
        } else {
            mViewHolder = (MyViewHolder) convertView.getTag();
        }

        final StoreSearchItem currentListData = (StoreSearchItem) getItem(position);
        mViewHolder.textView.setText(currentListData.getTitle());
        mViewHolder.textView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mItemClickListener.itemClicked(currentListData);
            }
        });

        return convertView;
    }


    private class MyViewHolder {
        android.widget.TextView textView;

        public MyViewHolder(View convertView) {
            textView = (android.widget.TextView) convertView.findViewById(android.R.id.text1);
        }
    }

    /**
     * Returns a search result for the given keyword.
     */
    private void getSearchResults(String searchText) {
        List<String> searchData = new ArrayList<String>();

        try {
            searchResponse = Utils.sendGet(Constants.LIVE_SEARCH_URL, "term=" + URLEncoder.encode(searchText, "UTF-8"));
            StoreSearchItem.setSearchResults(searchResponse);
            if (searchResponse != null) {
                JSONArray jsonArray = new JSONArray(searchResponse);
                searchItems = new ArrayList<StoreSearchItem>();
                StoreSearchItem item;
                for (int i = 0; i < jsonArray.length(); i++) {
                    searchData.add(jsonArray.getJSONObject(i).getString("title_text"));
                    String objectType = jsonArray.getJSONObject(i).getString("objectType");
                    String title = jsonArray.getJSONObject(i).getString("title_text");
                    String overviewURL = jsonArray.getJSONObject(i).getString("overviewURL");
                    if (objectType.equals("All")) {
                        StoreSearchItem.setSearchUrl(overviewURL);
                    }
                    String objectCount = jsonArray.getJSONObject(i).getString("objectCount");
                    item = new StoreSearchItem(title, overviewURL, objectCount);
                    searchItems.add(item);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

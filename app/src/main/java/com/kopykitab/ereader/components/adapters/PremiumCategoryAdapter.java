package com.kopykitab.ereader.components.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.kopykitab.ereader.R;

import java.util.List;

public class PremiumCategoryAdapter extends BaseAdapter {

    Context context;
    List<String> premiumCategoryList;
    LayoutInflater inflter;

    public PremiumCategoryAdapter(Context context, List<String> premiumCategoryList) {
        this.context = context;
        this.premiumCategoryList = premiumCategoryList;
        inflter = (LayoutInflater.from(context));
    }

    @Override
    public int getCount() {
        return premiumCategoryList.size();
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        convertView = inflter.inflate(R.layout.premium_spinner_item, null);
        TextView names = (TextView) convertView.findViewById(R.id.textView);
        names.setText(premiumCategoryList.get(position));
        return convertView;
    }
}

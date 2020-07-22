package com.kopykitab.ereader.components.adapters;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.kopykitab.ereader.R;

public class NewFeatureAdapter extends BaseAdapter {

    private Context context;
    private String[] newFeatureHeaderTexts;
    private String[] newFeatureTexts;
    private int[] newFeatureImages;

    public NewFeatureAdapter(Context context, String[] newFeatureHeaderTexts, String[] newFeatureTexts, int[] newFeatureImages) {
        this.context = context;
        this.newFeatureHeaderTexts = newFeatureHeaderTexts;
        this.newFeatureTexts = newFeatureTexts;
        this.newFeatureImages = newFeatureImages;
    }

    @Override
    public int getCount() {
        return newFeatureTexts.length;
    }

    @Override
    public Object getItem(int i) {
        return i;
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {

        ViewHolder viewHolder;

        if (convertView == null) {
            viewHolder = new ViewHolder();
            LayoutInflater inflater = LayoutInflater.from(context);
            convertView = inflater.inflate(R.layout.new_feature_list, parent, false);
            viewHolder.headerFeatureName = (TextView) convertView.findViewById(R.id.new_header_feature_text);
            viewHolder.featureName = (TextView) convertView.findViewById(R.id.new_feature_text);
            viewHolder.icon = (ImageView) convertView.findViewById(R.id.new_feature_image);

            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        viewHolder.headerFeatureName.setText(newFeatureHeaderTexts[position]);
        viewHolder.featureName.setText(Html.fromHtml(newFeatureTexts[position]));
        viewHolder.featureName.setMaxLines(6);
        viewHolder.icon.setImageResource(newFeatureImages[position]);

        return convertView;
    }

    private static class ViewHolder {
        TextView headerFeatureName, featureName;
        ImageView icon;
    }
}

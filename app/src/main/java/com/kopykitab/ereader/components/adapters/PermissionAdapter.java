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

public class PermissionAdapter extends BaseAdapter {

    Context context;
    private final String[] permissionTexts;
    private final int[] permissionImages;

    public PermissionAdapter(Context context, String[] permissionTexts, int[] permissionImages) {
        this.context = context;
        this.permissionTexts = permissionTexts;
        this.permissionImages = permissionImages;
    }

    @Override
    public int getCount() {
        return permissionTexts.length;
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
            convertView = inflater.inflate(R.layout.permission_list, parent, false);
            viewHolder.txtName = convertView.findViewById(R.id.permission_text);
            viewHolder.icon = convertView.findViewById(R.id.permission_image);

            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        viewHolder.txtName.setText(Html.fromHtml(permissionTexts[position]));
        viewHolder.txtName.setMaxLines(6);
        viewHolder.icon.setImageResource(permissionImages[position]);

        return convertView;
    }

    private static class ViewHolder {
        TextView txtName;
        ImageView icon;
    }
}


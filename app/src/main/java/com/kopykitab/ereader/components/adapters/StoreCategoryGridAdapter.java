package com.kopykitab.ereader.components.adapters;

import android.content.Context;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.kopykitab.ereader.R;
import com.kopykitab.ereader.models.StoreCategoryItem;
import com.kopykitab.ereader.models.StoreCategorySection;

import java.util.ArrayList;
import java.util.HashMap;

public class StoreCategoryGridAdapter extends RecyclerView.Adapter<StoreCategoryGridAdapter.ViewHolder> {

    //data array
    private ArrayList<Object> mDataArrayList;

    //context
    private final Context mContext;

    //listeners
    private final StoreItemClickListener mItemClickListener;
    private final StoreCategorySectionListener mStoreCategorySectionListener;

    //view type
    private static final int VIEW_TYPE_SECTION = R.layout.store_category_list_header;
    private static final int VIEW_TYPE_ITEM = R.layout.store_category_list_item; //TODO : change this

    private HashMap<String, String> mCategoryListDetails = new HashMap<String, String>();

    public StoreCategoryGridAdapter(Context context, ArrayList<Object> dataArrayList,
                                    final GridLayoutManager gridLayoutManager, StoreItemClickListener itemClickListener,
                                    StoreCategorySectionListener storeCategorySectionListener, HashMap<String, String> categoryListDetails) {
        mContext = context;
        mItemClickListener = itemClickListener;
        mStoreCategorySectionListener = storeCategorySectionListener;
        mDataArrayList = dataArrayList;
        this.mCategoryListDetails = categoryListDetails;

        gridLayoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                return isSection(position) ? gridLayoutManager.getSpanCount() : 1;
            }
        });
    }

    private boolean isSection(int position) {
        return mDataArrayList.get(position) instanceof StoreCategorySection;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(mContext).inflate(viewType, parent, false), viewType);
    }


    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        switch (holder.viewType) {
            case VIEW_TYPE_ITEM:
                final StoreCategoryItem item = (StoreCategoryItem) mDataArrayList.get(position);
                holder.subCategoryItemTextView.setText(Html.fromHtml(item.getName()));

                //de-active clickable action when item name is emptry
                if (item.getName().equals("")) {
                    holder.subCategoryItemView.setBackgroundResource(R.color.white);
                }

                holder.view.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mItemClickListener.itemClicked(item);
                    }
                });
                break;
            case VIEW_TYPE_SECTION:
                final StoreCategorySection section = (StoreCategorySection) mDataArrayList.get(position);
                holder.categoryTextView.setText(Html.fromHtml(section.getName()));
                String subCategoryText = Html.fromHtml(mCategoryListDetails.get(section.getName())).toString();
                if (subCategoryText.length() > 93) {
                    subCategoryText = subCategoryText.substring(0, 90) + "...";
                }
                if (subCategoryText.length() > 0) {
                    holder.subCategoryTextView.setVisibility(View.VISIBLE);
                    holder.categoryToggleButton.setVisibility(View.VISIBLE);
                }
                holder.subCategoryTextView.setText(subCategoryText);
                holder.categoryTextView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mItemClickListener.itemClicked(section);
                    }
                });
                holder.categoryToggleButton.setChecked(section.isExpanded);
                holder.categoryToggleButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        mStoreCategorySectionListener.onSectionStateChanged(section, isChecked);
                    }
                });
                holder.subCategoryTextView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mStoreCategorySectionListener.onSectionStateChanged(section, !section.isExpanded);
                    }
                });
                break;
        }
    }

    @Override
    public int getItemCount() {
        return mDataArrayList.size();
    }

    @Override
    public int getItemViewType(int position) {
        if (isSection(position))
            return VIEW_TYPE_SECTION;
        else return VIEW_TYPE_ITEM;
    }

    protected static class ViewHolder extends RecyclerView.ViewHolder {

        //common
        View view;
        int viewType;

        //for Category header
        TextView categoryTextView, subCategoryTextView;
        ToggleButton categoryToggleButton;

        //for Sub-Category item
        RelativeLayout subCategoryItemView;
        TextView subCategoryItemTextView;

        public ViewHolder(View view, int viewType) {
            super(view);
            this.viewType = viewType;
            this.view = view;
            if (viewType == VIEW_TYPE_ITEM) {
                subCategoryItemView = (RelativeLayout) view.findViewById(R.id.store_category_list_item);
                subCategoryItemTextView = (TextView) view.findViewById(R.id.store_subcategory_text_item);
            } else {
                categoryTextView = (TextView) view.findViewById(R.id.store_category);
                categoryToggleButton = (ToggleButton) view.findViewById(R.id.store_category_toggle_button);
                subCategoryTextView = (TextView) view.findViewById(R.id.store_sub_category);
            }
        }
    }
}

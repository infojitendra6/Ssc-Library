package com.kopykitab.ereader.components.adapters;

import android.content.Context;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;

import com.kopykitab.ereader.models.StoreCategoryItem;
import com.kopykitab.ereader.models.StoreCategorySection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;

public class StoreCategoryHelper implements StoreCategorySectionListener {

    //data list
    private LinkedHashMap<StoreCategorySection, ArrayList<StoreCategoryItem>> mSectionDataMap = new LinkedHashMap<StoreCategorySection, ArrayList<StoreCategoryItem>>();
    private ArrayList<Object> mDataArrayList = new ArrayList<Object>();

    //section map
    //TODO : look for a way to avoid this
    private HashMap<String, StoreCategorySection> mSectionMap = new HashMap<String, StoreCategorySection>();

    //adapter
    private StoreCategoryGridAdapter mGridAdapter;

    //recycler view
    RecyclerView mRecyclerView;

    private HashMap<String, String> categoryListDetails = new HashMap<String, String>();

    public StoreCategoryHelper(Context context, RecyclerView recyclerView, StoreItemClickListener itemClickListener,
                               int gridSpanCount) {

        //setting the recycler view
        GridLayoutManager gridLayoutManager = new GridLayoutManager(context, gridSpanCount);
        recyclerView.setLayoutManager(gridLayoutManager);
        mGridAdapter = new StoreCategoryGridAdapter(context, mDataArrayList,
                gridLayoutManager, itemClickListener, this, categoryListDetails);
        recyclerView.setAdapter(mGridAdapter);

        mRecyclerView = recyclerView;
    }

    public void notifyDataSetChanged() {
        //TODO : handle this condition such that these functions won't be called if the recycler view is on scroll
        generateDataList();
        mGridAdapter.notifyDataSetChanged();
    }

    public void addSection(String section, String categoryUrl, ArrayList<StoreCategoryItem> items, LinkedList<String> subCategories) {
        StoreCategorySection newSection;
        mSectionMap.put(section, (newSection = new StoreCategorySection(section, categoryUrl)));
        if (categoryListDetails.size() <= 0) {
            newSection.setIsExpanded(true);
        }

        mSectionDataMap.put(newSection, items);

        String subCategoriesString = subCategories.toString();
        categoryListDetails.put(section, subCategoriesString.substring(1, subCategoriesString.length() - 1));
    }

    public void addSection(String section, String categoryUrl, ArrayList<StoreCategoryItem> items, LinkedList<String> subCategories, boolean isExpand) {
        StoreCategorySection newSection;
        mSectionMap.put(section, (newSection = new StoreCategorySection(section, categoryUrl)));
        newSection.setIsExpanded(isExpand);
        mSectionDataMap.put(newSection, items);

        String subCategoriesString = subCategories.toString();
        categoryListDetails.put(section, subCategoriesString.substring(1, subCategoriesString.length() - 1));
    }

    public void addItem(String section, StoreCategoryItem item) {
        mSectionDataMap.get(mSectionMap.get(section)).add(item);
    }

    public void removeItem(String section, StoreCategoryItem item) {
        mSectionDataMap.get(mSectionMap.get(section)).remove(item);
    }

    public void removeSection(String section) {
        mSectionDataMap.remove(mSectionMap.get(section));
        mSectionMap.remove(section);
    }

    private void generateDataList() {
        mDataArrayList.clear();
        for (Map.Entry<StoreCategorySection, ArrayList<StoreCategoryItem>> entry : mSectionDataMap.entrySet()) {
            StoreCategorySection key;
            mDataArrayList.add((key = entry.getKey()));
            if (key.isExpanded)
                mDataArrayList.addAll(entry.getValue());
        }
    }

    @Override
    public void onSectionStateChanged(StoreCategorySection section, boolean isOpen) {
        if (!mRecyclerView.isComputingLayout()) {
            section.isExpanded = isOpen;
            notifyDataSetChanged();
        }
    }
}
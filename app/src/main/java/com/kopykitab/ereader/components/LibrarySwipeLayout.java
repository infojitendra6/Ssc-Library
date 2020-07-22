package com.kopykitab.ereader.components;

import android.content.Context;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.View;
import android.widget.GridView;
import android.widget.ListView;

public class LibrarySwipeLayout extends SwipeRefreshLayout {
	
    private View mTargetView;

    public LibrarySwipeLayout(Context context) {
        super(context);
    }

    public LibrarySwipeLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setTargetView(View targetView) {
        mTargetView = targetView;
    }

    @Override
    public boolean canChildScrollUp() {
        if (mTargetView != null) {
        	if (mTargetView instanceof GridView)	{
        		GridView targetGridView = (GridView) mTargetView; 
	            return (targetGridView.getChildCount() > 0) &&
	                    // And then, the first visible item must not be the first item
	                    ((targetGridView.getFirstVisiblePosition() > 0) ||
	                            // If the first visible item is the first item,
	                            // (we've reached the first item)
	                            // make sure that its top must not cross over the padding top of the wrapped ListView
	                            (targetGridView.getChildAt(0).getTop() < 0));
        	} else if(mTargetView instanceof ListView)	{
        		ListView targetListView = (ListView) mTargetView; 
	            return (targetListView.getChildCount() > 0) &&
	                    // And then, the first visible item must not be the first item
	                    ((targetListView.getFirstVisiblePosition() > 0) ||
	                            // If the first visible item is the first item,
	                            // (we've reached the first item)
	                            // make sure that its top must not cross over the padding top of the wrapped ListView
	                            (targetListView.getChildAt(0).getTop() < 0));        		
        	} else if(mTargetView instanceof RecyclerView)	{
        		RecyclerView targetRecyclerView = (RecyclerView) mTargetView;
        		LinearLayoutManager lm = (LinearLayoutManager) targetRecyclerView.getLayoutManager();
	            return (targetRecyclerView.getChildCount() > 0) &&
	                    // And then, the first visible item must not be the first item
	                    ((lm.findFirstVisibleItemPosition() > 0) ||
	                            // If the first visible item is the first item,
	                            // (we've reached the first item)
	                            // make sure that its top must not cross over the padding top of the wrapped ListView
	                            (targetRecyclerView.getChildAt(0).getTop() < 0));        		
        	} else	{
        		return super.canChildScrollUp();
        	}
        } else {
            // Fall back to default implementation
            return super.canChildScrollUp();
        }
    }
}
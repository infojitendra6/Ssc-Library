package com.kopykitab.ereader.components;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;

import com.kopykitab.ereader.LibraryActivity;
import com.kopykitab.ereader.R;

public class LibraryAutofitRecyclerView extends RecyclerView {
    private GridLayoutManager gridLayoutManager;
    private LinearLayoutManager listLayoutManager;
    private int columnWidth = -1; //default value
    private boolean isGridView = true;

    private Bitmap mShelfBackground;
    private int mShelfWidth;
    private int mShelfHeight;

    public LibraryAutofitRecyclerView(Context context) {
        super(context);
        init(context, null);
    }

    public LibraryAutofitRecyclerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
        load(context, attrs, 0);
    }

    public LibraryAutofitRecyclerView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context, attrs);
        load(context, attrs, defStyle);
    }

    private void init(Context context, AttributeSet attrs) {
        if (attrs != null) {
            int[] attrsArray = {android.R.attr.columnWidth};
            TypedArray array = context.obtainStyledAttributes(attrs, attrsArray);
            columnWidth = array.getDimensionPixelSize(0, -1);
            array.recycle();
        }

        gridLayoutManager = new GridLayoutManager(getContext(), 1);
        listLayoutManager = new LinearLayoutManager(getContext());

        if (LibraryActivity.isListView()) {
            setLayoutManager(listLayoutManager);
            isGridView = false;
        } else {
            setLayoutManager(gridLayoutManager);
            isGridView = true;
        }
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        super.onMeasure(widthSpec, heightSpec);
        if (columnWidth > 0) {
            int spanCount = Math.max(1, getMeasuredWidth() / columnWidth);
            gridLayoutManager.setSpanCount(spanCount);
        }
    }

    private void load(Context context, AttributeSet attrs, int defStyle) {
        final Bitmap shelfBackground = BitmapFactory.decodeResource(context.getResources(), R.drawable.book_shelf);
        if (shelfBackground != null) {
            mShelfWidth = shelfBackground.getWidth();
            mShelfHeight = shelfBackground.getHeight();
            mShelfBackground = shelfBackground;
        }
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        if (isGridView) {
            int count = getChildCount();
            int top = count > 0 ? getChildAt(0).getTop() : 0;
            final int width = getWidth();
            final int height = getHeight();

            for (int x = 0; x < width; x += mShelfWidth) {
                for (int y = top; y < height; y += mShelfHeight) {
                    canvas.drawBitmap(mShelfBackground, x, y, null);
                }
            }
        } else {
            canvas.drawColor(Color.WHITE);
        }

        super.dispatchDraw(canvas);
    }

    @Override
    public void setPressed(boolean pressed) {
        super.setPressed(pressed);
    }

    public void switchToGridView() {
        isGridView = true;
        setLayoutManager(gridLayoutManager);
    }

    public void switchToListView() {
        isGridView = false;
        setLayoutManager(listLayoutManager);
    }
}
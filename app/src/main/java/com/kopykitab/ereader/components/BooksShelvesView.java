package com.kopykitab.ereader.components;

import com.kopykitab.ereader.R;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.widget.GridView;

public class BooksShelvesView extends GridView {
	private Bitmap mShelfBackground;
    private int mShelfWidth;
    private int mShelfHeight;

	public BooksShelvesView(Context context) {
		super(context);
	}

	public BooksShelvesView(Context context, AttributeSet attrs) {
		super(context, attrs);
		load(context, attrs, 0);
	}

	public BooksShelvesView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		load(context, attrs, defStyle);
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
        int count = getChildCount();
        int top = count > 0 ? getChildAt(0).getTop() : 0;
        final int width = getWidth();
        final int height = getHeight();

        for (int x = 0; x < width; x += mShelfWidth) {
            for (int y = top; y < height; y += mShelfHeight) {
                canvas.drawBitmap(mShelfBackground, x, y, null);
            }
        }
        
        super.dispatchDraw(canvas);
    }    

    @Override
    public void setPressed(boolean pressed) {
        super.setPressed(pressed);
    }
}

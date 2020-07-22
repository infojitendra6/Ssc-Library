package com.kopykitab.ereader.components;

import com.kopykitab.ereader.R;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.util.AttributeSet;

public class TextView extends android.widget.TextView {
	
	private boolean addStrike = false;
	
	public TextView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init(attrs);
	}

	public TextView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(attrs);
	}

	public TextView(Context context) {
		super(context);
		init(null);
	}

	private void init(AttributeSet attrs) {
		Typeface myTypeface = Typeface.createFromAsset(getContext().getAssets(), "fonts/Roboto-Regular.ttf");
		if (attrs != null) {			
			TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.EditText);
			String fontName = a.getString(R.styleable.EditText_font_name);
			if (fontName != null && !fontName.isEmpty() && !fontName.equals("")) {
				myTypeface = Typeface.createFromAsset(getContext().getAssets(), "fonts/" + fontName + ".ttf");
			}	
			a.recycle();
		}
		setTypeface(myTypeface);
	}
	
	public void setAddStrike(boolean addStrike)	{
		this.addStrike = addStrike;
		invalidate();
	}

	@Override
	protected void onDraw(Canvas canvas) {
		// TODO Auto-generated method stub
		super.onDraw(canvas);
		if(addStrike)	{
			Paint paint = new Paint();
			paint.setColor(Color.rgb(153, 153, 153));
	        paint.setStrokeWidth(getResources().getDisplayMetrics().density * 1);
	        canvas.drawLine(0, getHeight() / 2, getWidth(), getHeight() / 2, paint);	        
		}
	}
	
	public void setFont(String fontName) {
		Typeface myTypeface = Typeface.createFromAsset(getContext().getAssets(), "fonts/" + fontName + ".ttf");
		setTypeface(myTypeface);
	}
}
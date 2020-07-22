package com.kopykitab.ereader.components;

import com.kopykitab.ereader.R;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.util.AttributeSet;

public class EditText extends android.widget.EditText {
	public EditText(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init(attrs);
	}

	public EditText(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(attrs);
	}

	public EditText(Context context) {
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

	@Override
	public void setBackgroundResource(int resid) {
		// TODO Auto-generated method stub
		int pl = getPaddingLeft();
	    int pt = getPaddingTop();
	    int pr = getPaddingRight();
	    int pb = getPaddingBottom();

	    super.setBackgroundResource(resid);

	    setPadding(pl, pt, pr, pb);
	}
}
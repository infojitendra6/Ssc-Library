package com.kopykitab.ereader.components;

import com.kopykitab.ereader.R;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;
import android.util.AttributeSet;

public class Button extends android.widget.Button {

	private int drawableLeftSize, drawableRightSize, drawableTopSize, drawableBottomSize;

	public Button(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init(attrs);
	}

	public Button(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(attrs);

	}

	public Button(Context context) {
		super(context);
		init(null);
	}

	private void init(AttributeSet attrs) {
		Typeface myTypeface = Typeface.createFromAsset(getContext().getAssets(), "fonts/Roboto-Regular.ttf");
		if (attrs != null) {
			TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.Button);
			String fontName = a.getString(R.styleable.Button_font_name);
			if (fontName != null && !fontName.isEmpty() && !fontName.equals("")) {
				myTypeface = Typeface.createFromAsset(getContext().getAssets(), "fonts/" + fontName + ".ttf");
			}

			drawableLeftSize = a.getInteger(R.styleable.Button_drawable_left_size_in_percent, 0);
			drawableRightSize = a.getInteger(R.styleable.Button_drawable_right_size_in_percent, 0);
			drawableTopSize = a.getInteger(R.styleable.Button_drawable_top_size_in_percent, 0);
			drawableBottomSize = a.getInteger(R.styleable.Button_drawable_bottom_size_in_percent, 0);

			a.recycle();
		}

		setTypeface(myTypeface);
		Drawable[] compoundDrawables = getCompoundDrawables();
		setCompoundDrawables(compoundDrawables[0], compoundDrawables[1], compoundDrawables[2], compoundDrawables[3]);
	}

	public void setFont(String fontName) {
		setTypeface(Typeface.createFromAsset(getContext().getAssets(), "fonts/" + fontName + ".ttf"));
	}

	@Override
	public void setCompoundDrawables(@Nullable Drawable left, @Nullable Drawable top, @Nullable Drawable right, @Nullable Drawable bottom) {
		if (left != null) {
			left.setBounds(0, 0, (int) (left.getIntrinsicWidth() * (drawableLeftSize * 0.01)), (int) (left.getIntrinsicHeight() * (drawableLeftSize * 0.01)));
		}
		if(top != null) {
			top.setBounds(0, 0, (int) (top.getIntrinsicWidth() * (drawableTopSize * 0.01)), (int) (top.getIntrinsicHeight() * (drawableTopSize * 0.01)));
		}
		if (right != null) {
			right.setBounds(0, 0, (int) (right.getIntrinsicWidth() * (drawableRightSize * 0.01)), (int) (right.getIntrinsicHeight() * (drawableRightSize * 0.01)));
		}
		if(bottom != null) {
			bottom.setBounds(0, 0, (int) (bottom.getIntrinsicWidth() * (drawableBottomSize * 0.01)), (int) (bottom.getIntrinsicHeight() * (drawableBottomSize * 0.01)));
		}
		super.setCompoundDrawables(left, top, right, bottom);
	}
}
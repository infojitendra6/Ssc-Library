package com.kopykitab.ereader.components;

import com.kopykitab.ereader.R;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class PasswordEditText extends EditText{
 
    //The image we are going to use for the Clear button
    private Drawable imgEyeButton = getResources().getDrawable(R.drawable.eye_icon);
     
    public PasswordEditText(Context context) {
        super(context);
        init();
    }
 
    public PasswordEditText(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }
 
    public PasswordEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }
     
    void init() {
         
        // Set bounds of the Clear button so it will look ok
    	imgEyeButton.setAlpha(128);
    	imgEyeButton.setBounds(0, 0, imgEyeButton.getIntrinsicWidth(), imgEyeButton.getIntrinsicHeight());
 
        // There may be initial text in the field, so we may need to display the  button
        handleClear();
 
        //if the Close image is displayed and the user remove his finger from the button, clear it. Otherwise do nothing
        this.setOnTouchListener(new OnTouchListener() {

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				// TODO Auto-generated method stub
				PasswordEditText et = PasswordEditText.this;
				 
                if (et.getCompoundDrawables()[2] == null)
                    return false;
                 
                if (event.getAction() != MotionEvent.ACTION_UP)
                    return false;
                 
                if (event.getX() > et.getWidth() - et.getPaddingRight() - imgEyeButton.getIntrinsicWidth()) {
                    if(getTransformationMethod() instanceof PasswordTransformationMethod)	{
                    	imgEyeButton.setAlpha(255);
                    	setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                    } else	{
                    	imgEyeButton.setAlpha(128);
                    	setTransformationMethod(PasswordTransformationMethod.getInstance());
                    }
                }
                return false;
			}
        });
 
        //if text changes, take care of the button
        this.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
 
            	PasswordEditText.this.handleClear();
            }
 
            @Override
            public void afterTextChanged(Editable arg0) {
            }
 
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }
        });
    }
     
    void handleClear() {
        if (this.getText().toString().equals(""))
        {
            // add the clear button
            this.setCompoundDrawables(this.getCompoundDrawables()[0], this.getCompoundDrawables()[1], null, this.getCompoundDrawables()[3]);
        }
        else
        {
            //remove clear button
            this.setCompoundDrawables(this.getCompoundDrawables()[0], this.getCompoundDrawables()[1], imgEyeButton, this.getCompoundDrawables()[3]);
        }
    }
}
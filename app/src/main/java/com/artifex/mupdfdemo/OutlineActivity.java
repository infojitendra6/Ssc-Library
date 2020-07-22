package com.artifex.mupdfdemo;

import com.kopykitab.ereader.R;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.ViewAnimator;

public class OutlineActivity extends Activity {
	OutlineItem mItems[];

	private ViewAnimator topBarSwitcher;
	private ListView outlineListView;
	private TextView bookName;
	private EditText outlineSearchText;
	private InputMethodManager imm;
	private OutlineAdapter outlineAdapter;
	private boolean isNightMode = false;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.outline_list);

		isNightMode = OutlineActivityData.getNightMode();
		imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);

		topBarSwitcher = (ViewAnimator) findViewById(R.id.outline_topbar_switcher);
		topBarSwitcher.setBackgroundResource(isNightMode ? android.R.color.black : android.R.color.white);
		topBarSwitcher.getChildAt(0).setBackgroundResource(isNightMode ? R.color.night_mode_background_color : R.color.day_mode_background_color);
		RelativeLayout headingLayout = (RelativeLayout) findViewById(R.id.outline_heading);
		headingLayout.setBackgroundResource(isNightMode ? android.R.color.black : android.R.color.white);
		for(int i=0; i < headingLayout.getChildCount(); i++) {
			((TextView) headingLayout.getChildAt(i)).setTextColor(isNightMode ? Color.WHITE : Color.BLACK);
		}
		outlineListView = (ListView) findViewById(R.id.outline_results);
		outlineListView.setBackgroundResource(isNightMode ? android.R.color.black : android.R.color.white);
		bookName = (TextView) findViewById(R.id.docNameText);
		outlineSearchText = (EditText) findViewById(R.id.outline_search_text);

		topBarSwitcher.setInAnimation(AnimationUtils.loadAnimation(OutlineActivity.this, R.anim.slide_in_up));
		topBarSwitcher.setOutAnimation(AnimationUtils.loadAnimation(OutlineActivity.this, R.anim.slide_out_up));

		mItems = OutlineActivityData.get().items;
		setBookName();
		outlineAdapter = new OutlineAdapter(getLayoutInflater(), mItems, isNightMode);
		outlineListView.setAdapter(outlineAdapter);
		outlineListView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				// TODO Auto-generated method stub
				OutlineItem tempItem = ((OutlineAdapter) parent.getAdapter()).getItem(position);
				setResult(tempItem.page);
				onBackPressed();
			}
		});

		outlineSearchText.addTextChangedListener(new TextWatcher() {

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				// TODO Auto-generated method stub
				String searchText = outlineSearchText.getText().toString();
				if(searchText != null)	{
					outlineAdapter.getFilter().filter(searchText);
				}
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
				// TODO Auto-generated method stub

			}

			@Override
			public void afterTextChanged(Editable s) {
				// TODO Auto-generated method stub

			}
		});
	}

	private void setBookName()	{
		if(OutlineActivityData.getBook().containsKey("name")) {
			String bookNameString = OutlineActivityData.getBook().get("name");
			if(bookNameString != null && !bookNameString.isEmpty() && !bookNameString.equals("") && bookNameString.length() > 0) {
				int bookNameStringLimit = 23;
				if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE)	{
					bookNameStringLimit = 50;
				}
				if(bookNameString.length() > bookNameStringLimit)	{
					bookNameString = bookNameString.substring(0, (bookNameStringLimit - 3)) + "...";
				}
				bookName.setText(bookNameString);
			}
		}
	}

	@Override
	public void onBackPressed()	{
		super.onBackPressed();
		finish();
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		// TODO Auto-generated method stub
		super.onConfigurationChanged(newConfig);
		setBookName();
	}

	public void OnOutlineBackButtonClick(View v)	{
		onBackPressed();
	}

	public void OnOutlineSearchButtonClick(View v)	{
		topBarSwitcher.setDisplayedChild(1);
		outlineSearchText.requestFocus();
		imm.showSoftInput(outlineSearchText, InputMethodManager.SHOW_IMPLICIT);
	}

	public void OnOutlineSearchViewBackButtonClick(View v)	{
		topBarSwitcher.setDisplayedChild(0);
		imm.hideSoftInputFromWindow(outlineSearchText.getWindowToken(), 0);
		outlineSearchText.setText("");
	}
}
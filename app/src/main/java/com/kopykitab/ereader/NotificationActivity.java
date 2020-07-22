package com.kopykitab.ereader;

import java.util.ArrayList;
import java.util.List;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.TypefaceSpan;
import android.util.TypedValue;
import android.view.MenuItem;
import android.view.Window;
import android.view.WindowManager;

import com.kopykitab.ereader.components.PagerSlidingTabStrip;
import com.kopykitab.ereader.settings.AppSettings;
import com.kopykitab.ereader.settings.Utils;
import com.kopykitab.ereader.R;

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class NotificationActivity extends AppCompatActivity {

	private PagerSlidingTabStrip tabs;
	private ViewPager pager;
	private NotificationPagerAdapter adapter;
	private List<String> tabList = new ArrayList<String>();
	
	private String screenName = "Notifications";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.notifications);

		ActionBar actionBar = getSupportActionBar();
		actionBar.setHomeAsUpIndicator(R.drawable.back_button);
		actionBar.setDisplayHomeAsUpEnabled(true);
		SpannableString actionBarLabel = new SpannableString(getResources().getString(R.string.notification_label));
		actionBarLabel.setSpan(new TypefaceSpan("" + Typeface.createFromAsset(getAssets(), "fonts/Roboto-Medium.ttf")), 0, actionBarLabel.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		actionBar.setTitle(actionBarLabel);

		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)	{
			Window window = getWindow();
			window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
			window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
			window.setStatusBarColor(getResources().getColor(R.color.action_bar_background_dark));
		}

		tabs = (PagerSlidingTabStrip) findViewById(R.id.notification_tabs);
		pager = (ViewPager) findViewById(R.id.notification_pager);

		String[] tabNames = getResources().getStringArray(R.array.notification_tabs_items);
		for(int i = 0 ; i < tabNames.length ; i++){
			tabList.add(tabNames[i]);
		}

		tabs.setTabNames(tabList);
		int offScreenPageLimit = tabList.size();
		pager.setOffscreenPageLimit(offScreenPageLimit);
		adapter = new NotificationPagerAdapter(getSupportFragmentManager(), NotificationActivity.this, tabList);
		pager.setAdapter(adapter);

		final int pageMargin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4, getResources().getDisplayMetrics());
		pager.setPageMargin(pageMargin);
		
		String notificationType = getIntent().getStringExtra("notification_type");
		if(notificationType != null && !notificationType.isEmpty())	{
			int notificationTypeValue = Integer.parseInt(notificationType);
			String customerId = AppSettings.getInstance(this).get("CUSTOMER_ID");
			if(notificationTypeValue <= (tabNames.length - 1))	{
				pager.setCurrentItem(notificationTypeValue, true);
				Utils.triggerGAEvent(this, "Notifications", "Push", customerId);
				Utils.triggerGAEvent(this, "Notifications", tabNames[notificationTypeValue], customerId);
			}
		}
		
		tabs.setViewPager(pager);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			onBackPressed();
			return true;
		default: 
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void onBackPressed() {
		// TODO Auto-generated method stub
		super.onBackPressed();
		finish();
	}
	
	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		Utils.triggerScreen(NotificationActivity.this, screenName);
	}


	public class NotificationPagerAdapter extends FragmentStatePagerAdapter {

		private Context mContext;
		private List<String> tabItems;

		public NotificationPagerAdapter(FragmentManager fm, Context mContext, List<String> tabList) {
			super(fm);
			this.mContext = mContext;
			this.tabItems = tabList;
		}

		@Override
		public int getCount() {
			return tabItems.size();
		}

		@Override
		public Fragment getItem(int position) {
			return new DetailedNotification(mContext, position); 
		}
	}
}
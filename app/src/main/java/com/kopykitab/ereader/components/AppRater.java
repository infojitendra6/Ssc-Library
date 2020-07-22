package com.kopykitab.ereader.components;

import com.kopykitab.ereader.settings.AppSettings;
import com.kopykitab.ereader.settings.Utils;
import com.kopykitab.ereader.R;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.view.ViewGroup.LayoutParams;

public class AppRater {
	
	public static String RATE_NOW = "Yes, Rate Now";
	public static String REMIND_LATER = "Remind me Later";
	public static String NO_THANKS = "No Thanks";
	public static int RATE_NOW_REMIND_DAYS = 60;
	public static int REMIND_LATER_REMIND_DAYS = 7;
	public static int NO_THANKS_REMIND_DAYS = 30;

	public static void showRateDialog(final Context mContext) {
		final String customerId = AppSettings.getInstance(mContext).get("CUSTOMER_ID"); 
		
		final AlertDialog rateDialog = Utils.createAlertBox(mContext);
		rateDialog.setCanceledOnTouchOutside(true);
		rateDialog.show();
		((TextView) rateDialog.findViewById(R.id.dialog_title)).setText("Please Rate it Now !");
		TextView dialogMessage = (TextView) rateDialog.findViewById(R.id.dialog_message);
		LayoutParams dialogMessageParams = dialogMessage.getLayoutParams();
		dialogMessageParams.height = 0;
		dialogMessage.setLayoutParams(dialogMessageParams);
		((LinearLayout) rateDialog.findViewById(R.id.dialog_two_button)).setVisibility(View.VISIBLE);
		Button rateNowButton = (Button) rateDialog.findViewById(R.id.dialog_two_button_button1);
		rateNowButton.setText(RATE_NOW);
		rateNowButton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View paramView) {
				// TODO Auto-generated method stub
				if(Utils.isNetworkConnected(mContext))	{
					AppSettings.getInstance(mContext).set("RateApp", RATE_NOW + "|" + System.currentTimeMillis());
					Utils.triggerGAEvent(mContext, "Notifications", RATE_NOW, customerId);
					String url = null;
					try {
						// Check whether Google Play store is installed or not
						mContext.getPackageManager().getPackageInfo("com.android.vending", 0);
						url = "market://details?id=" + mContext.getPackageName();
					} catch (Exception e) {
						url = "https://play.google.com/store/apps/details?id=" + mContext.getPackageName();
					}
					Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
					mContext.startActivity(intent);
				} else	{
					Utils.networkNotAvailableAlertBox(mContext);
				}
				rateDialog.dismiss();
			}
		});
		
		Button remindLaterButton = (Button) rateDialog.findViewById(R.id.dialog_two_button_button2);
		remindLaterButton.setText(REMIND_LATER);
		remindLaterButton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View paramView) {
				// TODO Auto-generated method stub
				AppSettings.getInstance(mContext).set("RateApp", REMIND_LATER + "|" + System.currentTimeMillis());
				Utils.triggerGAEvent(mContext, "Notifications", REMIND_LATER, customerId);
				rateDialog.dismiss();
			}
		});
		
		/*Button noThanksButton = (Button) rateDialog.findViewById(R.id.dialog_three_button_button3);
		noThanksButton.setText(NO_THANKS);
		noThanksButton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View paramView) {
				// TODO Auto-generated method stub
				AppSettings.getInstance(mContext).set("RateApp", NO_THANKS + "|" + System.currentTimeMillis());
				Utils.triggerGAEvent(mContext, "Notifications", NO_THANKS, customerId);
				rateDialog.dismiss();
			}
		});*/
	}
}
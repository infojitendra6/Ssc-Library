package com.kopykitab.ereader;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.TypefaceSpan;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.kopykitab.ereader.settings.AppSettings;
import com.kopykitab.ereader.settings.Constants;
import com.kopykitab.ereader.settings.Utils;

import org.json.JSONObject;

import java.net.URLEncoder;

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class FeedbackActivity extends AppCompatActivity {

    private EditText feedbackDescription;

    private String screenName = "Feedback";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.feedback);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setHomeAsUpIndicator(R.drawable.back_button);
        actionBar.setDisplayHomeAsUpEnabled(true);
        SpannableString actionBarLabel = new SpannableString(getResources().getString(R.string.feedback_label));
        actionBarLabel.setSpan(new TypefaceSpan("" + Typeface.createFromAsset(getAssets(), "fonts/Roboto-Medium.ttf")), 0, actionBarLabel.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        actionBar.setTitle(actionBarLabel);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.setStatusBarColor(getResources().getColor(R.color.action_bar_background_dark));
        }

        feedbackDescription = (EditText) findViewById(R.id.feedback_description);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.feedback_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            case R.id.submit_feedback:
                OnSubmitFeedbackButtonClick(feedbackDescription);
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
        Utils.triggerScreen(FeedbackActivity.this, screenName);
    }

    public void OnSubmitFeedbackButtonClick(View v) {
        String description = feedbackDescription.getText().toString();

        if (description.trim().equals("")) {
            Constants.showToast("Enter Comments", this);
            feedbackDescription.requestFocus();
        } else {
            new SubmitFeedback().execute(description);
        }
    }

    private class SubmitFeedback extends AsyncTask<String, Void, String> {

        private ProgressDialog pDialog;

        @Override
        protected void onPreExecute() {
            // TODO Auto-generated method stub
            super.onPreExecute();
            pDialog = new ProgressDialog(FeedbackActivity.this);
            pDialog.setMessage("Feedback submit in progress...");
            pDialog.setCancelable(false);
            pDialog.show();
        }

        @Override
        protected String doInBackground(String... params) {
            // TODO Auto-generated method stub
            String response = null;

            try {
                response = Utils.sendPost(FeedbackActivity.this, Constants.FEEDBACK_API_URL, "customer_id=" + URLEncoder.encode(AppSettings.getInstance(FeedbackActivity.this).get("CUSTOMER_ID"), "UTF-8") + "&feedback_description=" + URLEncoder.encode(params[0], "UTF-8") + "&login_source=" + URLEncoder.encode(Constants.LOGIN_SOURCE, "UTF-8"));
            } catch (Exception e) {
                e.printStackTrace();
            }

            return response;
        }

        @Override
        protected void onPostExecute(String result) {
            // TODO Auto-generated method stub
            super.onPostExecute(result);
            if (pDialog.isShowing()) {
                pDialog.dismiss();
            }
            if (result != null) {
                Log.i("Feedback Submit Details", result);

                try {
                    JSONObject jsonObject = new JSONObject(result);
                    boolean status = jsonObject.getBoolean("status");
                    final AlertDialog alertDialog = Utils.createAlertBox(FeedbackActivity.this);
                    alertDialog.show();
                    if (status) {
                        ((ImageView) alertDialog.findViewById(R.id.dialog_icon)).setImageResource(R.drawable.thumbs_up);
                        ((TextView) alertDialog.findViewById(R.id.dialog_title)).setText("Success");
                    } else {
                        ((ImageView) alertDialog.findViewById(R.id.dialog_icon)).setImageResource(R.drawable.error_icon);
                        ((TextView) alertDialog.findViewById(R.id.dialog_title)).setText("Error");
                    }
                    ((TextView) alertDialog.findViewById(R.id.dialog_message)).setText(jsonObject.getString("message"));
                    ((LinearLayout) alertDialog.findViewById(R.id.dialog_one_button)).setVisibility(View.VISIBLE);
                    Button alertDialogButton = (Button) alertDialog.findViewById(R.id.dialog_one_button_button);
                    alertDialogButton.setText("Ok");
                    alertDialogButton.setOnClickListener(new OnClickListener() {

                        @Override
                        public void onClick(View paramView) {
                            // TODO Auto-generated method stub
                            onBackPressed();
                            alertDialog.dismiss();
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
package com.kopykitab.ereader;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Patterns;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.kopykitab.ereader.settings.Constants;
import com.kopykitab.ereader.settings.Utils;

import org.json.JSONObject;

import java.net.URLEncoder;

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class ForgotPassword extends AppCompatActivity {

    private String screenName = "ForgotPassword";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.forgot_password);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.setStatusBarColor(getResources().getColor(R.color.action_bar_background_dark));
        }

        ((EditText) findViewById(R.id.forgot_email)).setHintTextColor(Color.argb(179, 255, 255, 255));
        ((EditText) findViewById(R.id.forgot_email)).setOnEditorActionListener(new EditText.OnEditorActionListener() {

            @Override
            public boolean onEditorAction(TextView tv, int actionId, KeyEvent event) {
                // TODO Auto-generated method stub
                if (actionId == EditorInfo.IME_ACTION_DONE || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                    OnGetPasswordButtonClick(tv);
                    return true;
                }
                return false;
            }
        });
    }

    public void OnGetPasswordButtonClick(View v) {
        String forgotEmail = ((EditText) findViewById(R.id.forgot_email)).getText().toString();
        if (forgotEmail.equals("")) {
            Constants.showToast("Enter Email Id", this);
        } else if (!Patterns.EMAIL_ADDRESS.matcher(forgotEmail).matches()) {
            Constants.showToast("Enter Proper Email Id", this);
        } else {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
            if (Utils.isNetworkConnected(this)) {
                new ResetPassword().execute(forgotEmail);
            } else {
                Utils.networkNotAvailableAlertBox(this);
            }
        }
    }

    public void OnBackButtonClick(View v) {
        onBackPressed();
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
        Utils.triggerScreen(ForgotPassword.this, screenName);
    }

    private class ResetPassword extends AsyncTask<String, Void, String> {

        private ProgressDialog pDialog;

        @Override
        protected void onPreExecute() {
            // TODO Auto-generated method stub
            super.onPreExecute();
            pDialog = new ProgressDialog(ForgotPassword.this);
            pDialog.setMessage("Please wait while we reset your password");
            pDialog.setCancelable(false);
            pDialog.show();
        }

        @Override
        protected String doInBackground(String... params) {
            // TODO Auto-generated method stub
            String response = null;

            try {
                response = Utils.sendPost(ForgotPassword.this, Constants.FORGOT_PASSWORD_API_URL, "email=" + URLEncoder.encode(params[0], "UTF-8") + "&login_source=" + URLEncoder.encode(Constants.LOGIN_SOURCE, "UTF-8"));
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
            try {
                JSONObject jsonObject = new JSONObject(result);
                boolean status = jsonObject.getBoolean("status");
                final AlertDialog alertDialog = Utils.createAlertBox(ForgotPassword.this);
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
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }
}
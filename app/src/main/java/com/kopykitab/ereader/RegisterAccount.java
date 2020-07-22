package com.kopykitab.ereader;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
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
import android.widget.RadioGroup;
import android.widget.TextView;

import com.kopykitab.ereader.components.RadioButton;
import com.kopykitab.ereader.components.handler.PermissionHandler;
import com.kopykitab.ereader.components.handler.PermissionHandler.PermissionListener;
import com.kopykitab.ereader.settings.AppSettings;
import com.kopykitab.ereader.settings.Constants;
import com.kopykitab.ereader.settings.Utils;

import org.json.JSONObject;

import java.net.URLEncoder;

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class RegisterAccount extends AppCompatActivity {
    private String screenName = "Register";
    private PermissionHandler mPermissionHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.register_account);
        mPermissionHandler = new PermissionHandler();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.setStatusBarColor(getResources().getColor(R.color.action_bar_background_dark));
        }

        ((EditText) findViewById(R.id.register_firstname)).setHintTextColor(Color.argb(179, 255, 255, 255));
        ((EditText) findViewById(R.id.register_lastname)).setHintTextColor(Color.argb(179, 255, 255, 255));
        ((EditText) findViewById(R.id.register_email)).setHintTextColor(Color.argb(179, 255, 255, 255));
        ((EditText) findViewById(R.id.register_password)).setHintTextColor(Color.argb(179, 255, 255, 255));
        ((EditText) findViewById(R.id.register_mobile_number)).setHintTextColor(Color.argb(179, 255, 255, 255));
        ((EditText) findViewById(R.id.register_referral_code)).setHintTextColor(Color.argb(179, 255, 255, 255));
        ((EditText) findViewById(R.id.register_password)).setOnEditorActionListener(new EditText.OnEditorActionListener() {

            @Override
            public boolean onEditorAction(TextView tv, int actionId, KeyEvent event) {
                // TODO Auto-generated method stub
                if (actionId == EditorInfo.IME_ACTION_DONE || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                    OnRegisterAccountButtonClick(tv);
                    return true;
                }
                return false;
            }
        });

        AccountManager accountManager = AccountManager.get(RegisterAccount.this);
        for (Account account : accountManager.getAccounts()) {
            if (account.type.equals("com.google")) {
                if (Patterns.EMAIL_ADDRESS.matcher(account.name).matches()) {
                    ((EditText) findViewById(R.id.register_email)).setText(account.name);
                }
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // TODO Auto-generated method stub
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        mPermissionHandler.onPermissionsResult(requestCode, permissions, grantResults);
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
        Utils.triggerScreen(RegisterAccount.this, screenName);

        if (Utils.isPermissionDialogOpen()) {
            if (Utils.hasPermissions(RegisterAccount.this, Constants.REQUIRED_PERMISSIONS)) {
                if (isFinishing()) {
                    Utils.closePermissionDialog();
                }
            }
        }
    }

    public void OnRegisterAccountButtonClick(View v) {
        final String firstName = ((EditText) findViewById(R.id.register_firstname)).getText().toString();
        final String lastName = ((EditText) findViewById(R.id.register_lastname)).getText().toString();
        final String emailId = ((EditText) findViewById(R.id.register_email)).getText().toString();
        final String password = ((EditText) findViewById(R.id.register_password)).getText().toString();
        final String referralCode = ((EditText) findViewById(R.id.register_referral_code)).getText().toString();
        final String mobileNumber = ((EditText) findViewById(R.id.register_mobile_number)).getText().toString();

        RadioGroup customerTypeGroup = (RadioGroup) findViewById(R.id.customer_type_group);
        final String customerType = ((RadioButton) findViewById(customerTypeGroup.getCheckedRadioButtonId())).getText().toString();

        if (firstName.equals("")) {
            Constants.showToast("Enter First Name", this);
        } else if (lastName.equals("")) {
            Constants.showToast("Enter Last Name", this);
        } else if (emailId.equals("")) {
            Constants.showToast("Enter Email Id", this);
        } else if (password.equals("")) {
            Constants.showToast("Enter Password", this);
        } else if (!Patterns.EMAIL_ADDRESS.matcher(emailId).matches()) {
            Constants.showToast("Enter Proper Email Id", this);
        } else {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
            if (Utils.isNetworkConnected(this)) {
                mPermissionHandler.requestPermission(this, Constants.REQUIRED_PERMISSIONS, 101, new PermissionListener() {
                    @Override
                    public void onPermissionGranted() {
                        new CreateAccount().execute(firstName, lastName, emailId, password, referralCode, mobileNumber, customerType);

                        Utils.triggerGAEventOnline(RegisterAccount.this, "Permission_Allow_" + screenName, "Not_Logged_In", "");
                    }

                    @Override
                    public void onPermissionDenied() {
                        Utils.triggerGAEventOnline(RegisterAccount.this, "Permission_Denied_" + screenName, "Not_Logged_In", "");
                    }

                    @Override
                    public void onPermissionPermanentlyDenied() {
                        Utils.showPermissionInfoDialog(RegisterAccount.this);
                        Utils.triggerGAEventOnline(RegisterAccount.this, "Permission_Permanently_Denied_" + screenName, "Not_Logged_In", "");
                    }
                });
            } else {
                Utils.networkNotAvailableAlertBox(this);
            }
        }
    }

    public void OnAlreadyRegisteredButtonClick(View v) {
        Utils.showLoginActivity(this);
    }

    public void OnBackButtonClick(View v) {
        onBackPressed();
    }

    private class CreateAccount extends AsyncTask<String, Void, String> {

        private ProgressDialog pDialog;
        private String email;
        private String appId;

        @Override
        protected void onPreExecute() {
            // TODO Auto-generated method stub
            super.onPreExecute();
            pDialog = new ProgressDialog(RegisterAccount.this);
            pDialog.setMessage("Please wait while we register your account");
            pDialog.setCancelable(false);
            pDialog.show();
        }

        @Override
        protected String doInBackground(String... params) {
            // TODO Auto-generated method stub
            appId = Utils.getAppId(RegisterAccount.this);
            email = params[2];
            String response = null;

            try {
                response = Utils.sendPost(RegisterAccount.this, Constants.REGISTER_API_URL, "firstname=" + URLEncoder.encode(params[0], "UTF-8") + "&lastname=" + URLEncoder.encode(params[1], "UTF-8") +
                        "&email=" + URLEncoder.encode(email, "UTF-8") + "&password=" + URLEncoder.encode(params[3], "UTF-8") + "&appid=" + URLEncoder.encode(appId, "UTF-8") +
                        "&login_source=" + URLEncoder.encode(Constants.LOGIN_SOURCE, "UTF-8") + "&referral_code=" + URLEncoder.encode(params[4], "UTF-8") +
                        "&mobile_number=" + URLEncoder.encode(params[5], "UTF-8") + "&attributes[customer_type]=" + URLEncoder.encode(params[6], "UTF-8"));

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
                String statusMessage = jsonObject.getString("message");

                boolean status = jsonObject.getBoolean("status");
                if (status) {
                    String customerId = jsonObject.getString("customer_id");
                    AppSettings.getInstance(RegisterAccount.this).setConfiguration(email, customerId, appId);
                    AppSettings.getInstance(RegisterAccount.this).set("customer_name", jsonObject.getString("firstname") + " " + jsonObject.getString("lastname"));
                    Constants.showToast(statusMessage, RegisterAccount.this);

                    Utils.showLibraryAndSyncData(RegisterAccount.this);
                } else {
                    final AlertDialog alertDialog = Utils.createAlertBox(RegisterAccount.this);
                    alertDialog.show();
                    ((ImageView) alertDialog.findViewById(R.id.dialog_icon)).setImageResource(R.drawable.error_icon);
                    ((TextView) alertDialog.findViewById(R.id.dialog_title)).setText("Error");
                    ((TextView) alertDialog.findViewById(R.id.dialog_message)).setText(statusMessage);
                    ((LinearLayout) alertDialog.findViewById(R.id.dialog_one_button)).setVisibility(View.VISIBLE);
                    Button alertDialogButton = (Button) alertDialog.findViewById(R.id.dialog_one_button_button);
                    alertDialogButton.setText("Ok");
                    alertDialogButton.setOnClickListener(new OnClickListener() {

                        @Override
                        public void onClick(View paramView) {
                            // TODO Auto-generated method stub
                            alertDialog.dismiss();
                        }
                    });
                }
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }
}
package com.kopykitab.ereader;

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
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
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

import com.kopykitab.ereader.components.handler.PermissionHandler;
import com.kopykitab.ereader.components.handler.PermissionHandler.PermissionListener;
import com.kopykitab.ereader.settings.AppSettings;
import com.kopykitab.ereader.settings.Constants;
import com.kopykitab.ereader.settings.Utils;

import org.json.JSONObject;

import java.net.URLEncoder;

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class LoginActivity extends AppCompatActivity {
    private EditText loginEmail, loginPassword;
    private Button loginButton;
    private PermissionHandler mPermissionHandler;

    private String screenName = "Login";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mPermissionHandler = new PermissionHandler();
        if (AppSettings.getInstance(this).isConfigurationDone()) {
            mPermissionHandler.requestPermission(this, Constants.REQUIRED_PERMISSIONS, 101, new PermissionListener() {
                @Override
                public void onPermissionGranted() {
                    Intent libraryIntent = new Intent(LoginActivity.this, LibraryActivity.class);
                    startActivity(libraryIntent);
                    finish();

                    if (Utils.isNetworkConnected(LoginActivity.this)) {
                        Utils.triggerGAEventOnline(LoginActivity.this, "Permission_Allow_" + screenName, "Logged_In", AppSettings.getInstance(LoginActivity.this).get("CUSTOMER_ID"));
                    }
                }

                @Override
                public void onPermissionDenied() {
                    if (Utils.isNetworkConnected(LoginActivity.this)) {
                        Utils.triggerGAEventOnline(LoginActivity.this, "Permission_Denied_" + screenName, "Logged_In", AppSettings.getInstance(LoginActivity.this).get("CUSTOMER_ID"));
                    }
                }

                @Override
                public void onPermissionPermanentlyDenied() {
                    Utils.showPermissionInfoDialog(LoginActivity.this);
                    if (Utils.isNetworkConnected(LoginActivity.this)) {
                        Utils.triggerGAEventOnline(LoginActivity.this, "Permission_Permanently_Denied_" + screenName, "Logged_In", AppSettings.getInstance(LoginActivity.this).get("CUSTOMER_ID"));
                    }
                }
            });
        } else {
            setContentView(R.layout.activity_login);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                Window window = getWindow();
                window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
                window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
                window.setStatusBarColor(getResources().getColor(R.color.action_bar_background_dark));
            }

            loginEmail = (EditText) findViewById(R.id.login_email);
            loginPassword = (EditText) findViewById(R.id.login_password);
            loginButton = (Button) findViewById(R.id.login_button);

            loginEmail.setHintTextColor(Color.argb(179, 255, 255, 255));
            loginPassword.setHintTextColor(Color.argb(179, 255, 255, 255));

            loginEmail.addTextChangedListener(mTextWatcher);
            loginPassword.addTextChangedListener(mTextWatcher);
            loginPassword.setOnEditorActionListener(new EditText.OnEditorActionListener() {

                @Override
                public boolean onEditorAction(TextView tv, int actionId, KeyEvent event) {
                    // TODO Auto-generated method stub
                    if (actionId == EditorInfo.IME_ACTION_DONE || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                        validateLogin(tv);
                        return true;
                    }
                    return false;
                }
            });
        }
    }

    private TextWatcher mTextWatcher = new TextWatcher() {

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            // TODO Auto-generated method stub

        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            // TODO Auto-generated method stub

        }

        @Override
        public void afterTextChanged(Editable s) {
            // TODO Auto-generated method stub
            if (!loginEmail.getText().toString().equals("") && !loginPassword.getText().toString().equals("")) {
                loginButton.setEnabled(true);
            } else {
                loginButton.setEnabled(false);
            }
        }
    };

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
        Utils.triggerScreen(LoginActivity.this, screenName);

        if (Utils.isPermissionDialogOpen()) {
            if (Utils.hasPermissions(LoginActivity.this, Constants.REQUIRED_PERMISSIONS)) {
                if (isFinishing()) {
                    Utils.closePermissionDialog();
                }
            }
        }
    }

    public void validateLogin(View v) {
        final String login_email = loginEmail.getText().toString();
        final String login_password = loginPassword.getText().toString();

        if (login_email.equals("")) {
            Constants.showToast("Enter Email Id", this);
        } else if (login_password.equals("")) {
            Constants.showToast("Enter Password", this);
        } else if (!Patterns.EMAIL_ADDRESS.matcher(login_email).matches()) {
            Constants.showToast("Enter Proper Email Id", this);
        } else {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
            if (Utils.isNetworkConnected(LoginActivity.this)) {
                mPermissionHandler.requestPermission(this, Constants.REQUIRED_PERMISSIONS, 101, new PermissionListener() {
                    @Override
                    public void onPermissionGranted() {
                        new ValidateLogin().execute(login_email, login_password);

                        Utils.triggerGAEventOnline(LoginActivity.this, "Permission_Allow_" + screenName, "Not_Logged_In", "");
                    }

                    @Override
                    public void onPermissionDenied() {
                        Utils.triggerGAEventOnline(LoginActivity.this, "Permission_Denied_" + screenName, "Not_Logged_In", "");
                    }

                    @Override
                    public void onPermissionPermanentlyDenied() {
                        Utils.showPermissionInfoDialog(LoginActivity.this);
                        Utils.triggerGAEventOnline(LoginActivity.this, "Permission_Permanently_Denied_" + screenName, "Not_Logged_In", "");
                    }
                });
            } else {
                Utils.networkNotAvailableAlertBox(LoginActivity.this);
            }
        }
    }

    public void OnBackButtonClick(View v) {
        onBackPressed();
    }

    public void OnForgotPasswordButtonClick(View v) {
        Utils.showForgotPasswordActivity(this);
    }

    public void OnRegisterButtonClick(View v) {
        Utils.showRegisterActivity(this);
    }

    private class ValidateLogin extends AsyncTask<String, Void, String> {

        private String appId, email;
        private ProgressDialog pDialog;

        protected ValidateLogin() {
            appId = Utils.getAppId(LoginActivity.this);
        }

        @Override
        protected void onPreExecute() {
            // TODO Auto-generated method stub
            super.onPreExecute();
            pDialog = new ProgressDialog(LoginActivity.this);
            pDialog.setMessage("Please wait, Logging in progress...");
            pDialog.setCancelable(false);
            pDialog.show();
        }

        @Override
        protected String doInBackground(String... params) {
            // TODO Auto-generated method stub
            email = params[0];
            String response = null;

            try {
                response = Utils.sendPost(LoginActivity.this, Constants.LOGIN_API_URL, "email=" + URLEncoder.encode(email, "UTF-8") + "&password=" + URLEncoder.encode(params[1], "UTF-8") + "&appid=" + URLEncoder.encode(appId, "UTF-8") + "&login_source=" + URLEncoder.encode(Constants.LOGIN_SOURCE, "UTF-8"));
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
                Log.i("Login Details", result);

                try {
                    JSONObject jsonObject = new JSONObject(result);
                    String status = jsonObject.getString("status");
                    String customerId = "";
                    Utils.triggerGAEvent(LoginActivity.this, "Login", jsonObject.getString("messageStatus"), email);
                    if (status.equals("True")) {
                        customerId = jsonObject.getString("id");
                        AppSettings.getInstance(LoginActivity.this).setConfiguration(email, customerId, appId);
                        AppSettings.getInstance(LoginActivity.this).set("customer_name", jsonObject.getString("Firstname") + " " + jsonObject.getString("Lastname"));
                        Utils.showLibrary(LoginActivity.this);
                    } else if (status.equals("False")) {
                        final AlertDialog alertDialog = Utils.createAlertBox(LoginActivity.this);
                        alertDialog.show();
                        ((ImageView) alertDialog.findViewById(R.id.dialog_icon)).setImageResource(R.drawable.error_icon);
                        ((TextView) alertDialog.findViewById(R.id.dialog_title)).setText("Error");
                        ((TextView) alertDialog.findViewById(R.id.dialog_message)).setText(jsonObject.getString("messageStatus"));
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
                    e.printStackTrace();
                }
            } else {
                Log.e("Error Login", "Problem in login");
                Constants.showToast("Error in Login. Check Internet Connection.", LoginActivity.this);
            }
        }
    }
}
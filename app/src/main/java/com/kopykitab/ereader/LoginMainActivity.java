package com.kopykitab.ereader;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.kopykitab.ereader.components.handler.PermissionHandler;
import com.kopykitab.ereader.components.handler.PermissionHandler.PermissionListener;
import com.kopykitab.ereader.settings.AppSettings;
import com.kopykitab.ereader.settings.Constants;
import com.kopykitab.ereader.settings.Utils;

import org.json.JSONObject;

import java.net.URLEncoder;

public class LoginMainActivity extends AppCompatActivity {
    private CallbackManager callbackManager;
    private static final String TAG = LoginMainActivity.class.getSimpleName();
    private static final int RC_SIGN_IN = 007;

    private GoogleApiClient mGoogleApiClient;
    public static Handler finishActivityHandler = new Handler();
    private PermissionHandler mPermissionHandler;

    private String screenName = "Login_Main";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.setStatusBarColor(getResources().getColor(R.color.action_bar_background_dark));
        }

        mPermissionHandler = new PermissionHandler();

        if (AppSettings.getInstance(this).isConfigurationDone()) {
            mPermissionHandler.requestPermission(this, Constants.STORAGE_PERMISSION, 101, new PermissionListener() {
                @Override
                public void onPermissionGranted() {
                    Intent libraryIntent = new Intent(LoginMainActivity.this, LibraryActivity.class);
                    startActivity(libraryIntent);
                    finish();

                    Utils.triggerGADataReceiver(LoginMainActivity.this, AppSettings.getInstance(LoginMainActivity.this).get("CUSTOMER_ID"), false);
                    if (Utils.isNetworkConnected(LoginMainActivity.this)) {
                        Utils.triggerGAEventOnline(LoginMainActivity.this, "Permission_Allow_" + screenName, "Logged_In", AppSettings.getInstance(LoginMainActivity.this).get("CUSTOMER_ID"));
                    }
                }

                @Override
                public void onPermissionDenied() {
                    Utils.showPermissionInfoDialog(LoginMainActivity.this);
                    if (Utils.isNetworkConnected(LoginMainActivity.this)) {
                        Utils.triggerGAEventOnline(LoginMainActivity.this, "Permission_Denied_" + screenName, "Logged_In", AppSettings.getInstance(LoginMainActivity.this).get("CUSTOMER_ID"));
                    }
                }

                @Override
                public void onPermissionPermanentlyDenied() {
                    Utils.showPermissionInfoDialog(LoginMainActivity.this);
                    if (Utils.isNetworkConnected(LoginMainActivity.this)) {
                        Utils.triggerGAEventOnline(LoginMainActivity.this, "Permission_Permanently_Denied_" + screenName, "Logged_In", AppSettings.getInstance(LoginMainActivity.this).get("CUSTOMER_ID"));
                    }
                }
            });
        } else {
            FacebookSdk.sdkInitialize(getApplicationContext());
            callbackManager = CallbackManager.Factory.create();

            setContentView(R.layout.activity_main_login);

            mPermissionHandler.requestPermission(this, Constants.REQUIRED_PERMISSIONS, 101, new PermissionListener() {
                @Override
                public void onPermissionGranted() {
                    Utils.triggerGADataReceiver(LoginMainActivity.this, null, false);
                    if (Utils.isNetworkConnected(LoginMainActivity.this)) {
                        Utils.triggerGAEventOnline(LoginMainActivity.this, "Permission_Allow_" + screenName, "Not_Logged_In", "");
                    }
                }

                @Override
                public void onPermissionDenied() {
                    if (Utils.isNetworkConnected(LoginMainActivity.this)) {
                        Utils.triggerGAEventOnline(LoginMainActivity.this, "Permission_Denied_" + screenName, "Not_Logged_In", "");
                    }
                }

                @Override
                public void onPermissionPermanentlyDenied() {
                    Utils.showPermissionInfoDialog(LoginMainActivity.this);
                    if (Utils.isNetworkConnected(LoginMainActivity.this)) {
                        Utils.triggerGAEventOnline(LoginMainActivity.this, "Permission_Permanently_Denied_" + screenName, "Not_Logged_In", "");
                    }
                }
            });

            LoginButton loginWithFBButton = (LoginButton) findViewById(R.id.login_with_fb_button);
            loginWithFBButton.setReadPermissions("public_profile", "email", "user_friends");
            loginWithFBButton.registerCallback(callbackManager, new FacebookCallback<LoginResult>() {

                @Override
                public void onSuccess(LoginResult loginResult) {
                    // TODO Auto-generated method stub
                    Utils.registerWithFB(LoginMainActivity.this, loginResult);
                    LoginManager.getInstance().logOut();
                }

                @Override
                public void onError(FacebookException error) {
                    // TODO Auto-generated method stub
                    error.printStackTrace();
                }

                @Override
                public void onCancel() {
                    // TODO Auto-generated method stub

                }
            });

            //Google Login Configurations
            GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestEmail()
                    .build();

            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .enableAutoManage(this, new GoogleApiClient.OnConnectionFailedListener() {
                        @Override
                        public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
                            Log.d("onConnectionFailed:", connectionResult.toString());
                        }
                    })
                    .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                    .build();

            mGoogleApiClient.connect();
        }

        finishActivityHandler = new Handler() {
            public void handleMessage(Message message) {
                super.handleMessage(message);

                switch (message.what) {
                    case 0:
                        finish();
                        break;

                }
            }
        };
    }

    public void OnFacebookButtonClick(View v) {
        if (Utils.isNetworkConnected(this)) {
            mPermissionHandler.requestPermission(this, Constants.REQUIRED_PERMISSIONS, 101, new PermissionListener() {
                @Override
                public void onPermissionGranted() {
                    Utils.triggerSocialInteractions(LoginMainActivity.this, "Facebook", "Login", screenName);
                    ((LoginButton) findViewById(R.id.login_with_fb_button)).performClick();

                    Utils.triggerGAEventOnline(LoginMainActivity.this, "Permission_Allow_Facebook_Login_" + screenName, "Not_Logged_In", "");

                }

                @Override
                public void onPermissionDenied() {
                    Utils.triggerGAEventOnline(LoginMainActivity.this, "Permission_Denied_Facebook_Login_" + screenName, "Not_Logged_In", "");
                }

                @Override
                public void onPermissionPermanentlyDenied() {
                    Utils.showPermissionInfoDialog(LoginMainActivity.this);
                    Utils.triggerGAEventOnline(LoginMainActivity.this, "Permission_Permanently_Denied_Facebook_Login_" + screenName, "Not_Logged_In", "");
                }
            });
        } else {
            Utils.networkNotAvailableAlertBox(this);
        }
    }

    public void OnGoogleButtonClick(View v) {
        if (Utils.isNetworkConnected(this)) {
            mPermissionHandler.requestPermission(this, Constants.REQUIRED_PERMISSIONS, 101, new PermissionListener() {
                @Override
                public void onPermissionGranted() {
                    Utils.triggerSocialInteractions(LoginMainActivity.this, "Google", "Login", screenName);
                    Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
                    startActivityForResult(signInIntent, RC_SIGN_IN);

                    Utils.triggerGAEventOnline(LoginMainActivity.this, "Permission_Allow_Google_Login_" + screenName, "Not_Logged_In", "");
                }

                @Override
                public void onPermissionDenied() {
                    Utils.triggerGAEventOnline(LoginMainActivity.this, "Permission_Denied_Google_Login_" + screenName, "Not_Logged_In", "");
                }

                @Override
                public void onPermissionPermanentlyDenied() {
                    Utils.showPermissionInfoDialog(LoginMainActivity.this);
                    Utils.triggerGAEventOnline(LoginMainActivity.this, "Permission_Permanently_Denied_Google_Login_" + screenName, "Not_Logged_In", "");
                }
            });
        } else {
            Utils.networkNotAvailableAlertBox(this);
        }
    }

    public void OnEmailLoginButtonClick(View v) {
        mPermissionHandler.requestPermission(this, Constants.REQUIRED_PERMISSIONS, 101, new PermissionListener() {
            @Override
            public void onPermissionGranted() {
                Utils.showLoginActivity(LoginMainActivity.this);
                if (Utils.isNetworkConnected(LoginMainActivity.this)) {
                    Utils.triggerGAEventOnline(LoginMainActivity.this, "Permission_Allow_Email_Login_" + screenName, "Not_Logged_In", "");
                }
            }

            @Override
            public void onPermissionDenied() {
                Utils.showLoginActivity(LoginMainActivity.this);
                if (Utils.isNetworkConnected(LoginMainActivity.this)) {
                    Utils.triggerGAEventOnline(LoginMainActivity.this, "Permission_Denied_Email_Login_" + screenName, "Not_Logged_In", "");
                }
            }

            @Override
            public void onPermissionPermanentlyDenied() {
                Utils.showPermissionInfoDialog(LoginMainActivity.this);
                if (Utils.isNetworkConnected(LoginMainActivity.this)) {
                    Utils.triggerGAEventOnline(LoginMainActivity.this, "Permission_Permanently_Denied_Email_Login_" + screenName, "Not_Logged_In", "");
                }
            }
        });
    }

    public void OnEmailSigninButtonClick(View v) {
        mPermissionHandler.requestPermission(this, Constants.REQUIRED_PERMISSIONS, 101, new PermissionListener() {
            @Override
            public void onPermissionGranted() {
                Utils.showRegisterActivity(LoginMainActivity.this);
                if (Utils.isNetworkConnected(LoginMainActivity.this)) {
                    Utils.triggerGAEventOnline(LoginMainActivity.this, "Permission_Allow_Email_Signin_" + screenName, "Not_Logged_In", "");
                }
            }

            @Override
            public void onPermissionDenied() {
                Utils.showRegisterActivity(LoginMainActivity.this);
                if (Utils.isNetworkConnected(LoginMainActivity.this)) {
                    Utils.triggerGAEventOnline(LoginMainActivity.this, "Permission_Denied_Email_Signin_" + screenName, "Not_Logged_In", "");
                }
            }

            @Override
            public void onPermissionPermanentlyDenied() {
                Utils.showPermissionInfoDialog(LoginMainActivity.this);
                if (Utils.isNetworkConnected(LoginMainActivity.this)) {
                    Utils.triggerGAEventOnline(LoginMainActivity.this, "Permission_Permanently_Denied_Email_Signin_" + screenName, "Not_Logged_In", "");
                }
            }
        });
    }

    private void handleSignInResult(GoogleSignInResult result) {
        Log.d(TAG, "handleSignInResult:" + result.isSuccess());
        if (result.isSuccess()) {
            // Signed in successfully, show authenticated UI.
            GoogleSignInAccount acct = result.getSignInAccount();

            String firstName = acct.getGivenName();
            String lastName = acct.getFamilyName();
            String emailId = acct.getEmail();
            String id = acct.getId();

            Log.e(TAG, "Google Login Details: " + "First Name : " + firstName + ", Last Name : " + lastName + ", Email ID : " + emailId + ", ID : " + id);

            registerWithGoogle(firstName, lastName, emailId, id);
        }
    }

    private void registerWithGoogle(final String firstName, final String lastName, final String emailId, final String id) {
        if (Utils.isNetworkConnected(this)) {
            final Context context = LoginMainActivity.this;

            new AsyncTask<String, Void, String>() {
                ProgressDialog pDialog;
                String appId;

                @Override
                protected void onPreExecute() {
                    super.onPreExecute();
                    pDialog = new ProgressDialog(context);
                    pDialog.setMessage("Please wait, Logging in progress...");
                    pDialog.setCancelable(false);
                    pDialog.show();
                }

                @Override
                protected String doInBackground(String... params) {
                    appId = Utils.getAppId(context);
                    String response = null;

                    try {
                        response = Utils.sendPost(context, Constants.LOGIN_WITH_GOOGLE_API_URL, "firstname=" + URLEncoder.encode(firstName, "UTF-8") + "&lastname=" + URLEncoder.encode(lastName, "UTF-8") + "&email=" + URLEncoder.encode(emailId, "UTF-8") + "&id=" + URLEncoder.encode(id, "UTF-8") + "&appid=" + URLEncoder.encode(appId, "UTF-8") + "&login_source=" + URLEncoder.encode(Constants.LOGIN_SOURCE, "UTF-8"));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    return response;
                }

                @Override
                protected void onPostExecute(String result) {
                    super.onPostExecute(result);

                    if (pDialog.isShowing()) {
                        pDialog.dismiss();
                    }

                    if (result != null) {
                        Log.i("Login Detail via Google", result);

                        try {
                            JSONObject jsonObject = new JSONObject(result);
                            boolean status = jsonObject.getBoolean("status");
                            String customerId = jsonObject.getString("customer_id"), email = jsonObject.getString("email"), newRegistration = null;
                            if (jsonObject.has("new_registration")) {
                                newRegistration = jsonObject.getString("new_registration");
                            }
                            if (status) {
                                if (newRegistration != null && newRegistration.equals("1")) {
                                    Constants.showToast(jsonObject.getString("messageStatus"), context);
                                    Utils.triggerGAEvent(context, "Register", "Google", customerId);
                                } else {
                                    Utils.triggerGAEvent(context, "Login", "Google", customerId);
                                }
                                AppSettings.getInstance(context).setConfiguration(email, customerId, appId);
                                Utils.showLibraryAndSyncData(context);
                            } else {
                                Utils.triggerGAEvent(context, "Login", jsonObject.getString("messageStatus"), email);
                                final AlertDialog alertDialog = Utils.createAlertBox(context);
                                alertDialog.show();
                                ((ImageView) alertDialog.findViewById(R.id.dialog_icon)).setImageResource(R.drawable.error_icon);
                                ((TextView) alertDialog.findViewById(R.id.dialog_title)).setText("Error");
                                ((TextView) alertDialog.findViewById(R.id.dialog_message)).setText(jsonObject.getString("messageStatus"));
                                ((LinearLayout) alertDialog.findViewById(R.id.dialog_one_button)).setVisibility(View.VISIBLE);
                                Button alertDialogButton = (Button) alertDialog.findViewById(R.id.dialog_one_button_button);
                                alertDialogButton.setText("Ok");
                                alertDialogButton.setOnClickListener(new View.OnClickListener() {

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
                    }
                }
            }.execute();
        } else {
            Utils.networkNotAvailableAlertBox(this);
        }
    }

    @Override
    protected void onResume() {
        // TODO Auto-generated method stub
        super.onResume();
        Utils.triggerScreen(LoginMainActivity.this, screenName);

        if (Utils.isPermissionDialogOpen()) {
            if (AppSettings.getInstance(this).isConfigurationDone()) {
                if (Utils.hasPermissions(LoginMainActivity.this, Constants.STORAGE_PERMISSION)) {
                    if (isFinishing()) {
                        Utils.closePermissionDialog();
                    }
                    Intent libraryIntent = new Intent(LoginMainActivity.this, LibraryActivity.class);
                    startActivity(libraryIntent);
                    finish();

                    Utils.triggerGADataReceiver(LoginMainActivity.this, AppSettings.getInstance(LoginMainActivity.this).get("CUSTOMER_ID"), false);
                }
            } else {
                if (Utils.hasPermissions(LoginMainActivity.this, Constants.REQUIRED_PERMISSIONS)) {
                    if (isFinishing()) {
                        Utils.closePermissionDialog();
                    }
                }
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        mPermissionHandler.onPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // TODO Auto-generated method stub
        super.onActivityResult(requestCode, resultCode, data);
        if (callbackManager != null)
            callbackManager.onActivityResult(requestCode, resultCode, data);

        //Always revoke google account
        try {
            Auth.GoogleSignInApi.revokeAccess(mGoogleApiClient).setResultCallback(new ResultCallback<Status>() {
                @Override
                public void onResult(@NonNull Status status) {
                    Log.d(TAG, "Revoke Access of Google Login : " + status);
                }
            });
        } catch (IllegalStateException ise) {
            ise.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            handleSignInResult(result);
        }
    }
}
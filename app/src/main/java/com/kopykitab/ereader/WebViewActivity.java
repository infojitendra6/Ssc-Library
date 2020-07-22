package com.kopykitab.ereader;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.graphics.Bitmap;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.kopykitab.ereader.components.handler.PermissionHandler;
import com.kopykitab.ereader.components.handler.PermissionHandler.PermissionListener;
import com.kopykitab.ereader.settings.Constants;
import com.kopykitab.ereader.settings.Utils;

import org.apache.commons.lang3.text.WordUtils;
import org.apache.http.util.EncodingUtils;
import org.json.JSONObject;

import java.util.List;
import java.util.Locale;

public class WebViewActivity extends MainFoundationActivity {
    private ProgressDialog pDialog;
    private WebView webView;
    private long startTime;
    private String productId, productType;
    private Handler mHandler;
    private String postData, url = null;
    private PermissionHandler mPermissionHandler;

    private String screenName = "WebView";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (customerId != null) {
            setContentView(R.layout.web_view);

            mHandler = new Handler(Looper.getMainLooper());

            webView = (WebView) findViewById(R.id.web_page_display);
            WebSettings webViewSettings = webView.getSettings();
            webViewSettings.setJavaScriptEnabled(true);
            webViewSettings.setAllowFileAccess(true);
            webViewSettings.setJavaScriptCanOpenWindowsAutomatically(false);
            webViewSettings.setSupportMultipleWindows(false);
            webViewSettings.setSupportZoom(false);
            webViewSettings.setSaveFormData(false);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
            } else {
                webView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
            }

            webViewSettings.setCacheMode(WebSettings.LOAD_NO_CACHE);
            try {
                PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
                webViewSettings.setUserAgentString(webViewSettings.getUserAgentString() + " [" + packageInfo.packageName + "/web_app/" + packageInfo.versionName + "]");
            } catch (Exception e) {
                e.printStackTrace();
                webViewSettings.setUserAgentString(webViewSettings.getUserAgentString() + " [" + getPackageName() + "/web_app]");
            }

            webView.clearCache(true);
            webView.clearHistory();
            webView.addJavascriptInterface(new WebAppInterface(this), "WebApp");
            webView.requestFocus(View.FOCUS_DOWN);
            webView.setWebChromeClient(new WebChromeClient());
            pDialog = new ProgressDialog(this);

            if (getIntent().hasExtra("product_id")) {
                startTime = System.currentTimeMillis();
                productId = getIntent().getStringExtra("product_id");
                productType = getIntent().getStringExtra("product_type").replace("_", " ");
                productType = WordUtils.capitalize(productType);
                Utils.triggerGAEvent(WebViewActivity.this, productType.replace(" ", "_") + "_Open", productId, customerId);
                pDialog.setMessage("Preparing " + productType + "... Please wait...");
            } else {
                pDialog.setMessage("Opening Page... Please wait...");
            }
            pDialog.setCancelable(false);

            if (getIntent().hasExtra("web_url")) {
                url = getIntent().getStringExtra("web_url");
            } else if (getIntent().getData() != null) {
                url = getIntent().getData().toString();
            }

            if (url != null) {
                postData = "&customer_id=" + customerId + "&page_type=web_app&source=" + Constants.LOGIN_SOURCE;

                mPermissionHandler = new PermissionHandler();
                mPermissionHandler.requestPermission(this, Constants.LOCATION_PERMISSION, 101, new PermissionListener() {
                    @Override
                    public void onPermissionGranted() {
                        try {
                            LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
                            Location location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                            if (location != null) {
                                Geocoder geocoder = new Geocoder(WebViewActivity.this, Locale.getDefault());
                                List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                                Address address = addresses.get(0);
                                JSONObject addressJsonObject = new JSONObject();
                                addressJsonObject.put("latitude", address.getLatitude());
                                addressJsonObject.put("longitude", address.getLongitude());
                                addressJsonObject.put("city", address.getLocality());
                                addressJsonObject.put("district", address.getSubAdminArea());
                                addressJsonObject.put("state", address.getAdminArea());
                                addressJsonObject.put("postal_code", address.getPostalCode());
                                addressJsonObject.put("country", address.getCountryName());
                                postData += "&location=" + addressJsonObject.toString();
                            }
                        } catch (SecurityException se) {
                            se.printStackTrace();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        webView.postUrl(url, EncodingUtils.getBytes(postData, "base64"));

                        Utils.triggerGAEventOnline(WebViewActivity.this, "Permission_Allow_" + screenName, "Logged_In", customerId);
                    }

                    @Override
                    public void onPermissionDenied() {
                        webView.postUrl(url, EncodingUtils.getBytes(postData, "base64"));
                        Utils.triggerGAEventOnline(WebViewActivity.this, "Permission_Denied_" + screenName, "Logged_In", customerId);
                    }

                    @Override
                    public void onPermissionPermanentlyDenied() {
                        webView.postUrl(url, EncodingUtils.getBytes(postData, "base64"));
                        Utils.triggerGAEventOnline(WebViewActivity.this, "Permission_Permanently_Denied_" + screenName, "Logged_In", customerId);
                    }
                });

                webView.setWebViewClient(new WebViewClient() {

                    @Override
                    public void onPageStarted(WebView view, String url, Bitmap favicon) {
                        // TODO Auto-generated method stub
                        super.onPageStarted(view, url, favicon);
                        if (url.contains("intent://") && url.contains("mylibrary")) {
                            pDialog.dismiss();
                            Utils.showLibraryAndSyncData(WebViewActivity.this);
                            webView.destroy();
                            finish();
                        } else {
                            if (!pDialog.isShowing()) {
                                pDialog.show();
                            }
                        }
                    }

                    @Override
                    public void onPageFinished(WebView view, String url) {
                        // TODO Auto-generated method stub
                        super.onPageFinished(view, url);
                        if (pDialog.isShowing()) {
                            pDialog.dismiss();
                        }
                    }
                });
            } else {
                webView.destroy();
                finish();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        mPermissionHandler.onPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public void onBackPressed() {
        // TODO Auto-generated method stub
        if (productId != null) {
            if (startTime > 0) {
                long currentTime = System.currentTimeMillis();
                int differenceTimeInMinutes = (int) Math.ceil((double) (currentTime - startTime) / 60000);    //1000*60
                Log.i(productType.replace(" ", "_") + "_Close", productId + " (Time : " + differenceTimeInMinutes + " Minutes)");
                Utils.triggerGAEvent(this, productType.replace(" ", "_") + "_Close", productId, customerId, differenceTimeInMinutes);
            }
            startTime = 0;
            webView.destroy();
            finish();
        } else {
            if (webView.canGoBack()) {
                webView.goBack();
            } else {
                super.onBackPressed();
                webView.goBack();
                webView.destroy();
                finish();
            }
        }
    }

    @Override
    protected void onResume() {
        // TODO Auto-generated method stub
        super.onResume();
        Utils.triggerScreen(WebViewActivity.this, screenName);
    }

    private class WebAppInterface {

        Context mContext;

        public WebAppInterface(Context c) {
            // TODO Auto-generated constructor stub
            mContext = c;
        }

        @JavascriptInterface
        public void backPressed() {
            mHandler.post(new Runnable() {

                @Override
                public void run() {
                    // TODO Auto-generated method stub
                    onBackPressed();
                }
            });
        }

        @JavascriptInterface
        public void shareProduct(String productId, String shareSubject, String shareBody) {
            Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
            sharingIntent.setType("text/plain");
            sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, shareSubject);
            sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, shareBody);
            mContext.startActivity(Intent.createChooser(sharingIntent, "Share this Product Via :"));
            Utils.triggerGAEvent(mContext, "Product_Share", productId, customerId);
        }

        @JavascriptInterface
        public void showMyLibrary(String productTypes) {
            Utils.showLibrary(mContext, productTypes);
        }

        @JavascriptInterface
        public void showStore() {
            Utils.showStoreActivity(mContext);
        }

        @JavascriptInterface
        public void showCart() {
            Utils.triggerGAEvent(mContext, "CART", customerId, "");
            webView.loadUrl(Constants.CART_URL);
        }

        @JavascriptInterface
        public void showWishlist() {
            Utils.triggerGAEvent(mContext, "WISHLIST", customerId, "");
            webView.loadUrl(Constants.WISHLIST_URL);
        }

        @JavascriptInterface
        public void showRecommendations() {
            Utils.showRecommendationsActivity(mContext);
        }

        @JavascriptInterface
        public void showInviteFriend() {
            webView.loadUrl(Constants.REFER_N_EARN_URL);
        }

        @JavascriptInterface
        public void syncData() {
            syncDataFromAPI();
        }

        @JavascriptInterface
        public void showNotifications() {
            Utils.showNotificationsActivity(mContext);
        }

        @JavascriptInterface
        public void showFeedback() {
            Utils.showFeedbackActivity(mContext);
        }

        @JavascriptInterface
        public void showStreamSelection() {
            Utils.showStreamsSelectionActivity(mContext);
        }

        @JavascriptInterface
        public void showAboutUs() {
            Utils.showAboutUsActivity(mContext);
        }

        @JavascriptInterface
        public void showWhatsnew() {
            Utils.triggerGAEvent(mContext, "WHAT'S NEW", customerId, "");
            webView.loadUrl(Constants.WHATSNEW_URL);
        }

        @JavascriptInterface
        public void showMoreApps() {
            Utils.showMoreAppsActivity(mContext);
        }

        @JavascriptInterface
        public void showSettings() {
            Utils.triggerGAEvent(mContext, "Settings", customerId, "");
            webView.loadUrl(Constants.ACCOUNT_SETTINGS_URL);
        }

        @JavascriptInterface
        public void showProfile() {
            Utils.triggerGAEvent(mContext, "Profile", customerId, "");
            webView.loadUrl(Constants.ACCOUNT_PROFILE_URL);
        }

        @JavascriptInterface
        public void logout() {
            Utils.logout(mContext);
        }

        @JavascriptInterface
        public void showInviteFriend(String shareSubject, String shareBody) {
            Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
            sharingIntent.setType("text/plain");
            sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, shareSubject);
            sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, shareBody);
            mContext.startActivity(Intent.createChooser(sharingIntent, "Invite & Earn Via :"));
            Utils.triggerGAEvent(mContext, "Invite_n_Earn", customerId, "");
        }

        @JavascriptInterface
        public void showPremiumActivity() {
            Utils.showPremiumActivity(mContext);
        }

        @JavascriptInterface
        public void pageLoaded() {
            pDialog.dismiss();
        }
    }

    @Override
    protected void syncDataFromAPI() {
        // TODO Auto-generated method stub
        if (Utils.isNetworkConnected(this)) {
            Utils.triggerGAEvent(this, "REFRESH", customerId, "");
            new PrepareStore(WebViewActivity.this, true).execute();
        } else {
            Utils.networkNotAvailableAlertBox(this);
        }
    }

    private class PrepareStore extends SyncData {

        public PrepareStore(Context context, boolean syncFromAPI) {
            super(context, syncFromAPI);
            // TODO Auto-generated constructor stub
        }

        @Override
        protected void onPostExecute(String result) {
            // TODO Auto-generated method stub
            super.onPostExecute(result);

            if (pDialog.isShowing()) {
                pDialog.dismiss();
            }
        }
    }
}

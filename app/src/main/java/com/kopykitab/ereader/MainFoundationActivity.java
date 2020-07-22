package com.kopykitab.ereader;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.kopykitab.ereader.settings.AppSettings;
import com.kopykitab.ereader.settings.Constants;
import com.kopykitab.ereader.settings.Utils;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.LinkedList;

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public abstract class MainFoundationActivity extends AppCompatActivity {
    protected String customerId;
    protected SharedPreferences sharedPrefs = null;

    protected abstract void syncDataFromAPI();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!AppSettings.getInstance(this).isConfigurationDone()) {
            Utils.showLoginActivity(this);
        } else {
            customerId = AppSettings.getInstance(this).get("CUSTOMER_ID");

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                Window window = getWindow();
                window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
                window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
                window.setStatusBarColor(getResources().getColor(R.color.action_bar_background_dark));
            }

            String gcmRegId = AppSettings.getInstance(this).get("GCM_REG_ID");
            if (Utils.isNetworkConnected(this) && Utils.checkPlayServices(this, 9000) && (gcmRegId == null || gcmRegId.isEmpty())) {
                new RegisterIntoFCM().execute();
            }
        }
    }

    protected class CustomerDetails extends AsyncTask<Void, Void, String> {

        @Override
        protected String doInBackground(Void... params) {
            String response = null;
            try {
                response = Utils.sendPost(Constants.LOGIN_API_URL + "getCustomerDetails", "customer_id=" + URLEncoder.encode(customerId, "UTF-8") + "&login_source=" + URLEncoder.encode(Constants.LOGIN_SOURCE, "UTF-8"));
            } catch (Exception e) {
                e.printStackTrace();
            }

            return response;
        }

        @Override
        protected void onPostExecute(String result) {
            // TODO Auto-generated method stub
            super.onPostExecute(result);
            if (result != null) {
                Log.i("Customer Details Response", result);
                try {
                    JSONObject jsonObject = new JSONObject(result);
                    if (jsonObject.getBoolean("status")) {
                        JSONObject customerDetailsJsonObject = jsonObject.getJSONObject("customer_details");
                        AppSettings.getInstance(MainFoundationActivity.this).set("customer_email", customerDetailsJsonObject.getString("email"));
                        AppSettings.getInstance(MainFoundationActivity.this).set("customer_name", customerDetailsJsonObject.getString("firstname") + " " + customerDetailsJsonObject.getString("lastname"));
                        AppSettings.getInstance(MainFoundationActivity.this).set("email_status", customerDetailsJsonObject.getString("email_status"));
                        AppSettings.getInstance(MainFoundationActivity.this).set("customer_recommendations_interval", customerDetailsJsonObject.getString("recommendations_interval"));
                        if (customerDetailsJsonObject.has("offers")) {
                            AppSettings.getInstance(MainFoundationActivity.this).set("offers", customerDetailsJsonObject.getString("offers"));
                        }
                        if (customerDetailsJsonObject.has("attributes")) {
                            JSONObject attributesObject = customerDetailsJsonObject.getJSONObject("attributes");
                            Utils.annotationsPrintTaken = attributesObject.getInt("annotations_print_taken_count");
                            Utils.annotationsPrintLimit = attributesObject.getInt("annotations_print_limit");
                        }
                        if (customerDetailsJsonObject.has("is_dirty") && Integer.parseInt(customerDetailsJsonObject.getString("is_dirty")) == 1) {
                            syncDataFromAPI();
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    protected class SyncData extends AsyncTask<String, Void, String> {
        private Context mContext;
        private boolean syncFromAPI;
        private String login_source;
        private File syncDataFile;
        private PowerManager.WakeLock wakeLock;

        protected SyncData(Context context, boolean sync) {
            mContext = context;
            syncFromAPI = sync;

            customerId = AppSettings.getInstance(mContext).get("CUSTOMER_ID");
            login_source = Constants.LOGIN_SOURCE;
            syncDataFile = new File(Utils.getDirectory(mContext) + Constants.SYNCDATA_JSON_FILENAME);

            if (!syncFromAPI) {
                if (!syncDataFile.exists()) {
                    syncFromAPI = true;
                } else {
                    File tempDir = new File(Utils.getDirectory(mContext));
                    File[] tempFiles = tempDir.listFiles(new FilenameFilter() {

                        @Override
                        public boolean accept(File dir, String name) {
                            return name.endsWith("_by_categories.json");
                        }
                    });
                    if (tempFiles != null && tempFiles.length < 2) {
                        syncFromAPI = true;
                    }
                }
            }
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            PowerManager pm = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, getClass().getName());
            wakeLock.acquire();
        }

        @Override
        protected String doInBackground(String... params) {

            String response = null;
            if (!Utils.isNetworkConnected(mContext)) {
                syncFromAPI = false;
            }

            try {
                if (syncFromAPI) {
                    Utils.deleteFiles(mContext);

                    Log.i("Sync Data", "Getting Data from API");
                    response = Utils.sendGet(mContext, Constants.SYNCDATA_API_URL, "customer_id=" + URLEncoder.encode(customerId, "UTF-8") + "&login_source=" + URLEncoder.encode(login_source, "UTF-8"));

                    Log.i("Writing Output to file " + syncDataFile.getName(), response);
                    FileWriter newJsonFile = new FileWriter(syncDataFile);
                    newJsonFile.write(Base64.encodeToString(response.getBytes(), Base64.DEFAULT));
                    newJsonFile.flush();
                    newJsonFile.close();

                    JSONObject jsonResponseObject = new JSONObject(response);
                    Constants.setImageBaseUrl(jsonResponseObject.getString("image_base_url"));

                    JSONArray resultsArray = jsonResponseObject.getJSONArray("results");
                    LinkedList<HashMap<String, String>> menuItems = new LinkedList<HashMap<String, String>>();
                    for (int i = 0; i < resultsArray.length(); i++) {
                        JSONObject productType = resultsArray.getJSONObject(i);

                        HashMap<String, String> item = new HashMap<String, String>();
                        if (productType.getString("product_type").equals("ebook")) {
                            String menuItemText = productType.getString("text");
                            item.put("text", "All " + menuItemText);
                            item.put("left_drawer_icon", productType.getString("left_drawer_icon"));
                            item.put("product_type", productType.getString("product_type"));
                            menuItems.add(item);

                            menuItemText = menuItemText.replaceAll("\\(\\d+\\)", "");
                            item = new HashMap<String, String>();
                            item.put("text", "Downloaded " + menuItemText);
                            item.put("left_drawer_icon", "downloaded_" + productType.getString("left_drawer_icon"));
                            item.put("product_type", "downloaded_" + productType.getString("product_type"));
                            menuItems.add(item);
                        } else {
                            item.put("text", productType.getString("text"));
                            item.put("left_drawer_icon", productType.getString("left_drawer_icon"));
                            item.put("product_type", productType.getString("product_type"));
                            menuItems.add(item);
                        }

                        // Write API data based on product type in different files
                        String jsonFileName = productType.getString("product_type").replaceAll("(\\s|,)+", "_") + "_by_categories.json";
                        File file = new File(Utils.getDirectory(mContext) + jsonFileName);
                        FileWriter fw = new FileWriter(file);
                        if (!file.exists()) {
                            file.createNewFile();
                        }
                        fw.write(Base64.encodeToString(productType.toString().getBytes(), Base64.DEFAULT));
                        fw.flush();
                        fw.close();
                    }

                    // Save menu items in shared preferences
                    if (sharedPrefs == null) {
                        sharedPrefs = mContext.getSharedPreferences("kk_shared_prefs", Context.MODE_PRIVATE);
                    }
                    SharedPreferences.Editor collection = sharedPrefs.edit();
                    collection.putString("customerId", customerId);
                    collection.putString("image_base_url", jsonResponseObject.getString("image_base_url"));
                    collection.putString("menu_items", new Gson().toJson(menuItems));
                    collection.commit();

                    // Set menu items
                    Utils.setMenuItems(menuItems);
                } else {
                    if (sharedPrefs == null) {
                        sharedPrefs = mContext.getSharedPreferences("kk_shared_prefs", Context.MODE_PRIVATE);
                    }
                    String custId = sharedPrefs.getString("customerId", null);
                    if (custId != null && custId.equals(customerId)) {
                        String imageBaseURL = sharedPrefs.getString("image_base_url", null);
                        LinkedList<HashMap<String, String>> menuItems = new Gson().fromJson(new String(sharedPrefs.getString("menu_items", null)), new TypeToken<LinkedList<HashMap<String, String>>>() {
                        }.getType());

                        // Set base image URL & menu items
                        Constants.setImageBaseUrl(imageBaseURL);
                        Utils.setMenuItems(menuItems);
                    } else {
                        Log.i("Sync Data", "Getting Data from Storage");
                        FileInputStream jsonFile = new FileInputStream(syncDataFile);
                        byte[] fileRead = new byte[jsonFile.available()];
                        jsonFile.read(fileRead);
                        jsonFile.close();
                        response = new String(Base64.decode(fileRead, Base64.DEFAULT));

                        JSONObject jsonResponseObject = new JSONObject(response);
                        Constants.setImageBaseUrl(jsonResponseObject.getString("image_base_url"));

                        JSONArray resultsArray = jsonResponseObject.getJSONArray("results");
                        LinkedList<HashMap<String, String>> menuItems = new LinkedList<HashMap<String, String>>();
                        for (int i = 0; i < resultsArray.length(); i++) {
                            JSONObject productType = resultsArray.getJSONObject(i);

                            HashMap<String, String> item = new HashMap<String, String>();
                            if (productType.getString("product_type").equals("ebook")) {
                                String menuItemText = productType.getString("text");
                                item.put("text", "All " + menuItemText);
                                item.put("left_drawer_icon", productType.getString("left_drawer_icon"));
                                item.put("product_type", productType.getString("product_type"));
                                menuItems.add(item);

                                menuItemText = menuItemText.replaceAll("\\(\\d+\\)", "");
                                item = new HashMap<String, String>();
                                item.put("text", "Downloaded " + menuItemText);
                                item.put("left_drawer_icon", "downloaded_" + productType.getString("left_drawer_icon"));
                                item.put("product_type", "downloaded_" + productType.getString("product_type"));
                                menuItems.add(item);
                            } else {
                                item.put("text", productType.getString("text"));
                                item.put("left_drawer_icon", productType.getString("left_drawer_icon"));
                                item.put("product_type", productType.getString("product_type"));
                                menuItems.add(item);
                            }
                        }

                        // Save menu items in shared preferences
                        SharedPreferences.Editor collection = sharedPrefs.edit();
                        collection.putString("customerId", customerId);
                        collection.putString("image_base_url", jsonResponseObject.getString("image_base_url"));
                        collection.putString("menu_items", new Gson().toJson(menuItems));
                        collection.commit();

                        // Set menu items
                        Utils.setMenuItems(menuItems);
                    }
                }
            } catch (StackOverflowError sfe) {
                sfe.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            wakeLock.release();
        }
    }

    private class RegisterIntoFCM extends AsyncTask<Void, Void, String> {

        @Override
        protected String doInBackground(Void... params) {
            String regId = null;
            try {
                regId = FirebaseInstanceId.getInstance().getToken();
            } catch (Exception e) {
                e.printStackTrace();
            }

            return regId;
        }

        @Override
        protected void onPostExecute(final String regId) {
            if (regId != null && !regId.isEmpty()) {
                AppSettings.getInstance(MainFoundationActivity.this).set("GCM_REG_ID", regId);

                new Thread() {

                    @Override
                    public void run() {

                        String response = null;
                        try {
                            response = Utils.sendPost(MainFoundationActivity.this, Constants.STORE_GCM_DETAILS_URL, "customer_id=" + URLEncoder.encode(customerId, "UTF-8") + "&app_id=" + URLEncoder.encode(AppSettings.getInstance(MainFoundationActivity.this).get("APP_ID"), "UTF-8") + "&gcm_reg_id=" + URLEncoder.encode(regId, "UTF-8") + "&login_source=" + URLEncoder.encode(Constants.LOGIN_SOURCE, "UTF-8"));
                            Log.i("FCM Store Details", response);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }.start();
            }
        }
    }

    protected class GetLatestVersion extends AsyncTask<Void, Void, String> {

        @Override
        protected String doInBackground(Void... params) {
            // TODO Auto-generated method stub
            String latestVersion = null;

            try {
                String url = "https://play.google.com/store/apps/details?id=" + getPackageName();
                Document doc = Jsoup.connect(url).get();
                latestVersion = doc.getElementsByAttributeValue("itemprop", "softwareVersion").first().text();
            } catch (Exception e) {
                e.printStackTrace();
            }

            return latestVersion;
        }

        @Override
        protected void onPostExecute(final String result) {
            // TODO Auto-generated method stub
            super.onPostExecute(result);
            try {
                PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
                String currentVersion = packageInfo.versionName;
                if (result != null && !currentVersion.equals(result)) {
                    final AlertDialog alertDialog = Utils.createAlertBox(MainFoundationActivity.this);
                    alertDialog.setCanceledOnTouchOutside(true);
                    alertDialog.show();
                    AppSettings.getInstance(MainFoundationActivity.this).set("version_popup_expiry_time", "" + (System.currentTimeMillis() + 1296000000L));
                    ((TextView) alertDialog.findViewById(R.id.dialog_title)).setText("New Version " + result + " Available");
                    ((TextView) alertDialog.findViewById(R.id.dialog_message)).setText("Free upgrade to use more Features, Faster ebook downloads, Night mode and much better reading experience in eReader. Click to upgrade now.");
                    ((LinearLayout) alertDialog.findViewById(R.id.dialog_one_button)).setVisibility(View.VISIBLE);
                    Button alertDialogButton = (Button) alertDialog.findViewById(R.id.dialog_one_button_button);
                    alertDialogButton.setText("Update Now");
                    alertDialogButton.setOnClickListener(new OnClickListener() {

                        @Override
                        public void onClick(View paramView) {
                            // TODO Auto-generated method stub
                            String url = null;
                            try {
                                getPackageManager().getPackageInfo("com.android.vending", 0);
                                url = "market://details?id=" + getPackageName();
                            } catch (Exception e) {
                                url = "https://play.google.com/store/apps/details?id=" + getPackageName();
                            }

                            if (Utils.isNetworkConnected(MainFoundationActivity.this)) {
                                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                                startActivity(intent);

                                Utils.triggerGAEvent(MainFoundationActivity.this, "Notifications", "New_Version_" + result, customerId);
                            } else {
                                Utils.networkNotAvailableAlertBox(MainFoundationActivity.this);
                            }
                            alertDialog.dismiss();
                        }
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
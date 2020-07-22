package com.kopykitab.ereader.components;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.PowerManager;
import android.util.Base64;
import android.util.Log;

import com.google.gson.Gson;
import com.kopykitab.ereader.settings.AppSettings;
import com.kopykitab.ereader.settings.Constants;
import com.kopykitab.ereader.settings.Utils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.LinkedList;

public class SyncDataFromAPI extends AsyncTask<String, Void, String> {
    private Context mContext;
    private ProgressDialog pDialog;
    private PowerManager.WakeLock wakeLock;
    private String customerId, login_source, loadingMessage;
    private File syncDataFile;
    private SharedPreferences sharedPrefs = null;

    public SyncDataFromAPI(Context mContext, String loadingMessage) {
        this.mContext = mContext;
        this.loadingMessage = loadingMessage;

        customerId = AppSettings.getInstance(mContext).get("CUSTOMER_ID");
        login_source = Constants.LOGIN_SOURCE;
        syncDataFile = new File(Utils.getDirectory(mContext) + Constants.SYNCDATA_JSON_FILENAME);
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();

        pDialog = new ProgressDialog(mContext);
        pDialog.setMessage(loadingMessage);
        pDialog.setCancelable(false);
        pDialog.show();

        PowerManager pm = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, getClass().getName());
        wakeLock.acquire();
        pDialog.show();
    }

    @Override
    protected String doInBackground(String... strings) {
        if (Utils.isNetworkConnected(mContext)) {
            try {
                Utils.deleteFiles(mContext);

                Log.i("Sync Data", "Getting Data from API");
                String response = Utils.sendGet(mContext, Constants.SYNCDATA_API_URL, "customer_id=" + URLEncoder.encode(customerId, "UTF-8") + "&login_source=" + URLEncoder.encode(login_source, "UTF-8"));

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
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return null;
    }

    @Override
    protected void onPostExecute(String s) {
        super.onPostExecute(s);
        wakeLock.release();
        if (pDialog != null) {
            pDialog.dismiss();
        }

        String className = mContext.getClass().getSimpleName();
        if (className.equals("LibraryActivity")) {
            Activity activity = (Activity) mContext;
            mContext.startActivity(activity.getIntent());
            activity.overridePendingTransition(0, 0);
            activity.finish();
        }
    }
}


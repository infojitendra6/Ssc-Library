package com.kopykitab.ereader;

import android.app.Application;
import android.content.ContentValues;
import android.content.Context;
import android.os.AsyncTask;
import android.support.multidex.MultiDex;

import com.kopykitab.ereader.models.DatabaseHelper;
import com.kopykitab.ereader.settings.AppSettings;
import com.kopykitab.ereader.settings.Constants;
import com.kopykitab.ereader.settings.DatabaseConstants.StoreBannersEntry;
import com.kopykitab.ereader.settings.DatabaseConstants.RecommendationsEntry;
import com.kopykitab.ereader.settings.DatabaseConstants.StoreCategoryEntry;
import com.kopykitab.ereader.settings.Utils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URLEncoder;
import java.util.LinkedList;

public class KopykitabApplication extends Application {

    private static KopykitabApplication singleton;
    private String customerId, login_source;

    public static KopykitabApplication getInstance() {
        return singleton;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        singleton = this;
    }

    public void prepareCachedAPI(Context mContext) {
        customerId = AppSettings.getInstance(mContext).get("CUSTOMER_ID");
        login_source = Constants.LOGIN_SOURCE;

        new GetStoreCategories(mContext).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

        new GetRecommendations(mContext).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

        new GetBanners(mContext).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private class GetStoreCategories extends AsyncTask<String, Void, String> {
        private String response = null;
        private Context mContext;

        public GetStoreCategories(Context mContext) {
            this.mContext = mContext;
        }

        @Override
        protected String doInBackground(String... strings) {
            try {
                response = Utils.sendGet(Constants.CATEGORIES_URL, "customer_id=" + URLEncoder.encode(customerId, "UTF-8") + "&login_source=" + URLEncoder.encode(login_source, "UTF-8"));
            } catch (Exception e) {
                e.printStackTrace();
            }
            return response;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            if (result != null) {
                try {
                    LinkedList<ContentValues> valuesList = new LinkedList<ContentValues>();
                    JSONArray topCategoriesJsonArray = new JSONArray(result);
                    for (int i = 0; i < topCategoriesJsonArray.length(); i++) {
                        JSONObject parentObject = topCategoriesJsonArray.getJSONObject(i);
                        String parentName = parentObject.getString("name"), parentUrl = "";
                        int parentLevel = 0;

                        if (parentObject.has("url")) {
                            parentUrl = parentObject.getString("url");
                        }

                        ContentValues values = new ContentValues();
                        /*values.put(StoreCategoryEntry.COLUMN_ID, "");*/
                        values.put(StoreCategoryEntry.COLUMN_NAME, parentName);
                        values.put(StoreCategoryEntry.COLUMN_PARENT_NAME, "");
                        values.put(StoreCategoryEntry.COLUMN_URL, parentUrl);
                        values.put(StoreCategoryEntry.COLUMN_LEVEL, parentLevel);
                        valuesList.add(values);

                        JSONArray subCategoriesJsonArray = topCategoriesJsonArray.getJSONObject(i).getJSONArray("children");
                        for (int j = 0; j < subCategoriesJsonArray.length(); j++) {
                            JSONObject childObject = subCategoriesJsonArray.getJSONObject(j);
                            String childName = childObject.getString("name"), childUrl = childObject.getString("url");
                            int childLevel = 1;
                            values = new ContentValues();
                            /*values.put(StoreCategoryEntry.COLUMN_ID, "");*/
                            values.put(StoreCategoryEntry.COLUMN_NAME, childName);
                            values.put(StoreCategoryEntry.COLUMN_PARENT_NAME, parentName);
                            values.put(StoreCategoryEntry.COLUMN_URL, childUrl);
                            values.put(StoreCategoryEntry.COLUMN_LEVEL, childLevel);
                            valuesList.add(values);
                        }
                    }

                    //before inserting truncate table
                    DatabaseHelper.getInstance(mContext).deleteData("DELETE FROM " + StoreCategoryEntry.TABLE_NAME);

                    //insert into database
                    DatabaseHelper.getInstance(mContext).insertMultipleData(StoreCategoryEntry.TABLE_NAME, valuesList);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private class GetRecommendations extends AsyncTask<String, Void, String> {
        private String response = null;
        private Context mContext;

        public GetRecommendations(Context mContext) {
            this.mContext = mContext;
        }

        @Override
        protected String doInBackground(String... strings) {
            try {
                response = Utils.sendPost(Constants.RECOMMENDATIONS_URL, "customer_id=" + URLEncoder.encode(customerId, "UTF-8") + "&source=" + URLEncoder.encode(login_source, "UTF-8"));
            } catch (Exception e) {
                e.printStackTrace();
            }
            return response;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            if (result != null) {
                try {
                    JSONObject recommendationObject = new JSONObject(result);
                    JSONArray recommendationBooksArray = recommendationObject.getJSONArray("products");
                    if (recommendationBooksArray.length() > 0) {
                        LinkedList<ContentValues> valuesList = new LinkedList<ContentValues>();
                        for (int i = 0; i < recommendationBooksArray.length(); i++) {
                            JSONObject book = recommendationBooksArray.getJSONObject(i);

                            ContentValues values = new ContentValues();
                            values.put(RecommendationsEntry.COLUMN_PRODUCT_ID, book.getString("product_id").trim());
                            values.put(RecommendationsEntry.COLUMN_IMAGE, book.getString("image"));
                            values.put(RecommendationsEntry.COLUMN_NAME, book.getString("name").trim());
                            values.put(RecommendationsEntry.COLUMN_DESCRIPTION, book.getString("description"));
                            values.put(RecommendationsEntry.COLUMN_RENTAL_PERIOD, book.getString("rental_period"));
                            values.put(RecommendationsEntry.COLUMN_PRICE_1, book.getString("price_1"));
                            values.put(RecommendationsEntry.COLUMN_PRICE_2, book.getString("price_2"));
                            values.put(RecommendationsEntry.COLUMN_FREE_PRODUCT, book.getString("free_product"));
                            values.put(RecommendationsEntry.COLUMN_HREF, book.getString("href"));
                            values.put(RecommendationsEntry.COLUMN_PRODUCT_TYPE, book.getString("product_type"));
                            valuesList.add(values);

                        }

                        //before inserting truncate table
                        DatabaseHelper.getInstance(mContext).deleteData("DELETE FROM " + RecommendationsEntry.TABLE_NAME);

                        //insert into database
                        DatabaseHelper.getInstance(mContext).insertMultipleData(RecommendationsEntry.TABLE_NAME, valuesList);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private class GetBanners extends AsyncTask<String, Void, String> {
        private String response = null;
        private Context mContext;

        public GetBanners(Context mContext) {
            this.mContext = mContext;
        }

        @Override
        protected String doInBackground(String... strings) {
            try {
                response = Utils.sendPost(Constants.BANNERS_URL, "customer_id=" + URLEncoder.encode(customerId, "UTF-8") + "&source=" + URLEncoder.encode(login_source, "UTF-8"));
            } catch (Exception e) {
                e.printStackTrace();
            }
            return response;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            if (result != null) {
                try {
                    JSONArray bannerArray = new JSONArray(result);
                    if (bannerArray.length() > 0) {
                        LinkedList<ContentValues> valuesList = new LinkedList<ContentValues>();
                        for (int i = 0; i < bannerArray.length(); i++) {
                            JSONObject banner = bannerArray.getJSONObject(i);

                            ContentValues values = new ContentValues();
                            /*values.put(StoreBannersEntry.COLUMN_ID, "");*/
                            values.put(StoreBannersEntry.COLUMN_IMAGE_URL, banner.getString("image_url"));
                            values.put(StoreBannersEntry.COLUMN_DESCRIPTION, banner.getString("description"));
                            values.put(StoreBannersEntry.COLUMN_HREF, banner.getString("href"));
                            valuesList.add(values);
                        }

                        //before inserting truncate table
                        DatabaseHelper.getInstance(mContext).deleteData("DELETE FROM " + StoreBannersEntry.TABLE_NAME);

                        //insert into database
                        DatabaseHelper.getInstance(mContext).insertMultipleData(StoreBannersEntry.TABLE_NAME, valuesList);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }
}

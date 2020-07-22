package com.kopykitab.ereader.settings;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.provider.Settings.Secure;
import android.support.design.widget.BottomSheetDialog;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.telephony.TelephonyManager;
import android.text.Html;
import android.util.Base64;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.login.LoginResult;
import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.kopykitab.ereader.FeedbackActivity;
import com.kopykitab.ereader.ForgotPassword;
import com.kopykitab.ereader.KopykitabApplication;
import com.kopykitab.ereader.LibraryActivity;
import com.kopykitab.ereader.LoginActivity;
import com.kopykitab.ereader.LoginMainActivity;
import com.kopykitab.ereader.NotificationActivity;
import com.kopykitab.ereader.PremiumActivity;
import com.kopykitab.ereader.R;
import com.kopykitab.ereader.RecommendationsActivity;
import com.kopykitab.ereader.RegisterAccount;
import com.kopykitab.ereader.StoreActivity;
import com.kopykitab.ereader.StreamsSelectionActivity;
import com.kopykitab.ereader.WebViewActivity;
import com.kopykitab.ereader.components.NetworkChangeReceiver;
import com.kopykitab.ereader.models.BookItem;
import com.kopykitab.ereader.models.DatabaseHelper;
import com.kopykitab.ereader.settings.DatabaseConstants.DatabaseEntry;
import com.kopykitab.ereader.components.adapters.PermissionAdapter;
import com.nostra13.universalimageloader.cache.disc.impl.UnlimitedDiskCache;
import com.nostra13.universalimageloader.cache.disc.naming.FileNameGenerator;
import com.nostra13.universalimageloader.cache.memory.impl.WeakMemoryCache;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.QueueProcessingType;
import com.nostra13.universalimageloader.core.download.BaseImageDownloader;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

class ImageFileNameGenertor implements FileNameGenerator {

    @Override
    public String generate(String imageUri) {
        // TODO Auto-generated method stub
        return imageUri.substring(imageUri.lastIndexOf("/") + 1, imageUri.length());
    }
}

class ImageDownloader extends BaseImageDownloader {

    private Context mContext;

    public ImageDownloader(Context context) {
        super(context);
        mContext = context;
    }

    @Override
    protected HttpURLConnection createConnection(String url, Object extra) throws IOException {
        // TODO Auto-generated method stub
        HttpURLConnection conn = super.createConnection(url, extra);
        try {
            PackageInfo packageInfo = mContext.getPackageManager().getPackageInfo(mContext.getPackageName(), 0);
            conn.setRequestProperty("User-Agent", System.getProperty("http.agent") + " [" + packageInfo.packageName + "/image/" + packageInfo.versionName + "]");
        } catch (Exception e) {
            e.printStackTrace();
            conn.setRequestProperty("User-Agent", System.getProperty("http.agent") + " [" + mContext.getPackageName() + "/image]");
        }

        return conn;
    }
}

public final class Utils {

    private static ImageLoader imageLoader, imageLoaderObj, imageLoaderOnline;
    private static Tracker mTracker;
    private static LinkedList<HashMap<String, String>> menuItems;
    private static String libraryView = "GridView";
    private static AlertDialog permissionDialog;
    private static String searchInStoreKeyword = "";
    public static int annotationsPrintTaken = 0, annotationsPrintLimit = -1;

    public static ImageLoader getImageLoader(Context context) {
        if (hasPermissions(context, Constants.STORAGE_PERMISSION)) {
            if (imageLoaderObj == null) {
                DisplayImageOptions defaultDisplayImageOptions = new DisplayImageOptions.Builder()
                        .cacheInMemory(true)
                        .cacheOnDisk(true)
                        .showImageForEmptyUri(R.drawable.cover_unavailable)
                        .showImageOnFail(R.drawable.cover_unavailable)
                        .build();

                ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(context)
                        .threadPriority(Thread.MAX_PRIORITY)
                        .memoryCache(new WeakMemoryCache())
                        .diskCache(new UnlimitedDiskCache(new File(getImageDirectory(context)), new File(getImageDirectory(context)), new ImageFileNameGenertor()))
                        .defaultDisplayImageOptions(defaultDisplayImageOptions)
                        .threadPoolSize(5)
                        .tasksProcessingOrder(QueueProcessingType.FIFO)
                        .imageDownloader(new ImageDownloader(context))
                        .build();

                imageLoaderObj = ImageLoader.getInstance();
                imageLoaderObj.init(config);
            }

            return imageLoaderObj;
        } else {
            if (imageLoader == null) {
                DisplayImageOptions defaultDisplayImageOptions = new DisplayImageOptions.Builder()
                        .cacheInMemory(true)
                        .cacheOnDisk(true)
                        .showImageForEmptyUri(R.drawable.cover_unavailable)
                        .showImageOnFail(R.drawable.cover_unavailable)
                        .build();

                ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(context)
                        .threadPriority(Thread.MAX_PRIORITY)
                        .memoryCache(new WeakMemoryCache())
                        .defaultDisplayImageOptions(defaultDisplayImageOptions)
                        .threadPoolSize(5)
                        .tasksProcessingOrder(QueueProcessingType.FIFO)
                        .imageDownloader(new ImageDownloader(context))
                        .build();

                imageLoader = ImageLoader.getInstance();
                imageLoader.init(config);
            }

            return imageLoader;
        }
    }

    public static ImageLoader getImageLoaderOnline(Context context) {
        if (imageLoaderOnline == null) {
            DisplayImageOptions defaultDisplayImageOptions = new DisplayImageOptions.Builder()
                    .cacheInMemory(true)
                    .cacheOnDisk(true)
                    .showImageForEmptyUri(R.drawable.cover_unavailable)
                    .showImageOnFail(R.drawable.cover_unavailable)
                    .build();

            ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(context)
                    .threadPriority(Thread.MAX_PRIORITY)
                    .memoryCache(new WeakMemoryCache())
                    .defaultDisplayImageOptions(defaultDisplayImageOptions)
                    .threadPoolSize(5)
                    .tasksProcessingOrder(QueueProcessingType.FIFO)
                    .imageDownloader(new ImageDownloader(context))
                    .build();

            imageLoaderOnline = ImageLoader.getInstance();
            imageLoaderOnline.init(config);
        }

        return imageLoaderOnline;
    }

    public static String getDirectory(Context context) {
        String externalStorageState = Environment.getExternalStorageState();
        String directoryPath = "";
        String customerId = AppSettings.getInstance(context).get("CUSTOMER_ID");
        if (Environment.MEDIA_MOUNTED.equals(externalStorageState)) {
            directoryPath = Environment.getExternalStorageDirectory().getPath() + "/.jitu/" + customerId + "/";
        } else {
            directoryPath = context.getFilesDir().getPath() + "/.jitu/" + customerId + "/";
        }

        File downloadDirectory = new File(directoryPath);
        if (!downloadDirectory.exists()) {
            downloadDirectory.mkdirs();
        }

        return directoryPath;
    }

    public static String getImageDirectory(Context context) {
        String imageDirectoryPath = getDirectory(context) + "images/";
        File imageDirectory = new File(imageDirectoryPath);
        if (!imageDirectory.exists()) {
            imageDirectory.mkdir();
        }

        return imageDirectoryPath;
    }

    public static String getFileDownloadPath(Context context, String url) {
        String fileName = url.substring(url.lastIndexOf("/") + 1, url.length());

        return getDirectory(context) + fileName;
    }

    public static String getImageDownloadPath(Context context, String imageUrl) {
        String imageFileName = imageUrl.substring(imageUrl.lastIndexOf("/") + 1, imageUrl.length());

        return getImageDirectory(context) + imageFileName;
    }

    public static void deleteFiles(Context context) {
        File tempDir = new File(getDirectory(context));
        File[] tempFiles = tempDir.listFiles(new FilenameFilter() {

            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".json") && !(name.startsWith("annots") || name.startsWith("data"));
            }
        });

        if (tempFiles != null && tempFiles.length > 0) {
            for (File f : tempFiles) {
                f.delete();
            }
        }
    }

    public static Tracker getTracker(Context context) {
        if (mTracker == null) {
            mTracker = GoogleAnalytics.getInstance(context).newTracker(Constants.GA_TRACKING_ID);
        }

        return mTracker;
    }

    public static void openPDFFile(Context context, BookItem book) {
        String productId = book.getProductId();
        File file = new File(getFileDownloadPath(context, book.getProductLink()));
        Uri uri = Uri.parse(file.getAbsolutePath());
        Intent intent = new Intent(context, com.artifex.mupdfdemo.MuPDFActivity.class);
        intent.setAction(Intent.ACTION_VIEW);
        intent.setData(uri);
        HashMap<String, String> bookDetails = new HashMap<String, String>();
        bookDetails.put("name", book.getName());
        bookDetails.put("image_url", book.getImageURL());
        bookDetails.put("description", book.getDescription());
        bookDetails.put("product_id", book.getProductId());
        bookDetails.put("product_type", book.getProductType());
        bookDetails.put("option_name", book.getOptionName());
        bookDetails.put("option_value", book.getOptionValue());
        bookDetails.put("product_link", book.getProductLink());
        intent.putExtra("product_detail", bookDetails);

        context.startActivity(intent);
        Log.i("Pdf_Open", productId);
        triggerGAEvent(context, "Pdf_Open", productId, AppSettings.getInstance(context).get("CUSTOMER_ID"));
    }

    public static String getAppId(Context context) {
        SharedPreferences credentials = context.getSharedPreferences("first_install", 0);
        String appId = credentials.getString("APP_ID", "");
        if (appId == null || appId.isEmpty()) {
            if (checkPermission(context, Constants.PHONE_PERMISSION)) {
                TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
                appId = telephonyManager.getDeviceId();
            }
            if (appId == null || appId.isEmpty()) {
                appId = Secure.getString(context.getContentResolver(), Secure.ANDROID_ID);
            }
        }

        Log.i("Your Device Id", appId);
        return appId;
    }

    public static boolean isNetworkConnected(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return (activeNetworkInfo != null && activeNetworkInfo.isConnected());
    }

    public static void networkNotAvailableAlertBox(final Context context) {
        final AlertDialog alertBox = createAlertBox(context);
        alertBox.show();
        ((ImageView) alertBox.findViewById(R.id.dialog_icon)).setImageResource(R.drawable.error_icon);
        ((TextView) alertBox.findViewById(R.id.dialog_title)).setText("No Internet Connection");
        ((TextView) alertBox.findViewById(R.id.dialog_message)).setText("Please check your Wi-Fi or mobile network connection and try again.");
        ((LinearLayout) alertBox.findViewById(R.id.dialog_two_button)).setVisibility(View.VISIBLE);
        Button alertBoxButton1 = (Button) alertBox.findViewById(R.id.dialog_two_button_button1);
        alertBoxButton1.setText("Settings");
        alertBoxButton1.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View paramView) {
                // TODO Auto-generated method stub
                Activity activity = (Activity) context;
                activity.startActivityForResult(new Intent(Settings.ACTION_SETTINGS), 0);
                alertBox.dismiss();
            }
        });
        Button alertBoxButton2 = (Button) alertBox.findViewById(R.id.dialog_two_button_button2);
        alertBoxButton2.setText("Cancel");
        alertBoxButton2.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View paramView) {
                // TODO Auto-generated method stub
                alertBox.dismiss();
            }
        });
    }

    public static void storeBooksList(Context context, List<HashMap<String, String>> booksListJson, String productTypes) {
        try {
            String jsonFileName = productTypes.replaceAll("(\\s|,)+", "_") + "_by_categories.json";
            Log.i("Writing Output to file " + jsonFileName, booksListJson.toString());
            FileWriter newJsonFile = new FileWriter(getDirectory(context) + jsonFileName);
            newJsonFile.write(Base64.encodeToString(new Gson().toJson(booksListJson).getBytes(), Base64.DEFAULT));
            newJsonFile.flush();
            newJsonFile.close();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public static List<HashMap<String, String>> readBooksList(Context context, String productTypes) {
        Set<HashMap<String, String>> booksList = new HashSet<HashMap<String, String>>();
        List<HashMap<String, String>> finalBooksList = new ArrayList<HashMap<String, String>>();
        try {
            String tempProductTypes = productTypes;
            if (tempProductTypes.startsWith("downloaded_")) {
                productTypes = productTypes.replace("downloaded_", "");
            }
            String jsonFileName = productTypes.replaceAll("(\\s|,)+", "_") + "_by_categories.json";
            File jsonFile = new File(getDirectory(context) + jsonFileName);
            long serverTime = jsonFile.lastModified(), systemTime = new Date().getTime();

            FileInputStream jsonFileInputStream = new FileInputStream(jsonFile);
            byte[] fileRead = new byte[jsonFileInputStream.available()];
            jsonFileInputStream.read(fileRead);
            jsonFileInputStream.close();
            JSONObject productTypeJsonObject = new JSONObject(new String(Base64.decode(fileRead, Base64.DEFAULT)));
            JSONArray productsByCategories = productTypeJsonObject.getJSONArray("products");
            for (int i = 0; i < productsByCategories.length(); i++) {
                List<HashMap<String, String>> tempBooksList = new ArrayList<HashMap<String, String>>();
                tempBooksList = new Gson().fromJson(productsByCategories.getJSONObject(i).getJSONArray("products").toString(), new TypeToken<ArrayList<HashMap<String, String>>>() {
                }.getType());
                booksList.addAll(tempBooksList);
            }

            for (HashMap<String, String> book : booksList) {
                boolean shouldIncludeBook = true;
                if (tempProductTypes.startsWith("downloaded_")) {
                    File pdfFile = new File(getFileDownloadPath(context, book.get("product_link")));
                    if (!pdfFile.exists())
                        shouldIncludeBook = false;
                }
                if (shouldIncludeBook) {
                    book.put("image_url", Constants.IMAGE_URL + book.get("image_url"));
                    long diffDays = Math.abs(systemTime - serverTime) / 86400000; // Days calculation by 1000*60*60*24
                    if (book.get("left_days") == null || book.get("left_days").isEmpty() || book.get("left_days").equals("")) {
                        book.put("left_days", "999999");
                    }
                    int leftDays = Integer.valueOf(book.get("left_days")).intValue();
                    if (leftDays == 999999) {
                        finalBooksList.add(book);
                    } else {
                        if ((leftDays - diffDays) >= 0) {
                            book.put("left_days", Integer.valueOf(leftDays - Long.valueOf(diffDays).intValue()).toString());
                            finalBooksList.add(book);
                        } else {
                            removeExpiredFiles(context, book.get("product_link"), book.get("image_url"), book.get("product_id"));
                        }
                    }
                }
            }
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return finalBooksList;
    }

    public static List<HashMap<String, String>> readBooksList(Context context, String productTypes, int categoryPosition) {
        List<HashMap<String, String>> booksList = new ArrayList<HashMap<String, String>>();
        List<HashMap<String, String>> finalBooksList = new ArrayList<HashMap<String, String>>();
        try {
            String tempProductTypes = productTypes;
            if (tempProductTypes.startsWith("downloaded_")) {
                productTypes = productTypes.replace("downloaded_", "");
            }
            String jsonFileName = productTypes.replaceAll("(\\s|,)+", "_") + "_by_categories.json";
            File jsonFile = new File(getDirectory(context) + jsonFileName);
            long serverTime = jsonFile.lastModified(), systemTime = new Date().getTime();

            FileInputStream jsonFileInputStream = new FileInputStream(jsonFile);
            byte[] fileRead = new byte[jsonFileInputStream.available()];
            jsonFileInputStream.read(fileRead);
            jsonFileInputStream.close();
            JSONObject productTypeJsonObject = new JSONObject(new String(Base64.decode(fileRead, Base64.DEFAULT)));
            booksList = new Gson().fromJson(productTypeJsonObject.getJSONArray("products").getJSONObject(categoryPosition).getJSONArray("products").toString(), new TypeToken<ArrayList<HashMap<String, String>>>() {
            }.getType());

            for (HashMap<String, String> book : booksList) {
                boolean shouldIncludeBook = true;
                if (tempProductTypes.startsWith("downloaded_")) {
                    File pdfFile = new File(getFileDownloadPath(context, book.get("product_link")));
                    if (!pdfFile.exists())
                        shouldIncludeBook = false;
                }
                if (shouldIncludeBook) {
                    book.put("image_url", Constants.IMAGE_URL + book.get("image_url"));
                    long diffDays = Math.abs(systemTime - serverTime) / 86400000; // Days calculation by 1000*60*60*24
                    if (book.get("left_days") == null || book.get("left_days").isEmpty() || book.get("left_days").equals("")) {
                        book.put("left_days", "999999");
                    }
                    int leftDays = Integer.valueOf(book.get("left_days")).intValue();
                    if (leftDays == 999999) {
                        finalBooksList.add(book);
                    } else {
                        if ((leftDays - diffDays) >= 0) {
                            book.put("left_days", Integer.valueOf(leftDays - Long.valueOf(diffDays).intValue()).toString());
                            finalBooksList.add(book);
                        } else {
                            removeExpiredFiles(context, book.get("product_link"), book.get("image_url"), book.get("product_id"));
                        }
                    }
                }
            }
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return finalBooksList;
    }

    public static void removeExpiredFiles(Context context, String pdfUrl, String imageUrl, String productId) {
        File pdfFile = new File(getFileDownloadPath(context, pdfUrl));
        if (pdfFile.exists()) {
            Log.i("Remove Expired PDF Files", pdfUrl);
            pdfFile.delete();

            String customerId = AppSettings.getInstance(context).get("CUSTOMER_ID");
            triggerGAEvent(context, "Pdf_Expire", productId, customerId);

            // Delete downloaded book details from database
            String query = "DELETE FROM " + DatabaseEntry.DOWNLOADED_BOOKS_TABLE_NAME + " WHERE product_id = " + productId;
            DatabaseHelper.getInstance(context).deleteData(query);
        }

        File imageFile = new File(getImageDownloadPath(context, imageUrl));
        if (imageFile.exists()) {
            Log.i("Remove Expired Image Files", imageUrl);
            imageFile.delete();
        }
    }

    public static void showLibraryAndSyncData(Context context) {
        deleteFiles(context);

        Intent libraryIntent = new Intent(context, LibraryActivity.class);
        libraryIntent.putExtra("is_sync", true);
        context.startActivity(libraryIntent);

        Activity activity = (Activity) context;
        activity.overridePendingTransition(0, 0);
        activity.getIntent().addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        activity.finish();

        // finish LoginMainActivity
        LoginMainActivity.finishActivityHandler.sendEmptyMessage(0);
    }

    public static void showLibrary(Context context) {
        Intent libraryIntent = new Intent(context, LibraryActivity.class);
        context.startActivity(libraryIntent);

        Activity activity = (Activity) context;
        activity.overridePendingTransition(0, 0);
        activity.getIntent().addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        activity.finish();

        // finish LoginMainActivity
        LoginMainActivity.finishActivityHandler.sendEmptyMessage(0);
    }

    public static void showLibrary(Context context, String productTypes) {
        Intent myLibraryIntent = new Intent(context, LibraryActivity.class);
        myLibraryIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        myLibraryIntent.putExtra("product_type", productTypes);

        String className = context.getClass().getSimpleName();
        if (className.equals("LibraryActivity")) {
            Activity activity = (Activity) context;

            context.startActivity(myLibraryIntent);
            activity.overridePendingTransition(0, 0);
            activity.finish();
        } else {
            context.startActivity(myLibraryIntent);
        }
    }

    public static void showRegisterActivity(Context context) {
        Intent registerAccountIntent = new Intent(context, RegisterAccount.class);
        context.startActivity(registerAccountIntent);

        String className = context.getClass().getSimpleName();
        if (className.equals("LoginActivity")) {
            Activity activity = (Activity) context;
            activity.getIntent().addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
            activity.finish();
        }

    }

    public static void showForgotPasswordActivity(Context context) {
        Intent forgotPasswordIntent = new Intent(context, ForgotPassword.class);
        context.startActivity(forgotPasswordIntent);

    }

    public static void showLoginMainActivity(Context context) {
        Intent loginMainIntent = new Intent(context, LoginMainActivity.class);
        loginMainIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        context.startActivity(loginMainIntent);

        Activity activity = (Activity) context;
        activity.getIntent().addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        activity.finish();
    }

    public static void showLoginActivity(Context context) {
        Intent loginIntent = new Intent(context, LoginActivity.class);
        context.startActivity(loginIntent);

        String className = context.getClass().getSimpleName();
        if (className.equals("RegisterAccount")) {
            Activity activity = (Activity) context;
            activity.getIntent().addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
            activity.finish();
        }
    }

    public static void showStoreActivity(Context context) {
        triggerGAEvent(context, "STORE", AppSettings.getInstance(context).get("CUSTOMER_ID"), "");

        Intent storeIntent = new Intent(context, StoreActivity.class);
        context.startActivity(storeIntent);
    }

    public static void showPremiumActivity(Context context) {
        Intent premiumIntent = new Intent(context, PremiumActivity.class);
        context.startActivity(premiumIntent);
    }

    public static void showWebViewActivity(Context context, String url) {
        if (isNetworkConnected(context)) {
            Intent webViewIntent = new Intent(context, WebViewActivity.class);
            webViewIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            webViewIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            webViewIntent.putExtra("web_url", url);
            context.startActivity(webViewIntent);
            ((Activity) context).getIntent().addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        } else {
            networkNotAvailableAlertBox(context);
        }
    }

    public static void showCartActivity(Context context) {
        if (isNetworkConnected(context)) {
            String customerId = AppSettings.getInstance(context).get("CUSTOMER_ID");

            Intent webViewIntent = new Intent(context, WebViewActivity.class);
            webViewIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            webViewIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            webViewIntent.putExtra("web_url", Constants.CART_URL);
            context.startActivity(webViewIntent);
            ((Activity) context).getIntent().addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
            triggerGAEvent(context, "CART", customerId, "");
        } else {
            networkNotAvailableAlertBox(context);
        }
    }

    public static void showScorecardActivity(Context context) {
        if (isNetworkConnected(context)) {
            String customerId = AppSettings.getInstance(context).get("CUSTOMER_ID");

            Intent webViewIntent = new Intent(context, WebViewActivity.class);
            webViewIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            webViewIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            webViewIntent.putExtra("web_url", Constants.SCORECARD_URL);
            context.startActivity(webViewIntent);
            ((Activity) context).getIntent().addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
            triggerGAEvent(context, "CART", customerId, "");
        } else {
            networkNotAvailableAlertBox(context);
        }
    }

    public static void showNotificationsActivity(Context context) {
        if (isNetworkConnected(context)) {
            String customerId = AppSettings.getInstance(context).get("CUSTOMER_ID");

            Intent notificationsIntent = new Intent(context, NotificationActivity.class);
            context.startActivity(notificationsIntent);
            triggerGAEvent(context, "Notifications", "Menu", customerId);
        } else {
            networkNotAvailableAlertBox(context);
        }
    }

    public static void showWishlistActivity(Context context) {
        if (isNetworkConnected(context)) {
            String customerId = AppSettings.getInstance(context).get("CUSTOMER_ID");

            Intent webViewIntent = new Intent(context, WebViewActivity.class);
            webViewIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            webViewIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            webViewIntent.putExtra("web_url", Constants.WISHLIST_URL);
            context.startActivity(webViewIntent);
            ((Activity) context).getIntent().addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
            triggerGAEvent(context, "WISHLIST", customerId, "");
        } else {
            networkNotAvailableAlertBox(context);
        }
    }

    public static void showRecommendationsActivity(Context context) {
        if (isNetworkConnected(context)) {
            String customerId = AppSettings.getInstance(context).get("CUSTOMER_ID");

            Intent recommendationsIntent = new Intent(context, RecommendationsActivity.class);
            context.startActivity(recommendationsIntent);
            triggerGAEvent(context, "Recommendations", customerId, "");
        } else {
            networkNotAvailableAlertBox(context);
        }
    }

    public static void showStreamsSelectionActivity(Context context) {
        String customerId = AppSettings.getInstance(context).get("CUSTOMER_ID");

        triggerGAEvent(context, "Stream_Selection", customerId, "");
        Intent streamsSelectionIntent = new Intent(context, StreamsSelectionActivity.class);
        context.startActivity(streamsSelectionIntent);
    }

    public static void showFeedbackActivity(Context context) {
        if (isNetworkConnected(context)) {
            String customerId = AppSettings.getInstance(context).get("CUSTOMER_ID");

            triggerGAEvent(context, "Feedback", customerId, "");
            Intent notificationsIntent = new Intent(context, FeedbackActivity.class);
            context.startActivity(notificationsIntent);
        } else {
            networkNotAvailableAlertBox(context);
        }
    }

    public static void showInviteFriendActivity(final Context context) {
        if (isNetworkConnected(context)) {
            Intent webViewIntent = new Intent(context, WebViewActivity.class);
            webViewIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            webViewIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            webViewIntent.putExtra("web_url", Constants.REFER_N_EARN_URL);
            context.startActivity(webViewIntent);
            ((Activity) context).getIntent().addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        } else {
            networkNotAvailableAlertBox(context);
        }
    }

    public static void showAboutUsActivity(Context context) {
        String customerId = AppSettings.getInstance(context).get("CUSTOMER_ID");
        triggerGAEvent(context, "About_Us", customerId, "");

        final AlertDialog alertDialog = createAlertBox(context);
        alertDialog.setCanceledOnTouchOutside(true);
        alertDialog.show();
        ((ImageView) alertDialog.findViewById(R.id.dialog_icon)).setImageResource(R.drawable.about_us_icon);
        ((TextView) alertDialog.findViewById(R.id.dialog_title)).setText("About Us | Kopykitab.com");
        ((TextView) alertDialog.findViewById(R.id.dialog_message)).setText("Kopykitab is India's 1st digital & multiple publishers platform. Kopykitab has largest collection of e-Textbooks & Branded Digital Content in Higher & School Education.\n\nWe have strong foundation of leading publishers & tutorials as content partners.");
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

    public static void showWhatsnewActivity(Context context) {
        if (isNetworkConnected(context)) {
            String customerId = AppSettings.getInstance(context).get("CUSTOMER_ID");

            Intent webViewIntent = new Intent(context, WebViewActivity.class);
            webViewIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            webViewIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            webViewIntent.putExtra("web_url", Constants.WHATSNEW_URL);
            context.startActivity(webViewIntent);
            ((Activity) context).getIntent().addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
            triggerGAEvent(context, "WHAT'S NEW", customerId, "");
        } else {
            networkNotAvailableAlertBox(context);
        }
    }

    public static void showMoreAppsActivity(Context context) {
        if (isNetworkConnected(context)) {
            String customerId = AppSettings.getInstance(context).get("CUSTOMER_ID");

            triggerGAEvent(context, "MORE APPS", customerId, "");
            Intent notificationsIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/dev?id=4992030870609250669"));
            context.startActivity(notificationsIntent);
        } else {
            networkNotAvailableAlertBox(context);
        }
    }

    public static void showSettingsActivity(Context context) {
        if (isNetworkConnected(context)) {
            String customerId = AppSettings.getInstance(context).get("CUSTOMER_ID");

            Intent webViewIntent = new Intent(context, WebViewActivity.class);
            webViewIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            webViewIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            webViewIntent.putExtra("web_url", Constants.ACCOUNT_SETTINGS_URL);
            context.startActivity(webViewIntent);
            ((Activity) context).getIntent().addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
            triggerGAEvent(context, "Settings", customerId, "");
        } else {
            networkNotAvailableAlertBox(context);
        }
    }

    public static void showProfileActivity(Context context) {
        if (isNetworkConnected(context)) {
            String customerId = AppSettings.getInstance(context).get("CUSTOMER_ID");

            Intent webViewIntent = new Intent(context, WebViewActivity.class);
            webViewIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            webViewIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            webViewIntent.putExtra("web_url", Constants.ACCOUNT_PROFILE_URL);
            context.startActivity(webViewIntent);
            ((Activity) context).getIntent().addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
            triggerGAEvent(context, "Profile", customerId, "");
        } else {
            networkNotAvailableAlertBox(context);
        }
    }

    public static boolean checkPlayServices(Context context, int requestCode) {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(context);
        if (resultCode == ConnectionResult.SUCCESS) {
            Log.i("GCM", "Device supports play services.");
            return true;
        } else {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                Log.e("GCM", "Play services user recoverable error");
            } else {
                Log.e("GCM", "Device does not support play services");
            }
            return false;
        }
    }

    public static void registerWithFB(final Context context, LoginResult loginResult) {
        GraphRequest userGraphRequest = GraphRequest.newMeRequest(loginResult.getAccessToken(), new GraphRequest.GraphJSONObjectCallback() {

            @Override
            public void onCompleted(final JSONObject object, GraphResponse response) {
                // TODO Auto-generated method stub
                Log.i("FB Graph Response", response.toString());
                if (object != null && object.length() > 0) {
                    new AsyncTask<String, Void, String>() {

                        private ProgressDialog pDialog;
                        private String appId;

                        @Override
                        protected void onPreExecute() {
                            // TODO Auto-generated method stub
                            super.onPreExecute();
                            appId = getAppId(context);

                            pDialog = new ProgressDialog(context);
                            pDialog.setMessage("Logging in progress...");
                            pDialog.setCancelable(false);
                            pDialog.show();
                        }

                        @Override
                        protected String doInBackground(String... params) {
                            // TODO Auto-generated method stub
                            try {
                                object.put("login_source", Constants.LOGIN_SOURCE);
                                object.put("appid", appId);
                            } catch (JSONException e1) {
                                // TODO Auto-generated catch block
                                e1.printStackTrace();
                            }

                            String response = null;
                            try {
                                response = sendPost(context, Constants.LOGIN_WITH_FB_API_URL, "fb_response=" + URLEncoder.encode(object.toString(), "UTF-8"));
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
                                Log.i("Login Details via Facebook", result);

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
                                            triggerGAEvent(context, "Register", "Facebook", customerId);
                                        } else {
                                            triggerGAEvent(context, "Login", "Facebook", customerId);
                                        }
                                        AppSettings.getInstance(context).setConfiguration(email, customerId, appId);
                                        KopykitabApplication.getInstance().prepareCachedAPI(context);
                                        showLibraryAndSyncData(context);
                                    } else {
                                        triggerGAEvent(context, "Login", jsonObject.getString("messageStatus"), email);
                                        final AlertDialog alertDialog = createAlertBox(context);
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
                                Constants.showToast("Error in Login. Check Internet Connection.", context);
                            }
                        }

                    }.execute();
                } else {
                    Constants.showToast("Some error in Login with Facebook", context);
                }
            }
        });
        Bundle parameters = new Bundle();
        parameters.putString("fields", "id,email,birthday,first_name,gender,last_name,link,location,locale,name,updated_time,verified");
        userGraphRequest.setParameters(parameters);
        userGraphRequest.executeAsync();
    }

    public static AlertDialog createAlertBox(Context context) {
        ContextThemeWrapper ctw = new ContextThemeWrapper(context, android.R.style.Theme_Holo_Light_Dialog);
        AlertDialog.Builder alertBox = new AlertDialog.Builder(ctw);
        alertBox.setInverseBackgroundForced(true);
        AlertDialog alertDialog = alertBox.create();
        alertDialog.setView(LayoutInflater.from(context).inflate(R.layout.dialog_box, null), 0, 0, 0, 0);

        return alertDialog;
    }

    public static AlertDialog createAlertBox(Context context, int resLayout) {
        ContextThemeWrapper ctw = new ContextThemeWrapper(context, android.R.style.Theme_Holo_Light_Dialog);
        AlertDialog.Builder alertBox = new AlertDialog.Builder(ctw);
        alertBox.setInverseBackgroundForced(true);
        AlertDialog alertDialog = alertBox.create();
        alertDialog.setView(LayoutInflater.from(context).inflate(resLayout, null), 0, 0, 0, 0);

        return alertDialog;
    }

    public static BottomSheetDialog createBottomSheetDialog(Context context, int layoutId) {
        View bottomSheetLayout = LayoutInflater.from(context).inflate(layoutId, null);
        BottomSheetDialog mBottomSheetDialog = new BottomSheetDialog(context);
        mBottomSheetDialog.setContentView(bottomSheetLayout);

        return mBottomSheetDialog;
    }

    public static void showPermissionInfoDialog(final Context context) {
        if (permissionDialog == null) {
            permissionDialog = createAlertBox(context, R.layout.permissions_view);
        }
        permissionDialog.setCancelable(false);
        permissionDialog.show();

        TextView title = permissionDialog.findViewById(R.id.permission_title);
        TextView subTitle = permissionDialog.findViewById(R.id.permission_sub_title);
        ListView permissionView = permissionDialog.findViewById(R.id.permission_list_view);
        Button settingButton = permissionDialog.findViewById(R.id.permission_setting_button);

        title.setText(context.getResources().getString(R.string.permission_text));
        title.setMaxLines(3);
        subTitle.setText(Html.fromHtml(context.getResources().getString(R.string.permission_sub_text)), TextView.BufferType.SPANNABLE);
        subTitle.setMaxLines(3);

        permissionView.setAdapter(new PermissionAdapter(context, context.getResources().getStringArray(R.array.permission_text_array), Constants.PERMISSION_IMAGES));

        settingButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                Uri uri = Uri.fromParts("package", context.getPackageName(), null);
                intent.setData(uri);
                context.startActivity(intent);
            }
        });
    }

    public static boolean isPermissionDialogOpen() {
        if (permissionDialog != null) {
            return permissionDialog.isShowing();
        }

        return false;
    }

    public static void closePermissionDialog() {
        if (permissionDialog != null) {
            permissionDialog.dismiss();
        }
    }

    public static void logout(final Context context) {
        final String customerId = AppSettings.getInstance(context).get("CUSTOMER_ID");

        triggerGAEvent(context, "LOGOUT", customerId, "");
        if (hasPermissions(context, Constants.STORAGE_PERMISSION)) {
            triggerGADataReceiver(context, customerId, true);
            DatabaseHelper.resetDatabaseInstance();
        }

        AppSettings.getInstance(context).resetConfiguration();
        showLoginMainActivity(context);

        if (isNetworkConnected(context)) {
            new AsyncTask<Void, Void, String>() {

                @Override
                protected String doInBackground(Void... params) {
                    // TODO Auto-generated method stub
                    String response = null;

                    try {
                        response = sendPost(context, Constants.LOGOUT_URL, "customer_id=" + URLEncoder.encode(customerId, "UTF-8") + "&appid=" + URLEncoder.encode(AppSettings.getInstance(context).get("APP_ID"), "UTF-8") + "&login_source=" + URLEncoder.encode(Constants.LOGIN_SOURCE, "UTF-8"));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    return response;
                }

                @Override
                protected void onPostExecute(String result) {
                    // TODO Auto-generated method stub
                    super.onPostExecute(result);

                    Log.i("Logout Result", result);
                }

            }.execute();
        }
    }

    public static void triggerGAEvent(Context context, String category, String action, String label) {
        triggerGAEvent(context, category, action, label, 0);
    }

    public static void triggerGAEvent(Context context, String category, String action, String label, long value) {
        Log.e("GA Trigger Data", category + "-" + action + "-" + label + "-" + value);

        //save offline GA Trigger data to JSON file
        List<HashMap<String, String>> gATriggeredData = new ArrayList<HashMap<String, String>>();
        try {
            File jsonFile = new File(getDirectory(context) + Constants.GA_TRIGGER_OFFLINE_DATA_JSON_FILENAME);
            if (jsonFile.exists()) {
                gATriggeredData = new Gson().fromJson(new FileReader(jsonFile), new TypeToken<List<HashMap<String, String>>>() {
                }.getType());
            }

            if (gATriggeredData == null) {
                gATriggeredData = new ArrayList<HashMap<String, String>>();
            }

            HashMap<String, String> eventData = new HashMap<String, String>();
            eventData.put("category", category);
            eventData.put("action", action);
            eventData.put("label", label);
            eventData.put("value", Long.toString(value));
            eventData.put("date_added", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Calendar.getInstance().getTime()));
            eventData.put("source", Constants.LOGIN_SOURCE);
            eventData.put("latitude", "");
            eventData.put("longitude", "");

            if (hasPermissions(context, Constants.LOCATION_PERMISSION)) {
                LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
                List<String> providers = locationManager.getProviders(true);
                for (String provider : providers) {
                    Location location = locationManager.getLastKnownLocation(provider);
                    if (location != null) {
                        eventData.put("latitude", "" + location.getLatitude());
                        eventData.put("longitude", "" + location.getLongitude());
                        break;
                    }
                }
            }

            //add triggered data
            gATriggeredData.add(eventData);
            File outerJsonFile = new File(getDirectory(context) + "../" + Constants.GA_TRIGGER_OFFLINE_DATA_JSON_FILENAME);
            if (outerJsonFile.exists()) {
                List<HashMap<String, String>> outerGATriggeredData = new Gson().fromJson(new FileReader(outerJsonFile), new TypeToken<List<HashMap<String, String>>>() {
                }.getType());
                if (outerGATriggeredData != null && outerGATriggeredData.size() > 0) {
                    gATriggeredData.addAll(outerGATriggeredData);
                }

                outerJsonFile.delete();
            }

            try {
                // write data to JSON file
                FileWriter newJsonFile = new FileWriter(new File(getDirectory(context) + Constants.GA_TRIGGER_OFFLINE_DATA_JSON_FILENAME));
                newJsonFile.write(new Gson().toJson(gATriggeredData));
                newJsonFile.flush();
                newJsonFile.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void triggerGAEventOnline(Context context, String category, String action, String label) {
        triggerGAEventOnline(context, category, action, label, 0);
    }

    public static void triggerGAEventOnline(Context context, String category, String action, String label, long value) {
        if (isNetworkConnected(context)) {
            Tracker gaTracker = GoogleAnalytics.getInstance(context).newTracker(Constants.GA_TRACKING_ID);
            Log.i("GA Trigger", "GA Trigger for data : " + category + "-" + action + "-" + label + "-" + value);
            gaTracker.send(new HitBuilders.EventBuilder()
                    .setCategory(category)
                    .setAction(action)
                    .setLabel(label)
                    .setValue(value)
                    .build());
        } else {
            Log.e("GA Trigger Error", "Internet connection not available. Given data : " + category + "-" + action + "-" + label + "-" + value);
        }
    }

    public static void triggerScreen(Context context, String screenName) {
        Log.i("GA Trigger Screen", "GA Trigger Screen : " + screenName);

        if (isNetworkConnected(context)) {
            Tracker gaTracker = getTracker(context);
            gaTracker.setScreenName(screenName);
            gaTracker.send(new HitBuilders.ScreenViewBuilder().build());
        }
    }

    public static void triggerSocialInteractions(Context context, String network, String action, String target) {
        Log.i("GA Trigger Social Interactions", "GA Trigger Social Interactions : " + network + "-" + action + "-" + target);

        if (isNetworkConnected(context)) {
            Tracker gaTracker = getTracker(context);
            gaTracker.send(new HitBuilders.SocialBuilder()
                    .setNetwork(network)
                    .setAction(action)
                    .setTarget(target)
                    .build());
        }
    }

    public static void triggerGADataReceiver(Context context, String customerId, boolean sendBroadCast) {
        Intent broadCastIntent = new Intent("com.kopykitab.ereader.components");
        broadCastIntent.setClass(context, NetworkChangeReceiver.class);
        broadCastIntent.putExtra("customer_id", customerId);

        long timeDuration = 1000 * 60 * 60 * 12;
        if (sendBroadCast) {
            context.sendBroadcast(broadCastIntent);
        } else {
            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, broadCastIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            alarmManager.setRepeating(AlarmManager.RTC, System.currentTimeMillis(), timeDuration, pendingIntent);
        }
    }

    public static String sendGet(String targetUrl, String urlParameters) {
        if (urlParameters != null && urlParameters.length() > 0) {
            if (targetUrl.indexOf("?") > 0) {
                targetUrl += "&" + urlParameters;
            } else {
                targetUrl += "?" + urlParameters;
            }
        }

        HttpURLConnection connection = null;
        try {
            // Create connection
            URL url = new URL(targetUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-Agent", System.getProperty("http.agent", ""));

            // Get Response
            InputStream is = connection.getInputStream();
            BufferedReader rd = new BufferedReader(new InputStreamReader(is));
            String line;
            StringBuffer response = new StringBuffer();
            while ((line = rd.readLine()) != null) {
                response.append(line);
            }
            rd.close();
            return response.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    public static String sendGet(Context mContext, String targetUrl, String urlParameters) {
        if (urlParameters != null && urlParameters.length() > 0) {
            if (targetUrl.indexOf("?") > 0) {
                targetUrl += "&" + urlParameters;
            } else {
                targetUrl += "?" + urlParameters;
            }
        }

        HttpURLConnection connection = null;
        try {
            // Create connection
            URL url = new URL(targetUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            try {
                PackageInfo packageInfo = mContext.getPackageManager().getPackageInfo(mContext.getPackageName(), 0);
                connection.setRequestProperty("User-Agent", System.getProperty("http.agent") + " [" + packageInfo.packageName + "/" + packageInfo.versionName + "]");
            } catch (Exception e) {
                e.printStackTrace();
                connection.setRequestProperty("User-Agent", System.getProperty("http.agent") + " [" + mContext.getPackageName() + "]");
            }

            // Get Response
            InputStream is = connection.getInputStream();
            BufferedReader rd = new BufferedReader(new InputStreamReader(is));
            String line;
            StringBuffer response = new StringBuffer();
            while ((line = rd.readLine()) != null) {
                response.append(line);
            }
            rd.close();
            return response.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    public static String sendPost(String targetUrl, String urlParameters) {
        HttpURLConnection connection = null;
        try {
            // Create connection
            URL url = new URL(targetUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            connection.setRequestProperty("Content-Length", "" + Integer.toString(urlParameters.getBytes().length));
            connection.setRequestProperty("Content-Language", "en-US");
            connection.setRequestProperty("User-Agent", System.getProperty("http.agent", ""));

            connection.setUseCaches(false);
            connection.setDoInput(true);
            connection.setDoOutput(true);

            // Send request
            DataOutputStream wr = new DataOutputStream(
                    connection.getOutputStream());
            wr.writeBytes(urlParameters);
            wr.flush();
            wr.close();

            // Get Response
            InputStream is = connection.getInputStream();
            BufferedReader rd = new BufferedReader(new InputStreamReader(is));
            String line;
            StringBuffer response = new StringBuffer();
            while ((line = rd.readLine()) != null) {
                response.append(line);
            }
            rd.close();
            return response.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    public static String sendPost(Context mContext, String targetUrl, String urlParameters) {
        HttpURLConnection connection = null;
        try {
            // Create connection
            URL url = new URL(targetUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            connection.setRequestProperty("Content-Length", "" + Integer.toString(urlParameters.getBytes().length));
            connection.setRequestProperty("Content-Language", "en-US");
            try {
                PackageInfo packageInfo = mContext.getPackageManager().getPackageInfo(mContext.getPackageName(), 0);
                connection.setRequestProperty("User-Agent", System.getProperty("http.agent") + " [" + packageInfo.packageName + "/" + packageInfo.versionName + "]");
            } catch (Exception e) {
                e.printStackTrace();
                connection.setRequestProperty("User-Agent", System.getProperty("http.agent") + " [" + mContext.getPackageName() + "]");
            }
            connection.setUseCaches(false);
            connection.setDoInput(true);
            connection.setDoOutput(true);

            // Send request
            DataOutputStream wr = new DataOutputStream(
                    connection.getOutputStream());
            wr.writeBytes(urlParameters);
            wr.flush();
            wr.close();

            // Get Response
            InputStream is = connection.getInputStream();
            BufferedReader rd = new BufferedReader(new InputStreamReader(is));
            String line;
            StringBuffer response = new StringBuffer();
            while ((line = rd.readLine()) != null) {
                response.append(line);
            }
            rd.close();
            return response.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    public static List<String> getTabs(Context mContext, String productTypes) {
        List<String> bookTabList = new ArrayList<String>();
        try {
            String jsonFileName = productTypes.replaceAll("(\\s|,)+", "_") + "_by_categories.json";
            File jsonFile = new File(getDirectory(mContext) + jsonFileName);
            if (jsonFile.exists()) {
                FileInputStream jsonFileInputStream = new FileInputStream(jsonFile);
                byte[] fileRead = new byte[jsonFileInputStream.available()];
                jsonFileInputStream.read(fileRead);
                jsonFileInputStream.close();
                JSONObject productTypeJsonObject = new JSONObject(new String(Base64.decode(fileRead, Base64.DEFAULT)));

                JSONArray categoriesJsonArray = productTypeJsonObject.getJSONArray("products");
                if (categoriesJsonArray.length() > 0) {
                    for (int i = 0; i < categoriesJsonArray.length(); i++) {
                        JSONObject categoryJsonObject = categoriesJsonArray.getJSONObject(i);
                        bookTabList.add(categoryJsonObject.getString("name"));
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return bookTabList;
    }

    public static void setSearchTextForSearchInStore(String keyword) {
        searchInStoreKeyword = keyword;
    }

    public static String getSearchInStoreKeyword() {
        return searchInStoreKeyword;
    }

    public static boolean checkPermission(Context mContext, String permission) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int result = ContextCompat.checkSelfPermission(mContext, permission);

            //If the app does permission, then return true
            if (result == PackageManager.PERMISSION_GRANTED) {
                return true;
            } else {
                return false;
            }
        }
        return true;
    }

    public static boolean hasPermissions(Context context, String[] permissions) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    public static void setMenuItems(LinkedList<HashMap<String, String>> items) {
        menuItems = items;
    }

    public static LinkedList<HashMap<String, String>> getMenuItems() {
        return menuItems;
    }

    public static void setLibraryView(String view) {
        libraryView = view;
    }

    public static String getLibraryView() {
        return libraryView;
    }
}
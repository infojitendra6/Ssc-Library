package com.kopykitab.ereader.settings;

import android.Manifest;
import android.content.Context;
import android.view.View;
import android.widget.Toast;

import com.kopykitab.ereader.R;

public final class Constants {

    // URLs
    public static final String BASE_URL = "https://www.kopykitab.com/";
    public static final String INDEX_URL = BASE_URL + "index.php";
    public static String IMAGE_URL = BASE_URL + "image/cache/";
    public static final String ICON_URL = BASE_URL + "image/cache/icons/";


//    // test url
//    public static final String BASE_URL = "https://fresheropenings.com/";
//    public static final String INDEX_URL = BASE_URL + "index.php";
//    public static String IMAGE_URL = BASE_URL + "image/cache/";
//    public static final String ICON_URL = BASE_URL + "image/cache/icons/";

    //API URLs
    public static final String LOGIN_API_URL = INDEX_URL + "?route=account/applogin/";
    public static final String FORGOT_PASSWORD_API_URL = LOGIN_API_URL + "forgotten";
    public static final String REGISTER_API_URL = LOGIN_API_URL + "register";
    public static final String NOTIFICATIONS_API_URL = LOGIN_API_URL + "getNotificationsByNotificationType";
    public static final String SYNCDATA_API_URL = LOGIN_API_URL + "syncAllDataByCategories";

// test

//    public static final String SYNCDATA_API_URL = "https://fresheropenings.com/";


    public static final String FEEDBACK_API_URL = LOGIN_API_URL + "feedback";


    public static final String CATEGORIES_URL = LOGIN_API_URL + "getCategoriesList";
    public static final String RECOMMENDATIONS_URL = LOGIN_API_URL + "recommendations";
    public static final String LOGOUT_URL = LOGIN_API_URL + "logout";
    public static final String STREAMS_URL = LOGIN_API_URL + "getStreams";
    public static final String ADD_UPDATE_STREAMS_URL = LOGIN_API_URL + "addStreams";
    public static final String GA_TRIGGER_OFFLINE_URL = LOGIN_API_URL + "offlineData";
    public static final String BANNERS_URL = LOGIN_API_URL + "banners";
    public static final String CART_DETAILS_URL = LOGIN_API_URL + "getCartDetails";
    public static final String PREMIUM_URL = LOGIN_API_URL + "premiumList";
    public static final String ANNOTATION_PRINT_URL = LOGIN_API_URL + "annotationsMail";


    //Site URLs
    public static final String LOGIN_WITH_FB_API_URL = INDEX_URL + "?route=account/register/registerWithFB";
    public static final String LOGIN_WITH_GOOGLE_API_URL = INDEX_URL + "?route=account/register/registerWithGoogle";
    public static final String LIVE_SEARCH_URL = INDEX_URL + "?route=product/live_search";
    public static final String CART_URL = INDEX_URL + "?route=checkout/checkout";
    public static final String SCORECARD_URL = INDEX_URL + "?route=account/dashboard/study";
    public static final String WISHLIST_URL = INDEX_URL + "?route=account/wishlist";
    public static final String WHATSNEW_URL = INDEX_URL + "?route=information/whatsnew";
    public static final String REFER_N_EARN_URL = INDEX_URL + "?route=account/referral";
    public static final String ACCOUNT_SETTINGS_URL = INDEX_URL + "?route=account/settings";
    public static final String ACCOUNT_PROFILE_URL = INDEX_URL + "?route=account/account";
    public static final String PUBLISHERS_URL = INDEX_URL + "?route=product/manufacturer";


    public static final String DISHA_PUBLISHER_URL = BASE_URL + "Disha-Publication-eBooks";
    public static final String OSWAAL_BOOKS_URL = BASE_URL + "Oswaal-Books";
    public static final String LAXMI_PUBLISHER_URL = BASE_URL + "laxmi-publications-ebooks";
    public static final String SCHAND_PUBLISHER_URL = BASE_URL + "eBooks-for-schand-publishing";
    public static final String PHI_PUBLISHER_URL = BASE_URL + "PHI-Learning-Books";
    public static final String SHUCHITA_PUBLISHER_URL = BASE_URL + "Shuchita-Prakashan-solved-scanners-cs-ca-books-ebooks-publishers";


    public static final String SEARCH_IN_STORE_URL = INDEX_URL + "?route=product/search";
	public static final String PRODUCT_ADD_TO_CART = INDEX_URL + "?route=bossthemes/cart/add";


    // Paths
     public static final String SYNCDATA_JSON_FILENAME = "sync_all_data_by_categories.json";
    public static final String GA_TRIGGER_OFFLINE_DATA_JSON_FILENAME = "data.json";
    public static final String ANNOT_JSON_FILENAME = "annots_n.json";
    public static final String OLD_ANNOT_JSON_FILENAME = "annots.json";
    public static final String BOOKMARKED_PAGE_JSON_FILENAME = "annots_bookmarks.json";
    public static final String ANNOT_ADD_NOTES_JSON_FILENAME = "annots_notes.json";
    public static final String PRINT_ANNOTATION_FILE_NAME = "print_annotation.html";
    public static final String LOGIN_SOURCE = "android_app";

    public static final int FETCH_SEARCH_TEXT = 20;

    //GA related details
    public static final String GA_TRACKING_ID = "UA-30674928-10";

    //GCM related details
    public static final String STORE_GCM_DETAILS_URL = LOGIN_API_URL + "addGCMRegistration";

    //Permission required
    public static final String[] STORAGE_PERMISSION = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };
    public static final String PHONE_PERMISSION = Manifest.permission.READ_PHONE_STATE;
    public static final String[] LOCATION_PERMISSION = {
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
    };

    public static final String[] REQUIRED_PERMISSIONS = {
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    //Permission Images
    public static final int[] PERMISSION_IMAGES = {R.drawable.ic_storage, R.drawable.ic_phone, R.drawable.ic_contact, R.drawable.ic_location};

    //New Feature Images
    public static final int[] NEW_FEATURE_IMAGES = {R.drawable.ic_edit_color, R.drawable.ic_highlight_color, R.drawable.ic_bookmark_color, R.drawable.ic_print_color};

    // common function for showing of toast
    static Toast toastActivity = null;

    public static void showToast(String text, Context mContext) {
        if (toastActivity != null) {
            View v = toastActivity.getView();
            if (v.isShown()) {
                toastActivity.setText(text);
                return;
            }
            toastActivity.cancel();
            toastActivity = null;
        }
        toastActivity = Toast.makeText(mContext, text, Toast.LENGTH_LONG);
        toastActivity.show();
    }

    public static void cancelToast() {
        if (toastActivity != null) {
            toastActivity.cancel();
            toastActivity = null;
        }
    }

    public static void setImageBaseUrl(String imageBaseUrl) {
        IMAGE_URL = imageBaseUrl;
    }

    static String activateBottomNavigationWidgetButton = "";

    public static void setActivateBottomNavigationWidgetButton(String activateButton) {
        activateBottomNavigationWidgetButton = activateButton;
    }

    public static String getActivateBottomNavigationWidgetButton() {
        return activateBottomNavigationWidgetButton;
    }

}
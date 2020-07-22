package com.kopykitab.ereader.settings;

import android.content.Context;
import android.content.SharedPreferences;

public class AppSettings {

    private static SharedPreferences credentials = null;
    private static AppSettings instance = null;

    private AppSettings() { // So that this class cannot be initialised outside
    }

    public static AppSettings getInstance(Context context) {
        if (instance == null) {
            instance = new AppSettings();
        }
        if (credentials == null) {
            credentials = context.getSharedPreferences("first_install", 0);
        }

        return instance;
    }

    public void setConfiguration(String email, String customerId, String appId) {
        if (credentials.getInt("first_install", 0) == 0) {
            set("EMAIL", email);
            set("APP_ID", appId);
            set("CUSTOMER_ID", customerId);
            set("GCM_REG_ID", "");
            set("recommendation_shown_time", "");
            set("offers", "[]");

            SharedPreferences.Editor credentialsEditor = credentials.edit();
            credentialsEditor.putInt("first_install", 1);
            credentialsEditor.commit();
        }
    }

    public boolean isConfigurationDone() {
        boolean result = (credentials.getInt("first_install", 0) == 1) ? true : false;
        return result;
    }

    public void set(String key, String valueToSet) {
        SharedPreferences.Editor editor = credentials.edit();

        try {
            editor.putString(key, valueToSet);
            editor.commit();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String get(String key) {
        return credentials.getString(key, "");
    }

    public void resetConfiguration() {
        SharedPreferences.Editor credentialsEditor = credentials.edit();
        credentialsEditor.putInt("first_install", 0);
        credentialsEditor.commit();

        set("EMAIL", "");
        set("CUSTOMER_ID", "");
        set("STREAMS_CONFIGURED", "0");
        set("GCM_REG_ID", "");
        set("recommendation_shown_time", "");
        set("offers", "[]");
        set("MUPDF_NEW_FEATURE", "0");
    }
}
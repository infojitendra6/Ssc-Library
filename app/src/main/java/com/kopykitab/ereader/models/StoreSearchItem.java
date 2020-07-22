package com.kopykitab.ereader.models;

public class StoreSearchItem {
    private String title;
    private String overviewURL;
    private String objectCount;
    private static String searchResults;
    private static String searchUrl;

    public StoreSearchItem(String title, String overviewURL) {
        this.title = title;
        this.overviewURL = overviewURL;
    }

    public StoreSearchItem(String title, String overviewURL, String objectCount) {
        this.title = title;
        this.overviewURL = overviewURL;
        this.objectCount = objectCount;
    }

    public String getTitle() {
        return title;
    }

    public String getOverviewURL() {
        return overviewURL;
    }

    public String getObjectCount() {
        return objectCount;
    }

    public static String getSearchResults() {
        return searchResults;
    }

    public static void setSearchResults(String searchResults) {
        StoreSearchItem.searchResults = searchResults;
    }

    public static String getSearchUrl() {
        return searchUrl;
    }

    public static void setSearchUrl(String searchUrl) {
        StoreSearchItem.searchUrl = searchUrl;
    }
}

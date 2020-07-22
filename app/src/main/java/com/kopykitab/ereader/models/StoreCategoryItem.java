package com.kopykitab.ereader.models;

public class StoreCategoryItem {

    private final String name;
    private final int id;
    private final String url;

    public StoreCategoryItem(String name, int id, String url) {
        this.name = name;
        this.id = id;
        this.url = url;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getUrl() {
        return url;
    }
}

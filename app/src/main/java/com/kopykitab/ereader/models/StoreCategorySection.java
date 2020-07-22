package com.kopykitab.ereader.models;

public class StoreCategorySection {

    private final String name;
    private final String url;
    public boolean isExpanded = false;

    public StoreCategorySection(String name, String url) {
        this.name = name;
        this.url = url;
    }

    public String getName() {
        return name;
    }

    public String getUrl() {
        return url;
    }

    public void setIsExpanded(boolean isExpanded) {
        this.isExpanded = isExpanded;
    }
}

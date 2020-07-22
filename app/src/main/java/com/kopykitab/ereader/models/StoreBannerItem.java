package com.kopykitab.ereader.models;


public class StoreBannerItem {
    private final String image;
    private final String description;
    private final String href;

    public StoreBannerItem(String image, String description, String href) {
        this.image = image;
        this.description = description;
        this.href = href;
    }

    public String getImage() {
        return image;
    }

    public String getDescription() {
        return description;
    }

    public String getHref() {
        return href;
    }
}

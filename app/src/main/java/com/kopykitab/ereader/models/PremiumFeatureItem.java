package com.kopykitab.ereader.models;

public class PremiumFeatureItem {
    private Integer image;
    private String title;
    private String description;

    public PremiumFeatureItem(Integer image, String title, String description) {
        this.image = image;
        this.title = title;
        this.description = description;
    }

    public Integer getImage() {
        return image;
    }

    public void setImage(Integer image) {
        this.image = image;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}

package com.kopykitab.ereader.models;

public class PremiumItem {
    private String imageURL;
    private String price_1;
    private String price_2;

    public PremiumItem(String imageURL, String price_1, String price_2) {
        this.imageURL = imageURL;
        this.price_1 = price_1;
        this.price_2 = price_2;
    }

    public String getImageURL() {
        return imageURL;
    }

    public String getPrice_1() {
        return price_1;
    }

    public String getPrice_2() {
        return price_2;
    }
}

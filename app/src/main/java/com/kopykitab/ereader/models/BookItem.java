package com.kopykitab.ereader.models;

public class BookItem {
    private String name;
    private String orderProductId;
    private String cidd;
    private String imageURL;
    private String description;
    private String price;
    private String leftDays;
    private String productId;
    private String productType;
    private String pdfDownloadedDate;
    private String licensePeriod;
    private String productLink;
    private String optionName;
    private String optionValue;
    private String dateAdded;

    public BookItem(String name, String orderProductId, String cidd, String imageURL, String description, String price, String leftDays, String productId, String productType, String optionName, String optionValue, String pdfDownloadedDate, String licensePeriod, String productLink, String dateAdded) {
        this.name = name;
        this.orderProductId = orderProductId;
        this.cidd = cidd;
        this.imageURL = imageURL;
        this.description = description;
        this.price = price;
        this.leftDays = leftDays;
        this.productId = productId;
        this.productType = productType;
        this.optionName = optionName;
        this.optionValue = optionValue;
        this.pdfDownloadedDate = pdfDownloadedDate;
        this.licensePeriod = licensePeriod;
        this.productLink = productLink;
        this.dateAdded = dateAdded;
    }

    public String getName() {
        return name;
    }

    public String getOrderProductId() {
        return orderProductId;
    }

    public String getCidd() {
        return cidd;
    }

    public String getImageURL() {
        return imageURL;
    }

    public String getDescription() {
        return description;
    }

    public String getPrice() {
        return price;
    }

    public String getLeftDays() {
        return leftDays;
    }

    public String getProductId() {
        return productId;
    }

    public String getProductType() {
        return productType;
    }

    public String getOptionName() {
        return optionName;
    }

    public String getOptionValue() {
        return optionValue;
    }

    public String getPdfDownloadedDate() {
        return pdfDownloadedDate;
    }

    public String getLicensePeriod() {
        return licensePeriod;
    }

    public String getProductLink() {
        return productLink;
    }

    public String getDateAdded() {
        return dateAdded;
    }
}
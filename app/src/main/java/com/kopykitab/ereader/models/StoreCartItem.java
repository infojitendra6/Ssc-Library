package com.kopykitab.ereader.models;

public class StoreCartItem {
    private String productId, image, name, optionDetail, stockStatusId, price_1, price_2, href;

    public StoreCartItem() {
    }

    public StoreCartItem(String productId, String image, String name, String optionDetail, String stockStatusId, String price_1, String price_2, String href) {
        this.productId = productId;
        this.image = image;
        this.name = name;
        this.optionDetail = optionDetail;
        this.stockStatusId = stockStatusId;
        this.price_1 = price_1;
        this.price_2 = price_2;
        this.href = href;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public String getImageUrl() {
        return image;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getOptionDetail() {
        return optionDetail;
    }

    public String getStockStatusId() {
        return stockStatusId;
    }

    public String getPrice_1() {
        return price_1;
    }

    public String getPrice_2() {
        return price_2;
    }

    public String getHref() {
        return href;
    }

}

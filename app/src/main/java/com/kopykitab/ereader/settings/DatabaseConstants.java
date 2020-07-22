package com.kopykitab.ereader.settings;

import android.provider.BaseColumns;

public final class DatabaseConstants {
    // To prevent someone from accidentally instantiating the DatabaseConstants class, make the constructor private.
    private DatabaseConstants() {
    }

    /* Inner classes that defines the database & table contents */
    public static class DatabaseEntry {
        public static final String DATABSE_NAME = "kitabstore.db";
        public static final String DOWNLOADED_BOOKS_TABLE_NAME = "downloaded_books";
    }

    public static class SearchSuggestionsEntry implements BaseColumns {
        public static final String TABLE_NAME = "search_suggestions";
        public static final String COLUMN_ID = "id";
        public static final String COLUMN_KEYWORD = "keyword";
        public static final String COLUMN_RESULTS = "results";
        public static final String COLUMN_DATE = "date";
    }

    public static class StoreCategoryEntry implements BaseColumns {
        public static final String TABLE_NAME = "store_category";
        public static final String COLUMN_ID = "id";
        public static final String COLUMN_NAME = "name";
        public static final String COLUMN_PARENT_NAME = "parent_name";
        public static final String COLUMN_URL = "url";
        public static final String COLUMN_LEVEL = "level";
    }

    public static class RecommendationsEntry implements BaseColumns {
        public static final String TABLE_NAME = "recommendations";
        public static final String COLUMN_PRODUCT_ID = "product_id";
        public static final String COLUMN_IMAGE = "image";
        public static final String COLUMN_NAME = "name";
        public static final String COLUMN_DESCRIPTION = "description";
        public static final String COLUMN_RENTAL_PERIOD = "rental_period";
        public static final String COLUMN_PRICE_1 = "price_1";
        public static final String COLUMN_PRICE_2 = "price_2";
        public static final String COLUMN_FREE_PRODUCT = "free_product";
        public static final String COLUMN_HREF = "href";
        public static final String COLUMN_PRODUCT_TYPE = "product_type";
    }

    public static class StoreBannersEntry implements BaseColumns {
        public static final String TABLE_NAME = "store_banners";
        public static final String COLUMN_ID = "id";
        public static final String COLUMN_IMAGE_URL = "image_url";
        public static final String COLUMN_DESCRIPTION = "description";
        public static final String COLUMN_HREF = "href";
    }

    public static class BookEntry implements BaseColumns {
        public static final String COLUMN_ID = "id";
        public static final String COLUMN_CUSTOMER_ID = "customer_id";
        public static final String COLUMN_PRODUCT_ID = "product_id";
        public static final String COLUMN_ORDER_PRODUCT_ID = "order_product_id";
        public static final String COLUMN_CIDD = "cidd";
        public static final String COLUMN_NAME = "name";
        public static final String COLUMN_IMAGE_URL = "image_url";
        public static final String COLUMN_DESCRIPTION = "description";
        public static final String COLUMN_PRICE = "price";
        public static final String COLUMN_LEFT_DAYS = "left_days";
        public static final String COLUMN_PRODUCT_TYPE = "product_type";
        public static final String COLUMN_PDF_DOWNLOADED_DATE = "pdf_downloaded_date";
        public static final String COLUMN_LICENCE_PERIOD = "licence_period";
        public static final String COLUMN_PRODUCT_LINK = "product_link";
    }
}
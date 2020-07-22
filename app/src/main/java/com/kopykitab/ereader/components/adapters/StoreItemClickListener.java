package com.kopykitab.ereader.components.adapters;

import com.kopykitab.ereader.models.StoreBannerItem;
import com.kopykitab.ereader.models.StoreCartItem;
import com.kopykitab.ereader.models.StoreCategoryItem;
import com.kopykitab.ereader.models.StoreCategorySection;
import com.kopykitab.ereader.models.StoreRecommendationsItem;
import com.kopykitab.ereader.models.StoreSearchItem;

public interface StoreItemClickListener {
    void itemClicked(StoreSearchItem search);

    void itemClicked(StoreCartItem search);

    void itemClicked(StoreBannerItem banner);

    void itemClicked(StoreRecommendationsItem recommendation);

    void itemClicked(StoreCategorySection section);

    void itemClicked(StoreCategoryItem item);
}

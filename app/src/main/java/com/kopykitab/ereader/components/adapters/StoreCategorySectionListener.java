package com.kopykitab.ereader.components.adapters;

import com.kopykitab.ereader.models.StoreCategorySection;

/**
 * interface to listen changes in state of sections
 */
public interface StoreCategorySectionListener {
    void onSectionStateChanged(StoreCategorySection section, boolean isOpen);
}
package com.weinmann.ccr.core;

import com.weinmann.ccr.services.ContentService;

public interface ContentServiceListener {
    void onContentServiceChanged(ContentService service);
}

package com.weinmann.ccr.ui;

import java.util.List;

import android.app.Activity;
import android.os.Bundle;

import com.weinmann.ccr.core.CarCastResurrectedApplication;
import com.weinmann.ccr.core.ContentServiceListener;
import com.weinmann.ccr.core.Subscription;
import com.weinmann.ccr.services.ContentService;


public abstract class BaseActivity extends Activity implements ContentServiceListener {
	protected ContentService contentService;

	public ContentService getContentService() {
		return contentService;
	}

	protected List<Subscription> getSubscriptions() {
		return contentService.getSubscriptions();
	}

	protected void onContentService() { // TODO rename
	    // does nothing by default
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	public void onContentServiceChanged(ContentService service) {
	    contentService = service;
	    if (service != null) {
            onContentService();
        }
	}

	@Override
	protected void onResume() {
	    super.onResume();
	    getCarCastResurrectedApplication().setContentServiceListener(this);
	}

    protected CarCastResurrectedApplication getCarCastResurrectedApplication() {
        return ((CarCastResurrectedApplication)getApplication());
    }
}

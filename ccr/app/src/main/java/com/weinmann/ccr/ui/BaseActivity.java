package com.weinmann.ccr.ui;

import java.util.List;

import android.app.Activity;

import com.weinmann.ccr.core.CarCastResurrectedApplication;
import com.weinmann.ccr.core.Config;
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

	protected void onPostContentServiceChanged() {
		// default is no-op
	}

	@Override
	public void onContentServiceChanged(ContentService service) {
	    contentService = service;
	    if (service != null) {
            onPostContentServiceChanged();
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

	protected Config getConfig() {
		return ((CarCastResurrectedApplication)getApplicationContext()).getConfig();
	}
}

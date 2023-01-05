package com.weinmann.ccr.ui;

import java.util.List;

import android.app.Activity;

import com.weinmann.ccr.core.CarCastResurrectedApplication;
import com.weinmann.ccr.core.ContentServiceListener;
import com.weinmann.ccr.core.Subscription;
import com.weinmann.ccr.services.ContentService;
import com.weinmann.ccr.services.PlayStatusListener;


public abstract class BaseActivity extends Activity implements ContentServiceListener, PlayStatusListener {
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
		if (contentService != null) {
			contentService.setPlayStatusListener(null);
		}

	    contentService = service;
	    if (service != null) {
	    	service.setPlayStatusListener(this);
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

	@Override
	public void playStateUpdated(boolean playing) {
		// default implementation does nothing
	}
}

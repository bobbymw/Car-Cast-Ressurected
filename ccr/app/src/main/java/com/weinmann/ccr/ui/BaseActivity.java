package com.weinmann.ccr.ui;

import java.util.List;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.weinmann.ccr.core.CarCastResurrectedApplication;
import com.weinmann.ccr.core.Config;
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

	protected void onContentService() { // TODO rename
	    // does nothing by default
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	public void onContentServiceChanged(ContentService service) {
		if (contentService != null) {
			contentService.setPlayStatusListener(null);
		}
	    contentService = service;
	    if (service != null) {
	    	service.setPlayStatusListener(this);
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

	@Override
	public void playStateUpdated(boolean playing) {
		// default implementation does nothing
	}
}

package com.weinmann.ccr.ui;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.TextView;

import com.weinmann.ccr.R;
import com.weinmann.ccr.core.ExternalMediaStatus;
import com.weinmann.ccr.core.OrderingPreference;
import com.weinmann.ccr.core.Subscription;
import com.weinmann.ccr.core.Util;
import com.weinmann.ccr.services.DownloadHistory;
import com.weinmann.ccr.services.EnclosureHandler;
import com.weinmann.ccr.services.FileSubscriptionHelper;

public class SubscriptionEdit extends BaseActivity implements Runnable {

	private Subscription currentSub;
	private ProgressDialog dialog;

	private void setCurrentSubValues() {
		if (currentSub != null) {
			((TextView) findViewById(R.id.editsite_name)).setText(currentSub.name);
			((TextView) findViewById(R.id.editsite_url)).setText(currentSub.url);
			((CheckBox) findViewById(R.id.enabled)).setChecked(currentSub.enabled);
			((CheckBox) findViewById(R.id.priority)).setChecked(currentSub.priority);
			((CheckBox) findViewById(R.id.fifoLifo)).setChecked(currentSub.orderingPreference == OrderingPreference.FIFO);
			Spinner spinner = findViewById(R.id.subMax);
			int max = currentSub.maxDownloads;
			for (int i = 0; i < mValues.length; i++) {
				if (max == mValues[i])
					spinner.setSelection(i);
			}

		} // end if
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.edit_subscription);

		currentSub = null;
		FileSubscriptionHelper subscriptionHelper = new FileSubscriptionHelper(getConfig());

		findViewById(R.id.saveEditSite).setOnClickListener(v -> {
			String name = ((TextView) findViewById(R.id.editsite_name)).getText().toString();
			String url = getURL();
			boolean enabled = ((CheckBox) findViewById(R.id.enabled)).isChecked();
			boolean priority = ((CheckBox) findViewById(R.id.priority)).isChecked();
			CheckBox newestFirst = findViewById(R.id.fifoLifo);
			Spinner spinner = findViewById(R.id.subMax);
			int max = mValues[spinner.getSelectedItemPosition()];
			OrderingPreference orderingPreference = OrderingPreference.FIFO;
			if (!newestFirst.isChecked()) {
				orderingPreference = OrderingPreference.LIFO;
			}

			// try out the url:
			if (!Util.isValidURL(url)) {
				Util.toast(SubscriptionEdit.this, "URL to site is malformed.");
				return;
			} // endif

			ExternalMediaStatus status = ExternalMediaStatus.getExternalMediaStatus();
			if (status != ExternalMediaStatus.writeable) {
				// unable to access sdcard
				Util.toast(this, "Unable to add subscription to sdcard");
				return;
			}

			Subscription newSub = new Subscription(name, url, max, orderingPreference, enabled, priority);
			if (currentSub != null) {
				// edit:
				subscriptionHelper.editSubscription(currentSub, newSub);

			} else {
				// add:
				subscriptionHelper.addSubscription(newSub);
			} // endif

			SubscriptionEdit.this.setResult(RESULT_OK);
			SubscriptionEdit.this.finish();
		});

		findViewById(R.id.testEditSite).setOnClickListener(v -> testUrl());

		Spinner s1 = findViewById(R.id.subMax);
		ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, mStrings);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		s1.setAdapter(adapter);
		if (currentSub != null) {
			for (int i = 0; i < mValues.length; i++) {
				if (mValues[i] == currentSub.maxDownloads) {
					s1.setSelection(i);
				}
			}
		}
		
		if (getIntent().hasExtra("focus")) {
			findViewById(R.id.editsite_url).requestFocus();
        }

		if (getIntent().hasExtra("subscription")) {
			currentSub = (Subscription) getIntent().getExtras().get(
					"subscription");
		} else {
			// we're coming from the browser
			if( Intent.ACTION_VIEW.equals( getIntent().getAction() ) ) {
				Log.d("onCreate", "data: "+getIntent().getDataString());
				String feedUrl = getIntent().getDataString();
				((TextView) findViewById(R.id.editsite_url)).setText(feedUrl);
//				currentSub = new Subscription("", feedFile);
				testUrl();
			}
		}

		setCurrentSubValues();
	}
	
	private void testUrl() {
		DownloadHistory history = new DownloadHistory(getConfig());
		enclosureHandler = new EnclosureHandler(history);
		Spinner spinner = findViewById(R.id.subMax);
		int max = mValues[spinner.getSelectedItemPosition()];
		if (max == Subscription.GLOBAL) {
			max = getConfig().getMax();
		}
		enclosureHandler.setMax(max);

		dialog = ProgressDialog.show(SubscriptionEdit.this, "Testing Subscription", "Testing Subscription URL.\nPlease wait...",
				true);
		dialog.show();

		new Thread(SubscriptionEdit.this).start();

	}


	private static final String[] mStrings = { "global setting", "2", "4", "6", "10", "Unlimited" };
	private static final int[] mValues = { Subscription.GLOBAL, 2, 4, 6, 10, EnclosureHandler.UNLIMITED };

	private EnclosureHandler enclosureHandler;

	@Override
	public void run() {
		testException = null;
		try {
			Util.findAvailablePodcasts(getURL(), enclosureHandler);
		} catch (Exception e) {
			testException = e;
		}

		handler.sendEmptyMessage(0);
	}

	private String getURL() {
		String url = ((TextView) findViewById(R.id.editsite_url)).getText().toString();
		if (!url.startsWith("http://") && !url.startsWith("https://")){
			url = "http://"+url;
		}
		if(url.startsWith("http://http://")){
			url = url.substring("http://".length());
		}
		return url;
	}

	Exception testException;

	private final Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			dialog.dismiss();
			if (testException != null) {
				Log.e("editSite", "testURL " + getURL(), testException);
				Util.toast(SubscriptionEdit.this, "Problem accessing feed. " + testException.toString());
				TextView urlTV = findViewById(R.id.editsite_url);
				urlTV.requestFocus();
				return;
			}
			Util.toast(SubscriptionEdit.this, "Feed is OK.  Would download " + enclosureHandler.metaNets.size() + " podcasts.");

			TextView nameTV = findViewById(R.id.editsite_name);
			if (enclosureHandler.title.length() != 0 && nameTV.getText().length() == 0) {
				nameTV.setText(enclosureHandler.getTitle());
			}
			
			findViewById(R.id.saveEditSite).requestFocus();
		}
	};
}

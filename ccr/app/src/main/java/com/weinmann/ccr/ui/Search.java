package com.weinmann.ccr.ui;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Button;
import android.widget.TextView;

import com.weinmann.ccr.R;
import com.weinmann.ccr.core.CarCastResurrectedApplication;
import com.weinmann.ccr.core.Util;
import com.weinmann.ccr.util.Updater;

public class Search extends BaseActivity {

	// Need handler for callbacks to the UI thread
	final Handler handler = new Handler();

	// Create runnable for posting
	final Runnable mUpdateResults = new Runnable() {
		@Override public void run() {
			if (contentService.startSearch("-status-").equals("done")) {
				updater.allDone();
				if (contentService.startSearch("-results-").equals("")) {
					Util.toast(Search.this, "No Results Found.");
					TextView searchText = findViewById(R.id.searchText);
					Button searchButton = findViewById(R.id.searchButton);
					searchButton.setEnabled(true);
					searchText.setEnabled(true);
				} else {
					startActivity(new Intent(Search.this,
							SearchResults.class));
				}
			}
		}
	};

	Updater updater;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.search);

		setTitle(CarCastResurrectedApplication.getAppTitle()+": search for new subscriptions");

		final TextView searchText = findViewById(R.id.searchText);
		final Button searchButton = findViewById(R.id.searchButton);
		searchText.setOnEditorActionListener((v, actionId, event) -> {
			searchButton.setEnabled(false);
			searchText.setEnabled(false);
			Util.toast(this, "Searching....");
			contentService.startSearch(searchText.getText().toString());
			updater = new Updater(handler, mUpdateResults);
			return true;
		});

		searchButton.setOnClickListener(v -> {
			searchButton.setEnabled(false);
			searchText.setEnabled(false);
			contentService.startSearch(searchText.getText().toString());
			updater = new Updater(handler, mUpdateResults);
		});

	}

	@Override
	protected void onPause() {
		super.onPause();
		if (updater!=null)
			updater.allDone();
	}

	@Override
	protected void onResume() {
		super.onResume();
		TextView searchText = findViewById(R.id.searchText);
		Button searchButton = findViewById(R.id.searchButton);
		searchButton.setEnabled(true);
		searchText.setEnabled(true);
	}

}

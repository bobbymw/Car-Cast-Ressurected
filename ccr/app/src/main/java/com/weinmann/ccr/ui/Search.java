package com.weinmann.ccr.ui; import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

import com.weinmann.ccr.R;
import com.weinmann.ccr.core.CarCastResurrectedApplication;
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
					Toast.makeText(getApplicationContext(),
							"No Results Found.", Toast.LENGTH_LONG).show();
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
			Toast.makeText(getContentService(), "Searching....", Toast.LENGTH_LONG).show();
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

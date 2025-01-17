package com.weinmann.ccr.ui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.weinmann.ccr.R;
import com.weinmann.ccr.core.Config;
import com.weinmann.ccr.util.Updater;

public class DownloadProgress extends BaseActivity implements Runnable {

	final Handler handler = new Handler();
	Updater updater;

	@Override
	protected void onPostContentServiceChanged() {
		String status = contentService.encodedDownloadStatus();
		boolean idle = false;
		if (status.equals("")) {
			idle = true;
		} else {
			if (status.split(",")[0].equals("done")) {
				idle = true;
			}
		}
		Button startDownloads = findViewById(R.id.startDownloads);
		Button abort = findViewById(R.id.AbortDownloads);
		startDownloads.setEnabled(idle);
		abort.setEnabled(!idle);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.download_progress);

		final Button startDownloads = findViewById(R.id.startDownloads);
		startDownloads.setOnClickListener(v -> {

			SharedPreferences app_preferences = PreferenceManager.getDefaultSharedPreferences(DownloadProgress.this);
			WifiManager wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);

			if (app_preferences.getBoolean("wifiDownload", true) && (!wifi.isWifiEnabled() || wifi.getConnectionInfo().getIpAddress() == 0)
					&& app_preferences.getBoolean("wifiWarning", true) 
					) {

				String title =  "WIFI is not enabled.";
				if (wifi.getConnectionInfo().getIpAddress() == 0) title = "WIFI is not connected.";

				new AlertDialog.Builder(DownloadProgress.this).setTitle(title).setIcon(android.R.drawable.ic_dialog_alert)
				.setMessage("Do you want to use your carrier?  You may use up your data plan's bandwidth allocation or incur overage charges...")
				.setNegativeButton("Yikes, no", null).setPositiveButton("Sure, go ahead", (dialog, which) -> doDownloads()).show();
			} else {
				//either WIFI is enabled or settings indicate WIFI is not required for auto download, so go ahead
				doDownloads();
			}
		});

		final Button abort = findViewById(R.id.AbortDownloads);
		abort.setOnClickListener(v -> {
			NotificationManager mNotificationManager = (NotificationManager) getSystemService(Activity.NOTIFICATION_SERVICE);
			mNotificationManager.cancel(22);
			mNotificationManager.cancel(23);
			// Crude... but effective...
			System.exit(-1);
		});

		final Button downloadDetails = findViewById(R.id.downloadDetails);
		downloadDetails.setOnClickListener(v -> startActivity(new Intent(DownloadProgress.this, Downloader.class)));

		startDownloads.setEnabled(false);
		abort.setEnabled(false);
		reset();
	}

	//Do the downloads
	private void doDownloads()
	{
		reset();
		contentService.startDownloadingNewPodCasts();

		findViewById(R.id.AbortDownloads).setEnabled(true);
		findViewById(R.id.startDownloads).setEnabled(false);
	}

	@Override
	protected void onPause() {
		super.onPause();
		// stop display thread
		updater.allDone();
	}

	@Override
	protected void onResume() {
		super.onResume();
		updater = new Updater(handler, this);
	}

	private void reset() {
		TextView labelSubscriptionSites = findViewById(R.id.labelSubscriptionSites);
		TextView scanning = findViewById(R.id.scanning);
		TextView downloadingLabel = findViewById(R.id.downloadingLabel);
		TextView progressSimple = findViewById(R.id.progressSimple);
		TextView progressBytes = findViewById(R.id.progressBytes);
		ProgressBar progressBar = findViewById(R.id.progress);
		TextView subscriptionName = findViewById(R.id.subscriptionName);
		TextView title = findViewById(R.id.title);

		labelSubscriptionSites.setVisibility(TextView.INVISIBLE);
		scanning.setVisibility(TextView.INVISIBLE);
		downloadingLabel.setVisibility(TextView.INVISIBLE);
		progressSimple.setVisibility(TextView.INVISIBLE);
		progressBytes.setVisibility(TextView.INVISIBLE);
		progressBar.setVisibility(TextView.INVISIBLE);

		scanning.setText("");
		progressSimple.setText("");
		progressBytes.setText("");
		progressBar.setProgress(0);
		subscriptionName.setText("");
		title.setText("");
	}

	// Called once a second in the UI thread to update the screen.
	@Override public void run() {
		String downloadStatus = null;
		try {
			downloadStatus = contentService.encodedDownloadStatus();
			if (!downloadStatus.equals("")) {
				updateFromString(downloadStatus);
			}
			if (downloadStatus.equals("") || downloadStatus.startsWith("done,")) {
				findViewById(R.id.startDownloads).setEnabled(true);
				findViewById(R.id.AbortDownloads).setEnabled(false);
			} else {
				findViewById(R.id.startDownloads).setEnabled(false);
				findViewById(R.id.AbortDownloads).setEnabled(true);
			}
		} catch (Exception e) {
		}
	}

	private void updateFromString(String downloadStatus) {
		// Toss out first comma separated value (busy or idle)
		List<String> fullStatus = new ArrayList<>(Arrays.asList(downloadStatus.split(",")));
		fullStatus.remove(0);
		String[] status = fullStatus.toArray(new String[fullStatus.size()]);

		TextView labelSubscriptionSites = findViewById(R.id.labelSubscriptionSites);
		TextView scanning = findViewById(R.id.scanning);
		labelSubscriptionSites.setVisibility(TextView.VISIBLE);
		scanning.setVisibility(TextView.VISIBLE);
		TextView downloadingLabel = findViewById(R.id.downloadingLabel);
		TextView progressSimple = findViewById(R.id.progressSimple);
		TextView progressBytes = findViewById(R.id.progressBytes);
		ProgressBar progressBar = findViewById(R.id.progress);

		labelSubscriptionSites.setVisibility(TextView.VISIBLE);
		scanning.setText("  Scanning sites " + status[0] + "/" + status[1]);
		if (status[3].equals("0")) {
			downloadingLabel.setVisibility(TextView.INVISIBLE);
			progressSimple.setVisibility(TextView.INVISIBLE);
			progressBytes.setVisibility(TextView.INVISIBLE);
			progressBar.setVisibility(TextView.INVISIBLE);
			progressBar.setProgress(0);
		} else {
			downloadingLabel.setVisibility(TextView.VISIBLE);
			progressSimple.setVisibility(TextView.VISIBLE);
			progressBytes.setVisibility(TextView.VISIBLE);
			progressBar.setVisibility(TextView.VISIBLE);
			progressSimple.setText("   Podcast " + status[2] + "/" + status[3]);
			long cb = Long.parseLong(status[4]);
			long tb = Long.parseLong(status[5]);
			if (tb == 0) {
				progressBytes.setText("");
				progressBar.setProgress(0);
			} else {
				progressBytes.setText("   " + cb / 1024 + "k/" + tb / 1024 + "k");
				progressBar.setProgress((int) ((cb * 100) / tb));
			}
		}
		TextView subscriptionName = findViewById(R.id.subscriptionName);
		TextView title = findViewById(R.id.title);
		subscriptionName.setText(status[6]);
		title.setText(status[7]);

		if (downloadStatus.startsWith("done,")) {
			if (status[3].equals("0")) {
				progressSimple.setVisibility(TextView.VISIBLE);
				progressSimple.setText("  No new podcasts found.");
			}
			subscriptionName.setText("");
			title.setText("   *** COMPLETED ***");
		}
	}

}
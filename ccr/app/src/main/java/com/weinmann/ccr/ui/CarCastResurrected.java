package com.weinmann.ccr.ui;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;

import com.weinmann.ccr.R;
import com.weinmann.ccr.core.CarCastResurrectedApplication;
import com.weinmann.ccr.core.MediaMode;
import com.weinmann.ccr.util.Updater;

import java.io.File;
import java.lang.reflect.Method;

public class CarCastResurrected extends BaseActivity {
	public final static int MANAGE_EXTERNAL_STORAGE_PERMISSION_REQUEST = 10101;

	boolean toggleOnPause;
	Updater updater;
	ImageButton pausePlay = null;

	// Need handler for callbacks to the UI thread
	final Handler handler = new Handler();

	// Create runnable for posting
	final Runnable mUpdateResults = () -> updateUI();

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if (requestCode == MANAGE_EXTERNAL_STORAGE_PERMISSION_REQUEST) {
			contentService.resetPodcastDir();
		}
		updateUI();
	}

	@Override
	protected void onPostContentServiceChanged() {
		updatePausePlay();
		updateUI();
	}

	private void updatePausePlay() {
		if (contentService == null) {
			return;
		}
		if (pausePlay == null) {
			pausePlay = findViewById(R.id.pausePlay);
		}
		if (contentService.isPlaying()) {
			pausePlay.setImageResource(R.drawable.player_102_pause);
		} else {
			pausePlay.setImageResource(R.drawable.player_102_play);
		}
	}

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);

		setTitle(CarCastResurrectedApplication.getAppTitle());

		setContentView(R.layout.player);

		ProgressBar progressBar = findViewById(R.id.progress);
		progressBar.setProgress(0);
		progressBar.setOnTouchListener((v, event) -> {
			contentService.moveTo(event.getX() / v.getWidth());
			updateUI();
			return true;
		});

		final ImageButton pausePlay = findViewById(R.id.pausePlay);
		pausePlay.setBackgroundColor(0x0);
		pausePlay.setSoundEffectsEnabled(true);
		pausePlay.setImageResource(R.drawable.player_102_play);
		pausePlay.setOnClickListener(v -> {
			if (contentService.getCount() == 0)
				return;
			if (contentService.pauseOrPlay()) {
				pausePlay.setImageResource(R.drawable.player_102_pause);
			} else {
				pausePlay.setImageResource(R.drawable.player_102_play);
			}
			updateUI();
		});

		ImageButton rewind30Button = findViewById(R.id.rewind30);
		rewind30Button.setBackgroundColor(0x0);
		rewind30Button.setSoundEffectsEnabled(true);
		rewind30Button.setOnClickListener(new Bumper(this, -30));

		ImageButton forward60Button = findViewById(R.id.forward30);
		forward60Button.setBackgroundColor(0x0);
		forward60Button.setSoundEffectsEnabled(true);
		forward60Button.setOnClickListener(new Bumper(this, 30));

		ImageButton nextButton = findViewById(R.id.next);
		nextButton.setBackgroundColor(0x0);
		nextButton.setSoundEffectsEnabled(true);
		nextButton.setOnClickListener(new BumpCast(this, true));

		ImageButton previousButton = findViewById(R.id.previous);
		previousButton.setBackgroundColor(0x0);
		previousButton.setSoundEffectsEnabled(true);
		previousButton.setOnClickListener(new BumpCast(this, false));

		String lastRun = getConfig().getLastRun();
		boolean shouldShowSplash = getConfig().getShowSplash();
		if (lastRun == null || shouldShowSplash) {
			startActivity(new Intent(this, Splash.class));
		} else if (!lastRun.equals(CarCastResurrectedApplication.releaseData[0])) {
				new AlertDialog.Builder(CarCastResurrected.this)
						.setTitle(CarCastResurrectedApplication.getAppTitle() + " updated")
						.setMessage(CarCastResurrectedApplication.releaseData[1])
						.setNeutralButton("Close", null).show();
		}
		getConfig().saveLastRun();

		int orientationValue = getConfig().getOrientation();
		setRequestedOrientation(orientationValue);

		ActivityCompat.requestPermissions(
				this,
				getConfig().requestedPermissions,
				1
		);

	}

	@SuppressLint("NewApi")
	@Override
	public void onRequestPermissionsResult(int requestCode,
										   @NonNull String[] permissions,
										   @NonNull int[] grantResults) {
		getManageAllFilesPermission();
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.player_menu, menu);

		return true;
	}

	@Override
	public void finish() {
		Log.i("CarCastResurrected", "Finishing CC; contentService is " + contentService);
		if (contentService != null && contentService.isIdle()) {
			getCarCastResurrectedApplication().stopContentService();
		}
		super.finish();
	}

	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		if (item.getItemId() == R.id.purgeAll) {
			contentService.deleteAll();
			return true;
		}
		if (item.getItemId() == R.id.downloadNewPodcasts) {

			startActivityForResult(new Intent(this, DownloadProgress.class), 0);

			return true;
		}
		if (item.getItemId() == R.id.settings) {
			startActivity(new Intent(this, CcrSettings.class));
			return true;
		}
		if (item.getItemId() == R.id.stats) {
            startActivity(new Intent(this, Stats.class));
            return true;
        }
		if (item.getItemId() == R.id.menuSiteList) {
			startActivityForResult(new Intent(this, Subscriptions.class), 0);
			return true;
		}
		if (item.getItemId() == R.id.listPodcasts) {
			startActivityForResult(new Intent(this, PodcastList.class), 0);
			return true;
		}
		return false;
	}

	@Override
	protected void onPause() {
		super.onPause();

		updater.allDone();
	}

	@Override
	protected void onResume() {
		super.onResume();

		if (getConfig().getKeepDisplayOn())
			getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		else
			getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		TextView titleTextView = findViewById(R.id.title);

        titleTextView.setBackground(null);

        updater = new Updater(handler, mUpdateResults);
	}

	public void updateUI() {
		if (contentService == null || !getConfig().arePermissionsConfigured()) {
			return;
		}

		try {
			File podcastsRoot = getConfig().getPodcastsRoot();
			if (!podcastsRoot.exists() || !podcastsRoot.canWrite()) {
				if (!podcastsRoot.mkdirs() || !podcastsRoot.canWrite()) {
					TextView textView = findViewById(R.id.title);
					StringBuilder sb = new StringBuilder();
					sb.append("ERROR ** " + CarCastResurrectedApplication.getAppTitle() + " cannot write to storage: "+podcastsRoot+" ** ");
					sb.append(moreSpaceSuggestions());
					textView.setText(sb.toString());
					return;
				}
			}

			TextView textView = findViewById(R.id.subscriptionName);
			textView.setText(contentService.getCurrentSubscriptionName());

			textView = findViewById(R.id.title);
			textView.setText(contentService.getCurrentTitle());

			textView = findViewById(R.id.location);
			if (contentService.getMediaMode() == MediaMode.Paused) {
				if (toggleOnPause) {
					toggleOnPause = false;
					textView.setText("");
				} else {
					toggleOnPause = true;
					textView.setText(contentService.getLocationString());
				}
			} else {
				textView.setText(contentService.getLocationString());
			}

			textView = findViewById(R.id.where);
			textView.setText(contentService.getWhereString());

			textView = findViewById(R.id.duration);
			textView.setText(contentService.getDurationString());

			ProgressBar progressBar = findViewById(R.id.progress);
			progressBar.setProgress(contentService.getCurrentProgressPercent());
			updatePausePlay();

		} catch (Throwable e) {
			Log.e("ccr", "", e);
		}
	}


    String moreSpaceSuggestions(){

        // getExternalFilesDirs
        try {
            Method method = getApplicationContext().getClass().getMethod("getExternalFilesDirs", String.class);

            File[] fileBases = (File[])method.invoke(getApplicationContext(), new Object[]{null});

            StringBuilder sb = new StringBuilder("\n\nTry one of:\n ");
            sb.append(" ");
            sb.append(android.os.Environment.getExternalStorageDirectory());
            sb.append("\n");
            for(File file: fileBases){
                sb.append("  ");
                sb.append(file);
                sb.append("\n");
            }

            return sb.toString();

        } catch (Throwable e){
            return "";
        }
    }

	@RequiresApi(api = Build.VERSION_CODES.R)
	private void getManageAllFilesPermission() {
		if (!getConfig().arePermissionsConfigured()){
			Intent intent = new Intent();
			intent.setAction(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
			Uri uri = Uri.fromParts("package", this.getPackageName(), null);
			intent.setData(uri);
			startActivityForResult(intent, MANAGE_EXTERNAL_STORAGE_PERMISSION_REQUEST);
		}
	}
}

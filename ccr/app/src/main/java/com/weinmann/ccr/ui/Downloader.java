package com.weinmann.ccr.ui;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.zip.GZIPOutputStream;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import com.weinmann.ccr.R;
import com.weinmann.ccr.core.Sayer;
import com.weinmann.ccr.util.Updater;

/**
 * Lets the user observe download details in all their command line glory.
 *
 * @author bob
 *
 */
public class Downloader extends BaseActivity implements Runnable {

	final Handler handler = new Handler() {
		@Override
		public void handleMessage(Message m) {
			tv.append(m.getData().getCharSequence("text"));
		}
	};

	TextView tv;
	Updater updater;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.download);

		tv = findViewById(R.id.textconsole);
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

	// Called once a second in the UI thread to update the screen.
	@Override public void run() {
		try {
			String text = contentService.getDownloadProgress();
			if (text.length() != 0)
				tv.setText(text);
			else {
				tv.setText("\n\n\nNo download has run or is running.");
			}
		} catch (Exception e) {
		}
	}

}

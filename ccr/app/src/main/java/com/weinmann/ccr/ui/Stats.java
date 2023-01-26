package com.weinmann.ccr.ui;

import android.os.Bundle;
import android.widget.TextView;

import com.weinmann.ccr.R;
import com.weinmann.ccr.core.CarCastResurrectedApplication;
import com.weinmann.ccr.core.Subscription;
import com.weinmann.ccr.services.DownloadHistory;

public class Stats extends BaseActivity {


    protected void onPostContentServiceChanged() {

        setTitle(CarCastResurrectedApplication.getAppTitle() + ": Stats");

        StringBuilder sb = new StringBuilder();
        sb.append("History Size: ");

        DownloadHistory downloadHistory = new DownloadHistory(getConfig());
        sb.append(downloadHistory.size());
        sb.append("\n");

        for(Subscription sub: getSubscriptions()){
            sb.append(sub.name);
            sb.append(": ");
            sb.append(sub.maxDownloads);
            sb.append("\n");
        }
        TextView textView = findViewById(R.id.statsText);
        textView.setText(sb.toString());
    }

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.stats);
	}
}

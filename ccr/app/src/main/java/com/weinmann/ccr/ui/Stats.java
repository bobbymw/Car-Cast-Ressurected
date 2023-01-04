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
import com.weinmann.ccr.core.Subscription;
import com.weinmann.ccr.services.DownloadHistory;
import com.weinmann.ccr.util.Updater;

import java.util.List;

public class Stats extends BaseActivity {


    protected void onContentService() {

        setTitle(CarCastResurrectedApplication.getAppTitle() + ": Stats");

        StringBuilder sb = new StringBuilder();
        sb.append("History Size: ");

        DownloadHistory downloadHistory = new DownloadHistory(getApplicationContext());
        sb.append(downloadHistory.size());
        sb.append("\n");

        for(Subscription sub: getSubscriptions()){
            sb.append(sub.name);
            sb.append(": ");
            sb.append(sub.maxDownloads);
            sb.append("\n");
        }
        TextView textView = (TextView) findViewById(R.id.statsText);
        textView.setText(sb.toString());
    }

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.stats);


	}




}

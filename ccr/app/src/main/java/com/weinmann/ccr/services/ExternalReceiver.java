package com.weinmann.ccr.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class ExternalReceiver extends BroadcastReceiver {

	public static final String PAUSE     = "com.weinmann.ccr.services.external.PAUSE";
	public static final String PLAY      = "com.weinmann.ccr.services.external.PLAY";
	public static final String PAUSEPLAY = "com.weinmann.ccr.services.external.PAUSEPLAY";
	public static final String DOWNLOAD  = "com.weinmann.ccr.services.external.DOWNLOAD";

	public ExternalReceiver() {
                Log.i("CarCastResurrected", "ExternalReceiver()");
	}

	@Override
	public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                Log.i("CarCastResurrected", "ExternalReceiver.onReceive: " + action);

                if ( action.equals(DOWNLOAD) )
                {
                     Log.i("CarCastResurrected", "Download...");
                     Intent dlIntent = new Intent(context, AlarmService.class);
                     context.startService(dlIntent);
                     abortBroadcast();
                     return;
                }

                Intent i = new Intent(context, ContentService.class);
                i.putExtra("external", action);
                context.startService(i);
                abortBroadcast();
                return;
	}
}


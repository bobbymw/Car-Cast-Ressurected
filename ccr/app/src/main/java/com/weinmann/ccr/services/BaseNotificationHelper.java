package com.weinmann.ccr.services;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.weinmann.ccr.ui.CarCastResurrected;

public abstract class BaseNotificationHelper {
    public static final int IMPORTANCE = NotificationManager.IMPORTANCE_MIN;
    public static final String NOTIFICATION_CHANNEL_ID = "10001";
    protected final ContentService mContext;
    protected static NotificationChannel mChannel;
    protected boolean mIsInitialized;

    public BaseNotificationHelper(ContentService context) {
        mContext = context;
        mIsInitialized = false;
    }

    private PendingIntent createPendingIntentForMainActivity() {
        Intent resultIntent = new Intent(mContext , CarCastResurrected.class);
        // resultIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent resultPendingIntent = PendingIntent.getActivity(mContext,
                0 /* Request code */, resultIntent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        return resultPendingIntent;
    }

    protected void cancel(int notificationId)
    {
        NotificationManagerCompat.from(mContext).cancel(notificationId);
    }

    protected NotificationCompat.Builder getBuilder() {
        PendingIntent resultPendingIntent = createPendingIntentForMainActivity();
        NotificationCompat.Builder builder = new NotificationCompat.Builder(mContext, NOTIFICATION_CHANNEL_ID);
        builder.setContentIntent(resultPendingIntent)
               .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
        return builder;
    }

    protected void initialize() {
        if (mChannel == null) {
            NotificationManager notificationManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
            mChannel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, "NOTIFICATION_CHANNEL_NAME", IMPORTANCE);
            mChannel.enableLights(false);
            mChannel.enableVibration(false);

            notificationManager.createNotificationChannel(mChannel);
        }

        mIsInitialized = true;
    }
}

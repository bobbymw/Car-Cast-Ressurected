package com.weinmann.ccr.services;


import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.media.session.MediaButtonReceiver;

import com.weinmann.ccr.R;
import com.weinmann.ccr.ui.CarCastResurrected;

public class DownloadNotificationHelper {

    public static final int IMPORTANCE = NotificationManager.IMPORTANCE_MIN;
    public static final String NOTIFICATION_CHANNEL_ID = "10001";
    public static final int DOWNLOAD_NOTIFICATION_ID = 22;

    private final Context mContext;
    private NotificationChannel mChannel;

    public DownloadNotificationHelper(ContentService context) {
        mContext = context;
    }

    public NotificationCompat.Builder setupBuilder(int icon, String title, String message)
    {
        Intent resultIntent = new Intent(mContext , CarCastResurrected.class);
        resultIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        int pendingIntentFlags = Build.VERSION.SDK_INT > Build.VERSION_CODES.S ? PendingIntent.FLAG_MUTABLE : 0;
        pendingIntentFlags |= PendingIntent.FLAG_UPDATE_CURRENT;
        PendingIntent resultPendingIntent = PendingIntent.getActivity(mContext,
                0 /* Request code */, resultIntent,
                pendingIntentFlags);

        initialize();

        NotificationCompat.Builder builder = new NotificationCompat.Builder(mContext, MediaNotificationHelper.NOTIFICATION_CHANNEL_ID);
        builder.setSmallIcon(icon);
        builder.setContentTitle(title)
               .setContentText(message)
               .setAutoCancel(false)
               .setContentIntent(resultPendingIntent)
               .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);

        return builder;
    }

    public void cancel()
    {
        NotificationManagerCompat.from(mContext).cancel(DOWNLOAD_NOTIFICATION_ID);
    }

    /**
     * Create and push the notification
     */
    public void notify(int icon, String title, String message) {

        Notification notification = setupBuilder(icon, title, message).build();
        NotificationManagerCompat.from(mContext).notify(DOWNLOAD_NOTIFICATION_ID, notification);
    }

    private void initialize() {
        if (mChannel == null) {
            NotificationManager notificationManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
            mChannel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, "NOTIFICATION_CHANNEL_NAME", IMPORTANCE);
            mChannel.enableLights(false);
            mChannel.enableVibration(false);

            notificationManager.createNotificationChannel(mChannel);
        }
    }
}
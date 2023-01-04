package com.weinmann.ccr.services;


import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import androidx.core.app.NotificationCompat;

import com.weinmann.ccr.ui.CarCastResurrected;

public class NotificationHelper {

    private Context mContext;
    private NotificationCompat.Builder mBuilder;
    private NotificationChannel mChannel;
    public static final int IMPORTANCE = NotificationManager.IMPORTANCE_NONE;
    public static final String NOTIFICATION_CHANNEL_ID = "10001";
    public static final int DOWNLOAD_NOTIFICATION_ID = 22;
    public static final int PLAYING_NOTIFICATION_ID = 23;

    public NotificationHelper(Context context) {
        mContext = context;
        mBuilder = new NotificationCompat.Builder(mContext, NOTIFICATION_CHANNEL_ID);
    }

    public Notification buildNotification(int icon, String title, String message)
    {
        /**Creates an explicit intent for an Activity in your app**/
        Intent resultIntent = new Intent(mContext , CarCastResurrected.class);
        resultIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        PendingIntent resultPendingIntent = PendingIntent.getActivity(mContext,
                0 /* Request code */, resultIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        mBuilder.setSmallIcon(icon);
        mBuilder.setContentTitle(title)
                .setContentText(message)
                .setAutoCancel(false)
                .setContentIntent(resultPendingIntent);

        NotificationManager notificationManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);

        if (mChannel == null && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O)
        {
            mChannel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, "NOTIFICATION_CHANNEL_NAME", IMPORTANCE);
            mChannel.enableLights(false);
            mChannel.enableVibration(false);

            mBuilder.setChannelId(NOTIFICATION_CHANNEL_ID);
            notificationManager.createNotificationChannel(mChannel);
        }
        return mBuilder.build();
    }

    public void cancel(int notificationId)
    {
        NotificationManager notificationManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(notificationId);
    }

    /**
     * Create and push the notification
     */
    public void notify(int notificationId, int icon, String title, String message) {
        Notification notification = buildNotification(icon, title, message);
        NotificationManager notificationManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(notificationId, notification);
    }
}
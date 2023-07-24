package com.weinmann.ccr.services;

import android.Manifest;
import android.app.Notification;
import android.content.pm.PackageManager;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

public class DownloadNotificationHelper extends BaseNotificationHelper {

    private static final int DOWNLOAD_NOTIFICATION_ID = 22;


    public DownloadNotificationHelper(ContentService context) {
        super(context);
    }

    public void cancel() {
        cancel(DOWNLOAD_NOTIFICATION_ID);
    }

    public void notify(int icon, String title, String message) {
        initialize();
        NotificationCompat.Builder builder = getBuilder();
        builder.setSmallIcon(icon);
        builder.setContentTitle(title)
                .setContentText(message)
                .setAutoCancel(false);

        Notification notification = builder.build();
        if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        NotificationManagerCompat.from(mContext).notify(DOWNLOAD_NOTIFICATION_ID, notification);
    }
}
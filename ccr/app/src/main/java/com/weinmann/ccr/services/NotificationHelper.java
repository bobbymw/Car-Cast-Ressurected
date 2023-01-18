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

public class NotificationHelper {

    public static final int IMPORTANCE = NotificationManager.IMPORTANCE_MIN;
    public static final String NOTIFICATION_CHANNEL_ID = "10001";
    public static final int DOWNLOAD_NOTIFICATION_ID = 22;
    public static final int PLAYING_NOTIFICATION_ID = 23;

    private final ContentService mContext;
    private NotificationChannel mChannel;
    private boolean isForegroundServiceRunning = false;

    public NotificationHelper(ContentService context) {
        mContext = context;
    }

    public NotificationCompat.Builder setupBuilder(int icon, String title, String message)
    {
        Intent resultIntent = new Intent(mContext , CarCastResurrected.class);
        resultIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        int pendingIntentFlags = android.os.Build.VERSION.SDK_INT > Build.VERSION_CODES.S ? PendingIntent.FLAG_MUTABLE : 0;
        pendingIntentFlags |= PendingIntent.FLAG_UPDATE_CURRENT;
        PendingIntent resultPendingIntent = PendingIntent.getActivity(mContext,
                0 /* Request code */, resultIntent,
                pendingIntentFlags);

        initialize();

        NotificationCompat.Builder builder = MediaStyleHelper.from(mContext, mContext.getMediaSessionCompat());
        builder.setSmallIcon(icon);
        builder.setContentTitle(title)
               .setContentText(message)
               .setAutoCancel(false)
               .setContentIntent(resultPendingIntent)
               .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);

        return builder;
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

        Notification notification = setupBuilder(icon, title, message).build();
        NotificationManagerCompat.from(mContext).notify(notificationId, notification);
    }

    public void notifyPlayPause(boolean play) {
        MediaSessionCompat mediaSessionCompat = mContext.getMediaSessionCompat();
        int nextActionIcon = play ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play;
        String nextActionText = play ? "Pause" : "Play";

        initialize();

        MediaSessionCompat.Token token = mediaSessionCompat.getSessionToken();

        NotificationCompat.Builder builder = MediaStyleHelper.from(mContext, mContext.getMediaSessionCompat());
        addMediaAction(builder, nextActionIcon, nextActionText, PlaybackStateCompat.ACTION_PLAY_PAUSE);
        addMediaAction(builder, android.R.drawable.ic_media_previous, "Prev", PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS);
        addMediaAction(builder, android.R.drawable.ic_media_rew, "Rew", PlaybackStateCompat.ACTION_REWIND);
        addMediaAction(builder, android.R.drawable.ic_media_ff, "FF", PlaybackStateCompat.ACTION_FAST_FORWARD);
        addMediaAction(builder, android.R.drawable.ic_media_next, "Next", PlaybackStateCompat.ACTION_SKIP_TO_NEXT);

        builder.setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
               .setShowActionsInCompactView(0).setMediaSession(token));
        builder.setSmallIcon(R.drawable.ccp_launcher);
        builder.setChannelId(mChannel.getId());

        Notification notification = builder.build();

        if (!isForegroundServiceRunning) {
            isForegroundServiceRunning = true;
            mContext.startForeground(NotificationHelper.PLAYING_NOTIFICATION_ID, notification);
        } else {
            NotificationManagerCompat.from(mContext).notify(NotificationHelper.PLAYING_NOTIFICATION_ID, notification);
        }
    }

    private void addMediaAction(NotificationCompat.Builder builder, int actionIcon, String actionText, long action) {
        builder.addAction(new NotificationCompat.Action(actionIcon, actionText,
                MediaButtonReceiver.buildMediaButtonPendingIntent(mContext, action)));
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
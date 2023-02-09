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

public class MediaNotificationHelper {

    public static final int IMPORTANCE = NotificationManager.IMPORTANCE_MIN;
    public static final String NOTIFICATION_CHANNEL_ID = "10001";
    public static final int PLAYING_NOTIFICATION_ID = 23;

    private final ContentService mContext;
    private NotificationChannel mChannel;
    private NotificationCompat.Action mPrevTrackAction;
    private NotificationCompat.Action mRewindAction;
    private NotificationCompat.Action mFastForwardAction;
    private NotificationCompat.Action mNextTrackAction;
    private boolean isForegroundServiceRunning = false;

    public MediaNotificationHelper(ContentService context) {
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

    public void cancel()
    {
        NotificationManagerCompat.from(mContext).cancel(PLAYING_NOTIFICATION_ID);
    }

    public void notifyPlayPause(boolean play, int position, float speed) {
        MediaSessionCompat mediaSessionCompat = mContext.getMediaSessionCompat();

        updatePlaybackState(play, position, speed, mediaSessionCompat);

        int nextActionIcon = play ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play;
        String nextActionText = play ? "Pause" : "Play";
        NotificationCompat.Action nextAction = createMediaAction(nextActionIcon, nextActionText, PlaybackStateCompat.ACTION_PLAY_PAUSE);

        initialize();

        MediaSessionCompat.Token token = mediaSessionCompat.getSessionToken();

        NotificationCompat.Builder builder = MediaStyleHelper.from(mContext, mContext.getMediaSessionCompat());
        builder.addAction(mPrevTrackAction);
        builder.addAction(mRewindAction);
        builder.addAction(nextAction);
        builder.addAction(mFastForwardAction);
        builder.addAction(mNextTrackAction);

        builder.setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                   .setShowActionsInCompactView(1, 2, 3)
                   .setMediaSession(token));
        builder.setSmallIcon(R.drawable.ccp_launcher);
        builder.setChannelId(mChannel.getId());

        Notification notification = builder.build();

        if (!isForegroundServiceRunning) {
            isForegroundServiceRunning = true;
            mContext.startForeground(MediaNotificationHelper.PLAYING_NOTIFICATION_ID, notification);
        } else {
            NotificationManagerCompat.from(mContext).notify(MediaNotificationHelper.PLAYING_NOTIFICATION_ID, notification);
        }
    }

    private void updatePlaybackState(boolean play, int position, float speed, MediaSessionCompat mediaSessionCompat) {
        int playbackState = play ? PlaybackStateCompat.STATE_PLAYING : PlaybackStateCompat.STATE_PAUSED;
        PlaybackStateCompat.Builder playbackStateCompatBuilder = new PlaybackStateCompat.Builder().setState(playbackState, position, speed);
        PlaybackStateCompat playbackStateCompat = playbackStateCompatBuilder.build();
        mediaSessionCompat.setPlaybackState(playbackStateCompat);
    }

    private NotificationCompat.Action createMediaAction(int actionIcon, String actionText, long action) {
        return new NotificationCompat.Action(actionIcon, actionText,
                MediaButtonReceiver.buildMediaButtonPendingIntent(mContext, action));
    }

    private void initialize() {
        if (mChannel == null) {
            NotificationManager notificationManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
            mChannel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, "NOTIFICATION_CHANNEL_NAME", IMPORTANCE);
            mChannel.enableLights(false);
            mChannel.enableVibration(false);

            notificationManager.createNotificationChannel(mChannel);

            mPrevTrackAction = createMediaAction(android.R.drawable.ic_media_previous, "Prev", PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS);
            mRewindAction = createMediaAction(android.R.drawable.ic_media_rew, "Rew", PlaybackStateCompat.ACTION_REWIND);
            mFastForwardAction = createMediaAction(android.R.drawable.ic_media_ff, "FF", PlaybackStateCompat.ACTION_FAST_FORWARD);
            mNextTrackAction = createMediaAction(android.R.drawable.ic_media_next, "Next", PlaybackStateCompat.ACTION_SKIP_TO_NEXT);
        }
    }
}
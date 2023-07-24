package com.weinmann.ccr.services;


import android.Manifest;
import android.app.Notification;
import android.content.pm.PackageManager;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.media.session.MediaButtonReceiver;

import com.weinmann.ccr.R;

public class MediaNotificationHelper extends BaseNotificationHelper {
    private static final int PLAYING_NOTIFICATION_ID = 23;

    private NotificationCompat.Action mPrevTrackAction;
    private NotificationCompat.Action mRewindAction;
    private NotificationCompat.Action mPlayAction;
    private NotificationCompat.Action mPauseAction;
    private NotificationCompat.Action mFastForwardAction;
    private NotificationCompat.Action mNextTrackAction;
    private boolean isForegroundServiceRunning = false;

    public MediaNotificationHelper(ContentService context) {
        super(context);
    }

    public void cancel() {
        cancel(PLAYING_NOTIFICATION_ID);
    }

    public void notifyPlayPause(boolean isPlaying, int position, float speed) {
        initialize();
        MediaSessionCompat mediaSessionCompat = mContext.getMediaSessionCompat();

        updatePlaybackState(isPlaying, position, speed, mediaSessionCompat);

        NotificationCompat.Builder builder = getBuilder();

        NotificationCompat.Action nextAction = isPlaying ? mPauseAction : mPlayAction;
        builder.addAction(mPrevTrackAction);
        builder.addAction(mRewindAction);
        builder.addAction(nextAction);
        builder.addAction(mFastForwardAction);
        builder.addAction(mNextTrackAction);

        MediaSessionCompat.Token token = mediaSessionCompat.getSessionToken();
        builder.setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                .setShowActionsInCompactView(1, 2, 3)
                .setMediaSession(token));

        MediaDescriptionCompat description = mediaSessionCompat.getController().getMetadata().getDescription();
        builder.setContentTitle(description.getTitle())
                .setContentText(description.getSubtitle())
                .setSmallIcon(R.drawable.ccp_launcher)
                .setLargeIcon(description.getIconBitmap())
                .setDeleteIntent(
                        MediaButtonReceiver.buildMediaButtonPendingIntent(mContext, PlaybackStateCompat.ACTION_STOP));

        Notification notification = builder.build();

        if (!isForegroundServiceRunning) {
            isForegroundServiceRunning = true;
            mContext.startForeground(PLAYING_NOTIFICATION_ID, notification);
        } else {
            if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            NotificationManagerCompat.from(mContext).notify(PLAYING_NOTIFICATION_ID, notification);
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

    @Override
    protected void initialize() {
        if (!mIsInitialized) {
            super.initialize();

            mPrevTrackAction = createMediaAction(android.R.drawable.ic_media_previous, "Prev", PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS);
            mRewindAction = createMediaAction(android.R.drawable.ic_media_rew, "Rew", PlaybackStateCompat.ACTION_REWIND);
            mPlayAction = createMediaAction(android.R.drawable.ic_media_play, "Play", PlaybackStateCompat.ACTION_PLAY_PAUSE);
            mPauseAction = createMediaAction(android.R.drawable.ic_media_pause, "Pause", PlaybackStateCompat.ACTION_PLAY_PAUSE);
            mFastForwardAction = createMediaAction(android.R.drawable.ic_media_ff, "FF", PlaybackStateCompat.ACTION_FAST_FORWARD);
            mNextTrackAction = createMediaAction(android.R.drawable.ic_media_next, "Next", PlaybackStateCompat.ACTION_SKIP_TO_NEXT);
        }
    }
}
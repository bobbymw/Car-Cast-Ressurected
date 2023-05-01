package com.weinmann.ccr.services;

import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.PlaybackParams;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.KeyEvent;

import androidx.annotation.NonNull;
import androidx.media.session.MediaButtonReceiver;

import com.weinmann.ccr.R;
import com.weinmann.ccr.core.AudioFocusHelper;
import com.weinmann.ccr.core.CarCastResurrectedApplication;
import com.weinmann.ccr.core.Config;
import com.weinmann.ccr.core.Location;
import com.weinmann.ccr.core.MediaMode;
import com.weinmann.ccr.core.MusicFocusable;
import com.weinmann.ccr.core.Util;

import java.io.File;
import java.io.IOException;
import java.util.SortedSet;

public class ContentService extends Service implements MediaPlayer.OnCompletionListener, MusicFocusable {
    private final IBinder binder = new LocalBinder();
    private final MediaNotificationHelper mMediaNotificationHelper = new MediaNotificationHelper(this);
    private final DownloadNotificationHelper mDownloadNotificationHelper = new DownloadNotificationHelper(this);
    private DownloadHelper downloadHelper;
    private Location location;
    private MediaMode mediaMode = MediaMode.UnInitialized;

    public MediaSessionCompat getMediaSessionCompat() {
        return mMediaSessionCompat;
    }

    private MediaSessionCompat mMediaSessionCompat;
    private MediaPlayer mediaPlayer;
    private MetaHolder metaHolder;
    private SearchHelper searchHelper;

    enum PauseReason {
        PhoneCall,
        UserRequest,  // paused by user request
        FocusLoss,    // paused because of audio focus loss
    }

    // why did we pause?
    private PauseReason mPauseReason = PauseReason.UserRequest;

    // do we have audio focus?
    enum AudioFocus {
        NoFocusNoDuck,    // we don't have audio focus, and can't duck
        NoFocusCanDuck,   // we don't have focus, but can play at a low volume ("ducking")
        Focused           // we have full audio focus
    }

    private AudioFocus mAudioFocus = AudioFocus.NoFocusNoDuck;

    // Lifted from RandomMusicPlayer google reference application
    // our AudioFocusHelper object, if it's available (it's available on SDK level >= 8)
    // If not available, this will be null. Always check for null before using!
    private AudioFocusHelper mAudioFocusHelper = null;

    private final BroadcastReceiver mNoisyReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            pauseNow();
        }
    };

    private final MediaSessionCompat.Callback mMediaSessionCallback = new MediaSessionCompat.Callback() {
        @Override
        public boolean onMediaButtonEvent(@NonNull Intent mediaButtonEvent) {
            String intentAction = mediaButtonEvent.getAction();

            if (Intent.ACTION_MEDIA_BUTTON.equals(intentAction))
            {
                KeyEvent event = mediaButtonEvent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);

                if (event != null)
                {
                    int action = event.getAction();

                    if (action == KeyEvent.ACTION_DOWN) {
                        switch (event.getKeyCode()) {
                            case KeyEvent.KEYCODE_MEDIA_FAST_FORWARD:
                                bumpForwardSeconds(30);
                                return true;
                            case KeyEvent.KEYCODE_MEDIA_NEXT:
                                next();
                                return true;
                            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                                pauseOrPlay();
                                return true;
                            case KeyEvent.KEYCODE_MEDIA_PLAY:
                                if (!isPlaying()) {
                                    play();
                                }
                                return true;
                            case KeyEvent.KEYCODE_MEDIA_PAUSE:
                            case KeyEvent.KEYCODE_MEDIA_STOP:
                                if (isPlaying()) {
                                    pauseNow();
                                }
                                return true;
                            case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
                                previous();
                                return true;
                            case KeyEvent.KEYCODE_MEDIA_REWIND:
                                bumpForwardSeconds(-30);
                                return true;
                        }
                    }
                }

                return false;
            }
            return super.onMediaButtonEvent(mediaButtonEvent);
        }
    };

    public void initMediaPlayer() {
        try {
            if (mediaPlayer == null) {
                mediaPlayer = new MediaPlayer();
            }
        } catch (Exception e) {
            Log.d("CarCastResurrected", "Error doing reset", e);
        }
    }

    /**
     * Class for clients to access. Because we know this service always runs in the same process as its clients, we
     * don't need to deal with IPC.
     */
    public class LocalBinder extends Binder {
        public ContentService getService() {
            return ContentService.this;
        }
    }

    public void bumpForwardSeconds(int seconds) {
        if (metaHolder.getCurrentMeta() == null) {
            return;
        }

        int offsetMs = seconds * 1000;
        int ms = mediaPlayer.getCurrentPosition() + offsetMs;
        seekToMilliseconds(ms);
    }

    private void seekToMilliseconds(int ms) {
        int durationMs = getCurrentDurationMs();

        if (durationMs <= 0)
            return;

        if (ms < 0) {
            ms = 0;
        } else if (ms > durationMs) {
            ms = durationMs - 1;
        }

        if (mediaMode == MediaMode.UnInitialized) {
            loadPlayerFromCurrentMeta();
        }

        mediaPlayer.seekTo(ms);
        metaHolder.getCurrentMeta().setCurrentPosMs(ms);
        metaHolder.getCurrentMeta().save();
        notifyPlayPause();
    }

    public MetaFile getCurrentMeta() {
        return metaHolder.getCurrentMeta();
    }

    private int getCurrentDurationMs() {
        MetaFile metaFile = metaHolder.getCurrentMeta();
        if (metaFile == null) {
            return 0;
        }

        int dur = metaFile.getDurationMs();

        if (dur <= 0) {
            metaFile.computeDuration();
            dur = metaFile.getDurationMs();
        }

        return dur;
    }

    public File getCurrentFile() {
        MetaFile meta = metaHolder.getCurrentMeta();
        return meta == null ? null : meta.file;
    }

    private int getCurrentPositionMsFromMeta() {
        MetaFile meta = metaHolder.getCurrentMeta();
        return meta == null ? 0 : meta.getCurrentPosMs();
    }

    public int getCurrentProgress() {
        int duration = getCurrentDurationMs();
        if (duration <= 0)
            return 0;

        return getCurrentPositionMs() * 100 / duration;
    }

    private int getCurrentPositionMs() {
        return mediaMode == MediaMode.UnInitialized ? getCurrentPositionMsFromMeta() :
                mediaPlayer.getCurrentPosition();
    }

    boolean isDownloading() {
        if (downloadHelper == null)
            return false;
        return downloadHelper.isRunning();
    }

    public String getCurrentTitle() {
        if (metaHolder.getSize() > 0) {
            if (isDownloading()) {
                return "Downloading podcasts\n" + downloadHelper.getStatus();
            }
        }

        return metaHolder.getCurrentTitle();
    }

    public void deletePodcast(int position) {
        if (isPlaying() && metaHolder.getCurrentPodcastInPlayer() == position) {
            pauseNow();
        }

        metaHolder.delete(position);

        try {
            fullReset();
        } catch (Throwable e) {
            // bummer.
        }
    }

    void doDownloadCompletedNotification(int got) {

        // Allow UI to update download text (only when in debug mode) this seems
        // suboptimal
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        if (got == 0 && !getConfig().getNotifyOnZeroDownloads()) {
            mDownloadNotificationHelper.cancel();
        } else {

            mDownloadNotificationHelper.notify(R.drawable.icon2, "Downloads Finished", "Downloaded " + got + " podcasts.");
        }

        metaHolder = new MetaHolder(getConfig(), getCurrentFile());
    }

    public String encodedDownloadStatus() {
        if (downloadHelper == null) {
            return "";
        }

        return downloadHelper.getEncodedStatus();
    }

    private boolean fullReset() {
        loadPlayerFromCurrentMeta();
        mediaPlayer.setOnCompletionListener(this);
        return true;
    }

    private void applyVariableSpeedProperties() {
        float speed = getConfig().getSpeedChoice();

        PlaybackParams playbackParams = mediaPlayer.getPlaybackParams();
        playbackParams.setSpeed(speed);
        mediaPlayer.setPlaybackParams(playbackParams);
    }

    public int getCount() {
        return metaHolder.getSize();
    }

    public String getCurrentSubscriptionName() {
        MetaFile currentMeta = metaHolder.getCurrentMeta();
        return currentMeta == null ? "" : currentMeta.getFeedName();
    }

    public String getDurationString() {
        return Util.getTimeString(getCurrentDurationMs());
    }

    public String getLocationString() {
        if (isPlaying()) {
            return Util.getTimeString(mediaPlayer.getCurrentPosition());
        }
        if (metaHolder.getCurrentMeta() != null)
            return Util.getTimeString(metaHolder.getCurrentMeta().getCurrentPosMs());
        return "";
    }

    public MediaMode getMediaMode() {
        return mediaMode;
    }

    public String getWhereString() {
        StringBuilder sb = new StringBuilder();
        if (metaHolder.getSize() == 0)
            sb.append('0');
        else
            sb.append(metaHolder.getCurrentPodcastInPlayer() + 1);
        sb.append('/');
        sb.append(metaHolder.getSize());
        return sb.toString();
    }

    public void moveToProgressPercentage(double progressPercentage) {
        int ms = (int) (progressPercentage * getCurrentDurationMs());

        seekToMilliseconds(ms);
    }

    // Called when user hits next button. Might be playing or not playing at the
    // time.
    public void next() {
        boolean isPlaying = mediaPlayer.isPlaying();
        if (isPlaying) {
            metaHolder.getCurrentMeta().setCurrentPosMs(mediaPlayer.getCurrentPosition());
            metaHolder.getCurrentMeta().save();
            mediaPlayer.stop();
        }
        next(isPlaying);
    }

    // called when user hits button (might be playing or not playing) and called
    // when
    // the playback engine his the "onCompletion" event (ie. a podcast has
    // finished, in which case
    // we are actually no longer playing but we were just were a millisecond or
    // so ago.)
    private void next(boolean wasPlaying) {
        mediaMode = MediaMode.UnInitialized;

        boolean positionWasSet = metaHolder.setCurrentMeta(metaHolder.getCurrentPodcastInPlayer() + 1);
        // if we are at end.
        if (!positionWasSet) {
            saveState();
            // activity.disableJumpButtons();
            mediaPlayer.reset();
            // say(activity, "That's all folks");
            notifyPlayPause();
            return;
        }

        if (wasPlaying)
            play();
        else
            notifyPlayPause();
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.i("CarCastResurrected", "ContentService binding " + intent);
        return binder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.i("CarCastResurrected", "ContentService unbinding " + intent);
        return super.onUnbind(intent);
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        if (metaHolder.getCurrentMeta() == null)
            return;
        metaHolder.getCurrentMeta().setCurrentPosMs(0);
        metaHolder.getCurrentMeta().setListenedTo();
        metaHolder.getCurrentMeta().save();
        if (getConfig().getAutoPlayNext()) {
            next(true);
        } else {
            disableNotification();
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();

        partialWakeLock = ((PowerManager) getSystemService(Context.POWER_SERVICE)).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                CarCastResurrectedApplication.getAppTitle());
        partialWakeLock.setReferenceCounted(false);

        initPhoneStateHandling();

        metaHolder = new MetaHolder(getConfig());

        restoreState();

        mAudioFocusHelper = new AudioFocusHelper(getApplicationContext(), this);
        initMediaSession();
        updateMediaSessionMetadata();
        initNoisyReceiver();
    }

    private void initPhoneStateHandling() {
        PhoneStateListener phoneStateListener = new PhoneStateListener() {
            @Override
            public void onCallStateChanged(int state, String incomingNumber) {
                super.onCallStateChanged(state, incomingNumber);

                if (state == TelephonyManager.CALL_STATE_OFFHOOK || state == TelephonyManager.CALL_STATE_RINGING) {
                    // It's possible that this listener is registered before the initMediaPlayer
                    // method is called, which establishes the MediaPlayer instance.
                    if (isPlaying()) {
                        mPauseReason = PauseReason.PhoneCall;
                        pauseNow();
                        bumpForwardSeconds(-5);
                    }
                }

                if (state == TelephonyManager.CALL_STATE_IDLE && mPauseReason == PauseReason.PhoneCall) {
                    mPauseReason = PauseReason.UserRequest;
                    pauseOrPlay();
                }
            }
        };

        final TelephonyManager telMgr = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        telMgr.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
    }


    private void initMediaSession() {
        ComponentName mediaButtonReceiver = new ComponentName(getApplicationContext(), MediaButtonReceiver.class);
        mMediaSessionCompat = new MediaSessionCompat(getApplicationContext(), "Tag", mediaButtonReceiver, null);

        Intent mediaButtonIntent = new Intent(Intent.ACTION_MEDIA_BUTTON);
        mediaButtonIntent.setClass(this, MediaButtonReceiver.class);

        int pendingIntentFlags = android.os.Build.VERSION.SDK_INT > Build.VERSION_CODES.S ? PendingIntent.FLAG_MUTABLE : 0;
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, mediaButtonIntent, pendingIntentFlags);
        mMediaSessionCompat.setCallback(mMediaSessionCallback);
        mMediaSessionCompat.setMediaButtonReceiver(pendingIntent);
    }

    private void initNoisyReceiver() {
        //Handles headphones coming unplugged. cannot be done through a manifest receiver
        IntentFilter filter = new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
        registerReceiver(mNoisyReceiver, filter);
    }

    private void updateMediaSessionMetadata() {
        MediaMetadataCompat.Builder metadataBuilder = new MediaMetadataCompat.Builder();
        Bitmap icon = BitmapFactory.decodeResource(getResources(), R.drawable.ccp_launcher);

        //Notification icon in card
        metadataBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON, icon);
        metadataBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, icon);

        String title = getCurrentTitle();
        metadataBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_ART, icon);
        metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_ARTIST, getCurrentSubscriptionName());
        metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_TITLE, title);
        metadataBuilder.putLong(MediaMetadataCompat.METADATA_KEY_DURATION, getCurrentDurationMs());
        metadataBuilder.putLong(MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER, metaHolder.getCurrentPodcastInPlayer() + 1);
        metadataBuilder.putLong(MediaMetadataCompat.METADATA_KEY_NUM_TRACKS, metaHolder.getSize());

        mMediaSessionCompat.setMetadata(metadataBuilder.build());
    }

    public void resetPodcastDir() {
        metaHolder = new MetaHolder(getConfig());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i("CarCastResurrected", "ContentService.onStartCommand()");

        MediaButtonReceiver.handleIntent(mMediaSessionCompat, intent);

        return Service.START_NOT_STICKY;
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i("CarCastResurrected", "ContentService destroyed");
        disableNotification();

        giveUpAudioFocus();
        mMediaSessionCompat.release();
        unregisterReceiver(mNoisyReceiver);
    }


    void giveUpAudioFocus() {
        if (mAudioFocus == AudioFocus.Focused && mAudioFocusHelper != null
                && mAudioFocusHelper.abandonFocus())
            mAudioFocus = AudioFocus.NoFocusNoDuck;
    }


    public void pauseNow() {
        if (isPlaying()) {

            // Save current position
            metaHolder.getCurrentMeta().setCurrentPosMs(mediaPlayer.getCurrentPosition());
            metaHolder.getCurrentMeta().save();

            mediaPlayer.pause();
            mediaMode = MediaMode.Paused;
            saveState();
        }
        notifyPlayPause();
    }

    /**
     * True for playing. False for paused.
     */
    public boolean pauseOrPlay() {
        try {
            if (isPlaying()) {
                pauseNow();
                return false;
            } else {
                play();
                return true;
            }
        } catch (Exception e) {
            Log.e("CarCastResurrected", "Unexpected exception", e);
            return false;
        }
    }

    private void play() {
        try {
            if (!fullReset())
                return;

            tryToGetAudioFocus();
            applyVariableSpeedProperties();

            if (!isPlaying()) {
                mediaPlayer.start();
            }

            mediaMode = MediaMode.Playing;
            mMediaSessionCompat.setActive(true);

            notifyPlayPause();
            saveState();
        } catch (Exception e) {
            Log.e("CarCastResurrected", "Unexpected exception", e);
        }
    }

    public void play(int position) {
        if (isPlaying()) {
            mediaPlayer.stop();
        }

        boolean positionWasSet = metaHolder.setCurrentMeta(position);

        if (positionWasSet) {
            play();
        }
    }

    public void previous() {
        boolean wasPlaying = isPlaying();
        if (wasPlaying) {
            mediaPlayer.stop();
        }
        mediaMode = MediaMode.UnInitialized;

        wasPlaying = metaHolder.setCurrentMeta(metaHolder.getCurrentPodcastInPlayer() - 1);

        if (wasPlaying) {
            play();
        }
    }

    public void restoreState() {
        final File stateFile = getConfig().getPodcastRootPath("state.dat");
        if (!stateFile.exists()) {
            location = null;
            return;
        }
        try {
            if (location == null) {
                location = Location.load(stateFile);
            }
            tryToRestoreLocation();
        } catch (Throwable e) {
            // bummer.
        }

    }

    public void saveState() {
        try {
            final File stateFile = getConfig().getPodcastRootPath("state.dat");
            location = Location.save(stateFile, getCurrentTitle());
        } catch (Throwable e) {
            // bummer.
        }
    }

    // Synchronized: to ensure that maximum one startDownloadingNewPodCasts()
    // can be active at any time.
    //
    public synchronized void startDownloadingNewPodCasts() {
        if (downloadHelper != null && downloadHelper.isRunning()) {
            Log.w("CarCastResurrected", "abort start - CarCastResurrected already running");
            return;
        } else {
            downloadHelper = new DownloadHelper(getConfig());
        }

        Log.w("CarCastResurrected", "startDownloadingNewPodCasts");
        boolean autoDelete = getConfig().getAutoDelete();
        if (autoDelete) {
            metaHolder.deleteListenedTo();
        }

        updateDownloadNotification("Downloading podcasts started");

        new Thread(() -> {
            try {
                partialWakeLock.acquire(60*1000L /*1 minute*/);

                Log.i("CarCastResurrected", "starting download thread.");
                // Lets not the phone go to sleep while doing
                // downloads....
                PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
                WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "CarCastResurrected:ContentServiceDownloadThread");

                WifiManager.WifiLock wifiLock = null;

                try {
                    // The intent here is keep the phone from shutting
                    // down during a download.
                    wl.acquire(60*1000L /*1 minute*/);

                    // If we have wifi now, lets hold on to it.
                    WifiManager wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
                    if (wifi.isWifiEnabled()) {
                        wifiLock = wifi.createWifiLock("CarCastResurrected");
                        if (wifiLock != null)
                            wifiLock.acquire();
                        Log.i("CarCastResurrected", "Locked Wifi.");
                    }

                    downloadHelper.downloadNewPodCasts(ContentService.this);
                } finally {
                    if (wifiLock != null) {
                        try {
                            wifiLock.release();
                            Log.i("CarCastResurrected", "released Wifi.");
                        } catch (Throwable t) {
                            Log.i("CarCastResurrected", "Yikes, issue releasing Wifi.");
                        }
                    }

                    wl.release();
                }
            } catch (Throwable t) {
                Log.i("CarCastResurrected", "Unpleasantness during download: " + t.getMessage());
            } finally {
                Log.i("CarCastResurrected", "finished download thread.");
                partialWakeLock.release();
            }
        }).start();

    }

    public String startSearch(String search) {
        if (search.equals("-status-")) {
            if (searchHelper.done)
                return "done";
            return "";
        }
        if (search.equals("-results-")) {
            return searchHelper.results;
        }

        searchHelper = new ItunesSearchHelper(search);
        searchHelper.start();
        return "";
    }

    private void tryToRestoreLocation() {
        try {
            if (location == null) {
                return;
            }

            boolean found = metaHolder.setCurrentMetaByTitle(location.title);

            if (!found) {
                location = null;
                return;
            }
            loadPlayerFromCurrentMeta();
        } catch (Throwable e) {
            // bummer.
        }

    }

    private void loadPlayerFromCurrentMeta() {
        mediaPlayer.reset();
        try {
            mediaPlayer.setDataSource(getCurrentFile().toString());
            mediaPlayer.prepare();
        } catch (IOException ex) {
            //no-op
        }
        mediaPlayer.seekTo(metaHolder.getCurrentMeta().getCurrentPosMs());
        if (isPlaying()) {
            mediaPlayer.pause();
        }
        mediaMode = MediaMode.Paused;
    }

    public void updateDownloadNotification(String update) {
        mDownloadNotificationHelper.cancel();
        mDownloadNotificationHelper.notify(R.drawable.iconbusy, "Downloading started", update);
    }

    public boolean isPlaying() {
        return mediaPlayer != null && mediaPlayer.isPlaying();
    }

    public boolean isIdle() {
        return !isPlaying() && (downloadHelper == null || !downloadHelper.isRunning());
    }

    public void purgeAll() {
        if (isPlaying()) {
            pauseNow();
            mediaPlayer.stop();
            mediaPlayer.reset();
        }
        mediaMode = MediaMode.UnInitialized;
        metaHolder.purgeAll();

        metaHolder = new MetaHolder(getConfig(), getCurrentFile());
    }

    public String getDownloadProgress() {
        if (downloadHelper == null)
            return "";
        return downloadHelper.sb.toString();
    }

    public void newContentAdded() {
        metaHolder = new MetaHolder(getConfig(), getCurrentFile());
    }

    public void directorySettingsChanged() {
        metaHolder = new MetaHolder(getConfig(), getCurrentFile());
    }

    private WakeLock partialWakeLock;

    void disableNotification() {
        mMediaNotificationHelper.cancel();
        stopForeground(true);

        partialWakeLock.release();
    }

    private void notifyPlayPause() {
        float speed = getConfig().getSpeedChoice();
        partialWakeLock.acquire(60*1000L /*1 minute*/);
        updateMediaSessionMetadata();
        mMediaNotificationHelper.notifyPlayPause(isPlaying(), getCurrentPositionMs(), speed);
    }

    public SortedSet<Integer> moveTop(SortedSet<Integer> checkedItems) {
        return metaHolder.moveTop(checkedItems);
    }

    public SortedSet<Integer> moveUp(SortedSet<Integer> checkedItems) {
        return metaHolder.moveUp(checkedItems);
    }

    public SortedSet<Integer> moveBottom(SortedSet<Integer> checkedItems) {
        return metaHolder.moveBottom(checkedItems);
    }

    public SortedSet<Integer> moveDown(SortedSet<Integer> checkedItems) {
        return metaHolder.moveDown(checkedItems);
    }

    /**
     * Signals that audio focus was gained.
     */
    @Override
    public void onGainedAudioFocus() {
        mAudioFocus = AudioFocus.Focused;

        if (mPauseReason == PauseReason.FocusLoss) {
            mPauseReason = PauseReason.UserRequest;
            play();
        }
    }

    /**
     * Signals that audio focus was lost.
     *
     * @param canDuck If true, audio can continue in "ducked" mode (low volume). Otherwise, all
     *                audio must stop.
     */
    @Override
    public void onLostAudioFocus(boolean canDuck) {
        mAudioFocus = canDuck ? AudioFocus.NoFocusCanDuck : AudioFocus.NoFocusNoDuck;

        if (isPlaying()) {
            mPauseReason = PauseReason.FocusLoss;
            pauseNow();
            bumpForwardSeconds(-3);
        }
    }

    private void tryToGetAudioFocus() {
        if (mAudioFocus != AudioFocus.Focused && mAudioFocusHelper != null
                && mAudioFocusHelper.requestFocus())
            mAudioFocus = AudioFocus.Focused;
    }

    private Config getConfig() {
        return ((CarCastResurrectedApplication)getApplicationContext()).getConfig();
    }
}

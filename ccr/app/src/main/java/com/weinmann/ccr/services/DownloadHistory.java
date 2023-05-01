package com.weinmann.ccr.services;

import android.util.Log;

import com.weinmann.ccr.core.Config;
import com.weinmann.ccr.core.Sayer;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * The history of all downloaded episodes the data is backed into a file on the SD-card
 */
public class DownloadHistory implements Sayer {
    private static final String UNKNOWN_SUBSCRIPTION = "unknown";
    private final static String HISTORY_TWO_HEADER = "history version 2";
    private List<HistoryEntry> historyEntries = new ArrayList<>();
    private final StringBuilder sb = new StringBuilder();
    private final Config mConfig;


    /**
     * Create a object that represents the download history. It is backed to a file.
     */
    @SuppressWarnings("unchecked")
    public DownloadHistory(Config config) {
        mConfig = config;
        File historyFile = mConfig.getPodcastRootPath("history.prop");
        try (FileInputStream historyFileStream = new FileInputStream(historyFile);
                ObjectInputStream ois = new ObjectInputStream(historyFileStream)) {
                historyEntries = (List<HistoryEntry>) ois.readObject();
        } catch (Throwable e) {
            // would be nice to ask the user if we can submit his history file
            // to the devs for review
            Log.e(DownloadHelper.class.getName(), "error reading history file " + historyFile.toString(), e);
        }
    }

    /**
     * Add a item to the history
     *
     * @param metaNet podcast metadata
     */
    public void add(MetaNet metaNet) {
        historyEntries.add(new HistoryEntry(metaNet.getSubscription(), metaNet.getUrl()));
        save();
    }

    /**
     * Check if a item is in the history
     *
     * @param metaNet the item to check for
     * @return true it the item is in the history
     */
    public boolean contains(MetaNet metaNet) {
        for (HistoryEntry historyEntry : historyEntries) {
            if (!historyEntry.subscription.equals(UNKNOWN_SUBSCRIPTION) &&
                    !historyEntry.subscription.equals(metaNet.getSubscription())) {
                continue;
            }
            if (historyEntry.podcastURL.equals(metaNet.getUrl())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Remove history of all downloaded podcasts
     *
     * @return number of history items deleted
     */
    public int eraseHistory() {
        int size = historyEntries.size();
        historyEntries = new ArrayList<>();
        save();
        return size;
    }

    /**
     * Remove history of all downloaded podcasts for the specified subscription
     *
     * @return number of history items deleted
     */
    public int eraseHistory(String subscription) {
        int size = historyEntries.size();
        List<HistoryEntry> nh = new ArrayList<>();
        for (HistoryEntry he : historyEntries) {
            if (!he.subscription.equals(subscription))
                nh.add(he);
        }
        historyEntries = nh;
        save();
        return size - nh.size();
    }


    private void save() {
        File historyFile = mConfig.getPodcastRootPath("history.prop");
        try (FileOutputStream fos = new FileOutputStream(historyFile);
             DataOutputStream dosDataOutputStream = new DataOutputStream(fos);
             ObjectOutputStream oos = new ObjectOutputStream(dosDataOutputStream)) {

            dosDataOutputStream.write(HISTORY_TWO_HEADER.getBytes());
            dosDataOutputStream.write('\n');

            oos.writeObject(historyEntries);
        } catch (IOException e) {
            say("problem writing history file: " + historyFile + " ex:" + e);
        }
    }

    @Override
    public void say(String text) {
        sb.append(text);
        sb.append('\n');
    }

    /**
     * Get the current size of the download history
     *
     * @return the size
     */
    public int size() {
        return historyEntries.size();
    }
}

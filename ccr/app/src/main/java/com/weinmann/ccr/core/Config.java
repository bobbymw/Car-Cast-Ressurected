package com.weinmann.ccr.core;

import java.io.File;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;

import androidx.core.content.ContextCompat;

public class Config {
    public static final String[] requestedPermissions = {
                    Manifest.permission.READ_PHONE_STATE,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
            };
    private static final String[] orientations = { "AUTO", "Landscape", "Flipped Landscape", "Portrait", "Flipped Portrait" };
    private static final int[] orientationValues = {
            ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR,
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE,
            ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE,
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,
            ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT
    };

    final Context context;

    public Config(Context context) {
        this.context = context;
    }

    public int getMax() {
        String value = getValueOrSetDefault("listmax", "2");
        return Integer.parseInt(value);
    }

    public float getSpeedChoice() {
        String value = getValueOrSetDefault("speedChoice", "1.00");
        return Float.parseFloat(value);
    }

    public boolean getShowSplash() {
        boolean value = getValueOrSetDefault("showSplash", false);
        return value;
    }

    public boolean getKeepDisplayOn() {
        boolean value = getValueOrSetDefault("keep_display_on", true);
        return value;
    }

    public boolean getNotifyOnZeroDownloads() {
        boolean value = getValueOrSetDefault("notifyOnZeroDownloads", true);
        return value;
    }

    public boolean getAutoPlayNext() {
        boolean value = getValueOrSetDefault("autoPlayNext", true);
        return value;
    }

    public boolean getAutoDelete() {
        boolean value = getValueOrSetDefault("autoDelete", false);
        return value;
    }

    public String getLastRun() {
        SharedPreferences app_preferences = PreferenceManager.getDefaultSharedPreferences(context);
        String value = app_preferences.getString("lastRun", null); // no default
        return value;
    }

    public int getOrientation() {
        SharedPreferences app_preferences = PreferenceManager.getDefaultSharedPreferences(context);
        String orientation = app_preferences.getString("orientation", null);
        if (orientation != null) {
            for (int i = 0; i < orientations.length; i++) {
                if (orientation.equals(orientations[i])) {
                    Log.i("CarCastResurrected", "Orientation set to " + orientation + " v=" + orientationValues[i]);
                    return orientationValues[i];
                }
            }
        }

        return ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
    }

    public File getCarCastRoot() {
        String defaultValue = new File(android.os.Environment.getExternalStorageDirectory(), "carcast").toString();
        String fileName = getValueOrSetDefault("CarCastRoot", defaultValue);
        return new File(fileName);
    }

    public File getPodcastsRoot() {
        File result = new File(getCarCastRoot(), "podcasts");
        if (!result.exists()) {
            result.mkdirs();
        }

        return result;
    }

    public File getPodcastRootPath(String path) {
        return new File(getPodcastsRoot(), path);
    }

    public File getCarCastPath(String path) {
        return new File(getCarCastRoot(), path);
    }

    public boolean arePermissionsConfigured() {
        boolean result = (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.R ||
                Environment.isExternalStorageManager());

        if (result) {
            for (String requestedPermission : requestedPermissions) {
                int permissionResult = ContextCompat.checkSelfPermission(context, requestedPermission);
                if (permissionResult != PackageManager.PERMISSION_GRANTED) {
                    result = false;
                    break;
                }
            }
        }

        return result;
    }

    public void saveLastRun() {
        SharedPreferences app_preferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = app_preferences.edit();
        editor.putString("lastRun", CarCastResurrectedApplication.releaseData[0]);
        if (!app_preferences.contains("listmax")) {
            editor.putString("listmax", "2");
        }
        editor.commit();
    }

    private String getValueOrSetDefault(String name, String defaultValue) {
        SharedPreferences app_preferences = PreferenceManager.getDefaultSharedPreferences(context);
        String result = app_preferences.getString(name, null);
        if (result == null)
        {
            SharedPreferences.Editor editor = app_preferences.edit();
            editor.putString(name, defaultValue);
            editor.commit();
            return defaultValue;
        }

        return result;
    }

    private boolean getValueOrSetDefault(String name, boolean defaultValue) {
        SharedPreferences app_preferences = PreferenceManager.getDefaultSharedPreferences(context);
        if (!app_preferences.contains(name))
        {
            SharedPreferences.Editor editor = app_preferences.edit();
            editor.putBoolean(name, defaultValue);
            editor.commit();
            return defaultValue;
        }

        boolean result = app_preferences.getBoolean(name, defaultValue);
        return result;
    }
}

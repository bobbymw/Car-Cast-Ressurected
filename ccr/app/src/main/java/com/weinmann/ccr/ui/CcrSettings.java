package com.weinmann.ccr.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.util.Log;

import com.weinmann.ccr.R;
import com.weinmann.ccr.core.CarCastResurrectedApplication;

public class CcrSettings extends PreferenceActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String version = "";
        try {
            String ourPackage = CarCastResurrected.class.getPackage().getName();
            int lastDot = ourPackage.lastIndexOf('.');
            ourPackage = ourPackage.substring(0, lastDot);
            PackageInfo pInfo = getPackageManager().getPackageInfo(ourPackage, PackageManager.GET_META_DATA);
            version = pInfo.versionName;
        } catch (NameNotFoundException e) {
            Log.e("Settings", "looking up own version", e);
        }
        addPreferencesFromResource(R.xml.settings);

        setTitle(CarCastResurrectedApplication.getAppTitle() + ": " + CarCastResurrectedApplication.getVersion() + " / " + version);
    }

    @Override
    protected void onStop() {
        super.onStop();
        SharedPreferences app_preferences = PreferenceManager.getDefaultSharedPreferences(this);

        //Prepare to cycle the alarm host service
        Intent serviceIntent = new Intent();
        serviceIntent.setAction("com.weinmann.ccr.services.AlarmHostService");

        //We always want to stop
        try {
            stopService(serviceIntent);
        } catch (Throwable e) {
            Log.w("Settings", "stopping AlarmHostService", e);
        }

        //We might want to start
        if (app_preferences.getBoolean("autoDownload", false)) {
            try {
                startService(serviceIntent);
            } catch (Throwable e) {
                Log.e("Settings", "starting AlarmHostService", e);
            }
        }

        ((CarCastResurrectedApplication)getApplication()).directorySettingsChanged();
    }
}

package com.imlongluo.blogreader;

import com.imlongluo.blogreader.service.RefreshService;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.preference.PreferenceManager;

public class BootCompletedBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context
                    .createPackageContext(Constants.PACKAGE, 0));

            preferences.edit().putLong(Constants.PREFERENCE_LASTSCHEDULEDREFRESH, 0).commit();
            if (preferences.getBoolean(Constants.SETTINGS_REFRESHENABLED, false)) {
                context.startService(new Intent(context, RefreshService.class));
            }
            context.sendBroadcast(new Intent(Constants.ACTION_UPDATEWIDGET));
        } catch (NameNotFoundException e) {
        }
    }

}

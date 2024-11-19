package com.hnguyen48206.blesrv;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

public class BootReceiver extends BroadcastReceiver {
private static final String TAG = "BootReceiver";
private static final String PREFS_NAME = "CapacitorStorage";
private static final String SERVICE_RUNNING_KEY = "serviceRunning";

@Override
public void onReceive(Context context, Intent intent) {
if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    boolean wasServiceRunning = prefs.getBoolean(SERVICE_RUNNING_KEY, false);

if (wasServiceRunning) {
Log.d(TAG, "Boot completed, starting BLEForegroundService");

 String activityToStart = "com.innova.tmpdr.BLEForegroundService";
        try {
            Class<?> c = Class.forName(activityToStart);
            Intent serviceIntent = new Intent(context, c);
            context.startForegroundService(serviceIntent);
        } catch (ClassNotFoundException ignored) {
        }

} else {
Log.d(TAG, "Boot completed, but service was not running before shutdown");
}
}
}
}

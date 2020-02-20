package ru.ttdev.wydo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;


public class BootCompleteReceiver extends BroadcastReceiver {
    private static final String TAG = "BootCompleteReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        if (action.equals(Intent.ACTION_POWER_CONNECTED))
            Log.d(TAG, "power connected detected");

        if (action.equals(Intent.ACTION_BOOT_COMPLETED))
            Log.d(TAG, "boot completed detected");

        if (action.equals(Intent.ACTION_LOCKED_BOOT_COMPLETED))
            Log.d(TAG, "locked boot completed detected");

        if (action.equals(Intent.ACTION_EXTERNAL_APPLICATIONS_AVAILABLE))
            Log.d(TAG, "external app available detected");

        Log.d(TAG, "bootcomplete recevied");
        if (AppPreferences.getAutoStart()) ScreenshotService.checkAndStartService();
    }
}

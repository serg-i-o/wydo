package ru.ttdev.wydo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;


public class BootCompleteReceiver extends BroadcastReceiver {
    private static final String LOG_TAG = "BootCompleteReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        if (AppPreferences.getAutoStart()){
            try {
                Log.d(LOG_TAG, "Try to start service");
                Intent service_intent = new Intent(context, ScreenshotService.class);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    Log.d(LOG_TAG, "startForegroundService");
                    context.startForegroundService(service_intent);
                } else {
                    context.startService(service_intent);
                }
            }catch(Exception ex) {
                Toast.makeText(context, ex.getMessage(), Toast.LENGTH_LONG).show();
            }
        } else {
            Log.d(LOG_TAG, "Autostart disabled");
        }
    }
}

package ru.ttdev.wydo;

import android.app.ActivityManager;
import android.content.Context;


public class ServiceHelper {

    public static boolean isMyServiceRunning(Class<?> serviceClass) {
        Context context = AppApplication.getAppContext();
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
}

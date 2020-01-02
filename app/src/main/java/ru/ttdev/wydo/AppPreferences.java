package ru.ttdev.wydo;

import android.content.Context;
import android.content.SharedPreferences;

public class AppPreferences {

    private static final String APP_PREFERENCES = "AppPrefs";
    private static final String AUTOSTART = "service_auto_start";
    private static final String DELAY_SECONDS = "delay_second";
    private static final String STORE_TO_SD = "store in SD";

    private static void savePreference(String key, Object value) {
        SharedPreferences.Editor editor = AppApplication.getAppContext()
                .getSharedPreferences(APP_PREFERENCES, Context.MODE_PRIVATE)
                .edit();

        if (value instanceof String) {
            editor.putString(key, (String) value);
        }
        if (value instanceof Integer) {
            editor.putInt(key, (int) value);
        }
        if (value instanceof Long) {
            editor.putLong(key, (long) value);
        }
        if (value instanceof Boolean) {
            editor.putBoolean(key, (boolean) value);
        }
        editor.apply();
    }

    public static boolean getAutoStart() {
        SharedPreferences sharedPref = AppApplication.getAppContext()
                .getSharedPreferences(APP_PREFERENCES, Context.MODE_PRIVATE);
        return sharedPref.getBoolean(AUTOSTART, true);
    }

    public static void setAutoStart(boolean deviceHash) {
        savePreference(AUTOSTART, deviceHash);
    }

    public static boolean getStoreSD() {
        SharedPreferences sharedPref = AppApplication.getAppContext()
                .getSharedPreferences(APP_PREFERENCES, Context.MODE_PRIVATE);
        return sharedPref.getBoolean(STORE_TO_SD, true);
    }

    public static void setStoreSD(boolean store_to_sd) {
        savePreference(STORE_TO_SD, store_to_sd);
    }

    public static Integer getDelay() {
        SharedPreferences sharedPref = AppApplication.getAppContext()
                .getSharedPreferences(APP_PREFERENCES, Context.MODE_PRIVATE);
        return sharedPref.getInt(DELAY_SECONDS, 5);
    }

    public static void setDelay(Integer delay_seconds) {
        savePreference(DELAY_SECONDS, delay_seconds);
    }
}

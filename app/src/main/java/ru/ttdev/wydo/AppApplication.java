package ru.ttdev.wydo;

import android.app.Application;
import android.content.Context;


public class AppApplication extends Application {

    public static final String TAG = "Wydo";
    private static final String LOG_TAG = "AppApplication";
    private static Context context;


    @Override
    public void onCreate() {
        super.onCreate();

        AppApplication.context = getApplicationContext();
    }

    public static Context getAppContext() {
        return AppApplication.context;
    }

    public static AppApplication getInstance() {
        return (AppApplication) context.getApplicationContext();
    }
}

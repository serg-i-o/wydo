package ru.ttdev.wydo;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import android.provider.Settings;
import android.util.Log;
import android.widget.RemoteViews;


public class ScreenshotService extends Service {
    private static final String LOG_TAG = "ScreenService";
    public static final String NOTIFICATION_CHANNEL_ID="wydo_notify_channel";
    public static final int NOTIFICATION_ID = 0xABC123;

    private Context context;
    private Integer delay_seconds;
    private Integer max_count;
    private boolean store_to_sd;

    private static ScreenshotService instance;
    private MonitorRunnable mMonitorRunnable;
    private ScreenReceiver mScreenReceiver;
    private Handler mHandler;

    public static ScreenshotService getInstance()
    {
        return instance;
    }

    public boolean isRunning() {
        return ServiceHelper.isMyServiceRunning(ScreenshotService.class);
    }

    public void stop() {
        context.stopService(new Intent(context, ScreenshotService.class));
        Log.d(LOG_TAG, "stop");
    }

    public void forceRestart() {
        Log.d(LOG_TAG, "forceRestart");
        stop();
        checkAndStartService();
    }

    public static void checkAndStartService(){
        Log.d(LOG_TAG, "try start ScreenshotService. isServiceRunning = "
                + ServiceHelper.isMyServiceRunning(ScreenshotService.class));
        if (!ServiceHelper.isMyServiceRunning(ScreenshotService.class)) {
            AppApplication.getAppContext().startService(
                    new Intent(AppApplication.getAppContext(), ScreenshotService.class)
            );
            Log.d(LOG_TAG, "start ScreenshotService (checkAndStartService)");
        } else {
            Log.d(LOG_TAG, "ScreenshotService already started");
        }
    }

    public static void checkAndStopService(){
        Log.d(LOG_TAG, "try stop ScreenshotService. isServiceRunning = "
                + ServiceHelper.isMyServiceRunning(ScreenshotService.class));
        if (ServiceHelper.isMyServiceRunning(ScreenshotService.class)) {
            AppApplication.getAppContext().stopService(
                    new Intent(AppApplication.getAppContext(), ScreenshotService.class)
            );
            Log.d(LOG_TAG, "stop ScreenshotService (checkAndStartService)");
        } else {
            Log.d(LOG_TAG, "ScreenshotService already stopped");
        }
    }


    @Override
    public IBinder onBind(Intent i) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        context = AppApplication.getAppContext();
        instance = this;

        delay_seconds = AppPreferences.getDelay();

        Log.d(LOG_TAG, "onCreate");

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        mScreenReceiver = new ScreenReceiver();
        registerReceiver(mScreenReceiver, filter);

//        mHandler = new Handler();
//        mMonitorRunnable = new MonitorRunnable();
//        mHandler.post(mMonitorRunnable);

//        startNotification();
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(LOG_TAG, "onStartCommand");

        mHandler = new Handler();
        mMonitorRunnable = new MonitorRunnable();
        mHandler.post(mMonitorRunnable);

        foregroundify();

        return START_STICKY;
    }


    private void foregroundify(){
        NotificationManager mgr=
                (NotificationManager)getSystemService(NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.O &&
                mgr.getNotificationChannel(NOTIFICATION_CHANNEL_ID)==null) {
            mgr.createNotificationChannel(new NotificationChannel(NOTIFICATION_CHANNEL_ID,
                    "Wydo channel", NotificationManager.IMPORTANCE_DEFAULT));
        }


        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID);
        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.custom_notification);
        builder.setContent(remoteViews);
        builder.setPriority(NotificationCompat.PRIORITY_LOW);
        builder.setVisibility(NotificationCompat.VISIBILITY_SECRET);

        startForeground(NOTIFICATION_ID, builder.build());
    }

    @Override
    public void onDestroy() {
        Log.i(LOG_TAG, "destroy");

        if (mScreenReceiver != null)
            unregisterReceiver(mScreenReceiver);

        super.onDestroy();
        if (mHandler != null){
            mHandler.removeCallbacksAndMessages(null);
        }

    }

    private class MonitorRunnable implements Runnable {
        private PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);

        @Override
        public void run() {
            if (pm.isInteractive()){
                Log.d(LOG_TAG, "try start screensaver");
                // TODO ScreenShot
                mHandler.postDelayed(mMonitorRunnable, delay_seconds * 1000);
            } else {
                Log.d(LOG_TAG, "service standby mode");
                mHandler.postDelayed(mMonitorRunnable, delay_seconds * 1000 * 5);
            }
        }
    }

    private final class ScreenReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
                Log.d(LOG_TAG, "open LockScreen on ACTION_SCREEN_ON");
                mHandler.removeCallbacksAndMessages(null);
                mHandler.post(mMonitorRunnable);
            }

            if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
                Log.d(LOG_TAG, "detected ACTION_SCREEN_OFF");
                mHandler.removeCallbacksAndMessages(null);
            }
        }
    }
}

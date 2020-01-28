package ru.ttdev.wydo;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import android.provider.Settings;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Array;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;


public class ScreenshotService extends Service {
    private static final String LOG_TAG = "ScreenService";
    public static final String NOTIFICATION_CHANNEL_ID="wydo_notify_channel";
    public static final int NOTIFICATION_ID = 0xABC123;
//    public FilesWorker filesWorker = new FilesWorker();

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

    public static void updateValues() {
        updateService();
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

    private static void updateService(){ // Чтобы обновить значения и задержку перезапускаем сервис
        if (ServiceHelper.isMyServiceRunning(ScreenshotService.class)) {
            checkAndStopService();
            checkAndStartService();
        }
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

        Log.d(LOG_TAG, "onCreate");

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        mScreenReceiver = new ScreenReceiver();
        registerReceiver(mScreenReceiver, filter);

//        startNotification();
    }

    public void setDelay_seconds( int v ){
        delay_seconds = v;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(LOG_TAG, "onStartCommand");

        mHandler = new Handler();
        mMonitorRunnable = new MonitorRunnable();
        mHandler.post(mMonitorRunnable);

        delay_seconds = AppPreferences.getDelay();
        max_count = AppPreferences.getMaxFilesCount();
        store_to_sd = AppPreferences.getStoreSD();

        if (!isExternalStorageAvailable() || isExternalStorageReadOnly()) {
            store_to_sd = false;
            Log.d(LOG_TAG, "SD not mounted or read only");
        }

        foregroundify();

        return START_STICKY;
    }

    public static boolean isExternalStorageReadOnly() {
        String extStorageState = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED_READ_ONLY.equals(extStorageState);
    }

    public static boolean isExternalStorageAvailable() {
        String extStorageState = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(extStorageState);
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
                Log.d(LOG_TAG, "try start filecreator");
                createTextFile();
                mHandler.postDelayed(mMonitorRunnable, delay_seconds * 1000);
            } else {
                Log.d(LOG_TAG, "service standby mode");
                mHandler.postDelayed(mMonitorRunnable, delay_seconds * 1000 * 5);
            }
        }
    }


    public void createTextFile(  ){
        Date dateNow = new Date();
        SimpleDateFormat formatForDateNow = new SimpleDateFormat("yyMMddHHmmss");
        String today = formatForDateNow.format(dateNow);
        String filename = today + ".txt";

        FileOutputStream fos = null;
        try {
            if(store_to_sd){
                Log.d(LOG_TAG, String.format("external directory: %s", getExternalFilesDir(null)));
                File fileOnSd = new File(getExternalFilesDir(null), filename);
                fos = new FileOutputStream(fileOnSd);
            }else {
                fos = openFileOutput(filename, MODE_PRIVATE);
            }
            fos.write(today.getBytes());
        }
        catch(IOException ex) {
            Log.d(LOG_TAG, ex.toString() );
            return;
        }
        finally {
            try {
                if (fos != null) fos.close();
            } catch (IOException ex) {
                Log.d(LOG_TAG, "Can't close stream", ex);
            }
        }
        clearFiles();
    }


    private void clearFiles() {
        File filesDir = null;
        if ( store_to_sd ){
            filesDir = getExternalFilesDir(null);
        } else {
            filesDir = getFilesDir();
        }

        if(filesDir == null || filesDir.listFiles() == null){
            Log.d(LOG_TAG, "No files found in the app dir" + getFilesDir().toString());
            return;
        }
        File[] fileList = filesDir.listFiles();

        Log.d( LOG_TAG, "Before clearing " + fileList.length );

        if(fileList.length > max_count ){
            File[] filesToRemove = Arrays.copyOfRange( fileList, max_count - 1, fileList.length - 1 );
            for( File fileToDel: filesToRemove ){
                Log.d(LOG_TAG, String.format("File to delete: %s", fileToDel.getPath()));
                if (fileToDel.exists()) {
                    if (fileToDel.delete()) {
                        System.out.println("file Deleted :" + fileToDel.getPath());
                    } else {
                        System.out.println("file not Deleted :" + fileToDel.getPath());
                    }
                }
            }
        }
        Log.d( LOG_TAG, "After clearing " + getFilesDir().listFiles().length );
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

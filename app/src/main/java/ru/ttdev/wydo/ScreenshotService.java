package ru.ttdev.wydo;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.PowerManager;
import androidx.core.app.NotificationCompat;

import android.util.Log;
import android.view.WindowManager;
import android.widget.RemoteViews;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;


public class ScreenshotService extends Service {
    private static final String LOG_TAG = "ScreenService";
    public static final String NOTIFICATION_CHANNEL_ID="wydo_notify_channel";
    public static final int NOTIFICATION_ID = 0xABC123;
    Notification notification;

    public static final String EXTRA_RESULT_CODE = "resultCode";
    public static final String EXTRA_RESULT_INTENT = "resultIntent";

    static final int VIRT_DISPLAY_FLAGS=
            DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY |
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC;

    private Context context;
    private Integer delay_seconds;
    private Integer max_count;
    private boolean store_to_sd;

    private static ScreenshotService instance;
    private MonitorRunnable mMonitorRunnable;
    private ScreenReceiver mScreenReceiver;
    private Handler mHandler;
    private Handler iHandler;  // image saving handler
    final private HandlerThread iHandlerThread=
            new HandlerThread(getClass().getSimpleName(),
                    android.os.Process.THREAD_PRIORITY_BACKGROUND);

    private WindowManager wmgr;
    private MediaProjectionManager mgr;
    private int resultMediaProjCode;
    private Intent resultMediaProjData;
    private MediaProjection projection;
    private VirtualDisplay vdisplay;
    private ImageTransmogrifier it;

    public static ScreenshotService getInstance()
    {
        return instance;
    }

    public boolean isRunning() {
        return ServiceHelper.isMyServiceRunning(ScreenshotService.class);
    }

    public static void updateServiceValues(){
        if (getInstance() != null){
            Log.d(LOG_TAG, "Try update service values");
            getInstance().updateValues();
        } else {
            Log.d(LOG_TAG, "service getInstance in null");
        }
    }

    private void updateValues() {
        delay_seconds = AppPreferences.getDelay();
        max_count = AppPreferences.getMaxFilesCount();
        store_to_sd = AppPreferences.getStoreSD();
    }

    public void stop() {
        context.stopService(new Intent(context, ScreenshotService.class));
        Log.d(LOG_TAG, "stop");
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

    WindowManager getWindowManager() {
        return(wmgr);
    }

    Handler getImageSavingHandler() {
        Log.i(LOG_TAG, "Get handler");
        return(iHandler);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        context = AppApplication.getAppContext();
        instance = this;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(LOG_TAG, "onStartCommand");
        updateValues();

        Bundle extras = intent.getExtras();
        if (extras != null) {
            Log.d(LOG_TAG, "Get result media proj from intent");
            resultMediaProjCode = intent.getIntExtra(EXTRA_RESULT_CODE, 1337);
            resultMediaProjData = intent.getParcelableExtra(EXTRA_RESULT_INTENT);

            Log.d(LOG_TAG, "Try get media projection service");
            mgr=(MediaProjectionManager)getSystemService(MEDIA_PROJECTION_SERVICE);
            wmgr=(WindowManager)getSystemService(WINDOW_SERVICE);


            if (!isExternalStorageAvailable() || isExternalStorageReadOnly()) {
                store_to_sd = false;
                Log.d(LOG_TAG, "SD not mounted or read only");
            }

            iHandlerThread.start();
            iHandler=new Handler(iHandlerThread.getLooper());
            mHandler = new Handler();
            mMonitorRunnable = new MonitorRunnable();
            mHandler.post(mMonitorRunnable);

        }
        else {
            Intent activity_intent = new Intent(getApplicationContext(), ProjectionActivity.class);
            activity_intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            activity_intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            activity_intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
            getApplicationContext().startActivity(activity_intent);
        }

        startNotification();

        Log.d(LOG_TAG, "start service result: isServiceRunning = "
                + ServiceHelper.isMyServiceRunning(ScreenshotService.class));

        if (extras == null) stopSelf();

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

    private void startNotification(){
        NotificationManager mgr=
                (NotificationManager)getSystemService(NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.O &&
                mgr.getNotificationChannel(ScreenshotService.NOTIFICATION_CHANNEL_ID)==null) {
            mgr.createNotificationChannel(
                    new NotificationChannel(ScreenshotService.NOTIFICATION_CHANNEL_ID,
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
                Log.d(LOG_TAG, "try start screen capture");
                startCapture();
            } else {
                Log.d(LOG_TAG, "service standby mode");
                mHandler.postDelayed(mMonitorRunnable, delay_seconds * 1000);
            }
        }
    }

    private void startCapture() {
        projection=mgr.getMediaProjection(resultMediaProjCode, resultMediaProjData);
        it = new ImageTransmogrifier(this);

        final MediaProjection.Callback cb=new MediaProjection.Callback() {
            @Override
            public void onStop() {
                vdisplay.release();
            }
        };

        vdisplay=projection.createVirtualDisplay("andshooter",
                it.getWidth(), it.getHeight(),
                getResources().getDisplayMetrics().densityDpi,
                VIRT_DISPLAY_FLAGS, it.getSurface(), null, iHandler);

        projection.registerCallback(cb, iHandler);
        mHandler.postDelayed(mMonitorRunnable,  1000 * delay_seconds);
    }

    public void saveFile(final byte[] png){
        new Thread() {
            @Override
            public void run() {
                Date dateNow = new Date();
                SimpleDateFormat formatForDateNow = new SimpleDateFormat("yyMMddHHmmss");
                String today = formatForDateNow.format(dateNow);
                String filename = today + ".png";

                FileOutputStream fos = null;
                try {
                    if(store_to_sd){
                        File fileOnSd = new File(getExternalFilesDir(null), filename);
                        fos = new FileOutputStream(fileOnSd);
                    }else {
                        fos = openFileOutput(filename, MODE_PRIVATE);
                    }
                    fos.write(png);
                    fos.flush();
                    fos.getFD().sync();
                    fos.close();
                }
                catch(IOException ex) {
                    Log.d(LOG_TAG, ex.toString() );
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
        }.start();
        stopCapture();
    }

    private void stopCapture() {
        if (projection!=null) {
            projection.stop();
            vdisplay.release();
            projection=null;
        }
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
                if (fileToDel.exists()) {
                    if (fileToDel.delete()) {
                        System.out.println("file Deleted :" + fileToDel.getPath());
                    } else {
                        System.out.println("file not Deleted :" + fileToDel.getPath());
                    }
                }
            }
        }
        Log.d( LOG_TAG, "After clearing " + filesDir.listFiles().length );
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

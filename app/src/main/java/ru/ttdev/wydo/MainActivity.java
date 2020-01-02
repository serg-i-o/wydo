package ru.ttdev.wydo;

import androidx.appcompat.app.AppCompatActivity;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;

import static ru.ttdev.wydo.ScreenshotService.NOTIFICATION_CHANNEL_ID;

public class MainActivity extends AppCompatActivity {
    private static final String LOG_TAG = "MainActivity";

    private TextView serviceStatusTextView;
    private Button startButton;
    private Button stopButton;
    private CheckBox autoStartCheckBox;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        serviceStatusTextView = (TextView) findViewById(R.id.service_status);
        startButton = (Button) findViewById(R.id.start_service_btn);
        stopButton = (Button) findViewById(R.id.stop_service_btn);

        update_service_view();
//        boolean service_status = ServiceHelper.isMyServiceRunning(ScreenshotService.class);
//        if (service_status) {
//            serviceStatusTextView.setText(R.string.service_status_running);
//            startButton.setEnabled(false);
//            stopButton.setEnabled(true);
//        } else {
//            serviceStatusTextView.setText(R.string.service_status_stopped);
//            startButton.setEnabled(true);
//            stopButton.setEnabled(false);
//        }

        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(LOG_TAG, "Start button clicked");
                ScreenshotService.checkAndStartService();
//                startButton.setEnabled(false);
//                stopButton.setEnabled(true);
                check_notification_settings();
                update_service_view();
            }
        });

        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(LOG_TAG, "Stop button clicked");
                ScreenshotService.checkAndStopService();
//                startButton.setEnabled(true);
//                stopButton.setEnabled(false);
                update_service_view();
            }
        });
    }

    private void check_notification_settings(){
        NotificationManager mgr=
                (NotificationManager)getSystemService(NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.O &&
                mgr.getNotificationChannel(NOTIFICATION_CHANNEL_ID) != null) {
            Intent notification_settings_intent = new Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName())
                    .putExtra(Settings.EXTRA_CHANNEL_ID, NOTIFICATION_CHANNEL_ID);
            startActivity(notification_settings_intent);
        }
    }

    private void update_service_view(){
        boolean service_status = ServiceHelper.isMyServiceRunning(ScreenshotService.class);
        if (service_status) {
            serviceStatusTextView.setText(R.string.service_status_running);
            startButton.setEnabled(false);
            stopButton.setEnabled(true);
        } else {
            serviceStatusTextView.setText(R.string.service_status_stopped);
            startButton.setEnabled(true);
            stopButton.setEnabled(false);
        }
    }

    
//    View.OnClickListener startButtonList = new View.OnClickListener() {
//        @Override
//        public void onClick(View v) {
//
//        }
//    };
}

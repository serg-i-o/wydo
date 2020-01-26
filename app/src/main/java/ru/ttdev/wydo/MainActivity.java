package ru.ttdev.wydo;

import androidx.appcompat.app.AppCompatActivity;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import org.w3c.dom.Text;

import java.util.Arrays;

import static ru.ttdev.wydo.ScreenshotService.NOTIFICATION_CHANNEL_ID;

public class MainActivity extends AppCompatActivity {
    private static final String LOG_TAG = "MainActivity";

    private TextView serviceStatusTextView;
    private Button startButton;
    private Button stopButton;
    private CheckBox autoStartCheckBox;
    private CheckBox storeOnSD;
    private Spinner secondsSpinner;
    private Spinner maxFilesSpinner;

    private static String[] filesCountValues = {"10", "100", "300", "500", "1000", "2000"};
    private static String[] secondsDelayValues = {"1", "2", "3", "4", "5", "6", "7", "8", "9", "10"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        serviceStatusTextView = (TextView) findViewById(R.id.service_status);
        startButton = (Button) findViewById(R.id.start_service_btn);
        stopButton = (Button) findViewById(R.id.stop_service_btn);
        autoStartCheckBox = (CheckBox) findViewById( R.id.autostart_checkBox );
        secondsSpinner = ( Spinner ) findViewById(R.id.seconds_spinner);
        maxFilesSpinner = (Spinner) findViewById(R.id.maxFiles_spinner);
        storeOnSD = (CheckBox) findViewById( R.id.storeOnSD_checkBox );

        {
            ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, filesCountValues);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            maxFilesSpinner.setAdapter(adapter);
        }

        {
            ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, secondsDelayValues);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            secondsSpinner.setAdapter(adapter);
        }

        secondsSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                AppPreferences.setDelay( Integer.parseInt( secondsSpinner.getSelectedItem().toString() ));
                ScreenshotService.updateValues();
                update_service_view();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        maxFilesSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                AppPreferences.setMaxFilesCount( Integer.parseInt( maxFilesSpinner.getSelectedItem().toString()) );
                update_service_view();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        storeOnSD.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                AppPreferences.setStoreSD(b);
                update_service_view();
            }
        });

        autoStartCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                AppPreferences.setAutoStart(b);
                update_service_view();
            }
        });

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
                //check_notification_settings();
                update_service_view();
                // TODO: вернуть к жизни notifications
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

        Integer delay_seconds = AppPreferences.getDelay();
        Integer max_count = AppPreferences.getMaxFilesCount();
        boolean store_to_sd = AppPreferences.getStoreSD();
        boolean autostart = AppPreferences.getAutoStart();

        storeOnSD.setChecked(store_to_sd);
        autoStartCheckBox.setChecked(autostart);

        int id = Arrays.asList(filesCountValues).indexOf( max_count.toString() );
        maxFilesSpinner.setSelection( id == -1 ? 0 : id );

        id = Arrays.asList(secondsDelayValues).indexOf( delay_seconds.toString() );
        secondsSpinner.setSelection( id == -1 ? 0 : id );
    }

    
//    View.OnClickListener startButtonList = new View.OnClickListener() {
//        @Override
//        public void onClick(View v) {
//
//        }
//    };
}

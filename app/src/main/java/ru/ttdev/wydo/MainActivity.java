package ru.ttdev.wydo;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
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
import android.widget.Toast;

import org.w3c.dom.Text;

import java.util.Arrays;

import static ru.ttdev.wydo.ScreenshotService.NOTIFICATION_CHANNEL_ID;

public class MainActivity extends AppCompatActivity {
    private static final String LOG_TAG = "MainActivity";
    private static final int PERMISSION_REQUEST_CODE = 100;

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
                Log.d(LOG_TAG, String.format("StoreSd checkbox click result = %b", b));
                Log.d(LOG_TAG, String.format("Stored SD preference = %b", AppPreferences.getStoreSD()));
                checkAndUpdateSoreSdView();
            }
        });

        autoStartCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                AppPreferences.setAutoStart(b);
                update_service_view();
            }
        });

        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(LOG_TAG, "Start button clicked");
                ScreenshotService.checkAndStartService();
                update_service_view();
                // TODO: вернуть к жизни notifications
                //check_notification_settings();
            }
        });

        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(LOG_TAG, "Stop button clicked");
                ScreenshotService.checkAndStopService();
                update_service_view();
            }
        });

        update_service_view();
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
        boolean autostart = AppPreferences.getAutoStart();

        checkAndUpdateSoreSdView();
        autoStartCheckBox.setChecked(autostart);

        int id = Arrays.asList(filesCountValues).indexOf( max_count.toString() );
        maxFilesSpinner.setSelection( id == -1 ? 0 : id );

        id = Arrays.asList(secondsDelayValues).indexOf( delay_seconds.toString() );
        secondsSpinner.setSelection( id == -1 ? 0 : id );
    }

    private void checkAndUpdateSoreSdView(){
        boolean store_to_sd = AppPreferences.getStoreSD();
        boolean sd_available = ScreenshotService.isExternalStorageAvailable();
        boolean sd_writable = !ScreenshotService.isExternalStorageReadOnly();
        Log.d(LOG_TAG, String.format("store_to_sd=%b, sd_available=%b, sd_writable=%b", store_to_sd, sd_available, sd_writable));

        if((store_to_sd && !sd_available) || (store_to_sd && !sd_writable)){
            AppPreferences.setStoreSD(false);
            storeOnSD.setChecked(false);
            storeOnSD.setEnabled(false);
            Log.d(LOG_TAG, "External storage is unavailable or read-only");
            Toast.makeText(MainActivity.this, "External storage is unavailable or read-only.", Toast.LENGTH_LONG).show();
            return;
        }

        boolean ext_storage_permission = checkPermission();
        Log.d(LOG_TAG, String.format("ext_storage_permission=%b", ext_storage_permission));
        if(store_to_sd && !ext_storage_permission){
            AppPreferences.setStoreSD(false);
            requestPermission();
        }

        storeOnSD.setChecked(store_to_sd);
    }

    private boolean checkPermission() {
        int result = ContextCompat.checkSelfPermission(
                MainActivity.this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE
        );
        return result == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermission() {
        Log.d(LOG_TAG, "Try to enable ext sorage permissions");
        if (ActivityCompat
                .shouldShowRequestPermissionRationale(
                        MainActivity.this,
                        android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
        ) {
            Toast.makeText(MainActivity.this, "Write External Storage permission allows us to save files. Please allow this permission in App Settings.", Toast.LENGTH_LONG).show();
        } else {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.e("value", "Permission Granted, Now you can use local drive .");
                AppPreferences.setStoreSD(true);
                checkAndUpdateSoreSdView();
            } else {
                Log.e("value", "Permission Denied, You cannot use local drive .");
                AppPreferences.setStoreSD(false);
                checkAndUpdateSoreSdView();
            }
            break;
        }
    }
    
//    View.OnClickListener startButtonList = new View.OnClickListener() {
//        @Override
//        public void onClick(View v) {
//
//        }
//    };
}

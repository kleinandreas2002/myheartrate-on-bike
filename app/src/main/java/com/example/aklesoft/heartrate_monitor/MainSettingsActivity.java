package com.example.aklesoft.heartrate_monitor;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.ParcelUuid;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Switch;
import android.bluetooth.le.ScanFilter;
import android.widget.TextView;
import android.widget.Toast;


import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.example.aklesoft.heartrate_monitor.Constants.ACTION_BROADCAST_RECEIVER;
import static com.example.aklesoft.heartrate_monitor.Constants.ACTION_BROADCAST_RECEIVER_DATA;
import static com.example.aklesoft.heartrate_monitor.Constants.CHARACTERISTIC_ECHO_STRING;
import static com.example.aklesoft.heartrate_monitor.Constants.CLIENT_CHARACTERISTIC_CONFIG;
import static com.example.aklesoft.heartrate_monitor.Constants.PERMISSION_REQUEST_FINE_LOCATION;
import static com.example.aklesoft.heartrate_monitor.Constants.SCAN_PERIOD;



public class MainSettingsActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener {
    public final static String TAG = MainActivity.class.getSimpleName();


    //  UUIDs
    static final UUID HEART_RATE_SERVICE_UUID = convertFromInteger(0x180D);
    static final UUID HEART_RATE_MEASUREMENT_CHAR_UUID = convertFromInteger(0x2A37);

    public static UUID convertFromInteger(long i) {
        final long MSB = 0x0000000000001000L;
        final long LSB = 0x800000805f9b34fbL;
        return new UUID(MSB | (i << 32), LSB);
    }

    //  Bluetooth
    private BluetoothAdapter mBluetoothAdapter;
    private List<ScanFilter> filters;


    //  Save settings
    SharedPreferences pref;
    SharedPreferences.Editor editor;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_settings);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER);

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_REQUEST_FINE_LOCATION);
            }
//            return;
        }

        // INIT layout items
//         initSwipeListener(this);
        initSwitched();
        initTextViews();
        setupOrientationSpinner();


        filters = new ArrayList<>();
        filters.add(new ScanFilter.Builder().setServiceUuid(new ParcelUuid(HEART_RATE_SERVICE_UUID)).build());
        filters.add(new ScanFilter.Builder().setServiceUuid(new ParcelUuid(HEART_RATE_MEASUREMENT_CHAR_UUID)).build());

        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        if (bluetoothManager == null) throw new AssertionError("Object cannot be null");
        mBluetoothAdapter = bluetoothManager.getAdapter();

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "BLE Not Supported",
                    Toast.LENGTH_SHORT).show();
//            finish();
        }
        else{
            if(mBluetoothAdapter.isEnabled()){
                setTextFieldTexts(m_BtStatus, getResources().getString(R.string.bluetooth_enabled));
            }
        }

        m_StopwatchStartAuto = findViewById(R.id.cbStartStopwatch);
        m_ReloadBtImage = findViewById(R.id.imageBtRefresh);


        Log.d(TAG, "onCreate -> BLBALBALBLABLALBLALBLABLALBLABLBLALBLBALBLA");

        //  prepare shared data
        pref = getSharedPreferences("Heartrate_Monitor", 0);
        m_SpeedometerSwitch.setChecked(pref.getBoolean("ShowSpeed", false));
        m_HeartrateSwitch.setChecked(pref.getBoolean("ShowHR", false));
        m_ClockSwitch.setChecked(pref.getBoolean("ShowClock", false));
        m_StopwatchSwitch.setChecked(pref.getBoolean("ShowStopwatch", false));
        m_StopwatchStartAuto.setChecked(pref.getBoolean("StartStopwatch", false));
        m_OrientationSpinner.setSelection(pref.getInt("BlackModeOrientation", 0));
        Log.d(TAG, "onCreate -> SharedPreferences -> selectedRadioButtonID:"+pref.getAll());


        editor = pref.edit();
        editor.apply();


    }

    @Override
    public void onStop() {
        super.onStop();

        editor = pref.edit();
        editor.putBoolean("ShowSpeed", m_SpeedometerSwitch.isChecked());
        editor.putBoolean("ShowHR", m_HeartrateSwitch.isChecked());
        editor.putBoolean("ShowClock", m_ClockSwitch.isChecked());
        editor.putBoolean("ShowStopwatch", m_StopwatchSwitch.isChecked());
        editor.putBoolean("StartStopwatch", m_StopwatchStartAuto.isChecked());
        editor.putInt("BlackModeOrientation", m_OrientationSpinner.getSelectedItemPosition());

// ignore the warning, because editor.apply() doesn't give me the correct preferences back on next application start
        editor.commit();
        Log.d(TAG, "onStop -> SharedPreferences -> pref.getAll():"+pref.getAll());
    }

    public void onClickStartBlackMode(View v)
    {
        startBlackMode();
    }

    public void onClickReloadBt(View v)
    {
        m_ReloadBtImage.setImageResource(R.drawable.ic_baseline_refresh_24px);
        m_ReloadBtImage.setImageResource(R.drawable.ic_baseline_clear_24px);
    }


    // NOTE: Spinner selection callbacks

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int pos, long id)
    {
        /*
        Get the imageView next to the spinner to update the image accordingly to the selected value
        of the spinner itself.
         */

        String selectedValue = parent.getItemAtPosition(pos).toString();
        ImageView img = findViewById(R.id.imageViewOrientationSettings);

        if (selectedValue.equals(getResources().getString(R.string.rbHorizontalOrientation)))
        {
            img.setImageResource(R.drawable.ic_baseline_screen_lock_landscape_24px_w);
        }
        else if (selectedValue.equals(getString(R.string.rbVertivalOrientation)))
        {
            img.setImageResource(R.drawable.ic_baseline_screen_lock_portrait_24px_w);
        }
        else if (selectedValue.equals(getString(R.string.rbRotationOrientation)))
        {
            img.setImageResource(R.drawable.ic_baseline_screen_rotation_24px_w);
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView)
    {

    }

    ////////////////////////////////////////
    //  updates the UI stuff
    public void setTextFieldTexts(TextView textview, String string) {
        runOnUiThread(() -> textview.setText(string));
    }

    // ---------------------------------------------------------------------------------------------
    // PRIVATE


//    private void initSwipeListener(Context _Context)
//    {
//        ConstraintLayout mainView = findViewById(R.id.activityMainSettings);
//
//        mainView.setOnTouchListener(new OnSwipeTouchListener(_Context)
//        {
//            public void onSwipeTop()
//            {
//                Log.d("", "onSwipeTop: ");
//            }
//
//            public void onSwipeRight()
//            {
//                finish();
//            }
//
//            public void onSwipeLeft()
//            {
//                startBlackMode();
//            }
//
//            public void onSwipeBottom()
//            {
//                Log.d("", "onSwipeBottom: ");
//            }
//        });
//    }

    private void initSwitched()
    {
        m_ClockSwitch = findViewById(R.id.switchClock);
        m_StopwatchSwitch = findViewById(R.id.switchStopwatch);
        m_SpeedometerSwitch = findViewById(R.id.switchSpeedometer);
        m_HeartrateSwitch = findViewById(R.id.switchHeartrate);
    }

    private void initTextViews()
    {
        m_BtDevice = findViewById(R.id.textViewBtDevice);
        m_BtStatus = findViewById(R.id.textViewBtStatus);
        m_BtData = findViewById(R.id.textViewBtData);
        m_GpsStatus = findViewById(R.id.textViewGps);

    }

    private void setupOrientationSpinner()
    {
        m_OrientationSpinner = findViewById(R.id.spinnerOrientation);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.blackmode_orienntations, R.layout.support_simple_spinner_dropdown_item);
        adapter.setDropDownViewResource(R.layout.support_simple_spinner_dropdown_item);

        m_OrientationSpinner.setAdapter(adapter);
        m_OrientationSpinner.setOnItemSelectedListener(this);
    }

    private void startBlackMode()
    {
        Intent intentToStartBlackMode = new Intent(getApplicationContext(), BlackMode.class);

        intentToStartBlackMode.putExtra("ShowSpeed", m_SpeedometerSwitch.isChecked());
        intentToStartBlackMode.putExtra("ShowHR", m_HeartrateSwitch.isChecked());
        intentToStartBlackMode.putExtra("ShowStopwatch", m_StopwatchSwitch.isChecked());
        intentToStartBlackMode.putExtra("ShowClock", m_ClockSwitch.isChecked());
        intentToStartBlackMode.putExtra("StartStopwatch", m_StopwatchStartAuto.isChecked());
        intentToStartBlackMode.putExtra("BlackModeOrientation", m_OrientationSpinner.getSelectedItem().toString());

        startActivity(intentToStartBlackMode);
    }

    // ---------------------------------------------------------------------------------------------

    private CheckBox m_StopwatchStartAuto;
    private Spinner m_OrientationSpinner;
    private Switch m_ClockSwitch;
    private Switch m_StopwatchSwitch;
    private Switch m_SpeedometerSwitch;
    private Switch m_HeartrateSwitch;
    private ImageView m_ReloadBtImage;
    private TextView m_BtDevice;
    private TextView m_BtStatus;
    private TextView m_BtData;
    private TextView m_GpsStatus;

}

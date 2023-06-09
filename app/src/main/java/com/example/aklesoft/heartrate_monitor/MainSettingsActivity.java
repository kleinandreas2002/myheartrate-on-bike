package com.example.aklesoft.heartrate_monitor;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.ParcelUuid;
import android.preference.PreferenceManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import org.osmdroid.config.Configuration;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import static com.example.aklesoft.heartrate_monitor.Constants.ACTION_BROADCAST_RECEIVER;
import static com.example.aklesoft.heartrate_monitor.Constants.ACTION_BROADCAST_RECEIVER_DATA;
import static com.example.aklesoft.heartrate_monitor.Constants.CHARACTERISTIC_ECHO_STRING;
import static com.example.aklesoft.heartrate_monitor.Constants.CLIENT_CHARACTERISTIC_CONFIG;
import static com.example.aklesoft.heartrate_monitor.Constants.PERMISSION_REQUEST_FINE_LOCATION;
import static com.example.aklesoft.heartrate_monitor.Constants.PERMISSION_REQUEST_READ_EXTERNAL_STORAGE;
import static com.example.aklesoft.heartrate_monitor.Constants.REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS;
import static com.example.aklesoft.heartrate_monitor.Constants.SCAN_PERIOD;



public class MainSettingsActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener {
    public final static String TAG = MainSettingsActivity.class.getSimpleName();

    public static Context context;

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
    private Boolean connected_and_send_data = false;

    // Device List
    ArrayList<BluetoothDevice> arrayDevices = new ArrayList<BluetoothDevice>();;
    List<String> listArrayDevices = new ArrayList<String>();;
    ArrayAdapter<String> adapterDevice;

    // Maps List
    ArrayList<String> arrayMaps;
    ArrayAdapter<String> adapterMaps;
    boolean bKmlFileFound = false;

    //  Save settings
    SharedPreferences pref;
    SharedPreferences.Editor editor;


    // ---------------------------------------------------------------------------------------------
    void checkPermissions() {
        List<String> permissions = new ArrayList<>();
        String message = "Application permissions:";
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
            message += "\nLocation to show user location.";
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            message += "\nStorage access to store map tiles.";
        }
        if (!permissions.isEmpty()) {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
            String[] params = permissions.toArray(new String[permissions.size()]);
            ActivityCompat.requestPermissions(this, params, REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS);
        } // else: We already have permissions, so handle as normal
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS: {
                Map<String, Integer> perms = new HashMap<>();
                // Initial
                perms.put(Manifest.permission.WRITE_EXTERNAL_STORAGE, PackageManager.PERMISSION_GRANTED);
                // Fill with results
                for (int i = 0; i < permissions.length; i++)
                    perms.put(permissions[i], grantResults[i]);
                // Check for WRITE_EXTERNAL_STORAGE
                Boolean storage = perms.get(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
                if (!storage) {
                    // Permission Denied
                    Toast.makeText(this, "Storage permission is required to store map tiles to reduce data usage and for offline usage.", Toast.LENGTH_LONG).show();
                } // else: permission was granted, yay!
            }
        }
    }

    // ---------------------------------------------------------------------------------------------
// Life cycle
    @SuppressLint("SourceLockedOrientationActivity")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate -> BLBALBALBLABLALBLALBLABLALBLABLBLALBLBALBLA");

        context = getApplicationContext();

        checkPermissions();

        Configuration.getInstance().load(context, PreferenceManager.getDefaultSharedPreferences(context));

        setContentView(R.layout.activity_main_settings);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

//        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
//            // TODO: Consider calling
//            //    ActivityCompat#requestPermissions
//            // here to request the missing permissions, and then overriding
//            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
//            //                                          int[] grantResults)
//            // to handle the case where the user grants the permission. See the documentation
//            // for ActivityCompat#requestPermissions for more details.
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_REQUEST_FINE_LOCATION);
//            }
////            return;
//        }
//
//        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_REQUEST_READ_EXTERNAL_STORAGE);
//            }
//        }


        // INIT layout items
//         initSwipeListener(this);
        initSwitched();
        initSpinner();
        initTextViews();
        initButtons();
        setupOrientationSpinner();

        m_StopwatchStartAuto = findViewById(R.id.cbStartStopwatch);
        m_ImageReloadBt = findViewById(R.id.imageBtRefresh);
        m_ImageMapColorMode = findViewById(R.id.imageMapColorMode);
        m_ImageMapOfflineMode = findViewById(R.id.imageMapOfflineMode);
        m_ImageMapRouteDirection = findViewById(R.id.imageMapDirectionArrows);

        if(!connected_and_send_data) {
            filters = new ArrayList<>();
            filters.add(new ScanFilter.Builder().setServiceUuid(new ParcelUuid(HEART_RATE_SERVICE_UUID)).build());
            filters.add(new ScanFilter.Builder().setServiceUuid(new ParcelUuid(HEART_RATE_MEASUREMENT_CHAR_UUID)).build());

            final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (bluetoothManager == null) throw new AssertionError("Object cannot be null");
            mBluetoothAdapter = bluetoothManager.getAdapter();

            if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
                Toast.makeText(this, getResources().getString(R.string.ble_not_supported),
                        Toast.LENGTH_LONG).show();
                //            finish();
            }

            if (mBluetoothAdapter != null) {
                for (BluetoothDevice alreadyConnectedDevice : bluetoothManager.getConnectedDevices(BluetoothProfile.GATT)) {
                    Log.e(TAG, "onCreate -> getConnectedDevices -> " + alreadyConnectedDevice.getName());
                    arrayDevices.add(alreadyConnectedDevice);
                }

                for (BluetoothDevice bt : arrayDevices)
                    listArrayDevices.add(bt.getName());

                adapterDevice = new ArrayAdapter<>(this,
                        android.R.layout.simple_spinner_dropdown_item,
                        listArrayDevices
                );
                adapterDevice.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                m_SpinnerDevice.setAdapter(adapterDevice);
            }
        }
        else {
            m_ImageBtIcon.setImageResource(R.drawable.ic_baseline_bluetooth_connected_24px);
            setTextFieldTexts(m_BtStatus, getResources().getString(R.string.device_send_data));

        }

        arrayMaps = new ArrayList<>();
        File sdcard = Environment.getExternalStorageDirectory();
        File path = new File(sdcard.getAbsolutePath()+File.separator+"kml"+File.separator);
        if (!path.isDirectory()) {
            path.mkdir();
        }

        if (path.exists()) {
            Log.d(TAG, "onCreate -> path.exists() -> " + path.exists());

            File[] list = path.listFiles();

            for (File file : list) {
                if (file.getName().endsWith(".kml")) {
                    arrayMaps.add(file.getAbsoluteFile().toString());
                    bKmlFileFound = true;
                    Log.d(TAG, "onCreate -> set bKmlFileFound-> " + bKmlFileFound);

                }
            }
            if (arrayMaps.isEmpty()) {
                arrayMaps.add("Please add *kml file to SDCard/kml");
            }
            Collections.sort(arrayMaps);

            adapterMaps = new ArrayAdapter<>(this,
                    android.R.layout.simple_spinner_dropdown_item,
                    arrayMaps
            );
            adapterMaps.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            m_SpinnerMaps.setAdapter(adapterMaps);
        }


        //  prepare shared data
        pref = getSharedPreferences("Heartrate_Monitor", 0);
        connected_and_send_data = pref.getBoolean("connected_and_send_data", false);
        m_SpeedometerSwitch.setChecked(pref.getBoolean("ShowSpeed", false));
        m_NavigatorSwitch.setChecked(pref.getBoolean("ShowNavigator", false));
        m_HeartrateSwitch.setChecked(pref.getBoolean("ShowHR", false));
        m_ClockSwitch.setChecked(pref.getBoolean("ShowClock", false));
        m_StopwatchSwitch.setChecked(pref.getBoolean("ShowStopwatch", false));
        m_StopwatchStartAuto.setChecked(pref.getBoolean("StartStopwatch", false));
        m_OrientationSpinner.setSelection(pref.getInt("BlackModeOrientation", 0));
        m_MapColorMode.setChecked(pref.getBoolean("MapColorMode", false));
        m_MapOfflineMode.setChecked(pref.getBoolean("MapOfflineMode", false));
        m_MapDirectionArrows.setChecked(pref.getBoolean("MapDirectionArrows", false));

        if (mBluetoothAdapter != null) {
            if (adapterDevice.getPosition(pref.getString("LastDevice", "N/A")) != 0) {
                m_SpinnerDevice.setSelection(adapterDevice.getPosition(pref.getString("LastDevice", "N/A")));
            }
        }
        else {
            setTextFieldTexts(m_BtStatus, getResources().getString(R.string.ble_not_supported));
            m_ImageReloadBt.setClickable(false);
            m_ImageReloadBt.setImageResource(0);
        }

        Log.d(TAG, "onCreate -> SharedPreferences -> selectedRadioButtonID:" + pref.getAll());


        editor = pref.edit();
        editor.apply();


    }

    @Override
    public void onStop() {
        super.onStop();
        Log.e(TAG, "onStop - > BLBALBALBLABLALBLALBLABLALBLABLBLALBLBALBL");


    }


    @Override
    protected void onResume() {
        super.onResume();
        Log.e(TAG, "onResume - > BLBALBALBLABLALBLALBLABLALBLABLBLALBLBALBL");
        if (mBluetoothAdapter != null) {
            if (mBluetoothAdapter.isEnabled() && !connected_and_send_data) {
                setTextFieldTexts(m_BtStatus, getResources().getString(R.string.bluetooth_enabled));
                m_ImageBtIcon.setImageResource(R.drawable.ic_baseline_bluetooth_enabled_24px);
                m_ImageReloadBt.setImageResource(R.drawable.ic_baseline_refresh_24px);
            }
        }

        if (mHandler == null) {
            mHandler = new Handler();
        }

        if(m_SpeedometerSwitch.isChecked()) {
            LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            if (locationManager == null) throw new AssertionError("Object cannot be null");
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER))
            {
                setTextFieldTexts(m_GpsStatus, getResources().getString(R.string.gps_enabled));
            }
        }

    }

    @Override
    protected void onPause(){
        super.onPause();
        Log.e(TAG, "onPause - > BLBALBALBLABLALBLALBLABLALBLABLBLALBLBALBL");

        if (mBluetoothAdapter != null) {
            if (m_HeartrateSwitch.isChecked()) {
                if ((mLeScanner != null || mBluetoothAdapter != null) && mBluetoothAdapter.isEnabled()) {
                    scanLeDevice(false);
                }
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.e(TAG, "onDestroy - > BLBALBALBLABLALBLALBLABLALBLABLBLALBLBALBL");

        if( m_HeartrateSwitch.isChecked() ) {

            if(connected_and_send_data) {
                connected_and_send_data = false;

                disconnectGattServer(mGatt);
                mGatt = null;

                unregister_receiver();
            }
            else{
                disconnectGattServer(mGatt);
                mGatt = null;

                unregister_receiver();
            }
        }

        Thread[] threads = new Thread[Thread.activeCount()];
        Thread.enumerate(threads);
        for (Thread t : threads) {
            if (t.isAlive()) {
//                Log.d(TAG, "onDestroy -> thread interrupt -> "+t);
                t.interrupt();
            }
        }
        Log.d(TAG, "onDestroy -> threads interrupted -> ");

        editor = pref.edit();
        editor.putBoolean("connected_and_send_data", connected_and_send_data);
        editor.putBoolean("ShowSpeed", m_SpeedometerSwitch.isChecked());
        editor.putBoolean("ShowNavigator", m_NavigatorSwitch.isChecked());
        editor.putBoolean("ShowHR", m_HeartrateSwitch.isChecked());
        editor.putBoolean("ShowClock", m_ClockSwitch.isChecked());
        editor.putBoolean("ShowStopwatch", m_StopwatchSwitch.isChecked());
        editor.putBoolean("StartStopwatch", m_StopwatchStartAuto.isChecked());
        editor.putBoolean("MapColorMode", m_MapColorMode.isChecked());
        editor.putBoolean("MapOfflineMode", m_MapOfflineMode.isChecked());
        editor.putBoolean("MapDirectionArrows", m_MapDirectionArrows.isChecked());

        if (m_SpinnerDevice.getSelectedItem() != null) {
            editor.putString("LastDevice", m_SpinnerDevice.getSelectedItem().toString());
        }
        editor.putInt("BlackModeOrientation", m_OrientationSpinner.getSelectedItemPosition());

// ignore the warning, because editor.apply() doesn't give me the correct preferences back on next application start
        editor.commit();
        Log.d(TAG, "onDestroy -> SharedPreferences -> pref.getAll():" + pref.getAll());

        if(isFinishing() == true) {
            System.exit(0);
        }

    }

    private void unregister_receiver( ) {

        if (flagReceiver == true) {
            try {
                unregisterReceiver(broadcastReceiver);
                broadcastReceiver = null;
                flagReceiver = false;
            } catch (Exception e) {
                Log.d(TAG, "unregister_receiver -> broadcastReceiver wasn't registered!");
            }
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Button methods
    public void onClickStartBlackMode(View v) {
        startBlackMode();
    }

    public void onClickReloadBt(View v) {
        Log.d(TAG, "onClickReloadBt -> ");
        Log.d(TAG, "onClickReloadBt -> "+ connected_and_send_data);
        Log.d(TAG, "onClickReloadBt -> "+ arrayDevices.isEmpty());

        if ( !connected_and_send_data && !arrayDevices.isEmpty() ) {
            Log.d(TAG, "onClickReloadBt -> selected device -> ");

            for (BluetoothDevice device : arrayDevices){
                Log.d(TAG, "onClickReloadBt -> selected device -> " + device.getName());
                Log.d(TAG, "onClickReloadBt -> selected device -> " + m_SpinnerDevice.getSelectedItem().toString());
                if (device.getName().equals(m_SpinnerDevice.getSelectedItem().toString())) {
                    Log.d(TAG, "onClickReloadBt -> selected device -> in if -> " + device.getName());
                    connectToDevice(device);
                }
            }
        }
        else if ( getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE ) && !connected_and_send_data) {
            Log.d(TAG, "onClickReloadBt -> startBTScan -> 1");

            m_ImageReloadBt.setImageResource(R.drawable.ic_baseline_clear_24px);
            startBTScan();
        }
        else if ( connected_and_send_data ) {
            Log.d(TAG, "onClickReloadBt -> disconnect -> ");
            if(!m_HeartrateSwitch.isChecked()) {
                m_HeartrateSwitch.setChecked(true);
            }

            if(m_HeartrateSwitch.isChecked()) {
                connected_and_send_data = false;

                disconnectGattServer(mGatt);
                mGatt = null;

                unregister_receiver();

                m_ImageReloadBt.setImageResource(R.drawable.ic_baseline_refresh_24px);

            }
        }
        else {
            Log.d(TAG, "onClickReloadBt -> startBTScan -> 2");

            m_ImageReloadBt.setImageResource(R.drawable.ic_baseline_clear_24px);
            startBTScan();
        }
    }


    // NOTE: Spinner selection callbacks

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
        /*
        Get the imageView next to the spinner to update the image accordingly to the selected value
        of the spinner itself.
         */

        String selectedValue = parent.getItemAtPosition(pos).toString();
        ImageView img = findViewById(R.id.imageViewOrientationSettings);

        if (selectedValue.equals(getResources().getString(R.string.rbHorizontalOrientation))) {
            img.setImageResource(R.drawable.ic_baseline_screen_lock_landscape_24px_w);
        } else if (selectedValue.equals(getString(R.string.rbVertivalOrientation))) {
            img.setImageResource(R.drawable.ic_baseline_screen_lock_portrait_24px_w);
        } else if (selectedValue.equals(getString(R.string.rbRotationOrientation))) {
            img.setImageResource(R.drawable.ic_baseline_screen_rotation_24px_w);
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {

    }


    // ---------------------------------------------------------------------------------------------
    // PRIVATE


    private void initSwitched() {
        m_ClockSwitch = findViewById(R.id.switchClock);
        m_StopwatchSwitch = findViewById(R.id.switchStopwatch);
        m_SpeedometerSwitch = findViewById(R.id.switchSpeedometer);
        m_NavigatorSwitch = findViewById(R.id.switchNavigator);
        m_NavigatorSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                // do something, the isChecked will be
                // true if the switch is in the On position
                if(isChecked && !m_SpeedometerSwitch.isChecked()){
                    m_SpeedometerSwitch.setChecked(true);
                }
            }
        });
        m_HeartrateSwitch = findViewById(R.id.switchHeartrate);
        m_MapColorMode = findViewById(R.id.switchMapColorMode);
        m_MapColorMode.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    m_ImageMapColorMode.setImageResource(R.drawable.ic_invert_colors_24px);
                } else {
                    m_ImageMapColorMode.setImageResource(R.drawable.ic_invert_colors_off_24px);
                }
            }
        });
        m_MapOfflineMode = findViewById(R.id.switchOfflineMap);
        m_MapOfflineMode.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    m_ImageMapOfflineMode.setImageResource(R.drawable.ic_cloud_off_24px);
                } else {
                    m_ImageMapOfflineMode.setImageResource(R.drawable.ic_cloud_queue_24px);
                }
            }
        });
        m_MapDirectionArrows = findViewById(R.id.switchMapDirectionArrows);
        m_MapDirectionArrows.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    m_ImageMapRouteDirection.setImageResource(R.drawable.ic_route_direction_button_24px);
                } else {
                    m_ImageMapRouteDirection.setImageResource(R.drawable.ic_route_direction_off_button_24px);
                }
            }
        });
    }

    private void initSpinner() {
        m_SpinnerDevice = findViewById(R.id.spinnerDevice);
        m_SpinnerMaps = findViewById(R.id.spinnerMaps);
    }

    private void initTextViews() {
//        m_BtDevice = findViewById(R.id.textViewBtDevice);
        m_BtStatus = findViewById(R.id.textViewBtStatus);
        m_BtData = findViewById(R.id.textViewBtData);
        m_GpsStatus = findViewById(R.id.textViewGps);

    }

    private void initButtons() {
        m_ImageBtIcon = findViewById(R.id.imageBtIcon);

    }

    private void setupOrientationSpinner() {
        m_OrientationSpinner = findViewById(R.id.spinnerOrientation);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.blackmode_orienntations, R.layout.support_simple_spinner_dropdown_item);
        adapter.setDropDownViewResource(R.layout.support_simple_spinner_dropdown_item);

        m_OrientationSpinner.setAdapter(adapter);
        m_OrientationSpinner.setOnItemSelectedListener(this);
    }

    private void startBlackMode() {
        Intent intentToStartBlackMode = new Intent(this.getApplicationContext(), BlackMode.class);

        intentToStartBlackMode.putExtra("ShowSpeed", m_SpeedometerSwitch.isChecked());
        intentToStartBlackMode.putExtra("ShowNavigator", m_NavigatorSwitch.isChecked());
        intentToStartBlackMode.putExtra("ShowHR", m_HeartrateSwitch.isChecked());
        intentToStartBlackMode.putExtra("ShowStopwatch", m_StopwatchSwitch.isChecked());
        intentToStartBlackMode.putExtra("ShowClock", m_ClockSwitch.isChecked());
        intentToStartBlackMode.putExtra("StartStopwatch", m_StopwatchStartAuto.isChecked());
        intentToStartBlackMode.putExtra("MapDirectionArrows", m_MapDirectionArrows.isChecked());
        intentToStartBlackMode.putExtra("MapColorMode", m_MapColorMode.isChecked());
        intentToStartBlackMode.putExtra("MapOfflineMode", m_MapOfflineMode.isChecked());

        intentToStartBlackMode.putExtra("BlackModeOrientation", m_OrientationSpinner.getSelectedItemPosition());

        if (bKmlFileFound) {
            Log.e(TAG, "SelectedMaps ->  " + m_SpinnerMaps.getSelectedItemPosition());

            intentToStartBlackMode.putExtra("SelectedMaps", arrayMaps.get(m_SpinnerMaps.getSelectedItemPosition()));
        }

        startActivity(intentToStartBlackMode);
    }

    // ---------------------------------------------------------------------------------------------
    // BLUETOOTH
    private int REQUEST_ENABLE_BT = 1;
    private BluetoothLeScanner mLeScanner;
    private ScanSettings settings;
    private Handler mHandler;
    private boolean mScanning;
    private boolean flagReceiver = false;
    private GattClientCallback gattClientCallback = null;
    private BluetoothGatt mGatt;


    public void connectToDevice(BluetoothDevice device) {
        Log.e(TAG, "connectToDevice");

        String text = device + " " + getResources().getString(R.string.connecting);
        setTextFieldTexts(m_BtStatus, text);

        if (gattClientCallback == null) {
            gattClientCallback = new GattClientCallback();
        }
        mGatt = device.connectGatt(this, false, gattClientCallback);

        Log.e(TAG, "connectToDevice -> mGatt -> " + mGatt.getDevice());

        if (mGatt.connect()) {
            setTextFieldTexts(m_BtStatus, getResources().getString(R.string.device_connected));
//                setTextFieldTexts(m_BtDevice, device.getName());

            m_ImageReloadBt.setImageResource(R.drawable.ic_baseline_bluetooth_break_24px);

            Log.e(TAG, "connectToDevice -> scanLeDevice -> false");
            scanLeDevice(false);// will stop after first device detection

        } else {
            gattClientCallback = null;
            Log.e(TAG, "connectToDevice -> mGatt.connect() -> false");
        }

    }


    private void startBTScan() {
        Log.e(TAG, "startBTScan -> call");


        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
        else {
            setTextFieldTexts(m_BtData, getResources().getString(R.string.scan_for_devices));
            m_ImageBtIcon.setImageResource(R.drawable.ic_baseline_bluetooth_searching_24px);

            if (Build.VERSION.SDK_INT >= 21) {
                mLeScanner = mBluetoothAdapter.getBluetoothLeScanner();
                settings = new ScanSettings.Builder()
                        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                        .build();
                Log.e(TAG, "startBTScan -> settings %s" + settings);

            }

            Log.e(TAG, "startBTScan -> scanLeDevice");
            scanLeDevice(false);
            scanLeDevice(true);

            mHandler.postDelayed(this::stopScan, SCAN_PERIOD);

            mScanning = true;
            Log.e(TAG, "startBTScan-> Scan started");
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == Activity.RESULT_CANCELED) {
                //Bluetooth not enabled.
                finish();
                return;
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void scanLeDevice(final boolean enable) {
        if (enable) {
            if (Build.VERSION.SDK_INT < 21) {
                mBluetoothAdapter.startLeScan(mLeScanCallback);
            } else {
                Log.e(TAG, "scanLeDevice -> startScan");
                mLeScanner.startScan(filters, settings, mScanCallback);
            }
        } else {
            if (Build.VERSION.SDK_INT < 21) {
                mBluetoothAdapter.stopLeScan(mLeScanCallback);
            } else {
                if(mLeScanner != null) {
                    Log.e(TAG, "scanLeDevice 2 -> stopScan");
                    mLeScanner.stopScan(mScanCallback);
                }
            }
        }
    }

    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            (device, rssi, scanRecord) -> runOnUiThread(() -> {
                Log.d("onLeScan", device.toString());
                connectToDevice(device);
            });

    private void stopScan() {
        Log.e(TAG, "Stopped scanning.");
        if (mScanning && (mLeScanner != null || mBluetoothAdapter != null) && mBluetoothAdapter.isEnabled()) {

            Log.e(TAG, "Stopped scanning -> cancelDiscovery()");
            mBluetoothAdapter.cancelDiscovery();
            Log.e(TAG, "Stopped scanning -> mLeScanner.stopScan(mScanCallback)");
            mLeScanner.stopScan(mScanCallback);

            if( !connected_and_send_data ){
                setTextFieldTexts(m_BtData, getResources().getString(R.string.scan_stopped));
                m_ImageReloadBt.setImageResource(R.drawable.ic_baseline_refresh_24px);
                m_ImageBtIcon.setImageResource(R.drawable.ic_baseline_bluetooth_enabled_24px);
            }
        }

        mScanning = false;
    }

    private ScanCallback mScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            Log.e(TAG, "onScanResult");
            Log.i(TAG, String.valueOf(callbackType));
            Log.e(TAG, "getDevice -> " + result.toString());
            Log.i(TAG, result.toString());
            BluetoothDevice btDevice = result.getDevice();
            Log.e(TAG, "onScanResult -> connectToDevice -> " + result.getDevice());
            if( !arrayDevices.isEmpty() || !(listArrayDevices.contains(result.getDevice().getName())))
            {
                listArrayDevices.add(btDevice.getName());
                arrayDevices.add(btDevice);
                adapterDevice.notifyDataSetChanged();
                setTextFieldTexts(m_BtStatus,getResources().getString(R.string.found_device));
                mLeScanner.stopScan(mScanCallback);
            }

            Log.e(TAG, "onScanResult -> selection equal Scanresult -> " + result.getDevice());
            connectToDevice(btDevice);

        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            for (ScanResult sr : results) {
                Log.i("ScanResult - Results", sr.toString());
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            Log.e("Scan Failed", "Error Code: " + errorCode);
        }
    };

    private void showToastMessage() {
        Toast.makeText(this, "NO paired device in range!", Toast.LENGTH_SHORT).show();
    }

    public IntentFilter broadcastReceiverUpdateIntentFilter() {

        final IntentFilter intentFilter = new IntentFilter();

        intentFilter.addAction(ACTION_BROADCAST_RECEIVER);
        flagReceiver = true;
        return intentFilter;
    }

    BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.e(TAG, "BroadcastReceiver -> onReceive");
// ONLY for DEBUGGING
//            if(intent.getAction().equals(ACTION_BROADCAST_RECEIVER)){
//
//                Log.d(TAG, String.format("ACTION_BROADCAST_RECEIVER_DATA: "+ intent.getStringExtra(ACTION_BROADCAST_RECEIVER_DATA)));
//                int heartRate = Integer.parseInt(intent.getStringExtra(ACTION_BROADCAST_RECEIVER_DATA));
//                setHrData(heartRate);
//
////                intent.putExtra(ACTION_BROADCAST_RECEIVER_DATA, intent.getAction().equals(ACTION_BROADCAST_RECEIVER_DATA));
//            }
        }
    };

    public void disconnectGattServer(BluetoothGatt gatt ) {

        if (gatt != null) {
            Log.e(TAG, "Closing Gatt connection");

            gatt.abortReliableWrite();
            gatt.disconnect();
            try {
                Thread.sleep(100);
            } catch (InterruptedException ex) {
//                        Logger.getLogger(MainActivity.class.getName()).log(Level.SEVERE, null, ex);
            }
            gatt.close();
            setTextFieldTexts(m_BtStatus, getResources().getString(R.string.device_disconnected));
            setTextFieldTexts(m_BtData, "");
            m_ImageBtIcon.setImageResource(R.drawable.ic_baseline_bluetooth_enabled_24px);


        }
    }


    // ---------------------------------------------------------------------------------------------

    private CheckBox m_StopwatchStartAuto;
    private Spinner m_OrientationSpinner;
    private Switch m_ClockSwitch;
    private Switch m_StopwatchSwitch;
    private Switch m_SpeedometerSwitch;
    private Switch m_NavigatorSwitch;
    private Switch m_MapDirectionArrows;
    private Switch m_MapOfflineMode;
    private Switch m_MapColorMode;
    private Switch m_HeartrateSwitch;
    private Spinner m_SpinnerDevice;
    private Spinner m_SpinnerMaps;
    private ImageView m_ImageReloadBt;
    private ImageView m_ImageBtIcon;
    private ImageView m_ImageMapOfflineMode;
    private ImageView m_ImageMapRouteDirection;
    private ImageView m_ImageMapColorMode;
    //    private TextView m_BtDevice;
    private TextView m_BtStatus;
    private TextView m_BtData;
    private TextView m_GpsStatus;



    // ---------------------------------------------------------------------------------------------

    private class GattClientCallback extends BluetoothGattCallback {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            Log.d(TAG, "onConnectionStateChange status: " + status);
            Log.d(TAG, "onConnectionStateChange newState: " + newState);

            String intentAction;
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
//                    intentAction = GATT_CONNECTED;
//                    broadcastUpdate(gatt.getDevice().getAddress(), intentAction);
                    Log.i(TAG, "Connected to GATT server.");
                    // Attempts to discover services after successful connection.
                    Log.i(TAG, "Attempting to start service discovery:" +
                            gatt.discoverServices());

                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
//                    intentAction = ACTION_GATT_DISCONNECTED;
                    Log.i(TAG, "Disconnected from GATT server.");
                    close(gatt.getDevice().getAddress());
//                    broadcastUpdate(gatt.getDevice().getAddress(), intentAction);
                }
            } else {

                if (gatt.getDevice() != null) {

                    disconnect_by_address(gatt.getDevice().getAddress());
                    close(gatt.getDevice().getAddress());

                    Log.i(TAG, "Gatt server error");
//                    broadcastUpdate(gatt.getDevice().getAddress(), ACTION_GATT_SERVICES_ERROR);
                }
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);

            if (status != BluetoothGatt.GATT_SUCCESS) {
                Log.e(TAG, "Device service discovery unsuccessful, status " + status);
                return;
            }

            Log.d(TAG, "onServicesDiscovered ->" + gatt);
            List<BluetoothGattCharacteristic> matchingCharacteristics = BluetoothUtils.findCharacteristics(gatt);
            if (matchingCharacteristics.isEmpty()) {
                Log.e(TAG, "Unable to find characteristics.");
                return;
            }

            Log.d(TAG, "Initializing: setting write type and enabling notification");
            for (BluetoothGattCharacteristic characteristic : matchingCharacteristics) {
                characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
                enableCharacteristicNotification(gatt, characteristic);
                readCharacteristic(characteristic);

            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "Characteristic written successfully");

            } else {
                Log.e(TAG, "Characteristic write unsuccessful, status: " + status);
                disconnectGattServer(gatt);
                mGatt = null;
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "Characteristic read successfully");
                readCharacteristic(characteristic);
            } else {
                Log.e(TAG, "Characteristic read unsuccessful, status: " + status);
                // Trying to read from the Time Characteristic? It doesnt have the property or permissions
                // set to allow this. Normally this would be an error and you would want to:
                // disconnectGattServer();
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            Log.d(TAG, "Characteristic changed, " + characteristic.getUuid().toString());
            readCharacteristic(characteristic);
        }


        private void enableCharacteristicNotification(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            boolean characteristicWriteSuccess = gatt.setCharacteristicNotification(characteristic, true);
            if (characteristicWriteSuccess) {
                Log.d(TAG, "Characteristic notification set successfully for " + characteristic.getUuid().toString());

// This is specific to Heart Rate Measurement.
                Log.d(TAG, "CHARACTERISTIC_ECHO_STRING " + CHARACTERISTIC_ECHO_STRING);
                Log.d(TAG, "characteristic.getUuid()   " + characteristic.getUuid());

                if (CHARACTERISTIC_ECHO_STRING.equalsIgnoreCase(characteristic.getUuid().toString())) {
                    BluetoothGattDescriptor descriptor = characteristic.getDescriptor(
                            UUID.fromString(CLIENT_CHARACTERISTIC_CONFIG));
                    if (descriptor != null) {
                        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                        boolean return_value = gatt.writeDescriptor(descriptor);
                        Log.d(TAG, "writeDescriptor -> " + return_value);
                    }

                }
//                if (BluetoothUtils.isEchoCharacteristic(characteristic)) {
//
//                }
            } else {
                Log.e(TAG, "Characteristic notification set failure for " + characteristic.getUuid().toString());
            }
        }

        private void readCharacteristic(BluetoothGattCharacteristic characteristic) {
            byte[] messageBytes = characteristic.getValue();

            try {
                String message = StringUtils.stringFromBytes(messageBytes);
                if (message == null) {
                    Log.e(TAG, "Unable to convert bytes to string");
                    return;
                }
                int flag = characteristic.getProperties();
                int format;
                if ((flag & 0x01) != 0) {
                    format = BluetoothGattCharacteristic.FORMAT_UINT16;
                    Log.d(TAG, "Heart rate format UINT16.");
                } else {
                    format = BluetoothGattCharacteristic.FORMAT_UINT8;
                    Log.d(TAG, "Heart rate format UINT8.");
                }
                final int heartRate = characteristic.getIntValue(format, 1);
                Log.d(TAG, String.format("Received heart rate: %d", heartRate));

//              Log.d(TAG, "Received message: " + message);

                final Intent intent = new Intent(ACTION_BROADCAST_RECEIVER_DATA);
                intent.putExtra(ACTION_BROADCAST_RECEIVER_DATA, String.valueOf(heartRate));
                intent.setAction(ACTION_BROADCAST_RECEIVER);
//                Log.e(TAG, "get null from messageBytes"+ intent.getAction());
                sendBroadcast(intent);
                setHrData(heartRate);
                connected_and_send_data = true;
            } catch (Exception e) {
                Log.d(TAG, "get null from messageBytes");
            }


        }


        private void disconnect_by_address(String address) {

            if (mBluetoothAdapter == null) {
                Log.w(TAG, "BluetoothAdapter not initialized");
                return;
            }

            if (mGatt == null || address == null || address.length() == 0) {
                Log.w(TAG, "No devices or gatts");
                return;
            }


            if (mGatt != null) {
                mGatt.disconnect();
                mGatt = null;
            }
        }


        private void close(String address) {

            if (mBluetoothAdapter == null) {
                Log.w(TAG, "BluetoothAdapter not initialized");
                return;
            }

            if (mGatt == null || address == null || address.length() == 0) {
                Log.w(TAG, "No devices or gatts");
                return;
            }

            try {

                if (mGatt.getDevice() != null && mGatt.getDevice().getAddress().equals(address)) {
                    mGatt.close();
                    mGatt = null;
                }

            } catch (ConcurrentModificationException cmexp) {
                cmexp.printStackTrace();
            }
        }
    }

////////////////////////////////////////
//  updates the UI with heart rate data

    public void setHrData(int hrData) {

        m_ImageBtIcon.setImageResource(R.drawable.ic_baseline_bluetooth_connected_24px);
        setTextFieldTexts(m_BtStatus, getResources().getString(R.string.device_send_data));

        runOnUiThread(() -> m_BtData.setText(String.format(Locale.getDefault(), "%d", hrData)));
    }


    ////////////////////////////////////////
//  updates the UI stuff
    public void setTextFieldTexts(TextView textview, String string) {
        runOnUiThread(() -> textview.setText(string));
    }
}


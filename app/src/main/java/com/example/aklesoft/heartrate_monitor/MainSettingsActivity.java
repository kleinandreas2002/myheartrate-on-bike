package com.example.aklesoft.heartrate_monitor;

import android.Manifest;
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
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Switch;
import android.bluetooth.le.ScanFilter;
import android.widget.TextView;
import android.widget.Toast;


import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
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

// ---------------------------------------------------------------------------------------------
// Life cycle
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_settings);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);

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
        initButtons();
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
        } else {
            if (mBluetoothAdapter.isEnabled()) {
                setTextFieldTexts(m_BtStatus, getResources().getString(R.string.bluetooth_enabled));
                m_ImageBtIcon.setImageResource(R.drawable.ic_baseline_bluetooth_searching_24px);
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
        Log.d(TAG, "onCreate -> SharedPreferences -> selectedRadioButtonID:" + pref.getAll());


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
        Log.d(TAG, "onStop -> SharedPreferences -> pref.getAll():" + pref.getAll());
    }


    @Override
    protected void onResume() {
        super.onResume();
        Log.e(TAG, "onResume - > BLBALBALBLABLALBLALBLABLALBLABLBLALBLBALBL");

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

        if(m_HeartrateSwitch.isChecked()) {
            if(getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
                try {
                    Log.e(TAG, "registerReceiver");
                    registerReceiver(broadcastReceiver, broadcastReceiverUpdateIntentFilter());
                    startScan();
                } catch (Exception e) {
                    Log.e(TAG, "broadcastReceiver isn't registered!");
                }
            }

        }
    }

    @Override
    protected void onPause(){
        super.onPause();

        if(m_HeartrateSwitch.isChecked()) {
            if( (mLeScanner != null || mBluetoothAdapter != null) && mBluetoothAdapter.isEnabled()) {
                scanLeDevice(false);
            }

            try {
                unregisterReceiver(broadcastReceiver);
            }
            catch (Exception e){
                Log.e(TAG, "broadcastReceiver wasn't registered!");
            }


        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if(m_HeartrateSwitch.isChecked()) {
            disconnectGattServer(mGatt);
            mGatt = null;
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

        System.exit(0);

    }

    // ---------------------------------------------------------------------------------------------
    // Button methods
    public void onClickStartBlackMode(View v) {
        startBlackMode();
    }

    public void onClickReloadBt(View v) {
        m_ReloadBtImage.setImageResource(R.drawable.ic_baseline_refresh_24px);
        m_ReloadBtImage.setImageResource(R.drawable.ic_baseline_clear_24px);
        if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            startScan();
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

    private void initSwitched() {
        m_ClockSwitch = findViewById(R.id.switchClock);
        m_StopwatchSwitch = findViewById(R.id.switchStopwatch);
        m_SpeedometerSwitch = findViewById(R.id.switchSpeedometer);
        m_HeartrateSwitch = findViewById(R.id.switchHeartrate);
    }

    private void initTextViews() {
        m_BtDevice = findViewById(R.id.textViewBtDevice);
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
        Intent intentToStartBlackMode = new Intent(getApplicationContext(), BlackMode.class);

        intentToStartBlackMode.putExtra("ShowSpeed", m_SpeedometerSwitch.isChecked());
        intentToStartBlackMode.putExtra("ShowHR", m_HeartrateSwitch.isChecked());
        intentToStartBlackMode.putExtra("ShowStopwatch", m_StopwatchSwitch.isChecked());
        intentToStartBlackMode.putExtra("ShowClock", m_ClockSwitch.isChecked());
        intentToStartBlackMode.putExtra("StartStopwatch", m_StopwatchStartAuto.isChecked());
        intentToStartBlackMode.putExtra("BlackModeOrientation", m_OrientationSpinner.getSelectedItemPosition());

        startActivity(intentToStartBlackMode);
    }

    // ---------------------------------------------------------------------------------------------
    // BLUETOOTH
    private int REQUEST_ENABLE_BT = 1;
    private BluetoothLeScanner mLeScanner;
    private ScanSettings settings;
    private Handler mHandler;
    private boolean mScanning;
    private GattClientCallback gattClientCallback = null;
    private BluetoothGatt mGatt;


    public void connectToDevice(BluetoothDevice device) {

        boolean paired = false;
        for (BluetoothDevice pairedDevice : mBluetoothAdapter.getBondedDevices()) {
            if (pairedDevice.getAddress().equals(device.getAddress())) {
                paired = true;
                break;
            }
        }

        if (paired) {
            String text = device + " " + getResources().getString(R.string.connecting);
            setTextFieldTexts(m_BtStatus, text);

            if (gattClientCallback == null) {
                gattClientCallback = new GattClientCallback();
            }
            mGatt = device.connectGatt(this, false, gattClientCallback);

            Log.e(TAG, "connectToDevice -> mGatt -> " + mGatt.getDevice());

            if (mGatt.connect()) {
                setTextFieldTexts(m_BtStatus, getResources().getString(R.string.device_connected));
                setTextFieldTexts(m_BtDevice, device.getName());

                m_ReloadBtImage.setImageResource(R.drawable.ic_baseline_refresh_24px);

                Log.e(TAG, "connectToDevice -> scanLeDevice -> false");
                scanLeDevice(false);// will stop after first device detection

            } else {
                gattClientCallback = null;
                Log.e(TAG, "connectToDevice -> mGatt.connect() -> false");
            }
        } else {
            showToastMessage();
        }

    }


    private void startScan() {
        Log.e(TAG, "onResume -> Start BT work");

        setTextFieldTexts(m_BtData, getResources().getString(R.string.scan_for_devices));

        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } else {
            if (Build.VERSION.SDK_INT >= 21) {
                mLeScanner = mBluetoothAdapter.getBluetoothLeScanner();
                settings = new ScanSettings.Builder()
                        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                        .build();

            }

            Log.e(TAG, "onResume -> scanLeDevice");
            scanLeDevice(true);

            mHandler.postDelayed(this::stopScan, SCAN_PERIOD);

            mScanning = true;
            Log.e(TAG, "Started scanning.");
        }
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
                Log.e(TAG, "scanLeDevice 2-> stopScan");
                mLeScanner.stopScan(mScanCallback);
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
            setTextFieldTexts(m_BtData, getResources().getString(R.string.scan_stopped));
        }

        mScanning = false;
        m_ReloadBtImage.setImageResource(R.drawable.ic_baseline_refresh_24px);
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

        return intentFilter;
    }

    BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
// ONLY for DEBUGGING
//            if(intent.getAction().equals(ACTION_BROADCAST_RECEIVER)){
//
//                Log.d(TAG, String.format("ACTION_BROADCAST_RECEIVER_DATA: "+ intent.getStringExtra(ACTION_BROADCAST_RECEIVER_DATA)));
//                intent.putExtra(ACTION_BROADCAST_RECEIVER_DATA, intent.getAction().equals(ACTION_BROADCAST_RECEIVER_DATA));
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

        }
    }


    // ---------------------------------------------------------------------------------------------

    private CheckBox m_StopwatchStartAuto;
    private Spinner m_OrientationSpinner;
    private Switch m_ClockSwitch;
    private Switch m_StopwatchSwitch;
    private Switch m_SpeedometerSwitch;
    private Switch m_HeartrateSwitch;
    private ImageView m_ReloadBtImage;
    private ImageView m_ImageBtIcon;
    private TextView m_BtDevice;
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


            if (status == BluetoothGatt.GATT_FAILURE) {
                Log.e(TAG, "Connection Gatt failure status " + status);
                disconnectGattServer(gatt);
                gatt = null;

            } else if (status != BluetoothGatt.GATT_SUCCESS) {
                // handle anything not SUCCESS as failure
                Log.e(TAG, "Connection not GATT sucess status " + status);
                disconnectGattServer(gatt);
                gatt = null;
            }

            if (newState == BluetoothProfile.STATE_CONNECTED) {
                if (gatt == null) throw new AssertionError("Object cannot be null");
                Log.d(TAG, "Connected to device " + gatt.getDevice().getAddress());

                gatt.discoverServices();

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.e(TAG, "Disconnected from device");

                disconnectGattServer(gatt);
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

        public void disconnectGattServer(BluetoothGatt gatt) {

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
                setTextFieldTexts(m_BtDevice, "");

            }
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
            } catch (Exception e) {
                Log.d(TAG, "get null from messageBytes");
            }


        }
    }

////////////////////////////////////////
//  updates the UI with heart rate data
    private boolean m_ChangeImage = true;

    public void setHrData(int hrData) {
        if(m_ChangeImage){
            runOnUiThread(() -> m_ImageBtIcon.setImageResource(R.drawable.ic_baseline_bluetooth_connected_24px));
            m_ChangeImage = false;
        }

        runOnUiThread(() -> m_BtData.setText(String.format(Locale.getDefault(), "%d", hrData)));
    }


    ////////////////////////////////////////
//  updates the UI stuff
    public void setTextFieldTexts(TextView textview, String string) {
        runOnUiThread(() -> textview.setText(string));
    }
}
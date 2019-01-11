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
import android.os.Bundle;
import android.os.Handler;
import android.os.ParcelUuid;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;


import static com.example.aklesoft.heartrate_monitor.Constants.ACTION_BROADCAST_RECEIVER;
import static com.example.aklesoft.heartrate_monitor.Constants.ACTION_BROADCAST_RECEIVER_DATA;
import static com.example.aklesoft.heartrate_monitor.Constants.CHARACTERISTIC_ECHO_STRING;
import static com.example.aklesoft.heartrate_monitor.Constants.CLIENT_CHARACTERISTIC_CONFIG;
import static com.example.aklesoft.heartrate_monitor.Constants.SCAN_PERIOD;


public class MainActivity extends AppCompatActivity implements Serializable {
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
    private int REQUEST_ENABLE_BT = 1;
    private Handler mHandler;
    private BluetoothLeScanner mLeScanner;
    private ScanSettings settings;
    private List<ScanFilter> filters;
    private BluetoothGatt mGatt;
    private GattClientCallback gattClientCallback = null;

    private boolean mScanning;

    public TextView tHR_Status;
    public TextView tHR_Data;
    public TextView tHR_Device;
    public TextView tGPS_Status;
    public TextView tBT_status;

    //  LinearLayout
    private LinearLayout MainView;
    TooltipWindow tipWindow;


    //  CheckBox
    private CheckBox cbStopwatch;
    private boolean bcbStopwatch;

//  parameters for BlackMode
    private boolean ShowSpeed;
    private boolean ShowHR;
    private boolean ShowClock;
    private boolean ShowTimer;

//  save settings
    SharedPreferences pref;
    SharedPreferences.Editor editor;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);


        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
//            return;
        }


        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "BLE Not Supported",
                    Toast.LENGTH_SHORT).show();
            finish();
        }

        filters = new ArrayList<>();
        filters.add(new ScanFilter.Builder().setServiceUuid(new ParcelUuid(HEART_RATE_SERVICE_UUID)).build());
        filters.add(new ScanFilter.Builder().setServiceUuid(new ParcelUuid(HEART_RATE_MEASUREMENT_CHAR_UUID)).build());

        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        if (bluetoothManager == null) throw new AssertionError("Object cannot be null");
        mBluetoothAdapter = bluetoothManager.getAdapter();

        //  ToggleButtons
        ToggleButton SettingSpeedView = (ToggleButton) findViewById(R.id.SettingSpeedView);
        ToggleButton SettingHRView = (ToggleButton) findViewById(R.id.SettingHRView);
        ToggleButton SettingClockView = (ToggleButton) findViewById(R.id.SettingClockView);
        ToggleButton SettingTimerView = (ToggleButton) findViewById(R.id.SettingStopWatchView);

        tHR_Status = (TextView) findViewById(R.id.HR_Status);
        tHR_Data = (TextView) findViewById(R.id.HR_Data);
        tHR_Device = (TextView) findViewById(R.id.HR_Device);

        tGPS_Status = (TextView) findViewById(R.id.GPS_Status);

        cbStopwatch = (CheckBox) findViewById(R.id.cbStopwatch);

        tBT_status = (TextView) findViewById(R.id.BT_Status);
        Log.d(TAG, "onCreate -> BLBALBALBLABLALBLALBLABLALBLABLBLALBLBALBLA");

        if(mBluetoothAdapter.isEnabled()){
            tBT_status.setText(getResources().getString(R.string.bluetooth_enabled));
        }

//  prepare shared data
        pref = getSharedPreferences("Heartrate_Monitor", 0);
        ShowSpeed = pref.getBoolean("ShowSpeed", false);
        ShowHR = pref.getBoolean("ShowHR", false);
        ShowClock = pref.getBoolean("ShowClock", false);
        ShowTimer = pref.getBoolean("ShowTimer", false);
        editor = pref.edit();
        editor.apply();


        bcbStopwatch = pref.getBoolean("StartStopwatch", false);
        cbStopwatch.setChecked(bcbStopwatch);

        SettingSpeedView.setChecked(ShowSpeed);
        SettingHRView.setChecked(ShowHR);
        SettingClockView.setChecked(ShowClock);
        SettingTimerView.setChecked(ShowTimer);


        tipWindow = new TooltipWindow(MainActivity.this);
        MainView = (LinearLayout) findViewById(R.id.MainView);

        MainView.setOnTouchListener(new OnSwipeTouchListener(MainActivity.this) {
            public void onSwipeTop() {
//                Toast.makeText(MainActivity.this, "top", Toast.LENGTH_SHORT).show();
//                Toast.makeText(MainActivity.this, "Swipe To Right To Start BlackMode", Toast.LENGTH_SHORT).show();
                tipWindow.showToolTip(MainView);
            }
            public void onSwipeRight() {
                finish();
            }
            public void onSwipeLeft() {
//                Toast.makeText(MainActivity.this, "left", Toast.LENGTH_SHORT).show();
                GoToBlackMode();
            }
            public void onSwipeBottom() {
//                Toast.makeText(MainActivity.this, "bottom", Toast.LENGTH_SHORT).show();
//                Toast.makeText(MainActivity.this, "Swipe To Right To Start BlackMode", Toast.LENGTH_SHORT).show();
                tipWindow.showToolTip(MainView);
            }
//            public boolean onTouch(View v, MotionEvent event) {
//                tipWindow.showToolTip(v);
//                return true;
//            }

        });

    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.e(TAG, "onResume - > BLBALBALBLABLALBLALBLABLALBLABLBLALBLBALBL");

        if(mHandler == null) {
            mHandler = new Handler();
        }

        if(ShowSpeed) {
                LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
                if (locationManager == null) throw new AssertionError("Object cannot be null");
                if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER))
                {
                    tGPS_Status.setText(getResources().getString(R.string.gps_enabled));
                }
        }

        if(ShowHR) {
            Log.e(TAG, "registerReceiver");
            registerReceiver(broadcastReceiver, broadcastReceiverUpdateIntentFilter());
            startScan();
        }
    }


    private void startScan() {
        Log.e(TAG, "onResume -> Start BT work");

        tHR_Status.setText(getResources().getString(R.string.scan_for_devices));

        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
        else {
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

    private void stopScan() {
        Log.e(TAG, "Stopped scanning.");
        if (mScanning && (mLeScanner != null || mBluetoothAdapter != null) && mBluetoothAdapter.isEnabled()) {

            Log.e(TAG, "Stopped scanning -> cancelDiscovery()");
            mBluetoothAdapter.cancelDiscovery();
            Log.e(TAG, "Stopped scanning -> mLeScanner.stopScan(mScanCallback)");
            mLeScanner.stopScan(mScanCallback);
            tHR_Status.setText(getResources().getString(R.string.scan_stopped));
        }
        
        mScanning = false;
        if(!mScanning){

        }
    }


    @Override
    protected void onPause(){
        super.onPause();

        if(ShowHR) {
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
    public void onStart() {
        super.onStart();

    }

    @Override
    public void onStop() {
        super.onStop();

        mHandler = null;

        Log.d(TAG, "onStop -> SharedPreferences -> this.ShowHR:"+this.ShowHR);
        editor = pref.edit();
        editor.putBoolean("ShowSpeed", this.ShowSpeed);
        editor.putBoolean("ShowHR", this.ShowHR);
        editor.putBoolean("ShowTimer", this.ShowTimer);
        editor.putBoolean("ShowClock", this.ShowClock);
        editor.putBoolean("StartStopwatch", this.bcbStopwatch);
        editor.apply();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if(ShowHR) {
            disconnectGattServer(mGatt);
            mGatt = null;
        }

        if (tipWindow != null && tipWindow.isTooltipShown())
            tipWindow.dismissTooltip();

        Thread[] threads = new Thread[Thread.activeCount()];
        Thread.enumerate(threads);
        for (Thread t : threads) {
            if (t.isAlive()) {
                Log.e(TAG, "onDestroy -> thread interrupt -> "+t);
                t.interrupt();
            }
        }

        System.exit(0);

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
                Log.e(TAG, "scanLeDevice 2-> stopScan");
                mLeScanner.stopScan(mScanCallback);
            }
        }

    }

    private ScanCallback mScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            Log.e(TAG, "onScanResult");
            Log.i(TAG, String.valueOf(callbackType));
            Log.e(TAG, "getDevice -> "+ result.toString());
            Log.i(TAG, result.toString());
            BluetoothDevice btDevice = result.getDevice();
            Log.e(TAG, "onScanResult -> connectToDevice -> "+result.getDevice());
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

    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            (device, rssi, scanRecord) -> runOnUiThread(() -> {
                Log.d("onLeScan", device.toString());
                connectToDevice(device);
            });

    public void connectToDevice(BluetoothDevice device) {

        boolean paired = false;
        for( BluetoothDevice pairedDevice : mBluetoothAdapter.getBondedDevices()){
            if(pairedDevice.getAddress().equals(device.getAddress())){
                paired=true;
                break;
            }
        }

        if(paired){
            String text = device+" "+getResources().getString(R.string.connecting);
            tHR_Status.setText(text);
            if( gattClientCallback == null) {
                gattClientCallback = new GattClientCallback();
            }
            mGatt = device.connectGatt(this,false, gattClientCallback);

            Log.e(TAG, "connectToDevice -> mGatt -> "+mGatt.getDevice());

            if( mGatt.connect() ){
                tHR_Status.setText(getResources().getString(R.string.device_connected));
                tHR_Device.setText(device.getName());
                Log.e(TAG, "connectToDevice -> scanLeDevice -> false");
                scanLeDevice(false);// will stop after first device detection

            }
            else{
                gattClientCallback = null;
                Log.e(TAG, "connectToDevice -> mGatt.connect() -> false");
            }
        }
        else{
            showToastMessage();
        }

    }

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

            Log.d(TAG, "onServicesDiscovered ->"+gatt);
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
                    if(descriptor != null) {
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
            }
            catch (Exception e){
                Log.d(TAG, "get null from messageBytes");
            }


        }
    }


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
            tHR_Status.setText(getResources().getString(R.string.device_disconnected));
            tHR_Device.setText("");
        }
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



////////////////////////////////////////
//  Set button for Blackmode
    public void GoToBlackMode() {
        Intent myIntent = new Intent(getApplicationContext(), BlackMode.class);
        myIntent.putExtra("ShowSpeed", this.ShowSpeed);
        myIntent.putExtra("ShowHR", this.ShowHR);
        myIntent.putExtra("ShowTimer", this.ShowTimer);
        myIntent.putExtra("ShowClock", this.ShowClock);
        myIntent.putExtra( "StartStopwatch", this.bcbStopwatch);

        startActivity(myIntent);
        Log.d("\">>>>>>>>AKL <1> : ", "" );
    }


//    private void setSaveDevice(BluetoothDevice device) {
//        SharedPreferences settings = getSharedPreferences("Heartrate_Monitor", 0);
//        SharedPreferences.Editor editor = settings.edit();
//
//        editor.putString("deviceAddress", device.getAddress());
//        editor.putString("deviceName", device.getName());
//
//        editor.commit();
//    }
//
//    private void getSaveDevice(SharedPreferences pref) {
//
//        mDeviceAddress = pref.getString("deviceAddress", null);
//        mDeviceName = pref.getString("deviceName", null);
//
//        if( mDeviceName != null && mDeviceAddress != null ){
//            ibResetDevice.setVisibility(View.VISIBLE);
//            setHRStatus("Found stored device");
//            setHRDevice(mDeviceName);
//        }
//    }

    ////////////////////////////////////////
//  updates the UI with heart rate data

    public void setHrData(int hrData) {
        runOnUiThread(() -> tHR_Data.setText(String.format(Locale.getDefault(), "%d", hrData )));
    }

    ////////////////////////////////////////
//  Set toggle button for Speed TextView
    public void setSpeedOnClick (View view){
        this.ShowSpeed = !this.ShowSpeed;
    }

    ////////////////////////////////////////
//  Set toggle button for Speed TextView
    public void setHrOnClick (View view){
        Log.d(TAG, String.format("SettingHrOnClick -> state ->: %b", this.ShowHR));

        if(this.ShowHR){
            Log.d(TAG, "SettingHrOnClick -> state -> 1 ");
        }

        if(!this.ShowHR){
            if( (mLeScanner != null || mBluetoothAdapter != null)) {
                Log.d(TAG, "SettingHrOnClick -> state -> 2 ");
                startScan();
            }
        }

        this.ShowHR = !this.ShowHR;

    }

    ////////////////////////////////////////
    public void setStopwatchOnClick(View view) {
        this.ShowTimer = !this.ShowTimer;
    }

    ////////////////////////////////////////
    public void setClockOnClick(View view) {
        this.ShowClock = !this.ShowClock;
    }

//    public void ResetDevice(View view) {
//        SharedPreferences settings = getSharedPreferences("Heartrate_Monitor", 0);
//        SharedPreferences.Editor editor = settings.edit();
//        editor.putString("deviceAddress", null);
//        editor.putString("deviceName", null);
//        editor.commit();
//        mDeviceAddress = null;
//        mDeviceName = null;
//        setHRDevice("Device removed");
//        ibResetDevice.setVisibility(View.GONE);
//
//    }

    public void setcbStopwatch(View view) {
        bcbStopwatch = cbStopwatch.isChecked();
    }


}

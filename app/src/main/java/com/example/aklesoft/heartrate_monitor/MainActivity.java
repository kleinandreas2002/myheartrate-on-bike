package com.example.aklesoft.heartrate_monitor;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
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
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


import static com.example.aklesoft.heartrate_monitor.Constants.CHARACTERISTIC_ECHO_STRING;
import static com.example.aklesoft.heartrate_monitor.Constants.SCAN_PERIOD;


public class MainActivity extends AppCompatActivity implements Serializable {
    public final static String TAG = MainActivity.class.getSimpleName();
    public static String HEART_RATE_MEASUREMENT = "0000180d-0000-1000-8000-00805f9b34fb";
    public static String CLIENT_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb";

    public static String ACTION_BROADCAST_RECEIVER = "com.example.aklesoft.heartrate_monitor.RECEIVER";
    public static String ACTION_BROADCAST_RECEIVER_DATA = "com.example.aklesoft.heartrate_monitor.DATA";
    //  UUIDs
    static final UUID HEART_RATE_SERVICE_UUID = convertFromInteger(0x180D);
    static final UUID HEART_RATE_MEASUREMENT_CHAR_UUID = convertFromInteger(0x2A37);
    final UUID HEART_RATE_CONTROL_POINT_CHAR_UUID = convertFromInteger(0x2A39);

    public static UUID convertFromInteger(int i) {
        final long MSB = 0x0000000000001000L;
        final long LSB = 0x800000805f9b34fbL;
        long value = i & 0xFFFFFFFF;
        return new UUID(MSB | (value << 32), LSB);
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
//    private ScanCallback mScanCallback;

    //    private BluetoothLeService mBluetoothLeService;
    private int HRStartData = -10;


    public String mDeviceAddress = null;
    public String mDeviceName = null;

    private long mTimestamp = 0;
    private boolean mScanning;

    public static TextView tHR_Status;
    public static TextView tHR_Data;
    public static TextView tHR_DeviceAddress;
    public static TextView tHR_Device;
    public static String HR_DeviceAddress;
    public static String HR_DeviceName;

    public static TextView tGPS_Status;

    public static TextView tBT_status;

    //  LinearLayout
    private LinearLayout MainView;
    TooltipWindow tipWindow;
    private float[] lastTouchDownXY = new float[2];

//  ImageButton
    private ImageButton ibResetDevice;

//  ToggleButtons
    private ToggleButton SettingSpeedView;
    private ToggleButton SettingHRView;
    private ToggleButton SettingClockView;
    private ToggleButton SettingTimerView;


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
//            finish();
        }

        filters = new ArrayList<ScanFilter>();
        filters.add(new ScanFilter.Builder().setServiceUuid(new ParcelUuid(HEART_RATE_SERVICE_UUID)).build());
        filters.add(new ScanFilter.Builder().setServiceUuid(new ParcelUuid(HEART_RATE_MEASUREMENT_CHAR_UUID)).build());

        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        SettingSpeedView = (ToggleButton) findViewById(R.id.SettingSpeedView);
        SettingHRView = (ToggleButton) findViewById(R.id.SettingHRView);
        SettingClockView = (ToggleButton) findViewById(R.id.SettingClockView);
        SettingTimerView = (ToggleButton) findViewById(R.id.SettingStopWatchView);

        ibResetDevice = (ImageButton) findViewById(R.id.ResetDevice);

        tHR_Status = (TextView) findViewById(R.id.HR_Status);
        tHR_Data = (TextView) findViewById(R.id.HR_Data);
        tHR_Device = (TextView) findViewById(R.id.HR_Device);

        tGPS_Status = (TextView) findViewById(R.id.GPS_Status);

        cbStopwatch = (CheckBox) findViewById(R.id.cbStopwatch);

        tBT_status = (TextView) findViewById(R.id.BT_Status);
//  prepare shared data
        Log.e(TAG, "onCreate -> BLBALBALBLABLALBLALBLABLALBLABLBLALBLBALBL");

        pref = getSharedPreferences("Heartrate_Monitor", 0);
        ShowSpeed = pref.getBoolean("ShowSpeed", false);
        ShowHR = pref.getBoolean("ShowHR", false);
        ShowClock = pref.getBoolean("ShowClock", false);
        ShowTimer = pref.getBoolean("ShowTimer", false);

        editor = pref.edit();
        editor.commit();

        bcbStopwatch = pref.getBoolean("StartStopwatch", false);
        if( bcbStopwatch ) {
            cbStopwatch.setChecked(bcbStopwatch);
        }
        getSaveDevice(pref);

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


    private void startTimeCounter() {
        mTimestamp = System.currentTimeMillis();
    }

    private boolean isTimeAlready(int mkSeconds) {
        return System.currentTimeMillis() - mTimestamp > mkSeconds ? true : false;
    }

    public void setHRStatus(String HRStatus) {
//        tHR_Status.setText("HR Status: "+ HRStatus);
        tHR_Status.setText(HRStatus);
    }
    public void setHRDevice(String HRDevice) {
//        tHR_Device.setText("HR Device: "+ HRDevice);
        tHR_Device.setText(HRDevice);
    }

    public void setHrData(int hrData) {
        this.HRStartData = hrData;
//        tHR_Data.setText("HR Data: "+ Integer.toString(hrData) );

        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                // Stuff that updates the UI
                tHR_Data.setText(Integer.toString(hrData) );

            }
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
                LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);;
                if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER))
                {
                    tGPS_Status.setText("GPS enabled");
                }
        }

        if(ShowHR) {

            pref = getSharedPreferences("Heartrate_Monitor", 0);
            mDeviceAddress = pref.getString("deviceAddress", null);
            mDeviceName = pref.getString("deviceName", null);

            if(mBluetoothAdapter.isEnabled()){
                tBT_status.setText("Bluetooth enabled");
            }


            Log.e(TAG, "registerReceiver");
            registerReceiver(broadcastReceiver, broadcastReceiverUpdateIntentFilter());
//            Log.e(TAG, "sendBroadcastIntent()");
//            sendBroadcastIntent();
            startScan();

        }
    }


    private void startScan() {
        Log.e(TAG, "onResume -> Start BT work");

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



        if( mDeviceName != null && mDeviceAddress != null ){
            ibResetDevice.setVisibility(View.VISIBLE);
            setHRStatus("Found stored device");
            setHRDevice(mDeviceName);
        }
    }

    private void stopScan() {
        Log.e(TAG, "Stopped scanning.");
        if (mScanning && (mLeScanner != null || mBluetoothAdapter != null) && mBluetoothAdapter.isEnabled()) {

            Log.e(TAG, "Stopped scanning -> cancelDiscovery()");
            mBluetoothAdapter.cancelDiscovery();
            Log.e(TAG, "Stopped scanning -> mLeScanner.stopScan(mScanCallback)");
            mLeScanner.stopScan(mScanCallback);
        }
        
        mScanning = false;
//        mHandler = null;
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

        if(ShowHR) {

        }
    }

    @Override
    public void onStop() {
        super.onStop();

        mHandler = null;

        Log.e(TAG, "onStop -> SharedPreferences -> this.ShowHR:"+this.ShowHR);
        setSaveDevice(mGatt.getDevice());
        editor = pref.edit();
        editor.putBoolean("ShowSpeed", this.ShowSpeed);
        editor.putBoolean("ShowHR", this.ShowHR);
        editor.putBoolean("ShowTimer", this.ShowTimer);
        editor.putBoolean("ShowClock", this.ShowClock);
        editor.putBoolean("StartStopwatch", this.bcbStopwatch);
        editor.commit();
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

    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            (device, rssi, scanRecord) -> runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Log.d("onLeScan", device.toString());
                    connectToDevice(device);
                }
            });

    public void connectToDevice(BluetoothDevice device) {

        tHR_Status.setText(device+" connecting");
        if( gattClientCallback == null) {
            gattClientCallback = new GattClientCallback();
        }
        mGatt = device.connectGatt(this,false, gattClientCallback);

        Log.e(TAG, "connectToDevice -> mGatt -> "+mGatt.getDevice());

        if( mGatt.connect() ){
            tHR_Status.setText(getResources().getString(R.string.device_connected));
            Log.e(TAG, "connectToDevice -> scanLeDevice -> false");
            scanLeDevice(false);// will stop after first device detection

        }
        else{
            gattClientCallback = null;
            Log.e(TAG, "connectToDevice -> mGatt.connect() -> false");
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
                int format = -1;
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
                Log.e(TAG, "get null from messageBytes");
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
            if(intent.getAction().equals(ACTION_BROADCAST_RECEIVER)){
// ONLY for DEBUGGING
//                Log.d(TAG, String.format("ACTION_BROADCAST_RECEIVER_DATA: "+ intent.getStringExtra(ACTION_BROADCAST_RECEIVER_DATA)));
//                intent.putExtra(ACTION_BROADCAST_RECEIVER_DATA, intent.getAction().equals(ACTION_BROADCAST_RECEIVER_DATA));
            }
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
        myIntent.putExtra( "HR_DeviceName", this.HR_DeviceName);
        myIntent.putExtra( "HR_DeviceAddress", this.HR_DeviceAddress);
        myIntent.putExtra( "HRData", this.HRStartData);
        myIntent.putExtra( "StartStopwatch", this.bcbStopwatch);

        startActivity(myIntent);
        Log.d("\">>>>>>>>AKL <1> : ", "" );
    }


    private void setSaveDevice(BluetoothDevice device) {
        SharedPreferences settings = getSharedPreferences("Heartrate_Monitor", 0);
        SharedPreferences.Editor editor = settings.edit();

        editor.putString("deviceAddress", device.getAddress());
        editor.putString("deviceName", device.getName());

        editor.commit();
    }

    private void getSaveDevice(SharedPreferences pref) {

        mDeviceAddress = pref.getString("deviceAddress", null);
        mDeviceName = pref.getString("deviceName", null);

        if( mDeviceName != null && mDeviceAddress != null ){
            ibResetDevice.setVisibility(View.VISIBLE);
            setHRStatus("Found stored device");
            setHRDevice(mDeviceName);
        }
    }

    ////////////////////////////////////////
//  Set toggle button for Speed TextView
    public void SettingSpeedOnClick (View view){
//ToDo
        this.ShowSpeed = !this.ShowSpeed;

    }

    ////////////////////////////////////////
//  Set toggle button for Speed TextView
    public void SettingHrOnClick (View view){
//ToDo
        Log.d(TAG, String.format("SettingHrOnClick -> state ->: %b", this.ShowHR));

        if(this.ShowHR){
            Log.d(TAG, String.format("SettingHrOnClick -> state -> 1 "));
        }

        if(!this.ShowHR){
            if( (mLeScanner != null || mBluetoothAdapter != null)) {
                Log.d(TAG, String.format("SettingHrOnClick -> state -> 2 "));
                startScan();
            }
        }

        this.ShowHR = !this.ShowHR;


//        scanForHRM(true);
//        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
    }

    ////////////////////////////////////////
    public void SettingStopwatchOnClick(View view) {
//ToDo
        this.ShowTimer = !this.ShowTimer;

    }

    ////////////////////////////////////////
    public void SettingClockOnClick(View view) {
//ToDo
        this.ShowClock = !this.ShowClock;

    }

    public void ResetDevice(View view) {
        SharedPreferences settings = getSharedPreferences("Heartrate_Monitor", 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString("deviceAddress", null);
        editor.putString("deviceName", null);
        editor.commit();
        mDeviceAddress = null;
        mDeviceName = null;
        setHRStatus(getString(R.string.tHR_Status));
        setHRDevice("");
        ibResetDevice.setVisibility(View.GONE);

    }

    public void setcbStopwatch(View view) {
        if( cbStopwatch.isChecked() ) {
            bcbStopwatch = true;
        }
        else {
            bcbStopwatch = false;
        }
    }


}

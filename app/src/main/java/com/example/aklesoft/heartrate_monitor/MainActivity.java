package com.example.aklesoft.heartrate_monitor;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.util.UUID;


public class MainActivity extends AppCompatActivity{
    private final static String TAG = MainActivity.class.getSimpleName();

    //  Bluetooth

    public static final int MODE_DISCONNECTED = 0;
    public static final int MODE_CONNECTING = 1;
    public static final int MODE_CONNECTED = 2;
    public static final int MODE_SERVICE_DISCOVERED = 3;

    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeService mBluetoothLeService;

    private int HRStartData = -10;

    public static BluetoothLeService mServiceStatus;

    Handler mHandler;
    private String stateBluetooth = "disabled";
    public final static UUID UUID_HEART_RATE_MEASUREMENT = UUID.fromString(SampleGattAttributes.HEART_RATE_MEASUREMENT);
    public String mDeviceAddress = null;
    public String mDeviceName = null;

    public int state = MODE_DISCONNECTED;


    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;
    private static final int REQUEST_ENABLE_BT = 1;
    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 10000;
    private boolean mScanning;

    public static TextView tHR_Status;
    public static TextView tHR_Data;
    public static TextView tHR_DeviceAddress;
    public static TextView tHR_Device;
    public static String HR_DeviceAddress;
    public static String HR_DeviceName;


//  ToggleButtons
    private ToggleButton SettingSpeedView;
    private ToggleButton SettingHRView;
    private ToggleButton SettingClockView;
    private ToggleButton SettingTimerView;

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


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android M Permission check
            if (this.checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("This app needs location access");
                builder.setMessage("Please grant location access so this app can detect beacons.");
                builder.setPositiveButton(android.R.string.ok, null);
                builder.setOnDismissListener(new DialogInterface.OnDismissListener() {

                    public void onDismiss(DialogInterface dialog) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            requestPermissions(new String[]{android.Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_COARSE_LOCATION);
                        }
                    }
                });
                builder.show();
            }
        }
        mHandler = new Handler();



        SettingSpeedView = (ToggleButton) findViewById(R.id.SettingSpeedView);
        SettingHRView = (ToggleButton) findViewById(R.id.SettingHRView);
        SettingClockView = (ToggleButton) findViewById(R.id.SettingClockView);
        SettingTimerView = (ToggleButton) findViewById(R.id.SettingTimerView);

        tHR_Status = (TextView) findViewById(R.id.HR_Status);
        tHR_Data = (TextView) findViewById(R.id.HR_Data);
        tHR_Device = (TextView) findViewById(R.id.HR_Device);

//  prepare shared data
        pref = getSharedPreferences("Heartrate_Monitor", 0);
        editor = pref.edit();
        this.ShowSpeed = pref.getBoolean("ShowSpeed", false);
        this.ShowHR = pref.getBoolean("ShowHR", false);
        this.ShowClock = pref.getBoolean("ShowClock", false);
        this.ShowTimer = pref.getBoolean("ShowTimer", false);

        SettingSpeedView.setChecked(this.ShowSpeed);
        SettingHRView.setChecked(this.ShowHR);
        SettingClockView.setChecked(this.ShowClock);
        SettingTimerView.setChecked(this.ShowTimer);

        Initialize_BTLE();


    }

    private void Initialize_BTLE() {

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "BT not supported by phone", Toast.LENGTH_SHORT).show();
            finish();
        }
        // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        BluetoothAdapter_Initialize(true);

        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "BT adapter not available", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        mHandler = new Handler();
        scanLeDevice(true, mBluetoothAdapter);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        if (!mScanning) {
            menu.findItem(R.id.menu_stop).setVisible(false);
            menu.findItem(R.id.menu_scan).setVisible(true);
            menu.findItem(R.id.menu_refresh).setActionView(null);
        } else {
            menu.findItem(R.id.menu_stop).setVisible(true);
            menu.findItem(R.id.menu_scan).setVisible(false);
            menu.findItem(R.id.menu_refresh).setActionView(
                    R.layout.actionbar_indeterminate_progress);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_scan:
                scanLeDevice(true, mBluetoothAdapter);

                break;
            case R.id.menu_stop:
                scanLeDevice(false, mBluetoothAdapter);

                break;
        }
        return true;
    }


    public void scanLeDevice(final boolean enable, final BluetoothAdapter mBluetoothAdapter) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                    invalidateOptionsMenu();
                }
            }, SCAN_PERIOD);

            mScanning = true;
            mBluetoothAdapter.startLeScan(mLeScanCallback);
        } else {
            mScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }
        invalidateOptionsMenu();


    }

    // Device scan callback.
    public BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {

                @Override
                public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Log.i(TAG, "Device Found: " + device.getName() + " / " + device.getAddress());

                            HR_DeviceAddress = device.getAddress();
                            HR_DeviceName = device.getName();
                            tHR_Device.setText("HR Device: "+HR_DeviceName);
                            checkForHrmService(device.getAddress());

                            if (mScanning) {
                                mBluetoothAdapter.stopLeScan(mLeScanCallback);
                                mScanning = false;

                            }

                        }
                    });

                }
            };

    private void checkForHrmService(String address) {
        Log.i(TAG, "ASYNC CHECK FOR HRM SERVICE");

        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);

        BluetoothGatt mBluetoothGatt = device.connectGatt(this, false, new BluetoothGattCallback() {

            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                super.onConnectionStateChange(gatt, status, newState);
                Log.i(TAG, "LOCAL BT SERVICE STATE CHANGE: " + newState);

                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    if (gatt != null) {
                        runOnUiThread(new Runnable() {
                                          @Override
                                          public void run() {

                                              tHR_Status.setText("HR Status: Connected");
                                          }
                                      });
                        Log.i(TAG, "Connected to GATT Server");
                        Log.i(TAG, "Attempting to start service discovery: " + gatt.discoverServices());
                    }

                }
                else if(newState == BluetoothProfile.STATE_DISCONNECTED) {
                    if (gatt != null) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {

                                tHR_Status.setText("HR Status: Disconnected");
                            }
                        });
                    }
                }
            }

            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                super.onServicesDiscovered(gatt, status);

                Log.i(TAG, "Async gatt discovered!!");

                for (BluetoothGattService gattService : gatt.getServices()) {

                    for(BluetoothGattCharacteristic gattCharacteristic : gattService.getCharacteristics()) {
                        if (UUID_HEART_RATE_MEASUREMENT.equals(gattCharacteristic.getUuid())) {
                            Log.i(TAG, "HRM service found!!");

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {

                                    mDeviceName = device.getName();
//                                    saveDevice(device);
                                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                                    mScanning = false;

                                }
                            });
                        }

                    }

                }

            }
        });
        if(device.getAddress() != null ) {
            mDeviceAddress = device.getAddress();
        }
        mBluetoothGatt.connect();
    }

    private void connectDevice() {

        if (mBluetoothLeService != null) {

            mBluetoothAdapter.stopLeScan(mLeScanCallback);
            mScanning = false;
            final boolean result = mBluetoothLeService.connect(mDeviceAddress, this);
            Log.d(TAG, "Connect request result=" + result);
        }
    }

    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            final String action = intent.getAction();

            Log.i(TAG, "BTS Callback action: " + action);

            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
//                labelStatus.setText("Connected. Preparing.");
                //ToDo
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
//                setActivityState(MODE_DISCONNECTED);
                //ToDo
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                //ToDo
                if (mBluetoothLeService != null) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            tHR_Data.setText("Reading Heart Rate");
                        }
                    });
                    mBluetoothLeService.turnHRMNotification();
//                    setActivityState(MODE_SERVICE_DISCOVERED);
                }

            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                if (state != MODE_CONNECTED) {
//                    setActivityState(MODE_CONNECTED);
                }

                Log.i(TAG, "HRM: " + intent.getStringExtra(mBluetoothLeService.EXTRA_DATA));
                updateHrValue(Integer.valueOf(intent.getStringExtra(mBluetoothLeService.EXTRA_DATA)));

            }

        }
    };
    public void updateHrValue(int hrData) {
        this.HRStartData = hrData;

        tHR_Data.setText("HR Data: "+ Integer.toString(hrData) );

    }

    private static IntentFilter makeGattUpdateIntentFilter() {

        final IntentFilter intentFilter = new IntentFilter();

        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);

        return intentFilter;
    }



    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            mServiceStatus = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }

            Log.e(TAG, "initialize Bluetooth");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mBluetoothLeService = null;
        }
    };

    @Override
    protected void onResume() {
        super.onResume();

        // Ensures Bluetooth is enabled on the device.  If Bluetooth is not currently enabled,
        // fire an intent to display a dialog asking the user to grant permission to enable it.
        connectDevice();


        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        if( mServiceStatus == null ) {
            startService(gattServiceIntent);
            bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
        }

        if (!mBluetoothAdapter.isEnabled()) {
            if (!mBluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
        }
//        BluetoothAdapter_Initialize(true);
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());

    }

    @Override
    protected void onPause(){
        super.onPause();

        LocalBroadcastManager.getInstance(this).unregisterReceiver(mGattUpdateReceiver);
//        unregisterReceiver(mGattUpdateReceiver);

    }

////////////////////////////////////
//  Bluetooth Adapter initializing
//  true = start from onStart()
//  false = start from ButtonListener
    public void BluetoothAdapter_Initialize(boolean checkOnActivityStart){

        if (mBluetoothAdapter == null) {
            stateBluetooth = "Bluetooth NOT supported";
        } else {
            if (mBluetoothAdapter.isEnabled()) {
                if (mBluetoothAdapter.isDiscovering()) {
                    stateBluetooth = "Bluetooth is currently in device discovery process.";
                } else {
                    stateBluetooth = "Bluetooth is Enabled.";
                }
            } else {
                stateBluetooth = "Bluetooth is NOT Enabled!";

                if(!checkOnActivityStart) {
                    mBluetoothAdapter.enable();
                    stateBluetooth = "Bluetooth is Enabled.";
                }
            }
        }
        TextView bt_status = (TextView) findViewById(R.id.BT_Status);
        bt_status.setText("BT Status: "+stateBluetooth);
    }

////////////////////////////////////////
//  Set button for Blackmode
    public void GoToBlackMode(View view) {
        Intent myIntent = new Intent(view.getContext(), BlackMode.class);
        myIntent.putExtra("ShowSpeed", this.ShowSpeed);
        myIntent.putExtra("ShowHR", this.ShowHR);
        myIntent.putExtra("ShowTimer", this.ShowTimer);
        myIntent.putExtra("ShowClock", this.ShowClock);
//        myIntent.putExtra("EXTRAS_DEVICE_NAME", mLeDeviceListAdapter.getDevice(BTDeviveList.getSelectedItemPosition()).getName());
//        myIntent.putExtra("EXTRAS_DEVICE_ADDRESS", mLeDeviceListAdapter.getDevice(BTDeviveList.getSelectedItemPosition()).getAddress());
        if (mScanning) {
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
            mScanning = false;
        }
//        Log.d("\">>>>>>>>>>>>>>>>>>>>>>>>>>AKL <0> : ", "" );
//        if( mServiceStatus != null ){
//            unregisterReceiver(mGattUpdateReceiver);
//            unbindService(mServiceConnection);
//        }

        startActivity(myIntent);
//        Log.d("\">>>>>>>>>>>>>>>>>>>>>>>>>>AKL <1> : ", "" );
    }



    @Override
    public void onStart() {
        super.onStart();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.


    }

    @Override
    public void onStop() {
        super.onStop();

        editor.putBoolean("ShowSpeed", this.ShowSpeed);
        editor.putBoolean("ShowHR", this.ShowHR);
        editor.putBoolean("ShowTimer", this.ShowTimer);
        editor.putBoolean("ShowClock", this.ShowClock);
        editor.commit();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        unbindService(mServiceConnection);
        unregisterReceiver(mGattUpdateReceiver);

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
        this.ShowHR = !this.ShowHR;

    }

    ////////////////////////////////////////
    public void SettingTimerOnClick(View view) {
//ToDo
        this.ShowTimer = !this.ShowTimer;

    }

    ////////////////////////////////////////
    public void SettingClockOnClick(View view) {
//ToDo
        this.ShowClock = !this.ShowClock;

    }



}

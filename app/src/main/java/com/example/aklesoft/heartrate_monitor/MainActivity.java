package com.example.aklesoft.heartrate_monitor;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
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
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
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
    public boolean mBluetoothLeServiceResult = false;

    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;
    private static final int REQUEST_ENABLE_BT = 1;
    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 5000;
    private boolean mScanning;

    public static TextView tHR_Status;
    public static TextView tHR_Data;
    public static TextView tHR_DeviceAddress;
    public static TextView tHR_Device;
    public static String HR_DeviceAddress;
    public static String HR_DeviceName;

//  LinearLayout
    private LinearLayout MainView;
    TooltipWindow tipWindow;
    private float[] lastTouchDownXY = new float[2];


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
    public static final String PREFS_NAME = "HRMPreferencesFile";

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
        mDeviceAddress = pref.getString("deviceAddress", null);
        mDeviceName = pref.getString("deviceName", null);

        ShowSpeed = pref.getBoolean("ShowSpeed", false);
        ShowHR = pref.getBoolean("ShowHR", false);
        ShowClock = pref.getBoolean("ShowClock", false);
        ShowTimer = pref.getBoolean("ShowTimer", false);

        SettingSpeedView.setChecked(ShowSpeed);
        SettingHRView.setChecked(ShowHR);
        SettingClockView.setChecked(ShowClock);
        SettingTimerView.setChecked(ShowTimer);

        Initialize_BTLE();

        tipWindow = new TooltipWindow(MainActivity.this);
        MainView = (LinearLayout) findViewById(R.id.MainView);
//        MainView.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                Toast.makeText(MainActivity.this, "Swipe To Right To Start BlackMode", Toast.LENGTH_SHORT).show();
//                if (!tipWindow.isTooltipShown())
//                    tipWindow.showToolTip(v);
//            }
//        });
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

    private boolean Initialize_BTLE() {

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "BT not supported by phone", Toast.LENGTH_SHORT).show();
            return false;
        }
        // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        BluetoothAdapter_Initialize(true);

        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "BT adapter not available", Toast.LENGTH_SHORT).show();
            finish();
            return false;
        }

        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, 1);
        }

        return true;
    }


//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        getMenuInflater().inflate(R.menu.main, menu);
//        if (!mScanning) {
//            menu.findItem(R.id.menu_stop).setVisible(false);
//            menu.findItem(R.id.menu_scan).setVisible(true);
//            menu.findItem(R.id.menu_refresh).setActionView(null);
//        } else {
//            menu.findItem(R.id.menu_stop).setVisible(true);
//            menu.findItem(R.id.menu_scan).setVisible(false);
//            menu.findItem(R.id.menu_refresh).setActionView(
//                    R.layout.actionbar_indeterminate_progress);
//        }
//        return true;
//    }

//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        switch (item.getItemId()) {
//            case R.id.menu_scan:
//                scanLeDevice(true, mBluetoothAdapter);
//
//                break;
//            case R.id.menu_stop:
//                scanLeDevice(false, mBluetoothAdapter);
//
//                break;
//        }
//        return true;
//    }


//    public void scanLeDevice(final boolean enable, final BluetoothAdapter mBluetoothAdapter) {
//        if (enable) {
//            // Stops scanning after a pre-defined scan period.
//            mHandler.postDelayed(new Runnable() {
//                @Override
//                public void run() {
//                    mScanning = false;
//                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
//                    invalidateOptionsMenu();
//                }
//            }, SCAN_PERIOD);
//
//            mScanning = true;
//            mBluetoothAdapter.startLeScan(mLeScanCallback);
//        } else {
//            mScanning = false;
//            mBluetoothAdapter.stopLeScan(mLeScanCallback);
//        }
//        invalidateOptionsMenu();
//
//
//    }

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
                            saveDevice(device);
                            mBluetoothAdapter.stopLeScan(mLeScanCallback);

                        }
                    });

                }
            };

    private void scanForHRM(final boolean enable) {

        if (enable) {

            if (mDeviceAddress == null ) {
                mScanning = true;
                mBluetoothAdapter.startLeScan(mLeScanCallback);
            } else {
                Log.i(TAG, "Connect to stored device: " + mDeviceAddress);
                setHRStatus("Connect to " + mDeviceAddress);
                setHRDevice(mDeviceName);

            }
        } else {

            mScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
            if (mBluetoothLeService != null) {
                mBluetoothLeService.disconnect();
                mBluetoothLeService.close();
            }
        }


    }

    public void setHRStatus(String HRStatus) {
        tHR_Status.setText("HR Status: "+ HRStatus);
    }
    public void setHRDevice(String HRDevice) {
        tHR_Device.setText("HR Device: "+ HRDevice);
    }

//    private void connectDevice() {
//
//        if (mBluetoothLeService != null) {
//
//            mBluetoothAdapter.stopLeScan(mLeScanCallback);
//            mScanning = false;
//            mBluetoothLeServiceResult = mBluetoothLeService.connect(mDeviceAddress, this);
//            Log.e(TAG, "Connect request result=" + mBluetoothLeServiceResult);
//        }
//    }

    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            final String action = intent.getAction();

            Log.i(TAG, "BTS Callback action: " + action);

            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                setHRStatus("Connected");
                //ToDo
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                setHRStatus("Disconnected");
                //ToDo
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                //ToDo
                if (mBluetoothLeService != null) {
                    setHRStatus("Connected - Ready to Start");
                    mBluetoothLeService.turnHRMNotification();
                }

            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                if (state != MODE_CONNECTED) {
//                    setActivityState(MODE_CONNECTED);
                }

                Log.i(TAG, "HRM: " + intent.getStringExtra(mBluetoothLeService.EXTRA_DATA));
                setHrValue(Integer.valueOf(intent.getStringExtra(mBluetoothLeService.EXTRA_DATA)));

            }

        }
    };
    public void setHrValue(int hrData) {
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

        if(ShowHR) {
            Log.e(TAG, "Resume -> scanForHRM");

            scanForHRM(true);
            registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        }
    }

    @Override
    protected void onPause(){
        super.onPause();
        if(ShowHR) {
            scanForHRM(false);
            unregisterReceiver(mGattUpdateReceiver);
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        if(ShowHR) {
            Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
            startService(gattServiceIntent);
            bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        editor = pref.edit();
        editor.putBoolean("ShowSpeed", this.ShowSpeed);
        editor.putBoolean("ShowHR", this.ShowHR);
        editor.putBoolean("ShowTimer", this.ShowTimer);
        editor.putBoolean("ShowClock", this.ShowClock);
        editor.commit();

    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (tipWindow != null && tipWindow.isTooltipShown())
            tipWindow.dismissTooltip();

        if(ShowHR) {
            unbindService(mServiceConnection);
        }

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

                if(checkOnActivityStart) {
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
    public void GoToBlackMode() {
        Intent myIntent = new Intent(getApplicationContext(), BlackMode.class);
        myIntent.putExtra("ShowSpeed", this.ShowSpeed);
        myIntent.putExtra("ShowHR", this.ShowHR);
        myIntent.putExtra("ShowTimer", this.ShowTimer);
        myIntent.putExtra("ShowClock", this.ShowClock);
        myIntent.putExtra( "HR_DeviceName", this.HR_DeviceName);
        myIntent.putExtra( "HR_DeviceAddress", this.HR_DeviceAddress);

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


    private void saveDevice(BluetoothDevice device) {
        SharedPreferences settings = getSharedPreferences("Heartrate_Monitor", 0);
        SharedPreferences.Editor editor = settings.edit();

        editor.putString("deviceAddress", device.getAddress());
        editor.putString("deviceName", device.getName());

        editor.commit();
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
        scanForHRM(true);
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
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

}

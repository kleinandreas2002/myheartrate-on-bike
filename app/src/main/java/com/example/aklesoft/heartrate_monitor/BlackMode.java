package com.example.aklesoft.heartrate_monitor;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.support.constraint.Group;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Places;

import static com.example.aklesoft.heartrate_monitor.Constants.ACTION_BROADCAST_RECEIVER;
import static com.example.aklesoft.heartrate_monitor.Constants.ACTION_BROADCAST_RECEIVER_DATA;


/**
 * Created by Thunder on 12.08.2017.
 */

public class BlackMode extends Activity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {

    private final static String TAG = BlackMode.class.getSimpleName();


    private GoogleApiClient client;
    private LocationManager locationManager;
    private String provider;
    private TextView tSpeedView;
    private TextView tHRView;
    private TextView tHRPercentage;
    private TextView tClockView;
    private TextView tTimerView;
    private LinearLayout lHRViewUnit;
    private LinearLayout lSpeedView;
    private LinearLayout lHRView;

    private Location pCurrentLocation;
    private Location mLastLocation;
    private Point displaySize;
    private boolean directionDownTextView = true;
//    private Thread threadMoveText;
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;

    private Thread refreshTimerThread;
    private boolean timerrunning;
    int time = 0;


    private boolean blocationManager;

    boolean bShowSpeed;
    boolean bShowHR;
    boolean bShowClock;
    boolean bShowTimer;
    boolean bStartStopwatch;

//  BlueTooth
    private static final int SCAN_PERIOD = 10000;
    private long mTimestamp = 0;

//    BluetoothAdapter mBluetoothAdapter;
//    private BluetoothLeService mBluetoothLeService;
//    public final static UUID UUID_HEART_RATE_MEASUREMENT = UUID.fromString(SampleGattAttributes.HEART_RATE_MEASUREMENT);

//    private String HR_DeviceAddress;
//    private String HR_DeviceName;

//    public static final int MODE_DISCONNECTED = 0;
//    public static final int MODE_CONNECTING = 1;
//    public static final int MODE_CONNECTED = 2;
//    public static final int MODE_SERVICE_DISCOVERED = 3;
//    public int state = MODE_DISCONNECTED;

    public MainActivity mainActivity;



    /** Called when the activity is first created. */
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON|
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD|
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED|
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        WindowManager.LayoutParams params = getWindow().getAttributes();
        params.screenBrightness = 0.5f;
        getWindow().setAttributes(params);

        setContentView(R.layout.black_mode);

//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//            // Android M Permission check
//            if (this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
//                final AlertDialog.Builder builder = new AlertDialog.Builder(this);
//                builder.setTitle("This app needs location access");
//                builder.setMessage("Please grant location access so this app can detect beacons.");
//                builder.setPositiveButton(android.R.string.ok, null);
//                builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
//
//                    public void onDismiss(DialogInterface dialog) {
//                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//                            requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_COARSE_LOCATION);
//                        }
//                    }
//                });
//                builder.show();
//            }
//        }

        mainActivity = new MainActivity();

        bShowSpeed = getIntent().getExtras().getBoolean("ShowSpeed");
        bShowHR = getIntent().getExtras().getBoolean("ShowHR");
        bShowClock = getIntent().getExtras().getBoolean("ShowClock");
        bShowTimer = getIntent().getExtras().getBoolean("ShowTimer");
        bStartStopwatch = getIntent().getExtras().getBoolean( "StartStopwatch");


        Group groupSpeedView = findViewById(R.id.groupSpeedView);
        Group groupHRView = findViewById(R.id.groupHRView);

        tSpeedView = this.findViewById(R.id.SpeedView);
        tHRView = this.findViewById(R.id.HRView);
        tHRPercentage = this.findViewById(R.id.HRPercentage);
        tClockView = this.findViewById(R.id.ClockView);
        tTimerView = this.findViewById(R.id.TimerView);

        if(bShowSpeed) {

            locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            blocationManager = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);

//            lSpeedView.setVisibility(View.VISIBLE);
            groupSpeedView.setVisibility(View.VISIBLE);

            client = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(Drive.API)
                    .addApi(LocationServices.API)
                    .addApi(Places.GEO_DATA_API)
                    .addApi(Places.PLACE_DETECTION_API)
                    .build();

// check if enabled and if not send user to the GSP settings
// Better solution would be to display a dialog and suggesting to
// go to the settings
            if (!blocationManager) {
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(intent);
            }


            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                    && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;

            }
            Criteria criteria = new Criteria();
            provider = locationManager.getBestProvider(criteria, true);
            locationManager.requestLocationUpdates(provider, 1, 0, this);

            Location location = locationManager.getLastKnownLocation(provider);

            onLocationChanged(location);

        }
        else
        {
//            lSpeedView.setVisibility(View.GONE);
            groupSpeedView.setVisibility(View.GONE);

        }
        if(bShowHR) {
//            lHRView.setVisibility(View.VISIBLE);
            groupHRView.setVisibility(View.VISIBLE);
//            int hrdata = getIntent().getExtras().getInt("HRData");
//            Log.d(TAG, String.format("Received heart rate: %d", hrdata));
//            runOnUiThread(new Runnable() {

//                @Override
//                public void run() {
//                     Stuff that updates the UI
//                    tHRView.setText(Integer.toString(hrdata) );

//                }
//            });

        }
        else
        {
//            lHRView.setVisibility(View.GONE);
            groupHRView.setVisibility(View.GONE);
        }
        if(bShowClock) {
            tClockView.setVisibility(View.VISIBLE);
        }
        else
        {
            tClockView.setVisibility(View.GONE);
        }
        if(bShowTimer) {
            tTimerView.setVisibility(View.VISIBLE);
            if( bStartStopwatch ) {
                StartTimer(getWindow().getDecorView().getRootView());
            }
        }
        else
        {
            tTimerView.setVisibility(View.GONE);
        }

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        getDisplaySize();

// change speed textview position
//        moveSpeedTextView();
// AKL - thread for ui change all 2 seconds
//        threadMoveText = new Thread() {
//            @Override
//            public void run() {
//                try {
//                    while (!isInterrupted()) {
//                        Thread.sleep(2000);
//                        runOnUiThread(new Runnable() {
//                            @Override
//                            public void run() {
//                                moveSpeedTextView();
//                            }
//                        });
//                    }
//                } catch (InterruptedException e) {
//                }
//            }
//        };
//        threadMoveText.start();
    }

//    private boolean initBt() {
//
//        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
//            Toast.makeText(this, "Bluetooth not found", Toast.LENGTH_SHORT).show();
//            return false;
//        }
//
//        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
//
//        mBluetoothAdapter = bluetoothManager.getAdapter();
//
//        if (mBluetoothAdapter == null) {
//            Toast.makeText(this, "LE Bluetooth not supported", Toast.LENGTH_SHORT);
//            return false;
//        }
//
//        if (!mBluetoothAdapter.isEnabled()) {
//            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
//            startActivityForResult(enableBtIntent, 1);
//        }
//
//        return true;
//
//    }

    private void moveSpeedTextView() {

        float cY = tSpeedView.getY();
        float maxY = displaySize.y-500;
        float deltaY = 100;

//        Log.d("\">>>>>>>>>>>>>>>>>>>>>>>>>>AKL <01> start : ", String.valueOf(cY) );
//        Log.d("\">>>>>>>>>>>>>>>>>>>>>>>>>>AKL <01> start : ", String.valueOf(maxY) );

        if( directionDownTextView == true && cY < maxY && cY >= 0 )
        {
            textAnimation();
            tSpeedView.setY(cY+deltaY);
        }
        else if ( directionDownTextView == false && cY < maxY && cY >= 0  )
        {
            textAnimation();
            tSpeedView.setY(cY-deltaY);
        }

        if( directionDownTextView == true )
        {
            if( tSpeedView.getY()+deltaY >= maxY )
                directionDownTextView = false;
        }
        else if( directionDownTextView == false )
        {
            if( tSpeedView.getY()-deltaY <= 0 )
                directionDownTextView = true;
        }

    }

    private void textAnimation(){
        Animation a = AnimationUtils.loadAnimation(BlackMode.this, R.anim.blink);
        tSpeedView.startAnimation(a);
    }

    private void getDisplaySize() {
        Display display = getWindowManager().getDefaultDisplay();
        displaySize = new Point();
        display.getSize(displaySize);

    }
//    public void moveSpeedTextView() {
//        Animation a;
////        while(true) {
//            a = AnimationUtils.loadAnimation(this, R.anim.move);
//            a.reset();
//            tSpeedView.clearAnimation();
//            tSpeedView.startAnimation(a);
//            a = AnimationUtils.loadAnimation(this, R.anim.move_back);
//            tSpeedView.clearAnimation();
//            tSpeedView.startAnimation(a);
////        }
//    }
//    public void moveSpeedTextView() {
//      class Timer implements Runnable {
//
//            @Override public void run() {
//                while (true) {
//                    try {
//                        Thread.sleep(2000);
//                        Log.d("\">>>>>>>>>>>>>>>>>>>>>>>>>>AKL <01> start",workthread.toString() );
//
//
////                        tSpeedView.setY(tSpeedView.getTop()+100);
//
////                        Animation a = AnimationUtils.loadAnimation(BlackMode.this, android.R.anim.fade_in);
////                        a.reset();
////                        tSpeedView.clearAnimation();
////                        tSpeedView.startAnimation(a);
//                    } catch (InterruptedException e) {
//                    }
//                }
//            }
//
//
//        }
//        workthread = new Thread( new Timer() );
//        workthread.start();
////        Log.d("\">>>>>>>>>>>>>>>>>>>>>>>>>>AKL <01> start",workthread.toString() );
//
//    }


    @Override
    protected void onResume() {
        super.onResume();
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        if (blocationManager) {
            locationManager.requestLocationUpdates(provider, 1, 0, this);
        }

        if(bShowHR) {
            Log.e(TAG, "registerReceiver");
            registerReceiver(broadcastReceiver, new MainActivity().broadcastReceiverUpdateIntentFilter());
//            Log.e(TAG, "scanForHRM");
//            scanForHRM(true);
        }


    }

    @Override
    protected void onPause() {
        super.onPause();
        if(blocationManager) {
            locationManager.removeUpdates(this);
        }

        if(bShowHR) {
//            setHrValues(Integer.valueOf(intent.getStringExtra(mBluetoothLeService.EXTRA_DATA)));
            unregisterReceiver(broadcastReceiver);
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
//        if(client != null) {
//            client.connect();
//        }
//        this.onLocationChanged(null);
//        if(bShowHR) {
//            Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
//            startService(gattServiceIntent);
//            Log.e(TAG, "bindService");
//            bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
//        }
    }

    @Override
    public void onStop() {
        super.onStop();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.

//        client.disconnect();

    }

//    private void scanForHRM(final boolean enable) {
//
//        if (enable) {
//
//            if (HR_DeviceAddress == null) {
//                startTimeCounter();
//                mBluetoothAdapter.startLeScan(mLeScanCallback);
//            } else {
//                Log.i(TAG, "Connect to stored device: " + HR_DeviceAddress);
//                connectDevice();
//            }
//        } else {
//
//            // mScanning = false;
//            mBluetoothAdapter.stopLeScan(mLeScanCallback);
//            if (mBluetoothLeService != null) {
//                mBluetoothLeService.disconnect();
//                mBluetoothLeService.close();
//            }
//        }
//    }

//    private void connectDevice() {
//        if (mBluetoothLeService != null) {
//            Log.e(TAG, "Connect to "+HR_DeviceAddress);
//            final boolean result = mBluetoothLeService.connect(HR_DeviceAddress);
//            Log.e(TAG, "Connect request result=" + result);
//
//        }
//    }

//    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
//        @Override
//        public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
//
//            runOnUiThread(new Runnable() {
//                @Override
//                public void run() {
//                    Log.i(TAG, "Device Found: " + device.getName() + " / " + device.getAddress());
//
//                    // async call to check for HRM interface
//                    checkForHrmService(device.getAddress());
////                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
//
//                    if (isTimeAlready(SCAN_PERIOD)) {
//                        mBluetoothAdapter.stopLeScan(mLeScanCallback);
//                        Log.i(TAG, "Stop BT scan. timeout");
//
//                    }
//                }
//            });
//
//        }
//    };

    private void startTimeCounter() {
        mTimestamp = System.currentTimeMillis();
    }
    private boolean isTimeAlready(int mkSeconds) {
        return System.currentTimeMillis() - mTimestamp > mkSeconds ? true : false;
    }

//    private void checkForHrmService(String address) {
//        Log.i(TAG, "ASYCN CHECK FOR HRM SERVICE");
//
//        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
//
//        BluetoothGatt mBluetoothGatt = device.connectGatt(this, true, new BluetoothGattCallback() {
//
//            @Override
//            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
//                super.onConnectionStateChange(gatt, status, newState);
//                Log.i(TAG, "LOCAL BT SERVICE STATE CHANGE: " + newState);
//
//                if (newState == BluetoothProfile.STATE_CONNECTED) {
//                    if (gatt != null) {
//                        Log.i(TAG, "Connected to GATT Server");
//                        Log.i(TAG, "Attempting to start service discovery: " + gatt.discoverServices());
//
//                    }
//                }
//                else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
//                    Log.i(TAG, "Disconnected");
//                    scanForHRM(true);
////                    setHrValue("Disconnected");
//                }
//
//            }
//
//            @Override
//            public void onServicesDiscovered(BluetoothGatt gatt, int status) {
//                super.onServicesDiscovered(gatt, status);
//
//                Log.i(TAG, "Asyn gatt discovered!!");
//
//                for (BluetoothGattService gattService : gatt.getServices()) {
//
//                    for(BluetoothGattCharacteristic gattCharacteristic : gattService.getCharacteristics()) {
//                        if (UUID_HEART_RATE_MEASUREMENT.equals(gattCharacteristic.getUuid())) {
//                            Log.i(TAG, "HRM service found!!");
//
//                            runOnUiThread(new Runnable() {
//                                @Override
//                                public void run() {
//
//                                    HR_DeviceAddress = device.getAddress();
//                                    HR_DeviceName = device.getName();
//                                    saveDevice(device);
//                                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
//                                    connectDevice();
//
//                                }
//                            });
//                        }
//
//                    }
//
//                }
//
//            }
//        });
//
//        mBluetoothGatt.connect();
//    }

    private void saveDevice(BluetoothDevice device) {
        SharedPreferences settings = getSharedPreferences("Heartrate_Monitor", 0);
        SharedPreferences.Editor editor = settings.edit();

        editor.putString("deviceAddress", device.getAddress());
        editor.putString("deviceName", device.getName());

        editor.commit();
    }

//    private final ServiceConnection mServiceConnection = new ServiceConnection() {
//        @Override
//        public void onServiceConnected(ComponentName name, IBinder service) {
//            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
//
//            if (mBluetoothLeService.isRunning) {
////                switchConnection.setChecked(true);
//                    Log.e(TAG, "Reading Heart Rate");
////                labelStatus.setText("Reading Heart Rate");
////                labelValue.setText("...");
//            }
//            if (!mBluetoothLeService.initialize()) {
//                Log.e(TAG, "Unable to initialize Bluetooth");
//                finish();
//            }
//        }
//
//        @Override
//        public void onServiceDisconnected(ComponentName name) {
//            mBluetoothLeService = null;
//        }
//    };


    BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.e(TAG, String.format("BroadcastReceiver -> onReceive"));

            if(intent.getAction().equals(ACTION_BROADCAST_RECEIVER)){
//                Log.e(TAG, String.format("ACTION_BROADCAST_RECEIVER: "+ ACTION_BROADCAST_RECEIVER));
//                Log.e(TAG, String.format("ACTION_BROADCAST_RECEIVER_DATA: "+ intent.getAction()));
//                Log.e(TAG, String.format("ACTION_BROADCAST_RECEIVER_DATA: "+ intent.getStringExtra(ACTION_BROADCAST_RECEIVER_DATA)));
//                Log.e(TAG, String.format("ACTION_BROADCAST_RECEIVER_DATA: %d", Integer.valueOf(intent.getStringExtra(ACTION_BROADCAST_RECEIVER_DATA))));
                setHrValues(Integer.valueOf(intent.getStringExtra(ACTION_BROADCAST_RECEIVER_DATA)));
            }
        }
    };
//    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
//        @Override
//        public void onReceive(Context context, Intent intent) {
//
//            final String action = intent.getAction();
//
//            Log.i(TAG, "BTS Callback action: " + action);
//
//            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
//                tHRView.setText("...");
//                //ToDo
//            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
////                setActivityState(MODE_DISCONNECTED);
//                //ToDo
//            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
//                //ToDo
//                if (mBluetoothLeService != null) {
//                    tHRView.setText("...process...");
//                    mBluetoothLeService.turnHRMNotification();
////                    setActivityState(MODE_SERVICE_DISCOVERED);
//                }
//
//            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
//                if (state != MODE_CONNECTED) {
////                    setActivityState(MODE_CONNECTED);
//                }
//
//                Log.i(TAG, "HRM: " + intent.getStringExtra(mBluetoothLeService.EXTRA_DATA));
//                setHrValues(Integer.valueOf(intent.getStringExtra(mBluetoothLeService.EXTRA_DATA)));
//
////                viewProgress.updateHrValue(Integer.valueOf(intent.getStringExtra(mBluetoothLeService.EXTRA_DATA)));
////                viewGauge.updateHrValue(Integer.valueOf(intent.getStringExtra(mBluetoothLeService.EXTRA_DATA)));
////                viewChart.invalidate();
//
//                //ToDo
//            }
//
//        }
//    };


    public void setHrValues(int hrData) {
        int iPercentage = (int) ((hrData*100.0f)/195);

        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                // Stuff that updates the UI
                tHRView.setText(Integer.toString(hrData));
                tHRPercentage.setText(Integer.toString(iPercentage));
            }
        });
    }

//    public void setHrValue(final String hrData) {
//        runOnUiThread(new Runnable() {
//            @Override
//            public void run() {
//                tHRView.setText(hrData);
////                lHRViewUnit.setVisibility(getWindow().getDecorView().getRootView().GONE);
//            }
//        });
//    }

//    private static IntentFilter makeGattUpdateIntentFilter() {
//
//        final IntentFilter intentFilter = new IntentFilter();
//
//        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
//        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
//        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
//        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
//
//        return intentFilter;
//
//    }

    @Override
    public void onLocationChanged(Location location) {


        if( location == null)
        {
            tSpeedView.setText("--");
        }
        else
        {
            this.pCurrentLocation = location;
            float calcSpeed = 0.0f;
            if (this.mLastLocation != null)
            {
                calcSpeed = (float) (Math.sqrt(
                        Math.pow(pCurrentLocation.getLongitude() - mLastLocation.getLongitude(), 2)
                                + Math.pow(pCurrentLocation.getLatitude() - mLastLocation.getLatitude(), 2)
                ) / (pCurrentLocation.getTime() - mLastLocation.getTime()));
            }
            //if there is speed from location
            if (pCurrentLocation.hasSpeed())
            {
                //get location speed
                calcSpeed = pCurrentLocation.getSpeed();
                calcSpeed = (calcSpeed * 3.6f);
                float roundcalcSpeed = Math.round(calcSpeed*10.0f)/10.0f;
                this.mLastLocation = pCurrentLocation;
                tSpeedView.setText("" + roundcalcSpeed);

            }
        }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }

    @Override
    public void onConnected(Bundle bundle) {

    }

    @Override
    public void onConnectionSuspended(int i) {

    }



    public void StartTimer(View view) {
        if (!timerrunning) {
            timerrunning = true;
            initTimer();

        } else {
            timerrunning = false;
            refreshTimerThread.interrupt();
            refreshTimerThread = null;
        }

    }

    public void initTimer() {
        refreshTimerThread = new Thread(new Runnable() {
            public void run() {
                while (timerrunning) {
                    time = time + 1;
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ex) {
//                        Logger.getLogger(MainActivity.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    runOnUiThread(new Runnable() {
                        public void run() {
                            tTimerView.setText( String.format("%02d:%02d:%02d", (time / 3600),((time % 3600) / 60), (time % 60) ) );
//                            tTimerView.setText( String.format(getString(R.string.timer_value),"%d:%d:%d", time / 3600,(time % 3600) / 60, (time % 60)));
                        }
                    });

                }
            }
        });
        refreshTimerThread.start();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
//        if(threadMoveText != null) {
//            threadMoveText.interrupt();
//            threadMoveText = null;
//        }

        if(refreshTimerThread != null) {
            refreshTimerThread.interrupt();
            refreshTimerThread = null;
        }

        if(client != null){
            client.disconnect();
        }

//        if(bShowHR) {
//            unbindService(mServiceConnection);
//        }
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }
}


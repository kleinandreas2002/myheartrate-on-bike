package com.example.aklesoft.heartrate_monitor;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.constraint.Group;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Places;

import java.util.Locale;

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
    private TextView tTimerView;


    private Location mLastLocation;

    private Thread refreshTimerThread;
    private boolean timerRunning;
    private int time = 0;

    private boolean bLocationManager;

    private boolean bShowHR;

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

        mainActivity = new MainActivity();

        boolean bShowStopwatch;
        boolean bStartStopwatch;
        boolean bShowSpeed;
        boolean bShowClock;

        int iBlackModeOrientation;

        bShowStopwatch = getIntent().getExtras().getBoolean("ShowStopwatch", false);
        Log.d(TAG, "BlackMode -> onCreate -> bShowStopwatch ->"+ bShowStopwatch);

        bStartStopwatch = getIntent().getExtras().getBoolean( "StartStopwatch", false);
        Log.d(TAG, "BlackMode -> onCreate -> bStartStopwatch ->"+ bStartStopwatch);

        bShowSpeed = getIntent().getExtras().getBoolean("ShowSpeed", false);
        Log.d(TAG, "BlackMode -> onCreate -> bShowSpeed ->"+ bShowSpeed);

        this.bShowHR = getIntent().getExtras().getBoolean("ShowHR", false);
        Log.d(TAG, "BlackMode -> onCreate -> bShowHR ->"+ this.bShowHR);

        bShowClock = getIntent().getExtras().getBoolean("ShowClock", false);
        Log.d(TAG, "BlackMode -> onCreate -> bShowClock ->"+ bShowClock);


        iBlackModeOrientation = getIntent().getExtras().getInt( "BlackModeOrientation");
        Log.d(TAG, "BlackMode -> onCreate -> BlackModeOrientation ->"+ iBlackModeOrientation);

        TextView tClockView = this.findViewById(R.id.ClockView);
        TextView tSpeedViewUnit = this.findViewById(R.id.SpeedViewUnit);
        TextView tHRViewUnit = this.findViewById(R.id.HRViewUnit);
        TextView tHRPercentageUnit = this.findViewById(R.id.HRPercentageUnit);

        tSpeedView = this.findViewById(R.id.SpeedView);
        tHRView = this.findViewById(R.id.HRView);
        tHRPercentage = this.findViewById(R.id.HRPercentage);
        tTimerView = this.findViewById(R.id.TimerView);

        if(bShowSpeed) {

            locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            if (locationManager == null) throw new AssertionError("Object cannot be null");
            bLocationManager = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);

            tSpeedView.setVisibility(View.VISIBLE);
            tSpeedViewUnit.setVisibility(View.VISIBLE);

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
            if (!bLocationManager) {
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
//                return;
            }

            Criteria criteria = new Criteria();
            provider = locationManager.getBestProvider(criteria, true);

            if ( provider == null ) {
                Log.d( TAG, "No location provider found!" );
            }
            else {
                locationManager.requestLocationUpdates(provider, 1, 0, this);

                Location location = locationManager.getLastKnownLocation(provider);

                onLocationChanged(location);
            }

        }
        else
        {
            tSpeedView.setVisibility(View.GONE);
            tSpeedViewUnit.setVisibility(View.GONE);

        }

        if(this.bShowHR) {
            tHRView.setVisibility(View.VISIBLE);
            tHRViewUnit.setVisibility(View.VISIBLE);
            tHRPercentage.setVisibility(View.VISIBLE);
            tHRPercentageUnit.setVisibility(View.VISIBLE);
        }
        else
        {
            tHRView.setVisibility(View.GONE);
            tHRViewUnit.setVisibility(View.GONE);
            tHRPercentage.setVisibility(View.GONE);
            tHRPercentageUnit.setVisibility(View.GONE);        }

        if(bShowClock) {
            tClockView.setVisibility(View.VISIBLE);
        }
        else
        {
            tClockView.setVisibility(View.GONE);
        }

        if(bShowStopwatch) {
            tTimerView.setVisibility(View.VISIBLE);
            if( bStartStopwatch ) {
                StartTimer(getWindow().getDecorView().getRootView());
            }
        }
        else
        {
            tTimerView.setVisibility(View.GONE);
        }


        setRequestedOrientation(iBlackModeOrientation);
        getDisplaySize();
    }


    private void getDisplaySize() {
        Display display = getWindowManager().getDefaultDisplay();
        Point displaySize = new Point();
        display.getSize(displaySize);

    }


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
        if (bLocationManager) {
            locationManager.requestLocationUpdates(provider, 1, 0, this);
        }

        if(this.bShowHR) {
            if(getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
                try {
                    Log.e(TAG, "registerReceiver");
                    registerReceiver(broadcastReceiver, new MainActivity().broadcastReceiverUpdateIntentFilter());
                } catch (Exception e) {
                    Log.e(TAG, "broadcastReceiver isn't registered!");
                }
            }
        }


    }

    @Override
    protected void onPause() {
        super.onPause();
        if(bLocationManager) {
            locationManager.removeUpdates(this);
        }

        if(this.bShowHR) {
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

    }


    BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.e(TAG, "BroadcastReceiver -> onReceive");

            if(intent.getAction().equals(ACTION_BROADCAST_RECEIVER)){
//                Log.d(TAG, String.format("ACTION_BROADCAST_RECEIVER: "+ ACTION_BROADCAST_RECEIVER));
//                Log.d(TAG, String.format("ACTION_BROADCAST_RECEIVER_DATA: "+ intent.getAction()));
//                Log.d(TAG, String.format("ACTION_BROADCAST_RECEIVER_DATA: "+ intent.getStringExtra(ACTION_BROADCAST_RECEIVER_DATA)));
//                Log.d(TAG, String.format("ACTION_BROADCAST_RECEIVER_DATA: %d", Integer.valueOf(intent.getStringExtra(ACTION_BROADCAST_RECEIVER_DATA))));
                setHrValues(Integer.valueOf(intent.getStringExtra(ACTION_BROADCAST_RECEIVER_DATA)));
            }
        }
    };


    public void setHrValues(int hrData) {
        int iPercentage = (int) ((hrData*100.0f)/195);

        runOnUiThread(() -> {
            // Stuff that updates the UI
            mainActivity.setTextFieldTexts(tHRView, Integer.toString(hrData));
            mainActivity.setTextFieldTexts(tHRPercentage, Integer.toString(iPercentage));
        });
    }


    @Override
    public void onLocationChanged(Location location) {

        Location pCurrentLocation;

        if( location == null)
        {
            tSpeedView.setText("--");
        }
        else
        {
            pCurrentLocation = location;
            float calcSpeed = 0.0f;
            if (this.mLastLocation != null) {
                //TODO
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
                String text = "" + roundcalcSpeed;
                tSpeedView.setText(text);

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
        if (!timerRunning) {
            timerRunning = true;
            initTimer();

        } else {
            timerRunning = false;
            refreshTimerThread.interrupt();
            refreshTimerThread = null;
        }

    }

    public void initTimer() {
        refreshTimerThread = new Thread(() -> {
            while (timerRunning) {
                this.time = this.time + 1;
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ex) {
//                        Logger.getLogger(MainActivity.class.getName()).log(Level.SEVERE, null, ex);
                }
                runOnUiThread(() -> tTimerView.setText( String.format(Locale.getDefault(),"%02d:%02d:%02d", (this.time / 3600),((this.time % 3600) / 60), (this.time % 60) ) ));

            }
        });
        refreshTimerThread.start();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();

        if(refreshTimerThread != null) {
            refreshTimerThread.interrupt();
            refreshTimerThread = null;
        }

        if(client != null){
            client.disconnect();
        }

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }
}


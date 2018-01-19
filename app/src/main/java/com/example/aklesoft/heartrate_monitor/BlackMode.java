package com.example.aklesoft.heartrate_monitor;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.view.Display;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Places;


/**
 * Created by Thunder on 12.08.2017.
 */

public class BlackMode extends Activity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {

    private GoogleApiClient client;
    private LocationManager locationManager;
    private String provider;
    private TextView tSpeedView;
    private TextView tHRView;
    private TextView tClockView;
    private TextView tTimerView;

    private Location pCurrentLocation;
    private Location mLastLocation;
    private Thread workthread;
    private Point displaySize;
    private boolean directionDownTextView = true;
    private Thread threadMoveText;
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;

    private Thread refreshTimerThread;


    private boolean timerrunning;
    private boolean blocationManager;

    int time = 0;


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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android M Permission check
            if (this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("This app needs location access");
                builder.setMessage("Please grant location access so this app can detect beacons.");
                builder.setPositiveButton(android.R.string.ok, null);
                builder.setOnDismissListener(new DialogInterface.OnDismissListener() {

                    public void onDismiss(DialogInterface dialog) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_COARSE_LOCATION);
                        }
                    }
                });
                builder.show();
            }
        }

        boolean bShowSpeed = getIntent().getExtras().getBoolean("ShowSpeed");
        boolean bShowHR = getIntent().getExtras().getBoolean("ShowHR");
        boolean bShowClock = getIntent().getExtras().getBoolean("ShowClock");
        boolean bShowTimer = getIntent().getExtras().getBoolean("ShowTimer");

        tSpeedView = (TextView) this.findViewById(R.id.SpeedView);
        tHRView = (TextView) this.findViewById(R.id.HRView);
        tClockView = (TextView) this.findViewById(R.id.ClockView);
        tTimerView = (TextView) this.findViewById(R.id.TimerView);

        if(bShowSpeed) {
            tSpeedView.setVisibility(View.VISIBLE);

            client = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(Drive.API)
                    .addApi(LocationServices.API)
                    .addApi(Places.GEO_DATA_API)
                    .addApi(Places.PLACE_DETECTION_API)
                    .build();

            locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            blocationManager = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
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
            tSpeedView.setVisibility(View.GONE);
        }
        if(bShowHR) {
            tHRView.setVisibility(View.VISIBLE);
        }
        else
        {
            tHRView.setVisibility(View.GONE);
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
        }
        else
        {
            tTimerView.setVisibility(View.GONE);
        }

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

    }

    @Override
    protected void onPause() {
        super.onPause();
        if(blocationManager) {
            locationManager.removeUpdates(this);
        }

    }

    @Override
    public void onStart() {
        super.onStart();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        if(client != null) {
            client.connect();
        }
//        this.onLocationChanged(null);
    }

    @Override
    public void onStop() {
        super.onStop();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.

//        client.disconnect();
    }


    @Override
    public void onLocationChanged(Location location) {


        if( location == null)
        {
            tSpeedView.setText("-- kmh");
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
    public void onConnected(@Nullable Bundle bundle) {

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

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
        if(threadMoveText != null) {
            threadMoveText.interrupt();
            threadMoveText = null;
        }
        if(refreshTimerThread != null) {
            refreshTimerThread.interrupt();
            refreshTimerThread = null;
        }
        if(client != null){
            client.disconnect();
        }

    }

}

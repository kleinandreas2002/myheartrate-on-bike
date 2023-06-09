package com.example.aklesoft.heartrate_monitor;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.VectorDrawable;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Places;

import org.osmdroid.api.IMapController;
import org.osmdroid.bonuspack.kml.KmlDocument;
import org.osmdroid.bonuspack.kml.KmlFeature;
import org.osmdroid.bonuspack.kml.KmlLineString;
import org.osmdroid.bonuspack.kml.KmlMultiGeometry;
import org.osmdroid.bonuspack.kml.KmlPlacemark;
import org.osmdroid.bonuspack.kml.KmlPoint;
import org.osmdroid.bonuspack.kml.KmlPolygon;
import org.osmdroid.bonuspack.kml.KmlTrack;
import org.osmdroid.bonuspack.kml.Style;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.FolderOverlay;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Overlay;
import org.osmdroid.views.overlay.Polygon;
import org.osmdroid.views.overlay.Polyline;
import org.osmdroid.views.overlay.compass.CompassOverlay;
import org.osmdroid.views.overlay.compass.InternalCompassOrientationProvider;
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay;
import org.osmdroid.views.overlay.milestones.MilestoneManager;
import org.osmdroid.views.overlay.milestones.MilestoneMeterDistanceLister;
import org.osmdroid.views.overlay.milestones.MilestonePathDisplayer;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
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

    private ImageView ivImageSetPosition;

    LinearLayout lNavigatorLayout;
    LinearLayout lDataLayout;

    private Location mLastLocation;
    private Location currentGpsPosition;
    private GeoPoint gpCurrentGpsPosition;

    private Thread refreshTimerThread;
    private boolean timerRunning;
    private int time = 0;

    private boolean bLocationManager;

    private boolean bShowHR;
    private boolean bShowSpeed;
    private boolean bShowNavigator;
    private boolean bMapColorMode;
    private boolean bMapOfflineMode;
    private boolean bMapDirectionArrows;
    protected boolean mTrackingMode;


    public MainSettingsActivity mainSettingsActivity;

//    Navigation navigation = new Navigation(new Navigation.NavigatorViewCallback() {
//        @Override
//        public void updateNavigatorView(String newString) {
//            ((TextView)findViewById(R.id.NavigatorView)).setText(newString);
//        }
//    });

    float mAzimuthAngleSpeed = 0.0f;

    String sSelectedMap = null;
    public static KmlDocument mKmlDocument;
    KmlMultiGeometry kmlMultiGeometry;
    MapView map = null;

    /** Called when the activity is first created. */
    public void onCreate(Bundle savedInstanceState) {
        Log.e(TAG, "onCreate -> BLACKMODEBLACKMODEBLACKMODEBLACKMODEBLACKMODE");

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);


        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        WindowManager.LayoutParams params = getWindow().getAttributes();
//        params.screenBrightness = 0.5f;
//        getWindow().setAttributes(params);


        setContentView(R.layout.black_mode);

        mainSettingsActivity = new MainSettingsActivity();


        int iBlackModeOrientation;


        boolean bShowStopwatch = getIntent().getExtras().getBoolean("ShowStopwatch", false);
        Log.d(TAG, "BlackMode -> onCreate -> bShowStopwatch ->" + bShowStopwatch);

        boolean bStartStopwatch = getIntent().getExtras().getBoolean("StartStopwatch", false);
        Log.d(TAG, "BlackMode -> onCreate -> bStartStopwatch ->" + bStartStopwatch);

        bShowSpeed = getIntent().getExtras().getBoolean("ShowSpeed", false);
        Log.d(TAG, "BlackMode -> onCreate -> bShowSpeed ->" + bShowSpeed);

        bShowNavigator = getIntent().getExtras().getBoolean("ShowNavigator", false);
        Log.d(TAG, "BlackMode -> onCreate -> bShowNavigator ->" + bShowNavigator);

        bShowHR = getIntent().getExtras().getBoolean("ShowHR", false);
        Log.d(TAG, "BlackMode -> onCreate -> bShowHR ->" + bShowHR);

        boolean bShowClock = getIntent().getExtras().getBoolean("ShowClock", false);
        Log.d(TAG, "BlackMode -> onCreate -> bShowClock ->" + bShowClock);

        bMapColorMode = getIntent().getExtras().getBoolean("MapColorMode", false);
        Log.d(TAG, "BlackMode -> onCreate -> bMapColorMode ->" + bMapColorMode);

        bMapOfflineMode = getIntent().getExtras().getBoolean("MapOfflineMode", false);
        Log.d(TAG, "BlackMode -> onCreate -> bMapOfflineMode ->" + bMapOfflineMode);

        bMapDirectionArrows = getIntent().getExtras().getBoolean("MapDirectionArrows", false);
        Log.d(TAG, "BlackMode -> onCreate -> bMapDirectionArrows ->" + bMapDirectionArrows);

        iBlackModeOrientation = getIntent().getExtras().getInt("BlackModeOrientation");
        Log.d(TAG, "BlackMode -> onCreate -> BlackModeOrientation ->" + iBlackModeOrientation);

        if (getIntent().getExtras().containsKey("SelectedMaps")) {
            Log.d(TAG, "BlackMode -> onCreate -> SelectedMaps ->" + sSelectedMap);
            sSelectedMap = getIntent().getExtras().getString("SelectedMaps");
        }


        lNavigatorLayout = this.findViewById(R.id.NavigatorLayout);
        lDataLayout = this.findViewById(R.id.DataLayout);

        TextView tClockView = this.findViewById(R.id.ClockView);
        TextView tSpeedViewUnit = this.findViewById(R.id.SpeedViewUnit);
        TextView tHRViewUnit = this.findViewById(R.id.HRViewUnit);
        TextView tHRPercentageUnit = this.findViewById(R.id.HRPercentageUnit);

//        ImageView ivImageMapDownload = this.findViewById(R.id.imageMapDownload);
//        ivImageMapDownload.setImageResource(R.drawable.ic_cloud_download_24px);

        ivImageSetPosition = this.findViewById(R.id.imageSetPositon);
        ivImageSetPosition.setImageResource(R.drawable.ic_gps_positon_24px);
        mTrackingMode = true;

        tSpeedView = this.findViewById(R.id.SpeedView);
        tHRView = this.findViewById(R.id.HRView);
        tHRPercentage = this.findViewById(R.id.HRPercentage);
        tTimerView = this.findViewById(R.id.TimerView);

        if (bShowSpeed) {

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


        } else {
            tSpeedView.setVisibility(View.GONE);
            tSpeedViewUnit.setVisibility(View.GONE);
        }

        if (bShowHR) {
            tHRView.setVisibility(View.VISIBLE);
            tHRViewUnit.setVisibility(View.VISIBLE);
            tHRPercentage.setVisibility(View.VISIBLE);
            tHRPercentageUnit.setVisibility(View.VISIBLE);
        } else {
            tHRView.setVisibility(View.GONE);
            tHRViewUnit.setVisibility(View.GONE);
            tHRPercentage.setVisibility(View.GONE);
            tHRPercentageUnit.setVisibility(View.GONE);
        }

        if (bShowClock) {
            tClockView.setVisibility(View.VISIBLE);
        } else {
            tClockView.setVisibility(View.GONE);
        }

        if (bShowNavigator && sSelectedMap != null) {
            lNavigatorLayout.setVisibility(View.VISIBLE);
        }
        else {
            lNavigatorLayout.setVisibility(View.GONE);
        }

        if (bShowStopwatch) {
            tTimerView.setVisibility(View.VISIBLE);
            if (bStartStopwatch) {
                StartTimer(getWindow().getDecorView().getRootView());
            }
        } else {
            tTimerView.setVisibility(View.GONE);
        }

        if (!bShowSpeed && !bShowHR && !bShowStopwatch) {
            lDataLayout.setVisibility(View.GONE);
        }

        setRequestedOrientation(iBlackModeOrientation);
    }


    @Override
    public void onStart() {
        Log.e(TAG, "onStart -> BLACKMODEBLACKMODEBLACKMODEBLACKMODEBLACKMODE");
        super.onStart();

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

        if (bShowSpeed) {
            Criteria criteria = new Criteria();
            provider = locationManager.getBestProvider(criteria, true);

            if (provider == null) {
                Log.d(TAG, "No location provider found!");
            } else {
                locationManager.requestLocationUpdates(provider, 1, 0, this);

                currentGpsPosition = locationManager.getLastKnownLocation(provider);
            }
        }

        if (bShowNavigator && sSelectedMap != null) {

            Configuration.getInstance().setUserAgentValue(BuildConfig.APPLICATION_ID);

            gpCurrentGpsPosition = new GeoPoint(locationManager.getLastKnownLocation(provider));

            map = this.findViewById(R.id.NavigatorMap);
            map.setTileSource(TileSourceFactory.MAPNIK);

            if (bMapColorMode) {
//                map.getOverlayManager().getTilesOverlay().setColorFilter(TilesOverlay.INVERT_COLORS);

                ColorMatrix inverseMatrix = new ColorMatrix(new float[] {
                        0.0f, 0.0f, 0.0f, 0.0f, 255f,
                        0.0f, 0.0f, 0.0f, 0.0f, 255f,
                        0.0f, 0.0f, 0.0f, 0.0f, 255f,
                        0.0f, 0.0f, 0.0f, 1.0f, 0.0f
                });

                int destinationColor = Color.parseColor("#FF2A2A2A");
                float lr = (255.0f - Color.red(destinationColor))/255.0f;
                float lg = (255.0f - Color.green(destinationColor))/255.0f;
                float lb = (255.0f - Color.blue(destinationColor))/255.0f;
                ColorMatrix grayscaleMatrix = new ColorMatrix(new float[] {
                        0.0f, 0.0f, 0.0f, 0.0f, 255f,
                        0.0f, 0.0f, 0.0f, 0.0f, 255f,
                        0.0f, 0.0f, 0.0f, 0.0f, 255f,
                        0.0f, 0.0f, 0.0f, 1.0f, 0.0f
                });
                grayscaleMatrix.preConcat(inverseMatrix);
                int dr = Color.red(destinationColor);
                int dg = Color.green(destinationColor);
                int db = Color.blue(destinationColor);
                float drf = 0;
                float dgf = 0;
                float dbf = 0;
                ColorMatrix tintMatrix = new ColorMatrix(new float[] {
                        drf, 0, 0, 0, 0, //
                        0, dgf, 0, 0, 0, //
                        0, 0, dbf, 0, 0, //
                        0, 0, 0, 1, 0, //
                });
                tintMatrix.preConcat(grayscaleMatrix);
                float lDestination = drf * lr + dgf * lg + dbf * lb;
                float scale = 1f - lDestination;
                float translate = 1 - scale * 0.5f;
                ColorMatrix scaleMatrix = new ColorMatrix(new float[] {
                        scale, 0, 0, 0, 0, //
                        0, scale, 0, 0, 0, //
                        0, 0, scale, 0, 0, //
                        0, 0, 0, 1, 0, //
                });
                scaleMatrix.preConcat(tintMatrix);
                ColorMatrixColorFilter filter = new ColorMatrixColorFilter(scaleMatrix);
                map.getOverlayManager().getTilesOverlay().setColorFilter(filter);
            }

            if (bMapOfflineMode) {
                map.getOverlayManager().getTilesOverlay().setUseDataConnection(false);
            }

            mAzimuthAngleSpeed = currentGpsPosition.getBearing();
            map.setMapOrientation(-mAzimuthAngleSpeed);


            IMapController mapController = map.getController();
            mapController.setZoom(17.5);
            mapController.setCenter(gpCurrentGpsPosition);


            InternalCompassOrientationProvider internalCompassOrientationProvider = new InternalCompassOrientationProvider(this);
            CompassOverlay mCompassOverlay = new CompassOverlay(this, internalCompassOrientationProvider, map);
            mCompassOverlay.enableCompass(internalCompassOrientationProvider);
            map.getOverlays().add(mCompassOverlay);


            MyLocationNewOverlay mLocationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(this), map);
            mLocationOverlay.enableMyLocation();
            mLocationOverlay.setDrawAccuracyEnabled(true);
            mLocationOverlay.enableFollowLocation();
            //            Bitmap navigation_icon = getBitmap(this, R.drawable.ic_navigation_white_48dp);
            Bitmap navigation_icon = getBitmap(this, R.drawable.ic_navigation_black_48dp);
            Bitmap current_location_icon = getBitmap(this, R.drawable.ic_navigation_green_48dp);

            if (bMapColorMode) {
                navigation_icon = getBitmap(this, R.drawable.ic_navigation_red_48dp);
                current_location_icon = getBitmap(this, R.drawable.ic_navigation_blue_48dp);
            }
            //            Bitmap current_location_icon = ((BitmapDrawable)map.getContext().getResources().getDrawable(R.drawable.person)).getBitmap();
            mLocationOverlay.setDirectionArrow(current_location_icon, navigation_icon);
            map.getOverlays().add(mLocationOverlay);


            File sdcard = Environment.getExternalStorageDirectory();
            File file = new File(sSelectedMap);


            mKmlDocument = new KmlDocument();
            Log.d(TAG, "BlackMode -> onCreate -> kmlFilePath ->" + file.getAbsolutePath());
            boolean parseResult = mKmlDocument.parseKMLFile(file);
            Log.d(TAG, "BlackMode -> onCreate -> parseResult ->" + parseResult);
            Log.d(TAG, "BlackMode -> onCreate -> mKmlDocument ->" + mKmlDocument);


            RotationGestureOverlay mRotationGestureOverlay = new RotationGestureOverlay(map);
            mRotationGestureOverlay.setEnabled(true);
            map.setMultiTouchControls(true);
            map.getOverlays().add(mRotationGestureOverlay);


            // commented out -> no marker in use
            // Drawable defaultMarker = getResources().getDrawable(R.drawable.marker_kml_point);
            // Bitmap defaultBitmap = ((BitmapDrawable) defaultMarker).getBitmap();

            Style defaultStyle = new Style(null, Color.parseColor("#222F99"), 10.0f, Color.parseColor("#222F99"));
            if (bMapColorMode) {
                defaultStyle = new Style(null, Color.parseColor("#FFFFFF"), 10.0f, Color.parseColor("#FFFFFF"));
            }

            Log.d(TAG, "BlackMode -> onCreate -> defaultStyle ->" + defaultStyle);
            Log.d(TAG, "BlackMode -> onCreate -> mKmlDocument.mKmlRoot.mStyle ->" + mKmlDocument.mKmlRoot.mStyle);
            Log.d(TAG, "BlackMode -> onCreate -> mKmlDocument.mKmlRoot.getBoundingBox ->" + mKmlDocument.mKmlRoot.getBoundingBox());
            Log.d(TAG, "BlackMode -> onCreate -> mKmlDocument.mKmlRoot.mVisibility ->" + mKmlDocument.mKmlRoot.mVisibility);
            Log.d(TAG, "BlackMode -> onCreate -> mKmlDocument.mKmlRoot.mName ->" + mKmlDocument.mKmlRoot.mName);
            Log.d(TAG, "BlackMode -> onCreate -> mKmlDocument.mKmlRoot.mExtendedData ->" + mKmlDocument.mKmlRoot.mExtendedData);
            Log.d(TAG, "BlackMode -> onCreate -> mKmlDocument.mKmlRoot.mItems ->" + mKmlDocument.mKmlRoot.mItems);
            Log.d(TAG, "BlackMode -> onCreate -> mKmlDocument.mKmlRoot.mItems.size ->" + mKmlDocument.mKmlRoot.mItems.size());

// add direction arrows to route overlay
            KmlFeature.Styler arrowStyle = null;
            if (bMapDirectionArrows) {
                arrowStyle = new MyKmlStyler(defaultStyle);
            }
            FolderOverlay kmlOverlay = (FolderOverlay) mKmlDocument.mKmlRoot.buildOverlay(map, defaultStyle, arrowStyle, mKmlDocument);
//            FolderOverlay kmlOverlay = (FolderOverlay) mKmlDocument.mKmlRoot.buildOverlay(map, defaultStyle, null, mKmlDocument);
            map.getOverlays().add(kmlOverlay);
            map.invalidate();
            Log.d(TAG, "BlackMode -> onCreate -> map ->" + map);

        }
    }


    @Override
    protected void onResume() {
        Log.e(TAG, "onResume -> BLACKMODE");
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
            onLocationChanged(currentGpsPosition);
        }

        if (bShowNavigator && sSelectedMap != null) {
            map.onResume();
        }

        if (bShowHR) {
            if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
                try {
                    Log.e(TAG, "registerReceiver");
                    registerReceiver(broadcastReceiver, new MainSettingsActivity().broadcastReceiverUpdateIntentFilter());
                } catch (Exception e) {
                    Log.e(TAG, "broadcastReceiver isn't registered!");
                }
            }
        }
    }

    @Override
    protected void onPause() {
        Log.e(TAG, "onPause -> BLACKMODEBLACKMODEBLACKMODEBLACKMODEBLACKMODE");

        super.onPause();
        if (bLocationManager) {
            locationManager.removeUpdates(this);
        }

        if (bShowHR) {
            try {
                unregisterReceiver(broadcastReceiver);
            } catch (Exception e) {
                Log.e(TAG, "broadcastReceiver wasn't registered!");
            }
        }

        if (bShowNavigator && sSelectedMap != null) {
            map.onPause();
        }
    }


    @Override
    public void onStop() {
        Log.e(TAG, "onStop -> BLACKMODEBLACKMODEBLACKMODEBLACKMODEBLACKMODE");

        super.onStop();

    }

    @Override
    public void onLocationChanged(Location currentLocation) {

        Location pCurrentLocation;

//        navigation.setCurrentGpsPosition(location);
//        int mCoordinatesSize = this.kmlMultiGeometry.mItems.get(0).mCoordinates.size();
//        if (nextWaypoint <= mCoordinatesSize)
//            nextWaypoint = navigation.routeNavigation(nextWaypoint);

        if( currentLocation != null) {
            float calcSpeed = 0.0f;
            if (mLastLocation != null) {
                //TODO
                calcSpeed = (float) (Math.sqrt(
                        Math.pow(currentLocation.getLongitude() - mLastLocation.getLongitude(), 2)
                                + Math.pow(currentLocation.getLatitude() - mLastLocation.getLatitude(), 2)
                ) / (currentLocation.getTime() - mLastLocation.getTime()));
            }
            //if there is speed from location
            if (currentLocation.hasSpeed())
            {
                //get location speed
                calcSpeed = currentLocation.getSpeed();
                calcSpeed = (calcSpeed * 3.6f);
                float roundcalcSpeed = Math.round(calcSpeed*10.0f)/10.0f;
                mLastLocation = currentLocation;
                String text = "" + roundcalcSpeed;
                tSpeedView.setText(text);

            }

            if(bShowNavigator && sSelectedMap != null) {
                if (mTrackingMode) {
                    //keep the map view centered on current location:
                    gpCurrentGpsPosition = new GeoPoint(currentLocation);
                    map.getController().animateTo(gpCurrentGpsPosition);
//                    map.setMapOrientation(-mAzimuthAngleSpeed);

                    float bearing = currentLocation.getBearing();
                    float t = (360 - bearing);
                    if (t < 0) {
                        t += 360;
                    }
                    if (t > 360) {
                        t -= 360;
                    }
                    //help smooth everything out
                    t = (int) t;
                    t = t / 5;
                    t = (int) t;
                    t = t * 5;
                    map.setMapOrientation(t);

                } else {
                    //just redraw the location overlay:
                    map.invalidate();
                }

            }

        }
        else {
            tSpeedView.setText("--");
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

    @Override
    protected void onDestroy() {
        Log.e(TAG, "onDestroy -> BLACKMODEBLACKMODEBLACKMODEBLACKMODEBLACKMODE");

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
            mainSettingsActivity.setTextFieldTexts(tHRView, Integer.toString(hrData));
            mainSettingsActivity.setTextFieldTexts(tHRPercentage, Integer.toString(iPercentage));
        });
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
                time = time + 1;
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ex) {
//                        Logger.getLogger(MainActivity.class.getName()).log(Level.SEVERE, null, ex);
                }
                runOnUiThread(() -> tTimerView.setText( String.format(Locale.getDefault(),"%02d:%02d:%02d", (time / 3600),((time % 3600) / 60), (time % 60) ) ));

            }
        });
        refreshTimerThread.start();
    }


    public void onClickSetGpsPosition(View view) {
        mTrackingMode = !mTrackingMode;
        updateUIWithTrackingMode();
    }


    void updateUIWithTrackingMode(){
        if (mTrackingMode){
            ivImageSetPosition.setImageResource(R.drawable.ic_gps_positon_24px);
            map.getController().animateTo(gpCurrentGpsPosition);
            map.setMapOrientation(-mAzimuthAngleSpeed);
        } else {
            ivImageSetPosition.setImageResource(R.drawable.ic_gps_not_fixed_24px);
            map.setMapOrientation(0.0f);
        }
    }


    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private static Bitmap getBitmap(VectorDrawable vectorDrawable) {
        Bitmap bitmap = Bitmap.createBitmap(vectorDrawable.getIntrinsicWidth(),
                vectorDrawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        vectorDrawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        vectorDrawable.draw(canvas);
        return bitmap;
    }

    private static Bitmap getBitmap(Context context, int drawableId) {
        Drawable drawable = ContextCompat.getDrawable(context, drawableId);
        if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable) drawable).getBitmap();
        } else if (drawable instanceof VectorDrawable) {
            return getBitmap((VectorDrawable) drawable);
        } else {
            throw new IllegalArgumentException("unsupported drawable type");
        }
    }

    public void onClickDownloadMap(View view) {
//        CacheManager cacheManager = new CacheManager(map);
//        double zoomMin = map.getMinZoomLevel();
//        double zoomMax = map.getMaxZoomLevel()+4.0;
//        Log.d(TAG, "BlackMode -> onClickDownloadMap -> start");
////            Toast.makeText(this, getString(R.string.download_start), Toast.LENGTH_LONG).show();
//        cacheManager.downloadAreaAsync(this.getApplicationContext(), map.getBoundingBox(), (int)(zoomMin), (int) zoomMax);
//        Log.d(TAG, "BlackMode -> onClickDownloadMap -> end");

// DOWNLOAD in background
//        new tileDownlaodTask().execute();
    }

//Async task to reverse-geocode the KML point in a separate thread:
//    private class tileDownlaodTask extends AsyncTask<Void, Void, Boolean> {
//
//        protected Boolean doInBackground(Void... voids) {
//            CacheManager cacheManager = new CacheManager(map);
//            double zoomMin = map.getMinZoomLevel();
//            double zoomMax = map.getMaxZoomLevel()+4.0;
//            Log.d(TAG, "BlackMode -> onClickDownloadMap -> start");
////            Toast.makeText(this, getString(R.string.download_start), Toast.LENGTH_LONG).show();
//            CacheManager.CacheManagerTask task = cacheManager.downloadAreaAsync(getApplicationContext(), map.getBoundingBox(), (int)(zoomMin), (int) zoomMax);
////            Toast.makeText(this, "TEST context", Toast.LENGTH_LONG).show();
//
//            Log.d(TAG, "BlackMode -> onClickDownloadMap -> end");
//            return true;
//        }
//        protected void onPostExecute(String result) {
//
//        }
//    }
//
//
//    public void onClickDownloadMap(View view) {
//        MapView map = this.findViewById(R.id.NavigatorMap);
//        CacheManager cacheManager = new CacheManager(map);
//        long cacheUsage = cacheManager.currentCacheUsage() / (1024 * 1024);
//        long cacheCapacity = cacheManager.cacheCapacity() / (1024 * 1024);
//        float percent = 100.0f * cacheUsage / cacheCapacity;
//        String message = "Cache usage:\n" + cacheUsage + " Mo / " + cacheCapacity + " Mo = " + (int) percent + "%";
//        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
//    }


    class MyKmlStyler implements KmlFeature.Styler {
        Style mDefaultStyle;

        MyKmlStyler(Style defaultStyle) {
            mDefaultStyle = defaultStyle;
        }

        @Override
        public void onLineString(Polyline polyline, KmlPlacemark kmlPlacemark, KmlLineString kmlLineString) {
            //Custom styling:
//            polyline.setColor(Color.GREEN);
            polyline.setColor(mDefaultStyle.mLineStyle.mColor);
//            polyline.setWidth(Math.max(kmlLineString.mCoordinates.size() / 200.0f, 3.0f));
            polyline.setWidth(mDefaultStyle.mLineStyle.mWidth);

            final Paint arrowPaint = new Paint();

            arrowPaint.setColor(mDefaultStyle.mLineStyle.mColor);
            arrowPaint.setStrokeWidth(10.0f);
//            arrowPaint.setStyle(Paint.Style.FILL_AND_STROKE);
            arrowPaint.setStyle(Paint.Style.STROKE);
            arrowPaint.setAntiAlias(true);
            final Path arrowPath = new Path(); // a simple arrow towards the right
            arrowPath.moveTo(- 30, - 30);
            arrowPath.lineTo(30, 0);
            arrowPath.lineTo(- 30, 30);
//            arrowPath.close();
            final List<MilestoneManager> managers = new ArrayList<>();
            managers.add(new MilestoneManager(
//                    new MilestonePixelDistanceLister(200, 200),
//                    new MilestoneMiddleLister(100),
                    new MilestoneMeterDistanceLister(300),
                    new MilestonePathDisplayer(0, true, arrowPath, arrowPaint)
            ));
            polyline.setMilestoneManagers(managers);

        }

        @Override
        public void onPolygon(Polygon polygon, KmlPlacemark kmlPlacemark, KmlPolygon kmlPolygon) {
            //Keeping default styling:
            kmlPolygon.applyDefaultStyling(polygon, mDefaultStyle, kmlPlacemark, mKmlDocument, map);
        }

        @Override
        public void onTrack(Polyline polyline, KmlPlacemark kmlPlacemark, KmlTrack kmlTrack) {
            //Keeping default styling:
            kmlTrack.applyDefaultStyling(polyline, mDefaultStyle, kmlPlacemark, mKmlDocument, map);
        }

        @Override
        public void onPoint(Marker marker, KmlPlacemark kmlPlacemark, KmlPoint kmlPoint) {
            //Styling based on ExtendedData properties:
            if (kmlPlacemark.getExtendedData("maxspeed") != null)
                kmlPlacemark.mStyle = "maxspeed";
            kmlPoint.applyDefaultStyling(marker, mDefaultStyle, kmlPlacemark, mKmlDocument, map);
        }

        @Override
        public void onFeature(Overlay overlay, KmlFeature kmlFeature) {
            //If nothing to do, do nothing.
        }
    }

}


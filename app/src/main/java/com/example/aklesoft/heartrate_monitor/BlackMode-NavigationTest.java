//package com.example.aklesoft.heartrate_monitor;
//
//import android.Manifest;
//import android.annotation.TargetApi;
//import android.app.Activity;
//import android.content.BroadcastReceiver;
//import android.content.Context;
//import android.content.Intent;
//import android.content.pm.PackageManager;
//import android.content.res.TypedArray;
//import android.graphics.Bitmap;
//import android.graphics.Canvas;
//import android.graphics.Color;
//import android.graphics.ColorMatrix;
//import android.graphics.ColorMatrixColorFilter;
//import android.graphics.DashPathEffect;
//import android.graphics.Paint;
//import android.graphics.Path;
//import android.graphics.drawable.BitmapDrawable;
//import android.graphics.drawable.Drawable;
//import android.graphics.drawable.VectorDrawable;
//import android.location.Criteria;
//import android.location.Location;
//import android.location.LocationListener;
//import android.location.LocationManager;
//import android.os.Build;
//import android.os.Bundle;
//import android.os.Environment;
//import android.os.StrictMode;
//import android.provider.Settings;
//import android.support.annotation.NonNull;
//import android.support.v4.app.ActivityCompat;
//import android.support.v4.content.ContextCompat;
//import android.support.v4.content.res.ResourcesCompat;
//import android.util.Log;
//import android.view.View;
//import android.view.Window;
//import android.view.WindowManager;
//import android.widget.ImageView;
//import android.widget.LinearLayout;
//import android.widget.TextView;
//
//import com.google.android.gms.common.ConnectionResult;
//import com.google.android.gms.common.api.GoogleApiClient;
//import com.google.android.gms.drive.Drive;
//import com.google.android.gms.location.LocationServices;
//import com.google.android.gms.location.places.Places;
//
//import org.osmdroid.api.IMapController;
//import org.osmdroid.bonuspack.kml.KmlDocument;
//import org.osmdroid.bonuspack.kml.KmlFeature;
//import org.osmdroid.bonuspack.kml.KmlLineString;
//import org.osmdroid.bonuspack.kml.KmlMultiGeometry;
//import org.osmdroid.bonuspack.kml.KmlPlacemark;
//import org.osmdroid.bonuspack.kml.KmlPoint;
//import org.osmdroid.bonuspack.kml.KmlPolygon;
//import org.osmdroid.bonuspack.kml.KmlTrack;
//import org.osmdroid.bonuspack.kml.Style;
//import org.osmdroid.bonuspack.routing.OSRMRoadManager;
//import org.osmdroid.bonuspack.routing.Road;
//import org.osmdroid.bonuspack.routing.RoadManager;
//import org.osmdroid.bonuspack.routing.RoadNode;
//import org.osmdroid.config.Configuration;
//import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
//import org.osmdroid.util.GeoPoint;
//import org.osmdroid.views.MapView;
//import org.osmdroid.views.overlay.FolderOverlay;
//import org.osmdroid.views.overlay.Marker;
//import org.osmdroid.views.overlay.Overlay;
//import org.osmdroid.views.overlay.Polygon;
//import org.osmdroid.views.overlay.Polyline;
//import org.osmdroid.views.overlay.compass.CompassOverlay;
//import org.osmdroid.views.overlay.compass.InternalCompassOrientationProvider;
//import org.osmdroid.views.overlay.gestures.RotationGestureOverlay;
//import org.osmdroid.views.overlay.infowindow.BasicInfoWindow;
//import org.osmdroid.views.overlay.infowindow.MarkerInfoWindow;
//import org.osmdroid.views.overlay.milestones.MilestoneManager;
//import org.osmdroid.views.overlay.milestones.MilestoneMeterDistanceLister;
//import org.osmdroid.views.overlay.milestones.MilestoneMiddleLister;
//import org.osmdroid.views.overlay.milestones.MilestonePathDisplayer;
//import org.osmdroid.views.overlay.milestones.MilestonePixelDistanceLister;
//import org.osmdroid.views.overlay.milestones.MilestoneStep;
//import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
//import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;
//
//import java.io.File;
//import java.sql.Array;
//import java.util.ArrayList;
//import java.util.Iterator;
//import java.util.List;
//import java.util.Locale;
//import java.util.concurrent.Executor;
//import java.util.concurrent.ScheduledThreadPoolExecutor;
//
//import static com.example.aklesoft.heartrate_monitor.Constants.ACTION_BROADCAST_RECEIVER;
//import static com.example.aklesoft.heartrate_monitor.Constants.ACTION_BROADCAST_RECEIVER_DATA;
//
//
///**
// * Created by Thunder on 12.08.2017.
// */
//
//public class BlackMode extends Activity implements GoogleApiClient.ConnectionCallbacks,
//        GoogleApiClient.OnConnectionFailedListener,
//        LocationListener {
//
//    private final static String TAG = BlackMode.class.getSimpleName();
//
//
//    private GoogleApiClient client;
//    private LocationManager locationManager;
//    private String provider;
//    private TextView tSpeedView;
//    private TextView tHRView;
//    private TextView tHRPercentage;
//    private TextView tTimerView;
//
//    private ImageView ivImageSetPosition;
//
//    LinearLayout lNavigatorLayout;
//    LinearLayout lDataLayout;
//
//    private Location mLastLocation;
//    private Location currentGpsPosition;
//    private GeoPoint gpCurrentGpsPosition;
//
//    private Thread refreshTimerThread;
//    private boolean timerRunning;
//    private int time = 0;
//
//    private boolean bLocationManager;
//
//    private boolean bShowHR;
//    private boolean bShowNavigator;
//    private boolean bMapColorMode;
//    private boolean bMapOfflineMode;
//    protected boolean mTrackingMode;
//
//
//    public MainSettingsActivity mainSettingsActivity;
//
////    Navigation navigation = new Navigation(new Navigation.NavigatorViewCallback() {
////        @Override
////        public void updateNavigatorView(String newString) {
////            ((TextView)findViewById(R.id.NavigatorView)).setText(newString);
////        }
////    });
//
//    float mAzimuthAngleSpeed = 0.0f;
//
//    String sSelectedMap = null;
//    public static KmlDocument mKmlDocument;
//
//    protected Polyline[] mRoadOverlays;
//    Road[] mRoads;
//    Road mRoad;
//    FolderOverlay mRoadNodeMarkers;
//
//    MapView map = null;
//
//    /** Called when the activity is first created. */
//    public void onCreate(Bundle savedInstanceState) {
//        Log.e(TAG, "onCreate -> BLACKMODEBLACKMODEBLACKMODEBLACKMODEBLACKMODE");
//
//        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
//        StrictMode.setThreadPolicy(policy);
//
//
//        super.onCreate(savedInstanceState);
//        requestWindowFeature(Window.FEATURE_NO_TITLE);
//        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
//                WindowManager.LayoutParams.FLAG_FULLSCREEN);
//        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
//                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
//                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
//                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
//        WindowManager.LayoutParams params = getWindow().getAttributes();
////        params.screenBrightness = 0.5f;
////        getWindow().setAttributes(params);
//
//
//        setContentView(R.layout.black_mode);
//
//        mainSettingsActivity = new MainSettingsActivity();
//
//
//        int iBlackModeOrientation;
//
//
//        boolean bShowStopwatch = getIntent().getExtras().getBoolean("ShowStopwatch", false);
//        Log.d(TAG, "BlackMode -> onCreate -> bShowStopwatch ->" + bShowStopwatch);
//
//        boolean bStartStopwatch = getIntent().getExtras().getBoolean("StartStopwatch", false);
//        Log.d(TAG, "BlackMode -> onCreate -> bStartStopwatch ->" + bStartStopwatch);
//
//        boolean bShowSpeed = getIntent().getExtras().getBoolean("ShowSpeed", false);
//        Log.d(TAG, "BlackMode -> onCreate -> bShowSpeed ->" + bShowSpeed);
//
//        bShowNavigator = getIntent().getExtras().getBoolean("ShowNavigator", false);
//        Log.d(TAG, "BlackMode -> onCreate -> bShowNavigator ->" + bShowNavigator);
//
//        bShowHR = getIntent().getExtras().getBoolean("ShowHR", false);
//        Log.d(TAG, "BlackMode -> onCreate -> bShowHR ->" + bShowHR);
//
//        boolean bShowClock = getIntent().getExtras().getBoolean("ShowClock", false);
//        Log.d(TAG, "BlackMode -> onCreate -> bShowClock ->" + bShowClock);
//
//        bMapColorMode = getIntent().getExtras().getBoolean("MapColorMode", false);
//        Log.d(TAG, "BlackMode -> onCreate -> bMapColorMode ->" + bMapColorMode);
//
//        bMapOfflineMode = getIntent().getExtras().getBoolean("MapOfflineMode", false);
//        Log.d(TAG, "BlackMode -> onCreate -> bMapOfflineMode ->" + bMapOfflineMode);
//
//        iBlackModeOrientation = getIntent().getExtras().getInt("BlackModeOrientation");
//        Log.d(TAG, "BlackMode -> onCreate -> BlackModeOrientation ->" + iBlackModeOrientation);
//
//        if (getIntent().getExtras().containsKey("SelectedMaps")) {
//            Log.d(TAG, "BlackMode -> onCreate -> SelectedMaps ->" + sSelectedMap);
//            sSelectedMap = getIntent().getExtras().getString("SelectedMaps");
//        }
//
//
//        lNavigatorLayout = this.findViewById(R.id.NavigatorLayout);
//        lDataLayout = this.findViewById(R.id.DataLayout);
//
//        TextView tClockView = this.findViewById(R.id.ClockView);
//        TextView tSpeedViewUnit = this.findViewById(R.id.SpeedViewUnit);
//        TextView tHRViewUnit = this.findViewById(R.id.HRViewUnit);
//        TextView tHRPercentageUnit = this.findViewById(R.id.HRPercentageUnit);
//
////        ImageView ivImageMapDownload = this.findViewById(R.id.imageMapDownload);
////        ivImageMapDownload.setImageResource(R.drawable.ic_cloud_download_24px);
//
//        ivImageSetPosition = this.findViewById(R.id.imageSetPositon);
//        ivImageSetPosition.setImageResource(R.drawable.ic_gps_positon_24px);
//        mTrackingMode = true;
//
//        tSpeedView = this.findViewById(R.id.SpeedView);
//        tHRView = this.findViewById(R.id.HRView);
//        tHRPercentage = this.findViewById(R.id.HRPercentage);
//        tTimerView = this.findViewById(R.id.TimerView);
//
//        if (bShowSpeed) {
//
//            locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
//            if (locationManager == null) throw new AssertionError("Object cannot be null");
//            bLocationManager = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
//
//            tSpeedView.setVisibility(View.VISIBLE);
//            tSpeedViewUnit.setVisibility(View.VISIBLE);
//
//            client = new GoogleApiClient.Builder(this)
//                    .addConnectionCallbacks(this)
//                    .addOnConnectionFailedListener(this)
//                    .addApi(Drive.API)
//                    .addApi(LocationServices.API)
//                    .addApi(Places.GEO_DATA_API)
//                    .addApi(Places.PLACE_DETECTION_API)
//                    .build();
//
//// check if enabled and if not send user to the GSP settings
//// Better solution would be to display a dialog and suggesting to
//// go to the settings
//            if (!bLocationManager) {
//                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
//                startActivity(intent);
//            }
//
//
//        } else {
//            tSpeedView.setVisibility(View.GONE);
//            tSpeedViewUnit.setVisibility(View.GONE);
//        }
//
//        if (bShowHR) {
//            tHRView.setVisibility(View.VISIBLE);
//            tHRViewUnit.setVisibility(View.VISIBLE);
//            tHRPercentage.setVisibility(View.VISIBLE);
//            tHRPercentageUnit.setVisibility(View.VISIBLE);
//        } else {
//            tHRView.setVisibility(View.GONE);
//            tHRViewUnit.setVisibility(View.GONE);
//            tHRPercentage.setVisibility(View.GONE);
//            tHRPercentageUnit.setVisibility(View.GONE);
//        }
//
//        if (bShowClock) {
//            tClockView.setVisibility(View.VISIBLE);
//        } else {
//            tClockView.setVisibility(View.GONE);
//        }
//
//        if (bShowNavigator && sSelectedMap != null) {
//            lNavigatorLayout.setVisibility(View.VISIBLE);
//        }
//        else {
//            lNavigatorLayout.setVisibility(View.GONE);
//        }
//
//        if (bShowStopwatch) {
//            tTimerView.setVisibility(View.VISIBLE);
//            if (bStartStopwatch) {
//                StartTimer(getWindow().getDecorView().getRootView());
//            }
//        } else {
//            tTimerView.setVisibility(View.GONE);
//        }
//
//        if (!bShowSpeed && !bShowHR && !bShowStopwatch) {
//            lDataLayout.setVisibility(View.GONE);
//        }
//
//        setRequestedOrientation(iBlackModeOrientation);
//    }
//
//
//    @Override
//    public void onStart() {
//        Log.e(TAG, "onStart -> BLACKMODEBLACKMODEBLACKMODEBLACKMODEBLACKMODE");
//        super.onStart();
//
//        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
//                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
//            // TODO: Consider calling
//            //    ActivityCompat#requestPermissions
//            // here to request the missing permissions, and then overriding
//            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
//            //                                          int[] grantResults)
//            // to handle the case where the user grants the permission. See the documentation
//            // for ActivityCompat#requestPermissions for more details.
////                return;
//        }
//        Criteria criteria = new Criteria();
//        provider = locationManager.getBestProvider(criteria, true);
//
//        if (provider == null) {
//            Log.d(TAG, "No location provider found!");
//        } else {
//            locationManager.requestLocationUpdates(provider, 1, 0, this);
//
//            currentGpsPosition = locationManager.getLastKnownLocation(provider);
//        }
//
//        if (bShowNavigator && sSelectedMap != null) {
//
//            Configuration.getInstance().setUserAgentValue(BuildConfig.APPLICATION_ID);
//
//            gpCurrentGpsPosition = new GeoPoint(locationManager.getLastKnownLocation(provider));
//
//            map = this.findViewById(R.id.NavigatorMap);
//            map.setTileSource(TileSourceFactory.MAPNIK);
//
//            if (bMapColorMode) {
////                map.getOverlayManager().getTilesOverlay().setColorFilter(TilesOverlay.INVERT_COLORS);
//
//                ColorMatrix inverseMatrix = new ColorMatrix(new float[] {
//                        0.0f, 0.0f, 0.0f, 0.0f, 255f,
//                        0.0f, 0.0f, 0.0f, 0.0f, 255f,
//                        0.0f, 0.0f, 0.0f, 0.0f, 255f,
//                        0.0f, 0.0f, 0.0f, 1.0f, 0.0f
//                });
//
//                int destinationColor = Color.parseColor("#FF2A2A2A");
//                float lr = (255.0f - Color.red(destinationColor))/255.0f;
//                float lg = (255.0f - Color.green(destinationColor))/255.0f;
//                float lb = (255.0f - Color.blue(destinationColor))/255.0f;
//                ColorMatrix grayscaleMatrix = new ColorMatrix(new float[] {
//                        0.0f, 0.0f, 0.0f, 0.0f, 255f,
//                        0.0f, 0.0f, 0.0f, 0.0f, 255f,
//                        0.0f, 0.0f, 0.0f, 0.0f, 255f,
//                        0.0f, 0.0f, 0.0f, 1.0f, 0.0f
//                });
//                grayscaleMatrix.preConcat(inverseMatrix);
//                int dr = Color.red(destinationColor);
//                int dg = Color.green(destinationColor);
//                int db = Color.blue(destinationColor);
//                float drf = 0;
//                float dgf = 0;
//                float dbf = 0;
//                ColorMatrix tintMatrix = new ColorMatrix(new float[] {
//                        drf, 0, 0, 0, 0, //
//                        0, dgf, 0, 0, 0, //
//                        0, 0, dbf, 0, 0, //
//                        0, 0, 0, 1, 0, //
//                });
//                tintMatrix.preConcat(grayscaleMatrix);
//                float lDestination = drf * lr + dgf * lg + dbf * lb;
//                float scale = 1f - lDestination;
//                float translate = 1 - scale * 0.5f;
//                ColorMatrix scaleMatrix = new ColorMatrix(new float[] {
//                        scale, 0, 0, 0, 0, //
//                        0, scale, 0, 0, 0, //
//                        0, 0, scale, 0, 0, //
//                        0, 0, 0, 1, 0, //
//                });
//                scaleMatrix.preConcat(tintMatrix);
//                ColorMatrixColorFilter filter = new ColorMatrixColorFilter(scaleMatrix);
//                map.getOverlayManager().getTilesOverlay().setColorFilter(filter);
//            }
//
//            if (bMapOfflineMode) {
//                map.getOverlayManager().getTilesOverlay().setUseDataConnection(false);
//            }
//
//            mAzimuthAngleSpeed = currentGpsPosition.getBearing();
//            map.setMapOrientation(-mAzimuthAngleSpeed);
//
//
//            IMapController mapController = map.getController();
//            mapController.setZoom(17.5);
//            mapController.setCenter(gpCurrentGpsPosition);
//
//
//            InternalCompassOrientationProvider internalCompassOrientationProvider = new InternalCompassOrientationProvider(this);
//            CompassOverlay mCompassOverlay = new CompassOverlay(this, internalCompassOrientationProvider, map);
//            mCompassOverlay.enableCompass(internalCompassOrientationProvider);
//            map.getOverlays().add(mCompassOverlay);
//
//
//            MyLocationNewOverlay mLocationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(this), map);
//            mLocationOverlay.enableMyLocation();
//            mLocationOverlay.setDrawAccuracyEnabled(true);
//            mLocationOverlay.enableFollowLocation();
//            //            Bitmap navigation_icon = getBitmap(this, R.drawable.ic_navigation_white_48dp);
//            Bitmap navigation_icon = getBitmap(this, R.drawable.ic_navigation_black_48dp);
//            Bitmap current_location_icon = getBitmap(this, R.drawable.ic_navigation_green_48dp);
//
//            if (bMapColorMode) {
//                navigation_icon = getBitmap(this, R.drawable.ic_navigation_red_48dp);
//                current_location_icon = getBitmap(this, R.drawable.ic_navigation_blue_48dp);
//            }
//            //            Bitmap current_location_icon = ((BitmapDrawable)map.getContext().getResources().getDrawable(R.drawable.person)).getBitmap();
//            mLocationOverlay.setDirectionArrow(current_location_icon, navigation_icon);
//            map.getOverlays().add(mLocationOverlay);
//
//
//            File sdcard = Environment.getExternalStorageDirectory();
//            File file = new File(sSelectedMap);
//
//
//            mKmlDocument = new KmlDocument();
//            Log.d(TAG, "BlackMode -> onCreate -> kmlFilePath ->" + file.getAbsolutePath());
//            boolean parseResult = mKmlDocument.parseKMLFile(file);
//            Log.d(TAG, "BlackMode -> onCreate -> parseResult ->" + parseResult);
//            Log.d(TAG, "BlackMode -> onCreate -> mKmlDocument ->" + mKmlDocument);
//
//
//            RotationGestureOverlay mRotationGestureOverlay = new RotationGestureOverlay(map);
//            mRotationGestureOverlay.setEnabled(true);
//            map.setMultiTouchControls(true);
//            map.getOverlays().add(mRotationGestureOverlay);
//
//
//            // commented out -> no marker in use
//            // Drawable defaultMarker = getResources().getDrawable(R.drawable.marker_kml_point);
//            // Bitmap defaultBitmap = ((BitmapDrawable) defaultMarker).getBitmap();
//
//            Style defaultStyle = new Style(null, Color.parseColor("#222F99"), 10.0f, Color.parseColor("#222F99"));
//            if (bMapColorMode) {
//                defaultStyle = new Style(null, Color.parseColor("#FFFFFF"), 10.0f, Color.parseColor("#FFFFFF"));
//            }
//            KmlFeature.Styler myStyler = new MyKmlStyler(defaultStyle);
//
//            Log.d(TAG, "BlackMode -> onCreate -> defaultStyle ->" + defaultStyle);
//            Log.d(TAG, "BlackMode -> onCreate -> mKmlDocument.mKmlRoot.mStyle ->" + mKmlDocument.mKmlRoot.mStyle);
//            Log.d(TAG, "BlackMode -> onCreate -> mKmlDocument.mKmlRoot.getBoundingBox ->" + mKmlDocument.mKmlRoot.getBoundingBox());
//            Log.d(TAG, "BlackMode -> onCreate -> mKmlDocument.mKmlRoot.mVisibility ->" + mKmlDocument.mKmlRoot.mVisibility);
//            Log.d(TAG, "BlackMode -> onCreate -> mKmlDocument.mKmlRoot.mName ->" + mKmlDocument.mKmlRoot.mName);
//            Log.d(TAG, "BlackMode -> onCreate -> mKmlDocument.mKmlRoot.mExtendedData ->" + mKmlDocument.mKmlRoot.mExtendedData);
//            Log.d(TAG, "BlackMode -> onCreate -> mKmlDocument.mKmlRoot.mItems ->" + mKmlDocument.mKmlRoot.mItems);
//            Log.d(TAG, "BlackMode -> onCreate -> mKmlDocument.mKmlRoot.mItems.size ->" + mKmlDocument.mKmlRoot.mItems.size());
//
//
//            FolderOverlay kmlOverlay = (FolderOverlay) mKmlDocument.mKmlRoot.buildOverlay(map, defaultStyle, myStyler, mKmlDocument);
//            map.getOverlays().add(kmlOverlay);
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//            mRoadNodeMarkers = new FolderOverlay();
//            mRoadNodeMarkers.setName("Route Steps");
//            map.getOverlays().add(mRoadNodeMarkers);
//
//            KmlPlacemark placemark = (KmlPlacemark) mKmlDocument.mKmlRoot.mItems.get(0);
//            Log.d(TAG, "BlackMode -> onCreate -> placemark ->" + placemark);
//
//            KmlMultiGeometry kmlMultiGeometry = (KmlMultiGeometry) placemark.mGeometry;
//            Log.d(TAG, "BlackMode -> onCreate -> kmlMultiGeometry ->" + kmlMultiGeometry);
////            List<Overlay> mapOverlays = map.getOverlays();
////            RoadManager roadManager = new OSRMRoadManager(map.getContext());
////            roadManager.getRoads(kmlMultiGeometry.mCoordinates);
//
////            Log.d(TAG, "BlackMode -> onCreate -> mCoordinates ->" + kmlMultiGeometry.mCoordinates);
//            Log.d(TAG, "BlackMode -> onCreate -> mCoordinates ->" + kmlMultiGeometry.mItems.get(0).mCoordinates.get(0));
//
//            RoadCalculation roadCalculation = new RoadCalculation(kmlMultiGeometry);
//            roadCalculation.roadNetworkCalculation(kmlMultiGeometry);
//            Log.d(TAG, "BlackMode -> onCreate -> roadCalculation ->" + roadCalculation);
//
//
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//            map.invalidate();
//            Log.d(TAG, "BlackMode -> onCreate -> map ->" + map);
//
//            // commented out -> TODO -> navigation for selected route
//            //            navigation.context = this.getApplicationContext();
//            //            navigation.setMap(map);
//            //            navigation.setKmlMultiGeometry(kmlMultiGeometry);
//        }
//    }
//
//    void selectRoad(int roadIndex){
//        int mSelectedRoad = roadIndex;
//        putRoadNodes(mRoad);
//        //Set route info in the text view:
////        TextView textView = (TextView)findViewById(R.id.routeInfo);
////        textView.setText(mRoads[roadIndex].getLengthDurationText(this, -1));
////        for (int i=0; i<mRoadOverlays.length; i++){
////            Paint p = mRoadOverlays[i].getPaint();
////            if (i == roadIndex)
////                p.setColor(0x800000FF); //blue
////            else
////                p.setColor(0x90666666); //grey
////        }
//        map.invalidate();
//    }
//
//    private void putRoadNodes(Road road){
//        mRoadNodeMarkers.getItems().clear();
//        Drawable icon = ResourcesCompat.getDrawable(getResources(), R.drawable.marker_node, null);
//        int n = road.mNodes.size();
////        MarkerInfoWindow infoWindow = new MarkerInfoWindow(org.osmdroid.bonuspack.R.layout.bonuspack_bubble, map);
//        TypedArray iconIds = getResources().obtainTypedArray(R.array.direction_icons);
//        for (int i=0; i<n; i++){
//            RoadNode node = road.mNodes.get(i);
//            String instructions = (node.mInstructions==null ? "" : node.mInstructions);
//            Marker nodeMarker = new Marker(map);
//            nodeMarker.setTitle("Step"+ " " + (i+1));
//            nodeMarker.setSnippet(instructions);
//            nodeMarker.setSubDescription(Road.getLengthDurationText(this, node.mLength, node.mDuration));
//            nodeMarker.setPosition(node.mLocation);
//            nodeMarker.setIcon(icon);
////            nodeMarker.setInfoWindow(infoWindow); //use a shared infowindow.
//            int iconId = iconIds.getResourceId(node.mManeuverType, R.drawable.ic_empty);
//            if (iconId != R.drawable.ic_empty){
//                Drawable image = ResourcesCompat.getDrawable(getResources(), iconId, null);
//                nodeMarker.setImage(image);
//            }
//            nodeMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
//            mRoadNodeMarkers.add(nodeMarker);
//        }
//        iconIds.recycle();
//    }
//
//    class RoadOnClickListener implements Polyline.OnClickListener{
//        @Override public boolean onClick(Polyline polyline, MapView mapView, GeoPoint eventPos){
//            int selectedRoad = (Integer)polyline.getRelatedObject();
////            selectRoad(selectedRoad);
//            polyline.setInfoWindowLocation(eventPos);
//            polyline.showInfoWindow();
//            return true;
//        }
//    }
//
//
//    @Override
//    protected void onResume() {
//        Log.e(TAG, "onResume -> BLACKMODEBLACKMODEBLACKMODEBLACKMODEBLACKMODE");
//        super.onResume();
//        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
//                ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
//            // TODO: Consider calling
//            //    ActivityCompat#requestPermissions
//            // here to request the missing permissions, and then overriding
//            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
//            //                                          int[] grantResults)
//            // to handle the case where the user grants the permission. See the documentation
//            // for ActivityCompat#requestPermissions for more details.
//            return;
//        }
//        if (bLocationManager) {
//            onLocationChanged(currentGpsPosition);
//        }
//
//        if (bShowNavigator && sSelectedMap != null) {
//            map.onResume();
//        }
//
//        if (bShowHR) {
//            if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
//                try {
//                    Log.e(TAG, "registerReceiver");
//                    registerReceiver(broadcastReceiver, new MainSettingsActivity().broadcastReceiverUpdateIntentFilter());
//                } catch (Exception e) {
//                    Log.e(TAG, "broadcastReceiver isn't registered!");
//                }
//            }
//        }
//    }
//
//    @Override
//    protected void onPause() {
//        Log.e(TAG, "onPause -> BLACKMODEBLACKMODEBLACKMODEBLACKMODEBLACKMODE");
//
//        super.onPause();
//        if (bLocationManager) {
//            locationManager.removeUpdates(this);
//        }
//
//        if (bShowHR) {
//            try {
//                unregisterReceiver(broadcastReceiver);
//            } catch (Exception e) {
//                Log.e(TAG, "broadcastReceiver wasn't registered!");
//            }
//        }
//
//        if (bShowNavigator && sSelectedMap != null) {
//            map.onPause();
//        }
//    }
//
//
//    @Override
//    public void onStop() {
//        Log.e(TAG, "onStop -> BLACKMODEBLACKMODEBLACKMODEBLACKMODEBLACKMODE");
//
//        super.onStop();
//
//    }
//
//    @Override
//    public void onLocationChanged(Location currentLocation) {
//
//        Location pCurrentLocation;
//
////        navigation.setCurrentGpsPosition(location);
////        int mCoordinatesSize = this.kmlMultiGeometry.mItems.get(0).mCoordinates.size();
////        if (nextWaypoint <= mCoordinatesSize)
////            nextWaypoint = navigation.routeNavigation(nextWaypoint);
//
//        if( currentLocation != null) {
//            float calcSpeed = 0.0f;
//            if (mLastLocation != null) {
//                //TODO
//                calcSpeed = (float) (Math.sqrt(
//                        Math.pow(currentLocation.getLongitude() - mLastLocation.getLongitude(), 2)
//                                + Math.pow(currentLocation.getLatitude() - mLastLocation.getLatitude(), 2)
//                ) / (currentLocation.getTime() - mLastLocation.getTime()));
//            }
//            //if there is speed from location
//            if (currentLocation.hasSpeed())
//            {
//                //get location speed
//                calcSpeed = currentLocation.getSpeed();
//                calcSpeed = (calcSpeed * 3.6f);
//                float roundcalcSpeed = Math.round(calcSpeed*10.0f)/10.0f;
//                mLastLocation = currentLocation;
//                String text = "" + roundcalcSpeed;
//                tSpeedView.setText(text);
//
//            }
//
//            if(bShowNavigator && sSelectedMap != null) {
//                if (mTrackingMode) {
//                    //keep the map view centered on current location:
//                    gpCurrentGpsPosition = new GeoPoint(currentLocation);
//                    map.getController().animateTo(gpCurrentGpsPosition);
////                    map.setMapOrientation(-mAzimuthAngleSpeed);
//
//                    float bearing = currentLocation.getBearing();
//                    float t = (360 - bearing);
//                    if (t < 0) {
//                        t += 360;
//                    }
//                    if (t > 360) {
//                        t -= 360;
//                    }
//                    //help smooth everything out
//                    t = (int) t;
//                    t = t / 5;
//                    t = (int) t;
//                    t = t * 5;
//                    map.setMapOrientation(t);
//
//                } else {
//                    //just redraw the location overlay:
//                    map.invalidate();
//                }
//
//            }
//
//        }
//        else {
//            tSpeedView.setText("--");
//        }
//    }
//
//    @Override
//    public void onStatusChanged(String provider, int status, Bundle extras) {
//
//    }
//
//    @Override
//    public void onProviderEnabled(String provider) {
//
//    }
//
//    @Override
//    public void onProviderDisabled(String provider) {
//
//    }
//
//    @Override
//    public void onConnected(Bundle bundle) {
//
//    }
//
//    @Override
//    public void onConnectionSuspended(int i) {
//
//    }
//
//    @Override
//    protected void onDestroy() {
//        Log.e(TAG, "onDestroy -> BLACKMODEBLACKMODEBLACKMODEBLACKMODEBLACKMODE");
//
//        super.onDestroy();
//
//        if(refreshTimerThread != null) {
//            refreshTimerThread.interrupt();
//            refreshTimerThread = null;
//        }
//
//        if(client != null){
//            client.disconnect();
//        }
//
//    }
//
//    @Override
//    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
//
//    }
//
//
//    BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
//        @Override
//        public void onReceive(Context context, Intent intent) {
//            Log.e(TAG, "BroadcastReceiver -> onReceive");
//
//            if(intent.getAction().equals(ACTION_BROADCAST_RECEIVER)){
////                Log.d(TAG, String.format("ACTION_BROADCAST_RECEIVER: "+ ACTION_BROADCAST_RECEIVER));
////                Log.d(TAG, String.format("ACTION_BROADCAST_RECEIVER_DATA: "+ intent.getAction()));
////                Log.d(TAG, String.format("ACTION_BROADCAST_RECEIVER_DATA: "+ intent.getStringExtra(ACTION_BROADCAST_RECEIVER_DATA)));
////                Log.d(TAG, String.format("ACTION_BROADCAST_RECEIVER_DATA: %d", Integer.valueOf(intent.getStringExtra(ACTION_BROADCAST_RECEIVER_DATA))));
//                setHrValues(Integer.valueOf(intent.getStringExtra(ACTION_BROADCAST_RECEIVER_DATA)));
//            }
//        }
//    };
//
//
//    public void setHrValues(int hrData) {
//        int iPercentage = (int) ((hrData*100.0f)/195);
//
//        runOnUiThread(() -> {
//            // Stuff that updates the UI
//            mainSettingsActivity.setTextFieldTexts(tHRView, Integer.toString(hrData));
//            mainSettingsActivity.setTextFieldTexts(tHRPercentage, Integer.toString(iPercentage));
//        });
//    }
//
//
//    public void StartTimer(View view) {
//        if (!timerRunning) {
//            timerRunning = true;
//            initTimer();
//
//        } else {
//            timerRunning = false;
//            refreshTimerThread.interrupt();
//            refreshTimerThread = null;
//        }
//
//    }
//
//
//    public void initTimer() {
//        refreshTimerThread = new Thread(() -> {
//            while (timerRunning) {
//                time = time + 1;
//                try {
//                    Thread.sleep(1000);
//                } catch (InterruptedException ex) {
////                        Logger.getLogger(MainActivity.class.getName()).log(Level.SEVERE, null, ex);
//                }
//                runOnUiThread(() -> tTimerView.setText( String.format(Locale.getDefault(),"%02d:%02d:%02d", (time / 3600),((time % 3600) / 60), (time % 60) ) ));
//
//            }
//        });
//        refreshTimerThread.start();
//    }
//
//
//    public void onClickSetGpsPosition(View view) {
//        mTrackingMode = !mTrackingMode;
//        updateUIWithTrackingMode();
//    }
//
//
//    void updateUIWithTrackingMode(){
//        if (mTrackingMode){
//            ivImageSetPosition.setImageResource(R.drawable.ic_gps_positon_24px);
//            map.getController().animateTo(gpCurrentGpsPosition);
//            map.setMapOrientation(-mAzimuthAngleSpeed);
//        } else {
//            ivImageSetPosition.setImageResource(R.drawable.ic_gps_not_fixed_24px);
//            map.setMapOrientation(0.0f);
//        }
//    }
//
//
//    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
//    private static Bitmap getBitmap(VectorDrawable vectorDrawable) {
//        Bitmap bitmap = Bitmap.createBitmap(vectorDrawable.getIntrinsicWidth(),
//                vectorDrawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
//        Canvas canvas = new Canvas(bitmap);
//        vectorDrawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
//        vectorDrawable.draw(canvas);
//        return bitmap;
//    }
//
//    private static Bitmap getBitmap(Context context, int drawableId) {
//        Drawable drawable = ContextCompat.getDrawable(context, drawableId);
//        if (drawable instanceof BitmapDrawable) {
//            return ((BitmapDrawable) drawable).getBitmap();
//        } else if (drawable instanceof VectorDrawable) {
//            return getBitmap((VectorDrawable) drawable);
//        } else {
//            throw new IllegalArgumentException("unsupported drawable type");
//        }
//    }
//
//    public void onClickDownloadMap(View view) {
////        CacheManager cacheManager = new CacheManager(map);
////        double zoomMin = map.getMinZoomLevel();
////        double zoomMax = map.getMaxZoomLevel()+4.0;
////        Log.d(TAG, "BlackMode -> onClickDownloadMap -> start");
//////            Toast.makeText(this, getString(R.string.download_start), Toast.LENGTH_LONG).show();
////        cacheManager.downloadAreaAsync(this.getApplicationContext(), map.getBoundingBox(), (int)(zoomMin), (int) zoomMax);
////        Log.d(TAG, "BlackMode -> onClickDownloadMap -> end");
//
//// DOWNLOAD in background
////        new tileDownlaodTask().execute();
//    }
//
////Async task to reverse-geocode the KML point in a separate thread:
////    private class tileDownlaodTask extends AsyncTask<Void, Void, Boolean> {
////
////        protected Boolean doInBackground(Void... voids) {
////            CacheManager cacheManager = new CacheManager(map);
////            double zoomMin = map.getMinZoomLevel();
////            double zoomMax = map.getMaxZoomLevel()+4.0;
////            Log.d(TAG, "BlackMode -> onClickDownloadMap -> start");
//////            Toast.makeText(this, getString(R.string.download_start), Toast.LENGTH_LONG).show();
////            CacheManager.CacheManagerTask task = cacheManager.downloadAreaAsync(getApplicationContext(), map.getBoundingBox(), (int)(zoomMin), (int) zoomMax);
//////            Toast.makeText(this, "TEST context", Toast.LENGTH_LONG).show();
////
////            Log.d(TAG, "BlackMode -> onClickDownloadMap -> end");
////            return true;
////        }
////        protected void onPostExecute(String result) {
////
////        }
////    }
////
////
////    public void onClickDownloadMap(View view) {
////        MapView map = this.findViewById(R.id.NavigatorMap);
////        CacheManager cacheManager = new CacheManager(map);
////        long cacheUsage = cacheManager.currentCacheUsage() / (1024 * 1024);
////        long cacheCapacity = cacheManager.cacheCapacity() / (1024 * 1024);
////        float percent = 100.0f * cacheUsage / cacheCapacity;
////        String message = "Cache usage:\n" + cacheUsage + " Mo / " + cacheCapacity + " Mo = " + (int) percent + "%";
////        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
////    }
//
//
//
//
//    class MyKmlStyler implements KmlFeature.Styler {
//        Style mDefaultStyle;
//
//        MyKmlStyler(Style defaultStyle) {
//            mDefaultStyle = defaultStyle;
//        }
//
//        @Override
//        public void onLineString(Polyline polyline, KmlPlacemark kmlPlacemark, KmlLineString kmlLineString) {
//            //Custom styling:
////            polyline.setColor(Color.GREEN);
//            polyline.setColor(mDefaultStyle.mLineStyle.mColor);
////            polyline.setWidth(Math.max(kmlLineString.mCoordinates.size() / 200.0f, 3.0f));
//            polyline.setWidth(mDefaultStyle.mLineStyle.mWidth);
//
//            final Paint arrowPaint = new Paint();
//
//            arrowPaint.setColor(mDefaultStyle.mLineStyle.mColor);
//            arrowPaint.setStrokeWidth(10.0f);
////            arrowPaint.setStyle(Paint.Style.FILL_AND_STROKE);
//            arrowPaint.setStyle(Paint.Style.STROKE);
//            arrowPaint.setAntiAlias(true);
//            final Path arrowPath = new Path(); // a simple arrow towards the right
//            arrowPath.moveTo(- 30, - 30);
//            arrowPath.lineTo(30, 0);
//            arrowPath.lineTo(- 30, 30);
////            arrowPath.close();
//            final List<MilestoneManager> managers = new ArrayList<>();
//            managers.add(new MilestoneManager(
////                    new MilestonePixelDistanceLister(200, 200),
////                    new MilestoneMiddleLister(100),
//                    new MilestoneMeterDistanceLister(200),
//                    new MilestonePathDisplayer(0, true, arrowPath, arrowPaint)
//            ));
//            polyline.setMilestoneManagers(managers);
//
//        }
//
//        @Override
//        public void onPolygon(Polygon polygon, KmlPlacemark kmlPlacemark, KmlPolygon kmlPolygon) {
//            //Keeping default styling:
//            kmlPolygon.applyDefaultStyling(polygon, mDefaultStyle, kmlPlacemark, mKmlDocument, map);
//        }
//
//        @Override
//        public void onTrack(Polyline polyline, KmlPlacemark kmlPlacemark, KmlTrack kmlTrack) {
//            //Keeping default styling:
//            kmlTrack.applyDefaultStyling(polyline, mDefaultStyle, kmlPlacemark, mKmlDocument, map);
//        }
//
//        @Override
//        public void onPoint(Marker marker, KmlPlacemark kmlPlacemark, KmlPoint kmlPoint) {
//            //Styling based on ExtendedData properties:
//            if (kmlPlacemark.getExtendedData("maxspeed") != null)
//                kmlPlacemark.mStyle = "maxspeed";
//            kmlPoint.applyDefaultStyling(marker, mDefaultStyle, kmlPlacemark, mKmlDocument, map);
//        }
//
//        @Override
//        public void onFeature(Overlay overlay, KmlFeature kmlFeature) {
//            //If nothing to do, do nothing.
//        }
//    }
//
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//    public class RoadCalculation<T> {
//
//        KmlMultiGeometry my_kmlMultiGeometry;
//        private Executor executor;;
//
//        RoadCalculation(KmlMultiGeometry kmlMultiGeometry) {
//            my_kmlMultiGeometry = kmlMultiGeometry;
//        }
//
//
//        public void makeCalculationRequest(final String jsonBody) {
//            this.executor.execute(new Runnable() {
//                @Override
//                public void run() {
//                    roadNetworkCalculation(my_kmlMultiGeometry);
//                }
//            });
//        }
//
//
//        private void roadNetworkCalculation(KmlMultiGeometry kmlMultiGeometry) {
//            RoadManager roadManager = new OSRMRoadManager(map.getContext());
//            List<Overlay> mapOverlays = map.getOverlays();
//
//
////
////            waypoints.add(new GeoPoint(50.911257,10.915912,240.724045));
////            waypoints.add(new GeoPoint(50.911019,10.916136,240.724045));
////            waypoints.add(new GeoPoint(50.910893,10.915089,240.724045));
////            waypoints.add(new GeoPoint(51.116732,10.778771,171.525784));
//
////            waypoints.add(new GeoPoint(50.910789,10.915915,239.37749));
////            waypoints.add(new GeoPoint(50.910576,10.91533,239.37749));
////            waypoints.add(new GeoPoint(50.910543,10.915223,239.37749));
////            waypoints.add(new GeoPoint(50.910549,10.91514,239.37749));
////            waypoints.add(new GeoPoint(50.910672,10.915147,239.37749));
////            waypoints.add(new GeoPoint(50.910774,10.915136,239.37749));
////            waypoints.add(new GeoPoint(50.910893,10.915089,239.378533));
////            waypoints.add(new GeoPoint(50.910983,10.915032,239.421475));
//
////  Limit to 100 because the test server is only using 100 requests
//// http://router.project-osrm.org/route/v1/driving/13.388860,52.517037;13.397634,52.529407;13.428555,52.523219?overview=false'
//// http://router.project-osrm.org/route/v1/driving/10.9159150000,50.9107890000;10.9153300000,50.9105760000;10.9152230000,50.9105430000?overview=false&skip_waypoints=true
//// http://router.project-osrm.org/route/v1/driving/10.9159150000,50.9107890000;10.9153300000,50.9105760000;10.9152230000,50.9105430000;10.9151400000,50.9105490000;10.9151470000,50.9106720000;10.9151360000,50.9107740000;10.9150890000,50.9108930000;10.9150320000,50.9109830000;10.9149930000,50.9110320000;10.9149090000,50.9111270000;10.9148140000,50.9112330000;10.9144690000,50.9115750000;10.9142640000,50.9117880000;10.9141610000,50.9118930000;10.9142020000,50.9119760000;10.9142280000,50.9120270000;10.9142660000,50.9121430000;10.9143170000,50.9122960000;10.9143690000,50.9124580000;10.9143950000,50.9125350000;10.9145210000,50.9128850000;10.9146060000,50.9131340000;10.9147930000,50.9136770000;10.9149100000,50.9139820000;10.9150550000,50.9144940000;10.9149750000,50.9145800000;10.9153420000,50.9148150000;10.9157140000,50.9150200000;10.9158370000,50.9150880000;10.9170060000,50.9158840000;10.9170950000,50.9159420000;10.9172820000,50.9160490000;10.9176160000,50.9161890000;10.9185830000,50.9166130000;10.9187230000,50.9166190000;10.9187850000,50.9166410000;10.9188550000,50.9166650000;10.9192490000,50.9167530000;10.9202080000,50.9168230000;10.9205740000,50.9168960000;10.9221120000,50.9176930000;10.9222780000,50.9177930000;10.9229810000,50.9181720000;10.9234530000,50.9185210000;10.9235530000,50.9185870000;10.9236330000,50.9186420000;10.9244460000,50.9190220000;10.9249660000,50.9191610000;10.9267480000,50.9192190000;10.9269980000,50.9192280000;10.9270220000,50.9192510000;10.9270770000,50.9193030000;10.9275780000,50.9193020000;10.9287230000,50.9193360000;10.9297930000,50.9196960000;10.9301380000,50.9197880000;10.9304050000,50.9198570000;10.9314490000,50.9201220000;10.9316750000,50.9201970000;10.9320060000,50.9203180000;10.9321830000,50.9203700000;10.9324740000,50.9204220000;10.9329160000,50.9204620000;10.9329730000,50.9204650000;10.9338990000,50.9205030000;10.9343010000,50.9205660000;10.9344560000,50.9205910000;10.9346090000,50.9205760000;10.9349720000,50.9204690000;10.9355630000,50.9204840000;10.9356850000,50.9204910000;10.9357900000,50.9204980000;10.9359930000,50.9205190000;10.9361540000,50.9205450000;10.9362800000,50.9205950000;10.9363610000,50.9206410000;10.9365200000,50.9207980000;10.9367190000,50.9213640000;10.9367510000,50.9214410000;10.9368500000,50.9216320000;10.9368820000,50.9216850000;10.9369310000,50.9217260000;10.9370730000,50.9217850000;10.9372220000,50.9218090000;10.9373740000,50.9218160000;10.9380360000,50.9217590000;10.9384010000,50.9217650000;10.9389440000,50.9218370000;10.9391830000,50.9218830000;10.9394770000,50.9219380000;10.9396890000,50.9219870000;10.9398890000,50.9220500000;10.9402770000,50.9221980000;10.9411970000,50.9226420000;10.9413300000,50.9227170000;10.9417410000,50.9229760000;10.9419400000,50.9231310000;10.9420320000,50.9232220000;10.9421190000,50.9233180000;10.9421810000,50.9233860000;10.9422740000,50.9234910000;10.9423220000,50.9235730000;10.9423330000,50.9235920000;10.9423930000,50.9237250000;10.9424140000,50.9237830000;10.9424410000,50.9238500000;10.9424830000,50.9239570000;10.9425880000,50.9242160000;10.9427740000,50.9245260000;10.9432750000,50.9248460000;10.9435070000,50.9249120000;10.9434720000,50.9249730000;10.9436540000,50.9250100000;10.9455750000,50.9249420000;10.9482130000,50.9245310000;10.9488680000,50.9243730000;10.9489720000,50.9243470000;10.9494680000,50.9242120000;10.9540930000,50.9227010000;10.9542120000,50.9226510000;10.9543050000,50.9226120000;10.9546990000,50.9223700000;10.9551710000,50.9218980000;10.9556170000,50.9215910000;10.9569490000,50.9208860000;10.9575030000,50.9206440000;10.9576810000,50.9205890000;10.9578820000,50.9205370000;10.9585230000,50.9204500000;10.9588790000,50.9204790000;10.9592160000,50.9205700000;10.9600000000,50.9209000000;10.9600620000,50.9209160000;10.9602330000,50.9209580000;10.9605250000,50.9209960000;10.9608090000,50.9210230000;10.9608190000,50.9209830000;10.9608290000,50.9209460000;10.9624780000,50.9209640000;10.9625560000,50.9209320000;10.9626620000,50.9209460000;10.9627900000,50.9209760000?overview=false&skip_waypoints=true
//            ArrayList<GeoPoint> waypoints = new ArrayList<>();
//            int kml_size = my_kmlMultiGeometry.mItems.get(0).mCoordinates.size();
//            Log.d(TAG, "BlackMode -> onCreate -> kml_size ->" + kml_size);
//
////            for( int i = 0; i < kml_size; i++) {
//            for( int i = 0; i < 50; i++) {
//                if( i % 2 == 0) {
//                    Log.d(TAG, "BlackMode -> onCreate -> add ->"+ i + " > " + kmlMultiGeometry.mItems.get(0).mCoordinates.get(i));
//                    waypoints.add(kmlMultiGeometry.mItems.get(0).mCoordinates.get(i));
//                }
//            }
////            mRoads = roadManager.getRoads(waypoints);
//            Road[] mRoads = new Road[waypoints.size()];
//
//            for (int i=0; i<waypoints.size()-1; i++) {
//                mRoads[i] = new Road();
//                mRoads[i].mNodes = new ArrayList<RoadNode>();
//
//                Log.d(TAG, "BlackMode -> onCreate -> i ->" + i + "/" + waypoints.size());
//
//                ArrayList<GeoPoint> two_waypoints = new ArrayList<>();
//                two_waypoints.add(waypoints.get(i));
//                two_waypoints.add(waypoints.get(i+1));
//                Log.d(TAG, "BlackMode -> onCreate -> two_waypoints ->" + two_waypoints);
//
//                mRoad = roadManager.getRoad(two_waypoints);
//                Log.d(TAG, "BlackMode -> onCreate -> mRoad.mNodes.size() -> " + mRoad.mNodes.size());
//                Log.d(TAG, "BlackMode -> onCreate -> mRoad.mNodes.size() -> " + mRoad.mNodes);
////                Log.d(TAG, "BlackMode -> onCreate -> mRoad.mNodes.size() ->" + mRoad.mNodes.get(0));
//                for (int j=0; j<mRoad.mNodes.size(); j++) {
////                    Log.d(TAG, "BlackMode -> onCreate -> mRoad.mNodes.get(j).mManeuverType 0->" + mRoad.mNodes.get(j).mManeuverType);
////                    Log.d(TAG, "BlackMode -> onCreate -> mRoad.mNodes.get(j).mInstructions 0->" + mRoad.mNodes.get(j).mInstructions);
//
//                    if (mRoad.mNodes.get(j).mManeuverType == 0 || mRoad.mNodes.get(j).mManeuverType == 24) {
////                        Log.d(TAG, "BlackMode -> onCreate -> mRoad.mNodes.get(j).mManeuverType 1->" + mRoad.mNodes.get(j).mManeuverType);
////                        Log.d(TAG, "BlackMode -> onCreate -> mRoad.mNodes.get(j).mInstructions 1->" + mRoad.mNodes.get(j).mInstructions);
////                        mRoads[i].mNodes.remove(j);
////                    mRoads[i].mNodes
////                        mRoad.mNodes.remove(j);
////                        mRoad.mNodes.remove(j);
////                        j = 1;
//                    }
//                    else {
//                        Log.d(TAG, "BlackMode -> onCreate -> mRoad.mNodes.get(j).mManeuverType 2->" + mRoad.mNodes.get(j).mManeuverType);
//                        Log.d(TAG, "BlackMode -> onCreate -> mRoad.mNodes.get(j).mInstructions 2->" + mRoad.mNodes.get(j).mInstructions);
//                        RoadNode node = mRoad.mNodes.get(j);
//
//                        mRoads[i].mNodes.add(node);
//                    }
//                }
//            }
//
//
//
//            Log.d(TAG, "BlackMode -> onCreate -> mRoads.length ->" + mRoads.length);
//
//            for (int i=0; i<mRoads.length; i++) {
//                Log.d(TAG, "BlackMode -> onCreate -> mRoads[i].mNodes.size() ->"+ i + "/" + mRoads[i]);
//
//
//                mRoadOverlays = new Polyline[1];
//                for (int j=0; j<mRoadOverlays.length; j++) {
//                    Polyline roadPolyline = RoadManager.buildRoadOverlay(mRoads[i]);
//                    mRoadOverlays[j] = roadPolyline;
//
//                    String routeDesc = mRoad.getLengthDurationText(map.getContext(), -1);
//                    roadPolyline.setTitle(" - " + routeDesc);
//    //                roadPolyline.setInfoWindow(new BasicInfoWindow(org.osmdroid.bonuspack.R.layout.bonuspack_bubble, map));
//                    roadPolyline.setRelatedObject(j);
//                    roadPolyline.setOnClickListener(new RoadOnClickListener());
//                    mapOverlays.add(1, roadPolyline);
//                    //we insert the road overlays at the "bottom", just above the MapEventsOverlay,
//                    //to avoid covering the other overlays.
//                }
//            }
//            selectRoad(0);
//        }
//
//
//    }
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//}
//

package com.example.aklesoft.heartrate_monitor;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.util.Log;

import org.osmdroid.bonuspack.kml.KmlMultiGeometry;
import org.osmdroid.bonuspack.routing.OSRMRoadManager;
import org.osmdroid.bonuspack.routing.Road;
import org.osmdroid.bonuspack.routing.RoadManager;
import org.osmdroid.bonuspack.routing.RoadNode;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;

import java.util.ArrayList;
import java.util.Locale;

// CONVERT website
// https://kmltools.appspot.com/gpx2kml

class Navigation {

    private final static String TAG = BlackMode.class.getSimpleName();
    NavigatorViewCallback navigatorViewCallback = null;

    Navigation(NavigatorViewCallback callback) {
        this.navigatorViewCallback = callback;
        this.setChangeDirectionList();
        this.setNoChangeDirectionList();
    }

    Context context = null;
    private KmlMultiGeometry kmlMultiGeometry = null;
    private Location currentGpsPosition = null;
    private MapView map = null;

    private ArrayList<Integer> changeDirectionList = new ArrayList<Integer>();
    private ArrayList<Integer> noChangeDirectionList = new ArrayList<Integer>();

    void setCurrentGpsPosition(Location currentGpsPosition) {
        this.currentGpsPosition = currentGpsPosition;
    }

    void setKmlMultiGeometry(KmlMultiGeometry kmlMultiGeometry) {
        this.kmlMultiGeometry = kmlMultiGeometry;
    }

    void setMap(MapView map) {
        this.map = map;
    }

    public interface NavigatorViewCallback {
        // Declaration of the template function for the interface
        public void updateNavigatorView(String newString);
    }

    int routeNavigation(int nextWaypoint) {

        GeoPoint startPoint = new GeoPoint(this.currentGpsPosition);

        RoadManager roadManager = new OSRMRoadManager(this.context);
        Drawable nodeIcon = this.context.getResources().getDrawable(R.drawable.marker_kml_point);

        ArrayList<GeoPoint> waypoints = new ArrayList<GeoPoint>();

        Log.d(TAG, "BlackMode -> Navigation -> kmlMultiGeometry ->"+ kmlMultiGeometry);
        Log.d(TAG, "BlackMode -> Navigation -> kmlMultiGeometry ->"+ kmlMultiGeometry.getClass());
        Log.d(TAG, "BlackMode -> Navigation -> kmlMultiGeometry ->"+ kmlMultiGeometry.mCoordinates);
        Log.d(TAG, "BlackMode -> Navigation -> kmlMultiGeometry ->"+ kmlMultiGeometry.mItems);
        Log.d(TAG, "BlackMode -> Navigation -> kmlMultiGeometry ->"+ kmlMultiGeometry.mItems.size());
        Log.d(TAG, "BlackMode -> Navigation -> kmlMultiGeometry ->"+ kmlMultiGeometry.mItems.get(0));

        Log.d(TAG, "BlackMode -> Navigation -> nextWaypoint -------------------------------------------->");
        Log.d(TAG, "BlackMode -> Navigation -> nextWaypoint ->"+ (nextWaypoint+1));

        GeoPoint endPoint = this.kmlMultiGeometry.mItems.get(0).mCoordinates.get((nextWaypoint + 1));

        waypoints.add(startPoint);
        waypoints.add(endPoint);
        Log.d(TAG, "BlackMode -> Navigation -> startPoint ->" + startPoint);
        Log.d(TAG, "BlackMode -> Navigation -> endPoint ->" + endPoint);


        Road road = roadManager.getRoad(waypoints);

        Log.d(TAG, "BlackMode -> Navigation -> roadmNodes.size ->" + road.mNodes.size());
        Log.d(TAG, "BlackMode -> Navigation -> road.mLength ->" + road.mLength * 1000);
        Log.d(TAG, "BlackMode -> Navigation -> road.mDuration ->" + road.mDuration);

        double roadLengthInMeters = road.mLength * 1000;
        RoadNode node = road.mNodes.get((road.mNodes.size() - 1));
        for (int i = 0; i < road.mNodes.size(); i++) {
            Log.d(TAG, "BlackMode -> Navigation -> .mNodes ------------------>" + i);
            Log.d(TAG, "BlackMode -> Navigation -> .mNodes.mManeuverType ->" + road.mNodes.get(i).mManeuverType);
            Log.d(TAG, "BlackMode -> Navigation -> .mNodes.mInstructions ->" + road.mNodes.get(i).mInstructions);
            Log.d(TAG, "BlackMode -> Navigation -> .mNodes.mDuration ->" + road.mNodes.get(i).mDuration);
            Log.d(TAG, "BlackMode -> Navigation -> .mNodes.mLength ->" + road.mNodes.get(i).mLength * 1000);
            Log.d(TAG, "BlackMode -> Navigation -> .mNodes.mLocation ->" + road.mNodes.get(i).mLocation);

            if (changeDirectionList.contains(node.mManeuverType)) {
                navigatorViewCallback.updateNavigatorView(node.mInstructions);
                break;
            }
        }
        Log.d(TAG, "BlackMode -> Navigation -> road.mManeuverType ->" + node.mManeuverType);
        Log.d(TAG, "BlackMode -> Navigation -> road.mInstructions ->" + node.mInstructions);
        navigatorViewCallback.updateNavigatorView(String.format(Locale.getDefault(), "%s -> %d", node.mInstructions, node.mManeuverType));


        if (noChangeDirectionList.contains(node.mManeuverType) && roadLengthInMeters < 10) {
            nextWaypoint = nextWaypoint + 1;
        }
        Log.d(TAG, "BlackMode -> Navigation -> return -> nextWaypoint -> " + nextWaypoint);

        return nextWaypoint;

//        int mCoordinatesSize = this.kmlMultiGeometry.mItems.get(0).mCoordinates.size();
//        for (int i=0; i<mCoordinatesSize; i++) {
//            Log.d(TAG, "BlackMode -> Navigation -> mCoordinates ->"+ i +" / "+ mCoordinatesSize);
//            GeoPoint endPoint = this.kmlMultiGeometry.mItems.get(0).mCoordinates.get(i);
//
//            waypoints.add(startPoint);
//            waypoints.add(endPoint);
//            Road road = roadManager.getRoad(waypoints);
//            Log.d(TAG, "BlackMode -> Navigation -> road ->"+ road);
//            road_list.add(road);
//            startPoint = endPoint;
//            waypoints = new ArrayList<GeoPoint>();
//        }
//
//
//        for (int h=0; h<road_list.size(); h++) {
//            Road road_item = road_list.get(h);
//            for (int i = 0; i<road_item.mNodes.size(); i++) {
//                RoadNode node = road_item.mNodes.get(i);
//                Log.d(TAG, "BlackMode -> Navigation -> node.mManeuverType ->" + node.mManeuverType);
//
//
//                Marker nodeMarker = new Marker(map);
//                nodeMarker.setPosition(node.mLocation);
//                nodeMarker.setIcon(nodeIcon);
//                nodeMarker.setTitle("Step " + i);
//                nodeMarker.setSnippet(node.mInstructions);
//                nodeMarker.setSubDescription(Road.getLengthDurationText(this.context, node.mLength, node.mDuration));
//                Drawable icon = this.context.getResources().getDrawable(R.drawable.ic_continue);
//                nodeMarker.setImage(icon);
//                map.getOverlays().add(nodeMarker);
//            }
//        }

    }

    public void setChangeDirectionList() {
        this.changeDirectionList.add(3);
        this.changeDirectionList.add(4);
        this.changeDirectionList.add(5);
        this.changeDirectionList.add(6);
        this.changeDirectionList.add(7);
        this.changeDirectionList.add(8);
        this.changeDirectionList.add(12);
        this.changeDirectionList.add(13);
        this.changeDirectionList.add(14);
    }

    public void setNoChangeDirectionList() {
        this.noChangeDirectionList.add(0);
        this.noChangeDirectionList.add(1);
        this.noChangeDirectionList.add(2);
        this.noChangeDirectionList.add(9);
        this.noChangeDirectionList.add(10);
        this.noChangeDirectionList.add(11);
        this.noChangeDirectionList.add(15);
        this.noChangeDirectionList.add(16);
        this.noChangeDirectionList.add(17);
        this.noChangeDirectionList.add(18);
        this.noChangeDirectionList.add(19);
        this.noChangeDirectionList.add(20);
        this.noChangeDirectionList.add(21);
        this.noChangeDirectionList.add(22);
        this.noChangeDirectionList.add(23);
        this.noChangeDirectionList.add(24);
        this.noChangeDirectionList.add(27);
        this.noChangeDirectionList.add(28);
        this.noChangeDirectionList.add(29);
        this.noChangeDirectionList.add(30);
        this.noChangeDirectionList.add(31);
        this.noChangeDirectionList.add(32);
        this.noChangeDirectionList.add(33);
        this.noChangeDirectionList.add(34);
        this.noChangeDirectionList.add(35);
        this.noChangeDirectionList.add(36);
        this.noChangeDirectionList.add(37);
        this.noChangeDirectionList.add(38);
        this.noChangeDirectionList.add(39);
    }
}

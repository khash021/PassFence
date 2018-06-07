package com.khashayarmortazavi.testgeofence;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.SphericalUtil;

import java.util.ArrayList;

public class MapViewGeofenceActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private FusedLocationProviderClient mFusedLocationClient;

    private ArrayList<Fence> fenceArrayList;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map_view);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        fenceArrayList = MainActivity.loadArrayList(this);

    }//onCreate

    @Override
    public void onMapReady(GoogleMap map) {
        mMap = map;

        //check for location permission
        if (!MainActivity.checkPermission(this)) {
            return;
        }//permission

        mMap.setMyLocationEnabled(true);

//        //move the camera
//        mFusedLocationClient.getLastLocation()
//                .addOnSuccessListener(this, new OnSuccessListener<Location>() {
//                    @Override
//                    public void onSuccess(Location location) {
//                        if (location != null) {
//                            LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
//                            mMap.animateCamera(CameraUpdateFactory
//                                    .newLatLng(latLng));
//                        }//if
//                    }//onSuccess
//                });

        addGeofenceToMap();
    }//onMapReady

    private void addGeofenceToMap() {

        //create our bound object to show everything
        LatLngBounds.Builder builder = new LatLngBounds.Builder();

        for (Fence fence : fenceArrayList) {
            //get the data from the fence object
            String name = fence.getId();
            LatLng latLng = fence.getLatLng();
            float radius = fence.getRadius();
            long duration = fence.getDuration();
            if (duration != -1) {
                duration = duration / 3600000; //convert to hour
            }
            String durationString;
            if (duration == -1) {
                durationString = "Never";
            } else {
                durationString = String.valueOf(duration) + "hours";
            }
            String type = "";
            switch (fence.getType()) {
                case Fence.FENCE_TYPE_ENTER:
                    type = "Enter";
                    break;
                case Fence.FENCE_TYPE_EXIT:
                    type = "Exit";
                    break;
                case Fence.FENCE_TYPE_ENTER_EXIT:
                    type = "Enter/Exit";
                    break;
            }
            String snippet = "Duration: " + durationString +
                    "\nTrigger: " + type;

            Marker marker = mMap.addMarker(new MarkerOptions()
                    .position(latLng)
                    .title(name)
                    .snippet(snippet)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
            );

            Circle circle = mMap.addCircle(new CircleOptions()
                    .center(latLng)
                    .radius(radius)
                    .strokeWidth(5.0f)
                    .strokeColor(getResources().getColor(R.color.circleStroke))
                    .fillColor(getResources().getColor(R.color.circleFill)));

            //get the bounds of the circle and add it to the builder
            LatLng swCorner = swCorner(latLng, radius);
            LatLng neCorner = neCorner(latLng, radius);

            builder.include(swCorner);
            builder.include(neCorner);
        }//for

        //build the builder
        LatLngBounds bounds = builder.build();

        mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 200));//this is the pixel padding


    }//addGeofenceToMap

    //helper methods to get the bounds of the cirlcle
    private LatLng swCorner(LatLng center, float radius ) {
        double distanceFromCenterToCorner = ((double) radius) * Math.sqrt(2.0);
        LatLng southwestCorner =
                SphericalUtil.computeOffset(center, distanceFromCenterToCorner, 225.0);
        return southwestCorner;
    }

    private LatLng neCorner (LatLng center, float radius ) {
        double distanceFromCenterToCorner = ((double) radius) * Math.sqrt(2.0);
        LatLng northeastCorner =
                SphericalUtil.computeOffset(center, distanceFromCenterToCorner, 45.0);
        return northeastCorner;
    }


}//MapViewGeofenceActivity

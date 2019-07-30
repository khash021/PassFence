package tech.khash.testgeofence;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;

public class MapViewGeofenceActivity extends AppCompatActivity implements OnMapReadyCallback {

    private final String TAG = MapViewGeofenceActivity.class.getSimpleName();

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

        //use custom window
        mMap.setInfoWindowAdapter(new CustomInfoWindowAdapter());

        //check for location permission
        if (MainActivity.checkPermission(this)) {
            mMap.setMyLocationEnabled(true);
        }//permission


        //disable map toolbar
        UiSettings mUiSettings = mMap.getUiSettings();
        mUiSettings.setMapToolbarEnabled(false);

        addGeofenceToMap();
    }//onMapReady

    private void addGeofenceToMap() {

        //check to make sure that the list is not null in case there is no fence added
        if (fenceArrayList == null) {
            Toast.makeText(this, "No Geofences to show", Toast.LENGTH_SHORT).show();
            Log.v(TAG, "Geofence ArrayList null");
            return;
        }//if

        //create our bound object to show everything
        LatLngBounds.Builder builder = new LatLngBounds.Builder();

        for (Fence fence : fenceArrayList) {
            //get the data from the fence object
            String name = fence.getId();
            LatLng latLng = fence.getLatLng();
            float radius = fence.getRadius();
            long duration = fence.getDuration();
            String snippet = fence.getSnippet();

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
            LatLng swCorner = MainActivity.swCorner(latLng, radius);
            LatLng neCorner = MainActivity.neCorner(latLng, radius);

            builder.include(swCorner);
            builder.include(neCorner);
        }//for

        //build the builder
        LatLngBounds bounds = builder.build();

        //Setting the width and height of your screen
        int width = getResources().getDisplayMetrics().widthPixels;
        int height = getResources().getDisplayMetrics().heightPixels;
        int padding = (int) (width * 0.1); // offset from edges of the map 10% of screen

        mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, width, height, padding));//this is the pixel padding


    }//addGeofenceToMap



    /**
     * This class is for customizing Info Windows
     */
    class CustomInfoWindowAdapter implements GoogleMap.InfoWindowAdapter {

        // Viewgroup containing 3 ImageViews with ids "garbage, container, and paper"
        private final View mContent;

        CustomInfoWindowAdapter() {
            mContent = getLayoutInflater().inflate(R.layout.custom_info_content, null);
        }//CustomInfoWindowAdapter

        /**
         * These two methods are part of the InfoWindowAdapter interface that we have to implement
         * It first calls getInfoWindow, and if this turns null, then it goes to getInfoContent;
         * if that one also returns null, then the default behavior will happen; which is the default
         * Info Window.
         *
         * @param marker is the markerMyLocation that was clicked on
         * @return view
         */
        @Override
        public View getInfoWindow(Marker marker) {
            return null;
        }//getInfoWindow

        @Override
        public View getInfoContents(Marker marker) {
            //For prototype, this returns null in case they click on marker (this will produce runtime error
            //since the markers dont have a tag
            render(marker, mContent);
            return mContent;
        }//getInfoContents

        /**
         * This is the helper method for making the custom Info Window.
         * In the layout file (custom_info_content), get the data from marker and set them
         *
         * @param marker is the markerMyLocation that was clicked on, passed using the callbacks above
         */
        private void render(Marker marker, View view) {
            //find the title text view and make it invisible (to prevent bugs for other info windows)
            TextView nameText = view.findViewById(R.id.text_name);
            TextView durationText = view.findViewById(R.id.text_duration);
            TextView criteriaText = view.findViewById(R.id.text_criteria);

            String snippet = marker.getSnippet();

            //TODO: add try/catch
            String[] properties = snippet.trim().split(",");
            String name = properties[0];
            String expiary = properties[1];
            String type = properties[2];

            nameText.setText("Id: " + name);
            durationText.setText(expiary);
            criteriaText.setText("Criteria: " + type);
        }//render
    }//CustomInfoWindowAdapter


}//MapViewGeofenceActivity

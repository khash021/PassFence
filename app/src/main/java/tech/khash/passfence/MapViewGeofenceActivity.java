package tech.khash.passfence;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
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

/**
 * Created by Khashayar "Khash" Mortazavi
 *
 * This is the class that hosts a Google Map fragment and shows the registered geofences on the map
 */

public class MapViewGeofenceActivity extends AppCompatActivity implements OnMapReadyCallback,
        GoogleMap.OnCircleClickListener {

    private final String TAG = MapViewGeofenceActivity.class.getSimpleName();

    private GoogleMap mMap;

    private ArrayList<Fence> fenceArrayList;
    private ArrayList<MarkerCircle> markerCircleArrayList;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map_view);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        fenceArrayList = MainActivity.loadArrayList(this);

    }//onCreate

    @Override
    public void onMapReady(GoogleMap map) {
        mMap = map;

        //use custom window
        mMap.setInfoWindowAdapter(new CustomInfoWindowAdapter());

        //check for location permission
        if (MainActivity.checkLocationPermission(this)) {
            mMap.setMyLocationEnabled(true);
        }//permission


        //disable map toolbar
        UiSettings mUiSettings = mMap.getUiSettings();
        mUiSettings.setMapToolbarEnabled(false);

        //set click listener for the circles and callbacks will be send to this activity
        mMap.setOnCircleClickListener(this);

        //add the locations to the map
        addGeofenceToMap();
    }//onMapReady



    //This gets called, when the circle is clicked and we will show the corresponding marker's info window
    @Override
    public void onCircleClick(Circle circle) {
        //get the name
        String tag = (String) circle.getTag();

        //find the corresponding markerCircle object and show the marker's info window
        for (MarkerCircle markerCircle : markerCircleArrayList) {
            //get the tag
            String markerCircleTag = (String) markerCircle.getTag();
            if (markerCircleTag.equalsIgnoreCase(tag)) {
                //get the corresponding marker
                Marker marker = markerCircle.getMarker();
                //show the marker's info window
                marker.showInfoWindow();
                return;
            }//if
        }//for
    }//onCircleClick

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        //get the inflater
        MenuInflater inflater = getMenuInflater();

        //inflate the menu
        inflater.inflate(R.menu.menu_map, menu);

        //You must return true for the menu to be displayed; if you return false it will not be shown.
        return true;
    }//onCreateOptionsMenu

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_add_map) {
            Intent intent = new Intent(MapViewGeofenceActivity.this, AddGeofenceActivity.class);
            startActivity(intent);
            return true;
        }//if

        return super.onOptionsItemSelected(item);
    }//onOptionsItemSelected

    /* Helper method for adding locations to the map from the arrayList
       This method only gets called from onMapReady, so there is no need to check for null map */
    private void addGeofenceToMap() {

        //check to make sure that the list is not null in case there is no fence added
        if (fenceArrayList == null) {
            Toast.makeText(this, getString(R.string.no_geofence_show_toast), Toast.LENGTH_SHORT).show();
            Log.v(TAG, "Geofence ArrayList null");
            return;
        }//if

        //create our bound object to show everything
        LatLngBounds.Builder builder = new LatLngBounds.Builder();

        //our ArrayList of MarkerCircle object. We add all markers and circle to this list in the
        //following loopo, then loop through the list and draw them on the map
        markerCircleArrayList = new ArrayList<MarkerCircle>();

        //our marker option and circle option objects to make the marker and circle from
        MarkerOptions markerOptions;
        CircleOptions circleOptions;
        Marker marker;
        Circle circle;
        for (Fence fence : fenceArrayList) {
            //get the data from the fence object
            String name = fence.getId();
            LatLng latLng = fence.getLatLng();
            float radius = fence.getRadius();
            long duration = fence.getDuration();
            String snippet = fence.getSnippet();

            //create the markeroption and marker
            markerOptions = new MarkerOptions();
            markerOptions.position(latLng)
                    .title(name)
                    .snippet(snippet)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));

            marker = mMap.addMarker(markerOptions);

            //create circle options and circle
            circleOptions = new CircleOptions();
            circleOptions.center(latLng)
                    .radius(radius)
                    .strokeWidth(5.0f)
                    .strokeColor(getResources().getColor(R.color.circleStroke))
                    .fillColor(getResources().getColor(R.color.circleFill));
            circle = mMap.addCircle(circleOptions);
            //set the circle clickable, onCircleClick will be called
            circle.setClickable(true);
            //set a tag on the circle to be retrieved on the circle click
            circle.setTag(name);

            //create a new object and add it to the arraylist
            MarkerCircle markerCircle = new MarkerCircle(marker, circle);
            markerCircleArrayList.add(markerCircle);

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
            //get the snippet from marker that contains the info regarding that geofence
            String snippet = marker.getSnippet();

            //use a try catch to get the info and set them accordingly
            try {
                String[] properties = snippet.trim().split(",");
                String name = properties[0];
                String expiry = properties[1];
                String type = properties[2];

                String text = getString(R.string.id_colon) + " " + name;
                String expires = getString(R.string.expires_colon) + " " + expiry;
                String criteria = getString(R.string.criteria_colon) + " " + type;
                nameText.setText(text);
                durationText.setText(expires);
                criteriaText.setText(criteria);
            } catch (Exception e) {
                Log.e(TAG, "Error getting snippet properties from marker + " + marker.getId(), e);
            }
        }//render
    }//CustomInfoWindowAdapter
}//MapViewGeofenceActivity

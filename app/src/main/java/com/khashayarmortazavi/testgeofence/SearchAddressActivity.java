package com.khashayarmortazavi.testgeofence;

import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class SearchAddressActivity extends AppCompatActivity implements OnMapReadyCallback {

    private final String TAG = SearchAddressActivity.class.getSimpleName();

    private GoogleMap mMap;

    private boolean local;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_address);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        final EditText addressText = findViewById(R.id.text_address);
        ((Button) findViewById(R.id.bttn_search)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String address = addressText.getText().toString().trim();
                getAddress(address);
            }
        });

        CheckBox localCheckBox = findViewById(R.id.box_local);
        localCheckBox.setChecked(true);
        local = localCheckBox.isChecked();
        localCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                local = isChecked;
            }
        });

    }//onCreate

    private void getAddress(String address) {
        //check for geocoder availability
        if (!Geocoder.isPresent()) {
            Log.v(TAG, "Geocoder not available");
            Toast.makeText(this, "Geocoder not available", Toast.LENGTH_SHORT).show();
            return;
        }
        //No we know it is available, Create geocoder to retrieve the location
        // responses will be localized for the given Locale. (A Locale object represents a specific geographical, political, or cultural region. An operation that requires a Locale to perform its task is called locale-sensitive )
        //based on the checkbox, determine local or not
        Geocoder geocoder;
        Locale locale;
        if (local) {
            locale = Locale.getDefault();
            geocoder = new Geocoder(this, locale);
        } else {
            geocoder = new Geocoder(this);
        }

        try {
            List<Address> addresses = geocoder.getFromLocationName(address, 5);
            //check to make sure we got results
            if (addresses.size() < 1) {
                Toast.makeText(this, "No results found", Toast.LENGTH_SHORT).show();
                Log.v(TAG, "No results");
                return;
            }//if
            //check the map first
            if (!checkMapReady()) {
                Log.v(TAG, "Map not ready");
                return;
            }
            //make a builder to include all points
            LatLngBounds.Builder builder = new LatLngBounds.Builder();

            //clear the map of previous markers
            mMap.clear();

            //go through all the results and put them on map
            int counter = 1;
            for (Address result : addresses) {
                LatLng latLng = new LatLng(result.getLatitude(), result.getLongitude());
                String title = "Result-" + counter;
                Marker marker = mMap.addMarker(new MarkerOptions()
                        .position(latLng)
                        .title(title));
                builder.include(latLng);
                counter++;
            }//for
            //don't need to do the builder if there is only one result
            if (counter == 2) {
                LatLng latLng = new LatLng(addresses.get(0).getLatitude(), addresses.get(0).getLongitude());
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 13.0f));
                return;
            }
            LatLngBounds bounds = builder.build();
            //Setting the width and height of your screen
            int width = getResources().getDisplayMetrics().widthPixels;
            int height = getResources().getDisplayMetrics().heightPixels;
            int padding = (int) (width * 0.2); // offset from edges of the map 20% of screen

            mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, width, height, padding));//this is the pixel padding

        } catch (IOException e) {
            Log.e(TAG, "Error getting location", e);
        }//try/catch
    }//getAddress

    @Override
    public void onMapReady(GoogleMap map) {
        mMap = map;

        if (MainActivity.checkPermission(this)) {
            mMap.setMyLocationEnabled(true);
        }
    }//onMapReady

    private boolean checkMapReady() {
        if (mMap == null) {
            Toast.makeText(this, "Map not ready", Toast.LENGTH_SHORT).show();
            return false;
        } else {
            return true;
        }
    }//checkMapReady

}//main class

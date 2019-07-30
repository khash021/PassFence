package tech.khash.testgeofence;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
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

public class SearchAddressActivity extends AppCompatActivity implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private final String TAG = SearchAddressActivity.class.getSimpleName();

    private GoogleMap mMap;

    private boolean local;

    protected GoogleApiClient mGoogleApiClient;
    private Location mLocation;

    private static final float RADIUS_RANGE = 100000; //100 km


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_address);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        buildGoogleApiClient();

        final EditText addressText = findViewById(R.id.text_address);
        ((Button) findViewById(R.id.bttn_search)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String address = addressText.getText().toString().trim();
                //hide the keyboard now
                hideKeyboard();
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

    private void hideKeyboard() {
        //check to make sure no view has focus
        View view = this.getCurrentFocus();

        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }//hideKeyboard

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
            //check for the user location permission, if there is permission, and  we have a location,
            //then we will only show results within a 100 km radius for better accuracy
            if (mLocation != null && local) {
                LatLng userLatLng = new LatLng(mLocation.getLatitude(), mLocation.getLongitude());
                //calculate a bound
                LatLng neLatLng = MainActivity.neCorner(userLatLng, RADIUS_RANGE);
                LatLng swLatLng = MainActivity.swCorner(userLatLng, RADIUS_RANGE);
                LatLngBounds.Builder resultBoundBuilder = new LatLngBounds.Builder();

                resultBoundBuilder.include(neLatLng);
                resultBoundBuilder.include(swLatLng);

                LatLngBounds resultBound = resultBoundBuilder.build();

                //make a builder to include all points
                LatLngBounds.Builder builder = new LatLngBounds.Builder();

                //clear the map of previous markers
                mMap.clear();

                //go through all the results and put them on map
                int counter = 1;
                for (Address result : addresses) {
                    LatLng latLng = new LatLng(result.getLatitude(), result.getLongitude());
                    //only show the result, if it is within our set radius
                    if (resultBound.contains(latLng)) {
                        String title = "Result-" + counter;
                        Marker marker = mMap.addMarker(new MarkerOptions()
                                .position(latLng)
                                .title(title));
                        builder.include(latLng);
                        counter++;
                    }
                }//for
                //we need to make sure, at least one result was within bounds. if our counter is still 1
                //that means nothing was withing that region
                if (counter == 1) {
                    int radius = ((int) RADIUS_RANGE) / 1000;
                    Toast.makeText(this, "No results are within " + radius +
                            " meters of your location", Toast.LENGTH_SHORT).show();
                    return;
                }

                //don't need to do the builder if there is only one result
                if (counter == 2) {
                    LatLng latLng = new LatLng(addresses.get(0).getLatitude(), addresses.get(0).getLongitude());
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 13.0f));
                    return;
                }

                //if we got this far, it means, we have more than 1 result within the bounds
                LatLngBounds bounds = builder.build();
                //Setting the width and height of your screen
                int width = getResources().getDisplayMetrics().widthPixels;
                int height = getResources().getDisplayMetrics().heightPixels;
                int padding = (int) (width * 0.2); // offset from edges of the map 20% of screen

                mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, width, height, padding));//this is the pixel padding
            } else {
                //this is the case when there is no user location, we will show them all

                //make a builder to include all points
                LatLngBounds.Builder builder = new LatLngBounds.Builder();

                //clear the map of previous markers
                mMap.clear();

                //go through all the results and put them on map
                int counter = 1;
                for (Address result : addresses) {
                    LatLng latLng = new LatLng(result.getLatitude(), result.getLongitude());
                    //only show the result, if it is within our set radius
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
            }//if-else for user locations and bounds

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

    protected synchronized void buildGoogleApiClient() {
        Log.v(TAG, "buildGoogleApiClient called");

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }//buildGoogleApiClient

    @Override
    protected void onStart() {
        super.onStart();
        if (!mGoogleApiClient.isConnecting() || !mGoogleApiClient.isConnected()) {
            mGoogleApiClient.connect();
        }
    }//onStart

    @Override
    protected void onStop() {
        super.onStop();
        if (mGoogleApiClient.isConnecting() || mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
    }//onStop

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        //move camera to users location
        if (!MainActivity.checkPermission(this)) {
            return;
        }
        Location location = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);

        if (location != null) {
            mLocation = location;
        }

    }//onConnected

    @Override
    public void onConnectionSuspended(int i) {
    }//onConnectionSuspended

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
    }//onConnectionFailed
}//main class

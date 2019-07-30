package tech.khash.testgeofence;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.SearchView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

//TODO: finish the activity on the results callback
//TODO: add suggestion to the search
//TODO: searchAddress method:  either only query one result and remove the bound
        //or maybe add a large bound around the user so you would only show the results that is close to them
        //I dont wanna see results in fucking Florida when I'm in vancouver


public class AddGeofenceActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, ResultCallback<Status>, OnMapReadyCallback,
        GoogleMap.OnCameraIdleListener {

    private static final String TAG = AddGeofenceActivity.class.getSimpleName();


    protected GoogleApiClient mGoogleApiClient;
    private GeofencingClient mGeofencingClient;
    private PendingIntent mGeofencePendingIntent;
    private GoogleMap mMap;
    private LatLng geoFenceLatLng;

    //geofence stuff
    private float mRadius;
    private long mDuration;
    private EditText mNameText;

    private CheckBox mBoxEnter, mBoxExit;


    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_geofence);
        Log.v(TAG, "onCreate Called");

        //Add map fragment
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);


        // Kick off the request to build GoogleApiClient.
        buildGoogleApiClient();

        //create an instance of the Geofencing client to access the location APIs
        mGeofencingClient = LocationServices.getGeofencingClient(this);

        //buttons
        Button addGeofenceButton = findViewById(R.id.button_add_geo);
        addGeofenceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addFence();
            }
        });//addGeofenceButton

        mNameText = findViewById(R.id.text_name);

        Spinner durationSpinner = findViewById(R.id.spinner_duration);
        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<CharSequence> spinnerAdapter = ArrayAdapter.createFromResource(this,
                R.array.spinner_duration_array, android.R.layout.simple_spinner_dropdown_item);
        // Specify the layout to use when the list of choices appears
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the spinner
        durationSpinner.setAdapter(spinnerAdapter);
        //initialized the array at 12 hours and set the radius to that
        durationSpinner.setSelection(7);
        mDuration = 12;
        //set listener
        durationSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                setDuration(position);

            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        //checkboxes

        mBoxEnter = findViewById(R.id.check_enter);
        mBoxExit = findViewById(R.id.check_exit);

        SeekBar seekBar = findViewById(R.id.seek_bar);
        //default at 50 meters
        seekBar.setProgress(5);
        mRadius = 50;
        final TextView radiusTextView = findViewById(R.id.text_radius);
        radiusTextView.setText("50");
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                //set the radius to 5 if it is at the very beginning (0 progress), otherwise, multiply by 10
                mRadius = (progress < 1) ? 5.0f : ((float) progress * 10);
                //draw circle
                drawCircle(geoFenceLatLng, mRadius);
                //add the radius
                String radius = String.valueOf(mRadius);
                radius = radius.substring(0, radius.indexOf("."));
                radiusTextView.setText(radius);
            }//onProgressChanged

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }//onStartTrackingTouch

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                //set the radius
                int radius = seekBar.getProgress();
                if (radius < 1) {
                    mRadius = 5.0f;
                } else {
                    mRadius = (float) seekBar.getProgress() * 10;
                }
                //add the radius
                String radiusString = String.valueOf(mRadius);
                radiusString = radiusString.substring(0, radiusString.indexOf("."));
                ((TextView) findViewById(R.id.text_radius)).setText(radiusString);

                //add marker to center
                if (geoFenceLatLng == null) {
                    Log.v(TAG, "location null");
                    return;
                }

                //place marker
                Marker marker = mMap.addMarker(new MarkerOptions()
                        .position(geoFenceLatLng));

                //check to see if we see the whole circle
                LatLngBounds visibleBound = mMap.getProjection().getVisibleRegion().latLngBounds;
                LatLng swCorner = MainActivity.swCorner(geoFenceLatLng, mRadius);
                LatLng neCorner = MainActivity.neCorner(geoFenceLatLng, mRadius);
                if (!visibleBound.contains(swCorner) || !visibleBound.contains(neCorner)) {
                    visibleBound.including(swCorner);
                    visibleBound.including(neCorner);
                    mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(visibleBound, 200));
                }

            }//onStopTrackingTouch
        });//SeekBarChangeListener

        if (getIntent().hasExtra(MainActivity.FENCE_EDIT_EXTRA_INTENT)) {
            String fenceId = getIntent().getStringExtra(MainActivity.FENCE_EDIT_EXTRA_INTENT);
            setupModifyMode(fenceId);
        }


    }//onCreate



    @Override
    public void onMapReady(GoogleMap map) {
        Log.v(TAG, "onMapReady called");
        mMap = map;

        mMap.setOnCameraIdleListener(this);

        //disable map toolbar
        UiSettings mUiSettings = mMap.getUiSettings();
        mUiSettings.setMapToolbarEnabled(false);

        if (MainActivity.checkPermission(this)) {
            mMap.setMyLocationEnabled(true);
        }
    }//onMapReady

    @Override
    public void onCameraIdle() {
        geoFenceLatLng = mMap.getCameraPosition().target;
    }//onCameraIdle

    private void drawCircle(LatLng center, float radius) {
        //draw circle on map
        if (mMap == null || center == null) {
            Toast.makeText(getApplicationContext(), "Error drawing circle", Toast.LENGTH_SHORT).show();
            return;
        }//if map null

        mMap.clear();
        Circle circle = mMap.addCircle(new CircleOptions()
                .center(center)
                .radius(radius)
                .strokeWidth(5.0f)
                .strokeColor(getResources().getColor(R.color.circleStroke))
                .fillColor(getResources().getColor(R.color.circleFill)));

    }//drawCircle

    //helper method for testing. this only adds the data to the array list of shared pref. It does not actually
    //turn it on. that is done later

    private void addFence() {
        //here we extract the Fence data
        String name = mNameText.getText().toString().trim();
        int type;
        boolean enter = mBoxEnter.isChecked();
        boolean exit = mBoxExit.isChecked();

        //check to make sure we have name
        if (name.length() < 1) {
            Toast.makeText(this, "Name required", Toast.LENGTH_SHORT).show();
            return;
        }

        //check to makes ure at least one checkbox is checked
        if (!enter && !exit) {
            Toast.makeText(this, "Criteria required", Toast.LENGTH_SHORT).show();
            return;
        }

        //check against duplicate ids
        ArrayList<Fence> savedFenceArrayList = MainActivity.loadArrayList(this);
        //check to make sure the array is not null
        if (savedFenceArrayList != null) {
            ArrayList<String> idArrayList = new ArrayList<>();
            for (Fence fence : savedFenceArrayList) {
                idArrayList.add(fence.getId().toLowerCase());
            }//for
            if (idArrayList.contains(name.toLowerCase())) {
                Toast.makeText(this, "Name is already registered", Toast.LENGTH_SHORT).show();
                return;
            }//if
        }

        //set the type
        //TODO: fix this
        if (enter && exit) {
            type = Fence.FENCE_TYPE_ENTER_EXIT;
        } else if (enter && !exit) {
            type = Fence.FENCE_TYPE_ENTER;
        } else if (!enter && exit) {
            type = Fence.FENCE_TYPE_EXIT;
        } else {
            type = -1;
        }

        //now we have done all the checks, we can safely add the geofence to the list
        //TODO: check for null pointer
        Fence fence = new Fence(name, geoFenceLatLng.latitude, geoFenceLatLng.longitude,
                mRadius, mDuration, type);

        ArrayList<Fence> fenceArrayList = new ArrayList<>();
        fenceArrayList.add(fence);
        MainActivity.saveArrayList(this, fenceArrayList);

        addGeofence(fence);
    }//addFence

    //TODO: move all of this in the Fence class. just call getBuilder
    private void addGeofence(final Fence fence) {
        //get the info from the fence
        String id = fence.getId();
        double lat = fence.getLatitude();
        double lng = fence.getLongitude();
        float radius = fence.getRadius();
        long duration = fence.getDuration();

        //use switch for different types:
        switch (fence.getType()) {
            case Fence.FENCE_TYPE_ENTER:
                //make the builder
                Geofence.Builder geofenceBuilderEnter = new Geofence.Builder();
                geofenceBuilderEnter.setRequestId(id)
                        .setCircularRegion(lat, lng, radius)
                        .setExpirationDuration(duration)
                        .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER);

                //build a geofence object using the builder
                Geofence geofenceEnter = geofenceBuilderEnter.build();

                GeofencingRequest.Builder requestBuilderEnter = new GeofencingRequest.Builder();
                //this means that GEOFENCE_TRANSITION_ENTER should be triggered if the device is already inside the geofence
                requestBuilderEnter.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER);
                requestBuilderEnter.addGeofence(geofenceEnter);

                //build the Geofencing  request object using the builder
                GeofencingRequest requestEnter = requestBuilderEnter.build();

                if (MainActivity.checkPermission(this)) {
                    mGeofencingClient.addGeofences(requestEnter, getGeofencePendingIntent())
                            .addOnSuccessListener(this, new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    Toast.makeText(getApplicationContext(), "Geofence added", Toast.LENGTH_SHORT).show();
                                    Log.v(TAG, "Geofence added");
                                    finish();
                                }
                            })
                            .addOnFailureListener(this, new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    Toast.makeText(getApplicationContext(), "Error adding geofence", Toast.LENGTH_SHORT).show();
                                    Log.v(TAG, "Geofence adding failed", e);
                                }
                            });
                }//if permission
                break;
            case Fence.FENCE_TYPE_EXIT:
                //make the builder
                Geofence.Builder geofenceBuilderExit = new Geofence.Builder();
                geofenceBuilderExit.setRequestId(id)
                        .setCircularRegion(lat, lng, radius)
                        .setExpirationDuration(duration)
                        .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_EXIT);

                //build a geofence object using the builder
                Geofence geofenceExit = geofenceBuilderExit.build();

                GeofencingRequest.Builder requestBuilderExit = new GeofencingRequest.Builder();
                //this means that GEOFENCE_TRANSITION_ENTER should be triggered if the device is already inside the geofence
                requestBuilderExit.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER);
                requestBuilderExit.addGeofence(geofenceExit);

                //build the Geofencing  request object using the builder
                GeofencingRequest requestExit = requestBuilderExit.build();

                if (MainActivity.checkPermission(this)) {
                    mGeofencingClient.addGeofences(requestExit, getGeofencePendingIntent())
                            .addOnSuccessListener(this, new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    Toast.makeText(getApplicationContext(), "Geofence added", Toast.LENGTH_SHORT).show();
                                    Log.v(TAG, "Geofence added");
                                    finish();
                                }
                            })
                            .addOnFailureListener(this, new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    Toast.makeText(getApplicationContext(), "Error adding geofence", Toast.LENGTH_SHORT).show();
                                    Log.v(TAG, "Geofence adding failed", e);
                                }
                            });
                }//if permission
                break;

            case Fence.FENCE_TYPE_ENTER_EXIT:
                //make the builder
                Geofence.Builder geofenceBuilderBoth = new Geofence.Builder();
                geofenceBuilderBoth.setRequestId(id)
                        .setCircularRegion(lat, lng, radius)
                        .setExpirationDuration(duration)
                        //TODO: fix this
                        .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT);

                //build a geofence object using the builder
                Geofence geofenceBoth = geofenceBuilderBoth.build();

                GeofencingRequest.Builder requestBuilderBoth = new GeofencingRequest.Builder();
                //this means that GEOFENCE_TRANSITION_ENTER should be triggered if the device is already inside the geofence
                requestBuilderBoth.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER);
                requestBuilderBoth.addGeofence(geofenceBoth);

                //build the Geofencing  request object using the builder
                GeofencingRequest requestBoth = requestBuilderBoth.build();

                if (MainActivity.checkPermission(this)) {
                    mGeofencingClient.addGeofences(requestBoth, getGeofencePendingIntent())
                            .addOnSuccessListener(this, new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    Toast.makeText(getApplicationContext(), "Geofence added", Toast.LENGTH_SHORT).show();
                                    Log.v(TAG, "Geofence added");
                                    finish();
                                }
                            })
                            .addOnFailureListener(this, new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    Toast.makeText(getApplicationContext(), "Error adding geofence", Toast.LENGTH_SHORT).show();
                                    Log.v(TAG, "Geofence adding failed", e);
                                }
                            });
                }//if permission
                break;
        }//switch
    }//addGeofence

    //helper method to define a PendingIntent that starts an IntentService
    private PendingIntent getGeofencePendingIntent() {
        Log.v(TAG, "getGeofencePendingIntent called");

        // Reuse the PendingIntent if we already have it.
        if (mGeofencePendingIntent != null) {
            return mGeofencePendingIntent;
        }
        Intent intent = new Intent(this, GeofenceTransitionsIntentService.class);
        // We use FLAG_UPDATE_CURRENT so that we get the same pending intent back when
        // calling addGeofences() and removeGeofences().
        mGeofencePendingIntent = PendingIntent.getService(this, 0, intent, PendingIntent.
                FLAG_UPDATE_CURRENT);
        return mGeofencePendingIntent;
    }//getGeofencePendingIntent

    @Override
    public void onResult(@NonNull Status status) {
        if (status.isSuccess()) {
            Toast.makeText(
                    this,
                    "Geofences Added",
                    Toast.LENGTH_SHORT
            ).show();
        } else {
            // Get the status code for the error and log it using a user-friendly message.
            String errorMessage = GeofenceErrorMessages.getErrorString(this,
                    status.getStatusCode());
            Log.e(TAG, errorMessage);
        }

    }//onResult

    private void setDuration(int index) {
        String[] durationArray = getResources().getStringArray(R.array.spinner_duration_array);
        switch (index) {
            case 0:
                mDuration = Long.parseLong(durationArray[0]);
                break;
            case 1:
                mDuration = Long.parseLong(durationArray[1]);
                break;
            case 2:
                mDuration = Long.parseLong(durationArray[2]);
                break;
            case 3:
                mDuration = Long.parseLong(durationArray[3]);
                break;
            case 4:
                mDuration = Long.parseLong(durationArray[4]);
                break;
            case 5:
                mDuration = Long.parseLong(durationArray[5]);
                break;
            case 6:
                mDuration = Long.parseLong(durationArray[6]);
                break;
            case 7:
                mDuration = Long.parseLong(durationArray[7]);
                break;
            case 8:
                mDuration = Long.parseLong(durationArray[8]);
                break;
            case 9:
                mDuration = -1;
                break;
        }//switch
    }//setDuration


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
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(), location.getLongitude()), 15.0f));
        }


    }//onConnected

    @Override
    public void onConnectionSuspended(int i) {
        mGoogleApiClient.connect();
    }//onConnectionSuspended

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
    }//onConnectionFailed

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //inflate the menu
        getMenuInflater().inflate(R.menu.menu_search, menu);

        //find the search item
        final MenuItem searchItem = menu.findItem(R.id.action_search);

        //check to make sure, it is not null
        if (searchItem != null) {
            //create a SearchView object using the search menu item
            SearchView searchView = (SearchView) MenuItemCompat.getActionView(searchItem);
            //add hint
            searchView.setQueryHint("Enter address");
            //this means it closes the keyboard when the user clicks the search button
            searchView.setIconifiedByDefault(true);

            //get a reference to the search box, so we can change the input type to cap words
            int id1 = searchView.getContext().getResources().getIdentifier("android:id/search_src_text", null, null);
            EditText searchEditText = (EditText) searchView.findViewById(id1);
            searchEditText.setInputType(InputType.TYPE_TEXT_FLAG_CAP_WORDS);

            // use this method for search process
            searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                //Called when the user submits the query. This could be due to a key press on the keyboard or
                // due to pressing a submit button. The listener can override the standard behavior by returning true to
                // indicate that it has handled the submit request. Otherwise return false to let the SearchView handle the
                // submission by launching any associated intent.
                @Override
                public boolean onQueryTextSubmit(String query) {
                    // use this method when query submitted
                    searchAddress(query);
                    return false;
                }

                //Called when the query text is changed by the user.
                @Override
                public boolean onQueryTextChange(String newText) {
                    // use this method for auto complete search process
                    return false;
                }
            });//query text change listener
        }//if
        return true;
    }//onCreateOptionsMenu

    private void searchAddress(String query) {
        //check for geocoder availability
        if (!Geocoder.isPresent()) {
            Log.v(TAG, "Geocoder not available");
            Toast.makeText(this, "Geocoder not available", Toast.LENGTH_SHORT).show();
            return;
        }
        //Now we know it is available, Create geocoder to retrieve the location
        // responses will be localized for the given Locale. (A Locale object represents a specific geographical,
        // political, or cultural region. An operation that requires a Locale to perform its task is called locale-sensitive )

        //create localized geocoder
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());


        try {
            //the second parameter is the number of max results, here we set it to 3
            List<Address> addresses = geocoder.getFromLocationName(query, 3);
            //check to make sure we got results
            if (addresses.size() < 1) {
                Toast.makeText(this, "No results found", Toast.LENGTH_SHORT).show();
                Log.v(TAG, "No results");
                return;
            }//if

            //check the map first
            if (mMap == null) {
                Log.v(TAG, "Map not ready");
                return;
            }

            //make a builder to include all points
            LatLngBounds.Builder builder = new LatLngBounds.Builder();

            //clear the map
            mMap.clear();

            //go through all the results and put them on map
            int counter = 1;
            for (Address result : addresses) {
                LatLng latLng = new LatLng(result.getLatitude(), result.getLongitude());
                Marker marker = mMap.addMarker(new MarkerOptions()
                        .position(latLng));
                //include the marker
                builder.include(latLng);
                counter++;
            }//for

            //don't need to set bounds if there is only one result. Just move the camera
            if (counter == 2) {
                LatLng latLng = new LatLng(addresses.get(0).getLatitude(), addresses.get(0).getLongitude());
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 13.0f));
                return;
            }

            //since we have more than one results, we want to show them all, so we need the builder
            //build the bounds builder
            LatLngBounds bounds = builder.build();
            //Setting the width and height of your screen (if not, sometimes the app crashes)
            int width = getResources().getDisplayMetrics().widthPixels;
            int height = getResources().getDisplayMetrics().heightPixels;
            int padding = (int) (width * 0.2); // offset from edges of the map 20% of screen

            mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, width, height, padding));//this is the pixel padding

        } catch (IOException e) {
            Log.e(TAG, "Error getting location", e);
        }//try/catch
    }//searchAddress

    private void hideKeyboard() {
        //check to make sure no view has focus
        View view = this.getCurrentFocus();

        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }//hideKeyboard

    //TODO: this needs to be better. either use arraylist method to search, or just send the fence in as input
    private void setupModifyMode(String fenceId) {
        ArrayList<Fence> fenceArrayList = MainActivity.loadArrayList(this);

        for (Fence fence : fenceArrayList) {
            if (fence.getId().equalsIgnoreCase(fenceId)) {
                mNameText.setText(fenceId);
                return;
            }//if
        }//for
    }//setupModifyMode

}//Activity

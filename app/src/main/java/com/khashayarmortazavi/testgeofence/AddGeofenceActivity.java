package com.khashayarmortazavi.testgeofence;

import android.app.PendingIntent;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.NumberPicker;
import android.widget.SeekBar;
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

import java.util.ArrayList;

public class AddGeofenceActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, ResultCallback<Status>, OnMapReadyCallback,
        GoogleMap.OnCameraIdleListener {

    private static final String TAG = AddGeofenceActivity.class.getSimpleName();
    private static final long GEOFENCE_DURATION_MILLISEC = 43200000; //12 hours in millisec


    protected GoogleApiClient mGoogleApiClient;
    private GeofencingClient mGeofencingClient;
    private PendingIntent mGeofencePendingIntent;
    private GoogleMap mMap;
    private LatLng geoFenceLatLng;

    private ArrayList<String> mGeofenceIdList;
    private ArrayList<Geofence> mGeofenceList;

    //geofence stuff
    private float mRadius;
    private long mDuration;
    private EditText mNameText;

    final String[] RADIUS_ENTRIES = {"5", "10", "25", "50", "100", "500", "1000"};
    final String[] DURATION_ENTRIES = {"1", "2", "3", "5", "10", "12", "24", "never"};

    //TODO: change duration to spinner
    private NumberPicker mRadiusPicker, mDurationPicker;
    private CheckBox mBoxEnter, mBoxExit;


    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_geofence);

        //Add map fragment
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        mGeofenceIdList = new ArrayList<>();


        mGeofenceList = new ArrayList<Geofence>();

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

        //default values
        mRadius = 50;
        //(-1 is never)
        mDuration = -1;

        //radius number picker
        mRadiusPicker = findViewById(R.id.np_raduis);
        mRadiusPicker.setMinValue(0);
        mRadiusPicker.setMaxValue(RADIUS_ENTRIES.length - 1);
        mRadiusPicker.setDisplayedValues(RADIUS_ENTRIES);
        //if this is true, then it will keep spinning, false makes it stop at each end
        mRadiusPicker.setWrapSelectorWheel(true);
        mRadiusPicker.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                mRadius = Float.parseFloat(RADIUS_ENTRIES[newVal]);
//                drawCircle();
            }
        });

        //duration number picker
        mDurationPicker = findViewById(R.id.np_duration);
        mDurationPicker.setMinValue(0);
        mDurationPicker.setMaxValue(DURATION_ENTRIES.length - 1);
        mDurationPicker.setDisplayedValues(DURATION_ENTRIES);
        mDurationPicker.setWrapSelectorWheel(true);
        mDurationPicker.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                if (newVal == 7) {
                    mDuration = -1;
                } else {
                    mDuration = Long.parseLong(DURATION_ENTRIES[newVal]);
                }

            }
        });

        //checkboxes

        mBoxEnter = findViewById(R.id.check_enter);
        mBoxExit = findViewById(R.id.check_exit);

        SeekBar seekBar = findViewById(R.id.seek_bar);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (progress < 1) {
                    return;
                }//if
                mRadius = (float) progress * 10;
                //draw circle
                drawCircle(geoFenceLatLng, mRadius);
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
                LatLng swCorner = MapViewGeofenceActivity.swCorner(geoFenceLatLng, mRadius);
                LatLng neCorner = MapViewGeofenceActivity.neCorner(geoFenceLatLng, mRadius);
                if (!visibleBound.contains(swCorner) || !visibleBound.contains(neCorner)) {
                    visibleBound.including(swCorner);
                    visibleBound.including(neCorner);
                    mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(visibleBound, 200));
                }

            }//onStopTrackingTouch
        });//SeekBarChangeListener


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
        Fence fence = new Fence(name, geoFenceLatLng.latitude, geoFenceLatLng.longitude,
                mRadius, mDuration, type);

        ArrayList<Fence> fenceArrayList = new ArrayList<>();
        fenceArrayList.add(fence);
        MainActivity.saveArrayList(this, fenceArrayList);

        addGeofence(fence);
    }//addFence

    //TODO: move all of this in the Fence class. just call getBuilder
    private void addGeofence(Fence fence) {
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
                        .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER);

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
                            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER);

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

    /**
     * Helper method for adding geoFence
     */
    private void addGeofenceButtonHandler() {
        Log.v(TAG, "addGeofenceButtonHandler called");
        //check to make sure the client is connected
        if (!mGoogleApiClient.isConnected()) {
            Toast.makeText(this, getString(R.string.not_connected), Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            //build geofence
//            buildGeofenceFromHash();
            mGeofencingClient.addGeofences(getGeofencingRequest(), getGeofencePendingIntent())
                    .addOnSuccessListener(this, new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            //geofence added
                            Toast.makeText(getApplicationContext(), "Geofence added", Toast.LENGTH_SHORT).show();
                            Log.v(TAG, "Geofence added");
                        }
                    })
                    .addOnFailureListener(this, new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            //failed
                            Toast.makeText(getApplicationContext(), "Error adding geofence", Toast.LENGTH_SHORT).show();
                            Log.v(TAG, "Geofence adding failed", e);
                        }
                    });
        } catch (SecurityException e) {
            Log.e(TAG, "Security exception", e);
        }

    }//addGeofenceButtonHandler

    //helper method for creating geofence
    private void buildGeofence() {
        Log.v(TAG, "buildGeofence called");
        //get the radius from the picker
        mRadius = Float.parseFloat(RADIUS_ENTRIES[mRadiusPicker.getValue()]);

        //Create geofence objects
        mGeofenceList.add(new Geofence.Builder()
                //set ID
                .setRequestId(mGeofenceIdList.get(0))
                //set region and radius
                .setCircularRegion(49.235675, -123.057118, mRadius) //ASCS
//                .setCircularRegion(49.235675, -123.057118, Constants.RADIUS_IN_METERS) //ASCS
//                .setCircularRegion(37.421997, -122.084007, RADIUS_IN_METERS) //Google (emulator)
                //set expiary
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
//                .setExpirationDuration(GEOFENCE_DURATION_MILLISEC)
                //set transition type
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER |
                        Geofence.GEOFENCE_TRANSITION_EXIT)
                .build());

    }//buildGeofence

    //helper method for building the geofence request
    private GeofencingRequest getGeofencingRequest() {
        Log.v(TAG, "getGeofencingRequest called");

        //create builder
        GeofencingRequest.Builder builder = new GeofencingRequest.Builder();
        //The INITIAL_TRIGGER_ENTER means that geofencing service should trigger a
        // GEOFENCE_TRANSITION_ENTER notification when the geodence is added and if the device
        //is already in that geofence
        builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER);
        //add the geofences to be monitored (our list that we made)
        builder.addGeofences(mGeofenceList);
        return builder.build();
    }//getGeofencingRequest

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


}//Activity

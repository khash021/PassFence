package tech.khash.passfence;

import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
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

//TODO: FUTURE
//TODO: add suggestion to the search


//TODO: finish the activity on the results callback

//TODO: add vibrate option here and use shared pref

//TODO: add the name of the fence to the toast for added

//TODO: minimize Toasts

//TODO: change the toast for updated geofence to "updated" not "added"


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
    private TextView mRadiusText;
    private Button mButtonAdd;

    private CheckBox mBoxEnter, mBoxExit;

    private SeekBar seekBar;

    private Spinner mDurationSpinner;

    private String editIntentExtra;

    private boolean editMode;

    private boolean unsavedChanges = false;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_geofence);
        Log.v(TAG, "onCreate Called");

        //check and ask for location permission
        if (!MainActivity.checkLocationPermission(this)) {
            askLocationPermission();
        }

        //check to see whether this is add or update
        if (getIntent().hasExtra(MainActivity.FENCE_EDIT_EXTRA_INTENT)) {
            editIntentExtra = getIntent().getStringExtra(MainActivity.FENCE_EDIT_EXTRA_INTENT);
            editMode = true;
        }

        //Add map fragment
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);


        // Kick off the request to build GoogleApiClient.
        buildGoogleApiClient();

        //create an instance of the Geofencing client to access the location APIs
        mGeofencingClient = LocationServices.getGeofencingClient(this);

        //buttons
        mButtonAdd = findViewById(R.id.button_add_geo);
        mButtonAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Decide whether this is edit or add new
                if (!editMode) {
                    //create fence object
                    Fence fence = createFenceObject();
                    //add geofence
                    addGeofence(fence);
                } else {
                    updatedFence();
                }
            }
        });//addGeofenceButton

        mNameText = findViewById(R.id.text_name);
        mNameText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //activate unsaved boolean
                unsavedChanges = true;
            }
        });
        //add this listener so when the user presses enter, this gets called and we can hide the keyboard
        mNameText.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                //check for the enter key
                if (keyCode == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_UP) {
                    //enter key has been pressed and we hide the keyboard
                    hideKeyboard();
                    //return true to let it know we handled the event
                    return true;
                }
                return false;
            }
        });

        mDurationSpinner = findViewById(R.id.spinner_duration);
        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<CharSequence> spinnerAdapter = ArrayAdapter.createFromResource(this,
                R.array.spinner_duration_array, android.R.layout.simple_spinner_dropdown_item);
        // Specify the layout to use when the list of choices appears
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the spinner
        mDurationSpinner.setAdapter(spinnerAdapter);
        //initialized the array at 12 hours and set the radius to that
        mDurationSpinner.setSelection(7);
        mDuration = 12;
        //set listener
        mDurationSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
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
        mBoxEnter.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                //activate unsaved boolean
                unsavedChanges = true;
            }
        });
        mBoxExit = findViewById(R.id.check_exit);
        mBoxExit.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                //activate unsaved boolean
                unsavedChanges = true;
            }
        });

        seekBar = findViewById(R.id.seek_bar);
        //default at 50 meters
        seekBar.setProgress(5);
        mRadius = 50;
        mRadiusText = findViewById(R.id.text_radius);
        mRadiusText.setText("50");
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
                mRadiusText.setText(radius);

                //activate unsaved boolean
                unsavedChanges = true;
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
                mRadiusText.setText(radiusString);

                //add marker to center
                if (geoFenceLatLng == null) {
                    Log.v(TAG, "location null");
                    return;
                }

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
    }//onCreate


    @Override
    public void onMapReady(GoogleMap map) {
        Log.v(TAG, "onMapReady called");
        mMap = map;

        mMap.setOnCameraIdleListener(this);

        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                //activate unsaved boolean
                unsavedChanges = true;
            }
        });

        //disable map toolbar
        UiSettings mUiSettings = mMap.getUiSettings();
        mUiSettings.setMapToolbarEnabled(false);

        if (MainActivity.checkLocationPermission(this)) {
            mMap.setMyLocationEnabled(true);
        }

        //here if Intent is not empty, setup edit page
        if (editIntentExtra != null) {
            String fenceId = getIntent().getStringExtra(MainActivity.FENCE_EDIT_EXTRA_INTENT);
            setupEditMode(fenceId);
        }
    }//onMapReady

    @Override
    public void onCameraIdle() {
        geoFenceLatLng = mMap.getCameraPosition().target;
    }//onCameraIdle

    private void drawCircle(LatLng center, float radius) {
        //draw circle on map
        if (mMap == null || center == null) {
            Log.v(TAG, "Error drawing circle");
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
    private Fence createFenceObject() {
        //here we extract the Fence data
        String name = mNameText.getText().toString().trim();
        int type;
        boolean enter = mBoxEnter.isChecked();
        boolean exit = mBoxExit.isChecked();

        //check to make sure we have name
        if (name.length() < 1) {
            Toast.makeText(this, getString(R.string.name_required_toast), Toast.LENGTH_SHORT).show();
            return null;
        }

        //check to makes ure at least one checkbox is checked
        if (!enter && !exit) {
            Toast.makeText(this, getString(R.string.criteria_required_toast), Toast.LENGTH_SHORT).show();
            return null;
        }

        //check against duplicate ids
        ArrayList<Fence> savedFenceArrayList = MainActivity.loadArrayList(this);
        //check to make sure the array is not null
        if (savedFenceArrayList != null && !editMode) {
            ArrayList<String> idArrayList = new ArrayList<>();
            for (Fence fence : savedFenceArrayList) {
                idArrayList.add(fence.getId().toLowerCase());
            }//for
            if (idArrayList.contains(name.toLowerCase())) {
                Toast.makeText(this, getString(R.string.duplicate_name_toast), Toast.LENGTH_SHORT).show();
                return null;
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

        return fence;
    }//createFenceObject

    private void addGeofence(final Fence fence) {
        //check for permission first
        if (!MainActivity.checkLocationPermission(this)) {
            askLocationPermission();
        }

        //get the Geofencing request object
        GeofencingRequest geofencingRequest = fence.getGeofencingRequestObject();

        //register geofence
        mGeofencingClient.addGeofences(geofencingRequest, getGeofencePendingIntent())
                .addOnSuccessListener(this, new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        //add the fence to the array list of fences
                        ArrayList<Fence> fenceArrayList = new ArrayList<>();
                        fenceArrayList.add(fence);
                        MainActivity.saveArrayList(getApplicationContext(), fenceArrayList);
                        //show a toast
                        Toast.makeText(getApplicationContext(), getString(R.string.geofence_added_toast), Toast.LENGTH_SHORT).show();
                        Log.v(TAG, "Geofence added");
                        //finish activity
                        finish();
                    }
                })
                .addOnFailureListener(this, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(getApplicationContext(), getString(R.string.error_geofence_add), Toast.LENGTH_SHORT).show();
                        Log.v(TAG, "Geofence adding failed", e);
                    }
                });
    }//addGeofence

    //helper method for updating the geofence
    private void updatedFence() {
        //first retrieve the fence object using id
        String fenceId = getIntent().getStringExtra(MainActivity.FENCE_EDIT_EXTRA_INTENT);
        //retrieve the array list of Fences
        ArrayList<Fence> fenceArrayList = MainActivity.loadArrayList(this);

        //find the Fence object we want to edit
        Fence fence = null;
        int fenceIndexInArray = -1;
        for (Fence f : fenceArrayList) {
            if (f.getId().equalsIgnoreCase(fenceId)) {
                fence = f;
                //get the index of our Fence object
                fenceIndexInArray = fenceArrayList.indexOf(f);
                break;
            }//if
        }//for

        //check for null or -1 index
        if (fence == null | fenceIndexInArray == -1) {
            Log.wtf(TAG, "Error in locating the fence object");
            return;
        }

        //create a new fence object
        Fence updatedFence = createFenceObject();
        if (updatedFence == null) {
            Log.v(TAG, "Updated fence is null");
            return;
        }

        //replace the old Fence object with the new one in the ArrayList
        fenceArrayList.set(fenceIndexInArray, updatedFence);
        //update app's arrayList
        MainActivity.updateArrayList(this, fenceArrayList);

        //replace the old geofence
        //replace method takes a list, so create a list with the fenceId to be removed
        List<String> oldFenceIdList = new ArrayList<String>();
        oldFenceIdList.add(fenceId);

        mGeofencingClient.removeGeofences(oldFenceIdList).addOnSuccessListener(this, new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                Log.v(TAG, "Geofence removed successfully");
            }
        })
                .addOnFailureListener(this, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.v(TAG, "Geofence removal error");
                    }
                });


        GeofencingRequest updatedGeofencingRequest = updatedFence.getGeofencingRequestObject();
        //register geofence
        mGeofencingClient.addGeofences(updatedGeofencingRequest, getGeofencePendingIntent())
                .addOnSuccessListener(this, new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        //show a toast
                        Toast.makeText(getApplicationContext(), getString(R.string.geofence_added_toast), Toast.LENGTH_SHORT).show();
                        Log.v(TAG, "Geofence added");
                        //finish activity
                        finish();
                    }
                })
                .addOnFailureListener(this, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(getApplicationContext(), getString(R.string.error_geofence_add), Toast.LENGTH_SHORT).show();
                        Log.v(TAG, "Geofence adding failed", e);
                    }
                });
    }//updatedFence

    //helper method for deleting the geofence
    private void deleteGeofence(String id) {
        //retrieve the array list of Fences
        ArrayList<Fence> fenceArrayList = MainActivity.loadArrayList(this);

        //find the Fence object we want to edit
        Fence fence = null;
        int fenceIndexInArray = -1;
        for (Fence f : fenceArrayList) {
            if (f.getId().equalsIgnoreCase(id)) {
                fence = f;
                //get the index of our Fence object
                fenceIndexInArray = fenceArrayList.indexOf(f);
                break;
            }//if
        }//for

        //check for null or -1 index
        if (fence == null | fenceIndexInArray == -1) {
            Log.wtf(TAG, "Error in locating the fence object");
            return;
        }

        //remove geofence
        //remove method takes a list, so create a list with the fenceId to be removed
        List<String> oldFenceIdList = new ArrayList<String>();
        oldFenceIdList.add(id);

        mGeofencingClient.removeGeofences(oldFenceIdList).addOnSuccessListener(this, new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                Log.v(TAG, "Geofence removed successfully");
                Toast.makeText(getApplicationContext(), getString(R.string.geofence_removed), Toast.LENGTH_SHORT).show();
            }
        })
                .addOnFailureListener(this, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.v(TAG, "Geofence removal error");
                        Toast.makeText(getApplicationContext(), getString(R.string.error_remove_geofence), Toast.LENGTH_SHORT).show();
                    }
                });

        //remove the geofence from the main arraylist (return true if it was removed)
        boolean removeSuccess = fenceArrayList.remove(fence);

        //update the main arraylist
        if (removeSuccess) {
            //update app's arrayList
            MainActivity.updateArrayList(this, fenceArrayList);
        }
    }//deleteGeofence

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
            Toast.makeText(this, getString(R.string.geofence_added_toast), Toast.LENGTH_SHORT
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
        if (!MainActivity.checkLocationPermission(this)) {
            return;
        }
        Location location = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);

        //only do this, if this is not the edit, otherwise don't move camera
        if (location != null && editIntentExtra == null) {
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
            searchView.setQueryHint(getString(R.string.enter_address_hint));
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

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        //click on the back button
        if (item.getItemId() == android.R.id.home) {
            if (unsavedChanges) {
                showUnsavedChangesDialog();
                //We return true, so the normal behavior doesn't continue (i.e. return to home screen)
                return true;
            }
        }
        return super.onOptionsItemSelected(item);
    }//onOptionsItemSelected

    @Override
    public void onBackPressed() {
        if (!unsavedChanges) {
            super.onBackPressed();
        } else {
            showUnsavedChangesDialog();
        }
    }//onBackPressed

    private void searchAddress(String query) {
        //check for geocoder availability
        if (!Geocoder.isPresent()) {
            Log.v(TAG, "Geocoder not available");
            Toast.makeText(this, getString(R.string.geocoder_not_available_toast), Toast.LENGTH_SHORT).show();
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
                Toast.makeText(this, getString(R.string.no_results_found_toast), Toast.LENGTH_SHORT).show();
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

    private void setupEditMode(String fenceId) {
        //change the text of the button
        mButtonAdd.setText(getString(R.string.update));

        //retrieve the array list of Fences
        ArrayList<Fence> fenceArrayList = MainActivity.loadArrayList(this);

        //find the Fence object we want to edit
        Fence fence = null;
        for (Fence f : fenceArrayList) {
            if (f.getId().equalsIgnoreCase(fenceId)) {
                fence = f;
                break;
            }//if
        }//for

        //check for null fence (no search results, should never happen)
        if (fence == null) {
            Log.wtf(TAG, "Error finding fence object");
            return;
        }

        //set the name
        mNameText.setText(fence.getId());
        //get the type
        int type = fence.getType();
        switch (type) {
            case Fence.FENCE_TYPE_ENTER:
                mBoxEnter.setChecked(true);
                break;
            case Fence.FENCE_TYPE_EXIT:
                mBoxExit.setChecked(true);
                break;
            case Fence.FENCE_TYPE_ENTER_EXIT:
                mBoxExit.setChecked(true);
                mBoxEnter.setChecked(true);
                break;
        }//switch - type

        //set radius
        mRadius = fence.getRadius();
        //convert to String
        String radiusString = String.valueOf(mRadius);
        //remove all decimals
        radiusString = radiusString.substring(0, radiusString.indexOf("."));
        //set the radius
        mRadiusText.setText(radiusString);

        //set the seekbar
        //get the radius in float and divide by 10 (same thing we did in onCreate)
        float radius = fence.getRadius();
        int seekbarProgress = ((int) radius) / 10;
        seekBar.setProgress(seekbarProgress);

        //set spinner
        int spinnerIndex = fence.getDurationIndex();
        //-1 is the default which should never happen, because the values were set using the spinner
        if (spinnerIndex != -1) {
            mDurationSpinner.setSelection(spinnerIndex);
        }

        //draw circle
        drawCircle(fence.getLatLng(), fence.getRadius());

        //animate camera to the location
        if (mMap != null) {
            if (mRadius < 500) {
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(fence.getLatLng(), 15.0f));
            } else {
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(fence.getLatLng(), 14.0f));
            }
        }//null map
    }//setupEditMode

    //Helper method for showing the dialog for unsaved data
    private void showUnsavedChangesDialog() {

        //create the builder
        AlertDialog.Builder builder =new AlertDialog.Builder(this);

        //add message and button functionality
        builder.setMessage(R.string.unsaved_changes_dialog_msg)
                .setPositiveButton(R.string.discard, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //make the sdiscard boolean false and go to back pressed to follow normal hierarchical back
                        unsavedChanges = false;
                        //continue with back button
                        onBackPressed();
                    }
                })
                .setNegativeButton(R.string.keep_editing, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //close the dialog
                        dialog.dismiss();
                    }
                });

        AlertDialog dialog = builder.create();
        dialog.show();
    }//showUnsavedChangesDialog

    /**
     * Helper method for showing a message to the user informing them about the benefits of turning on their
     * location. and also can direct them to the location settings of their phone
     */
    private void askLocationPermission() {
        //Create a dialog to inform the user about this feature's permission
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        //Chain together various setter methods to set the dialogConfirmation characteristics
        builder.setMessage(R.string.permission_required_text_dialog).setTitle(R.string.permission_required_title_dialog);
        // Add the buttons. We can call helper methods from inside the onClick if we need to
        builder.setPositiveButton(R.string.permission_required_yes_dialog, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                //figure out if they have checked the check box for never ask again.
                if (Build.VERSION.SDK_INT >= 23 && !shouldShowRequestPermissionRationale(android.Manifest.permission.ACCESS_FINE_LOCATION)) {
                    //This is the case when the user checked the box, so we send them to the settings
                    openPermissionSettings();
                } else {
                    //This is the case that either is is older than DK 23, or they have not checked the box, so use the normal one
                    ActivityCompat.requestPermissions(AddGeofenceActivity.this,
                            new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                            MainActivity.LOCATION_PERMISSION_REQUEST_CODE);
                }
            }
        });
        builder.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        //build and show dialog
        builder.create().show();
    }//askLocationPermission

    /**
     * Helper method for directing the user to the app's setting in their phone to turn on the permission
     */
    private void openPermissionSettings() {
        Intent intent = new Intent();
        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", getPackageName(), null);
        intent.setData(uri);
        startActivity(intent);
    }//openPermissionSettings

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

    /**
     * Helper method for checking if the map is ready, used by other methods before performing
     * their tasks
     *
     * @return true if the map is ready, false otherwise
     */
    private boolean checkMapReady() {
        if (mMap == null) {
            return false;
        }//if
        return true;
    }//checkMapReady

}//Activity

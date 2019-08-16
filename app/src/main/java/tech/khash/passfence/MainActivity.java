package tech.khash.passfence;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.maps.android.SphericalUtil;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    //TODO: add modify geofence both from list and map view

    //TODO: move these constants

    //TODO: Edit and finalize Settings and its xml files

    //used for saving/loading arraylist to sharedpref
    public final static String MY_PREF_ARRAY_KEY = "key";
    public final static String MY_PREF_NAME = "myPref";

    public final static String FENCE_EDIT_EXTRA_INTENT = "fence-edit-extra-intent";

    private final static String TAG = MainActivity.class.getSimpleName();
    public final static int LOCATION_PERMISSION_REQUEST_CODE = 1;

    private AdView mAdView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //check for location permission and ask for it
        if (!checkLocationPermission(this)) {

            askLocationPermission(this, this);
        } //permission


        ((Button) findViewById(R.id.button_geofence_activity)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(MainActivity.this, AddGeofenceActivity.class);
                startActivity(i);
            }
        });

        ((Button) findViewById(R.id.button_map_view_geofence)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(MainActivity.this, MapViewGeofenceActivity.class);
                startActivity(i);
            }
        });

        ((Button) findViewById(R.id.button_recreate)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                recreate();
            }
        });

        ((Button) findViewById(R.id.button_clear_fence)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                removeAllFences(getApplicationContext());
            }
        });

        ((Button) findViewById(R.id.button_address_map)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(MainActivity.this, SearchAddressActivity.class);
                startActivity(i);
            }
        });

        ((Button) findViewById(R.id.button_list_view_geofence)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(MainActivity.this, ListViewGeofenceActivity.class);
                startActivity(i);
            }
        });

        TextView textView = findViewById(R.id.text_pref);


        addBannerAd();
    }//onCreate

    /**
     * Inflates the menu, and adds items to the action bar if it is present.
     *
     * @param menu Menu to inflate.
     * @return Returns true if the menu inflated.
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }//onCreateOptionsMenu

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        //TODO: make the functionality of these
        switch (item.getItemId()) {
            case R.id.action_settings:


                break;
            case R.id.action_rate:

                return true;
            case R.id.action_help:

                return true;
            case R.id.action_contact:

                return true;
            case R.id.action_privacy_policy:

                return true;
            case R.id.action_about:

                return true;
        }//switch
        return super.onOptionsItemSelected(item);
    }//onOptionsItemSelected

    public static ArrayList<Fence> loadArrayList(Context context) {

        //create Gson object
        Gson gson = new Gson();
        //get reference to the shared pref
        SharedPreferences sharedPreferences = context.getSharedPreferences(MY_PREF_NAME, Context.MODE_PRIVATE);
        //get the string from the preference (this will be empty string if there is no data in there
        //yet). as a result the output array list will be null, so we need to check for this in the
        //save array list when we pull the old data
        String response = sharedPreferences.getString(MY_PREF_ARRAY_KEY, "");
        //convert the json string back to Fence Array list and return it
        ArrayList<Fence> outputArrayList = gson.fromJson(response,
                new TypeToken<List<Fence>>() {
                }.getType());

        return outputArrayList;

    }//loadArrayList

    /**
     * Helper method for saving the new arrayList to the the old one, it adds all of them.
     *
     * @param context        : context
     * @param inputArrayList : new arraylist to be added on top of the old one
     */
    public static void saveArrayList(Context context, ArrayList<Fence> inputArrayList) {

        //get reference to shared pref
        SharedPreferences sharedPreferences = context.getSharedPreferences(MY_PREF_NAME, Context.MODE_PRIVATE);

        //create Gson object
        Gson gson = new Gson();

        //load the previous data, and add the new list to it
        ArrayList<Fence> fullList = loadArrayList(context);

        //if there is nothing in there, this will be null, so we instantiate it
        if (fullList == null) {
            fullList = new ArrayList<>();
        }//if

        //add the new data to it
        fullList.addAll(inputArrayList);

        //convert arraylist
        String json = gson.toJson(fullList);

        //get the shared preference editor
        SharedPreferences.Editor editor = sharedPreferences.edit();
        //since we have added the old data to the new list, we can now delete the last entry
        editor.remove(MY_PREF_ARRAY_KEY).apply();
        //add the new updated list
        editor.putString(MY_PREF_ARRAY_KEY, json);
        editor.apply();
    }//saveArrayList

    /**
     * This updated the arraylist; i.e. it deletes the old one and replaces with the updated one
     *
     * @param context          : context
     * @param updatedArrayList : updated arraylist
     */
    public static void updateArrayList(Context context, ArrayList<Fence> updatedArrayList) {
        //get reference to shared pref
        SharedPreferences sharedPreferences = context.getSharedPreferences(MY_PREF_NAME, Context.MODE_PRIVATE);

        //create Gson object
        Gson gson = new Gson();

        //convert arraylist
        String json = gson.toJson(updatedArrayList);

        //get the shared preference editor
        SharedPreferences.Editor editor = sharedPreferences.edit();
        //since we have added the old data to the new list, we can now delete the last entry
        editor.remove(MY_PREF_ARRAY_KEY).apply();
        //add the new updated list
        editor.putString(MY_PREF_ARRAY_KEY, json);
        editor.apply();
    }//updateArrayList

    public static void eraseAllArrays(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(MY_PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(MY_PREF_ARRAY_KEY).apply();
        Toast.makeText(context, "Data erased", Toast.LENGTH_SHORT).show();
    }//eraseAllArray

    public static void removeAllFences(Context context) {
        ArrayList<Fence> fenceArrayList = loadArrayList(context);
        if (fenceArrayList == null) {
            Toast.makeText(context, "No Geofence", Toast.LENGTH_SHORT).show();
            Log.v(TAG, "No geofence to remove");
            return;
        }
        ArrayList<String> idArrayList = new ArrayList<>();
        for (Fence fence : fenceArrayList) {
            idArrayList.add(fence.getId());
        }

        GeofencingClient geofencingClient = LocationServices.getGeofencingClient(context);
        geofencingClient.removeGeofences(idArrayList);

        //also clean the internal arrayList
        eraseAllArrays(context);

        Toast.makeText(context, "Geofences removed", Toast.LENGTH_SHORT).show();
        Log.v(TAG, "Geofences removed");
    }//removeAllFences

    //helper methods to get the bounds of the circle (radius in meters)
    public static LatLng swCorner(LatLng center, float radius) {
        double distanceFromCenterToCorner = ((double) radius) * Math.sqrt(2.0);
        LatLng southwestCorner =
                SphericalUtil.computeOffset(center, distanceFromCenterToCorner, 225.0);
        return southwestCorner;
    }

    public static LatLng neCorner(LatLng center, float radius) {
        double distanceFromCenterToCorner = ((double) radius) * Math.sqrt(2.0);
        LatLng northeastCorner =
                SphericalUtil.computeOffset(center, distanceFromCenterToCorner, 45.0);
        return northeastCorner;
    }


    public static boolean checkLocationPermission(Context context) {
        //check for location permission and ask for it
        return ContextCompat.checkSelfPermission(context,
                android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }//checkLocationPermission

    //TODO: check this method and test again
    /**
     * Helper method for showing a message to the user informing them about the benefits of turning on their
     * location. and also can direct them to the location settings of their phone
     */
    public static void askLocationPermission(final Context context, final Activity activity) {
        //Create a dialog to inform the user about this feature's permission
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        //Chain together various setter methods to set the dialogConfirmation characteristics
        builder.setMessage(R.string.permission_required_text_dialog).setTitle(R.string.permission_required_title_dialog);
        // Add the buttons. We can call helper methods from inside the onClick if we need to
        builder.setPositiveButton(R.string.permission_required_yes_dialog, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                //first check to see if the user has denied permission before
                if (ContextCompat.checkSelfPermission(context,
                        android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_DENIED) {
                    //here we check to see if they have selected "never ask again". If that is the case, then
                    // shouldShowRequestPermissionRationale will return false. If that is false, and
                    //the build version is higher than 23 (that feature is only available to >= 23
                    //then send them to the
                    if (Build.VERSION.SDK_INT >= 23 && !(activity.shouldShowRequestPermissionRationale(android.Manifest.permission.ACCESS_FINE_LOCATION))) {
                        //This is the case when the user checked the box, so we send them to the settings
                        openPermissionSettings(activity);
                    } else {
                        ActivityCompat.requestPermissions(activity,
                                new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                                LOCATION_PERMISSION_REQUEST_CODE);
                    }
                } else {
                    //this is the case that the user has never denied permission, so we ask for it
                    ActivityCompat.requestPermissions(activity,
                            new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                            LOCATION_PERMISSION_REQUEST_CODE);
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
    private static void openPermissionSettings(Activity activity) {
        Intent intent = new Intent();
        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", activity.getPackageName(), null);
        intent.setData(uri);
        activity.startActivity(intent);
    }//openPermissionSettings

    private void addBannerAd() {
        //find the ad view
        mAdView = findViewById(R.id.ad_view);
        //attach the listener first before loading it
        //this is for testing of knowing what is going on at each step, so disable now
//        adView.setAdListener(new ToastAdListener(this));
        //create an ad request object using the builder
        AdRequest adRequest = new AdRequest.Builder()
                .build();
        //load the ad request into the ad view
        mAdView.loadAd(adRequest);
    }//addBannerAd


    @Override
    protected void onResume() {
        super.onResume();
        //resume adview
        mAdView.resume();
    }//onResume

    @Override
    protected void onPause() {
        super.onPause();
        //pause adview (Pauses any extra processing associated with this ad view.)
        mAdView.pause();
    }//onPause

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Destroy the AdView.
        mAdView.destroy();
    }//onDestroy

}//MainActivity

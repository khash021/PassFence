package tech.khash.passfence;

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
import android.util.Log;
import android.view.View;
import android.widget.Button;
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
    //TODO: fix the
    //TODO: move these constants
    //TODO: remove string literals
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

        //check for location permission and ask for it
        if (!checkLocationPermission(this)) {

            //TODO: show a dialog explaining the permission and then ask for it

            // No explanation needed; request the permission
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        } //permission


        ((Button) findViewById(R.id.button_geofence_activity)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(MainActivity.this, AddGeofenceActivity.class);
                startActivity(i);
            }
        });

        ((Button) findViewById(R.id.button_save_arraylist)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(MainActivity.this, SaveArraylistActivity.class);
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

        ((Button) findViewById(R.id.button_clear_list)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                eraseAllArrays(getApplicationContext());
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

        addBannerAd();
    }//onCreate

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
                new TypeToken<List<Fence>>(){}.getType());

        return outputArrayList;

    }//loadArrayList

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

    public static void eraseAllArrays (Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(MY_PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(MY_PREF_ARRAY_KEY).apply();
        Toast.makeText(context, "Data erased", Toast.LENGTH_SHORT).show();
    }//eraseAllArray

    public static void removeAllFences(Context context){
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
    public static LatLng swCorner(LatLng center, float radius ) {
        double distanceFromCenterToCorner = ((double) radius) * Math.sqrt(2.0);
        LatLng southwestCorner =
                SphericalUtil.computeOffset(center, distanceFromCenterToCorner, 225.0);
        return southwestCorner;
    }

    public static LatLng neCorner (LatLng center, float radius ) {
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
                    ActivityCompat.requestPermissions(MainActivity.this,
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
    private void openPermissionSettings() {
        Intent intent = new Intent();
        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", getPackageName(), null);
        intent.setData(uri);
        startActivity(intent);
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

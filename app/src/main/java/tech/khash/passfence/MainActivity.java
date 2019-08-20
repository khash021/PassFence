package tech.khash.passfence;

import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.app.ShareCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.maps.android.SphericalUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class MainActivity extends AppCompatActivity implements FenceListAdapter.ListItemLongClickListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        NavigationView.OnNavigationItemSelectedListener {

    //TODO: make needsUpdate in preferences, so if the user didnt add anything or edit, then it wont do it

    //TODO: remove unused methods all across (Analyze > inspect code)

    //TODO: add recreate boolean for all the stuff that comes back to activity

    //TODO: check for any variable/method that can be deleted or the scope

    //TODO: buttons theme (color, whatever, they look ugly as shit right now

    private final static String TAG = MainActivity.class.getSimpleName();

    public final static String FENCE_EDIT_EXTRA_INTENT = "fence-edit-extra-intent";
    public final static int LOCATION_PERMISSION_REQUEST_CODE = 1;

    private AdView mAdView;

    //SharedPreference constants
    public final static String ARRAY_PREF_KEY = "array_pref_key";
    public static final String RINGTONE_PREF_KEY = "notification_ringtone_key";
    public static final String VIBRATE_PREF_KEY = "notification_vibrate_key";
    public static final String LED_PREF_KEY = "notification_led_key";
    public static final String PRIORITY_PREF_KEY = "notification_priority_key";
    public static final String COLOR_PICKER_PREF_KEY = "notification_color_picker_key";

    //Notification channels for Android 8 and higher
    public final static String CHANNEL_ID = "Notification Settings";

    //For geofences
    protected GoogleApiClient mGoogleApiClient;
    private GeofencingClient mGeofencingClient;

    //mAdapter
    private ArrayList<Fence> mFenceArrayList;
    private FenceListAdapter mAdapter;
    private RecyclerView mRecyclerView;

    //drawer layout used for navigation drawer
    private DrawerLayout mDrawerLayout;

    //for tracking changes that needs the list to be updated/recreated
    private boolean needsUpdate = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.v(TAG, "onCreate Called");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //Set the tool bar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        //change the title
        getSupportActionBar().setTitle(getString(R.string.app_name));
        //Set the menu icon
        ActionBar actionbar = getSupportActionBar();
        //Enable app bar home button
        actionbar.setDisplayHomeAsUpEnabled(true);
        //Set the icon
        actionbar.setHomeAsUpIndicator(R.drawable.ic_menu_white);
        //In order for the button to open the menu we need to override onOption Item selected (below onCreate)

        //get the drawer layout and navigation drawer
        mDrawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        if (navigationView != null) {
            navigationView.setNavigationItemSelectedListener(this);
        }

        //check for location permission and ask for it
        if (!checkLocationPermission(this)) {
            askLocationPermission(this, this);
        } //permission

        //create notification channel
        createNotificationChannel();

        //add the add banner
        addBannerAd();

        //view containing the empty view
        LinearLayout emptyView = findViewById(R.id.empty_view);

        //get the arrayList, and set the visibility of empty view accordingly
        mFenceArrayList = loadArrayList(this);
        if (mFenceArrayList == null || mFenceArrayList.size() < 1) {
            emptyView.setVisibility(View.VISIBLE);
        } else {
            emptyView.setVisibility(View.GONE);
        }

        // Get a handle to the RecyclerView.
        mRecyclerView = findViewById(R.id.recycler_view);
        // Create an mAdapter and supply the data to be displayed.
        mAdapter = new FenceListAdapter(this, mFenceArrayList, this);
        // Connect the mAdapter with the RecyclerView.
        mRecyclerView.setAdapter(mAdapter);
        // Give the RecyclerView a default layout manager.
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        //Add divider between items using the DividerItemDecoration
        DividerItemDecoration decoration = new DividerItemDecoration(mRecyclerView.getContext(),
                DividerItemDecoration.VERTICAL);
        mRecyclerView.addItemDecoration(decoration);

        // Kick off the request to build GoogleApiClient.
        buildGoogleApiClient();

        //create an instance of the Geofencing client to access the location APIs
        mGeofencingClient = LocationServices.getGeofencingClient(this);

        //find the fab and set it up
        FloatingActionButton fabAddContent = findViewById(R.id.fab_add_content);
        fabAddContent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent addIntent = new Intent(MainActivity.this, AddGeofenceActivity.class);
                //needs update
                needsUpdate = true;
                startActivity(addIntent);
            }
        });
    }//onCreate

    @Override
    protected void onResume() {
        Log.v(TAG, "onResume Called");
        super.onResume();
        //resume adview
        mAdView.resume();
    }//onResume

    @Override
    protected void onStart() {
        Log.v(TAG, "onStart Called");
        //check for update boolean
        if (needsUpdate) {
            recreate();
        }
        super.onStart();
        if (!mGoogleApiClient.isConnecting() || !mGoogleApiClient.isConnected()) {
            mGoogleApiClient.connect();
        }
    }//onStart

    @Override
    protected void onPause() {
        Log.v(TAG, "onPause Called");
        super.onPause();
        //pause adview (Pauses any extra processing associated with this ad view.)
        mAdView.pause();
        //if the navigation drawer is open, we close it so when the user is directed back, it doesn't stay open
        if (mDrawerLayout != null) {
            if (mDrawerLayout.isDrawerOpen(GravityCompat.START)) {
                mDrawerLayout.closeDrawer(GravityCompat.START);
            }
        }
    }//onPause

    @Override
    protected void onStop() {
        Log.v(TAG, "onStop Called");
        super.onStop();
        if (mGoogleApiClient.isConnecting() || mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
    }//onStop

    @Override
    protected void onDestroy() {
        Log.v(TAG, "onDestroy Called");
        super.onDestroy();
        // Destroy the AdView.
        mAdView.destroy();
    }//onDestroy

    /**
     * Handles the Back button: closes the nav drawer.
     */
    @Override
    public void onBackPressed() {
        //If the user clicks the systems back button and if the navigation drawer is open, it closes it
        if (mDrawerLayout != null) {
            if (mDrawerLayout.isDrawerOpen(GravityCompat.START)) {
                mDrawerLayout.closeDrawer(GravityCompat.START);
            } else {
                super.onBackPressed();
            }
        }
    }//onBackPressed

    protected synchronized void buildGoogleApiClient() {
        Log.v(TAG, "buildGoogleApiClient called");

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }//buildGoogleApiClient

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
        switch (item.getItemId()) {
            case android.R.id.home:
                //open navigation drawer
                mDrawerLayout.openDrawer(GravityCompat.START);
                return true;
            case R.id.action_refresh:
                recreate();
                return true;
            case R.id.action_sort_name_ascending:
                //check for empty list and show a Toast
                if (mFenceArrayList == null || mFenceArrayList.size() < 1) {
                    displayToast(App.getContext().getString(R.string.list_empty_toast));
                    return true;
                }
                sortNameAscending();
                return true;
            case R.id.action_sort_name_descending:
                //check for empty list and show a Toast
                if (mFenceArrayList == null || mFenceArrayList.size() < 1) {
                    displayToast(App.getContext().getString(R.string.list_empty_toast));
                    return true;
                }
                sortNameDescending();
                return true;
            case R.id.action_sort_expiry_ascending:
                //check for empty list and show a Toast
                if (mFenceArrayList == null || mFenceArrayList.size() < 1) {
                    displayToast(App.getContext().getString(R.string.list_empty_toast));
                    return true;
                }
                sortExpiryAscending();
                return true;
            case R.id.action_sort_expiry_descending:
                //check for empty list and show a Toast
                if (mFenceArrayList == null || mFenceArrayList.size() < 1) {
                    displayToast(App.getContext().getString(R.string.list_empty_toast));
                    return true;
                }
                sortExpiryDescending();
                return true;
            case R.id.action_delete_all:
                //check for empty list and show a Toast
                if (mFenceArrayList == null || mFenceArrayList.size() < 1) {
                    displayToast(App.getContext().getString(R.string.no_geofence_show_toast));
                    return true;
                }
                deleteAllList();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }//switch
    }//onOptionsItemSelected

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.nav_add_location:
                Intent addIntent = new Intent(MainActivity.this, AddGeofenceActivity.class);
                needsUpdate = true;
                startActivity(addIntent);
                return true;
            case R.id.nav_edit_location:
                //TODO:
                return true;
            case R.id.nav_map_view:
                Intent mapViewIntent = new Intent(MainActivity.this, MapViewGeofenceActivity.class);
                startActivity(mapViewIntent);
                return true;
            case R.id.nav_settings:
                Intent settingsIntent = new Intent(MainActivity.this, SettingsActivity.class);
                startActivity(settingsIntent);
                return true;
            case R.id.nav_contact:
                //send email. Use Implicit intent so the user can choose their preferred app
                //create uri for email
                String email = getString(R.string.contact_email);
                Uri emailUri = Uri.parse("mailto:" + email);
                Intent emailIntent = new Intent(Intent.ACTION_SENDTO, emailUri);
                //make sure the device can handle the intent before sending
                if (emailIntent.resolveActivity(getPackageManager()) != null) {
                    startActivity(emailIntent);
                    return true;
                }
                return false;
            case R.id.nav_rate:
                Uri uri = Uri.parse("market://details?id=" + this.getPackageName());
                Intent goToMarket = new Intent(Intent.ACTION_VIEW, uri);
                // To count with Play market backstack, After pressing back button,
                // to taken back to our application, we need to add following flags to intent.
                goToMarket.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY |
                        Intent.FLAG_ACTIVITY_NEW_DOCUMENT |
                        Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
                try {
                    startActivity(goToMarket);
                } catch (ActivityNotFoundException e) {
                    startActivity(new Intent(Intent.ACTION_VIEW,
                            Uri.parse("http://play.google.com/store/apps/details?id=" + this.getPackageName())));
                }
                return true;
            case R.id.nav_share:
                ShareCompat.IntentBuilder.from(this)
                        .setType("text/plain")
                        .setChooserTitle(R.string.share_intent_title)
                        .setSubject(getResources().getString(R.string.share_dialog_title))
                        .setText(getResources().getString(R.string.google_play_address))
                        .startChooser();
                return true;
            case R.id.nav_help:
                //TODO:
                displayToast("Help");
                return true;
            case R.id.nav_about:
                Intent aboutIntent = new Intent(MainActivity.this, AboutActivity.class);
                startActivity(aboutIntent);
                return true;
            case R.id.nav_privacy_policy:
                try {
                    Uri addressUri = Uri.parse("https://firebasestorage.googleapis.com/v0/b/irecycle-1522273491755.appspot.com/o/PrivacyPolicyPassFence.pdf?alt=media&token=bddf80a6-bdee-45fc-bef1-70214e02cb19");
                    Intent privacyIntent = new Intent(Intent.ACTION_VIEW, addressUri);
                    startActivity(privacyIntent);
                } catch (Exception e) {
                    Log.e(TAG, "Error opening Privacy Policy page", e);

                }
                return true;
            default:
                return false;
        }//switch
    }//onNavigationItemSelected

    @Override
    public void onConnected(@Nullable Bundle bundle) {
    }//onConnected

    @Override
    public void onConnectionSuspended(int i) {
        mGoogleApiClient.connect();
    }//onConnectionSuspended

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
    }//onConnectionFailed

    @Override
    public void onListItemLongClick(int clickedItemIndex) {
        //get the corresponding fence object
        Fence fence = mFenceArrayList.get(clickedItemIndex);
        //show a dialog
        showLongClickDialog(fence);
    }//onListItemLongClick

    /*------------------------------------------------------------------------------------------
                    ---------------    HELPER METHODS    ---------------
    ------------------------------------------------------------------------------------------*/

    //Returns the main ArrayList<Fence> for the app
    public static ArrayList<Fence> loadArrayList(Context context) {
        //create Gson object
        Gson gson = new Gson();
        //get reference to the shared pref
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        //get the string from the preference (this will be empty string if there is no data in there
        //yet). as a result the output array list will be null, so we need to check for this in the
        //save array list when we pull the old data
        String response = sharedPreferences.getString(ARRAY_PREF_KEY, "");
        //convert the json string back to Fence Array list and return it
        ArrayList<Fence> outputArrayList = gson.fromJson(response,
                new TypeToken<List<Fence>>() {
                }.getType());

        return outputArrayList;

    }//loadArrayList

    /**
     * Helper method for saving the new ArrayList<Fence> by replcaing the old one
     *
     * @param context        : context
     * @param inputArrayList : new arraylist to be added on top of the old one
     */
    public static void saveArrayList(Context context, ArrayList<Fence> inputArrayList) {

        //get reference to shared pref
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);

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
        editor.remove(ARRAY_PREF_KEY).apply();
        //add the new updated list
        editor.putString(ARRAY_PREF_KEY, json);
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
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);

        //create Gson object
        Gson gson = new Gson();

        //convert arraylist
        String json = gson.toJson(updatedArrayList);

        //get the shared preference editor
        SharedPreferences.Editor editor = sharedPreferences.edit();
        //since we have added the old data to the new list, we can now delete the last entry
        editor.remove(ARRAY_PREF_KEY).apply();
        //add the new updated list
        editor.putString(ARRAY_PREF_KEY, json);
        editor.apply();
    }//updateArrayList

    //This deletes the Arraylist from the pref
    private static void eraseAllArrays(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(ARRAY_PREF_KEY).apply();
    }//eraseAllArray

    //This removes all the geofence by unregisterring them, and deleting the arraylist
    private void removeAllFences(Context context) {
        //get the list
        ArrayList<Fence> fenceArrayList = loadArrayList(context);
        //check for empty list and show toast
        if (fenceArrayList == null) {
            displayToast(App.getContext().getString(R.string.no_geofence_show_toast));
            Log.v(TAG, "No geofence to remove");
            return;
        }
        //make a list of all registered fences' ids
        ArrayList<String> idArrayList = new ArrayList<>();
        for (Fence fence : fenceArrayList) {
            idArrayList.add(fence.getId());
        }

        //get the geofencing client and remove all fences
        GeofencingClient geofencingClient = LocationServices.getGeofencingClient(context);
        geofencingClient.removeGeofences(idArrayList);

        //also clean the internal arrayList
        eraseAllArrays(context);
        displayToast(App.getContext().getString(R.string.all_locations_removed_toast));
        recreate();
        Log.v(TAG, "Geofences removed");
    }//removeAllFences

    //helper methods to get the bounds of the circle (radius in meters)
    public static LatLng swCorner(LatLng center, float radius) {
        double distanceFromCenterToCorner = ((double) radius) * Math.sqrt(2.0);
        LatLng southwestCorner =
                SphericalUtil.computeOffset(center, distanceFromCenterToCorner, 225.0);
        return southwestCorner;
    }//swCorner

    public static LatLng neCorner(LatLng center, float radius) {
        double distanceFromCenterToCorner = ((double) radius) * Math.sqrt(2.0);
        LatLng northeastCorner =
                SphericalUtil.computeOffset(center, distanceFromCenterToCorner, 45.0);
        return northeastCorner;
    }//neCorner

    //checks location permission
    public static boolean checkLocationPermission(Context context) {
        //check for location permission and ask for it
        return ContextCompat.checkSelfPermission(context,
                android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }//checkLocationPermission

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

    //helper method for creating notification channel required for Android 8 and higher. This needs
    //to be called before trying to send notification
    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = CHANNEL_ID;
            String description = getString(R.string.notification_description);

            int importance = NotificationManager.IMPORTANCE_HIGH;

            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            //enable light
            channel.enableLights(true);

            //vibrate
            long[] vibratorPattern = {0, 100, 200, 200, 200, 300, 1000};
            channel.setVibrationPattern(vibratorPattern);
            channel.enableVibration(true);

            //set default sound
            Uri defaultUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            channel.setSound(defaultUri, new AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION).build());

            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this. You will send to the system's settings
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }//API 26 higher
    }//createNotificationChannel

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

    //Adds the banner Ad
    private void addBannerAd() {
        //initialize the Mobile Ads SDK
        MobileAds.initialize(this, new OnInitializationCompleteListener() {
            @Override
            public void onInitializationComplete(InitializationStatus initializationStatus) {
            }
        });
        //find the ad view
        mAdView = findViewById(R.id.ad_view);
        //set listener
        mAdView.setAdListener(new ToastAdListener(getApplicationContext()));
        //create adRequest
        AdRequest adRequest = new AdRequest.Builder()
                .build();
        //load the ad request into the ad view
        mAdView.loadAd(adRequest);
    }//addBannerAd

    //helper method for removing all data
    private void deleteAllList() {
        //show a dialog for confirmation
        showDeleteAllDialog();
    }//deleteAllList

    //Helper method for sorting list based on their name (ascending)
    private void sortNameAscending() {
        Collections.sort(mFenceArrayList, new Comparator<Fence>() {
            @Override
            public int compare(Fence o1, Fence o2) {
                return o1.getId().compareTo(o2.getId());
            }
        });
        //notify the mAdapter that the data has changed, and it should update
        mAdapter.notifyDataSetChanged();
    }//sortNameAscending

    //Helper method for sorting list based on their name (ascending)
    private void sortNameDescending() {
        Collections.sort(mFenceArrayList, new Comparator<Fence>() {
            @Override
            public int compare(Fence o1, Fence o2) {
                return o2.getId().compareTo(o1.getId());
            }
        });
        //notify the mAdapter that the data has changed, and it should update
        mAdapter.notifyDataSetChanged();
    }//sortNameAscending

    ////Helper method for sorting list based on their expiray (ascending)
    private void sortExpiryAscending() {
        Collections.sort(mFenceArrayList, new Comparator<Fence>() {
            @Override
            public int compare(Fence f1, Fence f2) {
                Long t1 = f1.getExpiaryTimeMilliSec();
                Long t2 = f2.getExpiaryTimeMilliSec();

                return t1.compareTo(t2);
            }
        });
        //notify the mAdapter that the data has changed, and it should update
        mAdapter.notifyDataSetChanged();
    }//sortExpiryAscending

    ////Helper method for sorting list based on their expiray (descending)
    private void sortExpiryDescending() {
        Collections.sort(mFenceArrayList, new Comparator<Fence>() {
            @Override
            public int compare(Fence f1, Fence f2) {
                Long t1 = f1.getExpiaryTimeMilliSec();
                Long t2 = f2.getExpiaryTimeMilliSec();

                return t2.compareTo(t1);
            }
        });
        //notify the mAdapter that the data has changed, and it should update
        mAdapter.notifyDataSetChanged();
    }//sortExpiryAscending

    //Helper method for showing the dialog for deleting all data
    private void showDeleteAllDialog() {
        //create the builder
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        //add message and button functionality
        builder.setMessage(R.string.delete_all_dialog_msg)
                .setPositiveButton(R.string.delete_all, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //Delete all
                        removeAllFences(getApplicationContext());
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //close the dialog
                        dialog.dismiss();
                    }
                });

        AlertDialog dialog = builder.create();
        dialog.show();
    }//showUnsavedChangesDialog

    //helper method for showing the dialog when the user long clicks on item
    private void showLongClickDialog(final Fence fence) {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        String expiry = fence.getExpiry();
        dialogBuilder.setTitle(fence.getId());
        //show re-activate if it is expired
        String[] list;
        if (expiry.equals(getString(R.string.expired))) {
            list = new String[]{getString(R.string.reactivate), getString(R.string.delete), getString(R.string.cancel)};
        } else {
            list = new String[]{getString(R.string.edit), getString(R.string.delete), getString(R.string.cancel)};
        }
        dialogBuilder.setItems(list, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int index) {
                switch (index) {
                    case 0:
                        //reactivate
                        Intent editIntent = new Intent(getApplicationContext(), AddGeofenceActivity.class);
                        editIntent.putExtra(FENCE_EDIT_EXTRA_INTENT, fence.getId());
                        //set the update boolean
                        needsUpdate = true;
                        startActivity(editIntent);
                        break;
                    case 1:
                        //delete
                        showDeleteConfirmationDialog(fence);
                        break;
                    case 2:
                        //cancel
                        dialog.dismiss();
                        break;
                }//switch
            }
        });
        dialogBuilder.create().show();
    }//showBadLocationDialog

    //helper method for delete confirmation
    private void showDeleteConfirmationDialog(final Fence fence) {
        //get the id
        String fenceId = fence.getId();
        //create the builder
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        String message = getString(R.string.dialog_delete_fence) + " \"" + fenceId + "\"" +
                getString(R.string.question_mark);

        //add message and button functionality
        builder.setMessage(message)
                .setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //delete fence
                        deleteGeofence(fence);
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //close the dialog
                        dialog.dismiss();
                    }
                });

        AlertDialog dialog = builder.create();
        dialog.show();
    }//showDeleteConfirmationDialog

    //helper method for deleting the geofence
    private void deleteGeofence(Fence targetFence) {
        //get the id of the fence
        final String fenceId = targetFence.getId();
        //retrieve the array list of Fences
        mFenceArrayList = loadArrayList(this);

        //find the Fence object we want to edit
        Fence fence = null;
        int fenceIndexInArray = -1;
        for (Fence f : mFenceArrayList) {
            if (f.getId().equalsIgnoreCase(fenceId)) {
                fence = f;
                //get the index of our Fence object
                fenceIndexInArray = mFenceArrayList.indexOf(f);
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
        oldFenceIdList.add(fenceId);

        mGeofencingClient.removeGeofences(oldFenceIdList).addOnSuccessListener(this, new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                Log.v(TAG, "Geofence removed successfully");
                displayToast("\"" + fenceId + "\" " + App.getContext().getString(R.string.geofence_removed));
                //set the update boolean true
            }
        })
                .addOnFailureListener(this, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.v(TAG, "Geofence removal error");
                        displayToast(App.getContext().getString(R.string.error_remove_geofence));
                    }
                });

        //remove the geofence from the main arraylist (return true if it was removed)
        boolean removeSuccess = mFenceArrayList.remove(fence);

        //update the main arraylist
        if (removeSuccess) {
            //update app's arrayList
            updateArrayList(this, mFenceArrayList);
            //recreate activity
            recreate();
        }
    }//deleteGeofence

    private static void displayToast(String message) {
        Toast.makeText(App.getContext(), message, Toast.LENGTH_SHORT).show();
    }//displayToast

}//MainActivity












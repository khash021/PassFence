package com.khashayarmortazavi.testgeofence;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    //used for saving/loading arraylist to sharedpref
    public final static String MY_PREF_ARRAY_KEY = "key";
    public final static String MY_PREF_NAME = "myPref";

    private final static String TAG = MainActivity.class.getSimpleName();
    private final static int REQUEST_CODE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //check for location permission and ask for it
        if (!checkPermission(this)) {

            // No explanation needed; request the permission
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_CODE);
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

        ((Button) findViewById(R.id.button_view_geofence_activity)).setOnClickListener(new View.OnClickListener() {
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
    }//eraseAllArray


    public static boolean checkPermission (Context context) {
        //check for location permission and ask for it
        if (ContextCompat.checkSelfPermission(context,
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            return false;
        } else {
            return true;
        }
    }//checkPermission



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }//onCreateOptionsMenu

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }//onOptionsItemSelected

}//MainActivity

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
        if (ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {


            // No explanation needed; request the permission
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_CODE);

            // REQUEST_CODE is an
            // app-defined int constant. The callback method gets the
            // result of the request.
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


    }//onCreate

    public static ArrayList<String> loadArrayList(Context context) {

        Gson gson = new Gson();
        SharedPreferences sharedPreferences = context.getSharedPreferences(MY_PREF_NAME, Context.MODE_PRIVATE);
        String response = sharedPreferences.getString(MY_PREF_ARRAY_KEY, "");
        ArrayList<String> outputArrayList = gson.fromJson(response,
                new TypeToken<List<String>>(){}.getType());

        return outputArrayList;

    }//loadArrayList

    public static void saveArrayList(Context context, ArrayList<String> inputArrayList) {

        //get reference to shared pref
        SharedPreferences sharedPreferences = context.getSharedPreferences(MY_PREF_NAME, Context.MODE_PRIVATE);

        Gson gson = new Gson();

        //load the previous data, and add the new list to it
        ArrayList<String> fullList = loadArrayList(context);
        //if there is nothing in there, this will be null, so we instantiate it
        if (fullList == null) {
            fullList = new ArrayList<>();
        }
        //add the new data to it
        fullList.addAll(inputArrayList);
//        for (String item : inputArrayList) {
//            fullList.add(item);
//        }

        //convert arraylist
        String json = gson.toJson(fullList);

        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(MY_PREF_ARRAY_KEY).apply();
        editor.putString(MY_PREF_ARRAY_KEY, json);
        editor.apply();
    }//saveArrayList



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

    @Override
    protected void onStart() {
        super.onStart();
    }//onStart

    @Override
    protected void onStop() {
        super.onStop();
    }//onStop

}//MainActivity

package tech.khash.passfence;

import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.app.TaskStackBuilder;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Khashayar "Khash" Mortazavi
 * <p>
 * This is the class (which extends IntentService) gets called when the registered geofences
 * are triggered.
 * <p>
 * Here we get the geofence that triggered this, and then extract the required data from it ( such
 * as the event that triggered it, the name of the fence.
 * <p>
 * This is also where we send the notification. All the notification properties are set from the
 * corresponding Fence object, and the app's share preferences
 */

public class GeofenceTransitionsIntentService extends IntentService {

    protected static final String TAG = GeofenceTransitionsIntentService.class.getSimpleName();

    public final static int NOTIFICATION_CHANNEL_ID = 1;

    public GeofenceTransitionsIntentService() {
        // Use the TAG to name the worker thread.
        super(TAG);
    }//GeofenceTransitionsIntentService

    @Override
    public void onCreate() {
        super.onCreate();
    }//onCreate

    /**
     * This is the meat of this class that handles the functionality.
     *
     * @param intent : the intent that triggered this
     */
    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        //Create a GeofencingEvent object by calling the fromIntent method with our intent as an input
        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);
        //Checks to see if there are any error messages and exit the function in that case.
        //if no errors then execute the rest of the code.
        if (geofencingEvent.hasError()) {
            String errorMessage = GeofenceErrorMessages.getErrorString(this,
                    geofencingEvent.getErrorCode());
            Log.e(TAG, errorMessage);
            //if there is any error we return and end it here
            return;
        }//if

        //get the transition type (enter or exit). this return an int 1 for enter, 2 for exit
        int geofenceTransition = geofencingEvent.getGeofenceTransition();

        //test to make sure it is of interest (entry or exit)
        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER ||
                geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT) {

            // Get the geofences that were triggered. A single event can trigger multiple geofences.
            List<Geofence> triggeringGeofences = geofencingEvent.getTriggeringGeofences();

            // Get the transition details as a String.
            //This helper method gets all the triggered geofences and returns it as a string
            String geofenceTransitionDetails = getGeofenceTransitionDetails(
                    this,
                    geofenceTransition,
                    triggeringGeofences
            );

            // Send notification and log the transition details.
            sendNotificationWithChannel(geofenceTransitionDetails);
            Log.i(TAG, geofenceTransitionDetails);

        } else {
            // Log the error.
            Log.e(TAG, getString(R.string.geofence_transition_invalid_type));
        }
    }//onHandleIntent

    //This gets the details of the Geofence that triggered this
    private String getGeofenceTransitionDetails(
            Context context,
            int geofenceTransition,
            List<Geofence> triggeringGeofences) {

        String geofenceTransitionString = getTransitionString(geofenceTransition);

        // Get the Ids of each geofence that was triggered.
        ArrayList triggeringGeofencesIdsList = new ArrayList();
        for (Geofence geofence : triggeringGeofences) {
            triggeringGeofencesIdsList.add(geofence.getRequestId());
        }
        String triggeringGeofencesIdsString = TextUtils.join(", ", triggeringGeofencesIdsList);

        return geofenceTransitionString + ": " + triggeringGeofencesIdsString;
    } //getGeofenceTransitionDetails

    //Creates a string from the transitionType integer codes
    private String getTransitionString(int transitionType) {
        switch (transitionType) {
            case Geofence.GEOFENCE_TRANSITION_ENTER:
                return getString(R.string.geofence_transition_entered);
            case Geofence.GEOFENCE_TRANSITION_EXIT:
                return getString(R.string.geofence_transition_exited);
            default:
                return getString(R.string.unknown_geofence_transition);
        }
    } //getTransitionString

    //helper method for creating the notification builder
    private void sendNotificationWithChannel(String notificationDetails) {
        /*  By default, the notification's text content is truncated to fit one line.
            If you want your notification to be longer, you can enable an expandable notification
            by adding a style template with setStyle() */

        /**       This creates a normal notification, the back button acts normal in the app   */
        // Create an Intent to start password settings
        Intent passwordIntent = new Intent(Settings.ACTION_SECURITY_SETTINGS);
        // Create the TaskStackBuilder and add the intent, which inflates the back stack
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addNextIntentWithParentStack(passwordIntent);
        // Get the PendingIntent containing the entire back stack
        PendingIntent normalPendingIntent =
                stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

        /* Create NotificationCompat.Builder object
           NotificationCompat.Builder constructor requires that you provide a channel ID. This is
           required for compatibility with Android 8.0 (API level 26) and higher,
           but is ignored by older versions */
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, MainActivity.CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(notificationDetails)
                .setContentText(getString(R.string.notification_message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                // Set the intent that will fire when the user taps the notification
                .setContentIntent(normalPendingIntent)
                //setAutoCancel(), which automatically removes the notification when the user taps it.
                .setAutoCancel(true);

        //get the preferences
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        String ring = sharedPreferences.getString(MainActivity.RINGTONE_PREF_KEY, "def");
        boolean vibrate = sharedPreferences.getBoolean(MainActivity.VIBRATE_PREF_KEY, true);
        boolean colorBoolean = sharedPreferences.getBoolean(MainActivity.LED_PREF_KEY, true);
        String priority = sharedPreferences.getString(MainActivity.PRIORITY_PREF_KEY, "def");
        int colorInt = sharedPreferences.getInt(MainActivity.COLOR_PICKER_PREF_KEY, -1);
        Log.v(TAG, "Vibrate : " + vibrate + "\nRingtone: " + ring
                + "\nColor: " + colorInt + "\nPriority : " + priority);

        //set the details, these only work for lower devices, Oreo and higher gets this from channel
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            //set priority
            if (priority != null && !priority.equals("def")) {
                builder.setPriority(Integer.valueOf(priority));
            }

            //set ringtone
            if (ring != null && !ring.equals("def")) {
                //different ways for APIs
                Uri ringUri = Uri.parse(ring);
                builder.setSound(ringUri);
            }//ring

            //set vibrate
            if (vibrate) {
                //set the vibrating pattern and vibrate (you need to add vibrate permission in manifest)
                // Start without a delay
                // Each element then alternates between vibrate, sleep, vibrate, sleep...
                long[] vibratorPattern = {0, 100, 200, 200, 200, 300, 1000};
                //set it on the notification
                builder.setVibrate(vibratorPattern);
            }//vibrate

            //set notification light
            //Set the argb(Alpha(opacity) Red Green Blue) value that you would like the LED on the device to blink, as well as the rate.
            // The rate is specified in terms of the number of milliseconds to be on and then the number of milliseconds to be off.
            //get rid of the first # and then add ff for alpha
            if (colorBoolean) {
                if (colorInt != -1) {
                    String colorStringHex = Integer.toHexString(colorInt);
                    int ledColor = Color.parseColor("#" + colorStringHex);
                    Log.v(TAG, "LED color : " + ledColor);
                    try {
                        builder.setLights(ledColor, 500, 500);
                    } catch (Exception e) {
                        Log.v(TAG, "Error setting color" + e);
                    }//tr-catch
                }
            }//color

        }//if - Lower Oreo

        //show the notification
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);

        // notificationId is a unique int for each notification that you must define
        notificationManager.notify(NOTIFICATION_CHANNEL_ID, builder.build());
    }//sendNotificationWithChannel
}//GeofenceTransitionsIntentService




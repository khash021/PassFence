package tech.khash.passfence;

import android.app.IntentService;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.app.TaskStackBuilder;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;

import java.util.ArrayList;
import java.util.List;

public class GeofenceTransitionsIntentService extends IntentService {

    protected static final String TAG = GeofenceTransitionsIntentService.class.getSimpleName();

    //Notification channels for Android 8 and higher
    private final static String CHANNEL_ID = "pass_fence_notification_channel";
    private final static int SIMPLE_NOTIFICATION_ID = 1;

    public GeofenceTransitionsIntentService() {
        // Use the TAG to name the worker thread.
        super(TAG);
    }//GeofenceTransitionsIntentService

    @Override
    public void onCreate() {
        super.onCreate();

        createNotificationChannel();
    }//onCreate

    //This is the meat of this class that handles the functionality. We pass in an Intent, intent,
    //and there are no outputs
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
                geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT ) {

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
//            sendNotification(geofenceTransitionDetails);
            sendNotificationWithChannel(geofenceTransitionDetails);
            Log.i(TAG, geofenceTransitionDetails);

        } else {
            // Log the error.
            Log.e(TAG, getString(R.string.geofence_transition_invalid_type));
        }

    }//onHandleIntent

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

    //helper method for creating notification channel required for Android 8 and higher. This needs
    //to be called before trying to send notification
    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "notification_name";
            String description = "notification_description";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }//createNotificationChannel



    //helper method for creating the notification builder
    private void sendNotificationWithChannel(String notificationDetails) {


        /**       This creates a special notification, the back button exits the activity   */
        //we want this notification to open the main activity so we create a pending intent and pass into the builder
        // Create an explicit intent for an Activity in your app
        Intent specialIntent = new Intent(this, AddGeofenceActivity.class);
        specialIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent specialPendingIntent = PendingIntent.getActivity(this, 0, specialIntent, 0);


        /**       This creates a normal notification, the back button acts normal in the app   */
        // Create an Intent for the activity you want to start
        Intent normalIntent = new Intent(this, MainActivity.class);
        Intent passwordIntent = new Intent(Settings.ACTION_SECURITY_SETTINGS);
        // Create the TaskStackBuilder and add the intent, which inflates the back stack
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addNextIntentWithParentStack(passwordIntent);
        // Get the PendingIntent containing the entire back stack
        PendingIntent normalPendingIntent =
                stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);


        //create NotificationCompat.Builder object
        //NotificationCompat.Builder constructor requires that you provide a channel ID. This is
        // required for compatibility with Android 8.0 (API level 26) and higher, but is ignored by older versions
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(notificationDetails)
                .setContentText("Tap to go to password settings")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                // Set the intent that will fire when the user taps the notification
                .setContentIntent(normalPendingIntent)
                //setAutoCancel(), which automatically removes the notification when the user taps it.
                .setAutoCancel(true);
        //NOTE: By default, the notification's text content is truncated to fit one line. If you want your
        // notification to be longer, you can enable an expandable notification by adding a style template with setStyle()


        //show the notification
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);

        // notificationId is a unique int for each notification that you must define
        notificationManager.notify(SIMPLE_NOTIFICATION_ID, mBuilder.build());
    }//testNotificationWithChannel

//    //this helper method makes a notification
//    private void sendNotification(String notificationDetails) {
//        // Create an explicit content Intent that starts the main Activity.
//        Intent notificationIntent = new Intent(getApplicationContext(), MainActivity.class);
//
//        // Construct a task stack.
//        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
//
//        // Add the main Activity to the task stack as the parent.
//        stackBuilder.addParentStack(MainActivity.class);
//
//        // Push the content Intent onto the stack.
//        stackBuilder.addNextIntent(notificationIntent);
//
//        // Get a PendingIntent containing the entire back stack.
//        PendingIntent notificationPendingIntent =
//                stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
//
//        // Get a notification builder that's compatible with platform versions >= 4
//        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
//
//        // Define the notification settings.
//        builder.setSmallIcon(R.drawable.ic_notification)
//                // In a real app, you may want to use a library like Volley
//                // to decode the Bitmap.
////                .setLargeIcon(BitmapFactory.decodeResource(getResources(),
////                        R.mipmap.ic_launcher))
//                .setColor(Color.GREEN)
//                .setContentTitle(notificationDetails)
//                .setContentText(getString(R.string.geofence_transition_notification_text))
//                .setContentIntent(notificationPendingIntent);
//
//        // Dismiss notification once the user touches it.
//        builder.setAutoCancel(true);
//
//        // Get an instance of the Notification manager
//        NotificationManager mNotificationManager =
//                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
//
//        // Issue the notification
//        mNotificationManager.notify(0, builder.build());
//    }//sendNotification

}//GeofenceTransitionsIntentService


//        /**       This creates a normal notification, the back button acts normal in the app   */
//        // Create an Intent to start password settings
//        Intent passwordIntent = new Intent(Settings.ACTION_SECURITY_SETTINGS);
//        // Create the TaskStackBuilder and add the intent, which inflates the back stack
//        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
//        stackBuilder.addNextIntentWithParentStack(passwordIntent);
//        // Get the PendingIntent containing the entire back stack
//        PendingIntent normalPendingIntent =
//                stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
//
//
//        //create NotificationCompat.Builder object
//        //NotificationCompat.Builder constructor requires that you provide a channel ID. This is
//        // required for compatibility with Android 8.0 (API level 26) and higher, but is ignored by older versions
//        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
//                .setSmallIcon(R.drawable.ic_notification)
//                .setContentTitle(notificationDetails)
//                .setContentText(getString(R.string.notification_message))
//                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
//                // Set the intent that will fire when the user taps the notification
//                .setContentIntent(normalPendingIntent)
//                //setAutoCancel(), which automatically removes the notification when the user taps it.
//                .setAutoCancel(true);
//
//        //get the default notification and set it
//        Uri defaultSoundUri= RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
//        builder.setSound(defaultSoundUri);
//
//        //TODO: get the vibrate from sharedPreferences
//        //set the vibrating pattern and vibrate (you need to add vibrate permission in manifest)
//        // Start without a delay
//        // Each element then alternates between vibrate, sleep, vibrate, sleep...
//        long[] vibratorPattern = {0, 100, 200, 200, 200, 300, 1000};
//        //set it on the notification
//        builder.setVibrate(vibratorPattern);
//
//        //set notification light
//        //Set the argb(Alpha(opacity) Red Green Blue) value that you would like the LED on the device to blink, as well as the rate.
//        // The rate is specified in terms of the number of milliseconds to be on and then the number of milliseconds to be off.
//
//        int argb = Resources.getSystem().getColor(R.color.notificationLight);
//        builder.setLights(argb, 300, 300);

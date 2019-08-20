package tech.khash.passfence;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.maps.model.LatLng;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * Created by Khashayar "Khash" Mortazavi
 * <p>
 * Custom class for our Fence object.
 * <p>
 * This is our object that holds all the data for each location added by the user and holds
 * all the corresponding data such as ID (name), duration, expiry, criteria, etc
 * <p>
 * It has getters and setters and a public constructor.
 * <p>
 * It also creates Geofence object from the Fence object to be used for adding geofences
 */
public class Fence {

    private String id;
    private double latitude, longitude;
    private float radius;
    private long duration;
    private int type;
    private String durationString;
    private long expiaryTimeMilliSec;

    public static final int FENCE_TYPE_ENTER = 1;
    public static final int FENCE_TYPE_EXIT = 2;
    public static final int FENCE_TYPE_ENTER_EXIT = 3;

    private static final long HOUR_IN_MILLISEC = 3600000;

    /**
     * Public constructor
     *
     * @param id       : name of the Fence. Needs to be unique
     * @param lat      : latitude of the geofence
     * @param lng      : longitude of the geofence
     * @param radius   : radius of the geofence in meters
     * @param duration : duration of the geofence input as hours
     * @param type     : criteria: ENTER, EXIT, or both
     */
    public Fence(String id, double lat, double lng, float radius, long duration, int type) {
        this.id = id;
        this.latitude = lat;
        this.longitude = lng;
        this.radius = radius;
        this.type = type;

        //Sets the duration (-1 means never expires)
        this.durationString = (duration == -1) ? App.getContext().getString(R.string.never) : String.valueOf(duration);
        this.duration = (duration == -1) ? -1 : duration * HOUR_IN_MILLISEC;

        //Here we set the exact Epoch of expiry in milliseconds used for showing the calender of expiry
        if (duration == -1) {
            expiaryTimeMilliSec = -1;
        } else {
            expiaryTimeMilliSec = Calendar.getInstance().getTimeInMillis() +
                    (duration * HOUR_IN_MILLISEC);
        }
    }//public constructor

    /**
     * Getter methods
     */
    public String getId() {
        return id;
    }

    public LatLng getLatLng() {
        return new LatLng(latitude, longitude);
    }

    public float getRadius() {
        return radius;
    }

    public long getDuration() {
        return duration;
    }

    public int getType() {
        return type;
    }

    public long getExpiaryTimeMilliSec() {
        return expiaryTimeMilliSec;
    }

    public boolean isActive() {
        if (expiaryTimeMilliSec == -1) {
            return true;
        } else {
            return expiaryTimeMilliSec > Calendar.getInstance().getTimeInMillis();
        }
    }//isActive

    //This is used for adding this to our circle and marker, so we can extract the corresponding fence
    //object from the marker/circle on map
    public String getSnippet() {
        String output;
        String inputExpiry = getExpiry().trim();
        String expiry;
        if (inputExpiry.equalsIgnoreCase(App.getContext().getString(R.string.never))) {
            expiry = App.getContext().getString(R.string.expires_never);
        } else if (inputExpiry.equalsIgnoreCase(App.getContext().getString(R.string.expired))) {
            expiry = App.getContext().getString(R.string.expired);
        } else {
            expiry = inputExpiry;
        }
        output = id + "," + expiry + "," + getStringType();
        return output;
    }//getSnippet

    //It defines whether the fence is expired, never expires, or returns the date of expiry
    public String getExpiry() {
        if (expiaryTimeMilliSec == -1) {
            return App.getContext().getString(R.string.never);
        } else if (Calendar.getInstance().getTimeInMillis() > expiaryTimeMilliSec) {
            return App.getContext().getString(R.string.expired);
        } else {
            final DateFormat dateFormat = new SimpleDateFormat("MMM.dd 'at' HH:mm");
            return App.getContext().getString(R.string.expires_colon) + " " + dateFormat.format(expiaryTimeMilliSec);
        }
    }//getExpiry

    public String getStringType() {
        switch (type) {
            case FENCE_TYPE_ENTER:
                return App.getContext().getString(R.string.enter);
            case FENCE_TYPE_EXIT:
                return App.getContext().getString(R.string.exit);
            case FENCE_TYPE_ENTER_EXIT:
                return App.getContext().getString(R.string.enter_exit);
            default:
                return App.getContext().getString(R.string.error);
        }//switch
    }//getStringType

    //helper method for getting the spinner index based on the duration
    public int getDurationIndex() {
        switch (durationString) {
            case "1":
                return 0;
            case "2":
                return 1;
            case "3":
                return 2;
            case "4":
                return 3;
            case "5":
                return 4;
            case "8":
                return 5;
            case "10":
                return 6;
            case "12":
                return 7;
            case "24":
                return 8;
            case "Never":
                return 9;
            default:
                return -1;
        }
    }//getDurationIndex

    //helper method for getting the Geofence Object
    private Geofence getGeofenceObject() {
        //make the builder
        Geofence.Builder geofenceBuilder = new Geofence.Builder();
        geofenceBuilder.setRequestId(id)
                .setCircularRegion(latitude, longitude, radius)
                .setExpirationDuration(duration)
                .setTransitionTypes(getTransitionType());

        //return Geofence
        return geofenceBuilder.build();
    }//getGeofenceObject

    //helper method for getting the GeofencingRequest Object for registering geofence
    public GeofencingRequest getGeofencingRequestObject() {
        //create the builder
        GeofencingRequest.Builder requestBuilder = new GeofencingRequest.Builder();

        //this means that GEOFENCE_TRANSITION_ENTER should be triggered if the device is already inside the geofence
        requestBuilder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER);

        requestBuilder.addGeofence(getGeofenceObject());

        return requestBuilder.build();

    }//getGeofencingRequestObject

    //private helper method for getting transition type
    private int getTransitionType() {
        switch (type) {
            case FENCE_TYPE_ENTER:
                return Geofence.GEOFENCE_TRANSITION_ENTER;
            case FENCE_TYPE_EXIT:
                return Geofence.GEOFENCE_TRANSITION_EXIT;
            case FENCE_TYPE_ENTER_EXIT:
            default:
                return (Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT);
        }//switch
    }//getTransitionType

}//Fence class

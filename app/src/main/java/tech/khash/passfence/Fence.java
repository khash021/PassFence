package tech.khash.passfence;

import com.google.android.gms.maps.model.LatLng;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * Custom class for saving, and tracking geofence objects
 */

//TODO: add a method to create the Geofence object so the classes can just call that and then add it to the Geofence client. This will return GeofencingRequest object
//TODO: all add a removeGeofence object, easily get the id and remove it

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

    public static final long HOUR_IN_MILLISEC = 3600000;


    public Fence(String id, double lat, double lng, float radius, long duration, int type) {
        this.id = id;
        this.latitude = lat;
        this.longitude = lng;
        this.radius = radius;
        this.type = type;

        this.durationString = (duration == -1) ? "Never" : String.valueOf(duration);
        this.duration = (duration == -1) ? -1 : duration * HOUR_IN_MILLISEC;


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

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
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
            if (expiaryTimeMilliSec > Calendar.getInstance().getTimeInMillis()) {
                return true;
            } else {
                return false;
            }
        }
    }//isActive

    public String getSnippet() {
        String output;
        output = id + "," + getExpiary().trim() + "," + getStringType();
        return output;
    }

    //TODO: these two used to be private, change it back once you get rid of the simple list adapter
    public String getExpiary() {
        if (expiaryTimeMilliSec == -1) {
            return "Never";
        } else if (Calendar.getInstance().getTimeInMillis() > expiaryTimeMilliSec) {
            return "Expired";
        } else {
            final DateFormat dateFormat = new SimpleDateFormat("MMM.dd 'at' HH:mm");
            return "Expires: " + dateFormat.format(expiaryTimeMilliSec);
        }
    }

    public String getStringType() {
        switch (type) {
            case FENCE_TYPE_ENTER:
                return "Enter";
            case FENCE_TYPE_EXIT:
                return "Exit";
            case FENCE_TYPE_ENTER_EXIT:
                return "Enter/Exit";
            default:
                return "Error";
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
    }

    /**
     * Setter methods
     */

    public void setId(String id) {
        this.id = id;
    }

    public void setLatitude(double lat) {
        this.latitude = lat;
    }

    public void setLongitude(double lng) {
        this.longitude = lng;
    }

    public void setRadius(float radius) {
        this.radius = radius;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public void setType(int type) {
        this.type = type;
    }


}//Fence class

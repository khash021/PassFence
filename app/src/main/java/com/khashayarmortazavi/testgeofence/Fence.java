package com.khashayarmortazavi.testgeofence;

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
    private boolean active;
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
        this.durationString = (duration == -1) ? "Never" : String.valueOf(duration);
        this.duration = (duration == -1) ? -1 : duration * HOUR_IN_MILLISEC;
        this.type = type;
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

    public boolean isActive() {
        return active;
    }

    public String getSnippet() {
        String output;
        output = id + "," + getExpiary().trim() + "," + getStringType();
        return output;
    }

    public String getExpiary() {
        if (expiaryTimeMilliSec == -1) {
            return "Never";
        } else {
            final DateFormat dateFormat = new SimpleDateFormat("MMM.dd 'at' HH:mm");
            return dateFormat.format(expiaryTimeMilliSec);
        }
    }

    private String getStringType() {
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

    public void setActive(boolean active) {
        this.active = active;
    }


}//Fence class

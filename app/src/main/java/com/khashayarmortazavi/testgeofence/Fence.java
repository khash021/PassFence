package com.khashayarmortazavi.testgeofence;

import com.google.android.gms.maps.model.LatLng;

/**
 *
 * Custom class for saving, and tracking geofence objects
 *
 *
 */

public class Fence {

    private String id;
    private double latitude, longitude;
    private float radius;
    private long duration;
    private int type;
    private boolean active;

    public static final int FENCE_TYPE_ENTER = 1;
    public static final int FENCE_TYPE_EXIT = 2;
    public static final int FENCE_TYPE_ENTER_EXIT = 3;



    public Fence(String id, double lat, double lng, float radius, long duration, int type) {
        this.id = id;
        this.latitude = lat;
        this.longitude = lng;
        this.radius = radius;
        this.duration = duration;
        this.type = type;
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

    public String getCompleteFenceInfo () {
        String output;
        output = "id: " + id +
                "\nLat: " + latitude + ", Lng: " + longitude +
                "\nRadius: " + radius + ", duration: " + duration +
                "\nType: " + type;
        return output;
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

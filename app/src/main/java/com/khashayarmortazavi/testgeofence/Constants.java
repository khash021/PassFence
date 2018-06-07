package com.khashayarmortazavi.testgeofence;

import com.google.android.gms.maps.model.LatLng;

import java.util.HashMap;

public final class Constants {

    private Constants() {
    }

    public static final String PACKAGE_NAME = "com.google.android.gms.location.Geofence";

    public static final String SHARED_PREFERENCES_NAME = PACKAGE_NAME + ".SHARED_PREFERENCES_NAME";

    public static final String GEOFENCES_ADDED_KEY = PACKAGE_NAME + ".GEOFENCES_ADDED_KEY";

    /**
     * Used to set an expiration time for a geofence. After this amount of time Location Services
     * stops tracking the geofence.
     */
    public static final long GEOFENCE_EXPIRATION_IN_HOURS = 12;

    /**
     * For this sample, geofences expire after twelve hours.
     */
    public static final long GEOFENCE_EXPIRATION_IN_MILLISECONDS =
            GEOFENCE_EXPIRATION_IN_HOURS * 60 * 60 * 1000;
    //public static final float GEOFENCE_RADIUS_IN_METERS = 1609; // 1 mile, 1.6 km
    public static final float GEOFENCE_RADIUS_IN_METERS = 1; // 1 mile, 1.6 km

    public static final float RADIUS_IN_METERS = 50;

    /**
     * Map for storing information about airports in the San Francisco bay area.
     */
    public static final HashMap<String, LatLng> BAY_AREA_LANDMARKS = new HashMap<String, LatLng>();
    static {

        // San Francisco International Airport.
        BAY_AREA_LANDMARKS.put("SFO", new LatLng(37.621313, -122.378955));

        // Googleplex.
        BAY_AREA_LANDMARKS.put("GOOGLE", new LatLng(37.422611,-122.0840577));

        // Test
        BAY_AREA_LANDMARKS.put("Udacity Studio", new LatLng(37.3999497,-122.1084776));
    }

    /**
     * Headquarters
     */
    public static final HashMap<String, LatLng> ASCS_HEADQUARTERS = new HashMap<String, LatLng>();
    static {
        ASCS_HEADQUARTERS.put("ASCS", new LatLng(49.235675, -123.057118));
    }


    /**
     * TEST May.23.18
     */
    public static final HashMap<String, LatLng> MAY_23_FENCES = new HashMap<String, LatLng>();
    static {
        //Home
        MAY_23_FENCES.put("ASCS", new LatLng(49.235675, -123.057118));

        //29th ave station
        MAY_23_FENCES.put("29th", new LatLng( 49.244358, -123.046240));

        //Science center sky station
        MAY_23_FENCES.put("Science", new LatLng( 49.273273, -123.101158));

        //Amazon
        MAY_23_FENCES.put("AWS", new LatLng( 49.280978, -123.117006));

        //Walmart
        MAY_23_FENCES.put("Walmart", new LatLng(49.259505, -123.027523));

        //Willow cafe
        MAY_23_FENCES.put("Willow Cafe", new LatLng(49.257121, -123.121876));
    }



}

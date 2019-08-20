package tech.khash.passfence;


import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.Marker;

/**
 * Created by Khashayar "Khash" Mortazavi
 *
 *      Custom class for saving Marker-Circle objects to be used in Map View.
 *      This way we can  associate Circle with the corresponding Marker so we can show the
 *      corresponding info window when the circle is clicked.
 */
public class MarkerCircle {

    private Marker marker;
    private Circle circle;

    public MarkerCircle(Marker marker, Circle circle) {
        this.marker = marker;
        this.circle = circle;
    }//constructor

    public Marker getMarker() {
        return marker;
    }

    public String getTag() {
        return (String) circle.getTag();
    }
}//MarkerCircle

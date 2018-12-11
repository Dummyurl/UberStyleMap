package dev.uberstyle.map;

import android.animation.TypeEvaluator;
import com.google.android.gms.maps.model.LatLng;

public class RouteEvaluator implements TypeEvaluator<LatLng> {
    public LatLng evaluate(float f, LatLng latLng, LatLng latLng2) {
        if (latLng == null || latLng2 == null) {
            return null;
        }
        double d = (double) f;
        return new LatLng(latLng.latitude + ((latLng2.latitude - latLng.latitude) * d), latLng.longitude + (d * (latLng2.longitude - latLng.longitude)));
    }
}

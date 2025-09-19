package com.hoaithanh.qlgh.model.goong;

import java.util.ArrayList;
import java.util.List;

public class PolylineDecoder {
    // Decode polyline kiểu Google/Goong -> list LatLng
    public static List<LatLng> decode(String encoded) {
        List<LatLng> poly = new ArrayList<>();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;

        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;

            shift = 0; result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;

            double latD = lat / 1E5;
            double lngD = lng / 1E5;
            poly.add(new LatLng(latD, lngD));
        }
        return poly;
    }
}

package com.hoaithanh.qlgh.model.goong;

public class LatLng {
    public final double latitude;
    public final double longitude;

    public LatLng(double lat, double lng) {
        this.latitude = lat; this.longitude = lng;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }
}

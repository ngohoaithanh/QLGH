package com.hoaithanh.qlgh.model.goong;

public class PlaceDetailResponse {
    public Result result;

    public static class Result {
        public Geometry geometry;
        public String formatted_address;
        public String name;
        public String place_id;
    }
    public static class Geometry {
        public Location location;
    }
    public static class Location {
        public double lat;
        public double lng;
    }
}

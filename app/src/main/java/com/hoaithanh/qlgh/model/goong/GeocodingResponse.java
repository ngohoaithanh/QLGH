package com.hoaithanh.qlgh.model.goong;

import java.util.List;

public class GeocodingResponse {
    public List<Result> results;

    public static class Result {
        public String formatted_address;
        public Geometry geometry;
    }
    public static class Geometry {
        public Location location;
    }
    public static class Location {
        public double lat;
        public double lng;
    }
}

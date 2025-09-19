package com.hoaithanh.qlgh.model.goong;

import java.util.List;

public class DirectionResponse {
    public List<Route> routes;

    public static class Route {
        public OverviewPolyline overview_polyline; // encoded polyline
        public Leg[] legs;
    }
    public static class OverviewPolyline {
        public String points;
    }
    public static class Leg {
        public ValueText distance;
        public ValueText duration;
    }
    public static class ValueText {
        public long value;   // meters / seconds
        public String text;  // "12.3 km" / "25 mins"
    }
}

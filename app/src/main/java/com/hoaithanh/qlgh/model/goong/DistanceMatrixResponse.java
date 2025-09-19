package com.hoaithanh.qlgh.model.goong;

import java.util.List;

public class DistanceMatrixResponse {
    public List<Row> rows;

    public static class Row {
        public List<Element> elements;
    }
    public static class Element {
        public DirectionResponse.ValueText distance;
        public DirectionResponse.ValueText duration;
        public String status;
    }
}

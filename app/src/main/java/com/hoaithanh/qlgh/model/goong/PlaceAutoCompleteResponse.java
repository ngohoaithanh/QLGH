package com.hoaithanh.qlgh.model.goong;

import java.util.List;

public class PlaceAutoCompleteResponse {
    public List<Prediction> predictions;

    public static class Prediction {
        public String description; // địa chỉ hiển thị
        public String place_id;
        // có thể có các trường phụ (matched_substrings, etc.) nếu cần
    }
}

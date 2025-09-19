package com.hoaithanh.qlgh.repository;

import androidx.annotation.Nullable;

import com.hoaithanh.qlgh.api.GoongApi;
import com.hoaithanh.qlgh.api.GoongRetrofitClient;
import com.hoaithanh.qlgh.model.goong.DirectionResponse;
import com.hoaithanh.qlgh.model.goong.DistanceMatrixResponse;
import com.hoaithanh.qlgh.model.goong.GeocodingResponse;
import com.hoaithanh.qlgh.model.goong.PlaceAutoCompleteResponse;
import com.hoaithanh.qlgh.model.goong.PlaceDetailResponse;

import retrofit2.Call;

public class GoongRepository {
    private final GoongApi api;

    public GoongRepository() {
        api = GoongRetrofitClient.getInstance().create(GoongApi.class);
    }

    // Lấy chỉ đường (Directions API)
    public Call<DirectionResponse> getRoute(String originLatLng, String destLatLng,
                                            @Nullable String vehicle, String apiKey) {
        if (vehicle == null || vehicle.isEmpty()) vehicle = "car";
        return api.getDirection(originLatLng, destLatLng, vehicle, apiKey);
    }

    // Tính khoảng cách & thời gian (Distance Matrix API)
    public Call<DistanceMatrixResponse> getMatrix(String origins, String destinations,
                                                  @Nullable String vehicle, String apiKey) {
        if (vehicle == null || vehicle.isEmpty()) vehicle = "car";
        return api.getDistanceMatrix(origins, destinations, vehicle, apiKey);
    }

    // Geocoding: từ địa chỉ -> lat/lng
    public Call<GeocodingResponse> geocode(String address, String apiKey) {
        return api.geocode(address, apiKey);
    }

    // Reverse Geocoding: từ lat/lng -> địa chỉ
    public Call<GeocodingResponse> reverse(String latlng, String apiKey) {
        return api.reverseGeocode(latlng, apiKey);
    }

    // Autocomplete: gợi ý địa chỉ khi gõ
    public Call<PlaceAutoCompleteResponse> autoComplete(String input,
                                                        Integer limit,
                                                        String location,
                                                        Integer radius,
                                                        String apiKey) {
        return api.autoComplete(input, apiKey, limit, location, radius);
    }

    // Place Detail: từ place_id -> chi tiết (formatted_address, geometry.lat/lng)
    public Call<PlaceDetailResponse> placeDetail(String placeId, String apiKey) {
        return api.placeDetail(placeId, apiKey);
    }
}

package com.hoaithanh.qlgh.api;

import com.hoaithanh.qlgh.model.goong.DirectionResponse;
import com.hoaithanh.qlgh.model.goong.DistanceMatrixResponse;
import com.hoaithanh.qlgh.model.goong.GeocodingResponse;
import com.hoaithanh.qlgh.model.goong.PlaceAutoCompleteResponse;
import com.hoaithanh.qlgh.model.goong.PlaceDetailResponse;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface GoongApi {

    // Directions: origin=lat,lng  destination=lat,lng  vehicle=car|bike
    // Ví dụ: /Direction?origin=10.77,106.69&destination=10.80,106.65&vehicle=car&api_key=KEY
    @GET("Direction")
    Call<DirectionResponse> getDirection(
            @Query("origin") String originLatLng,
            @Query("destination") String destLatLng,
            @Query("vehicle") String vehicle,
            @Query("api_key") String apiKey
    );

    // Distance Matrix: origins/destinations có thể nhiều điểm, cách nhau bằng |
    // Ví dụ: /DistanceMatrix?origins=10.77,106.69|10.78,106.70&destinations=...&vehicle=car&api_key=KEY
    @GET("DistanceMatrix")
    Call<DistanceMatrixResponse> getDistanceMatrix(
            @Query("origins") String origins,
            @Query("destinations") String destinations,
            @Query("vehicle") String vehicle,
            @Query("api_key") String apiKey
    );

    // Geocoding: text -> lat/lng
    // Ví dụ: /Geocode?address=285+CMT8%2C+Q10&api_key=KEY
    @GET("Geocode")
    Call<GeocodingResponse> geocode(
            @Query("address") String address,
            @Query("api_key") String apiKey
    );

    // Reverse Geocoding: lat/lng -> địa chỉ
    // Ví dụ: /Geocode?latlng=10.77,106.69&api_key=KEY
    @GET("Geocode")
    Call<GeocodingResponse> reverseGeocode(
            @Query("latlng") String latlng,   // "lat,lng"
            @Query("api_key") String apiKey
    );

    // Autocomplete: gợi ý địa chỉ khi gõ
    // Ví dụ: /Place/AutoComplete?input=285+cmt8&limit=5&location=10.77,106.69&radius=30000&api_key=KEY
    @GET("Place/AutoComplete")
    Call<PlaceAutoCompleteResponse> autoComplete(
            @Query("input") String input,
            @Query("api_key") String apiKey,
            @Query("limit") Integer limit,          // optional
            @Query("location") String location,     // optional "lat,lng"
            @Query("radius") Integer radius         // optional (m)
    );

    // Place Detail: từ place_id -> formatted_address + geometry.lat/lng
    // Ví dụ: /Place/Detail?place_id=xyz&api_key=KEY
    @GET("Place/Detail")
    Call<PlaceDetailResponse> placeDetail(
            @Query("place_id") String placeId,
            @Query("api_key") String apiKey
    );
}

package com.hoaithanh.qlgh.database;

import androidx.room.TypeConverter;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.hoaithanh.qlgh.model.Tracking;
import com.hoaithanh.qlgh.model.Vehicle;

import java.lang.reflect.Type;
import java.util.Date;
import java.util.List;

public class Converters {
    @TypeConverter
    public static Date fromTimestamp(Long value) {
        return value == null ? null : new Date(value);
    }

    @TypeConverter
    public static Long dateToTimestamp(Date date) {
        return date == null ? null : date.getTime();
    }

    private static final Gson gson = new Gson();

    // --- Bộ chuyển đổi cho đối tượng Vehicle ---
    @TypeConverter
    public static String fromVehicleToString(Vehicle vehicle) {
        return gson.toJson(vehicle);
    }

    @TypeConverter
    public static Vehicle fromStringToVehicle(String json) {
        return gson.fromJson(json, Vehicle.class);
    }

    // --- Bộ chuyển đổi cho danh sách List<Tracking> ---
    @TypeConverter
    public static String fromTrackingListToString(List<Tracking> trackingList) {
        return gson.toJson(trackingList);
    }

    @TypeConverter
    public static List<Tracking> fromStringToTrackingList(String json) {
        Type listType = new TypeToken<List<Tracking>>() {}.getType();
        return gson.fromJson(json, listType);
    }
}

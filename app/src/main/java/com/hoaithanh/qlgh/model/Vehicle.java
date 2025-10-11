package com.hoaithanh.qlgh.model;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class Vehicle implements Serializable {
    @SerializedName("license_plate")
    private String licensePlate;
    @SerializedName("model")
    private String model;
    // Thêm các hàm Getters

    public String getLicensePlate() {
        return licensePlate;
    }

    public void setLicensePlate(String licensePlate) {
        this.licensePlate = licensePlate;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }
}

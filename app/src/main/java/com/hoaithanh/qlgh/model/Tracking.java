package com.hoaithanh.qlgh.model;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class Tracking implements Serializable {
    @SerializedName("Status")
    private String status;
    @SerializedName("Updated_at")
    private String updatedAt;
    // Thêm các hàm Getters

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }
}

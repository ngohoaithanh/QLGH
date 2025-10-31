package com.hoaithanh.qlgh.model;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class UserCheckResponse implements Serializable {
    @SerializedName("exists")
    private boolean exists;
    public boolean isExists() { return exists; }
}

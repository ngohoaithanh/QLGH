package com.hoaithanh.qlgh.model;
import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

public class Notification implements Serializable {
    @SerializedName("ID") private int id;
    @SerializedName("Title") private String title;
    @SerializedName("Message") private String message;
    @SerializedName("Type") private String type;
    @SerializedName("ReferenceID") private int referenceId;
    @SerializedName("Created_at") private String createdAt;

    // Getters
    public String getTitle() { return title; }
    public String getMessage() { return message; }
    public String getCreatedAt() { return createdAt; }
    public int getReferenceId() { return referenceId; }
}
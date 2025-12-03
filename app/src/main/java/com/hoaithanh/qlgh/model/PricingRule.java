package com.hoaithanh.qlgh.model;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

public class PricingRule implements Serializable {
    @SerializedName("BaseDistance")
    public double baseDistance;

    @SerializedName("BasePrice")
    public double basePrice;

    @SerializedName("PricePerKm")
    public double pricePerKm;

    @SerializedName("PricePerKg")
    public double pricePerKg;

    // --- THÊM TRƯỜNG MỚI ---
    @SerializedName("FreeWeight")
    public double freeWeight;
}
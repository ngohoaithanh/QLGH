package com.hoaithanh.qlgh.model;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

public class ShipperBalanceResponse implements Serializable {

    @SerializedName("net_income")
    private double netIncome;

    @SerializedName("fee_in_limit")
    private double feeInLimit; // Phí COD trong hạn

    @SerializedName("fee_overdue")
    private double feeOverdue; // Phí COD quá hạn

    @SerializedName("error_code")
    private String errorCode;

    public String getErrorCode() { return errorCode; }

    // --- Getters ---
    public double getNetIncome() {
        return netIncome;
    }


    public double getFeeInLimit() {
        return feeInLimit;
    }

    public double getFeeOverdue() {
        return feeOverdue;
    }
}
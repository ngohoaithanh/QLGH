package com.hoaithanh.qlgh.model;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

public class ShipperBalanceResponse implements Serializable {

//    @SerializedName("current_balance")
//    private double currentBalance;
//
//    @SerializedName("cod_held")
//    private double codHeld; // Số tiền COD shipper đang giữ
//
//    // --- Getters ---
//    public double getCurrentBalance() {
//        return currentBalance;
//    }
//
//    public double getCodHeld() {
//        return codHeld;
//    }

    @SerializedName("net_income") // <-- Đổi tên cho khớp API
    private double netIncome;

    @SerializedName("service_fee_held") // <-- Đổi tên cho khớp API
    private double serviceFeeHeld;

    // --- Getters ---
    public double getNetIncome() {
        return netIncome;
    }

    public double getServiceFeeHeld() {
        return serviceFeeHeld;
    }
}
package com.hoaithanh.qlgh.model;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

public class Transaction implements Serializable {

    // Các trường dữ liệu lấy từ API
    @SerializedName("ID")
    private int id;

    @SerializedName("OrderID")
    private Integer orderId; // Dùng Integer để có thể null

    @SerializedName("UserID")
    private int userId; // ID của shipper

    @SerializedName("Type")
    private String type; // Ví dụ: "shipping_fee", "collect_cod", "deposit_cod", "withdraw"

    @SerializedName("Amount")
    private double amount; // Số tiền

    @SerializedName("Status")
    private String status; // Ví dụ: "completed", "pending"

    @SerializedName("Note")
    private String note; // Ghi chú (nếu có)

    @SerializedName("Created_at")
    private String createdAt; // Thời gian giao dịch

    // --- Getters ---
    // (Bạn nên tự động tạo các hàm get bằng cách: Alt + Insert -> Getters)

    public int getId() {
        return id;
    }

    public Integer getOrderId() {
        return orderId;
    }

    public int getUserId() {
        return userId;
    }

    public String getType() {
        return type;
    }

    public double getAmount() {
        return amount;
    }

    public String getStatus() {
        return status;
    }

    public String getNote() {
        return note;
    }

    public String getCreatedAt() {
        return createdAt;
    }
}
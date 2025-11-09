package com.hoaithanh.qlgh.model;

import com.google.gson.annotations.SerializedName;

public class SimpleResult {
    @SerializedName("success")
    private boolean success;

//    @SerializedName("message")
//    private String message;
    @SerializedName(value = "message", alternate = {"error"})
    private String message;

    @SerializedName("error_code")
    private String errorCode;

    public String getErrorCode() {
        return errorCode;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    // Các hàm set (để tạo response lỗi thủ công nếu cần)
    public void setSuccess(boolean success) {
        this.success = success;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
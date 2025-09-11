package com.hoaithanh.qlgh.model;

public class ApiResult {
    public boolean success;
    public String message;
    public String error;

    // Getter & Setter (nếu bạn cần cho gọn gàng hơn)
    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public String getError() {
        return error;
    }
}

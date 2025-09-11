package com.hoaithanh.qlgh.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class ApiResponse<T> {
    @SerializedName("success")
    private boolean success;

    @SerializedName("data")
    private T data;

    @SerializedName("message")
    private String message;

    @SerializedName("pagination")
    private Pagination pagination;

    @SerializedName("timestamp")
    private long timestamp;

    // Getters and Setters
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public T getData() { return data; }
    public void setData(T data) { this.data = data; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Pagination getPagination() { return pagination; }
    public void setPagination(Pagination pagination) { this.pagination = pagination; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    // Pagination class
    public static class Pagination {
        @SerializedName("current_page")
        private int currentPage;

        @SerializedName("per_page")
        private int perPage;

        @SerializedName("total")
        private int total;

        @SerializedName("total_pages")
        private int totalPages;

        public int getCurrentPage() { return currentPage; }
        public void setCurrentPage(int currentPage) { this.currentPage = currentPage; }

        public int getPerPage() { return perPage; }
        public void setPerPage(int perPage) { this.perPage = perPage; }

        public int getTotal() { return total; }
        public void setTotal(int total) { this.total = total; }

        public int getTotalPages() { return totalPages; }
        public void setTotalPages(int totalPages) { this.totalPages = totalPages; }
    }
}
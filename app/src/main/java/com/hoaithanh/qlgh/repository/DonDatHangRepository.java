package com.hoaithanh.qlgh.repository;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.hoaithanh.qlgh.api.ApiService;
import com.hoaithanh.qlgh.api.RetrofitClient;
import com.hoaithanh.qlgh.model.DonDatHang;
import com.hoaithanh.qlgh.model.SimpleResult;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;



public class DonDatHangRepository {
    private final ApiService apiService;

    public DonDatHangRepository() {
        apiService = RetrofitClient.getApi();
    }
    public interface DonDatHangCallback {
        void onSuccess(List<DonDatHang> donDatHangList);
        void onError(String errorMessage);
    }

    public void getAllDonDatHang(final DonDatHangCallback callback) {
        ApiService apiService = RetrofitClient.getApi();
        Call<List<DonDatHang>> call = apiService.getDonDatHang();

        call.enqueue(new Callback<List<DonDatHang>>() {
            @Override
            public void onResponse(Call<List<DonDatHang>> call, Response<List<DonDatHang>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onError("Failed to get orders: " + response.message());
                }
            }

            @Override
            public void onFailure(Call<List<DonDatHang>> call, Throwable t) {
                callback.onError("Network error: " + t.getMessage());
            }
        });
    }

    // =============================================================
    // ## PHẦN MỚI THÊM VÀO ĐỂ CẬP NHẬT TRẠNG THÁI ##
    // =============================================================

    // 1. Tạo một interface callback mới cho hành động cập nhật
    public interface UpdateStatusCallback {
        void onUpdateSuccess(String message);
        void onUpdateError(String errorMessage);
    }

    // 2. Tạo hàm mới để gọi API cập nhật
    public void updateOrderStatus(int orderId, String newStatus, final UpdateStatusCallback callback) {
        apiService.updateOrderStatus(orderId, newStatus).enqueue(new Callback<SimpleResult>() {
            @Override
            public void onResponse(@NonNull Call<SimpleResult> call, @NonNull Response<SimpleResult> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    callback.onUpdateSuccess(response.body().getMessage());
                } else {
                    // Lỗi từ server hoặc response.body() báo thất bại
                    String errorMessage = "Failed to update status.";
                    if (response.body() != null) {
                        errorMessage = response.body().getMessage();
                    } else if (!response.isSuccessful()) {
                        errorMessage = "Server error: " + response.code();
                    }
                    callback.onUpdateError(errorMessage);
                }
            }

            @Override
            public void onFailure(@NonNull Call<SimpleResult> call, @NonNull Throwable t) {
                callback.onUpdateError("Network error: " + t.getMessage());
            }
        });
    }

    public interface OrderDetailCallback {
        void onSuccess(DonDatHang order);
        void onError(String errorMessage);
    }

    // 2. SỬA LẠI HÀM getOrderById
    public void getOrderById(int orderId, final OrderDetailCallback callback) {
        apiService.getOrderById(orderId).enqueue(new Callback<DonDatHang>() {
            @Override
            public void onResponse(Call<DonDatHang> call, Response<DonDatHang> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onError("Không tìm thấy đơn hàng hoặc có lỗi từ server.");
                }
            }
            @Override
            public void onFailure(Call<DonDatHang> call, Throwable t) {
                callback.onError("Lỗi mạng: " + t.getMessage());
            }
        });
    }
}
package com.hoaithanh.qlgh.repository;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.hoaithanh.qlgh.api.ApiService;
import com.hoaithanh.qlgh.api.RetrofitClient;
import com.hoaithanh.qlgh.model.DonDatHang;
import com.hoaithanh.qlgh.model.ShipperLocation;
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
    // Hàm NẠP CHỒNG MỚI: Dùng cho trạng thái thất bại (có thêm reason)
    public void updateOrderStatus(int orderId, String newStatus, String reason, final UpdateStatusCallback callback) {
        // Sử dụng hàm API mới với 3 tham số
        apiService.updateOrderStatusWithReason(orderId, newStatus, reason).enqueue(new Callback<SimpleResult>() {
            @Override
            public void onResponse(@NonNull Call<SimpleResult> call, @NonNull Response<SimpleResult> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    callback.onUpdateSuccess(response.body().getMessage());
                } else {
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

    // --- Interface và hàm cho đơn hàng của khách hàng ---
    public interface CustomerOrdersCallback {
        void onSuccess(List<DonDatHang> orders);
        void onError(String errorMessage);
    }

    public void getOrdersByCustomerId(int customerId, final CustomerOrdersCallback callback) {
        apiService.getOrdersByCustomerId(customerId).enqueue(new Callback<List<DonDatHang>>() {
            @Override
            public void onResponse(Call<List<DonDatHang>> call, Response<List<DonDatHang>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onError("Lỗi khi tải đơn hàng của khách hàng.");
                }
            }

            @Override
            public void onFailure(Call<List<DonDatHang>> call, Throwable t) {
                callback.onError("Lỗi mạng: " + t.getMessage());
            }
        });
    }

    public LiveData<ShipperLocation> getShipperLocation(int shipperId) {
        final MutableLiveData<ShipperLocation> data = new MutableLiveData<>();
        apiService.getShipperLocation(shipperId).enqueue(new Callback<ShipperLocation>() {
            @Override
            public void onResponse(Call<ShipperLocation> call, Response<ShipperLocation> response) {
                if (response.isSuccessful()) {
                    data.postValue(response.body());
                } else {
                    data.postValue(null);
                }
            }

            @Override
            public void onFailure(Call<ShipperLocation> call, Throwable t) {
                data.postValue(null);
            }
        });
        return data;
    }

    // --- Interface và hàm mới để gửi đánh giá ---
    public interface SubmitRatingCallback {
        void onSubmitSuccess(String message);
        void onSubmitError(String errorMessage);
    }

    public void submitShipperRating(int shipperId, int orderId, float rating, final SubmitRatingCallback callback) {
        apiService.submitShipperRating(shipperId, orderId, rating).enqueue(new Callback<SimpleResult>() { // <-- TRUYỀN orderId VÀO ĐÂY
            @Override
            public void onResponse(Call<SimpleResult> call, Response<SimpleResult> response) {
                if (response.body() != null) {
                    Log.d("RATING_DEBUG", "response.body().isSuccess(): " + response.body().isSuccess());
                }
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    callback.onSubmitSuccess(response.body().getMessage());
                } else {
                    String error = "Gửi đánh giá thất bại.";
                    if (response.body() != null) {
                        error = response.body().getMessage();
                    }
                    callback.onSubmitError(error);
                }
            }

            @Override
            public void onFailure(Call<SimpleResult> call, Throwable t) {
                callback.onSubmitError("Lỗi mạng: " + t.getMessage());
            }
        });
    }

    public interface CancelOrderCallback {
        void onCancelSuccess(String message);
        void onCancelError(String errorMessage);
    }

    // 2. Tạo hàm mới
    public void cancelOrder(int orderId, final CancelOrderCallback callback) {
        apiService.cancelOrder(orderId).enqueue(new Callback<SimpleResult>() {
            @Override
            public void onResponse(Call<SimpleResult> call, Response<SimpleResult> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    callback.onCancelSuccess(response.body().getMessage());
                } else {
                    String error = "Hủy đơn thất bại.";
                    if (response.body() != null) {
                        error = response.body().getMessage();
                    }
                    callback.onCancelError(error);
                }
            }
            @Override
            public void onFailure(Call<SimpleResult> call, Throwable t) {
                callback.onCancelError("Lỗi mạng: " + t.getMessage());
            }
        });
    }
}
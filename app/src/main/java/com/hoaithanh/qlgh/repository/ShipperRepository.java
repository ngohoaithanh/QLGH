package com.hoaithanh.qlgh.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.hoaithanh.qlgh.api.ApiService;
import com.hoaithanh.qlgh.api.RetrofitClient;
import com.hoaithanh.qlgh.model.ShipperBalanceResponse; // Import model mới
import com.hoaithanh.qlgh.model.Transaction;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ShipperRepository {

    private ApiService apiService;

    public ShipperRepository() {
        apiService = RetrofitClient.getApi(); // Lấy instance của ApiService
    }

    public interface TransactionsCallback {
        void onSuccess(List<Transaction> transactions);
        void onError(String errorMessage);
    }

    // Hàm lấy số dư
    public LiveData<ShipperBalanceResponse> getShipperBalance(int shipperId) {
        final MutableLiveData<ShipperBalanceResponse> data = new MutableLiveData<>();
        apiService.getShipperBalance(shipperId).enqueue(new Callback<ShipperBalanceResponse>() {
            @Override
            public void onResponse(Call<ShipperBalanceResponse> call, Response<ShipperBalanceResponse> response) {
                data.postValue(response.isSuccessful() ? response.body() : null);
            }
            @Override
            public void onFailure(Call<ShipperBalanceResponse> call, Throwable t) {
                data.postValue(null);
            }
        });
        return data;
    }

    // Hàm lấy lịch sử giao dịch
    // Thay thế hàm cũ bằng hàm này
    public void getShipperTransactions(int shipperId, String startDate, String endDate, final TransactionsCallback callback) {
        apiService.getShipperTransactions(shipperId, startDate, endDate).enqueue(new Callback<List<Transaction>>() {
            @Override
            public void onResponse(Call<List<Transaction>> call, Response<List<Transaction>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onError("Không thể tải lịch sử giao dịch.");
                }
            }
            @Override
            public void onFailure(Call<List<Transaction>> call, Throwable t) {
                callback.onError("Lỗi mạng: " + t.getMessage());
            }
        });
    }
}
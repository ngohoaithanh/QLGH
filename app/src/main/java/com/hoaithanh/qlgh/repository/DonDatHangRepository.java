package com.hoaithanh.qlgh.repository;

import com.hoaithanh.qlgh.api.ApiService;
import com.hoaithanh.qlgh.api.RetrofitClient;
import com.hoaithanh.qlgh.model.DonDatHang;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;



public class DonDatHangRepository {
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
}
package com.hoaithanh.qlgh.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.hoaithanh.qlgh.api.ApiService;
import com.hoaithanh.qlgh.api.RetrofitClient;
import com.hoaithanh.qlgh.model.Vehicle;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class UserRepository {
    private ApiService apiService;

    public UserRepository() {
        apiService = RetrofitClient.getApi();
    }

    public LiveData<Vehicle> getVehicleInfo(int shipperId) {
        MutableLiveData<Vehicle> data = new MutableLiveData<>();
        apiService.getVehicleInfo(shipperId).enqueue(new Callback<Vehicle>() {
            @Override
            public void onResponse(Call<Vehicle> call, Response<Vehicle> response) {
                data.postValue(response.isSuccessful() ? response.body() : null);
            }
            @Override
            public void onFailure(Call<Vehicle> call, Throwable t) {
                data.postValue(null);
            }
        });
        return data;
    }
}
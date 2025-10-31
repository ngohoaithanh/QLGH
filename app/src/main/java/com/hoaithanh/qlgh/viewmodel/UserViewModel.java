package com.hoaithanh.qlgh.viewmodel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.hoaithanh.qlgh.model.Vehicle;
import com.hoaithanh.qlgh.repository.UserRepository;

public class UserViewModel extends ViewModel {
    private UserRepository repository;
    private LiveData<Vehicle> vehicleInfo;

    public UserViewModel() {
        repository = new UserRepository();
    }

    public LiveData<Vehicle> getVehicleInfo(int shipperId) {
        if (vehicleInfo == null) {
            vehicleInfo = repository.getVehicleInfo(shipperId);
        }
        return vehicleInfo;
    }
}
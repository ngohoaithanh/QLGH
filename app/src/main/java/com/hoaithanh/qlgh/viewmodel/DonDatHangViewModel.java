package com.hoaithanh.qlgh.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;


import com.hoaithanh.qlgh.model.DonDatHang;
import com.hoaithanh.qlgh.repository.DonDatHangRepository;

import java.util.List;

public class DonDatHangViewModel extends ViewModel {
    private MutableLiveData<List<DonDatHang>> donDatHangLiveData = new MutableLiveData<>();
    private MutableLiveData<String> errorLiveData = new MutableLiveData<>();
    private MutableLiveData<Boolean> loadingLiveData = new MutableLiveData<>();

    private DonDatHangRepository repository = new DonDatHangRepository();

    public LiveData<List<DonDatHang>> getDonDatHangList() {
        return donDatHangLiveData;
    }

    public LiveData<String> getError() {
        return errorLiveData;
    }

    public LiveData<Boolean> getLoading() {
        return loadingLiveData;
    }

    public void loadDonDatHang() {
        loadingLiveData.setValue(true);

        repository.getAllDonDatHang(new DonDatHangRepository.DonDatHangCallback() {
            @Override
            public void onSuccess(List<DonDatHang> donDatHangList) {
                loadingLiveData.postValue(false);
                donDatHangLiveData.postValue(donDatHangList);
                if (donDatHangList == null || donDatHangList.isEmpty()) {
                    errorLiveData.postValue("Không có đơn hàng nào");
                }
            }

            @Override
            public void onError(String errorMessage) {
                loadingLiveData.postValue(false);
                errorLiveData.postValue(errorMessage);
                donDatHangLiveData.postValue(null);
            }
        });
    }
}
package com.hoaithanh.qlgh.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.hoaithanh.qlgh.model.ShipperBalanceResponse;
import com.hoaithanh.qlgh.model.Transaction;
import com.hoaithanh.qlgh.repository.ShipperRepository; // Đảm bảo import đúng

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class ShipperEarningsViewModel extends ViewModel {

    private ShipperRepository repository;
    private MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private MutableLiveData<String> error = new MutableLiveData<>();

    // LiveData cho số dư (giữ nguyên cách dùng LiveData)
    private LiveData<ShipperBalanceResponse> balanceData;

    // LiveData cho danh sách giao dịch (dùng MutableLiveData)
    private final MutableLiveData<List<Transaction>> transactionsData = new MutableLiveData<>();
    private final MediatorLiveData<Double> periodEarnings = new MediatorLiveData<>();

    public ShipperEarningsViewModel() {
        repository = new ShipperRepository();
        // Không cần khởi tạo transactionsData với switchMap nữa
        periodEarnings.addSource(transactionsData, transactions -> {
            calculatePeriodEarnings(transactions);
        });
    }

    private void calculatePeriodEarnings(List<Transaction> transactions) {
        double earnings = 0;
        if (transactions != null) {
            for (Transaction t : transactions) {
                if ("shipping_fee".equals(t.getType()) || "bonus".equals(t.getType())) {
                    earnings += t.getAmount();
                }
                // (Tùy chọn) Trừ đi các khoản phạt nếu có
                // else if ("penalty".equals(t.getType())) {
                //     earnings -= t.getAmount();
                // }
            }
        }
        periodEarnings.postValue(earnings);
    }

    public LiveData<Double> getPeriodEarnings() {
        return periodEarnings;
    }

    // --- Getters cho Activity observe ---
    public LiveData<ShipperBalanceResponse> getBalanceData(int shipperId) {
        if (balanceData == null) {
            balanceData = repository.getShipperBalance(shipperId);
        }
        return balanceData;
    }

    public LiveData<List<Transaction>> getTransactionsData() {
        return transactionsData;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public LiveData<String> getError() {
        return error;
    }

    // --- Hàm Activity gọi để tải dữ liệu giao dịch ---
    public void loadTransactionsForDateRange(int shipperId, Calendar start, Calendar end) {
        isLoading.setValue(true);
        error.setValue(null); // Xóa lỗi cũ (nếu có)

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String startDateStr = sdf.format(start.getTime());
        String endDateStr = sdf.format(end.getTime());

        repository.getShipperTransactions(shipperId, startDateStr, endDateStr, new ShipperRepository.TransactionsCallback() {
            @Override
            public void onSuccess(List<Transaction> transactions) {
                isLoading.postValue(false);
                transactionsData.postValue(transactions);
            }

            @Override
            public void onError(String errorMessage) {
                isLoading.postValue(false);
                error.postValue(errorMessage);
                transactionsData.postValue(null); // Trả về null khi có lỗi
            }
        });
    }
}
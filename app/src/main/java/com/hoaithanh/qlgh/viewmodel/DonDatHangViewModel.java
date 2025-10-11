package com.hoaithanh.qlgh.viewmodel;

import android.app.Application;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;


import com.hoaithanh.qlgh.api.ApiService;
import com.hoaithanh.qlgh.api.RetrofitClient;
import com.hoaithanh.qlgh.model.DonDatHang;
import com.hoaithanh.qlgh.model.SimpleResult;
import com.hoaithanh.qlgh.repository.DonDatHangRepository;
import com.hoaithanh.qlgh.session.SessionManager;

import java.util.List;
//import java.util.function.Function;
import androidx.arch.core.util.Function;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

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

//    ============================================
private final MutableLiveData<List<DonDatHang>> myOrders = new MutableLiveData<>();
    private boolean loadedOnce = false;

    public LiveData<List<DonDatHang>> getMyOrders() { return myOrders; }

    public void ensureFirstLoad(Context context){
        if (!loadedOnce) refreshMyOrders(context);
    }

    public void refreshMyOrders(Context context){
        SessionManager sm = new SessionManager(context);
        int shipperId = sm.getUserId();
        ApiService api = RetrofitClient.getApi();
        api.getOrdersByShipper(shipperId).enqueue(new Callback<List<DonDatHang>>() {
            @Override public void onResponse(Call<List<DonDatHang>> call, Response<List<DonDatHang>> res) {
                loadedOnce = true;
                if (res.isSuccessful()) myOrders.postValue(res.body());
            }
            @Override public void onFailure(Call<List<DonDatHang>> call, Throwable t) { }
        });
    }


//    =============================================

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

    // ## CODE MỚI THÊM VÀO ĐỂ CẬP NHẬT TRẠNG THÁI ##
    // LiveData để thông báo kết quả cập nhật về cho Activity
    private final MutableLiveData<SimpleResult> updateStatusResult = new MutableLiveData<>();

    // Activity sẽ lắng nghe (observe) LiveData này để biết kết quả
    public LiveData<SimpleResult> getUpdateStatusResult() {
        return updateStatusResult;
    }

    // Activity sẽ gọi hàm này khi người dùng nhấn nút
    public void updateOrderStatus(int orderId, String newStatus) {
        // Gọi hàm trong repository mà chúng ta đã tạo ở bước trước
        repository.updateOrderStatus(orderId, newStatus, new DonDatHangRepository.UpdateStatusCallback() {
            @Override
            public void onUpdateSuccess(String message) {
                SimpleResult result = new SimpleResult();
                result.setSuccess(true);
                result.setMessage(message);
                updateStatusResult.postValue(result); // Dùng postValue vì callback có thể chạy trên thread khác
            }

            @Override
            public void onUpdateError(String errorMessage) {
                SimpleResult result = new SimpleResult();
                result.setSuccess(false);
                result.setMessage(errorMessage);
                updateStatusResult.postValue(result);
            }
        });
    }

    // =====================================================================
    // ## PHẦN LẤY DỮ LIỆU MỚI NHẤT (VIẾT LẠI) ##
    // =====================================================================

    // 1. Dùng MutableLiveData để chứa chi tiết đơn hàng
    private final MutableLiveData<DonDatHang> orderDetails = new MutableLiveData<>();

    // 2. Cung cấp LiveData cho Activity observe
    public LiveData<DonDatHang> getOrderDetails() {
        return orderDetails;
    }

    // 3. Activity sẽ gọi hàm này
    public void loadOrderDetails(int orderId) {
        repository.getOrderById(orderId, new DonDatHangRepository.OrderDetailCallback() {
            @Override
            public void onSuccess(DonDatHang order) {
                // Khi có dữ liệu thành công, cập nhật LiveData
                orderDetails.postValue(order);
            }

            @Override
            public void onError(String errorMessage) {
                // Nếu lỗi, cập nhật LiveData với giá trị null
                // Activity sẽ dựa vào đây để hiển thị thông báo lỗi
                orderDetails.postValue(null);
            }
        });
    }

    // =====================================================================
// ## PHẦN LẤY ĐƠN HÀNG CỦA KHÁCH HÀNG ##
// =====================================================================

    // 1. Dùng MutableLiveData để chứa danh sách đơn hàng
    private final MutableLiveData<List<DonDatHang>> customerOrders = new MutableLiveData<>();

    // 2. Cung cấp LiveData cho Activity observe (hàm getCustomerOrders())
    public LiveData<List<DonDatHang>> getCustomerOrders() {
        return customerOrders;
    }

    // 3. Activity sẽ gọi hàm này để tải hoặc làm mới dữ liệu (hàm loadCustomerOrders())
    public void loadCustomerOrders(int customerId) {
        repository.getOrdersByCustomerId(customerId, new DonDatHangRepository.CustomerOrdersCallback() {
            @Override
            public void onSuccess(List<DonDatHang> orders) {
                // Khi có dữ liệu thành công, cập nhật LiveData
                customerOrders.postValue(orders);
            }

            @Override
            public void onError(String errorMessage) {
                // Nếu lỗi, cập nhật LiveData với giá trị null
                // Activity sẽ dựa vào đây để hiển thị thông báo lỗi
                customerOrders.postValue(null);
            }
        });
    }

}
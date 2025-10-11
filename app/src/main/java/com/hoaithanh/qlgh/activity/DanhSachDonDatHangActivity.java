package com.hoaithanh.qlgh.activity;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.hoaithanh.qlgh.R;
import com.hoaithanh.qlgh.adapter.DonDatHangAdapter;
import com.hoaithanh.qlgh.api.ApiService;
import com.hoaithanh.qlgh.api.RetrofitClient;
import com.hoaithanh.qlgh.database.DatabaseHelper;
import com.hoaithanh.qlgh.model.DonDatHang;
import com.hoaithanh.qlgh.session.SessionManager;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DanhSachDonDatHangActivity extends AppCompatActivity {

    private RecyclerView rvDonHang;
    private ProgressBar progressBar;
    private TextView tvEmpty, tvError;
    private DonDatHangAdapter donDatHangAdapter;
    private SessionManager session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_danh_sach_don_hang);
        session = new SessionManager(getApplicationContext());
        initViews();
        loadDonHangFromApi();
    }

    private void initViews() {
        rvDonHang = findViewById(R.id.rv_don_hang);
        progressBar = findViewById(R.id.progressBar);
        tvEmpty = findViewById(R.id.tvEmpty);
        tvError = findViewById(R.id.tvError);

        rvDonHang.setLayoutManager(new LinearLayoutManager(this));
        // Dùng adapter có submit(...)
        donDatHangAdapter = new DonDatHangAdapter();
        rvDonHang.setAdapter(donDatHangAdapter);
    }

    private void loadDonHangFromApi() {
        progressBar.setVisibility(View.VISIBLE);
        hideError();
        hideEmpty();


        ApiService api = RetrofitClient.getApi();
        Call<List<DonDatHang>> call;
        int role = session.getRole();
        if (role == 7) {
            // Khách hàng → gọi theo CustomerID
            int CustomerID = session.getUserId();   // đã lưu khi login
            call = api.getOrdersByCustomerId(CustomerID);
        } else {
            // Các role khác → lấy tất cả
            call = api.getDonDatHang();
        }
        call.enqueue(new Callback<List<DonDatHang>>() {
            @Override
            public void onResponse(Call<List<DonDatHang>> call, Response<List<DonDatHang>> response) {
                progressBar.setVisibility(View.GONE);

                if (response.isSuccessful() && response.body() != null) {
                    List<DonDatHang> donHangList = response.body();
                    handleDonHangList(donHangList);

                    // Nếu muốn lưu DB thì bật lại:
                    // saveToDatabase(donHangList);
                } else {
                    showError("Lỗi server: " + response.code());
                    loadDonHangFromDatabase();
                }
            }

            @Override
            public void onFailure(Call<List<DonDatHang>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                showError("Lỗi kết nối: " + t.getMessage());
                loadDonHangFromDatabase();
            }
        });
    }

    private void loadDonHangFromDatabase() {
        new Thread(() -> {
            try {
                List<DonDatHang> donHangList = DatabaseHelper.getInstance(DanhSachDonDatHangActivity.this)
                        .donDatHangDAO()
                        .getAll();

                runOnUiThread(() -> handleDonHangList(donHangList));
            } catch (Exception e) {
                runOnUiThread(() -> showError("Lỗi database: " + e.getMessage()));
            }
        }).start();
    }

    private void handleDonHangList(List<DonDatHang> donHangList) {
        if (donHangList != null && !donHangList.isEmpty()) {
            // DÙNG submit(...) để cập nhật mượt (DiffUtil)
            donDatHangAdapter.submit(donHangList);
            showDataView();

            // Debug
            for (DonDatHang donHang : donHangList) {
                Log.d("DON_HANG", "ID: " + donHang.getID() + ", Người nhận: " + donHang.getRecipient());
            }
        } else {
            showEmptyView();
        }
    }

    private void showDataView() {
        rvDonHang.setVisibility(View.VISIBLE);
        tvEmpty.setVisibility(View.GONE);
        tvError.setVisibility(View.GONE);
    }

    private void showEmptyView() {
        rvDonHang.setVisibility(View.GONE);
        tvEmpty.setVisibility(View.VISIBLE);
        tvError.setVisibility(View.GONE);
        tvEmpty.setText("Không có đơn hàng nào");
    }

    private void showError(String message) {
        rvDonHang.setVisibility(View.GONE);
        tvEmpty.setVisibility(View.GONE);
        tvError.setVisibility(View.VISIBLE);
        tvError.setText(message);
        Log.e("API_ERROR", message);
    }

    private void hideError() {
        tvError.setVisibility(View.GONE);
    }

    private void hideEmpty() {
        tvEmpty.setVisibility(View.GONE);
    }

    public void onRefreshClick(View view) {
        loadDonHangFromApi();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Nếu không cần auto refresh khi quay lại màn hình, có thể bỏ dòng dưới để tránh gọi API 2 lần (onCreate + onResume)
        loadDonHangFromApi();
    }
}

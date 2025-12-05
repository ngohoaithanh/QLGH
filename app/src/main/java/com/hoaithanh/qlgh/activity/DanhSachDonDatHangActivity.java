package com.hoaithanh.qlgh.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.tabs.TabLayout;
import com.hoaithanh.qlgh.R;
import com.hoaithanh.qlgh.adapter.DonDatHangAdapter;
import com.hoaithanh.qlgh.base.BaseActivity;
import com.hoaithanh.qlgh.model.DonDatHang;
import com.hoaithanh.qlgh.session.SessionManager;
import com.hoaithanh.qlgh.viewmodel.DonDatHangViewModel;

import java.util.ArrayList;
import java.util.List;

public class DanhSachDonDatHangActivity extends BaseActivity {

    private RecyclerView rvDonHang;
    private ProgressBar progressBar;
    private TextView tvEmpty, tvError;
    private TabLayout tabs;
    private DonDatHangAdapter donDatHangAdapter;
    private SwipeRefreshLayout swipeRefreshLayout;
    private DonDatHangViewModel viewModel;
    private SessionManager session;
    private BottomNavigationView bottomNavigationView;

    private final List<DonDatHang> masterOrderList = new ArrayList<>();
    private int currentTabPosition = 0;
    private static final int REQUEST_UPDATE_ORDER = 102;
    private boolean isFirstLoad = true;
    @Override
    public void initLayout() {
        setContentView(R.layout.activity_danh_sach_don_hang);
    }

    @Override
    public void initData() {
        viewModel = new ViewModelProvider(this).get(DonDatHangViewModel.class);
    }

    @Override
    public void initView() {

        session = new SessionManager(getApplicationContext());
//        viewModel = new ViewModelProvider(this).get(DonDatHangViewModel.class);

        MaterialToolbar tb = findViewById(R.id.toolbar);
        setSupportActionBar(tb);
        tb.setNavigationOnClickListener(v -> onBackPressed());

        rvDonHang = findViewById(R.id.rv_don_hang);
        progressBar = findViewById(R.id.progressBar);
        tvEmpty = findViewById(R.id.tvEmpty);
        tvError = findViewById(R.id.tvError);
        tabs = findViewById(R.id.tabs);
        swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout);

        bottomNavigationView = findViewById(R.id.bottom_navigation);
        setupBottomNavigation();
        rvDonHang.setLayoutManager(new LinearLayoutManager(this));

        // --- SỬ DỤNG ADAPTER VỚI LISTENER ---
        donDatHangAdapter = new DonDatHangAdapter();
        donDatHangAdapter.setOnItemClickListener(order -> {
            Intent intent = new Intent(this, ChiTietDonHangActivity.class);
            intent.putExtra("ID", order.getID()); // Gửi đúng ID với key là "ID"
            startActivityForResult(intent, REQUEST_UPDATE_ORDER);
        });
        rvDonHang.setAdapter(donDatHangAdapter);

        tabs.addTab(tabs.newTab().setText("Đang xử lý"));
        tabs.addTab(tabs.newTab().setText("Đã hoàn thành"));
        tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                currentTabPosition = tab.getPosition();
                filterAndDisplayOrders();
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });
        swipeRefreshLayout.setOnRefreshListener(() -> {
            loadData(); // Gọi loadData khi người dùng vuốt
        });

        observeViewModel();
//        loadData();
    }
    @Override
    protected void onResume() {
        super.onResume();
        // Tải lại dữ liệu mỗi khi quay lại màn hình
        loadData();
        if (bottomNavigationView != null) {
            // Thay R.id.navigation_notifications_shipper bằng ID thật trong menu của bạn
//            updateNotificationBadge(bottomNavigationView, R.id.navigation_notifications);
            startBadgePolling(bottomNavigationView, R.id.navigation_notifications);
        }
    }

    private void setupBottomNavigation() {
        bottomNavigationView.setSelectedItemId(R.id.navigation_orders);
        bottomNavigationView.setOnItemSelectedListener(new BottomNavigationView.OnItemSelectedListener() {

            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int itemId = item.getItemId();
                if (itemId == R.id.navigation_orders) {
                    // Đã ở trang chủ, không làm gì cả
                    return true;
                } else if (itemId == R.id.navigation_home) {
                    Intent intent = new Intent(DanhSachDonDatHangActivity.this, MainActivity.class);
                    startActivity(intent);
                    return false;
                } else if (itemId == R.id.navigation_notifications) {
                    Intent intent = new Intent(DanhSachDonDatHangActivity.this, NotificationActivity.class);
                    startActivity(intent);
                    return false;
                } else if (itemId == R.id.navigation_account) {
                    Intent intent = new Intent(DanhSachDonDatHangActivity.this, AccountActivity.class);
                    startActivity(intent);
                    return false;
                }
                return false;
            }
        });
    }

    private void observeViewModel() {
        viewModel.getIsLoading().observe(this, isLoading -> {
            if (isLoading) {
                // Chỉ hiển thị ProgressBar ở giữa nếu không phải là vuốt
                if (!swipeRefreshLayout.isRefreshing()) {
                    progressBar.setVisibility(View.VISIBLE);
                }
            } else {
                // Tắt cả 2 biểu tượng loading khi hết tải
                progressBar.setVisibility(View.GONE);
                swipeRefreshLayout.setRefreshing(false);
            }
        });

        viewModel.getCustomerOrders().observe(this, donHangList -> {
//            progressBar.setVisibility(View.GONE);
//            swipeRefreshLayout.setRefreshing(false);
            if (donHangList != null) {
                masterOrderList.clear();
                masterOrderList.addAll(donHangList);
                filterAndDisplayOrders();
            }
//            else {
//                showError("Không thể tải danh sách đơn hàng.");
//            }
        });
        // Lắng nghe Lỗi
        viewModel.getError().observe(this, errorMessage -> {
            if (errorMessage != null) {
                // Nếu có lỗi, hiển thị thông báo lỗi
                showError(errorMessage);
            }
        });
    }

    private void loadData() {
//        progressBar.setVisibility(View.VISIBLE);
        rvDonHang.setVisibility(View.GONE);
        tvEmpty.setVisibility(View.GONE);
        tvError.setVisibility(View.GONE);
        viewModel.loadCustomerOrders(session.getUserId());
    }

    private void filterAndDisplayOrders() {
        List<DonDatHang> filteredList = new ArrayList<>();
        boolean isCompletedTab = (currentTabPosition == 1);

        for (DonDatHang order : masterOrderList) {
            String status = order.getStatus() == null ? "" : order.getStatus().toLowerCase();
            boolean isOrderCompleted = status.equals("delivered") || status.equals("delivery_failed") || status.equals("cancelled");

            if (isCompletedTab == isOrderCompleted) {
                filteredList.add(order);
            }
        }

        if (!filteredList.isEmpty()) {
            donDatHangAdapter.submitList(filteredList);
            showDataView();
        } else {
            showEmptyView();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
//        if (requestCode == REQUEST_UPDATE_ORDER && resultCode == RESULT_OK) {
//            loadData();
//        }
    }

    // ... (các hàm showDataView, showEmptyView, showError giữ nguyên)
    private void showDataView() {
        rvDonHang.setVisibility(View.VISIBLE);
        tvEmpty.setVisibility(View.GONE);
        tvError.setVisibility(View.GONE);
    }

    private void showEmptyView() {
        rvDonHang.setVisibility(View.GONE);
        tvEmpty.setVisibility(View.VISIBLE);
        tvError.setVisibility(View.GONE);
        tvEmpty.setText(currentTabPosition == 0 ? "Không có đơn hàng nào đang xử lý" : "Không có đơn hàng nào đã hoàn thành");
    }

    private void showError(String message) {
        rvDonHang.setVisibility(View.GONE);
        tvEmpty.setVisibility(View.GONE);
        tvError.setVisibility(View.VISIBLE);
        tvError.setText(message);
    }

}
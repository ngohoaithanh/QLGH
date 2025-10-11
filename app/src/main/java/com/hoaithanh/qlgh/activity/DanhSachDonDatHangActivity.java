package com.hoaithanh.qlgh.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.tabs.TabLayout;
import com.hoaithanh.qlgh.R;
import com.hoaithanh.qlgh.adapter.DonDatHangAdapter;
import com.hoaithanh.qlgh.model.DonDatHang;
import com.hoaithanh.qlgh.session.SessionManager;
import com.hoaithanh.qlgh.viewmodel.DonDatHangViewModel;

import java.util.ArrayList;
import java.util.List;

public class DanhSachDonDatHangActivity extends AppCompatActivity {

    private RecyclerView rvDonHang;
    private ProgressBar progressBar;
    private TextView tvEmpty, tvError;
    private TabLayout tabs;
    private DonDatHangAdapter donDatHangAdapter;

    private DonDatHangViewModel viewModel;
    private SessionManager session;

    private final List<DonDatHang> masterOrderList = new ArrayList<>();
    private int currentTabPosition = 0;
    private static final int REQUEST_UPDATE_ORDER = 102;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_danh_sach_don_hang);

        session = new SessionManager(getApplicationContext());
        viewModel = new ViewModelProvider(this).get(DonDatHangViewModel.class);

        initViews();
        observeViewModel();
        loadData();
    }

    private void initViews() {
        MaterialToolbar tb = findViewById(R.id.toolbar);
        setSupportActionBar(tb);
        tb.setNavigationOnClickListener(v -> onBackPressed());

        rvDonHang = findViewById(R.id.rv_don_hang);
        progressBar = findViewById(R.id.progressBar);
        tvEmpty = findViewById(R.id.tvEmpty);
        tvError = findViewById(R.id.tvError);
        tabs = findViewById(R.id.tabs);

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
    }

    private void observeViewModel() {
        viewModel.getCustomerOrders().observe(this, donHangList -> {
            progressBar.setVisibility(View.GONE);
            if (donHangList != null) {
                masterOrderList.clear();
                masterOrderList.addAll(donHangList);
                filterAndDisplayOrders();
            } else {
                showError("Không thể tải danh sách đơn hàng.");
            }
        });
    }

    private void loadData() {
        progressBar.setVisibility(View.VISIBLE);
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
        if (requestCode == REQUEST_UPDATE_ORDER && resultCode == RESULT_OK) {
            loadData();
        }
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
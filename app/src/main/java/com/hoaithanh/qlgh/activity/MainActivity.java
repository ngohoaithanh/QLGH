package com.hoaithanh.qlgh.activity;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

import com.hoaithanh.qlgh.R;
import com.hoaithanh.qlgh.adapter.ServiceAdapter;
import com.hoaithanh.qlgh.base.BaseActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.hoaithanh.qlgh.model.ServiceItem;
import com.hoaithanh.qlgh.session.SessionManager;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends BaseActivity {

    private TextView tvGreeting;
    private ImageView ivAccountSettings;
    private RecyclerView rvServices;
    private BottomNavigationView bottomNavigationView;

    private SharedPreferences prefs;

    @Override
    public void initLayout() {
        setContentView(R.layout.activity_main);
    }

    @Override
    public void initData() {
        prefs = getSharedPreferences("QLGH_PREFS", Context.MODE_PRIVATE);
    }

    @Override
    public void initView() {
        tvGreeting = findViewById(R.id.tv_greeting_message); // Cần đặt ID cho TextView này trong XML
//        ivAccountSettings = findViewById(R.id.iv_account_settings);
        rvServices = findViewById(R.id.rv_services);
        bottomNavigationView = findViewById(R.id.bottom_navigation);

        // Lấy username từ SharedPreferences
//        String customerName = prefs.getString("username", "Khách hàng");
        SessionManager session = new SessionManager(this);
        String customerName = session.getUsername();
        if (customerName == null || customerName.isEmpty()) {
            customerName = "Khách hàng";
        }
        tvGreeting.setText("Xin chào, " + customerName + "!");

        // Gán vào TextView
//        tvGreeting.setText("Xin chào, " + customerName + "!");

        // Khởi tạo và thiết lập RecyclerView
        setupRecyclerView();
        setupBottomNavigation();
    }

    private void setupRecyclerView() {
        // Tạo danh sách các dịch vụ
        List<ServiceItem> serviceList = new ArrayList<>();
        // ServiceItem(tên, mô tả, icon)
        serviceList.add(new ServiceItem("Giao hàng", "Đặt đơn siêu tốc", R.drawable.ic_service));
//        serviceList.add(new ServiceItem("Đặt đồ ăn", "Hàng ngàn món ngon", R.drawable.ic_food_delivery)); // Giả sử bạn có icon này
//        serviceList.add(new ServiceItem("Đi chợ hộ", "Mua sắm tiện lợi", R.drawable.ic_shopping_bag)); // Giả sử bạn có icon này

        // Thiết lập layout manager cho RecyclerView
        GridLayoutManager gridLayoutManager = new GridLayoutManager(this, 2);
        rvServices.setLayoutManager(gridLayoutManager);

        // Thiết lập adapter
        ServiceAdapter adapter = new ServiceAdapter(serviceList, new ServiceAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(ServiceItem item) {
                // Xử lý khi nhấn vào một dịch vụ
                if (item.getName().equals("Giao hàng")) {
                    // Chuyển sang màn hình đặt đơn hàng
                    Intent intent = new Intent(MainActivity.this, DonDatHangActivity.class);
                    startActivity(intent);
                } else {
                    Toast.makeText(MainActivity.this, "Bạn đã chọn: " + item.getName(), Toast.LENGTH_SHORT).show();
                }
            }
        });
        rvServices.setAdapter(adapter);
    }

    private void setupBottomNavigation() {
        bottomNavigationView.setOnItemSelectedListener(new BottomNavigationView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int itemId = item.getItemId();
                if (itemId == R.id.navigation_home) {
                    // Đã ở trang chủ, không làm gì cả
                    return true;
                } else if (itemId == R.id.navigation_orders) {
                     Intent intent = new Intent(MainActivity.this, DanhSachDonDatHangActivity.class);
                     startActivity(intent);
                    return false;
                } else if (itemId == R.id.navigation_notifications) {
                     Intent intent = new Intent(MainActivity.this, NotificationActivity.class);
                     startActivity(intent);
                    return false;
                } else if (itemId == R.id.navigation_account) {
                     Intent intent = new Intent(MainActivity.this, AccountActivity.class);
                     startActivity(intent);
                    return false;
                }
                return false;
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Cập nhật badge mỗi khi màn hình hiện lên
        if (bottomNavigationView != null) {
            // Thay R.id.navigation_notifications_shipper bằng ID thật trong menu của bạn
            updateNotificationBadge(bottomNavigationView, R.id.navigation_notifications);
        }
    }
}
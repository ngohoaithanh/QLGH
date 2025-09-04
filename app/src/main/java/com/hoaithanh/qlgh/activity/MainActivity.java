package com.hoaithanh.qlgh.activity;

import androidx.appcompat.app.AppCompatActivity;

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

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends BaseActivity {

    private TextView tvGreeting;
    private ImageView ivAccountSettings;
    private RecyclerView rvServices;
    private BottomNavigationView bottomNavigationView;

    @Override
    public void initLayout() {
        setContentView(R.layout.activity_main);
    }

    @Override
    public void initData() {

    }

    @Override
    public void initView() {
        tvGreeting = findViewById(R.id.tv_greeting_message); // Cần đặt ID cho TextView này trong XML
        ivAccountSettings = findViewById(R.id.iv_account_settings);
        rvServices = findViewById(R.id.rv_services);
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        // Giả lập dữ liệu người dùng
        String customerName = "Hoài Thanh";
        tvGreeting.setText("Xin chào, " + customerName + "!");
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
                    Toast.makeText(MainActivity.this, "Mở trang đơn hàng", Toast.LENGTH_SHORT).show();
                     Intent intent = new Intent(MainActivity.this, DanhSachDonHangActivity.class);
                     startActivity(intent);
                    return true;
                } else if (itemId == R.id.navigation_notifications) {
                    Toast.makeText(MainActivity.this, "Mở trang thông báo", Toast.LENGTH_SHORT).show();
                    // Intent intent = new Intent(MainActivity.this, NotificationsActivity.class);
                    // startActivity(intent);
                    return true;
                } else if (itemId == R.id.navigation_account) {
                    Toast.makeText(MainActivity.this, "Mở trang tài khoản", Toast.LENGTH_SHORT).show();
                    // Intent intent = new Intent(MainActivity.this, AccountActivity.class);
                    // startActivity(intent);
                    return true;
                }
                return false;
            }
        });
    }

}
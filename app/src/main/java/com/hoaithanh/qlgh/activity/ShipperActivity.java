package com.hoaithanh.qlgh.activity;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable; // Cần import Nullable
import androidx.core.app.ActivityCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;
import com.hoaithanh.qlgh.R;
import com.hoaithanh.qlgh.base.BaseActivity; // <-- SỬA IMPORT
import com.hoaithanh.qlgh.fragment.ShipperListFragment;
import android.Manifest;

// SỬA LẠI: KẾ THỪA TỪ BASEACTIVITY
public class ShipperActivity extends BaseActivity {
    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new ShipperListFragment())
                    .commit();
        }
    }

    @Override
    public void initLayout() {
        setContentView(R.layout.activity_shipper);
    }

    @Override
    public void initData() {

    }

    @Override
    public void initView() {
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        setupBottomNavigation();
    }

    private void setupBottomNavigation() {
        bottomNavigationView.setSelectedItemId(R.id.navigation_home_shipper);
        bottomNavigationView.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int itemId = item.getItemId();
                if (itemId == R.id.navigation_home_shipper) {
                    // Đã ở trang chủ, không làm gì cả
                    return true;
                } else if (itemId == R.id.navigation_orders_shipper) {
                    Intent intent = new Intent(ShipperActivity.this, ShipperMyOrdersActivity.class);
                    startActivity(intent);
                    return false;
                }else if (itemId == R.id.navigation_notifications_shipper) {
                    Intent intent = new Intent(ShipperActivity.this, NotificationActivity.class);
                    startActivity(intent);
                    return false;
                } else if (itemId == R.id.navigation_account_shipper) {
                    Intent intent = new Intent(ShipperActivity.this, AccountActivity.class);
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
//            updateNotificationBadge(bottomNavigationView, R.id.navigation_notifications_shipper);
            startBadgePolling(bottomNavigationView, R.id.navigation_notifications_shipper);
        }
    }
}
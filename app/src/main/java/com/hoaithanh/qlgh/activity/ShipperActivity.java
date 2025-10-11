package com.hoaithanh.qlgh.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;
import com.hoaithanh.qlgh.R;
import com.hoaithanh.qlgh.fragment.ShipperListFragment;

public class ShipperActivity extends AppCompatActivity {
    private BottomNavigationView bottomNavigationView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shipper);

        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new ShipperListFragment())
                    .commit();
        }
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        setupBottomNavigation();
//        bottomNavigationView.setVisibility(View.INVISIBLE);
    }

    private void setupBottomNavigation() {
        bottomNavigationView.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int itemId = item.getItemId();
                if (itemId == R.id.navigation_home_shipper) {
                    // Đã ở trang chủ, không làm gì cả
                    return true;
                } else if (itemId == R.id.navigation_orders_shipper) {
                    Toast.makeText(ShipperActivity.this, "Mở trang đơn hàng", Toast.LENGTH_SHORT).show();
                     Intent intent = new Intent(ShipperActivity.this, ShipperMyOrdersActivity.class);
                     startActivity(intent);
                    return true;
                } else if (itemId == R.id.navigation_account_shipper) {
                    Toast.makeText(ShipperActivity.this, "Mở trang tài khoản", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(ShipperActivity.this, AccountActivity.class);
                    startActivity(intent);
                    return true;
                }
                return false;
            }
        });
    }
}

package com.hoaithanh.qlgh.activity;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;

import android.os.Bundle;

import com.hoaithanh.qlgh.R;
import com.hoaithanh.qlgh.base.BaseActivity;

public class SplashActivity extends BaseActivity {
    private static final int SPLASH_DELAY = 1500;

    @Override
    public void initLayout() {
        setContentView(R.layout.activity_splash);
    }

    @Override
    public void initData() {

    }

    @Override
    public void initView() {
        new Handler().postDelayed(() -> {
            SharedPreferences prefs = getSharedPreferences("QLGH_PREFS", Context.MODE_PRIVATE);

            // Kiểm tra đăng nhập
            int userId = prefs.getInt("user_id", -1);
            if (userId != -1) {
                // Đã đăng nhập → chuyển thẳng vào MainActivity
                startActivity(new Intent(SplashActivity.this, MainActivity.class));
            } else {
                // Chưa đăng nhập → vào màn Login
                startActivity(new Intent(SplashActivity.this, LoginActivity.class));
            }
            finish(); // đóng SplashActivity
        }, SPLASH_DELAY);
    }
}
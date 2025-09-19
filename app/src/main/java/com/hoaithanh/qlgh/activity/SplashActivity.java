package com.hoaithanh.qlgh.activity;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;

import android.os.Bundle;
import android.os.Looper;

import com.hoaithanh.qlgh.R;
import com.hoaithanh.qlgh.base.BaseActivity;
import com.hoaithanh.qlgh.session.SessionManager;

public class SplashActivity extends BaseActivity {
    private static final int SPLASH_DELAY = 1200; // ms

    @Override
    public void initLayout() {
        setContentView(R.layout.activity_splash);
    }

    @Override
    public void initData() { }

    @Override
    public void initView() {
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            SessionManager session = new SessionManager(this);

            Intent next;
            if (session.isLoggedIn()) {
                int role = session.getRole(); // 6 = shipper, 1/7 = admin/customer
                if (role == 6) {
                    next = new Intent(this, ShipperActivity.class);
                } else {
                    next = new Intent(this, MainActivity.class);
                }
            } else {
                next = new Intent(this, LoginActivity.class);
            }

            // Dọn back stack để không quay lại Splash/Login
            next.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(next);
            finish();
        }, SPLASH_DELAY);
    }
}
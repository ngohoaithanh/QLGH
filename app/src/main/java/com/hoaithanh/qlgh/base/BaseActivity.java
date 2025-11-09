package com.hoaithanh.qlgh.base;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.hoaithanh.qlgh.activity.LoginActivity;
import com.hoaithanh.qlgh.session.SessionManager;

public abstract class BaseActivity extends AppCompatActivity implements IBaseActivity{
    protected SessionManager session;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        session = new SessionManager(getApplicationContext());
        initLayout();
        initData();
        initView();
    }

    private BroadcastReceiver accountLockedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Khi nhận được tín hiệu "ACCOUNT_LOCKED"
            // Gọi hàm hiển thị dialog và đăng xuất
            showAccountLockedAndLogout();
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        // Đăng ký "nghe loa" khi Activity hoạt động
        LocalBroadcastManager.getInstance(this).registerReceiver(
                accountLockedReceiver,
                new IntentFilter("ACTION_ACCOUNT_LOCKED")
        );
    }

    @Override
    protected void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(accountLockedReceiver);
    }

    protected void showAccountLockedAndLogout() {
        String message = "Tài khoản của bạn đã bị khóa. Vui lòng liên hệ hỗ trợ để biết thêm chi tiết.";

        new MaterialAlertDialogBuilder(this)
                .setTitle("Tài khoản bị khóa")
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton("Đăng xuất", (dialog, which) -> {
                    // Đăng xuất người dùng
                    session.logout();
                    Intent i = new Intent(this, LoginActivity.class);
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(i);
                    finish();
                })
                .setNegativeButton("Đã hiểu", null)
                .show();
    }
}

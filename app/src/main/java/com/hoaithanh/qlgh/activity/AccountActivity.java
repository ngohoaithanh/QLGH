package com.hoaithanh.qlgh.activity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.hoaithanh.qlgh.R;
import com.hoaithanh.qlgh.session.SessionManager;
import com.hoaithanh.qlgh.base.BaseActivity;

public class AccountActivity extends BaseActivity {

    private SessionManager session;

    private TextView tvUsername, tvPhone, tvUserId, tvRole;
    private ImageView ivAvatar;
    private MaterialButton btnEditProfile, btnLogout;

    @Override
    public void initLayout() {
        setContentView(R.layout.activity_account);
    }

    @Override
    public void initData() {
        session = new SessionManager(this);
    }

    @Override
    public void initView() {
        ivAvatar = findViewById(R.id.ivAvatar);
        tvUsername = findViewById(R.id.tvUsername);
        tvPhone = findViewById(R.id.tvPhone);
        tvUserId = findViewById(R.id.tvUserId);
        tvRole = findViewById(R.id.tvRole);
        btnEditProfile = findViewById(R.id.btnEditProfile);
        btnLogout = findViewById(R.id.btnLogout);

        // Bind dữ liệu từ SharedPreferences
        String username = session.getUsername();
        String phone = session.getPhone();
        int userId = session.getUserId();
        int role = session.getRole();

        tvUsername.setText(username.isEmpty() ? "Người dùng" : username);
        tvPhone.setText(phone.isEmpty() ? "Chưa cập nhật SĐT" : phone);
        tvUserId.setText("#" + userId);
        tvRole.setText(role == 1 ? "Admin/Manager" : "Khách hàng");

        // Sửa hồ sơ - TODO: mở màn hình chỉnh sửa nếu có
        btnEditProfile.setOnClickListener(v -> {
            // startActivity(new Intent(this, EditProfileActivity.class));
            // TODO: implement
        });

        // Đăng xuất
        btnLogout.setOnClickListener(v -> showLogoutConfirm());
    }

    private void showLogoutConfirm() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Đăng xuất")
                .setMessage("Bạn có chắc muốn đăng xuất?")
                .setPositiveButton("Đồng ý", (d, w) -> doLogout())
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void doLogout() {
        session.logout();
        Intent i = new Intent(this, LoginActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
        finish();
    }
}

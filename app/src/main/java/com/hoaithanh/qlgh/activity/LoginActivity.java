package com.hoaithanh.qlgh.activity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.hoaithanh.qlgh.R;
import com.hoaithanh.qlgh.api.ApiService;
import com.hoaithanh.qlgh.api.RetrofitClient;
import com.hoaithanh.qlgh.base.BaseActivity;
import com.hoaithanh.qlgh.session.SessionManager;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends BaseActivity {

    private TextInputLayout tilPhone, tilPassword;
    private TextInputEditText etPhone, etPassword;
    private MaterialButton btnLogin;

    private SessionManager session;

    @Override
    public void initLayout() {
        setContentView(R.layout.activity_login);
    }

    @Override
    public void initData() {}

    @Override
    public void initView() {
        tilPhone = findViewById(R.id.tilPhone);
        tilPassword = findViewById(R.id.tilPassword);
        etPhone = findViewById(R.id.etPhone);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);

        session = new SessionManager(this);

        btnLogin.setOnClickListener(v -> tryLogin());
    }

    private void tryLogin() {
        // reset lỗi
        tilPhone.setError(null);
        tilPassword.setError(null);

        String phone = safe(etPhone.getText());
        String password = safe(etPassword.getText());

        boolean ok = true;
        if (phone.isEmpty()) {
            tilPhone.setError("Vui lòng nhập số điện thoại");
            ok = false;
        } else if (!isValidVnPhone(phone)) {
            tilPhone.setError("Số điện thoại không hợp lệ");
            ok = false;
        }
        if (password.isEmpty()) {
            tilPassword.setError("Vui lòng nhập mật khẩu");
            ok = false;
        }
        if (!ok) return;

        setLoading(true);

        ApiService api = RetrofitClient.getApi();
        api.loginByPhone(phone, password).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> res) {
                setLoading(false);
                if (!res.isSuccessful() || res.body() == null) {
                    Toast.makeText(LoginActivity.this, "Lỗi server: " + res.code(), Toast.LENGTH_SHORT).show();
                    return;
                }

                LoginResponse body = res.body();
                if (body.success && body.user != null) {
                    // Lưu vào session
                    session.saveLogin(
                            true,
                            body.user.ID,
                            body.user.Username,
                            body.user.Role,
                            body.token,
                            phone
                    );

                    Toast.makeText(LoginActivity.this, "Đăng nhập thành công", Toast.LENGTH_SHORT).show();

                    // Điều hướng theo role
                    if (body.user.Role == 1) {
                        startActivity(new Intent(LoginActivity.this, MainActivity.class));
                    } else {
//                        startActivity(new Intent(LoginActivity.this, CustomerActivity.class));
                        Toast.makeText(LoginActivity.this, "Tài khoản không có quyền truy cập MainActivity", Toast.LENGTH_SHORT).show();
                    }
                    finish();

                } else {
                    Toast.makeText(LoginActivity.this,
                            body.message != null ? body.message : "Đăng nhập thất bại",
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                setLoading(false);
                Toast.makeText(LoginActivity.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setLoading(boolean loading) {
        btnLogin.setEnabled(!loading);
        btnLogin.setText(loading ? "Đang đăng nhập..." : "Đăng nhập");
    }

    private String safe(CharSequence cs) {
        return cs == null ? "" : cs.toString().trim();
    }

    // Validator SĐT Việt Nam đơn giản
    private boolean isValidVnPhone(String phone) {
        String p = phone.replaceAll("\\s+", "");
        if (p.startsWith("+84")) p = "0" + p.substring(3);
        else if (p.startsWith("84")) p = "0" + p.substring(2);
        return p.matches("^0[2-9][0-9]{8}$");
    }

    // ====== Model parse JSON ======
    public static class LoginResponse {
        public boolean success;
        public String message;
        public String token;
        public User user;

        public static class User {
            public int ID;
            public String Username;
            public int Role;
            public String PhoneNumber;
        }
    }
}

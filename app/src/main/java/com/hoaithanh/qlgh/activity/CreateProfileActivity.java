package com.hoaithanh.qlgh.activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.hoaithanh.qlgh.R;
import com.hoaithanh.qlgh.api.ApiService; // Import ApiService
import com.hoaithanh.qlgh.api.RetrofitClient; // Import RetrofitClient
import com.hoaithanh.qlgh.model.SimpleResult; // Import SimpleResult

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CreateProfileActivity extends AppCompatActivity {

    private TextInputLayout tilFullName, tilPassword, tilConfirmPassword;
    private TextInputEditText etFullName, etPassword, etConfirmPassword;
    private Button btnCompleteRegistration;
    private ProgressBar progressBar;
    private TextView tvWelcomePhone;

    private String phoneNumber;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_profile);

        // Lấy số điện thoại đã xác thực từ Intent
        phoneNumber = getIntent().getStringExtra("PHONE_NUMBER");
        if (phoneNumber == null) {
            Toast.makeText(this, "Lỗi: Không tìm thấy số điện thoại.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // Ánh xạ View
        tilFullName = findViewById(R.id.tilFullName);
        tilPassword = findViewById(R.id.tilPassword);
        tilConfirmPassword = findViewById(R.id.tilConfirmPassword);
        etFullName = findViewById(R.id.etFullName);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnCompleteRegistration = findViewById(R.id.btnCompleteRegistration);
        progressBar = findViewById(R.id.progressBar);
        tvWelcomePhone = findViewById(R.id.tvWelcomePhone);

        tvWelcomePhone.setText("Chào mừng, " + phoneNumber);

        // Xử lý sự kiện click
        btnCompleteRegistration.setOnClickListener(v -> {
            validateAndRegister();
        });
    }

    private void validateAndRegister() {
        // Xóa lỗi cũ
        tilFullName.setError(null);
        tilPassword.setError(null);
        tilConfirmPassword.setError(null);

        String fullName = etFullName.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        // 1. Kiểm tra dữ liệu nhập
        if (TextUtils.isEmpty(fullName)) {
            tilFullName.setError("Họ tên không được để trống");
            return;
        }
        if (password.length() < 6) {
            tilPassword.setError("Mật khẩu phải có ít nhất 6 ký tự");
            return;
        }
        if (!password.equals(confirmPassword)) {
            tilConfirmPassword.setError("Mật khẩu nhập lại không khớp");
            return;
        }

        // 2. Bắt đầu quá trình đăng ký
        showLoading(true);
        callRegisterApi(phoneNumber, fullName, password);
    }

    private void callRegisterApi(String phone, String fullName, String password) {
        ApiService apiService = RetrofitClient.getApi();
        apiService.registerUser(phone, fullName, password).enqueue(new Callback<SimpleResult>() {
            @Override
            public void onResponse(Call<SimpleResult> call, Response<SimpleResult> response) {
                showLoading(false);
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    // ĐĂNG KÝ THÀNH CÔNG
                    Toast.makeText(CreateProfileActivity.this, "Đăng ký thành công! Vui lòng đăng nhập.", Toast.LENGTH_LONG).show();

                    // Quay về màn hình Đăng nhập
                    Intent intent = new Intent(CreateProfileActivity.this, LoginActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                } else {
                    // Hiển thị lỗi từ server
                    String error = "Đăng ký thất bại. Vui lòng thử lại.";
                    if (response.body() != null && response.body().getMessage() != null) {
                        error = response.body().getMessage();
                    }
                    Toast.makeText(CreateProfileActivity.this, error, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<SimpleResult> call, Throwable t) {
                showLoading(false);
                Toast.makeText(CreateProfileActivity.this, "Lỗi mạng: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showLoading(boolean isLoading) {
        if (isLoading) {
            progressBar.setVisibility(View.VISIBLE);
            btnCompleteRegistration.setEnabled(false);
        } else {
            progressBar.setVisibility(View.GONE);
            btnCompleteRegistration.setEnabled(true);
        }
    }
}
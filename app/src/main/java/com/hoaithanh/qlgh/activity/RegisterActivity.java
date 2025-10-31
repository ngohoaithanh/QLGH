package com.hoaithanh.qlgh.activity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity; // Hoặc BaseActivity

import com.chaos.view.PinView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;
import com.hoaithanh.qlgh.R;
import com.hoaithanh.qlgh.api.ApiService;
import com.hoaithanh.qlgh.api.RetrofitClient;
import com.hoaithanh.qlgh.base.BaseActivity; // Hoặc AppCompatActivity
import com.hoaithanh.qlgh.model.UserCheckResponse;

import java.util.concurrent.TimeUnit;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

// Bạn có thể kế thừa từ BaseActivity nếu đã có
public class RegisterActivity extends AppCompatActivity {

    // --- Khai báo UI Elements ---
    private TextInputLayout tilPhoneNumber;
    private TextInputEditText etPhoneNumber;
    private Button btnSendOtp, btnVerifyOtp;
    private LinearLayout layoutPhoneNumber, layoutOtp;
    private PinView otpView;
    private TextView tvOtpInstruction, tvResendOtp;
    private ProgressBar progressBar;

    // --- Khai báo Firebase ---
    private FirebaseAuth mAuth;
    private String mVerificationId;
    private PhoneAuthProvider.ForceResendingToken mResendToken;
    private PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallbacks;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Khởi tạo Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // --- Ánh xạ View ---
        tilPhoneNumber = findViewById(R.id.tilPhoneNumber);
        etPhoneNumber = findViewById(R.id.etPhoneNumber);
        btnSendOtp = findViewById(R.id.btnSendOtp);
        btnVerifyOtp = findViewById(R.id.btnVerifyOtp);
        layoutPhoneNumber = findViewById(R.id.layoutPhoneNumber);
        layoutOtp = findViewById(R.id.layoutOtp);
        otpView = findViewById(R.id.otpView);
        tvOtpInstruction = findViewById(R.id.tvOtpInstruction);
        tvResendOtp = findViewById(R.id.tvResendOtp);
        progressBar = findViewById(R.id.progressBar);

        // --- Định nghĩa Callbacks ---
        initializeFirebaseCallbacks();

        // --- Thiết lập Listeners ---
        setupClickListeners();
    }

    private void initializeFirebaseCallbacks() {
        mCallbacks = new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            @Override
            public void onVerificationCompleted(@NonNull PhoneAuthCredential credential) {
                // Tự động xác thực hoặc sau khi nhập mã OTP thành công
                Log.d("FIREBASE_AUTH", "onVerificationCompleted:" + credential);
                signInWithFirebase(credential);
            }

            @Override
            public void onVerificationFailed(@NonNull FirebaseException e) {
                // Gửi OTP thất bại
                Log.w("FIREBASE_AUTH", "onVerificationFailed", e);
                progressBar.setVisibility(View.GONE);
                tilPhoneNumber.setError("Gửi OTP thất bại: " + e.getMessage());

                // Hiển thị lại giao diện nhập SĐT
                layoutPhoneNumber.setVisibility(View.VISIBLE);
                layoutOtp.setVisibility(View.GONE);
            }

            @Override
            public void onCodeSent(@NonNull String verificationId,
                                   @NonNull PhoneAuthProvider.ForceResendingToken token) {
                // Gửi OTP thành công
                Log.d("FIREBASE_AUTH", "onCodeSent:" + verificationId);
                Toast.makeText(RegisterActivity.this, "Đã gửi mã OTP.", Toast.LENGTH_SHORT).show();

                // Lưu lại ID và Token
                mVerificationId = verificationId;
                mResendToken = token;

                // Ẩn/hiện giao diện
                progressBar.setVisibility(View.GONE);
                layoutPhoneNumber.setVisibility(View.GONE);
                layoutOtp.setVisibility(View.VISIBLE);

                String phoneNumber = etPhoneNumber.getText().toString().trim();
                tvOtpInstruction.setText("Nhập mã OTP đã gửi đến " + phoneNumber);
            }
        };
    }

    private void setupClickListeners() {
        btnSendOtp.setOnClickListener(v -> {
            String phoneNumber = etPhoneNumber.getText().toString().trim();
            if (!isValidPhoneNumber(phoneNumber)) {
                tilPhoneNumber.setError("Số điện thoại không hợp lệ (10 số, bắt đầu bằng 0)");
                return;
            }
            tilPhoneNumber.setError(null); // Xóa lỗi cũ
            startPhoneNumberVerification(phoneNumber);
        });

        btnVerifyOtp.setOnClickListener(v -> {
            String otpCode = otpView.getText().toString().trim();
            if (otpCode.length() < 6) {
                Toast.makeText(this, "Vui lòng nhập đủ 6 số OTP", Toast.LENGTH_SHORT).show();
                return;
            }
            verifyPhoneNumberWithCode(otpCode);
        });

        tvResendOtp.setOnClickListener(v -> {
            String phoneNumber = etPhoneNumber.getText().toString().trim();
            if (!isValidPhoneNumber(phoneNumber)) {
                tilPhoneNumber.setError("Số điện thoại không hợp lệ");
                return;
            }
            resendVerificationCode(phoneNumber);
        });
    }

    private boolean isValidPhoneNumber(String phoneNumber) {
        return phoneNumber.length() == 10 && phoneNumber.startsWith("0");
    }

    private void startPhoneNumberVerification(String phoneNumber) {
        // Hiển thị loading
        progressBar.setVisibility(View.VISIBLE);
        layoutPhoneNumber.setVisibility(View.GONE);

        // Chuyển đổi SĐT sang định dạng E.164 (+84...)
        String formattedPhone = "+84" + phoneNumber.substring(1);

        PhoneAuthOptions options =
                PhoneAuthOptions.newBuilder(mAuth)
                        .setPhoneNumber(formattedPhone)
                        .setTimeout(60L, TimeUnit.SECONDS)
                        .setActivity(this)
                        .setCallbacks(mCallbacks)
                        .build();
        PhoneAuthProvider.verifyPhoneNumber(options);
    }

    private void verifyPhoneNumberWithCode(String otpCode) {
        // Vô hiệu hóa nút và hiển thị loading
        btnVerifyOtp.setEnabled(false);
        progressBar.setVisibility(View.VISIBLE);

        try {
            PhoneAuthCredential credential = PhoneAuthProvider.getCredential(mVerificationId, otpCode);
            signInWithFirebase(credential);
        } catch (Exception e) {
            progressBar.setVisibility(View.GONE);
            btnVerifyOtp.setEnabled(true);
            Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void resendVerificationCode(String phoneNumber) {
        // Hiển thị loading
        progressBar.setVisibility(View.VISIBLE);
        Toast.makeText(this, "Đang gửi lại mã OTP...", Toast.LENGTH_SHORT).show();

        String formattedPhone = "+84" + phoneNumber.substring(1);

        PhoneAuthOptions options =
                PhoneAuthOptions.newBuilder(mAuth)
                        .setPhoneNumber(formattedPhone)
                        .setTimeout(60L, TimeUnit.SECONDS)
                        .setActivity(this)
                        .setCallbacks(mCallbacks)
                        .setForceResendingToken(mResendToken) // Quan trọng: Dùng token cũ để gửi lại
                        .build();
        PhoneAuthProvider.verifyPhoneNumber(options);
    }

    private void signInWithFirebase(PhoneAuthCredential credential) {
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    progressBar.setVisibility(View.GONE);
                    btnVerifyOtp.setEnabled(true);

                    if (task.isSuccessful()) {
                        // ĐĂNG NHẬP/ĐĂNG KÝ VỚI FIREBASE THÀNH CÔNG
                        Toast.makeText(RegisterActivity.this, "Xác thực thành công!", Toast.LENGTH_SHORT).show();

                        FirebaseUser user = task.getResult().getUser();
                        String uid = user.getUid();
                        String phoneNumber = user.getPhoneNumber();
                        String localPhoneNumber = convertToLocalFormat(phoneNumber);
                        checkIfUserExistsInMyDatabase(localPhoneNumber);
                        // TODO: Gọi API để lưu tài khoản vào database MySQL
                        // GỌI HÀM NÀY SAU KHI LƯU VÀO MYSQL THÀNH CÔNG
                        // navigateToMain();

                    } else {
                        // Xác thực thất bại (ví dụ: sai mã OTP)
                        Toast.makeText(RegisterActivity.this, "Xác thực thất bại: Mã OTP không đúng.", Toast.LENGTH_LONG).show();
                    }
                });
    }

    private String convertToLocalFormat(String e164PhoneNumber) {
        if (e164PhoneNumber != null && e164PhoneNumber.startsWith("+84") && e164PhoneNumber.length() > 3) {
            // Thay thế "+84" ở đầu bằng số "0"
            return "0" + e164PhoneNumber.substring(3);
        }
        // Trả về nguyên bản nếu không đúng định dạng
        return e164PhoneNumber;
    }

    private void checkIfUserExistsInMyDatabase(String localPhoneNumber) {
        progressBar.setVisibility(View.VISIBLE);

        // (Lưu ý: Tốt nhất nên gọi qua ViewModel/Repository, đây là cách gọi trực tiếp)
        ApiService apiService = RetrofitClient.getApi();
        apiService.checkUserExists(localPhoneNumber).enqueue(new Callback<UserCheckResponse>() {

            @Override
            public void onResponse(Call<UserCheckResponse> call, Response<UserCheckResponse> response) {
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    if (response.body().isExists()) {
                        // SĐT đã tồn tại
                        tilPhoneNumber.setError("Số điện thoại này đã được đăng ký. Vui lòng đăng nhập.");
                        layoutPhoneNumber.setVisibility(View.VISIBLE);
                        layoutOtp.setVisibility(View.GONE);
                    } else {
                        // SĐT HỢP LỆ ĐỂ ĐĂNG KÝ
                        // Chuyển sang màn hình điền thông tin
                        Intent intent = new Intent(RegisterActivity.this, CreateProfileActivity.class); // Tên Activity giả định
                        intent.putExtra("PHONE_NUMBER", localPhoneNumber);
                        startActivity(intent);
                    }
                } else {
                    Toast.makeText(RegisterActivity.this, "Lỗi kiểm tra tài khoản.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<UserCheckResponse> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(RegisterActivity.this, "Lỗi mạng: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void navigateToMain() {
        Intent intent = new Intent(RegisterActivity.this, MainActivity.class); // Sửa tên Activity chính nếu cần
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
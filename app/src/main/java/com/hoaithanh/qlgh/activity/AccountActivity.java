package com.hoaithanh.qlgh.activity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.hoaithanh.qlgh.R;
import com.hoaithanh.qlgh.api.ApiService;
import com.hoaithanh.qlgh.api.RetrofitClient;
import com.hoaithanh.qlgh.model.ApiResult;
import com.hoaithanh.qlgh.model.SimpleResult;
import com.hoaithanh.qlgh.session.SessionManager;
import com.hoaithanh.qlgh.base.BaseActivity;
import com.hoaithanh.qlgh.viewmodel.UserViewModel;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AccountActivity extends BaseActivity {
    private FusedLocationProviderClient fusedClient;

    private SessionManager session;

    private TextView tvUsername, tvPhone, tvUserId, tvRole;
    private ImageView ivAvatar;
    private MaterialButton btnEditProfile, btnLogout;

    private MaterialCardView cardShipperEarnings, cardShipperVehicle;
    private TextView tvShipperVehicleInfo;
    private UserViewModel userViewModel;
    private TextView tvShipperRating, tvShipperRatingStatus;
    private ActivityResultLauncher<String> pickImageLauncher;

    @Override
    public void initLayout() {
        setContentView(R.layout.activity_account);
    }

    @Override
    public void initData() {
        session = new SessionManager(this);
        userViewModel = new ViewModelProvider(this).get(UserViewModel.class);
        fusedClient = LocationServices.getFusedLocationProviderClient(this);
        try {
            Map<String, String> config = new HashMap<>();
            config.put("cloud_name", "dbaeafw6z");
            config.put("api_key", "747842428664635");
            config.put("api_secret", "_q8HnSYPip8Fw1eTXUfgcMK_q6k");
            MediaManager.init(this, config);
        } catch (Exception e) {
            // Đã khởi tạo rồi thì bỏ qua
        }
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

        cardShipperEarnings = findViewById(R.id.card_shipper_earnings);
        cardShipperVehicle = findViewById(R.id.card_shipper_vehicle);
        tvShipperVehicleInfo = findViewById(R.id.tvShipperVehicleInfo);

        tvShipperRating = findViewById(R.id.tvShipperRating);
        tvShipperRatingStatus = findViewById(R.id.tvShipperRatingStatus);

        // Bind dữ liệu từ SharedPreferences
        String username = session.getUsername();
        String phone = session.getPhone();
        int userId = session.getUserId();
        int role = session.getRole();
        float rating = session.getRating();
        String avatarUrl = session.getAvatar();
        if (!avatarUrl.isEmpty()) {
            Glide.with(this)
                    .load(avatarUrl)
                    .placeholder(R.drawable.ic_account_circle) // Hình mặc định khi đang tải
                    .error(R.drawable.ic_account_circle)       // Hình mặc định nếu lỗi
                    .circleCrop()                              // Cắt hình tròn
                    .into(ivAvatar);
        }

        tvUsername.setText(username.isEmpty() ? "Người dùng" : username);
        tvPhone.setText(phone.isEmpty() ? "Chưa cập nhật SĐT" : phone);
        tvUserId.setText("#" + userId);
        if (role == 7) {
            tvRole.setText("Khách hàng");
            cardShipperEarnings.setVisibility(View.GONE);
            cardShipperVehicle.setVisibility(View.GONE);
            tvShipperRating.setVisibility(View.GONE);
            tvShipperRatingStatus.setVisibility(View.GONE);
        } else if (role == 6) {
            tvRole.setText("Shipper");
            cardShipperEarnings.setVisibility(View.VISIBLE);
            cardShipperVehicle.setVisibility(View.VISIBLE);
            tvShipperRating.setVisibility(View.VISIBLE);
            tvShipperRatingStatus.setVisibility(View.VISIBLE);

            // --- LOGIC HIỂN THỊ RATING VÀ TRẠNG THÁI ---
            String ratingText = String.format(Locale.US, "%.1f ★", rating);
            tvShipperRating.setText(ratingText);

            // Đặt logic cho trạng thái/cảnh báo
            if (rating >= 4.8) {
                tvShipperRatingStatus.setText("Tài xế xuất sắc");
                tvShipperRatingStatus.setTextColor(ContextCompat.getColor(this, R.color.main_route_color)); // Màu xanh
            } else if (rating >= 4.5) {
                tvShipperRatingStatus.setText("Tài xế tốt");
                tvShipperRatingStatus.setTextColor(ContextCompat.getColor(this, R.color.colorPrimaryDark));
            } else if (rating >= 4.0) {
                tvShipperRatingStatus.setText("Cần cải thiện");
                tvShipperRatingStatus.setTextColor(ContextCompat.getColor(this, R.color.star_yellow)); // Màu cam/vàng
            } else {
                tvShipperRatingStatus.setText("Tỉ lệ đánh giá thấp (Có nguy cơ bị khóa)");
                tvShipperRatingStatus.setTextColor(ContextCompat.getColor(this, R.color.red)); // Màu đỏ
            }

            loadVehicleInfo(userId);

            cardShipperEarnings.setOnClickListener(v -> {
                startActivity(new Intent(this, ShipperEarningsActivity.class));
            });
        } else {
            tvRole.setText("Admin/Manager");
        }

        // Sửa hồ sơ - TODO: mở màn hình chỉnh sửa nếu có
        btnEditProfile.setOnClickListener(v -> {
            showEditProfileDialog();
        });

        // Đăng xuất
        btnLogout.setOnClickListener(v -> showLogoutConfirm());

        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        // Khi người dùng chọn ảnh xong -> Upload ngay
                        uploadImageToCloudinary(uri);
                    }
                }
        );

        // --- SỰ KIỆN CLICK VÀO AVATAR ĐỂ ĐỔI ẢNH ---
        ivAvatar.setOnClickListener(v -> {
            // Mở thư viện ảnh
            pickImageLauncher.launch("image/*");
        });
    }

    private void uploadImageToCloudinary(Uri imageUri) {
        // Hiển thị loading (nếu muốn)
        Toast.makeText(this, "Đang tải ảnh lên...", Toast.LENGTH_SHORT).show();

        MediaManager.get().upload(imageUri)
                // 1. CHỈ ĐỊNH FOLDER
                .option("folder", "avatars")

                // 2. (KHUYÊN DÙNG) Đặt tên ảnh theo User ID để tự động ghi đè ảnh cũ
                .option("public_id", "user_" + session.getUserId())
                .option("overwrite", true)
                .option("resource_type", "image")
                .callback(new UploadCallback() {
                    @Override
                    public void onStart(String requestId) {
                    }

                    @Override
                    public void onProgress(String requestId, long bytes, long totalBytes) {
                    }

                    @Override
                    public void onSuccess(String requestId, Map resultData) {
                        // Lấy URL ảnh trả về
                        String secureUrl = (String) resultData.get("secure_url");

                        // Gọi API để lưu URL vào database
                        updateAvatarOnServer(secureUrl);
                    }

                    @Override
                    public void onError(String requestId, ErrorInfo error) {
                        Toast.makeText(AccountActivity.this, "Lỗi upload: " + error.getDescription(), Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onReschedule(String requestId, ErrorInfo error) {
                    }
                })
                .dispatch();
    }

    // Hàm gọi API update_profile.php
    private void updateAvatarOnServer(String avatarUrl) {
        ApiService api = RetrofitClient.getApi();
        api.updateAvatar(session.getUserId(), avatarUrl).enqueue(new Callback<SimpleResult>() {
            @Override
            public void onResponse(Call<SimpleResult> call, Response<SimpleResult> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    Toast.makeText(AccountActivity.this, "Cập nhật ảnh đại diện thành công!", Toast.LENGTH_SHORT).show();

                    // 1. Lưu URL mới vào Session (để lần sau mở app vẫn còn)
                    // (Bạn cần thêm hàm saveAvatar vào SessionManager như đã bàn trước đó)
                    session.saveLogin(
                            session.isLoggedIn(), session.getUserId(), session.getUsername(),
                            session.getRole(), session.getToken(), session.getPhone(),
                            session.getRating(), session.getSessionId(),
                            avatarUrl // <-- Avatar mới
                    );

                    // 2. Hiển thị ảnh mới ngay lập tức
                    Glide.with(AccountActivity.this)
                            .load(avatarUrl)
                            .circleCrop()
                            .into(ivAvatar);

                } else {
                    Toast.makeText(AccountActivity.this, "Lỗi server: Cập nhật thất bại", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<SimpleResult> call, Throwable t) {
                Toast.makeText(AccountActivity.this, "Lỗi mạng: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadVehicleInfo(int shipperId) {
        userViewModel.getVehicleInfo(shipperId).observe(this, vehicle -> {
            if (vehicle != null && vehicle.getModel() != null) {
                String vehicleInfo = vehicle.getModel() + " - " + vehicle.getLicensePlate();
                tvShipperVehicleInfo.setText(vehicleInfo);
            } else {
                tvShipperVehicleInfo.setText("Chưa cập nhật thông tin xe");
            }
        });
    }

    private void showEditProfileDialog() {
        // Inflate layout của dialog
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_edit_profile, null);

        // Ánh xạ các view
        TextInputLayout tilName = dialogView.findViewById(R.id.tilEditName);
        TextInputEditText etName = dialogView.findViewById(R.id.etEditName);
        TextInputLayout tilEmail = dialogView.findViewById(R.id.tilEditEmail);
        TextInputEditText etEmail = dialogView.findViewById(R.id.etEditEmail);
        TextInputLayout tilPassword = dialogView.findViewById(R.id.tilEditPassword);
        TextInputEditText etPassword = dialogView.findViewById(R.id.etEditPassword);
        TextInputLayout tilConfirmPassword = dialogView.findViewById(R.id.tilEditConfirmPassword);
        TextInputEditText etConfirmPassword = dialogView.findViewById(R.id.etEditConfirmPassword);
        TextInputLayout tilOldPassword = dialogView.findViewById(R.id.tilEditOldPassword);
        TextInputEditText etOldPassword = dialogView.findViewById(R.id.etEditOldPassword);
        View dialogRoot = dialogView.findViewById(R.id.dialogRoot);
        // 2. Xử lý sự kiện CHẠM (Touch)
        if (dialogRoot != null) {
            dialogRoot.setOnTouchListener((v, event) -> {
                if (event.getAction() == android.view.MotionEvent.ACTION_DOWN) {
                    hideKeyboardFrom(v);
                    View currentFocus = v.findFocus();
                    if (currentFocus != null) {
                        currentFocus.clearFocus();
                    }
                }
                return false;
            });
        }

        etName.setText(session.getUsername());

        MaterialAlertDialogBuilder dialogBuilder = new MaterialAlertDialogBuilder(this)
                .setTitle("Chỉnh sửa Hồ sơ")
                .setView(dialogView)
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Lưu", null); // Tạm thời set null để Ghi đè

        AlertDialog dialog = dialogBuilder.create();
        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            // Xóa các lỗi cũ
            tilName.setError(null);
            tilEmail.setError(null);
            tilPassword.setError(null);
            tilConfirmPassword.setError(null);
            tilOldPassword.setError(null);

            // Lấy dữ liệu mới
            String newName = etName.getText().toString().trim();
            String newEmail = etEmail.getText().toString().trim();
            String newPassword = etPassword.getText().toString().trim();
            String confirmPassword = etConfirmPassword.getText().toString().trim();
            String oldPassword = etOldPassword.getText().toString().trim();

            boolean isValid = true;

            // --- BẮT ĐẦU VALIDATION ---
            if (newName.isEmpty()) {
                tilName.setError("Họ tên không được để trống");
                isValid = false;
            }

            // Kiểm tra email (nếu có nhập)
            if (!newEmail.isEmpty() && !Patterns.EMAIL_ADDRESS.matcher(newEmail).matches()) {
                tilEmail.setError("Email không đúng định dạng");
                isValid = false;
            }

            // Chỉ kiểm tra mật khẩu nếu người dùng có nhập
            if (!newPassword.isEmpty()) {
                if (oldPassword.isEmpty()) {
                    tilOldPassword.setError("Vui lòng nhập mật khẩu hiện tại");
                    isValid = false;
                }
                if (newPassword.length() < 6) {
                    tilPassword.setError("Mật khẩu mới phải có ít nhất 6 ký tự");
                    isValid = false;
                } else if (!newPassword.equals(confirmPassword)) {
                    tilConfirmPassword.setError("Mật khẩu xác nhận không khớp");
                    isValid = false;
                }
            }
            // --- KẾT THÚC VALIDATION ---

            if (isValid) {
                callUpdateApi(newName, newEmail, newPassword, oldPassword); // <-- Truyền Mật khẩu cũ
                dialog.dismiss();
            }
        });
    }

    // THÊM HÀM MỚI NÀY VÀO CLASS
    private void callUpdateApi(String newName, String newEmail, String newPassword, String oldPassword) {
        int userId = session.getUserId();

        // Hiển thị loading (nếu có)

        ApiService api = RetrofitClient.getApi();
        api.updateProfile(userId, newName, newEmail, newPassword, oldPassword).enqueue(new Callback<SimpleResult>() {
            @Override
            public void onResponse(Call<SimpleResult> call, Response<SimpleResult> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    Toast.makeText(AccountActivity.this, "Cập nhật thành công!", Toast.LENGTH_SHORT).show();

                    // Cập nhật lại thông tin trong Session
                    if (!newName.isEmpty()) {
                        session.setUsername(newName);
                    }
                    // (Lưu lại email mới nếu cần)

                    // Cập nhật lại giao diện ngay lập tức
                    tvUsername.setText(session.getUsername());
                    // (Cập nhật email nếu có)

                } else {
                    String error = (response.body() != null) ? response.body().getMessage() : "Lỗi không xác định";
                    Toast.makeText(AccountActivity.this, "Lỗi: " + error, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<SimpleResult> call, Throwable t) {
                Toast.makeText(AccountActivity.this, "Lỗi mạng: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showLogoutConfirm() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Đăng xuất")
                .setMessage("Bạn có chắc muốn đăng xuất?")
                .setPositiveButton("Đồng ý", (d, w) -> doLogout())
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void performLocalLogout() {
        session.logout();
        Intent i = new Intent(this, LoginActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
        finish();
    }

    // 2. Sửa lại hàm doLogout chính
    private void doLogout() {
        // 1. Nếu không phải Shipper -> Logout luôn
        if (session.getRole() != 6) {
            performLocalLogout();
            return;
        }

        Toast.makeText(this, "Đang đăng xuất...", Toast.LENGTH_SHORT).show();


        // 3. Lấy vị trí cuối cùng (Last Known Location)
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            callLogoutApi(0.0, 0.0);
            return;
        }
        fusedClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        // Có vị trí -> Gửi vị trí thật
                        callLogoutApi(location.getLatitude(), location.getLongitude());
                    } else {
                        // Không lấy được vị trí (ví dụ: tắt GPS) -> Gửi 0.0
                        callLogoutApi(0.0, 0.0);
                    }
                })
                .addOnFailureListener(e -> {
                    callLogoutApi(0.0, 0.0);
                });
    }

    // Thêm hàm này vào cuối file
    private void callLogoutApi(double lat, double lng) {
        ApiService apiService = RetrofitClient.getApi();
        apiService.updateShipperLocation(session.getUserId(), lat, lng, "offline")
                .enqueue(new Callback<ApiResult>() {
                    @Override
                    public void onResponse(Call<ApiResult> call, Response<ApiResult> response) {
                        performLocalLogout();
                    }

                    @Override
                    public void onFailure(Call<ApiResult> call, Throwable t) {
                        performLocalLogout();
                    }
                });
    }
}

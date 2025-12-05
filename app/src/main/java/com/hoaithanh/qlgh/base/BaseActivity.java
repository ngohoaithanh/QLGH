package com.hoaithanh.qlgh.base;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.material.badge.BadgeDrawable;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.hoaithanh.qlgh.activity.LoginActivity;
import com.hoaithanh.qlgh.api.RetrofitClient;
import com.hoaithanh.qlgh.model.UnreadCountResponse;
import com.hoaithanh.qlgh.session.SessionManager;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import android.os.Handler;
import android.os.Looper;

public abstract class BaseActivity extends AppCompatActivity implements IBaseActivity{
    protected SessionManager session;
    private Handler badgeHandler = new Handler(Looper.getMainLooper());
    private Runnable badgeRunnable;
    private BottomNavigationView currentNavView;
    private int currentMenuItemId;
    private static final long BADGE_POLL_INTERVAL = 30000;
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
        stopBadgePolling();
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

    protected void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
        // Tìm view đang có focus (thường là EditText)
        View view = getCurrentFocus();
        // Nếu không có view nào focus, tạo một view mới
        if (view == null) {
            view = new View(this);
        }
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }
    protected void hideKeyboardFrom(View view) {
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }
    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            View v = getCurrentFocus();
            if (v instanceof EditText) {
                Rect outRect = new Rect();
                v.getGlobalVisibleRect(outRect);

                // Kiểm tra xem vị trí chạm có nằm NGOÀI EditText đang focus không
                if (!outRect.contains((int)event.getRawX(), (int)event.getRawY())) {
                    v.clearFocus(); // Bỏ focus
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0); // Ẩn bàn phím
                }
            }
        }
        return super.dispatchTouchEvent(event);
    }

    protected void updateNotificationBadge(BottomNavigationView navView, int menuItemId) {
        if (navView == null) return;

        RetrofitClient.getApi().getUnreadCount().enqueue(new Callback<UnreadCountResponse>() {
            @Override
            public void onResponse(Call<UnreadCountResponse> call, Response<UnreadCountResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    int count = response.body().getCount();

                    // Lấy hoặc tạo Badge
                    BadgeDrawable badge = navView.getOrCreateBadge(menuItemId);

                    if (count > 0) {
                        badge.setVisible(true);
                        badge.setNumber(count); // Hiển thị số (ví dụ: 5)
                        // badge.clearNumber(); // Nếu chỉ muốn hiện chấm đỏ, bỏ comment dòng này
                    } else {
                        badge.setVisible(false); // Ẩn nếu = 0
                    }
                }
            }

            @Override
            public void onFailure(Call<UnreadCountResponse> call, Throwable t) {
                // Không làm gì nếu lỗi mạng (để tránh phiền user)
            }
        });
    }

    protected void startBadgePolling(BottomNavigationView navView, int menuItemId) {
        this.currentNavView = navView;
        this.currentMenuItemId = menuItemId;

        // Hủy vòng lặp cũ (nếu có) để tránh trùng lặp
        stopBadgePolling();

        badgeRunnable = new Runnable() {
            @Override
            public void run() {
                // 1. Gọi API cập nhật
                updateNotificationBadgeNow();

                // 2. Lên lịch chạy lại sau 30s
                badgeHandler.postDelayed(this, BADGE_POLL_INTERVAL);
            }
        };

        // Chạy ngay lập tức lần đầu tiên
        badgeHandler.post(badgeRunnable);
    }

    protected void stopBadgePolling() {
        if (badgeRunnable != null) {
            badgeHandler.removeCallbacks(badgeRunnable);
        }
    }
    private void updateNotificationBadgeNow() {
        if (currentNavView == null) return;

        RetrofitClient.getApi().getUnreadCount().enqueue(new Callback<UnreadCountResponse>() {
            @Override
            public void onResponse(Call<UnreadCountResponse> call, Response<UnreadCountResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    int count = response.body().getCount();
                    // Lấy Badge (An toàn: kiểm tra null)
                    if (currentNavView != null) {
                        BadgeDrawable badge = currentNavView.getOrCreateBadge(currentMenuItemId);
                        if (count > 0) {
                            badge.setVisible(true);
                            badge.setNumber(count);
                        } else {
                            badge.setVisible(false);
                        }
                    }
                }
            }
            @Override
            public void onFailure(Call<UnreadCountResponse> call, Throwable t) {}
        });
    }

}

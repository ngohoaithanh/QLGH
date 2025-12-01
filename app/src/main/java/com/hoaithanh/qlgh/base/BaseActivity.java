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
}

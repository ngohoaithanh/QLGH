package com.hoaithanh.qlgh.session;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {
    private static final String PREFS_NAME = "QLGH_PREFS";

    // Keys tương ứng với session bên PHP
    private static final String KEY_DANGNHAP = "dangnhap";     // boolean
    private static final String KEY_USER = "user";             // String (Username)
    private static final String KEY_USER_ID = "user_id";       // int
    private static final String KEY_ROLE = "role";             // int

    // Một số field tiện ích thêm (tùy backend bạn có trả không)
    private static final String KEY_TOKEN = "token";           // String
    private static final String KEY_PHONE = "phone";           // String

    private final SharedPreferences prefs;

    public SessionManager(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    // Lưu toàn bộ khi đăng nhập thành công
    public void saveLogin(boolean loggedIn, int userId, String username, int role, String token, String phone) {
        prefs.edit()
                .putBoolean(KEY_DANGNHAP, loggedIn)
                .putInt(KEY_USER_ID, userId)
                .putString(KEY_USER, username == null ? "" : username)
                .putInt(KEY_ROLE, role)
                .putString(KEY_TOKEN, token == null ? "" : token)
                .putString(KEY_PHONE, phone == null ? "" : phone)
                .apply();
    }

    // Getters
    public boolean isLoggedIn()            { return prefs.getBoolean(KEY_DANGNHAP, false); }
    public int getUserId()                 { return prefs.getInt(KEY_USER_ID, -1); }
    public String getUsername()            { return prefs.getString(KEY_USER, ""); }
    public int getRole()                   { return prefs.getInt(KEY_ROLE, 0); }
    public String getToken()               { return prefs.getString(KEY_TOKEN, ""); }
    public String getPhone()               { return prefs.getString(KEY_PHONE, ""); }

    // Cập nhật từng phần (nếu cần)
    public void setToken(String token)     { prefs.edit().putString(KEY_TOKEN, token == null ? "" : token).apply(); }
    public void setUsername(String name)   { prefs.edit().putString(KEY_USER, name == null ? "" : name).apply(); }

    // Đăng xuất
    public void logout() {
        prefs.edit().clear().apply();
    }
}
package com.hoaithanh.qlgh; // Đảm bảo đúng tên package

import android.app.Application;
import android.content.Context;

public class MainApplication extends Application {

    private static Context appContext;

    @Override
    public void onCreate() {
        super.onCreate();
        appContext = getApplicationContext();
    }

    public static Context getContext() {
        return appContext;
    }
}
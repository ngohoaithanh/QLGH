package com.hoaithanh.qlgh.database;

import android.content.Context;

import androidx.room.Room;

public class DatabaseHelper {
    private static AppDatabase instance;

    public static synchronized AppDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(context.getApplicationContext(),
                            AppDatabase.class, "qlgh_database")
                    .fallbackToDestructiveMigration()
                    .allowMainThreadQueries() // Chỉ dùng cho demo, nên dùng background thread
                    .build();
        }
        return instance;
    }
}
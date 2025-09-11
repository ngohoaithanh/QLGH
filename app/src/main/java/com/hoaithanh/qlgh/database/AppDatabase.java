package com.hoaithanh.qlgh.database;

import androidx.room.Database;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

import com.hoaithanh.qlgh.model.DonDatHang;

@Database(entities = {DonDatHang.class}, version = 2, exportSchema = false)
@TypeConverters({Converters.class})
public abstract class AppDatabase extends RoomDatabase {
    public abstract DonDatHangDAO donDatHangDAO();
}
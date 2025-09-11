package com.hoaithanh.qlgh.database;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import androidx.room.OnConflictStrategy;

import com.hoaithanh.qlgh.model.DonDatHang;

import java.util.List;

@Dao
public interface DonDatHangDAO {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(DonDatHang donDatHang);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<DonDatHang> donDatHangList);

    @Update
    void update(DonDatHang donDatHang);

    @Delete
    void delete(DonDatHang donDatHang);

    @Query("SELECT * FROM don_dat_hang ORDER BY Created_at DESC")
    List<DonDatHang> getAll();

    @Query("SELECT * FROM don_dat_hang WHERE ID = :id")
    DonDatHang getById(String id);

    @Query("SELECT * FROM don_dat_hang WHERE Status = :status ORDER BY Created_at DESC")
    List<DonDatHang> getByStatus(String status);

    @Query("UPDATE don_dat_hang SET Status = :status WHERE ID = :id")
    void updateStatus(String id, String status);

    @Query("DELETE FROM don_dat_hang")
    void deleteAll();

    @Query("SELECT COUNT(*) FROM don_dat_hang")
    int getCount();
}
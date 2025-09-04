package com.hoaithanh.qlgh.database;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.hoaithanh.qlgh.model.DonDatHang;

import java.util.List;

@Dao
public interface DonDatHangDAO {
    @Insert
    long insert(DonDatHang donDatHang);

    @Update
    void update(DonDatHang donDatHang);

    @Delete
    void delete(DonDatHang donDatHang);

    @Query("SELECT * FROM don_dat_hang ORDER BY thoiGianTao DESC")
    List<DonDatHang> getAll();

    @Query("SELECT * FROM don_dat_hang WHERE id = :id")
    DonDatHang getById(int id);

    @Query("SELECT * FROM don_dat_hang WHERE maDonHang = :maDonHang")
    DonDatHang getByMaDonHang(String maDonHang);

    @Query("SELECT * FROM don_dat_hang WHERE trangThai = :trangThai ORDER BY thoiGianTao DESC")
    List<DonDatHang> getByTrangThai(String trangThai);

    @Query("UPDATE don_dat_hang SET trangThai = :trangThai WHERE id = :id")
    void updateTrangThai(int id, String trangThai);

    @Query("SELECT COUNT(*) FROM don_dat_hang")
    int getCount();
}

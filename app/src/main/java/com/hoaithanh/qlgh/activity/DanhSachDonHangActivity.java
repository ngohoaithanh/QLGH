package com.hoaithanh.qlgh.activity;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.widget.Toast;

import com.hoaithanh.qlgh.R;
import com.hoaithanh.qlgh.adapter.DonHangAdapter;
import com.hoaithanh.qlgh.database.DatabaseHelper;
import com.hoaithanh.qlgh.model.DonDatHang;

import java.util.List;

public class DanhSachDonHangActivity extends AppCompatActivity {

    private RecyclerView rvDonHang;
    private DonHangAdapter donHangAdapter;
    private DatabaseHelper databaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_danh_sach_don_hang);

        initViews();
        loadDonHang();
    }

    private void initViews() {
        rvDonHang = findViewById(R.id.rv_don_hang);
        rvDonHang.setLayoutManager(new LinearLayoutManager(this));

        donHangAdapter = new DonHangAdapter(null);
        rvDonHang.setAdapter(donHangAdapter);
    }

    private void loadDonHang() {
        new Thread(() -> {
            try {
                List<DonDatHang> donHangList = DatabaseHelper.getInstance(this)
                        .donDatHangDAO()
                        .getAll();

                runOnUiThread(() -> {
                    if (donHangList != null && !donHangList.isEmpty()) {
                        donHangAdapter.updateData(donHangList);
                    } else {
                        Toast.makeText(this, "Chưa có đơn hàng nào", Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    Toast.makeText(this, "Lỗi khi tải dữ liệu: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadDonHang(); // Tải lại dữ liệu khi quay lại màn hình
    }
}
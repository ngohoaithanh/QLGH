package com.hoaithanh.qlgh.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.hoaithanh.qlgh.R;
import com.hoaithanh.qlgh.model.DonDatHang;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class DonHangAdapter extends RecyclerView.Adapter<DonHangAdapter.DonHangViewHolder> {

    private List<DonDatHang> donHangList;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());

    public DonHangAdapter(List<DonDatHang> donHangList) {
        this.donHangList = donHangList;
    }

    public void updateData(List<DonDatHang> newDonHangList) {
        this.donHangList = newDonHangList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public DonHangViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_don_hang, parent, false);
        return new DonHangViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DonHangViewHolder holder, int position) {
        DonDatHang donHang = donHangList.get(position);
        holder.bind(donHang);
    }

    @Override
    public int getItemCount() {
        return donHangList != null ? donHangList.size() : 0;
    }

    public static class DonHangViewHolder extends RecyclerView.ViewHolder {
        private TextView tvMaDon, tvNguoiNhan, tvDiaChi, tvTrangThai, tvTongTien, tvThoiGian;

        public DonHangViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMaDon = itemView.findViewById(R.id.tv_ma_don);
            tvNguoiNhan = itemView.findViewById(R.id.tv_nguoi_nhan);
            tvDiaChi = itemView.findViewById(R.id.tv_dia_chi);
            tvTrangThai = itemView.findViewById(R.id.tv_trang_thai);
            tvTongTien = itemView.findViewById(R.id.tv_tong_tien);
            tvThoiGian = itemView.findViewById(R.id.tv_thoi_gian);
        }

        public void bind(DonDatHang donHang) {
            tvMaDon.setText("Mã đơn: " + donHang.getMaDonHang());
            tvNguoiNhan.setText("Người nhận: " + donHang.getTenNguoiNhan());
            tvDiaChi.setText("Địa chỉ: " + donHang.getDiaChiNguoiNhan());
            tvTrangThai.setText("Trạng thái: " + donHang.getTrangThai());
            tvTongTien.setText("Tổng tiền: " + String.format("%,dđ", donHang.getTongTien()));

            SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
            tvThoiGian.setText("Thời gian: " + dateFormat.format(donHang.getThoiGianTao()));
        }
    }
}

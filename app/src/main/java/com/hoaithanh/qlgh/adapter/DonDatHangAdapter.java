package com.hoaithanh.qlgh.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.hoaithanh.qlgh.R;
import com.hoaithanh.qlgh.model.DonDatHang;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class DonDatHangAdapter extends RecyclerView.Adapter<DonDatHangAdapter.DonDatHangViewHolder> {

    private final List<DonDatHang> donDatHangList = new ArrayList<>();
    private OnItemClickListener listener;

    // --- Interface để xử lý sự kiện click ---
    public interface OnItemClickListener {
        void onItemClick(DonDatHang order);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    // --- Hàm cập nhật danh sách ---
    public void submitList(List<DonDatHang> newList) {
        if (newList == null) newList = Collections.emptyList();
        DiffUtil.DiffResult diff = DiffUtil.calculateDiff(new DiffCallback(this.donDatHangList, newList));
        this.donDatHangList.clear();
        this.donDatHangList.addAll(newList);
        diff.dispatchUpdatesTo(this);
    }

    @NonNull
    @Override
    public DonDatHangViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_don_hang, parent, false);
        return new DonDatHangViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DonDatHangViewHolder holder, int position) {
        DonDatHang item = donDatHangList.get(position);
        holder.bind(item, listener);
    }

    @Override
    public int getItemCount() {
        return donDatHangList.size();
    }

    // ---------- ViewHolder ----------
    static class DonDatHangViewHolder extends RecyclerView.ViewHolder {
        TextView tvMaDon, tvNguoiNhan, tvDiaChi, tvThoiGian, tvTrangThai, tvTongTien;

        DonDatHangViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMaDon = itemView.findViewById(R.id.tv_ma_don);
            tvNguoiNhan = itemView.findViewById(R.id.tv_nguoi_nhan);
            tvDiaChi = itemView.findViewById(R.id.tv_dia_chi);
            tvThoiGian = itemView.findViewById(R.id.tv_thoi_gian);
            tvTrangThai = itemView.findViewById(R.id.tv_trang_thai);
            tvTongTien = itemView.findViewById(R.id.tv_tong_tien);
        }

        void bind(final DonDatHang d, final OnItemClickListener listener) {
            tvMaDon.setText("Mã đơn: " + safe(d.getID()));
            tvNguoiNhan.setText("Người nhận: " + safe(d.getRecipient()));
            tvDiaChi.setText("Giao đến: " + safe(d.getDelivery_address()));
            tvThoiGian.setText("Ngày tạo: " + formatDate(d.getCreated_at()));

            String status = safe(d.getStatus());
            tvTrangThai.setText(getStatusText(status));
            setStatusColor(status);

            // --- SỬA LỖI NumberFormatException ---
            double shippingFee = 0.0;
            double codFee = 0.0;
            try {
                shippingFee = Double.parseDouble(safe(d.getShippingfee()));
            } catch (NumberFormatException ignored) {}
            try {
                codFee = Double.parseDouble(safe(d.getCODFee()));
            } catch (NumberFormatException ignored) {}

            double tongTien = shippingFee + codFee;
            tvTongTien.setText("Tổng phí: " + String.format(Locale.US, "%,.0f đ", tongTien));

            // --- Xử lý sự kiện click qua listener ---
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onItemClick(d);
                }
            });
        }

        // ... (các hàm phụ trợ như getStatusText, setStatusColor, formatDate... giữ nguyên)
        private String getStatusText(String status) {
            switch (status) {
                case "pending": return "Đang chờ xử lý";
                case "accepted": return "Đã chấp nhận";
                case "picked_up": return "Đã lấy hàng";
                case "in_transit": return "Đang vận chuyển";
                case "delivered": return "Đã giao hàng";
                case "delivery_failed": return "Giao hàng thất bại";
                case "cancelled": return "Đã hủy";
                default: return "Không xác định";
            }
        }

        private void setStatusColor(String status) {
            int colorRes;
            switch (status) {
                case "pending": colorRes = R.color.status_pending; break;
                case "accepted": colorRes = R.color.status_accepted; break;
                case "picked_up": colorRes = R.color.status_picked_up; break;
                case "in_transit": colorRes = R.color.status_in_transit; break;
                case "delivered": colorRes = R.color.status_delivered; break;
                case "delivery_failed": colorRes = R.color.status_delivery_failed; break;
                case "cancelled": colorRes = R.color.status_cancelled; break;
                default: colorRes = android.R.color.darker_gray; break;
            }
            tvTrangThai.setTextColor(ContextCompat.getColor(itemView.getContext(), colorRes));
        }

        private String formatDate(String dateString) {
            if (dateString == null || dateString.trim().isEmpty()) return "";
            try {
                SimpleDateFormat input = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                Date d = input.parse(dateString);
                SimpleDateFormat out = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
                return d != null ? out.format(d) : dateString;
            } catch (Exception e) {
                return dateString;
            }
        }

        private static String safe(String s) { return s == null ? "" : s; }
    }

    // ---------- DiffUtil ----------
    static class DiffCallback extends DiffUtil.Callback {
        private final List<DonDatHang> oldL, newL;
        DiffCallback(List<DonDatHang> oldL, List<DonDatHang> newL) { this.oldL = oldL; this.newL = newL; }
        @Override public int getOldListSize() { return oldL.size(); }
        @Override public int getNewListSize() { return newL.size(); }
        @Override public boolean areItemsTheSame(int oldP, int newP) {
            return Objects.equals(oldL.get(oldP).getID(), newL.get(newP).getID());
        }
        @Override public boolean areContentsTheSame(int oldP, int newP) {
            return oldL.get(oldP).equals(newL.get(newP)); // Giả sử DonDatHang có hàm equals()
        }
    }
}
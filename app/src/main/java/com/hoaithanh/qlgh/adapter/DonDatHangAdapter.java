package com.hoaithanh.qlgh.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
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
        LinearLayout layoutCodAmount;
        TextView tvCodAmount, tvShippingFeeDisplay, tvNetAmount, tvNetAmountLabel;

        DonDatHangViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMaDon = itemView.findViewById(R.id.tv_ma_don);
            tvNguoiNhan = itemView.findViewById(R.id.tv_nguoi_nhan);
            tvDiaChi = itemView.findViewById(R.id.tv_dia_chi);
            tvThoiGian = itemView.findViewById(R.id.tv_thoi_gian);
            tvTrangThai = itemView.findViewById(R.id.tv_trang_thai);
            tvCodAmount = itemView.findViewById(R.id.tv_cod_amount);
            layoutCodAmount = itemView.findViewById(R.id.layout_cod_amount);
            tvCodAmount = itemView.findViewById(R.id.tv_cod_amount);
            tvShippingFeeDisplay = itemView.findViewById(R.id.tv_shipping_fee_display);
            tvNetAmount = itemView.findViewById(R.id.tv_net_amount);
            tvNetAmountLabel = itemView.findViewById(R.id.tv_net_amount_label);

        }

        void bind(final DonDatHang d, final OnItemClickListener listener) {
            // --- Phần bind dữ liệu cơ bản (giữ nguyên) ---
            tvMaDon.setText("Mã đơn: " + safe(d.getID()));
            tvNguoiNhan.setText("Người nhận: " + safe(d.getRecipient()));
            tvDiaChi.setText("Giao đến: " + safe(d.getDelivery_address()));
            tvThoiGian.setText("Ngày tạo: " + formatDate(d.getCreated_at())); // Giả sử bạn có hàm formatDate

            String status = safe(d.getStatus());
            tvTrangThai.setText(getStatusText(status)); // Giả sử bạn có hàm getStatusText
            setStatusColor(status); // Giả sử bạn có hàm setStatusColor

            // --- Xử lý Phí và Tính toán số tiền Thực nhận ---
            double codValue = parseD(d.getCOD_amount());
            double shippingFee = parseD(d.getShippingfee());
            String feePayer = safe(d.getFee_payer()).toLowerCase();

            if (codValue > 0) {
                // --- KỊCH BẢN 1: CÓ COD (BÁN HÀNG) ---
                layoutCodAmount.setVisibility(View.VISIBLE);
                tvCodAmount.setText(formatCurrencyVND(String.valueOf(codValue)));
                tvNetAmountLabel.setText("Thực nhận:");

                double netAmount;
                if ("sender".equals(feePayer)) {
                    // Người gửi trả phí: Thực nhận = Tiền COD - Phí vận chuyển
                    netAmount = codValue - shippingFee;
                    tvShippingFeeDisplay.setText("-" + formatCurrencyVND(String.valueOf(shippingFee)));
                } else { // Người nhận trả phí
                    // Người nhận trả phí: Thực nhận = Toàn bộ tiền COD
                    netAmount = codValue;
                    tvShippingFeeDisplay.setText("Người nhận trả");
                }
                tvNetAmount.setText(formatCurrencyVND(String.valueOf(netAmount)));

            } else {
                // --- KỊCH BẢN 2: KHÔNG CÓ COD (GỬI ĐỒ) ---
                layoutCodAmount.setVisibility(View.GONE);
                tvNetAmountLabel.setText("Tổng phí:");

                if ("sender".equals(feePayer)) {
                    // Người gửi trả phí: Hiển thị phí phải trả
                    tvShippingFeeDisplay.setText(formatCurrencyVND(String.valueOf(shippingFee)));
                    tvNetAmount.setText(formatCurrencyVND(String.valueOf(shippingFee)));
                } else { // Người nhận trả phí
                    // Người gửi không phải trả gì cả
                    tvShippingFeeDisplay.setText("Người nhận trả");
                    tvNetAmount.setText("0 ₫");
                }
            }

            // --- Xử lý sự kiện click (giữ nguyên) ---
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onItemClick(d);
                }
            });
        }

        private String formatCurrencyVND(String amount) {
            if (amount == null || amount.trim().isEmpty()) {
                return "0 ₫";
            }
            try {
                double value = Double.parseDouble(amount.trim());
                Locale localeVN = new Locale("vi", "VN");
                NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(localeVN);
                return currencyFormatter.format(value);
            } catch (NumberFormatException e) {
                // Trả về chuỗi gốc nếu không thể định dạng
                return amount + " ₫";
            }
        }

        private static double parseD(String s) {
            if (s == null || s.trim().isEmpty()) {
                return 0.0;
            }
            try {
                return Double.parseDouble(s.trim());
            } catch (NumberFormatException e) {
                return 0.0;
            }
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
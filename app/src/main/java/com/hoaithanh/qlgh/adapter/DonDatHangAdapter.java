package com.hoaithanh.qlgh.adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.hoaithanh.qlgh.R;
import com.hoaithanh.qlgh.activity.ChiTietDonHangActivity;
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

    public void submit(List<DonDatHang> newList) {
        if (newList == null) newList = Collections.emptyList();
        DiffUtil.DiffResult diff = DiffUtil.calculateDiff(new DiffCallback(this.donDatHangList, newList));
        this.donDatHangList.clear();
        this.donDatHangList.addAll(newList);
        diff.dispatchUpdatesTo(this);
    }

    @Override
    public long getItemId(int position) {
        // ổn định ID để cuộn mượt (nếu ID là số, bạn có thể parseLong an toàn hơn)
        String id = DonDatHangViewHolder.safe(donDatHangList.get(position).getID());
        return id.hashCode();
    }

    public DonDatHangAdapter() {
        setHasStableIds(true);
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
        holder.bind(item);
//        holder.bind(donDatHangList.get(position));
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

        void bind(DonDatHang d) {
            String id = safe(d.getID());
            String recipient = safe(d.getRecipient());
            String phone = safe(d.getRecipientPhone());
            String address = safe(d.getDelivery_address());
            String createdAt = safe(d.getCreated_at());
            String status = safe(d.getStatus());
            String cod = safe(d.getCOD_amount()); // nếu muốn tổng = COD + phí ship, có thể cộng thêm

            tvMaDon.setText("Mã đơn: " + id);
            tvNguoiNhan.setText("Người nhận: " + recipient + (phone.isEmpty() ? "" : " - " + phone));
            tvDiaChi.setText("Giao đến: " + address);
            tvThoiGian.setText("Ngày tạo: " + formatDate(createdAt));

            tvTrangThai.setText(getStatusText(status));
            setStatusColor(status);

            double shippingFee = Double.parseDouble(d.getShippingfee());
            double codFee = Double.parseDouble(d.getCODFee());

            double tongTien = shippingFee + codFee;
            tvTongTien.setText("Tổng phí: " + String.format("%,.0f đ", tongTien));
//            tvTongTien.setText("Tổng: " + formatCurrencyVND(cod));

            itemView.setOnClickListener(v -> {
                Context ctx = v.getContext();
                Intent intent = new Intent(ctx, ChiTietDonHangActivity.class);
                intent.putExtra("ID", d.getID());
                ctx.startActivity(intent);
            });
        }

        private String getStatusText(String status) {
            switch (status) {
                case "pending":         return "Đang chờ xử lý";
                case "accepted":        return "Đã chấp nhận";
                case "picked_up":       return "Đã lấy hàng";
                case "in_transit":      return "Đang vận chuyển";
                case "delivered":       return "Đã giao hàng";
                case "delivery_failed": return "Giao hàng thất bại";
                case "cancelled":       return "Đã hủy";
                default:                return status; // fallback
            }
        }

        private void setStatusColor(String status) {
            Context ctx = itemView.getContext();
            int colorRes;
            switch (status) {
                case "pending":         colorRes = R.color.status_pending;         break;
                case "accepted":        colorRes = R.color.status_accepted;        break;
                case "picked_up":       colorRes = R.color.status_picked_up;       break;
                case "in_transit":      colorRes = R.color.status_in_transit;      break;
                case "delivered":       colorRes = R.color.status_delivered;       break;
                case "delivery_failed": colorRes = R.color.status_delivery_failed; break;
                case "cancelled":       colorRes = R.color.status_cancelled;       break;
                default:                colorRes = android.R.color.black;          break;
            }
            tvTrangThai.setTextColor(ContextCompat.getColor(ctx, colorRes));
        }

        private String formatDate(String dateString) {
            if (dateString == null || dateString.trim().isEmpty()) return "";
            try {
                SimpleDateFormat input = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                input.setLenient(false);
                Date d = input.parse(dateString);
                SimpleDateFormat out = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
                return d != null ? out.format(d) : dateString;
            } catch (Exception e) {
                return dateString;
            }
        }

        private String formatCurrencyVND(String amount) {
            try {
                double value = Double.parseDouble(amount);
                NumberFormat nf = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
                return nf.format(value);
            } catch (Exception e) {
                return amount + " ₫";
            }
        }

        private static String safe(String s) { return s == null ? "" : s; }
    }

    // ---------- DiffUtil ----------
    static class DiffCallback extends DiffUtil.Callback {
        private final List<DonDatHang> oldL, newL;
        DiffCallback(List<DonDatHang> oldL, List<DonDatHang> newL) {
            this.oldL = oldL; this.newL = newL;
        }
        @Override public int getOldListSize() { return oldL.size(); }
        @Override public int getNewListSize() { return newL.size(); }
        @Override public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            String oldId = safe(oldL.get(oldItemPosition).getID());
            String newId = safe(newL.get(newItemPosition).getID());
            return oldId.equals(newId);
        }
        @Override public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            DonDatHang o = oldL.get(oldItemPosition);
            DonDatHang n = newL.get(newItemPosition);
            // đơn giản: so sánh chuỗi JSON-hóa hoặc vài field quan trọng
            return Objects.equals(safe(o.getRecipient()), safe(n.getRecipient()))
                    && Objects.equals(safe(o.getStatus()), safe(n.getStatus()))
                    && Objects.equals(safe(o.getCOD_amount()), safe(n.getCOD_amount()))
                    && Objects.equals(safe(o.getDelivery_address()), safe(n.getDelivery_address()))
                    && Objects.equals(safe(o.getCreated_at()), safe(n.getCreated_at()));
        }
        private static String safe(String s) { return s == null ? "" : s; }
    }
}
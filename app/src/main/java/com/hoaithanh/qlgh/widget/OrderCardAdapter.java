package com.hoaithanh.qlgh.widget;

import android.view.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.hoaithanh.qlgh.R;
import com.hoaithanh.qlgh.model.DonDatHang;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class OrderCardAdapter extends RecyclerView.Adapter<OrderCardAdapter.VH> {

    public interface OnOrderClick { void onClick(DonDatHang order); }
    private final OnOrderClick cb;
    private final List<DonDatHang> data = new ArrayList<>();

    public OrderCardAdapter(OnOrderClick cb){ this.cb = cb; }

    public void submit(List<DonDatHang> list){
        data.clear();
        if (list != null) data.addAll(list);
        notifyDataSetChanged();
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_my_order, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int i) {
        DonDatHang o = data.get(i);
        h.tvOrderId.setText("#" + o.getID());
        h.tvStatus.setText(StatusUtil.pretty(o.getStatus()));
        h.tvPickup.setText(o.getPick_up_address());
        h.tvDelivery.setText(o.getDelivery_address());
        h.tvShippingFee.setText("Phí VC: " + formatCurrencyVN(o.getShippingfee()));
        double codValue = parseDouble(o.getCOD_amount()); // Cần hàm parseDouble
        if (codValue > 0) {
            h.tvCodAmount.setText("Ứng COD: " + formatCurrencyVN(o.getCOD_amount()));
            h.tvCodAmount.setVisibility(View.VISIBLE);
        } else {
            h.tvCodAmount.setVisibility(View.GONE);
        }

        String when = (o.getAccepted_at()!=null && !o.getAccepted_at().isEmpty())
                ? o.getAccepted_at()
                : (o.getCreated_at()==null ? "--" : o.getCreated_at());
        h.tvTime.setText(when);

        View.OnClickListener go = v -> cb.onClick(o);
        h.itemView.setOnClickListener(go);
        h.btnAction.setOnClickListener(go);

        // tô màu chip trạng thái nhẹ nhàng:
        String st = o.getStatus() == null ? "" : o.getStatus().toLowerCase();
        int bg = R.drawable.bg_dot_grey;
        if ("delivered".equals(st)) bg = R.drawable.bg_dot_green;
        else if ("in_transit".equals(st) || "picked_up".equals(st) || "accepted".equals(st)) bg = R.color.colorPrimary;
        else if ("delivery_failed".equals(st) || "cancelled".equals(st)) bg = R.color.red;
        h.tvStatus.setBackgroundResource(bg);

        // Kiểm tra xem đơn hàng có được đánh giá không
        if (o.getRatingValue() != null && o.getRatingValue() > 0) {
            // Nếu có, gán số sao và hiển thị layout
            h.tvOrderRating.setText(String.valueOf(o.getRatingValue()));
            h.ratingContainer.setVisibility(View.VISIBLE);
        } else {
            // Quan trọng: Phải ẩn đi nếu không có, vì RecyclerView sẽ tái sử dụng view
            h.ratingContainer.setVisibility(View.GONE);
        }
    }

    private String formatCurrencyVN(String amount) {
        if (amount == null || amount.trim().isEmpty()) return "0đ";
        try {
            double value = Double.parseDouble(amount);
            Locale localeVN = new Locale("vi", "VN");
            NumberFormat currencyVN = NumberFormat.getCurrencyInstance(localeVN);
            return currencyVN.format(value);
        } catch (Exception e) {
            return amount + " ₫";
        }
    }

    private double parseDouble(String s) {
        if (s == null || s.trim().isEmpty()) {
            return 0.0;
        }
        try {
            return Double.parseDouble(s.trim());
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    @Override public int getItemCount() { return data.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvOrderId, tvStatus, tvPickup, tvDelivery, tvTime, tvShippingFee;
        Button btnAction;
        TextView tvCodAmount;
        LinearLayout ratingContainer;
        TextView tvOrderRating;
        VH(@NonNull View v){
            super(v);
            tvOrderId = v.findViewById(R.id.tvOrderId);
            tvStatus = v.findViewById(R.id.tvStatus);
            tvPickup = v.findViewById(R.id.tvPickup);
            tvDelivery = v.findViewById(R.id.tvDelivery);
            tvShippingFee = v.findViewById(R.id.tvShippingFee);
            tvTime = v.findViewById(R.id.tvTime);
            btnAction = v.findViewById(R.id.btnAction);
            ratingContainer = v.findViewById(R.id.ratingContainer);
            tvOrderRating = v.findViewById(R.id.tvOrderRating);
            tvCodAmount = v.findViewById(R.id.tvCodAmount);
        }
    }


    private static String safeMoney(String s){
        if (s == null || s.trim().isEmpty()) return "0";
        return s;
    }


}

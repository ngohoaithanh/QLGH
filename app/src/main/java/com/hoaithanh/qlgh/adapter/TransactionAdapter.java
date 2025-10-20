package com.hoaithanh.qlgh.adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.hoaithanh.qlgh.R;
import com.hoaithanh.qlgh.model.Transaction;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder> {

    private final List<Transaction> transactionList = new ArrayList<>();

    public void submitList(List<Transaction> newList) {
        transactionList.clear();
        if (newList != null) {
            transactionList.addAll(newList);
        }
        notifyDataSetChanged(); // Cập nhật toàn bộ danh sách
    }

    @NonNull
    @Override
    public TransactionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_transaction_history, parent, false);
        return new TransactionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TransactionViewHolder holder, int position) {
        holder.bind(transactionList.get(position));
    }

    @Override
    public int getItemCount() {
        return transactionList.size();
    }

    // --- ViewHolder ---
    static class TransactionViewHolder extends RecyclerView.ViewHolder {
        ImageView ivIcon;
        TextView tvDescription, tvTimestamp, tvAmount;
        Context context;

        public TransactionViewHolder(@NonNull View itemView) {
            super(itemView);
            context = itemView.getContext();
            ivIcon = itemView.findViewById(R.id.ivTransactionIcon);
            tvDescription = itemView.findViewById(R.id.tvTransactionDescription);
            tvTimestamp = itemView.findViewById(R.id.tvTransactionTimestamp);
            tvAmount = itemView.findViewById(R.id.tvTransactionAmount);
        }

        void bind(Transaction transaction) {
            String type = transaction.getType() != null ? transaction.getType() : "";
            double amount = transaction.getAmount();
            String timestamp = transaction.getCreatedAt();
            Integer orderId = transaction.getOrderId();

            // 1. Set Icon và Mô tả dựa trên Loại giao dịch
            int iconRes = R.drawable.ic_income; // Icon mặc định
            String description = "Giao dịch không xác định";
            boolean isIncomeOrExpense = false; // Biến mới để xác định có tô màu xanh/đỏ không
            boolean isPositiveAmount = true;   // Mặc định hiển thị dấu cộng

            switch (type) {
                case "shipping_fee":
                    iconRes = R.drawable.ic_income;
                    description = "Thu phí ship";
                    if (orderId != null) description += " đơn #" + orderId;
                    isIncomeOrExpense = true;
                    isPositiveAmount = true;
                    break;
                case "bonus": // Nếu có
                    iconRes = R.drawable.ic_income;
                    description = "Tiền thưởng";
                    isIncomeOrExpense = true;
                    isPositiveAmount = true;
                    break;
                case "collect_cod":
                    iconRes = R.drawable.ic_cod_collect;
                    description = "Thu tiền mặt (COD+Phí)"; // Mô tả rõ hơn
                    if (orderId != null) description += " đơn #" + orderId;
                    isIncomeOrExpense = false; // Không phải thu nhập/chi phí trực tiếp
                    isPositiveAmount = true;   // Hiển thị là tiền shipper nhận vào tay
                    break;
                case "deposit_cod":
                    iconRes = R.drawable.ic_cod_deposit;
                    description = "Nộp tiền COD/Phí"; // Mô tả rõ hơn
                    isIncomeOrExpense = false;
                    isPositiveAmount = false;  // Hiển thị là tiền shipper nộp đi
                    break;
                case "withdraw":
                    iconRes = R.drawable.ic_expense;
                    description = "Rút tiền";
                    isIncomeOrExpense = true;
                    isPositiveAmount = false;
                    break;
                case "penalty": // Nếu có
                    iconRes = R.drawable.ic_expense;
                    description = "Phí phạt";
                    isIncomeOrExpense = true;
                    isPositiveAmount = false;
                    break;
            }
            ivIcon.setImageResource(iconRes);
            tvDescription.setText(description);

// 2. Set Thời gian (Giữ nguyên)
            tvTimestamp.setText(formatDate(timestamp));

// 3. Set Số tiền và Màu sắc (Logic mới)
            String amountString = formatCurrencyVN(String.valueOf(amount));
            if (isPositiveAmount) {
                tvAmount.setText("+" + amountString);
            } else {
                tvAmount.setText("-" + amountString);
            }

// Chỉ tô màu xanh/đỏ cho Thu nhập/Chi phí thực sự
            if (isIncomeOrExpense) {
                if (isPositiveAmount) {
                    tvAmount.setTextColor(ContextCompat.getColor(context, R.color.main_route_color)); // Xanh lá
                } else {
                    tvAmount.setTextColor(Color.RED); // Đỏ
                }
            } else {
                // Màu đen/xám cho các giao dịch tiền mặt COD
                tvAmount.setTextColor(Color.BLACK); // Hoặc màu xám
            }
        }

        // --- Hàm tiện ích (Copy từ Adapter khác nếu có) ---
        private String formatDate(String dateString) {
            if (dateString == null || dateString.trim().isEmpty()) return "";
            try {
                SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                Date date = inputFormat.parse(dateString);
                SimpleDateFormat outputFormat = new SimpleDateFormat("HH:mm - dd/MM/yyyy", Locale.getDefault());
                return date != null ? outputFormat.format(date) : dateString;
            } catch (Exception e) {
                return dateString;
            }
        }

        private String formatCurrencyVN(String amount) {
            if (amount == null || amount.trim().isEmpty()) return "0đ";
            try {
                double value = Double.parseDouble(amount.trim());
                Locale localeVN = new Locale("vi", "VN");
                NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(localeVN);
                // Bỏ ký hiệu tiền tệ nếu muốn gọn hơn
                // currencyFormatter.setCurrency(Currency.getInstance("VND"));
                // return currencyFormatter.format(value).replace(" VND", "đ");
                return currencyFormatter.format(value);
            } catch (NumberFormatException e) {
                return amount + " ₫";
            }
        }
    }
}
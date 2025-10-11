package com.hoaithanh.qlgh.adapter;

import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.hoaithanh.qlgh.R;
import com.hoaithanh.qlgh.model.Tracking;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TrackingHistoryAdapter extends RecyclerView.Adapter<TrackingHistoryAdapter.TrackingViewHolder> {

    private final List<Tracking> trackingList = new ArrayList<>();

    public void submitList(List<Tracking> newList) {
        trackingList.clear();
        if (newList != null) {
            trackingList.addAll(newList);
        }
        // Thông báo cho RecyclerView rằng dữ liệu đã thay đổi hoàn toàn
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TrackingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_tracking_timeline, parent, false);
        return new TrackingViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TrackingViewHolder holder, int position) {
        // Truyền cả vị trí và tổng số item để ViewHolder tự xử lý giao diện
        holder.bind(trackingList.get(position), position, getItemCount());
    }

    @Override
    public int getItemCount() {
        return trackingList.size();
    }

    // --- ViewHolder ---
    static class TrackingViewHolder extends RecyclerView.ViewHolder {
        ImageView ivDot;
        View lineTop, lineBottom;
        TextView tvStatus, tvTimestamp;
        Context context;

        public TrackingViewHolder(@NonNull View itemView) {
            super(itemView);
            context = itemView.getContext();
            ivDot = itemView.findViewById(R.id.ivTimelineDot);
            lineTop = itemView.findViewById(R.id.viewLineTop);
            lineBottom = itemView.findViewById(R.id.viewLineBottom);
            tvStatus = itemView.findViewById(R.id.tvTrackingStatus);
            tvTimestamp = itemView.findViewById(R.id.tvTrackingTimestamp);
        }

        void bind(Tracking tracking, int position, int totalItems) {
            tvStatus.setText(tracking.getStatus());
            tvTimestamp.setText(formatDate(tracking.getUpdatedAt()));

            // Item đầu tiên (mới nhất) sẽ được làm nổi bật
            if (position == 0) {
                lineTop.setVisibility(View.INVISIBLE); // Ẩn đường kẻ phía trên
                ivDot.setImageResource(R.drawable.bg_timeline_dot_active); // Chấm tròn màu xanh
                tvStatus.setTypeface(null, Typeface.BOLD);
                tvStatus.setTextColor(ContextCompat.getColor(context, R.color.main_route_color));
            } else {
                lineTop.setVisibility(View.VISIBLE);
                ivDot.setImageResource(R.drawable.bg_timeline_dot_normal); // Chấm tròn xám
                tvStatus.setTypeface(null, Typeface.NORMAL);
                tvStatus.setTextColor(ContextCompat.getColor(context, android.R.color.black));
            }

            // Item cuối cùng (cũ nhất)
            if (position == totalItems - 1) {
                lineBottom.setVisibility(View.INVISIBLE); // Ẩn đường kẻ phía dưới
            } else {
                lineBottom.setVisibility(View.VISIBLE);
            }
        }

        // Hàm định dạng ngày tháng
        private String formatDate(String dateString) {
            if (dateString == null || dateString.trim().isEmpty()) return "";
            try {
                // Định dạng đầu vào từ database: "yyyy-MM-dd HH:mm:ss"
                SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                Date date = inputFormat.parse(dateString);

                // Định dạng đầu ra mong muốn: "dd ThMM\nHH:mm"
                SimpleDateFormat outputFormat = new SimpleDateFormat("dd 'Th'MM\nHH:mm", new Locale("vi", "VN"));
                return date != null ? outputFormat.format(date) : dateString;
            } catch (Exception e) {
                e.printStackTrace();
                return dateString; // Trả về chuỗi gốc nếu có lỗi
            }
        }
    }
}
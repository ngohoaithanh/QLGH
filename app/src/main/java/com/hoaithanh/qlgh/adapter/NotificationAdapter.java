package com.hoaithanh.qlgh.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.hoaithanh.qlgh.R;
import com.hoaithanh.qlgh.model.Notification;

import java.util.ArrayList;
import java.util.List;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.VH> {
    private List<Notification> list = new ArrayList<>();

    public void submitList(List<Notification> data) {
        list.clear();
        if (data != null) list.addAll(data);
        notifyDataSetChanged();
    }

    public void addList(List<Notification> newData) {
        if (newData == null || newData.isEmpty()) return;

        int startPosition = list.size(); // Vị trí bắt đầu thêm
        list.addAll(newData);

        // Thông báo cho RecyclerView biết chỉ có các dòng mới được thêm vào
        notifyItemRangeInserted(startPosition, newData.size());
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notification, parent, false);
        return new VH(v);
    }


    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        Notification item = list.get(position);
        holder.tvTitle.setText(item.getTitle());
        holder.tvMessage.setText(item.getMessage());
        holder.tvTime.setText(item.getCreatedAt());
        // Có thể thêm sự kiện click để mở chi tiết đơn hàng dựa trên ReferenceID
    }

    @Override public int getItemCount() { return list.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvTitle, tvMessage, tvTime;
        public VH(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvNotiTitle);
            tvMessage = itemView.findViewById(R.id.tvNotiMessage);
            tvTime = itemView.findViewById(R.id.tvNotiTime);
        }
    }
}

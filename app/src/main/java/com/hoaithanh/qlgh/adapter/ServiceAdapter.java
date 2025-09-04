package com.hoaithanh.qlgh.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.hoaithanh.qlgh.R;
import com.hoaithanh.qlgh.model.ServiceItem;

import java.util.List;

public class ServiceAdapter extends RecyclerView.Adapter<ServiceAdapter.ServiceViewHolder> {

    private List<ServiceItem> serviceList;
    private OnItemClickListener listener;

    // Interface để xử lý sự kiện click
    public interface OnItemClickListener {
        void onItemClick(ServiceItem item);
    }

    public ServiceAdapter(List<ServiceItem> serviceList, OnItemClickListener listener) {
        this.serviceList = serviceList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ServiceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // "Thổi phồng" layout item_service.xml cho mỗi mục
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_service, parent, false);
        return new ServiceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ServiceViewHolder holder, int position) {
        // Lấy dữ liệu của mục hiện tại
        ServiceItem currentItem = serviceList.get(position);

        // Gán dữ liệu vào các view
        holder.serviceIcon.setImageResource(currentItem.getIconResource());
        holder.serviceName.setText(currentItem.getName());
        holder.serviceDescription.setText(currentItem.getDescription());

        // Thiết lập sự kiện click cho toàn bộ item
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(currentItem);
            }
        });
    }

    @Override
    public int getItemCount() {
        // Trả về tổng số mục trong danh sách
        return serviceList.size();
    }

    // ViewHolder: Lớp giữ các view của mỗi item
    public static class ServiceViewHolder extends RecyclerView.ViewHolder {
        public ImageView serviceIcon;
        public TextView serviceName;
        public TextView serviceDescription;

        public ServiceViewHolder(@NonNull View itemView) {
            super(itemView);
            // Ánh xạ các thành phần UI từ layout item_service.xml
            serviceIcon = itemView.findViewById(R.id.service_icon);
            serviceName = itemView.findViewById(R.id.service_name);
            serviceDescription = itemView.findViewById(R.id.service_description);
        }
    }
}

package com.hoaithanh.qlgh.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.hoaithanh.qlgh.R;
import com.hoaithanh.qlgh.activity.ShipperOrdersDetailActivity;
import com.hoaithanh.qlgh.model.DonDatHang;
import com.hoaithanh.qlgh.viewmodel.DonDatHangViewModel;
import com.hoaithanh.qlgh.widget.OrderCardAdapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class OrdersListFragment extends Fragment {

    private static final String ARG_COMPLETED = "completed";
    public static OrdersListFragment newInstance(boolean completed){
        Bundle b = new Bundle();
        b.putBoolean(ARG_COMPLETED, completed);
        OrdersListFragment f = new OrdersListFragment();
        f.setArguments(b);
        return f;
    }

    private boolean isCompletedTab;
    private DonDatHangViewModel vm;
    private SwipeRefreshLayout swipe;
    private RecyclerView rv;
    private View emptyView;
    private TextView tvEmpty;
    private OrderCardAdapter adapter;

    // Dữ liệu cho tab hiện tại
    private final List<DonDatHang> currentList = new ArrayList<>(); // đã lọc theo tab
    private final List<DonDatHang> displayed   = new ArrayList<>(); // sau khi filter + sort
    private String currentQuery = "";
    private int currentSort = 0; // 0 mới nhất, 1 cũ nhất, 2 COD desc, 3 COD asc
    private static final int REQUEST_UPDATE_ORDER = 101;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inf, @Nullable ViewGroup c, @Nullable Bundle s) {
        View v = inf.inflate(R.layout.fragment_orders_list, c, false);
        swipe = v.findViewById(R.id.swipe);
        rv = v.findViewById(R.id.rvOrders);
        emptyView = v.findViewById(R.id.emptyView);
        tvEmpty = v.findViewById(R.id.tvEmpty);
        return v;
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle s) {
        isCompletedTab = getArguments()!=null && getArguments().getBoolean(ARG_COMPLETED,false);
        vm = new ViewModelProvider(requireActivity()).get(DonDatHangViewModel.class);

        rv.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new OrderCardAdapter(order -> {
            Intent it = new Intent(getContext(), ShipperOrdersDetailActivity.class);
            it.putExtra("order", order);
            if (getActivity() != null) {
                getActivity().startActivityForResult(it, REQUEST_UPDATE_ORDER);
            }
//            startActivity(it);
        });
        rv.setAdapter(adapter);

        swipe.setOnRefreshListener(() -> vm.refreshMyOrders(requireContext()));

        vm.getMyOrders().observe(getViewLifecycleOwner(), all -> {
            // 1) Lọc theo tab (đã hoàn thành / chưa hoàn thành)
            currentList.clear();
            if (all != null) {
                for (DonDatHang d : all) {
                    String st = d.getStatus()==null ? "" : d.getStatus().toLowerCase();

                    boolean done =
                            "delivered".equals(st)
                                    || "delivery_failed".equals(st)
                                    || "cancelled".equals(st);

                    if (isCompletedTab == done) currentList.add(d);
                }
            }
            // 2) Áp dụng filter + sort hiện tại
            applyFiltersAndSort();
            swipe.setRefreshing(false);
        });

        // Nạp lần đầu
        vm.ensureFirstLoad(requireContext());
    }

    // ====== PUBLIC: gọi từ Activity ======
    public void filterOrders(String keyword){
        currentQuery = keyword == null ? "" : keyword.trim().toLowerCase();
        applyFiltersAndSort();
    }
    public void sortOrders(int option){
        currentSort = option;
        applyFiltersAndSort();
    }

    // ====== Core filter + sort ======
    private void applyFiltersAndSort(){
        // Filter theo query
        displayed.clear();
        if (currentQuery.isEmpty()) {
            displayed.addAll(currentList);
        } else {
            for (DonDatHang o : currentList) {
                String id   = safeLower(o.getID());
                String rec  = safeLower(o.getRecipient());
                String addr = safeLower(o.getDelivery_address());
                if (id.contains(currentQuery) || rec.contains(currentQuery) || addr.contains(currentQuery)) {
                    displayed.add(o);
                }
            }
        }

        // Sort theo lựa chọn
        Comparator<DonDatHang> cmp = null;
        switch (currentSort) {
            case 0: // Mới nhất
                cmp = (a,b) -> safe(a.getCreated_at()).compareTo(safe(b.getCreated_at())) < 0 ? 1 : -1;
                break;
            case 1: // Cũ nhất
                cmp = (a,b) -> safe(a.getCreated_at()).compareTo(safe(b.getCreated_at()));
                break;
            case 2: // COD cao -> thấp
                cmp = (a,b) -> Double.compare(b.getCodAmountAsDouble(), a.getCodAmountAsDouble());
                break;
            case 3: // COD thấp -> cao
                cmp = (a,b) -> Double.compare(a.getCodAmountAsDouble(), b.getCodAmountAsDouble());
                break;
        }
        if (cmp != null) Collections.sort(displayed, cmp);

        // Cập nhật UI
        adapter.submit(displayed);
        boolean empty = displayed.isEmpty();
        emptyView.setVisibility(empty ? View.VISIBLE : View.GONE);
        tvEmpty.setText(isCompletedTab ? "Chưa có đơn đã hoàn thành" : "Chưa có đơn đang thực hiện");
    }
    public void refreshData() {
        if (vm != null && getContext() != null) {
            // Hiển thị biểu tượng loading của SwipeRefreshLayout để người dùng biết
            if (swipe != null) {
                swipe.setRefreshing(true);
            }
            // Gọi lại hàm trong ViewModel để tải lại danh sách đơn hàng
            vm.refreshMyOrders(requireContext());
        }
    }

    private static String safe(String s){ return s == null ? "" : s; }
    private static String safeLower(String s){ return s == null ? "" : s.toLowerCase(); }
}


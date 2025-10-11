package com.hoaithanh.qlgh.activity;

import android.util.SparseArray;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import com.hoaithanh.qlgh.fragment.OrdersListFragment;

public class ShipperOrdersPagerAdapter extends FragmentStateAdapter {

    private final SparseArray<OrdersListFragment> refs = new SparseArray<>();

    public ShipperOrdersPagerAdapter(@NonNull FragmentActivity fa) {
        super(fa);
    }

    @NonNull @Override
    public Fragment createFragment(int position) {
        OrdersListFragment f = OrdersListFragment.newInstance(position == 1); // 1 = ĐÃ HOÀN THÀNH
        refs.put(position, f);
        return f;
    }

    @Override public int getItemCount() { return 2; }

    @Nullable
    public OrdersListFragment get(int position) { return refs.get(position); }
}

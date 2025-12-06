package com.hoaithanh.qlgh.activity;

import android.view.Menu;
import android.view.MenuItem;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.hoaithanh.qlgh.R;
import com.hoaithanh.qlgh.base.BaseActivity;
import com.hoaithanh.qlgh.fragment.OrdersListFragment;

public class ShipperMyOrdersActivity extends BaseActivity {

    private ViewPager2 viewPager;
    private TabLayout tabs;
    private ShipperOrdersPagerAdapter pagerAdapter;
    private BottomNavigationView bottomNavigationView;

    @Override
    public void initLayout() {
        setContentView(R.layout.activity_shipper_my_orders);
    }

    @Override
    public void initData() { /* no-op */ }

    @Override
    public void initView() {
        MaterialToolbar tb = findViewById(R.id.toolbar);
        setSupportActionBar(tb);
        tb.setNavigationOnClickListener(v -> onBackPressed());
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        setupBottomNavigation();
        viewPager = findViewById(R.id.viewPager);
        tabs = findViewById(R.id.tabs);

        pagerAdapter = new ShipperOrdersPagerAdapter(this);
        viewPager.setAdapter(pagerAdapter);

        new TabLayoutMediator(tabs, viewPager, (tab, pos) ->
                tab.setText(pos == 0 ? "Chưa hoàn thành" : "Đã hoàn thành")
        ).attach();
    }

    // ===== Toolbar: Search + Sort =====
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_my_orders, menu);

        MenuItem searchItem = menu.findItem(R.id.action_search);
        SearchView sv = (SearchView) searchItem.getActionView();
        sv.setQueryHint("Tìm mã đơn, tên người nhận, địa chỉ...");
        sv.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override public boolean onQueryTextSubmit(String q) { return false; }
            @Override public boolean onQueryTextChange(String newText) {
                OrdersListFragment f = getCurrentOrdersFragment();
                if (f != null) f.filterOrders(newText);
                return true;
            }
        });
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_sort) {
            String[] options = {"Mới nhất", "Cũ nhất", "COD cao → thấp", "COD thấp → cao"};
            new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                    .setTitle("Sắp xếp theo")
                    .setItems(options, (d, which) -> {
                        OrdersListFragment f = getCurrentOrdersFragment();
                        if (f != null) f.sortOrders(which);
                    }).show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Kiểm tra xem đây có phải là kết quả trả về từ màn hình chi tiết không
        // và kết quả có phải là OK (nghĩa là dữ liệu đã thay đổi) không.
        // Con số 101 phải khớp với hằng số REQUEST_UPDATE_ORDER bạn đã tạo trong Fragment.
        if (requestCode == 101 && resultCode == RESULT_OK) {

            // ViewPager2 tự động tạo tag cho các fragment theo dạng "f" + vị trí
            // Chúng ta sẽ dùng tag này để tìm lại các fragment
            OrdersListFragment fragChuaHoanThanh = (OrdersListFragment) getSupportFragmentManager()
                    .findFragmentByTag("f" + 0);
            OrdersListFragment fragDaHoanThanh = (OrdersListFragment) getSupportFragmentManager()
                    .findFragmentByTag("f" + 1);

            // Ra lệnh cho cả hai fragment làm mới dữ liệu
            // Cần làm mới cả hai vì một đơn hàng có thể di chuyển
            // từ tab "Chưa hoàn thành" sang "Đã hoàn thành".
            if (fragChuaHoanThanh != null) {
                fragChuaHoanThanh.refreshData();
            }
            if (fragDaHoanThanh != null) {
                fragDaHoanThanh.refreshData();
            }
        }
    }

    /** Lấy fragment đang hiển thị trong ViewPager2 để gửi lệnh filter/sort. */
    private OrdersListFragment getCurrentOrdersFragment() {
        return pagerAdapter.get(viewPager.getCurrentItem());
    }

    private void setupBottomNavigation() {
        bottomNavigationView.setSelectedItemId(R.id.navigation_orders_shipper);
        bottomNavigationView.setOnItemSelectedListener(new BottomNavigationView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int itemId = item.getItemId();
                if (itemId == R.id.navigation_orders_shipper) {
                    // Đã ở trang chủ, không làm gì cả
                    return true;
                } else if (itemId == R.id.navigation_home_shipper) {
                    Intent intent = new Intent(ShipperMyOrdersActivity.this, ShipperActivity.class);
                    startActivity(intent);
                    return false;
                } else if (itemId == R.id.navigation_notifications_shipper) {
                    Intent intent = new Intent(ShipperMyOrdersActivity.this, NotificationActivity.class);
                    startActivity(intent);
                    return false;
                } else if (itemId == R.id.navigation_account_shipper) {
                    Intent intent = new Intent(ShipperMyOrdersActivity.this, AccountActivity.class);
                    startActivity(intent);
                    return false;
                }
                return false;
            }
        });
    }

    @Override
    public void onBackPressed() {
        // Lấy role để biết trang chủ là ai
        int role = session.getRole();

        // Thay vì thoát app, chúng ta chuyển về Trang chủ
        if (role == 6) {
            startActivity(new Intent(this, ShipperActivity.class));
        } else {
            startActivity(new Intent(this, MainActivity.class));
        }

        // Tắt hiệu ứng chuyển cảnh cho mượt
        overridePendingTransition(0, 0);
        finish();
    }
}


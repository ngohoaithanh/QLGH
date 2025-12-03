package com.hoaithanh.qlgh.activity;

import android.content.Intent;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.badge.BadgeDrawable;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;
import com.hoaithanh.qlgh.R;
import com.hoaithanh.qlgh.adapter.NotificationAdapter;
import com.hoaithanh.qlgh.api.RetrofitClient;
import com.hoaithanh.qlgh.base.BaseActivity;
import com.hoaithanh.qlgh.model.Notification;
import com.hoaithanh.qlgh.model.SimpleResult;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NotificationActivity extends BaseActivity {

    private MaterialToolbar toolbar;
    private RecyclerView rvNotifications;
    private SwipeRefreshLayout swipeRefresh;
    private ProgressBar progressBar;
    private LinearLayout layoutEmpty;
    private BottomNavigationView bottomNavigationView;

    private NotificationAdapter adapter;

    private int currentPage = 1;
    private boolean isLoading = false; // Đang tải?
    private boolean isLastPage = false; // Đã hết dữ liệu?
    private static final int PAGE_SIZE = 20; // Khớp với limit bên PHP

    @Override
    public void initLayout() {
        setContentView(R.layout.activity_notification);
    }

    @Override
    public void initData() {
        // session đã được khởi tạo ở BaseActivity
    }

    @Override
    public void initView() {
        // 1. Ánh xạ
        toolbar = findViewById(R.id.toolbar);
        rvNotifications = findViewById(R.id.rvNotifications);
        swipeRefresh = findViewById(R.id.swipeRefresh);
        progressBar = findViewById(R.id.progressBar);
        layoutEmpty = findViewById(R.id.layoutEmpty);
        bottomNavigationView = findViewById(R.id.bottom_navigation);

        // 2. Setup Toolbar
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        // 3. Setup RecyclerView
        rvNotifications.setLayoutManager(new LinearLayoutManager(this));
        adapter = new NotificationAdapter();
        rvNotifications.setAdapter(adapter);

        // 4. Setup SwipeRefresh & Scroll Listener
        setupScrollListener();
        swipeRefresh.setOnRefreshListener(() -> {
            // Reset về trang 1 khi kéo làm mới
            currentPage = 1;
            isLastPage = false;
            loadNotifications(true);
        });

        // 5. Setup Bottom Navigation
        setupBottomNavigation();

        // 6. Tải dữ liệu lần đầu
        loadNotifications(true);

        // Đánh dấu đã đọc (để xóa chấm đỏ)
        markAsRead();

    }

    private void setupScrollListener() {
        rvNotifications.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                if (layoutManager == null) return;

                int visibleItemCount = layoutManager.getChildCount();
                int totalItemCount = layoutManager.getItemCount();
                int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();

                // Logic kiểm tra chạm đáy:
                // - Không đang tải
                // - Chưa phải trang cuối
                // - Đã cuộn xuống gần cuối danh sách
                if (!isLoading && !isLastPage) {
                    if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount
                            && firstVisibleItemPosition >= 0
                            && totalItemCount >= PAGE_SIZE) {

                        // Đã chạm đáy -> Tải trang tiếp theo
                        currentPage++;
                        loadNotifications(false); // false = là load more
                    }
                }
            }
        });
    }

    /**
     * Hàm tải dữ liệu chung
     * @param isRefresh: true nếu là làm mới (xóa cũ), false nếu là tải thêm (giữ cũ)
     */
    private void loadNotifications(boolean isRefresh) {
        isLoading = true;
        if (isRefresh) {
            // Nếu refresh thì hiện vòng xoay ở trên
            // progressBar.setVisibility(View.VISIBLE); // Hoặc dùng swipeRefresh.setRefreshing(true)
        } else {
            // Nếu load more thì có thể hiện một progress bar nhỏ ở dưới đáy (tùy chọn)
            Toast.makeText(this, "Đang tải thêm...", Toast.LENGTH_SHORT).show();
        }

        RetrofitClient.getApi().getNotifications(session.getUserId(), currentPage, PAGE_SIZE)
                .enqueue(new Callback<List<Notification>>() {
                    @Override
                    public void onResponse(Call<List<Notification>> call, Response<List<Notification>> response) {
                        isLoading = false;
                        swipeRefresh.setRefreshing(false);
                        progressBar.setVisibility(View.GONE);

                        if (response.isSuccessful() && response.body() != null) {
                            List<Notification> list = response.body();

                            // Kiểm tra nếu dữ liệu trả về ít hơn limit -> Đã là trang cuối
                            if (list.size() < PAGE_SIZE) {
                                isLastPage = true;
                            }

                            if (isRefresh) {
                                // Refresh: Xóa cũ, nạp mới
                                adapter.submitList(list);
                                if (list.isEmpty()) showEmptyState();
                                else showDataState(list);
                            } else {
                                // Load More: Nối thêm vào đuôi
                                adapter.addList(list);
                            }
                        } else {
                            // Xử lý lỗi
                        }
                    }

                    @Override
                    public void onFailure(Call<List<Notification>> call, Throwable t) {
                        isLoading = false;
                        swipeRefresh.setRefreshing(false);
                        progressBar.setVisibility(View.GONE);
                        // Nếu load more thất bại, giảm currentPage để lần sau thử lại đúng trang đó
                        if (!isRefresh && currentPage > 1) currentPage--;
                    }
                });
    }

    private void markAsRead() {
        RetrofitClient.getApi().markNotificationsAsRead().enqueue(new Callback<SimpleResult>() {
            @Override
            public void onResponse(Call<SimpleResult> call, Response<SimpleResult> response) {
                // Không cần làm gì nhiều, chỉ cần gọi để server biết
                // Nếu muốn cập nhật UI ngay lập tức (ẩn badge ở BottomNav của trang này):
                if (bottomNavigationView != null) {
                    BadgeDrawable badge = bottomNavigationView.getBadge(R.id.navigation_notifications);
                    if (badge != null) badge.setVisible(false);
                }
            }
            @Override
            public void onFailure(Call<SimpleResult> call, Throwable t) {}
        });
    }

    private void showEmptyState() {
        rvNotifications.setVisibility(View.GONE);
        layoutEmpty.setVisibility(View.VISIBLE);
    }

    private void showDataState(List<Notification> list) {
        layoutEmpty.setVisibility(View.GONE);
        rvNotifications.setVisibility(View.VISIBLE);
        adapter.submitList(list);
    }

    private void setupBottomNavigation() {
        bottomNavigationView.setOnItemSelectedListener(new BottomNavigationView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int itemId = item.getItemId();
                if (itemId == R.id.navigation_notifications) {
                    // Đã ở trang chủ, không làm gì cả
                    return true;
                } else if (itemId == R.id.navigation_orders) {
                    Toast.makeText(NotificationActivity.this, "Mở trang đơn hàng", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(NotificationActivity.this, DanhSachDonDatHangActivity.class);
                    startActivity(intent);
                    return false;
                } else if (itemId == R.id.navigation_home) {
                    Toast.makeText(NotificationActivity.this, "Trang chủ!", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(NotificationActivity.this, MainActivity.class);
                    startActivity(intent);
                    return false;
                } else if (itemId == R.id.navigation_account) {
                    Toast.makeText(NotificationActivity.this, "Mở trang tài khoản", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(NotificationActivity.this, AccountActivity.class);
                    startActivity(intent);
                    return false;
                }
                return false;
            }
        });
    }
}
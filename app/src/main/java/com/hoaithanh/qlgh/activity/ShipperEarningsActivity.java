package com.hoaithanh.qlgh.activity;

import android.os.Bundle;
import android.util.Log; // Thêm Log
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable; // Cần import Nullable
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.util.Pair; // Cần cho Date Range Picker
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.datepicker.MaterialDatePicker; // Cần cho Date Range Picker

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.hoaithanh.qlgh.R;
import com.hoaithanh.qlgh.adapter.TransactionAdapter;
import com.hoaithanh.qlgh.base.BaseActivity; // Hoặc AppCompatActivity
import com.hoaithanh.qlgh.session.SessionManager;
import com.hoaithanh.qlgh.viewmodel.ShipperEarningsViewModel;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class ShipperEarningsActivity extends BaseActivity { // Hoặc AppCompatActivity

    // --- Khai báo UI Elements ---
    private MaterialToolbar toolbar;
    private TextView tvCurrentBalance, tvPeriodEarnings, tvCodHeld; // Thêm TextViews mới
    private Button btnWithdraw;
    private ChipGroup chipGroupDateFilter;
    private RecyclerView rvTransactionHistory;
    private ProgressBar progressBar;
    private TextView tvEmpty;
    private ImageButton btnBalanceInfo;

    // --- Khai báo các thành phần khác ---
    private ShipperEarningsViewModel viewModel;
    private TransactionAdapter transactionAdapter;
    private SessionManager sessionManager;
    private int shipperId;

    @Override
    public void initLayout() {
        setContentView(R.layout.activity_shipper_earnings);
    }

    @Override
    public void initData() {
        sessionManager = new SessionManager(getApplicationContext());
        shipperId = sessionManager.getUserId();
        viewModel = new ViewModelProvider(this).get(ShipperEarningsViewModel.class);
    }

    @Override
    public void initView() {
        // --- Ánh xạ View ---
        toolbar = findViewById(R.id.toolbar);
        tvCurrentBalance = findViewById(R.id.tvCurrentBalance);
        tvPeriodEarnings = findViewById(R.id.tvPeriodEarnings); // Ánh xạ mới
        tvCodHeld = findViewById(R.id.tvCodHeld);               // Ánh xạ mới
        btnWithdraw = findViewById(R.id.btnWithdraw);
        chipGroupDateFilter = findViewById(R.id.chipGroupDateFilter);
        rvTransactionHistory = findViewById(R.id.rvTransactionHistory);
        progressBar = findViewById(R.id.progressBar);
        tvEmpty = findViewById(R.id.tvEmpty);
        btnBalanceInfo = findViewById(R.id.btnBalanceInfo);
        btnBalanceInfo.setOnClickListener(v -> {
            new MaterialAlertDialogBuilder(this)
                    .setTitle("Giải thích Số dư Ví") // Đổi tiêu đề cho rõ hơn
                    .setMessage("Số dư Ví (có thể rút) được tính bằng Thu nhập Ròng (Phí ship + Thưởng - Phạt) trừ đi tổng số Phí COD bạn cần nộp lại và số tiền bạn đã rút.\n\nVui lòng hoàn thành việc nộp lại Phí COD để tăng số dư có thể rút.") // Sửa lại nội dung
                    .setPositiveButton("Đã hiểu", null)
                    .show();
        });

        // --- Thiết lập Toolbar ---
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        // --- Thiết lập RecyclerView ---
        rvTransactionHistory.setLayoutManager(new LinearLayoutManager(this));
        transactionAdapter = new TransactionAdapter();
        rvTransactionHistory.setAdapter(transactionAdapter);

        // --- Thiết lập Listener cho Bộ lọc ---
        setupChipGroupListener();

        // --- Lắng nghe dữ liệu từ ViewModel ---
        observeViewModel();

        // --- Tải dữ liệu ban đầu (Số dư và giao dịch hôm nay) ---
        viewModel.getBalanceData(shipperId); // Tải số dư
        loadDataForChip(R.id.chipToday);     // Tải giao dịch hôm nay
    }

    private void setupChipGroupListener() {
        chipGroupDateFilter.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (!checkedIds.isEmpty()) {
                int checkedId = checkedIds.get(0);
                if (checkedId == R.id.chipCustomRange) {
                    openDateRangePicker();
                } else {
                    loadDataForChip(checkedId);
                }
            }
        });
    }

    private void observeViewModel() {
        // --- LẮNG NGHE SỐ DƯ VÍ (tvCurrentBalance) VÀ PHÍ COD ĐANG GIỮ (tvCodHeld) ---
        viewModel.getBalanceData(shipperId).observe(this, balanceResponse -> {
            if (balanceResponse != null) {
                double netIncome = balanceResponse.getNetIncome();
                double feeHeld = balanceResponse.getServiceFeeHeld();

                // Tính toán Số dư Ví thực tế
                double actualBalance = netIncome - feeHeld;

                // Gán Số dư Ví (có thể âm) vào tvCurrentBalance
                tvCurrentBalance.setText(formatCurrencyVN(String.valueOf(actualBalance)));

                // Gán Phí COD đang giữ vào tvCodHeld
                tvCodHeld.setText(formatCurrencyVN(String.valueOf(feeHeld)));
            } else {
                // Xử lý lỗi nếu không lấy được số dư
                tvCurrentBalance.setText("Lỗi");
                tvCodHeld.setText("Lỗi");
            }
        });

        // --- LẮNG NGHE THU NHẬP RÒNG TRONG KỲ (tvPeriodEarnings) ---
        // (Phần này trong code của bạn đã đúng, giữ nguyên)
        viewModel.getPeriodEarnings().observe(this, earnings -> {
            if (earnings != null) {
                tvPeriodEarnings.setText("+" + formatCurrencyVN(String.valueOf(earnings)));
            } else {
                tvPeriodEarnings.setText("0đ");
            }
        });

        // --- LẮNG NGHE LỊCH SỬ GIAO DỊCH ---
        // (Phần này trong code của bạn đã đúng, giữ nguyên)
        viewModel.getTransactionsData().observe(this, transactions -> {
            if (transactions != null) {
                if (transactions.isEmpty()) {
                    tvEmpty.setVisibility(View.VISIBLE);
                    rvTransactionHistory.setVisibility(View.GONE);
                } else {
                    tvEmpty.setVisibility(View.GONE);
                    rvTransactionHistory.setVisibility(View.VISIBLE);
                    transactionAdapter.submitList(transactions);
                }
            } else {
                tvEmpty.setText("Không thể tải lịch sử giao dịch.");
                tvEmpty.setVisibility(View.VISIBLE);
                rvTransactionHistory.setVisibility(View.GONE);
            }
        });

        // --- LẮNG NGHE TRẠNG THÁI LOADING ---
        // (Phần này trong code của bạn đã đúng, giữ nguyên)
        viewModel.getIsLoading().observe(this, isLoading -> {
            progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            if (isLoading) {
                tvEmpty.setVisibility(View.GONE);
                rvTransactionHistory.setVisibility(View.GONE);
            }
        });
    }

    private void loadDataForChip(int checkedId) {
        Calendar startCalendar = Calendar.getInstance(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
        Calendar endCalendar = Calendar.getInstance(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));

        // Reset giờ phút giây về đầu/cuối ngày
        resetCalendarTime(startCalendar, true);
        resetCalendarTime(endCalendar, false);

        if (checkedId == R.id.chipToday) {
            // Start và End là ngày hôm nay (đã reset)
        } else if (checkedId == R.id.chipThisWeek) {
            startCalendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY); // Về Thứ 2 đầu tuần
        } else if (checkedId == R.id.chipThisMonth) {
            startCalendar.set(Calendar.DAY_OF_MONTH, 1); // Về ngày đầu tháng
        }
        // Trường hợp chipCustomRange được xử lý riêng

        Log.d("EarningsDebug", "Loading data for range: " + startCalendar.getTime() + " to " + endCalendar.getTime());
        // Gọi ViewModel để tải dữ liệu
        viewModel.loadTransactionsForDateRange(shipperId, startCalendar, endCalendar);
    }

    private void resetCalendarTime(Calendar cal, boolean startOfDay) {
        if (startOfDay) {
            cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0);
        } else {
            cal.set(Calendar.HOUR_OF_DAY, 23); cal.set(Calendar.MINUTE, 59); cal.set(Calendar.SECOND, 59); cal.set(Calendar.MILLISECOND, 999);
        }
    }


    private void openDateRangePicker() {
        MaterialDatePicker.Builder<Pair<Long, Long>> builder = MaterialDatePicker.Builder.dateRangePicker();
        builder.setTitleText("Chọn khoảng thời gian");

        // Cài đặt ngày mặc định (ví dụ: tháng này)
        Calendar startMonth = Calendar.getInstance(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
        startMonth.set(Calendar.DAY_OF_MONTH, 1);
        resetCalendarTime(startMonth, true);
        Calendar endMonth = Calendar.getInstance(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
        resetCalendarTime(endMonth, false);
        builder.setSelection(new Pair<>(startMonth.getTimeInMillis(), endMonth.getTimeInMillis()));

        MaterialDatePicker<Pair<Long, Long>> picker = builder.build();

        picker.addOnPositiveButtonClickListener(selection -> {
            Long startDateMillis = selection.first;
            Long endDateMillis = selection.second;

            if (startDateMillis != null && endDateMillis != null) {
                Calendar startCal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
                startCal.setTimeInMillis(startDateMillis);
                resetCalendarTime(startCal, true); // Đảm bảo bắt đầu từ 00:00:00

                Calendar endCal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
                endCal.setTimeInMillis(endDateMillis);
                resetCalendarTime(endCal, false); // Đảm bảo kết thúc vào 23:59:59

                Log.d("EarningsDebug", "Custom range selected: " + startCal.getTime() + " to " + endCal.getTime());
                viewModel.loadTransactionsForDateRange(shipperId, startCal, endCal);
            }
        });

        picker.show(getSupportFragmentManager(), picker.toString());
    }


//    private void updateBalance(double balance) {
//        tvCurrentBalance.setText(formatCurrencyVN(String.valueOf(balance)));
//    }

    private String formatCurrencyVN(String amount) {
        if (amount == null || amount.trim().isEmpty()) return "0đ";
        try {
            double value = Double.parseDouble(amount.trim());
            Locale localeVN = new Locale("vi", "VN");
            NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(localeVN);
            return currencyFormatter.format(value);
        } catch (NumberFormatException e) {
            return amount + " ₫";
        }
    }
}
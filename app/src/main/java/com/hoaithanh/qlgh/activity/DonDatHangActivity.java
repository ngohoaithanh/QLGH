package com.hoaithanh.qlgh.activity;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.widget.*;
import android.view.View;

import com.hoaithanh.qlgh.R;
import com.hoaithanh.qlgh.api.RetrofitClient;
import com.hoaithanh.qlgh.api.ApiService;
import com.hoaithanh.qlgh.model.ApiResult;
import com.hoaithanh.qlgh.model.DonDatHang;
import com.hoaithanh.qlgh.session.SessionManager;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DonDatHangActivity extends AppCompatActivity {

    private CardView cardSenderInfo, cardReceiverInfo, cardProductInfo;
    private LinearLayout formSenderInfo, formReceiverInfo, formProductInfo;
    private TextView tvSenderPlaceholder, tvReceiverPlaceholder, tvProductPlaceholder;
    private TextView tvShippingFee, tvCodFee, tvTotal;
    private EditText etCodAmount;
    private RadioGroup rgService;
    private boolean isSenderExpanded = false;
    private boolean isReceiverExpanded = false;
    private boolean isProductExpanded = false;

    private SessionManager session;
    private EditText etSenderName, etSenderPhone;
    private AutoCompleteTextView actvSenderAddress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_don_dat_hang);

        session = new SessionManager(this); // khởi tạo session
        initView();
        setupClickListeners();
        prefillSenderInfo(); // 👉 tự động điền thông tin người gửi

        // Nút lưu thông tin
        findViewById(R.id.btn_sender_save).setOnClickListener(v -> collapseSenderInfo());
        findViewById(R.id.btn_receiver_save).setOnClickListener(v -> collapseReceiverInfo());
        findViewById(R.id.btn_product_save).setOnClickListener(v -> collapseProductInfo());

        // Nút hủy và đặt đơn
        findViewById(R.id.btn_cancel).setOnClickListener(v -> finish());
        findViewById(R.id.btn_submit).setOnClickListener(v -> submitOrder());
    }

    private void initView() {
        cardSenderInfo = findViewById(R.id.card_sender_info);
        cardReceiverInfo = findViewById(R.id.card_receiver_info);
        cardProductInfo = findViewById(R.id.card_product_info);

        formSenderInfo = findViewById(R.id.form_sender_info);
        formReceiverInfo = findViewById(R.id.form_receiver_info);
        formProductInfo = findViewById(R.id.form_product_info);

        tvSenderPlaceholder = findViewById(R.id.tv_sender_placeholder);
        tvReceiverPlaceholder = findViewById(R.id.tv_receiver_placeholder);
        tvProductPlaceholder = findViewById(R.id.tv_product_placeholder);

        tvShippingFee = findViewById(R.id.tv_shipping_fee);
        tvCodFee = findViewById(R.id.tv_cod_fee);
        tvTotal = findViewById(R.id.tv_total);

        etCodAmount = findViewById(R.id.et_cod_amount);
        rgService = findViewById(R.id.rg_service);

        EditText etWeight = findViewById(R.id.et_product_weight);

        etSenderName = findViewById(R.id.et_sender_name);
        etSenderPhone = findViewById(R.id.et_sender_phone);
        actvSenderAddress = findViewById(R.id.actv_sender_address);

        etWeight.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) { calculateFees(); }
        });
    }

    private void prefillSenderInfo() {
        String name = session.getUsername();
        String phone = session.getPhone();

        if (name != null && !name.isEmpty()) {
            etSenderName.setText(name);
            tvSenderPlaceholder.setText(name);
        }
        if (phone != null && !phone.isEmpty()) {
            etSenderPhone.setText(phone);
        }
    }

    private void setupClickListeners() {
        // Xử lý click thông tin người gửi
        cardSenderInfo.setOnClickListener(v -> {
            if (isSenderExpanded) collapseSenderInfo(); else expandSenderInfo();
        });

        // Xử lý click thông tin người nhận
        cardReceiverInfo.setOnClickListener(v -> {
            if (isReceiverExpanded) collapseReceiverInfo(); else expandReceiverInfo();
        });

        // Xử lý click thông tin hàng hóa
        cardProductInfo.setOnClickListener(v -> {
            if (isProductExpanded) collapseProductInfo(); else expandProductInfo();
        });

        // Xử lý thay đổi dịch vụ và số tiền COD
        rgService.setOnCheckedChangeListener((group, checkedId) -> calculateFees());
        etCodAmount.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) { calculateFees(); }
        });
    }

    private void expandSenderInfo() {
        formSenderInfo.setVisibility(View.VISIBLE);
        tvSenderPlaceholder.setVisibility(View.GONE);
        isSenderExpanded = true;
        if (isReceiverExpanded) collapseReceiverInfo();
        if (isProductExpanded) collapseProductInfo();
    }

    private void collapseSenderInfo() {
        formSenderInfo.setVisibility(View.GONE);
        tvSenderPlaceholder.setVisibility(View.VISIBLE);
        EditText etName = findViewById(R.id.et_sender_name);
        if (!TextUtils.isEmpty(etName.getText())) {
            tvSenderPlaceholder.setText(etName.getText().toString());
        }
        isSenderExpanded = false;
    }

    private void expandReceiverInfo() {
        formReceiverInfo.setVisibility(View.VISIBLE);
        tvReceiverPlaceholder.setVisibility(View.GONE);
        isReceiverExpanded = true;
        if (isSenderExpanded) collapseSenderInfo();
        if (isProductExpanded) collapseProductInfo();
    }

    private void collapseReceiverInfo() {
        formReceiverInfo.setVisibility(View.GONE);
        tvReceiverPlaceholder.setVisibility(View.VISIBLE);
        EditText etName = findViewById(R.id.et_receiver_name);
        if (!TextUtils.isEmpty(etName.getText())) {
            tvReceiverPlaceholder.setText(etName.getText().toString());
        }
        isReceiverExpanded = false;
    }

    private void expandProductInfo() {
        formProductInfo.setVisibility(View.VISIBLE);
        tvProductPlaceholder.setVisibility(View.GONE);
        isProductExpanded = true;
        if (isSenderExpanded) collapseSenderInfo();
        if (isReceiverExpanded) collapseReceiverInfo();
    }

    private void collapseProductInfo() {
        formProductInfo.setVisibility(View.GONE);
        tvProductPlaceholder.setVisibility(View.VISIBLE);
        EditText etName = findViewById(R.id.et_product_name);
        if (!TextUtils.isEmpty(etName.getText())) {
            tvProductPlaceholder.setText(etName.getText().toString());
        }
        isProductExpanded = false;
    }

    private void calculateFees() {
        // Lấy weight
        double weight = 0;
        try {
            String weightStr = ((EditText) findViewById(R.id.et_product_weight))
                    .getText().toString().trim();
            if (!weightStr.isEmpty()) {
                weight = Double.parseDouble(weightStr);
            }
        } catch (NumberFormatException ignore) {}

        // Tính phí vận chuyển theo công thức server
        int shippingFee = DonDatHang.calculateShippingFee(weight);

        // Lấy COD_amount
        double codAmount = 0;
        try {
            String codStr = etCodAmount.getText().toString().trim();
            if (!codStr.isEmpty()) {
                codAmount = Double.parseDouble(codStr);
            }
        } catch (NumberFormatException ignore) {}

        // ===== Phí COD đồng bộ với server =====
        int codFee = 0;
        if (codAmount > 0) {
            codFee = (int) Math.round(codAmount * 0.01);
            if (codFee < 5000) codFee = 5000;
            if (codFee > 15000) codFee = 15000;
        }

        int total = shippingFee + codFee;

        // Cập nhật UI
        tvShippingFee.setText(String.format("%,dđ", shippingFee));
        tvCodFee.setText(String.format("%,dđ", codFee));
        tvTotal.setText(String.format("%,dđ", total));
    }

    private void submitOrder() {
        // Lấy dữ liệu từ form
        String customerName = ((EditText) findViewById(R.id.et_sender_name)).getText().toString();
        String phoneNumber  = ((EditText) findViewById(R.id.et_sender_phone)).getText().toString();
        String pickAddress  = ((AutoCompleteTextView) findViewById(R.id.actv_sender_address)).getText().toString();

        String recipient    = ((EditText) findViewById(R.id.et_receiver_name)).getText().toString();
        String recipPhone   = ((EditText) findViewById(R.id.et_receiver_phone)).getText().toString();
        String delivAddress = ((AutoCompleteTextView) findViewById(R.id.actv_receiver_address)).getText().toString();

        double weight = 0;
        try { weight = Double.parseDouble(((EditText) findViewById(R.id.et_product_weight)).getText().toString()); }
        catch (Exception e) { weight = 0; }

        double codAmount = 0;
        try { codAmount = Double.parseDouble(etCodAmount.getText().toString()); }
        catch (Exception e) { codAmount = 0; }

        String note = ((EditText) findViewById(R.id.et_product_note)).getText().toString();

        // Kiểm tra dữ liệu
        if (TextUtils.isEmpty(customerName) || TextUtils.isEmpty(phoneNumber) ||
                TextUtils.isEmpty(pickAddress) || TextUtils.isEmpty(recipient) ||
                TextUtils.isEmpty(recipPhone) || TextUtils.isEmpty(delivAddress) ||
                weight <= 0) {
            Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin bắt buộc", Toast.LENGTH_SHORT).show();
            return;
        }

        String status = "pending"; // mặc định khi tạo mới

        // Gọi API qua Retrofit
        ApiService apiService = RetrofitClient.getApi();
        apiService.createOrder(customerName, phoneNumber, pickAddress,
                        delivAddress, recipient, recipPhone, status,
                        codAmount, weight, note)
                .enqueue(new Callback<ApiResult>() {
                    @Override
                    public void onResponse(Call<ApiResult> call, Response<ApiResult> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            ApiResult result = response.body();
                            if (result.success) {
                                Toast.makeText(DonDatHangActivity.this, "Đặt đơn thành công!", Toast.LENGTH_LONG).show();
                                startActivity(new Intent(DonDatHangActivity.this, DanhSachDonDatHangActivity.class));
                                finish();
                            } else {
                                Toast.makeText(DonDatHangActivity.this, "Thất bại: " + result.error, Toast.LENGTH_LONG).show();
                            }
                        } else {
                            Toast.makeText(DonDatHangActivity.this, "Lỗi server: " + response.code(), Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResult> call, Throwable t) {
                        Toast.makeText(DonDatHangActivity.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }
}

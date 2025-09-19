package com.hoaithanh.qlgh.activity;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import java.util.concurrent.atomic.AtomicReference;


import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.widget.*;
import android.view.View;
import android.content.Intent;

import com.hoaithanh.qlgh.BuildConfig;                       // NEW
import com.hoaithanh.qlgh.R;
import com.hoaithanh.qlgh.api.RetrofitClient;
import com.hoaithanh.qlgh.api.ApiService;
import com.hoaithanh.qlgh.model.ApiResult;
import com.hoaithanh.qlgh.model.DonDatHang;
import com.hoaithanh.qlgh.model.goong.GeocodingResponse;
import com.hoaithanh.qlgh.session.SessionManager;

// NEW: Goong repo + models + adapter
import com.hoaithanh.qlgh.repository.GoongRepository;
import com.hoaithanh.qlgh.model.goong.PlaceAutoCompleteResponse;
import com.hoaithanh.qlgh.model.goong.PlaceDetailResponse;
import com.hoaithanh.qlgh.adapter.PlaceSuggestionAdapter;

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
    private AutoCompleteTextView actvSenderAddress, actvReceiverAddress;

    // Goong Autocomplete
    private PlaceSuggestionAdapter senderAdapter, receiverAdapter;
    private final GoongRepository goongRepo = new GoongRepository();
    private final Handler uiHandler = new Handler(Looper.getMainLooper());
    private Runnable senderRunnable, receiverRunnable;

    // Lưu lựa chọn
    private String senderPlaceId, receiverPlaceId;
    private double senderLat, senderLng, receiverLat, receiverLng;

    // Ngăn TextWatcher reset placeId khi setText bằng code
    private boolean updatingSenderText = false;
    private boolean updatingReceiverText = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_don_dat_hang);

        session = new SessionManager(this);
        initView();
        setupClickListeners();
        setupAutocomplete();
        prefillSenderInfo();

        findViewById(R.id.btn_sender_save).setOnClickListener(v -> collapseSenderInfo());
        findViewById(R.id.btn_receiver_save).setOnClickListener(v -> collapseReceiverInfo());
        findViewById(R.id.btn_product_save).setOnClickListener(v -> collapseProductInfo());

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
        actvReceiverAddress = findViewById(R.id.actv_receiver_address);

        etWeight.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) { calculateFees(); }
        });
    }

    private void prefillSenderInfo() {
        String name = session.getUsername();
        String phone = session.getPhone();

        if (!TextUtils.isEmpty(name)) {
            etSenderName.setText(name);
            tvSenderPlaceholder.setText(name);
        }
        if (!TextUtils.isEmpty(phone)) {
            etSenderPhone.setText(phone);
        }

        // ===== Prefill địa chỉ lấy hàng lần trước (nếu có) =====
        String lastAddr = session.getLastPickupAddress();
        if (!TextUtils.isEmpty(lastAddr)) {
            updatingSenderText = true;
            actvSenderAddress.setText(lastAddr);
            actvSenderAddress.dismissDropDown();
            updatingSenderText = false;

            // khôi phục placeId + lat/lng (nếu đã lưu)
            String lastPlaceId = session.getLastPickupPlaceId();
            Double lastLat = session.getLastPickupLat();
            Double lastLng = session.getLastPickupLng();

            senderPlaceId = lastPlaceId; // có thể null
            if (lastLat != null) senderLat = lastLat;
            if (lastLng != null) senderLng = lastLng;
        }
    }

    private void setupClickListeners() {
        cardSenderInfo.setOnClickListener(v -> { if (isSenderExpanded) collapseSenderInfo(); else expandSenderInfo(); });
        cardReceiverInfo.setOnClickListener(v -> { if (isReceiverExpanded) collapseReceiverInfo(); else expandReceiverInfo(); });
        cardProductInfo.setOnClickListener(v -> { if (isProductExpanded) collapseProductInfo(); else expandProductInfo(); });

        rgService.setOnCheckedChangeListener((group, checkedId) -> calculateFees());
        etCodAmount.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) { calculateFees(); }
        });
    }

    // ===== Goong Autocomplete + Place Detail =====
    private void setupAutocomplete() {
        senderAdapter = new PlaceSuggestionAdapter(this);
        receiverAdapter = new PlaceSuggestionAdapter(this);
        actvSenderAddress.setAdapter(senderAdapter);
        actvReceiverAddress.setAdapter(receiverAdapter);

        actvSenderAddress.setOnItemClickListener((parent, view, position, id) -> {
            PlaceAutoCompleteResponse.Prediction p = senderAdapter.getPredictionAt(position);
            if (p != null) {
                senderPlaceId = p.place_id;
                fetchPlaceDetail(p.place_id, true);
            }
        });

        actvReceiverAddress.setOnItemClickListener((parent, view, position, id) -> {
            PlaceAutoCompleteResponse.Prediction p = receiverAdapter.getPredictionAt(position);
            if (p != null) {
                receiverPlaceId = p.place_id;
                fetchPlaceDetail(p.place_id, false);
            }
        });

        // Debounce khi gõ + không reset placeId khi setText bằng code
        actvSenderAddress.addTextChangedListener(new SimpleTextWatcher(text -> {
            if (!updatingSenderText) senderPlaceId = null;
            debounceAutocomplete(text, true);
        }));
        actvReceiverAddress.addTextChangedListener(new SimpleTextWatcher(text -> {
            if (!updatingReceiverText) receiverPlaceId = null;
            debounceAutocomplete(text, false);
        }));
    }

    private void debounceAutocomplete(String input, boolean isSender) {
        if (input == null || input.trim().length() < 3) {
            if (isSender) senderAdapter.setPredictions(null); else receiverAdapter.setPredictions(null);
            return;
        }
        if (isSender) {
            if (senderRunnable != null) uiHandler.removeCallbacks(senderRunnable);
            senderRunnable = () -> callAutocomplete(input.trim(), true);
            uiHandler.postDelayed(senderRunnable, 300);
        } else {
            if (receiverRunnable != null) uiHandler.removeCallbacks(receiverRunnable);
            receiverRunnable = () -> callAutocomplete(input.trim(), false);
            uiHandler.postDelayed(receiverRunnable, 300);
        }
    }

    private void callAutocomplete(String query, boolean isSender) {
        String key = BuildConfig.GOONG_API_KEY;
        Integer limit = 5;
        String location = null; // "lat,lng" nếu muốn bias
        Integer radius = null;  // ví dụ 30000

        goongRepo.autoComplete(query, limit, location, radius, key)
                .enqueue(new Callback<PlaceAutoCompleteResponse>() {
                    @Override
                    public void onResponse(Call<PlaceAutoCompleteResponse> call, Response<PlaceAutoCompleteResponse> resp) {
                        if (!resp.isSuccessful() || resp.body() == null) {
                            if (isSender) senderAdapter.setPredictions(null); else receiverAdapter.setPredictions(null);
                            return;
                        }
                        if (isSender) {
                            senderAdapter.setPredictions(resp.body().predictions);
                            actvSenderAddress.showDropDown();
                        } else {
                            receiverAdapter.setPredictions(resp.body().predictions);
                            actvReceiverAddress.showDropDown();
                        }
                    }

                    @Override
                    public void onFailure(Call<PlaceAutoCompleteResponse> call, Throwable t) {
                        if (isSender) senderAdapter.setPredictions(null); else receiverAdapter.setPredictions(null);
                    }
                });
    }

    private void fetchPlaceDetail(String placeId, boolean isSender) {
        String key = BuildConfig.GOONG_API_KEY;
        goongRepo.placeDetail(placeId, key).enqueue(new Callback<PlaceDetailResponse>() {
            @Override
            public void onResponse(Call<PlaceDetailResponse> call, Response<PlaceDetailResponse> resp) {
                if (!resp.isSuccessful() || resp.body() == null || resp.body().result == null) return;

                double lat = resp.body().result.geometry.location.lat;
                double lng = resp.body().result.geometry.location.lng;
                String addr = resp.body().result.formatted_address;

                if (isSender) {
                    senderLat = lat; senderLng = lng; senderPlaceId = resp.body().result.place_id;
                    if (!TextUtils.isEmpty(addr)) {
                        updatingSenderText = true;
                        actvSenderAddress.setText(addr);
                        actvSenderAddress.dismissDropDown();
                        updatingSenderText = false;
                    }
                    // LƯU lại pickup đã chọn
                    session.saveLastPickup(
                            !TextUtils.isEmpty(addr) ? addr : actvSenderAddress.getText().toString(),
                            senderPlaceId,
                            senderLat,
                            senderLng
                    );
                } else {
                    receiverLat = lat; receiverLng = lng; receiverPlaceId = resp.body().result.place_id;
                    if (!TextUtils.isEmpty(addr)) {
                        updatingReceiverText = true;
                        actvReceiverAddress.setText(addr);
                        actvReceiverAddress.dismissDropDown();
                        updatingReceiverText = false;
                    }
                }
            }
            @Override public void onFailure(Call<PlaceDetailResponse> call, Throwable t) { /* ignore */ }
        });
    }
    // ===== End Goong Autocomplete =====

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
        if (!TextUtils.isEmpty(etName.getText())) tvSenderPlaceholder.setText(etName.getText().toString());
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
        if (!TextUtils.isEmpty(etName.getText())) tvReceiverPlaceholder.setText(etName.getText().toString());
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
        if (!TextUtils.isEmpty(etName.getText())) tvProductPlaceholder.setText(etName.getText().toString());
        isProductExpanded = false;
    }

    private void calculateFees() {
        double weight = 0;
        try {
            String weightStr = ((EditText) findViewById(R.id.et_product_weight)).getText().toString().trim();
            if (!weightStr.isEmpty()) weight = Double.parseDouble(weightStr);
        } catch (NumberFormatException ignore) {}

        int shippingFee = DonDatHang.calculateShippingFee(weight);

        double codAmount = 0;
        try {
            String codStr = etCodAmount.getText().toString().trim();
            if (!codStr.isEmpty()) codAmount = Double.parseDouble(codStr);
        } catch (NumberFormatException ignore) {}

        int codFee = 0;
        if (codAmount > 0) {
            codFee = (int) Math.round(codAmount * 0.01);
            if (codFee < 5000) codFee = 5000;
            if (codFee > 15000) codFee = 15000;
        }
        int total = shippingFee + codFee;

        tvShippingFee.setText(String.format("%,dđ", shippingFee));
        tvCodFee.setText(String.format("%,dđ", codFee));
        tvTotal.setText(String.format("%,dđ", total));
    }

    private void submitOrder() {
        // Lấy dữ liệu từ form
        String customerName = ((EditText) findViewById(R.id.et_sender_name)).getText().toString();
        String phoneNumber  = ((EditText) findViewById(R.id.et_sender_phone)).getText().toString();
        String pickAddress  = actvSenderAddress.getText().toString();

        String recipient    = ((EditText) findViewById(R.id.et_receiver_name)).getText().toString();
        String recipPhone   = ((EditText) findViewById(R.id.et_receiver_phone)).getText().toString();
        String delivAddress = actvReceiverAddress.getText().toString();

        double weight = 0;
        try { weight = Double.parseDouble(((EditText) findViewById(R.id.et_product_weight)).getText().toString()); }
        catch (Exception ignore) {}

        double codAmount = 0;
        try { codAmount = Double.parseDouble(etCodAmount.getText().toString()); }
        catch (Exception ignore) {}

        String note = ((EditText) findViewById(R.id.et_product_note)).getText().toString();

        // Kiểm tra dữ liệu bắt buộc
        if (TextUtils.isEmpty(customerName) || TextUtils.isEmpty(phoneNumber) ||
                TextUtils.isEmpty(pickAddress) || TextUtils.isEmpty(recipient) ||
                TextUtils.isEmpty(recipPhone) || TextUtils.isEmpty(delivAddress) ||
                weight <= 0) {
            Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin bắt buộc", Toast.LENGTH_SHORT).show();
            return;
        }

        String status = "pending";

        // Nếu đã chọn gợi ý nhưng Place Detail chưa kịp trả → chặn submit 1 nhịp
        if (senderPlaceId != null && (senderLat == 0 && senderLng == 0)) {
            Toast.makeText(this, "Đang lấy tọa độ điểm lấy hàng, vui lòng đợi...", Toast.LENGTH_SHORT).show();
            return;
        }
        if (receiverPlaceId != null && (receiverLat == 0 && receiverLng == 0)) {
            Toast.makeText(this, "Đang lấy tọa độ điểm giao hàng, vui lòng đợi...", Toast.LENGTH_SHORT).show();
            return;
        }

        // Nếu thiếu toạ độ → fallback Geocoding
        if (senderPlaceId == null || receiverPlaceId == null) {
            geocodeAndSubmit(customerName, phoneNumber, pickAddress,
                    delivAddress, recipient, recipPhone,
                    status, codAmount, weight, note);
        } else {
            callCreateOrder(customerName, phoneNumber, pickAddress,
                    senderLat, senderLng, delivAddress,
                    receiverLat, receiverLng, recipient, recipPhone,
                    status, codAmount, weight, note);
        }
    }

    // Fallback Geocoding khi người dùng nhập tay
    private void geocodeAndSubmit(String customerName, String phoneNumber,
                                  String pickAddress, String delivAddress,
                                  String recipient, String recipPhone,
                                  String status, double codAmount,
                                  double weight, String note) {

        String key = BuildConfig.GOONG_API_KEY;

        final AtomicReference<Double> pLatRef = new AtomicReference<>(null);
        final AtomicReference<Double> pLngRef = new AtomicReference<>(null);
        final AtomicReference<Double> dLatRef = new AtomicReference<>(null);
        final AtomicReference<Double> dLngRef = new AtomicReference<>(null);

        // Geocode pickup trước
        goongRepo.geocode(pickAddress, key).enqueue(new Callback<GeocodingResponse>() {
            @Override
            public void onResponse(Call<GeocodingResponse> call, Response<GeocodingResponse> resp1) {
                if (resp1.isSuccessful() && resp1.body() != null &&
                        resp1.body().results != null && !resp1.body().results.isEmpty()) {
                    GeocodingResponse.Result r = resp1.body().results.get(0);
                    if (r != null && r.geometry != null && r.geometry.location != null) {
                        pLatRef.set(r.geometry.location.lat);
                        pLngRef.set(r.geometry.location.lng);
                    }
                }

                // Geocode delivery
                goongRepo.geocode(delivAddress, key).enqueue(new Callback<GeocodingResponse>() {
                    @Override
                    public void onResponse(Call<GeocodingResponse> call2, Response<GeocodingResponse> resp2) {
                        if (resp2.isSuccessful() && resp2.body() != null &&
                                resp2.body().results != null && !resp2.body().results.isEmpty()) {
                            GeocodingResponse.Result r2 = resp2.body().results.get(0);
                            if (r2 != null && r2.geometry != null && r2.geometry.location != null) {
                                dLatRef.set(r2.geometry.location.lat);
                                dLngRef.set(r2.geometry.location.lng);
                            }
                        }

                        callCreateOrder(customerName, phoneNumber, pickAddress,
                                pLatRef.get(), pLngRef.get(),
                                delivAddress, dLatRef.get(), dLngRef.get(),
                                recipient, recipPhone, status, codAmount, weight, note);
                    }

                    @Override
                    public void onFailure(Call<GeocodingResponse> call, Throwable t) {
                        callCreateOrder(customerName, phoneNumber, pickAddress,
                                pLatRef.get(), pLngRef.get(),
                                delivAddress, null, null,
                                recipient, recipPhone, status, codAmount, weight, note);
                    }
                });
            }

            @Override
            public void onFailure(Call<GeocodingResponse> call, Throwable t) {
                // pickup geocode fail → thử geocode delivery; nếu fail tiếp thì gửi null hết
                goongRepo.geocode(delivAddress, key).enqueue(new Callback<GeocodingResponse>() {
                    @Override
                    public void onResponse(Call<GeocodingResponse> call2, Response<GeocodingResponse> resp2) {
                        if (resp2.isSuccessful() && resp2.body() != null &&
                                resp2.body().results != null && !resp2.body().results.isEmpty()) {
                            GeocodingResponse.Result r2 = resp2.body().results.get(0);
                            if (r2 != null && r2.geometry != null && r2.geometry.location != null) {
                                dLatRef.set(r2.geometry.location.lat);
                                dLngRef.set(r2.geometry.location.lng);
                            }
                        }
                        callCreateOrder(customerName, phoneNumber, pickAddress,
                                null, null,
                                delivAddress, dLatRef.get(), dLngRef.get(),
                                recipient, recipPhone, status, codAmount, weight, note);
                    }

                    @Override
                    public void onFailure(Call<GeocodingResponse> call, Throwable t2) {
                        callCreateOrder(customerName, phoneNumber, pickAddress,
                                null, null,
                                delivAddress, null, null,
                                recipient, recipPhone, status, codAmount, weight, note);
                    }
                });
            }
        });
    }

    private void callCreateOrder(String customerName, String phoneNumber,
                                 String pickAddress, Double pickLat, Double pickLng,
                                 String delivAddress, Double delivLat, Double delivLng,
                                 String recipient, String recipPhone,
                                 String status, double codAmount,
                                 double weight, String note) {

        // Cảnh báo nhẹ nếu không có toạ độ
        if (pickLat == null || pickLng == null || delivLat == null || delivLng == null) {
            Toast.makeText(this, "Lưu ý: đơn sẽ được lưu KHÔNG có tọa độ đầy đủ.", Toast.LENGTH_SHORT).show();
        }

        ApiService apiService = RetrofitClient.getApi();
        apiService.createOrder(
                customerName,
                phoneNumber,
                pickAddress,
                pickLat, pickLng,
                delivAddress,
                delivLat, delivLng,
                recipient,
                recipPhone,
                status,
                codAmount,
                weight,
                note
        ).enqueue(new Callback<ApiResult>() {
            @Override
            public void onResponse(Call<ApiResult> call, Response<ApiResult> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResult result = response.body();
                    if (result.success) {
                        // LƯU lại pickup cuối cùng (kể cả dùng geocoding, toạ độ có thể null)
                        session.saveLastPickup(
                                pickAddress,
                                senderPlaceId,  // có thể null nếu dùng geocoding
                                pickLat, pickLng
                        );

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

    // TextWatcher gọn
    static class SimpleTextWatcher implements TextWatcher {
        interface OnChanged { void changed(String s); }
        private final OnChanged cb;
        SimpleTextWatcher(OnChanged cb){ this.cb = cb; }
        @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
        @Override public void onTextChanged(CharSequence s, int st, int b, int c) { cb.changed(s.toString()); }
        @Override public void afterTextChanged(Editable s) {}
    }
}

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

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.hoaithanh.qlgh.BuildConfig;                       // NEW
import com.hoaithanh.qlgh.R;
import com.hoaithanh.qlgh.api.RetrofitClient;
import com.hoaithanh.qlgh.api.ApiService;
import com.hoaithanh.qlgh.base.BaseActivity;
import com.hoaithanh.qlgh.model.ApiResult;
import com.hoaithanh.qlgh.model.DonDatHang;
import com.hoaithanh.qlgh.model.PricingResponse;
import com.hoaithanh.qlgh.model.PricingRule;
import com.hoaithanh.qlgh.model.goong.DirectionResponse;
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

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.CancellationTokenSource;

public class DonDatHangActivity extends BaseActivity {
    private static final int REQ_LOCATION = 1001;
    private FusedLocationProviderClient fusedClient;
    private CancellationTokenSource cts;
    private ImageButton btnPickCurrentLocation;

    private CardView cardSenderInfo, cardReceiverInfo, cardProductInfo;
    private LinearLayout formSenderInfo, formReceiverInfo, formProductInfo;
    private TextView tvSenderPlaceholder, tvReceiverPlaceholder, tvProductPlaceholder;
    private TextView tvShippingFee, tvCodFee, tvTotal;
    private EditText etCodAmount;

    private boolean isSubmitting = false;
    private boolean isSenderExpanded = false;
    private boolean isReceiverExpanded = false;
    private boolean isProductExpanded = false;
    private ImageButton btnCodInfo;
    private RelativeLayout optionSenderPays, optionReceiverPays;
    private RadioButton rbSenderPays, rbReceiverPays;

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
    private int calculatedShippingFee = 0;
    private double calculatedDistance = 0.0;
    private Button btnCalculateFee;
    private View btnSubmit;
    private PricingRule currentPricingRule;

    @Override
    public void initLayout() {
        setContentView(R.layout.activity_don_dat_hang);
    }

    @Override
    public void initData() {
        loadPricingRule();
    }

    private void loadPricingRule() {
        setDefaultPricing(); // Set mặc định trước

        RetrofitClient.getApi().getActivePricing().enqueue(new Callback<PricingResponse>() {
            @Override
            public void onResponse(Call<PricingResponse> call, Response<PricingResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().success) {
                    currentPricingRule = response.body().data;
                    // Log.d("Pricing", "Đã tải bảng giá mới: " + currentPricingRule.basePrice);
                }
            }
            @Override
            public void onFailure(Call<PricingResponse> call, Throwable t) {
                // Lỗi thì dùng mặc định, không cần báo lỗi làm phiền user
            }
        });
    }

    @Override
    public void initView() {
        session = new SessionManager(this);
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

        EditText etWeight = findViewById(R.id.et_product_weight);
        btnCodInfo = findViewById(R.id.btnCodInfo);
        optionSenderPays = findViewById(R.id.optionSenderPays);
        optionReceiverPays = findViewById(R.id.optionReceiverPays);
        rbSenderPays = findViewById(R.id.rbSenderPays);
        rbReceiverPays = findViewById(R.id.rbReceiverPays);

        etSenderName = findViewById(R.id.et_sender_name);
        etSenderPhone = findViewById(R.id.et_sender_phone);
        actvSenderAddress = findViewById(R.id.actv_sender_address);
        actvReceiverAddress = findViewById(R.id.actv_receiver_address);

        setupClickListeners();
        setupAutocomplete();
        prefillSenderInfo();

//        findViewById(R.id.btn_product_save).setOnClickListener(v -> collapseProductInfo());
        btnCalculateFee = findViewById(R.id.btnCalculateFee);
        btnCalculateFee.setOnClickListener(v -> {
            // 1. Kích hoạt tính phí
            triggerFeeCalculation();
        });

        findViewById(R.id.btn_cancel).setOnClickListener(v -> finish());
        btnSubmit = findViewById(R.id.btn_submit);
        btnSubmit.setOnClickListener(v -> submitOrder());

        fusedClient = LocationServices.getFusedLocationProviderClient(this);
        cts = new CancellationTokenSource();

        btnPickCurrentLocation = findViewById(R.id.btn_pick_current_location);
        btnPickCurrentLocation.setOnClickListener(v -> onPickCurrentLocationClicked());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cts != null) cts.cancel();
        if (senderRunnable != null) uiHandler.removeCallbacks(senderRunnable);
        if (receiverRunnable != null) uiHandler.removeCallbacks(receiverRunnable);
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_LOCATION) {
            boolean granted = true;
            for (int r : grantResults) {
                if (r != PackageManager.PERMISSION_GRANTED) { granted = false; break; }
            }
            if (granted) {
                onPickCurrentLocationClicked();
            } else {
                toast("Bạn cần cấp quyền vị trí để tự động điền địa chỉ.");
            }
        }
    }

    private void setSubmitLoading(boolean loading) {
        if (btnSubmit == null) return;
        btnSubmit.setEnabled(!loading);
        if (btnSubmit instanceof Button) {
            ((Button) btnSubmit).setText(loading ? "Đang gửi..." : "Đặt đơn");
        }
    }

    private void onPickCurrentLocationClicked() {
        boolean fine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
        boolean coarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;

        if (!fine && !coarse) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    REQ_LOCATION
            );
            return;
        }

        setPickButtonLoading(true);

        fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.getToken())
                .addOnSuccessListener(loc -> {
                    if (loc != null) {
                        handleGotLocation(loc);
                    } else {
                        fusedClient.getLastLocation()
                                .addOnSuccessListener(last -> {
                                    if (last != null) handleGotLocation(last);
                                    else {
                                        setPickButtonLoading(false);
                                        toast("Không lấy được vị trí. Hãy bật GPS/định vị.");
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    setPickButtonLoading(false);
                                    toast("Không lấy được vị trí: " + e.getMessage());
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    setPickButtonLoading(false);
                    toast("Không lấy được vị trí: " + e.getMessage());
                });
    }

    private void handleGotLocation(Location loc) {
        double lat = loc.getLatitude();
        double lng = loc.getLongitude();

        String key = BuildConfig.GOONG_API_KEY;
        String latlng = lat + "," + lng;

        goongRepo.reverse(latlng, key).enqueue(new Callback<GeocodingResponse>() {
            @Override
            public void onResponse(Call<GeocodingResponse> call, Response<GeocodingResponse> resp) {
                setPickButtonLoading(false);
                if (!resp.isSuccessful() || resp.body() == null || resp.body().results == null || resp.body().results.isEmpty()) {
                    toast("Không tìm thấy địa chỉ từ vị trí.");
                    // Dù không có địa chỉ, vẫn set lat/lng để submit dùng được
                    senderLat = lat;
                    senderLng = lng;
                    senderPlaceId = null; // GeocodingResponse của bạn hiện không có place_id
                    return;
                }

                GeocodingResponse.Result r = resp.body().results.get(0);
                String addr = (r != null) ? r.formatted_address : null;

                updatingSenderText = true;
                if (!TextUtils.isEmpty(addr)) {
                    actvSenderAddress.setText(addr);
                    actvSenderAddress.dismissDropDown();
                }
                updatingSenderText = false;

                // cập nhật state
                senderLat = lat;
                senderLng = lng;
                senderPlaceId = null; // model hiện không có place_id → để null

                // Lưu session
                session.saveLastPickup(
                        !TextUtils.isEmpty(addr) ? addr : latlng,
                        senderPlaceId,
                        senderLat,
                        senderLng
                );
            }

            @Override
            public void onFailure(Call<GeocodingResponse> call, Throwable t) {
                setPickButtonLoading(false);
                toast("Reverse geocode thất bại: " + t.getMessage());
                // vẫn set lat/lng để có thể submit
                senderLat = lat;
                senderLng = lng;
                senderPlaceId = null;
            }
        });
    }


    private void setPickButtonLoading(boolean loading) {
        btnPickCurrentLocation.setEnabled(!loading);
        btnPickCurrentLocation.setAlpha(loading ? 0.6f : 1f);
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

        btnCodInfo.setOnClickListener(v -> {
            new MaterialAlertDialogBuilder(this)
                    .setTitle("Thu tiền hộ (COD) là gì?")
                    .setMessage("Đây là dịch vụ mà tài xế sẽ ứng trước tiền hàng cho bạn (người gửi). Sau đó, tài xế sẽ thu lại đúng số tiền này từ người nhận khi giao hàng thành công.\n\nPhí dịch vụ COD sẽ được tính dựa trên giá trị của đơn hàng.")
                    .setPositiveButton("Đã hiểu", null)
                    .show();
        });

        optionSenderPays.setOnClickListener(v -> {
            // Chọn "Người gửi trả"
            rbSenderPays.setChecked(true);
            rbReceiverPays.setChecked(false);

            updateFeeUI();
        });

        optionReceiverPays.setOnClickListener(v -> {
            // Chọn "Người nhận trả"
            rbReceiverPays.setChecked(true);
            rbSenderPays.setChecked(false);

            updateFeeUI();
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
            actvSenderAddress.clearFocus();
            actvSenderAddress.dismissDropDown();
        });


        actvReceiverAddress.setOnItemClickListener((parent, view, position, id) -> {
            PlaceAutoCompleteResponse.Prediction p = receiverAdapter.getPredictionAt(position);
            if (p != null) {
                receiverPlaceId = p.place_id;
                fetchPlaceDetail(p.place_id, false);
            }
            actvReceiverAddress.clearFocus();
            actvReceiverAddress.dismissDropDown();
        });

        // Debounce khi gõ + không reset placeId khi setText bằng code
        actvSenderAddress.addTextChangedListener(new SimpleTextWatcher(text -> {
            if (!updatingSenderText) {
                senderPlaceId = null;
                senderLat = 0;
                senderLng = 0;
            }
            debounceAutocomplete(text, true);
        }));
        actvReceiverAddress.addTextChangedListener(new SimpleTextWatcher(text -> {
            if (!updatingReceiverText) {
                receiverPlaceId = null;
                receiverLat = 0;
                receiverLng = 0;
            }
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
//                            actvSenderAddress.showDropDown();
                            if (actvSenderAddress.hasFocus()) {
                                actvSenderAddress.showDropDown();
                            }
                        } else {
                            receiverAdapter.setPredictions(resp.body().predictions);
//                            actvReceiverAddress.showDropDown();
                            if (actvReceiverAddress.hasFocus()) {   // 🔹 VÀ DÒNG NÀY
                                actvReceiverAddress.showDropDown();
                            }
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
        // Hiển thị thông tin cân nặng thay vì tên
        EditText etWeight = findViewById(R.id.et_product_weight);
        if (!TextUtils.isEmpty(etWeight.getText())) {
            tvProductPlaceholder.setText("Khối lượng: " + etWeight.getText().toString() + " kg");
        } else {
            tvProductPlaceholder.setText("Nhấn để nhập thông tin");
        }
        isProductExpanded = false;
    }

    private void updateFeeUI() {
        // Lấy phí ship đã được tính toán và lưu lại
        int shippingFee = calculatedShippingFee;

        double codAmount = 0;
        try {
            String codStr = etCodAmount.getText().toString().trim();
            if (!codStr.isEmpty()) codAmount = Double.parseDouble(codStr);
        } catch (NumberFormatException ignore) {}

        // Logic tính phí COD (giữ nguyên)
        int codFee = 0;
        if (codAmount > 0) {
            codFee = (int) Math.round(codAmount * 0.01);
            if (codFee < 5000) codFee = 5000;
            if (codFee > 15000) codFee = 15000;
        }

        // Logic tính Tổng cộng (giữ nguyên)
        int total = shippingFee + codFee;

        tvShippingFee.setText(String.format("%,dđ", shippingFee));
        tvCodFee.setText(String.format("%,dđ", codFee));
        tvTotal.setText(String.format("%,dđ", total));
    }

    /**
     * Bước 1: Kích hoạt khi người dùng nhấn nút "Tính phí & Lưu"
     */
    private void triggerFeeCalculation() {
        // 1. Lấy cân nặng
        double weight = 0;
        try {
            String weightStr = ((EditText) findViewById(R.id.et_product_weight)).getText().toString().trim();
            if (!weightStr.isEmpty()) weight = Double.parseDouble(weightStr);
        } catch (NumberFormatException ignore) {}

        // 2. Kiểm tra điều kiện (Phải có đủ 2 địa chỉ và cân nặng)
        if (weight <= 0) {
            toast("Vui lòng nhập cân nặng hợp lệ.");
            return;
        }
        if (senderLat == 0 || senderLng == 0 || receiverLat == 0 || receiverLng == 0) {
            toast("Vui lòng nhập đầy đủ địa chỉ lấy và giao hàng.");
            return;
        }

        // 3. Nếu đủ điều kiện, gọi Goong
        callGoongToGetDistanceAndFee(senderLat, senderLng, receiverLat, receiverLng, weight);
    }

    /**
     * Bước 2: Gọi API Goong để lấy quãng đường
     */
    private void callGoongToGetDistanceAndFee(double pLat, double pLng, double dLat, double dLng, double weight) {
        setLoading(true); // Hiển thị ProgressBar

        String origin = pLat + "," + pLng;
        String dest = dLat + "," + dLng;
        String goongKey = BuildConfig.GOONG_API_KEY;
        goongRepo.getRoute(origin, dest, "bike", goongKey).enqueue(new Callback<DirectionResponse>() {
            @Override
            public void onResponse(Call<DirectionResponse> call, Response<DirectionResponse> response) {
                setLoading(false);
                if (!response.isSuccessful() || response.body() == null || response.body().routes.isEmpty() ||
                        response.body().routes.get(0).legs == null || response.body().routes.get(0).legs.length == 0) {
                    toast("Không thể tính quãng đường. Vui lòng thử lại.");
                    return;
                }

                double distanceInMeters = response.body().routes.get(0).legs[0].distance.value;
                double distanceInKm = distanceInMeters / 1000.0;

                // 5. Tính phí cuối cùng và LƯU LẠI
                calculatedShippingFee = calculateFinalFee(weight, distanceInKm);
                calculatedDistance = distanceInKm;
                // 6. Cập nhật UI chi phí
                updateFeeUI();

                // 7. Thu gọn Card
                collapseProductInfo();
            }

            @Override
            public void onFailure(Call<DirectionResponse> call, Throwable t) {
                setLoading(false);
                toast("Lỗi mạng khi tính phí: " + t.getMessage());
            }
        });
    }

    /**
     * Bước 3: HÀM LOGIC NGHIỆP VỤ MỚI (Công thức của bạn)
     */
    private int calculateFinalFee(double weight, double distanceInKm) {
        // 1. Lấy tham số từ DB
        double baseDist = currentPricingRule.baseDistance;
        double basePrice = currentPricingRule.basePrice;
        double perKm = currentPricingRule.pricePerKm;
        double perKg = currentPricingRule.pricePerKg;
        double freeWeightLimit = currentPricingRule.freeWeight; // <-- LẤY TỪ DB

        // 2. Tính Phí Quãng đường
        double distanceFee;
        if (distanceInKm <= baseDist) {
            distanceFee = basePrice;
        } else {
            double extraKm = Math.ceil(distanceInKm - baseDist);
            distanceFee = basePrice + (extraKm * perKm);
        }

        // 3. Tính Phụ phí Cân nặng (Dùng biến động)
        double weightFee = 0;

        if (weight > freeWeightLimit) { // So sánh với biến từ DB
            double extraWeight = Math.ceil(weight - freeWeightLimit);
            weightFee = extraWeight * perKg;
        }

        // 4. Tổng cộng
        return (int) (distanceFee + weightFee);
    }

    private void setLoading(boolean loading) {
        if (btnCalculateFee != null) {
            btnCalculateFee.setEnabled(!loading);
            btnCalculateFee.setText(loading ? "Đang tính..." : "Tính phí & Lưu");
        }
    }

    private void submitOrder() {
        if (isSubmitting) return;

        if (!validateForm()) return;

        // disable nút submit
        isSubmitting = true;
        setSubmitLoading(true);
//        View btnSubmit = findViewById(R.id.btn_submit);
//        btnSubmit.setEnabled(false);

        // Lấy dữ liệu từ form
        String customerName = ((EditText) findViewById(R.id.et_sender_name)).getText().toString().trim();
        String phoneNumber  = ((EditText) findViewById(R.id.et_sender_phone)).getText().toString().trim();
        String pickAddress  = actvSenderAddress.getText().toString().trim();

        String recipient    = ((EditText) findViewById(R.id.et_receiver_name)).getText().toString().trim();
        String recipPhone   = ((EditText) findViewById(R.id.et_receiver_phone)).getText().toString().trim();
        String delivAddress = actvReceiverAddress.getText().toString().trim();

        double weight = 0;
        try { weight = Double.parseDouble(((EditText) findViewById(R.id.et_product_weight)).getText().toString().trim()); }
        catch (Exception ignore) {}

        double codAmount = 0;
        try { codAmount = Double.parseDouble(etCodAmount.getText().toString().trim()); }
        catch (Exception ignore) {}

        String feePayer = rbSenderPays.isChecked() ? "sender" : "receiver";

        String note = ((EditText) findViewById(R.id.et_product_note)).getText().toString().trim();

        if (TextUtils.isEmpty(customerName) || TextUtils.isEmpty(phoneNumber) ||
                TextUtils.isEmpty(pickAddress) || TextUtils.isEmpty(recipient) ||
                TextUtils.isEmpty(recipPhone) || TextUtils.isEmpty(delivAddress) ||
                weight <= 0) {
            toast("Vui lòng nhập đầy đủ thông tin bắt buộc");
            resetSubmitState();
            return;
        }

        String status = "pending";

        if (senderPlaceId != null && (senderLat == 0 && senderLng == 0)) {
            toast("Đang lấy tọa độ điểm lấy hàng, vui lòng đợi...");
            resetSubmitState();
            return;
        }
        if (receiverPlaceId != null && (receiverLat == 0 && receiverLng == 0)) {
            toast("Đang lấy tọa độ điểm giao hàng, vui lòng đợi...");
            resetSubmitState();
            return;
        }

        if (calculatedShippingFee <= 0) {
            toast("Vui lòng nhấn 'Tính phí & Lưu' trước khi đặt đơn.");
            resetSubmitState();
            return;
        }

        boolean needGeoSender   = (senderPlaceId == null && (senderLat == 0 && senderLng == 0));
        boolean needGeoReceiver = (receiverPlaceId == null && (receiverLat == 0 && receiverLng == 0));

        if (needGeoSender || needGeoReceiver) {
            geocodeAndSubmit(customerName, phoneNumber, pickAddress,
                    delivAddress, recipient, recipPhone,
                    status, codAmount, weight, note, feePayer);
        } else {
            callCreateOrder(customerName, phoneNumber, pickAddress,
                    senderLat, senderLng, delivAddress,
                    receiverLat, receiverLng, recipient, recipPhone,
                    status, codAmount, weight, note, feePayer,
                    calculatedDistance);
        }
    }

    private void resetSubmitState() {
//        isSubmitting = false;
//        View btnSubmit = findViewById(R.id.btn_submit);
//        btnSubmit.setEnabled(true);
        isSubmitting = false;
        setSubmitLoading(false);
    }



    private boolean isValidPhone(String s) {
        return s != null && s.matches("0\\d{9,10}");
    }

    private boolean validateForm() {
        if (TextUtils.isEmpty(etSenderName.getText())) { toast("Nhập tên người gửi"); return false; }
        if (!isValidPhone(etSenderPhone.getText().toString())) { toast("SĐT người gửi không hợp lệ"); return false; }
        if (TextUtils.isEmpty(actvSenderAddress.getText())) { toast("Nhập địa chỉ lấy hàng"); return false; }

//        if (TextUtils.isEmpty(etName.getText())) { toast("Nhập tên người nhận"); return false; }
//        if (!isValidPhone(etReceiverPhone.getText().toString())) { toast("SĐT người nhận không hợp lệ"); return false; }
        if (TextUtils.isEmpty(actvReceiverAddress.getText())) { toast("Nhập địa chỉ giao hàng"); return false; }

//        double w = 0;
//        try { w = Double.parseDouble(etProductWeight.getText().toString().trim()); } catch (Exception ignore) {}
//        if (w <= 0) { toast("Khối lượng phải > 0"); return false; }

        return true;
    }
    private void toast(String m){ Toast.makeText(this, m, Toast.LENGTH_SHORT).show(); }

    // Fallback Geocoding khi người dùng nhập tay
    private void geocodeAndSubmit(String customerName, String phoneNumber,
                                  String pickAddress, String delivAddress,
                                  String recipient, String recipPhone,
                                  String status, double codAmount,
                                  double weight, String note, String feePayer) {

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
                                recipient, recipPhone, status, codAmount, weight, note, feePayer, calculatedDistance);
                    }

                    @Override
                    public void onFailure(Call<GeocodingResponse> call, Throwable t) {
                        callCreateOrder(customerName, phoneNumber, pickAddress,
                                pLatRef.get(), pLngRef.get(),
                                delivAddress, null, null,
                                recipient, recipPhone, status, codAmount, weight, note, feePayer, calculatedDistance);
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
                                recipient, recipPhone, status, codAmount, weight, note, feePayer, calculatedDistance);
                    }

                    @Override
                    public void onFailure(Call<GeocodingResponse> call, Throwable t2) {
                        callCreateOrder(customerName, phoneNumber, pickAddress,
                                null, null,
                                delivAddress, null, null,
                                recipient, recipPhone, status, codAmount, weight, note, feePayer, calculatedDistance);
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
                                 double weight, String note, String feePayer, double distance) {

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
                note,
                feePayer,
                calculatedShippingFee,
                distance
        ).enqueue(new Callback<ApiResult>() {
            @Override
            public void onResponse(Call<ApiResult> call, Response<ApiResult> response) {
                resetSubmitState();
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
                resetSubmitState();
                Toast.makeText(DonDatHangActivity.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void setDefaultPricing() {
        currentPricingRule = new PricingRule();
        currentPricingRule.baseDistance = 2.0;
        currentPricingRule.basePrice = 15000;
        currentPricingRule.pricePerKm = 5000;
        currentPricingRule.pricePerKg = 2500;
        currentPricingRule.freeWeight = 3.0;
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

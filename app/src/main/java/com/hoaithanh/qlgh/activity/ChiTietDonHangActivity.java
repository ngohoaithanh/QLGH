package com.hoaithanh.qlgh.activity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.VectorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.hoaithanh.qlgh.BuildConfig;
import com.hoaithanh.qlgh.R;
import com.hoaithanh.qlgh.adapter.TrackingHistoryAdapter;
import com.hoaithanh.qlgh.base.BaseActivity;
import com.hoaithanh.qlgh.model.DonDatHang;
import com.hoaithanh.qlgh.model.goong.DirectionResponse;
import com.hoaithanh.qlgh.model.goong.LatLng;
import com.hoaithanh.qlgh.model.goong.PolylineDecoder;
import com.hoaithanh.qlgh.repository.GoongRepository;
import com.hoaithanh.qlgh.viewmodel.DonDatHangViewModel;

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChiTietDonHangActivity extends BaseActivity {

    // --- Các biến UI ---
    private MapView mapView;
    private IMapController mapController;
    private BottomSheetBehavior<FrameLayout> bottomSheetBehavior;
    private DonDatHangViewModel viewModel;
    private DonDatHang currentOrder;

    private TextView tvStatusTitle, tvEta, tvShipperName, tvShipperRating, tvVehicleInfo;
    private TextView tvSenderName, tvSenderPhone, tvReceiverName, tvReceiverPhone;
    private TextView tvTrafficWarning;
    private long lastEtaValueInSeconds = -1;// Biến lưu ETA (tính bằng giây) của lần trước
    private MaterialCardView cardNote;
    private TextView tvOrderNote;
    private TextView tvPickupAddress, tvDeliveryAddress;
    private TextView tvShippingFee, tvCodAmount, tvTotalAmount, tvCodFee;
    private ImageButton btnCallShipper, btnMessageShipper;

    private ImageView ivProgress1, ivProgress2, ivProgress3;
    private View lineProgress1, lineProgress2;

    private RecyclerView rvTrackingHistory;
    private TrackingHistoryAdapter trackingAdapter;

    // --- Các biến cho Bản đồ (lấy từ ShipperOrdersDetailActivity) ---
    private GoongRepository goongRepo;
    private String goongKey;

    // --- CÁC BIẾN MỚI CHO THEO DÕI THỜI GIAN THỰC ---
    private Handler trackingHandler = new Handler();
    private Runnable trackingRunnable;
    private Marker shipperMarker;
    private static final long TRACKING_INTERVAL = 15000; // 15 giây

    // Biến để lưu vị trí cuối cùng đã cập nhật
    private double lastTrackedLat = Double.NaN;
    private double lastTrackedLng = Double.NaN;
    private static final float MIN_TRACKING_DISTANCE = 50f; // 50 mét

    // --- BIẾN MỚI ĐỂ GIỚI HẠN VẼ LẠI TUYẾN ĐƯỜNG ---
    private long lastRouteDrawTime = 0L;
    private double lastRouteDrawLat = Double.NaN;
    private double lastRouteDrawLng = Double.NaN;
    private static final long MIN_ROUTE_DRAW_INTERVAL = 15000; // 15 giây
    private static final float MIN_ROUTE_DRAW_DISTANCE = 50f; // 50 mét

    private Polyline currentRoutePolyline;

    // Thêm các biến này vào đầu class
    private MaterialCardView cardRating;
    private RatingBar ratingBar;
    private MaterialButton btnSubmitRating;

    private MaterialButton btnCancelOrder;

    @Override
    public void initLayout() {
        setContentView(R.layout.activity_chi_tiet_don_hang);
    }

    @Override
    public void initData() { }

    @Override
    public void initView() {
        Configuration.getInstance().setUserAgentValue(getPackageName());

        ivProgress1 = findViewById(R.id.ivProgress1);
        ivProgress2 = findViewById(R.id.ivProgress2);
        ivProgress3 = findViewById(R.id.ivProgress3);
        lineProgress1 = findViewById(R.id.lineProgress1);
        lineProgress2 = findViewById(R.id.lineProgress2);
        tvPickupAddress = findViewById(R.id.tvPickupAddress);
        tvDeliveryAddress = findViewById(R.id.tvDeliveryAddress);

        cardRating = findViewById(R.id.cardRating);
        ratingBar = findViewById(R.id.ratingBar);
        btnSubmitRating = findViewById(R.id.btnSubmitRating);
        btnCancelOrder = findViewById(R.id.btnCancelOrder);

        tvShippingFee = findViewById(R.id.tvShippingFee);
        tvCodAmount = findViewById(R.id.tvCodAmount);
        tvTotalAmount = findViewById(R.id.tvTotalAmount);
        tvCodFee = findViewById(R.id.tvCodFee);

        tvSenderName = findViewById(R.id.tvSenderName);
        tvSenderPhone = findViewById(R.id.tvSenderPhone);
        tvReceiverName = findViewById(R.id.tvReceiverName);
        tvReceiverPhone = findViewById(R.id.tvReceiverPhone);
        cardNote = findViewById(R.id.cardNote);
        tvOrderNote = findViewById(R.id.tvOrderNote);

        btnCallShipper = findViewById(R.id.btnCallShipper);
        btnMessageShipper = findViewById(R.id.btnMessageShipper);

        // --- Ánh xạ View ---
        mapView = findViewById(R.id.mapView);
        mapController = mapView.getController();
        mapView.setMultiTouchControls(true);
        mapView.getZoomController().setVisibility(org.osmdroid.views.CustomZoomButtonsController.Visibility.NEVER);
        mapController.setZoom(15.0);

        // ... (ánh xạ các view khác như cũ) ...
        FrameLayout bottomSheetContainer = findViewById(R.id.bottom_sheet_container);
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheetContainer);
        tvStatusTitle = findViewById(R.id.tvStatusTitle);
        tvEta = findViewById(R.id.tvEta);
        tvTrafficWarning = findViewById(R.id.tvTrafficWarning);
        tvShipperName = findViewById(R.id.tvShipperName);
        tvShipperRating = findViewById(R.id.tvShipperRating);
        tvVehicleInfo = findViewById(R.id.tvVehicleInfo);
        // ... (các view khác)
        rvTrackingHistory = findViewById(R.id.rvTrackingHistory);
        rvTrackingHistory.setLayoutManager(new LinearLayoutManager(this));
        trackingAdapter = new TrackingHistoryAdapter();
        rvTrackingHistory.setAdapter(trackingAdapter);

        // --- Khởi tạo các biến cần thiết ---
        goongRepo = new GoongRepository();
        goongKey = BuildConfig.GOONG_API_KEY;

        btnCancelOrder.setOnClickListener(v -> {
            new MaterialAlertDialogBuilder(this)
                    .setTitle("Xác nhận hủy đơn")
                    .setMessage("Bạn có chắc chắn muốn hủy đơn hàng này không?")
                    .setNegativeButton("Không", null)
                    .setPositiveButton("Hủy đơn", (dialog, which) -> {
                        btnCancelOrder.setEnabled(false); // Vô hiệu hóa nút
                        viewModel.cancelOrder(Integer.parseInt(currentOrder.getID()));
                    })
                    .show();
        });

        // Trong hàm initView() hoặc setupClickListeners()

        btnCallShipper.setOnClickListener(v -> {
            // 1. Kiểm tra xem đã có thông tin đơn hàng và SĐT shipper chưa
            if (currentOrder == null || currentOrder.getShipperPhoneNumber() == null || currentOrder.getShipperPhoneNumber().trim().isEmpty()) {
                Toast.makeText(this, "Chưa có thông tin số điện thoại shipper.", Toast.LENGTH_SHORT).show();
                return;
            }

            // 2. Lấy và làm sạch số điện thoại
            String phoneNumber = currentOrder.getShipperPhoneNumber();
            String cleanedPhoneNumber = phoneNumber.replaceAll("[^0-9+*#]", ""); // Xóa các ký tự không phải số

            if (cleanedPhoneNumber.isEmpty()) {
                Toast.makeText(this, "Số điện thoại shipper không hợp lệ.", Toast.LENGTH_SHORT).show();
                return;
            }

            // 3. Tạo Intent để mở trình gọi điện
            Intent intent = new Intent(Intent.ACTION_DIAL);
            intent.setData(Uri.parse("tel:" + cleanedPhoneNumber));

            // 4. Mở trình gọi điện (nên có try-catch)
            try {
                startActivity(intent);
            } catch (android.content.ActivityNotFoundException e) {
                Toast.makeText(this, "Không thể mở ứng dụng gọi điện.", Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
        });

        viewModel = new ViewModelProvider(this).get(DonDatHangViewModel.class);
        observeViewModel();

        // --- Tải Dữ liệu ---
        String orderIdStr = getIntent().getStringExtra("ID");
        if (orderIdStr != null && !orderIdStr.isEmpty()) {
            viewModel.loadOrderDetails(Integer.parseInt(orderIdStr));
        } else {
            Toast.makeText(this, "Không tìm thấy mã đơn hàng", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void observeViewModel() {
//        viewModel.getOrderDetails().observe(this, order -> {
//            if (order != null) {
//                this.currentOrder = order;
//                bindDataToViews(order);
//            } else {
//                Toast.makeText(this, "Không thể tải chi tiết đơn hàng", Toast.LENGTH_SHORT).show();
//            }
//        });
//
//        viewModel.getShipperLocation().observe(this, newLocation -> {
//            if (newLocation != null) {
//                GeoPoint shipperPosition = new GeoPoint(newLocation.getLat(), newLocation.getLng());
//
//                if (shouldUpdateMarker(newLocation.getLat(), newLocation.getLng())) {
//                    lastTrackedLat = newLocation.getLat();
//                    lastTrackedLng = newLocation.getLng();
//
//                    if (shipperMarker != null) {
//                        shipperMarker.setPosition(shipperPosition);
//                        mapView.invalidate();
//                    }
//                }
//                updateRouteBasedOnStatus(currentOrder, shipperPosition);
//            }
//        });

        // --- LẮNG NGHE DỮ LIỆU ĐƠN HÀNG (DÙNG ĐỂ SETUP VÀ CẬP NHẬT BOTTOMSHEET) ---
        viewModel.getOrderDetails().observe(this, order -> {
            if (order == null) {
                // Xảy ra khi loadOrderDetails bị lỗi (ví dụ: tài khoản khóa)
                // Lỗi đã được xử lý bởi Interceptor, ở đây chỉ cần không làm gì
                return;
            }

            // Kiểm tra xem đây có phải là lần đầu tiên tải dữ liệu không
            boolean isFirstLoad = (this.currentOrder == null);

            // Cập nhật đơn hàng hiện tại
            this.currentOrder = order;

            // 1. Luôn luôn cập nhật BottomSheet
            bindDataToViews(order);

            // 2. Chỉ thực hiện các hành động sau MỘT LẦN KHI MỚI VÀO MÀN HÌNH
            if (isFirstLoad) {
                // Vẽ các marker tĩnh (Điểm lấy, Điểm giao)
                drawMap(order);

                // Vẽ tuyến đường TĨNH ban đầu (nếu đơn hàng không phải là 'accepted')
                if (!"accepted".equals(safe(order.getStatus()).toLowerCase())) {
                    updateRouteBasedOnStatus(order, null);
                }

                // Bắt đầu vòng lặp theo dõi (chỉ gọi 1 lần)
                startRealtimeTracking(order);
            }
        });

        // --- LẮNG NGHE VỊ TRÍ SHIPPER (ĐỂ CẬP NHẬT BẢN ĐỒ REAL-TIME) ---
        viewModel.getShipperLocation().observe(this, newLocation -> {
            if (newLocation != null && currentOrder != null) {
                GeoPoint shipperPosition = new GeoPoint(newLocation.getLat(), newLocation.getLng());

                // 1. Cập nhật vị trí marker của shipper
                if (shouldUpdateMarker(newLocation.getLat(), newLocation.getLng())) {
                    lastTrackedLat = newLocation.getLat();
                    lastTrackedLng = newLocation.getLng();

                    if (shipperMarker != null) {
                        shipperMarker.setPosition(shipperPosition);
                        mapView.invalidate();
                    }
                }

                // 2. Cập nhật tuyến đường ĐỘNG (chỉ khi status là 'accepted')
                updateRouteBasedOnStatus(currentOrder, shipperPosition);
            }
        });


        // LẮNG NGHE KẾT QUẢ GỬI ĐÁNH GIÁ
        viewModel.getSubmitRatingResult().observe(this, result -> {
            if (result != null && result.isSuccess()) {
                Toast.makeText(this, "Cảm ơn bạn đã đánh giá!", Toast.LENGTH_SHORT).show();
                // Ẩn card đánh giá sau khi thành công
                cardRating.setVisibility(View.GONE);
            } else {
                Toast.makeText(this, "Gửi đánh giá thất bại, vui lòng thử lại.", Toast.LENGTH_SHORT).show();
                // Kích hoạt lại nút để người dùng thử lại
                btnSubmitRating.setEnabled(true);
                btnSubmitRating.setText("Gửi đánh giá");
            }
        });

        // LẮNG NGHE KẾT QUẢ HỦY ĐƠN
        viewModel.getCancelOrderResult().observe(this, result -> {
            if (result != null && result.isSuccess()) {
                Toast.makeText(this, "Hủy đơn hàng thành công!", Toast.LENGTH_SHORT).show();
                // Yêu cầu tải lại dữ liệu để cập nhật trạng thái mới nhất
                viewModel.loadOrderDetails(Integer.parseInt(currentOrder.getID()));
            } else {
                String message = (result != null) ? result.getMessage() : "Có lỗi xảy ra";
                Toast.makeText(this, "Lỗi: " + message, Toast.LENGTH_LONG).show();
                btnCancelOrder.setEnabled(true); // Kích hoạt lại nút để thử lại
            }
        });
    }

    private boolean isOrderActive(DonDatHang order) {
        if (order == null || order.getStatus() == null) {
            return false;
        }
        String status = order.getStatus().toLowerCase();
        // Chỉ theo dõi vị trí khi đơn hàng đang ở các trạng thái này
        switch (status) {
            case "accepted":
            case "picked_up":
            case "in_transit":
                return true;
            default:
                return false;
        }
    }

    private void startRealtimeTracking(DonDatHang order) {
        // CHỈ CẦN MỘT LẦN KIỂM TRA LÀ ĐỦ
        if (!isOrderActive(order) || order.getShipperID() == null) {
            return;
        }

        if (shipperMarker == null) {
            shipperMarker = new Marker(mapView);
            shipperMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
            Drawable shipperIcon = resizeDrawable(R.drawable.ic_shipper_moto, 24); // Tăng kích thước icon một chút
            if (shipperIcon != null) {
                shipperMarker.setIcon(shipperIcon);
            }
            mapView.getOverlays().add(shipperMarker);
        }

        lastTrackedLat = Double.NaN;
        lastTrackedLng = Double.NaN;

        // Dừng các vòng lặp cũ trước khi bắt đầu cái mới để tránh chạy song song
        stopRealtimeTracking();

        trackingRunnable = new Runnable() {
            @Override
            public void run() {
                // Luôn kiểm tra bằng currentOrder để có trạng thái mới nhất
                if (!isOrderActive(currentOrder)) {
                    stopRealtimeTracking();
                    return;
                }

                // SỬA LỖI: Luôn dùng currentOrder để lấy ID shipper
                viewModel.fetchShipperLocation(Integer.parseInt(currentOrder.getShipperID()));
                viewModel.loadOrderDetails(Integer.parseInt(currentOrder.getID()));

                // SỬA LỖI: CHỈ GỌI postDelayed MỘT LẦN DUY NHẤT
                trackingHandler.postDelayed(this, TRACKING_INTERVAL);
            }
        };

        trackingHandler.post(trackingRunnable);
    }

    private boolean shouldUpdateMarker(double newLat, double newLng) {
        // Luôn cập nhật ở lần đầu tiên
        if (Double.isNaN(lastTrackedLat) || Double.isNaN(lastTrackedLng)) {
            return true;
        }

        // Tính khoảng cách di chuyển
        float[] distance = new float[1];
        android.location.Location.distanceBetween(lastTrackedLat, lastTrackedLng, newLat, newLng, distance);

        // Chỉ cập nhật nếu di chuyển lớn hơn 50 mét
        return distance[0] > MIN_TRACKING_DISTANCE;
    }

    private void stopRealtimeTracking() {
        if (trackingHandler != null && trackingRunnable != null) {
            trackingHandler.removeCallbacks(trackingRunnable);
        }
    }

    private void bindDataToViews(DonDatHang order) {
//        stopRealtimeTracking();
//        lastRouteDrawTime = 0L;
//        if (currentRoutePolyline != null) {
//            mapView.getOverlays().remove(currentRoutePolyline);
//            currentRoutePolyline = null;
//        }

        // --- Trạng thái & ETA ---
        tvStatusTitle.setText(getStatusText(order.getStatus()));

        tvSenderName.setText(safe(order.getUserName()));
        tvSenderPhone.setText(safe(order.getPhoneNumberCus()));
        tvReceiverName.setText(safe(order.getRecipient()));
        tvReceiverPhone.setText(safe(order.getRecipientPhone()));
        String note = safe(order.getNote());
        if (!note.isEmpty()) {
            tvOrderNote.setText(note);
            cardNote.setVisibility(View.VISIBLE);
        } else {
            cardNote.setVisibility(View.GONE);
        }

        // --- Shipper & Xe ---
        tvShipperName.setText(safe(order.getShipperName()));

        // Xử lý và gán điểm rating
        String rating = "0.0";
        if (order.getShipperRating() != null && !order.getShipperRating().isEmpty()) {
            try {
                double ratingValue = Double.parseDouble(order.getShipperRating());
                rating = String.format(Locale.US, "%.1f", ratingValue);
            } catch (NumberFormatException e) {
                rating = order.getShipperRating();
            }
        }
        tvShipperRating.setText(rating);

        if (order.getVehicle() != null) {
            String vehicleInfo = safe(order.getVehicle().getLicensePlate()) + " • " + safe(order.getVehicle().getModel());
            tvVehicleInfo.setText(vehicleInfo);
            tvVehicleInfo.setVisibility(View.VISIBLE);
        } else {
            tvVehicleInfo.setVisibility(View.GONE);
        }

        // --- Thanh tiến trình ---
        updateProgressTracker(order.getStatus());

        // --- Hành trình ---
        tvPickupAddress.setText(safe(order.getPick_up_address()));
        tvDeliveryAddress.setText(safe(order.getDelivery_address()));

        // --- Chi phí ---
        double shippingFee = parseD(order.getShippingfee());
        double cod = parseD(order.getCOD_amount());
        double codFee = parseD(order.getCODFee());
        tvShippingFee.setText(formatCurrency(shippingFee));
        tvCodAmount.setText(formatCurrency(cod));
        tvCodFee.setText(formatCurrency(codFee));
        tvTotalAmount.setText(formatCurrency(shippingFee + codFee));

        // --- Lịch sử Tracking ---
        if (order.getTrackingHistory() != null) {
            trackingAdapter.submitList(order.getTrackingHistory());
        }

        // --- Logic Bản đồ ---
//        drawMap(order);
//        updateRouteBasedOnStatus(order, null);
        if (!"accepted".equals(safe(order.getStatus()).toLowerCase())) {
            Double pLat = toDouble(order.getPick_up_lat());
            Double pLng = toDouble(order.getPick_up_lng());
            Double dLat = toDouble(order.getDelivery_lat());
            Double dLng = toDouble(order.getDelivery_lng());

            if (pLat != null && pLng != null && dLat != null && dLng != null) {
                GeoPoint origin = new GeoPoint(pLat, pLng);
                GeoPoint dest = new GeoPoint(dLat, dLng);
//                fetchAndDrawRoute(origin, dest, isOrderCompleted(order));
            }
        }

        if (isOrderCompleted(order) && !order.isRated()) {
            cardRating.setVisibility(View.VISIBLE);
        } else {
            cardRating.setVisibility(View.GONE);
        }

        btnSubmitRating.setOnClickListener(v -> {
            submitRating();
        });

        String status = safe(order.getStatus()).toLowerCase();

        if (status.equals("pending")) {
            // Có thể hủy
            btnCancelOrder.setVisibility(View.VISIBLE);
            btnCancelOrder.setEnabled(true);
            btnCancelOrder.setText("Hủy đơn hàng");
        } else if (status.equals("accepted") || status.equals("picked_up") || status.equals("in_transit")) {
            // Không thể hủy
            btnCancelOrder.setVisibility(View.VISIBLE);
            btnCancelOrder.setEnabled(false);
            btnCancelOrder.setText("Không thể hủy (Shipper đã nhận đơn)");
        } else {
            // Đơn đã kết thúc, ẩn nút đi
            btnCancelOrder.setVisibility(View.GONE);
        }

//        startRealtimeTracking(order);
    }

    private void submitRating() {
        float rating = ratingBar.getRating();
        // String comment = etRatingComment.getText().toString().trim(); // Nếu cần gửi comment

        if (rating == 0) {
            Toast.makeText(this, "Vui lòng chọn số sao để đánh giá", Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentOrder == null || currentOrder.getShipperID() == null) {
            Toast.makeText(this, "Không tìm thấy thông tin shipper", Toast.LENGTH_SHORT).show();
            return;
        }

        int shipperId = Integer.parseInt(currentOrder.getShipperID());
        int orderId = Integer.parseInt(currentOrder.getID());

        // Vô hiệu hóa nút để tránh gửi nhiều lần
        btnSubmitRating.setEnabled(false);
        btnSubmitRating.setText("Đang gửi...");

        // GỌI VIEWMODEL ĐỂ GỬI ĐÁNH GIÁ
        viewModel.submitShipperRating(shipperId, orderId, rating);
    }

    private void updateProgressTracker(String status) {
        if (status == null) return;

        status = status.toLowerCase();

        // Lấy màu sắc từ file colors.xml
        int activeColor = ContextCompat.getColor(this, R.color.main_route_color); // Màu xanh
        int inactiveColor = ContextCompat.getColor(this, R.color.gray_light_route); // Màu xám

        // --- BƯỚC 1: Reset tất cả về trạng thái mặc định (chưa hoàn thành) ---
        ivProgress1.setImageResource(R.drawable.ic_circle_outline);
        ivProgress2.setImageResource(R.drawable.ic_circle_outline);
        ivProgress3.setImageResource(R.drawable.ic_circle_outline);
        lineProgress1.setBackgroundColor(inactiveColor);
        lineProgress2.setBackgroundColor(inactiveColor);

        // --- BƯỚC 2: Cập nhật giao diện dựa trên trạng thái hiện tại ---

        // Giai đoạn 1: Đã lấy hàng (hoặc các giai đoạn sau nó)
        if (status.equals("picked_up") || status.equals("in_transit") || status.equals("delivered")) {
            ivProgress1.setImageResource(R.drawable.ic_checkmark_circle);
        }

        // Giai đoạn 2: Đang giao hàng (hoặc các giai đoạn sau nó)
        if (status.equals("in_transit") || status.equals("delivered")) {
            lineProgress1.setBackgroundColor(activeColor); // Kích hoạt đường kẻ 1
            ivProgress2.setImageResource(R.drawable.ic_checkmark_circle);
        }

        // Giai đoạn 3: Đã giao hàng
        if (status.equals("delivered")) {
            lineProgress2.setBackgroundColor(activeColor); // Kích hoạt đường kẻ 2
            ivProgress3.setImageResource(R.drawable.ic_checkmark_circle);
        }
    }

    private String getStatusText(String status) {
        if (status == null) return "Không xác định";
        switch (status.toLowerCase()) {
            case "pending": return "Chờ xác nhận";
            case "accepted": return "Shipper đang đến lấy hàng";
            case "picked_up": return "Đã lấy hàng";
            case "in_transit": return "Đang giao hàng";
            case "delivered": return "Giao hàng thành công";
            case "delivery_failed": return "Giao hàng thất bại";
            case "cancelled": return "Đã hủy";
            default: return status;
        }
    }

    private static String safe(String s) {
        return s == null ? "" : s.trim();
    }

    private static double parseD(String s) {
        try {
            return Double.parseDouble(s == null ? "0" : s.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static String formatCurrency(double v) {
        NumberFormat nf = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        return nf.format(v);
    }

    // =============================================================
    // ## CÁC HÀM XỬ LÝ BẢN ĐỒ (LẤY TỪ SHIPPERORDERSDETAILACTIVITY) ##
    // =============================================================

    private void drawMap(DonDatHang order) {
        if (mapView == null || order == null) return;

        // Chỉ xóa các marker và tuyến đường cũ khi bắt đầu
        mapView.getOverlays().clear();

        Double pLat = toDouble(order.getPick_up_lat());
        Double pLng = toDouble(order.getPick_up_lng());
        Double dLat = toDouble(order.getDelivery_lat());
        Double dLng = toDouble(order.getDelivery_lng());

        if (pLat != null && pLng != null && dLat != null && dLng != null) {
            GeoPoint startPoint = new GeoPoint(pLat, pLng);
            GeoPoint endPoint = new GeoPoint(dLat, dLng);

            // Luôn thêm marker điểm lấy và điểm giao
            addMarker(startPoint, R.drawable.ic_sender, "Điểm lấy");
            addMarker(endPoint, R.drawable.ic_receiver, "Điểm giao");
        }
    }

    private void updateRouteBasedOnStatus(DonDatHang order, @Nullable GeoPoint shipperLocation) {
        if (order == null) return;

        String status = order.getStatus().toLowerCase();
        GeoPoint origin = null;
        GeoPoint dest = null;

        // Xác định điểm đầu và điểm cuối dựa trên trạng thái
        switch (status) {
            case "accepted":
                if (shipperLocation != null) {
                    origin = shipperLocation; // Từ vị trí shipper...
                    dest = new GeoPoint(toDouble(order.getPick_up_lat()), toDouble(order.getPick_up_lng())); // ...đến điểm lấy hàng
                }
                break;
            case "picked_up":
            case "in_transit":
                if (shipperLocation != null) {
                    origin = shipperLocation;
                    dest = new GeoPoint(toDouble(order.getDelivery_lat()), toDouble(order.getDelivery_lng()));
                }
                break;
            case "delivered":
            case "delivery_failed":
            case "cancelled":
                origin = new GeoPoint(toDouble(order.getPick_up_lat()), toDouble(order.getPick_up_lng())); // Từ điểm lấy hàng...
                dest = new GeoPoint(toDouble(order.getDelivery_lat()), toDouble(order.getDelivery_lng())); // ...đến điểm giao hàng
                break;
        }

        if (origin != null && dest != null) {
            // Kiểm tra xem có nên vẽ lại tuyến đường không
            if (shouldRedrawRoute(origin)) {
                fetchAndDrawRoute(origin, dest, isOrderCompleted(order));
            }
        }
    }

    private boolean shouldRedrawRoute(GeoPoint newOrigin) {
        // Luôn vẽ lần đầu tiên
        if (lastRouteDrawTime == 0L) {
            return true;
        }

        if (isOrderCompleted(currentOrder)) {
            return false;
        }

        long now = System.currentTimeMillis();
        // Giới hạn theo thời gian
        if (now - lastRouteDrawTime < MIN_ROUTE_DRAW_INTERVAL) {
            return false;
        }

        // Giới hạn theo khoảng cách
        if (Double.isNaN(lastRouteDrawLat) || Double.isNaN(lastRouteDrawLng)) {
            return true;
        }
//        float[] distance = new float[1];
//        android.location.Location.distanceBetween(lastRouteDrawLat, lastRouteDrawLng, newOrigin.getLatitude(), newOrigin.getLongitude(), distance);
//
//        return distance[0] > MIN_ROUTE_DRAW_DISTANCE;

        return true;
    }

    private void fetchAndDrawRoute(GeoPoint origin, GeoPoint dest, final boolean isCompleted) {
        String originStr = origin.getLatitude() + "," + origin.getLongitude();
        String destStr = dest.getLatitude()   + "," + dest.getLongitude();

        goongRepo.getRoute(originStr, destStr, "car", goongKey).enqueue(new Callback<DirectionResponse>() {
            @Override
            public void onResponse(Call<DirectionResponse> call, Response<DirectionResponse> response) {
                if (!response.isSuccessful() || response.body() == null ||
                        response.body().routes == null || response.body().routes.isEmpty()) {
                    tvEta.setText("Không tìm thấy tuyến đường.");
                    return;
                }

                DirectionResponse.Route route = response.body().routes.get(0);

//                if (!isCompleted && route.legs != null && route.legs.length > 0 && route.legs[0].duration != null) {
//                    tvEta.setText("Dự kiến đến sau " + route.legs[0].duration.text);
//                } else if (isCompleted) {
//                    tvEta.setText("Đơn hàng đã hoàn thành");
//                }

                // --- BẮT ĐẦU LOGIC XỬ LÝ ETA VÀ KẸT XE ---
                if (route.legs != null && route.legs.length > 0 && route.legs[0].duration != null) {

                    long newEtaValue = route.legs[0].duration.value; // ETA mới (tính bằng giây)
                    String newEtaText = route.legs[0].duration.text; // ETA mới (văn bản)

                    // Cập nhật ETA lên giao diện
                    tvEta.setText("Dự kiến đến sau " + newEtaText);

                    // Kiểm tra kẹt xe (chỉ chạy sau lần đầu tiên)
                    if (lastEtaValueInSeconds != -1 && !isOrderCompleted(currentOrder)) {
                        // Tính toán thời gian đã trôi qua (khoảng 15s)
                        long expectedDecreaseInSeconds = TRACKING_INTERVAL / 1000;

                        // Nếu ETA mới LỚN HƠN (ETA cũ - thời gian đã trôi qua)
                        // -> Có nghĩa là thời gian không giảm như dự kiến
                        if (newEtaValue > (lastEtaValueInSeconds - expectedDecreaseInSeconds)) {
                            tvTrafficWarning.setVisibility(View.VISIBLE); // HIỆN CẢNH BÁO
                        } else {
                            tvTrafficWarning.setVisibility(View.GONE); // ẨN CẢNH BÁO
                        }
                    }

                    // Lưu lại ETA của lần này để so sánh với lần sau
                    lastEtaValueInSeconds = newEtaValue;

                } else if (isCompleted) {
                    tvEta.setText("Đơn hàng đã hoàn thành");
                    tvTrafficWarning.setVisibility(View.GONE);
                } else {
                    tvEta.setText("ETA --:--");
                    tvTrafficWarning.setVisibility(View.GONE);
                }

                String encoded = (route.overview_polyline != null) ? route.overview_polyline.points : null;
                if (encoded == null) return;

                List<LatLng> decoded = PolylineDecoder.decode(encoded);
                List<GeoPoint> geoPts = new ArrayList<>();
                for (LatLng p : decoded) geoPts.add(new GeoPoint(p.latitude, p.longitude));

                // --- XÓA TUYẾN ĐƯỜNG CŨ TRƯỚC KHI VẼ MỚI ---
                if (currentRoutePolyline != null) {
                    mapView.getOverlays().remove(currentRoutePolyline);
                }
                Polyline line = new Polyline();
                line.setPoints(geoPts);
                line.setWidth(8f);

                if (isCompleted) {
                    line.setColor(ContextCompat.getColor(ChiTietDonHangActivity.this, R.color.gray_light_route));
                } else {
                    line.setColor(ContextCompat.getColor(ChiTietDonHangActivity.this, R.color.main_route_color));
                }

                currentRoutePolyline = line; // LƯU LẠI TUYẾN ĐƯỜNG MỚI
                mapView.getOverlays().add(currentRoutePolyline);
                mapView.invalidate();

                // Tự động zoom bản đồ cho vừa với tuyến đường
                BoundingBox box = BoundingBox.fromGeoPoints(geoPts);
                mapView.zoomToBoundingBox(box, true, 120); // 120 là padding

                lastRouteDrawTime = System.currentTimeMillis();
                lastRouteDrawLat = origin.getLatitude();
                lastRouteDrawLng = origin.getLongitude();
            }

            @Override
            public void onFailure(Call<DirectionResponse> call, Throwable t) {
                tvEta.setText("Lỗi tải tuyến đường.");
            }
        });
    }

    private void addMarker(GeoPoint point, int iconRes, String title) {
        Drawable icon = resizeDrawable(iconRes, 24); // 24dp
        if (icon == null) return;

        Marker marker = new Marker(mapView);
        marker.setPosition(point);
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        marker.setIcon(icon);
        marker.setTitle(title);
        mapView.getOverlays().add(marker);
    }

    private Drawable resizeDrawable(int drawableResId, int sizeDp) {
        Drawable drawable = ContextCompat.getDrawable(this, drawableResId);
        if (drawable == null) return null;

        Bitmap bmp;
        if (drawable instanceof BitmapDrawable) {
            bmp = ((BitmapDrawable) drawable).getBitmap();
        } else if (drawable instanceof VectorDrawable || drawable instanceof AnimatedVectorDrawable) {
            bmp = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bmp);
            drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
            drawable.draw(canvas);
        } else {
            return drawable;
        }

        int sizePx = (int) (sizeDp * getResources().getDisplayMetrics().density);
        Bitmap scaledBmp = Bitmap.createScaledBitmap(bmp, sizePx, sizePx, true);
        return new BitmapDrawable(getResources(), scaledBmp);
    }

    private boolean isOrderCompleted(DonDatHang order) {
        if (order == null || order.getStatus() == null) return true;
        String status = order.getStatus().toLowerCase();
        return status.equals("delivered") || status.equals("delivery_failed") || status.equals("cancelled");
    }

    private static @Nullable Double toDouble(String s) {
        try { return s == null ? null : Double.parseDouble(s.trim()); }
        catch (Exception e) { return null; }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mapView != null) {
            mapView.onResume();
        }
    }

    @Override
    protected void onPause() {
        if (mapView != null) {
            mapView.onPause();
        }
        super.onPause();
    }
}
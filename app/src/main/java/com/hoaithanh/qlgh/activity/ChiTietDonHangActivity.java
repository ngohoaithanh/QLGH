package com.hoaithanh.qlgh.activity;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.VectorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
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
    private TextView tvPickupAddress, tvDeliveryAddress;
    private TextView tvShippingFee, tvCodAmount, tvTotalAmount;
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

        tvShippingFee = findViewById(R.id.tvShippingFee);
        tvCodAmount = findViewById(R.id.tvCodAmount);
        tvTotalAmount = findViewById(R.id.tvTotalAmount);

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
        viewModel.getOrderDetails().observe(this, order -> {
            if (order != null) {
                this.currentOrder = order;
                bindDataToViews(order);
            } else {
                Toast.makeText(this, "Không thể tải chi tiết đơn hàng", Toast.LENGTH_SHORT).show();
            }
        });

        viewModel.getShipperLocation().observe(this, newLocation -> {
            if (newLocation != null) {
                GeoPoint shipperPosition = new GeoPoint(newLocation.getLat(), newLocation.getLng());

                if (shouldUpdateMarker(newLocation.getLat(), newLocation.getLng())) {
                    lastTrackedLat = newLocation.getLat();
                    lastTrackedLng = newLocation.getLng();

                    if (shipperMarker != null) {
                        shipperMarker.setPosition(shipperPosition);
                        mapView.invalidate();
                    }
                }
                updateRouteBasedOnStatus(currentOrder, shipperPosition);
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
        // Dừng vòng lặp theo dõi của đơn hàng cũ (nếu có)
        stopRealtimeTracking();
        // Reset bộ đếm thời gian
        lastRouteDrawTime = 0L;
        // Xóa tuyến đường cũ khỏi bản đồ
        if (currentRoutePolyline != null) {
            mapView.getOverlays().remove(currentRoutePolyline);
            currentRoutePolyline = null;
        }
        // --- Trạng thái & ETA ---
        tvStatusTitle.setText(getStatusText(order.getStatus()));
        // TODO: Tính toán ETA và hiển thị lên tvEta. Tạm thời để trống.
        // tvEta.setText("Dự kiến đến: 10:15");

        // --- Shipper & Xe ---
        tvShipperName.setText(safe(order.getShipperName())); // Gán tên shipper

        // Xử lý và gán điểm rating
        String rating = "0.0";
        if (order.getShipperRating() != null && !order.getShipperRating().isEmpty()) {
            try {
                double ratingValue = Double.parseDouble(order.getShipperRating());
                rating = String.format(Locale.US, "%.1f", ratingValue); // Định dạng thành 1 chữ số thập phân
            } catch (NumberFormatException e) {
                rating = order.getShipperRating(); // Giữ nguyên nếu không phải là số
            }
        }
        tvShipperRating.setText(rating); // Gán điểm rating

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
        tvShippingFee.setText(formatCurrency(shippingFee));
        tvCodAmount.setText(formatCurrency(cod));
        tvTotalAmount.setText(formatCurrency(shippingFee + cod));

        // --- Lịch sử Tracking ---
        if (order.getTrackingHistory() != null) {
            trackingAdapter.submitList(order.getTrackingHistory());
        }

        // --- Logic Bản đồ ---
        drawMap(order);
        updateRouteBasedOnStatus(order, null);
//        updateRouteBasedOnStatus(order, null); // Vẽ tuyến đường ban đầu (tĩnh)
        // NẾU TRẠNG THÁI KHÔNG PHẢI LÀ "accepted", VẼ TUYẾN ĐƯỜNG TĨNH NGAY LẬP TỨC
        if (!"accepted".equals(safe(order.getStatus()).toLowerCase())) {
            Double pLat = toDouble(order.getPick_up_lat());
            Double pLng = toDouble(order.getPick_up_lng());
            Double dLat = toDouble(order.getDelivery_lat());
            Double dLng = toDouble(order.getDelivery_lng());

            if (pLat != null && pLng != null && dLat != null && dLng != null) {
                GeoPoint origin = new GeoPoint(pLat, pLng);
                GeoPoint dest = new GeoPoint(dLat, dLng);
                fetchAndDrawRoute(origin, dest, isOrderCompleted(order));
            }
        }

        // --- Bắt đầu theo dõi thời gian thực ---
        startRealtimeTracking(order);
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
            case "delivered":
            case "delivery_failed":
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

        // Nếu trạng thái không phải là "accepted", chỉ vẽ 1 lần duy nhất
        if (!"accepted".equals(currentOrder.getStatus().toLowerCase())) {
            return lastRouteDrawTime == 0L;
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
        float[] distance = new float[1];
        android.location.Location.distanceBetween(lastRouteDrawLat, lastRouteDrawLng, newOrigin.getLatitude(), newOrigin.getLongitude(), distance);

        return distance[0] > MIN_ROUTE_DRAW_DISTANCE;
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

                if (!isCompleted && route.legs != null && route.legs.length > 0 && route.legs[0].duration != null) {
                    tvEta.setText("Dự kiến đến sau " + route.legs[0].duration.text);
                } else if (isCompleted) {
                    tvEta.setText("Đơn hàng đã hoàn thành");
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

//                mapView.getOverlays().add(line);
//                mapView.invalidate();
                currentRoutePolyline = line; // LƯU LẠI TUYẾN ĐƯỜNG MỚI
                mapView.getOverlays().add(currentRoutePolyline);
                mapView.invalidate();

                // Tự động zoom bản đồ cho vừa với tuyến đường
                BoundingBox box = BoundingBox.fromGeoPoints(geoPts);
                mapView.zoomToBoundingBox(box, true, 120); // 120 là padding

                // ## BỔ SUNG: CẬP NHẬT CÁC BIẾN GIỚI HẠN ##
                // =======================================================
                // Lưu lại thời điểm và vị trí vừa vẽ thành công để
                // hàm shouldRedrawRoute() có thể so sánh ở lần gọi tiếp theo.
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

    // --- Các hàm tiện ích khác (getStatusText, formatCurrency...) giữ nguyên ---

    // =============================================================
    // ## QUẢN LÝ VÒNG ĐỜI BẢN ĐỒ (RẤT QUAN TRỌNG) ##
    // =============================================================
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
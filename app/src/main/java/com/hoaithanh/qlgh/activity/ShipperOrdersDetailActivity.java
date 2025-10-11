package com.hoaithanh.qlgh.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.VectorDrawable;
import android.net.Uri;
import android.os.*;
import android.text.TextUtils;
import android.view.View;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.gms.location.*;

import com.hoaithanh.qlgh.BuildConfig;
import com.hoaithanh.qlgh.R;
import com.hoaithanh.qlgh.base.BaseActivity;
import com.hoaithanh.qlgh.model.goong.DirectionResponse;
import com.hoaithanh.qlgh.model.goong.LatLng;
import com.hoaithanh.qlgh.repository.GoongRepository;
import com.hoaithanh.qlgh.model.*;
import com.hoaithanh.qlgh.model.goong.PolylineDecoder;
import com.hoaithanh.qlgh.viewmodel.DonDatHangViewModel;

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;

import java.text.NumberFormat;
import java.util.*;
import java.util.Locale;

import retrofit2.*;

public class ShipperOrdersDetailActivity extends BaseActivity {

    private MapView mapView;
    private IMapController mapController;
    private TextView tvStage, tvEta, tvPickupAddr, tvDeliveryAddr, tvRecipient, tvCod, tvShipFee, tvNote, tvSender;
    private MaterialButton btnCall, btnNavigate, btnCopyAddr, btnPrimary, btnFail;
    private com.google.android.material.floatingactionbutton.FloatingActionButton btnMyLocation;

    private DonDatHang order;
    private GoongRepository goongRepo;
    private String goongKey;

    // Location
    private FusedLocationProviderClient fused;
    private LocationCallback locCallback;
    private double curLat = Double.NaN, curLng = Double.NaN;
    private boolean hasFix = false, autoFocus = true;
    private static final int REQ_LOC = 321;

    // Giới hạn gọi Directions
    private static final long MIN_INTERVAL_MS = 15000;
    private static final float MIN_MOVE_METERS = 50f;
    private long lastRouteTime = 0L;
    private double lastRouteLat = Double.NaN, lastRouteLng = Double.NaN;
    private String lastRouteKey = "";

    private DonDatHangViewModel viewModel;  //update trang thai don hang tren db

    @Override
    public void initLayout() {
        setContentView(R.layout.activity_shipper_order_detail);
    }

    @Override
    public void initData() {
        Intent it = getIntent();
        if (it == null || !it.hasExtra("order")) {
            Toast.makeText(this, "Không tìm thấy dữ liệu đơn hàng", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        order = (DonDatHang) it.getSerializableExtra("order");
    }

    @Override
    public void initView() {
        MaterialToolbar tb = findViewById(R.id.toolbar);
        setSupportActionBar(tb);
        tb.setNavigationOnClickListener(v -> onBackPressed());

        mapView = findViewById(R.id.mapView);
        Configuration.getInstance().setUserAgentValue(getPackageName());
        mapView.setMultiTouchControls(true);
        mapView.getZoomController().setVisibility(org.osmdroid.views.CustomZoomButtonsController.Visibility.NEVER);
        mapController = mapView.getController();
        mapController.setZoom(15.0);

        tvStage = findViewById(R.id.tvStage);
        tvEta = findViewById(R.id.tvEta);
        tvPickupAddr = findViewById(R.id.tvPickupAddr);
        tvDeliveryAddr = findViewById(R.id.tvDeliveryAddr);
        tvRecipient = findViewById(R.id.tvRecipient);
        tvSender = findViewById(R.id.tvSender);
        tvCod = findViewById(R.id.tvCod);
        tvShipFee = findViewById(R.id.tvShipFee);
        tvNote = findViewById(R.id.tvNote);
        btnCall = findViewById(R.id.btnCall);
        btnNavigate = findViewById(R.id.btnNavigate);
        btnCopyAddr = findViewById(R.id.btnCopyAddr);
        btnPrimary = findViewById(R.id.btnPrimary);
        btnFail = findViewById(R.id.btnFail);
        btnMyLocation = findViewById(R.id.btnMyLocation);

        viewModel = new ViewModelProvider(this).get(DonDatHangViewModel.class);
        observeViewModel();

        goongRepo = new GoongRepository();
        goongKey = BuildConfig.GOONG_API_KEY;
        fused = LocationServices.getFusedLocationProviderClient(this);

        showOrderInfo();
        ensureLocationPermissionThenStart();

        if (order != null && order.getID() != null) {
            try {
                int orderId = Integer.parseInt(order.getID());
                viewModel.loadOrderDetails(orderId);
            } catch (NumberFormatException e) {
                Toast.makeText(this, "ID đơn hàng không hợp lệ", Toast.LENGTH_SHORT).show();
            }
        }

        // Ngắt auto-focus khi người dùng chạm map
         mapView.setOnTouchListener((v, e) -> {
             autoFocus = false;
             return false;
         });
         // Nút “vị trí của tôi”
         btnMyLocation.setOnClickListener(v -> {
             autoFocus = true;
             if (hasFix) smoothFocusTo(curLat, curLng, 17.0);
             else Toast.makeText(this, "Chưa xác định vị trí hiện tại", Toast.LENGTH_SHORT).show();
         });
    }

    // Hàm phụ để lấy trạng thái tiếp theo, giúp code gọn hơn
    private String getNextStatus(String currentStatus) {
        if (currentStatus == null) return null;
        switch (currentStatus) {
            case "accepted": return "picked_up";
            case "picked_up": return "in_transit";
            case "in_transit": return "delivered";
            default: return null;
        }
    }

    // TÁCH VIỆC LẮNG NGHE RA MỘT HÀM RIÊNG ĐỂ GỌN GÀNG
    private void observeViewModel() {
        // LẮNG NGHE DỮ LIỆU MỚI NHẤT TỪ SERVER
        viewModel.getOrderDetails().observe(this, newOrderDetails -> {
            if (newOrderDetails != null) {
                // 3. CẬP NHẬT LẠI BIẾN "order" BẰNG DỮ LIỆU MỚI
                this.order = newOrderDetails;

                // 4. CẬP NHẬT LẠI GIAO DIỆN VỚI TRẠNG THÁI ĐÚNG
                tvStage.setText(statusToStage(this.order.getStatus()));
                updateButtonsForStatus();
                setResult(RESULT_OK);
            }
        });

        viewModel.getUpdateStatusResult().observe(this, apiResponse -> {
            // hideLoadingDialog();
            if (apiResponse != null && apiResponse.isSuccess()) {
                Toast.makeText(this, "Cập nhật thành công!", Toast.LENGTH_SHORT).show();

                // Tính toán lại trạng thái tiếp theo và cập nhật UI
                String nextStatus = getNextStatus(order.getStatus());
                if (nextStatus != null) {
                    order.setStatus(nextStatus);
                    tvStage.setText(statusToStage(nextStatus));
                    drawRouteForStatus();
                    updateButtonsForStatus();
                }
            } else {
                String message = (apiResponse != null) ? apiResponse.getMessage() : "Có lỗi xảy ra";
                Toast.makeText(this, "Cập nhật thất bại: " + message, Toast.LENGTH_LONG).show();
            }
        });
    }

    /** ============= PERMISSION + LOCATION ============= **/
    private void ensureLocationPermissionThenStart() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQ_LOC);
        } else startLocationUpdates();
    }

    @SuppressLint("MissingPermission")
    private void startLocationUpdates() {
        LocationRequest req = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 15000)
                .setMinUpdateIntervalMillis(8000)
                .build();

        locCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult result) {
                if (result.getLastLocation() == null) return;
                curLat = result.getLastLocation().getLatitude();
                curLng = result.getLastLocation().getLongitude();
                hasFix = true;

                if ("accepted".equals(order.getStatus())) drawRouteForStatus();
//                mapController.animateTo(new GeoPoint(curLat, curLng)); // luôn focus về vị trí shipper
            }
        };
        fused.requestLocationUpdates(req, locCallback, getMainLooper());

        fused.getLastLocation().addOnSuccessListener(loc -> {
            if (loc != null) {
                curLat = loc.getLatitude();
                curLng = loc.getLongitude();
                hasFix = true;
                drawRouteForStatus();
                // Chỉ focus bản đồ lần đầu tiên khi lấy được vị trí
                mapController.animateTo(new GeoPoint(curLat, curLng));
            }
        });
    }

    /** ============= MAP & ROUTE ============= **/
    private boolean shouldRecalculate(String origin, String dest) {
        long now = System.currentTimeMillis();
        if (now - lastRouteTime < MIN_INTERVAL_MS) return false;
        String key = origin + "|" + dest + "|bike";
        if (!key.equals(lastRouteKey)) return true;
        if (Double.isNaN(lastRouteLat) || Double.isNaN(lastRouteLng)) return true;

        float[] d = new float[1];
        android.location.Location.distanceBetween(lastRouteLat, lastRouteLng, curLat, curLng, d);
        return d[0] >= MIN_MOVE_METERS;
    }

    private void drawRouteForStatus() {
        String status = order.getStatus();
        String origin, dest;

        if ("accepted".equals(status)) {
            origin = curLat + "," + curLng;
            dest = order.getPick_up_lat() + "," + order.getPick_up_lng();
        } else {
            origin = order.getPick_up_lat() + "," + order.getPick_up_lng();
            dest = order.getDelivery_lat() + "," + order.getDelivery_lng();
        }

        if (!shouldRecalculate(origin, dest)) return;

        goongRepo.getRoute(origin, dest, "bike", goongKey).enqueue(new Callback<DirectionResponse>() {
            @Override
            public void onResponse(Call<DirectionResponse> call, Response<DirectionResponse> res) {
                if (res.code() == 429) {
                    tvEta.setText("Đang quá tải, thử lại sau...");
                    return;
                }

                if (!res.isSuccessful() || res.body() == null || res.body().routes == null || res.body().routes.isEmpty()) {
                    tvEta.setText("Không tìm thấy tuyến đường");
                    return;
                }

                DirectionResponse.Route route = res.body().routes.get(0);
                List<LatLng> latLngs = PolylineDecoder.decode(route.overview_polyline.points);
                List<GeoPoint> points = new ArrayList<>();
                for (LatLng p : latLngs) points.add(new GeoPoint(p.latitude, p.longitude));

                mapView.getOverlays().clear();
                Polyline line = new Polyline();
                line.setPoints(points);
                line.setWidth(6f);
                mapView.getOverlays().add(line);

                addMarker(points.get(0), R.drawable.ic_sender, 24);
                addMarker(points.get(points.size() - 1), R.drawable.ic_receiver, 24);
                mapController.setCenter(points.get(0));

                if (route.legs != null && route.legs.length > 0 && route.legs[0].duration != null)
                    tvEta.setText("ETA " + route.legs[0].duration.text);
                else
                    tvEta.setText("ETA --:--");

                lastRouteTime = System.currentTimeMillis();
                lastRouteLat = curLat;
                lastRouteLng = curLng;
                lastRouteKey = origin + "|" + dest + "|bike";

                mapView.invalidate();
            }

            @Override
            public void onFailure(Call<DirectionResponse> call, Throwable t) {
                tvEta.setText("Không thể tải tuyến đường");
            }
        });
    }

    /** Pan & zoom mượt */
    private void smoothFocusTo(double lat, double lng, double zoom) {
        GeoPoint p = new GeoPoint(lat, lng);
        mapController.animateTo(p);
        mapView.postDelayed(() -> mapController.setZoom(zoom), 300);
    }

    private Drawable resizeDrawable(int drawableResId, int sizeDp) {
        Drawable drawable = ContextCompat.getDrawable(this, drawableResId);
        if (drawable == null) return null;

        // 1. Chuyển đổi Drawable thành Bitmap (Hỗ trợ Vector Drawable)
        Bitmap bmp = null;
        if (drawable instanceof BitmapDrawable) {
            // Trường hợp Drawable là BitmapDrawable (tức là file .png, .jpg)
            bmp = ((BitmapDrawable) drawable).getBitmap();
        } else if (drawable instanceof VectorDrawable || drawable instanceof AnimatedVectorDrawable) {
            // Trường hợp Drawable là Vector/AnimatedVector Drawable (.xml)
            bmp = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bmp);
            drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
            drawable.draw(canvas);
        } else {
            // Xử lý các loại Drawable khác (nếu cần)
            return drawable;
        }

        // Kiểm tra Bitmap sau khi chuyển đổi
        if (bmp == null) return null;

        // 2. Tính toán kích thước pixel
        int sizePx = (int) (sizeDp * getResources().getDisplayMetrics().density);

        // 3. Thay đổi kích thước và trả về BitmapDrawable mới
        Bitmap scaledBmp = Bitmap.createScaledBitmap(bmp, sizePx, sizePx, true);
        return new BitmapDrawable(getResources(), scaledBmp);
    }

    private void addMarker(GeoPoint point, int iconRes, int sizeDp) {
        Drawable icon = resizeDrawable(iconRes, sizeDp);
        if (icon == null) return;
        Marker marker = new Marker(mapView);
        marker.setPosition(point);
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        marker.setIcon(icon);
        mapView.getOverlays().add(marker);
    }

    /** ============= ORDER INFO ============= **/
    private void showOrderInfo() {
        tvPickupAddr.setText(order.getPick_up_address());
        tvDeliveryAddr.setText(order.getDelivery_address());
        tvRecipient.setText("Người nhận: " + order.getRecipient() + " (" + order.getRecipientPhone() + ")");
        tvSender.setText("Người gửi: " + order.getUserName());
        tvCod.setText("COD: " + formatCurrency(order.getCOD_amount()));
        tvShipFee.setText("Phí VC: " + formatCurrency(order.getShippingfee()));
        tvNote.setText("Ghi chú: " + (order.getNote() == null ? "--" : order.getNote()));
        tvStage.setText(statusToStage(order.getStatus()));

//        btnCall.setOnClickListener(v -> callRecipient());
        btnCall.setOnClickListener(v -> showCallDialog());
        btnCopyAddr.setOnClickListener(v -> copyToClipboard(order.getDelivery_address()));
        btnNavigate.setOnClickListener(v -> openGoogleMaps(order.getDelivery_lat(), order.getDelivery_lng()));
        btnPrimary.setOnClickListener(v -> updateStatusNextStage());
    }

    private void showCallDialog() {
        // Gom tất cả số điện thoại hợp lệ (người nhận + khách hàng)
        List<String> phones = new ArrayList<>();
        Map<String, String> map = new LinkedHashMap<>(); // hiển thị tên rõ ràng

        if (!TextUtils.isEmpty(order.getRecipientPhone())) {
            phones.add(order.getRecipientPhone());
            map.put(order.getRecipientPhone(), "Người nhận (" + order.getRecipient() + ")");
        }

        if (!TextUtils.isEmpty(order.getPhoneNumberCus())) {
            phones.add(order.getPhoneNumberCus());
            map.put(order.getPhoneNumberCus(), "Khách hàng (" + order.getUserName() + ")");
        }

        if (phones.isEmpty()) {
            Toast.makeText(this, "Không có số điện thoại khả dụng", Toast.LENGTH_SHORT).show();
            return;
        }

        // Hiển thị dialog chọn người để gọi
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle("Chọn người để gọi")
                .setItems(map.values().toArray(new String[0]), (dialog, which) -> {
                    String phone = phones.get(which);
//                    Intent intent = new Intent(Intent.ACTION_DIAL);
//                    intent.setData(Uri.parse("tel:" + phone));
//                    startActivity(intent);
                    // 1. Làm sạch số (quan trọng, tránh lỗi URI)
                    String cleanPhone = phone.replaceAll("[^0-9+*#]", "");

                    if (cleanPhone.isEmpty()) {
                        Toast.makeText(this, "Số điện thoại không hợp lệ", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    Intent intent = new Intent(Intent.ACTION_DIAL);
                    intent.setData(Uri.parse("tel:" + cleanPhone));

                    // 2. BẢO VỆ LỆNH GỌI BẰNG TRY-CATCH
                    try {
                        startActivity(intent);
                    } catch (android.content.ActivityNotFoundException e) {
                        // Thông báo cho người dùng biết lỗi xảy ra do môi trường
                        Toast.makeText(this, "Không thể mở trình gọi điện. Vui lòng kiểm tra cài đặt LDPlayer.", Toast.LENGTH_LONG).show();
                        e.printStackTrace();
                    }
                })
                .show();
    }

    private void copyToClipboard(String text) {
        android.content.ClipboardManager cb = (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        cb.setPrimaryClip(android.content.ClipData.newPlainText("addr", text));
        Toast.makeText(this, "Đã sao chép địa chỉ", Toast.LENGTH_SHORT).show();
    }

    private void openGoogleMaps(String lat, String lng) {
        Uri uri = Uri.parse("google.navigation:q=" + lat + "," + lng);
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, uri);
        mapIntent.setPackage("com.google.android.apps.maps");
        startActivity(mapIntent);
    }

    private String formatCurrency(String amount) {
        try {
            double v = Double.parseDouble(amount);
            return NumberFormat.getInstance(new Locale("vi", "VN")).format(v) + " ₫";
        } catch (Exception e) {
            return amount + " ₫";
        }
    }

    private String statusToStage(String s) {
        if (s == null) return "Không xác định";
        switch (s) {
            case "pending": return "Chờ xác nhận";
            case "accepted": return "Chuẩn bị lấy hàng";
            case "picked_up": return "Đang giao hàng";
            case "in_transit": return "Trên đường giao";
            case "delivered": return "Đã giao thành công";
            default: return "Không xác định";
        }
    }

    private void updateStatusNextStage() {
//        String st = order.getStatus();
//        String next;
//        switch (st) {
//            case "accepted": next = "picked_up"; break;
//            case "picked_up": next = "in_transit"; break;
//            case "in_transit": next = "delivered"; break;
//            default:
//                Toast.makeText(this, "Đơn đã hoàn thành", Toast.LENGTH_SHORT).show();
//                return;
//        }
////        order.setStatus(next);
////        tvStage.setText(statusToStage(next));
////        drawRouteForStatus();
////        updateButtonsForStatus();
//
//        // Lấy ID đơn hàng (cần đảm bảo ID là kiểu int)
//        int orderId = Integer.parseInt(order.getID()); // Giả sử getID() trả về String
//
//        // Hiển thị loading...
//        // showLoadingDialog();
//
//        // Gọi ViewModel để thực hiện cập nhật
//        viewModel.updateOrderStatus(orderId, next);
//
//        // Lắng nghe kết quả trả về từ ViewModel
//        viewModel.getUpdateStatusResult().observe(this, apiResponse -> {
//            // hideLoadingDialog();
//            if (apiResponse != null && apiResponse.isSuccess()) { // Giả sử có hàm isSuccess()
//                // KHI VÀ CHỈ KHI API THÀNH CÔNG, MỚI CẬP NHẬT UI
//                Toast.makeText(this, "Cập nhật thành công!", Toast.LENGTH_SHORT).show();
//                order.setStatus(next);
//                tvStage.setText(statusToStage(next));
//                drawRouteForStatus();
//                updateButtonsForStatus();
//            } else {
//                // Thông báo lỗi nếu API thất bại
//                Toast.makeText(this, "Cập nhật thất bại, vui lòng thử lại.", Toast.LENGTH_LONG).show();
//            }
//        });
        String nextStatus = getNextStatus(order.getStatus());
        if (nextStatus == null) {
            Toast.makeText(this, "Đơn đã hoàn thành", Toast.LENGTH_SHORT).show();
            return;
        }

        // showLoadingDialog();

        // Chỉ cần gọi ViewModel, không cần observe ở đây nữa
        int orderId = Integer.parseInt(order.getID());
        viewModel.updateOrderStatus(orderId, nextStatus);

    }
    // Thêm hàm này vào Activity của bạn
    private void updateButtonsForStatus() {
        String status = order.getStatus();

        // 1. Cập nhật nút Chính (btnPrimary)
        switch (status) {
            case "accepted":
                btnPrimary.setText("Đã lấy hàng");
                break;
            case "picked_up":
                btnPrimary.setText("Tiếp tục giao hàng");
                break;
            case "in_transit":
                btnPrimary.setText("Xác nhận đã giao");
                break;
            case "delivered":
                btnPrimary.setText("Đã hoàn thành");
                btnPrimary.setEnabled(false); // Vô hiệu hóa nút
                break;
            default:
                btnPrimary.setText("Không rõ");
                btnPrimary.setEnabled(false);
        }

        // 2. Cập nhật nút Phụ (btnFail)
        if ("delivered".equals(status)) {
            btnFail.setVisibility(View.GONE); // Ẩn nút báo thất bại nếu đã giao
        } else {
            btnFail.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (fused != null && locCallback != null)
            fused.removeLocationUpdates(locCallback);
    }
}

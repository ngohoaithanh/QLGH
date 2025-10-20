package com.hoaithanh.qlgh.fragment;

import static java.lang.Double.parseDouble;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.hoaithanh.qlgh.R;
import com.hoaithanh.qlgh.activity.ShipperEarningsActivity;
import com.hoaithanh.qlgh.api.ApiService;
import com.hoaithanh.qlgh.api.RetrofitClient;
import com.hoaithanh.qlgh.model.ApiResult;
import com.hoaithanh.qlgh.model.ApiResultNearbyOrders;
import com.hoaithanh.qlgh.model.DonDatHang;
import com.hoaithanh.qlgh.session.SessionManager;

import org.osmdroid.config.Configuration;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import java.text.DateFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ShipperListFragment extends Fragment {

    // UI
    private MapView mapView;
    private LinearLayout btnToggleOnline, actEarning;
    private TextView tvToggleText, tvStatus, tvLastUpdate;
    private View dotStatus;
    private RecyclerView rvNearby;
    private NearbyOrderAdapter adapter;

    // State
    private boolean isOnline = false;
    private SessionManager session;

    // Location
    private FusedLocationProviderClient fused;
    private LocationCallback locationCallback;
    private LocationRequest locationRequest;

    // Toạ độ hiện tại (được cập nhật liên tục). Gán mặc định HCM cho lần load đầu.
    private double curLat = 10.7769, curLng = 106.7009;

    // Đã có GPS fix thật chưa?
    private boolean hasFix = false;

    // Scheduler
    private final Handler handler = new Handler(Looper.getMainLooper());
    private static final int PUSH_INTERVAL_MS = 15_000;  // gửi vị trí lên server
    private static final int POLL_INTERVAL_MS = 10_000;  // tải đơn gần
    private static final int REQ_LOCATION_PERM = 1234;

    // Marker của shipper & danh sách marker đơn hàng
    private Marker selfMarker;
    private final List<Marker> orderMarkers = new ArrayList<>();

    // Chống double-tap nhận đơn
    private boolean isAccepting = false;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_shipper_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, Bundle savedInstanceState) {
        session = new SessionManager(requireContext());

        // Bind UI
        mapView = v.findViewById(R.id.mapView);
        btnToggleOnline = v.findViewById(R.id.btnToggleOnline);
        tvToggleText = v.findViewById(R.id.tvToggleText);
        tvStatus = v.findViewById(R.id.tvStatus);
        tvLastUpdate = v.findViewById(R.id.tvLastUpdate);
        dotStatus = v.findViewById(R.id.dotStatus);
        actEarning = v.findViewById(R.id.actEarning);

        rvNearby = v.findViewById(R.id.rvNearbyOrders);
        rvNearby.setLayoutManager(new LinearLayoutManager(getContext()));
        // CHANGED: truyền callback nhận đơn
        adapter = new NearbyOrderAdapter(this::acceptOrder);
        rvNearby.setAdapter(adapter);

        v.findViewById(R.id.btnRefreshNearby).setOnClickListener(view -> loadNearbyOrders());

        // OSMDroid init
        Configuration.getInstance().setUserAgentValue(requireContext().getPackageName());
        mapView.setMultiTouchControls(true);
        mapView.getController().setZoom(15.0);
        mapView.getController().setCenter(new GeoPoint(curLat, curLng));

        // FusedLocation init
        fused = LocationServices.getFusedLocationProviderClient(requireContext());
        locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10_000)
                .setMinUpdateIntervalMillis(5_000)
                .setMinUpdateDistanceMeters(5f)
                .build();

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult result) {
                if (result.getLastLocation() == null) return;
                curLat = result.getLastLocation().getLatitude();
                curLng = result.getLastLocation().getLongitude();

                // vẽ/cập nhật marker của shipper
                renderSelfMarker(curLat, curLng);

                // Sau lần fix đầu tiên, bắt đầu load & poll
                if (!hasFix) {
                    hasFix = true;
                    updateOnlineUI();
                    loadNearbyOrders();   // gọi 1 lần ngay
                    startPollOrders();    // từ giờ mới poll định kỳ
                }
            }
        };

        // Toggle online/offline
        btnToggleOnline.setOnClickListener(view -> {
            isOnline = !isOnline;
            updateOnlineUI();
            if (isOnline) {
                startLocationUpdates();
                startPushLocation();
                // đợi có fix thật mới poll
            } else {
                pushOfflineStatusImmediately();
                stopLocationUpdates();
                stopPushLocation();
                stopPollOrders();
                hasFix = false; // reset khi offline
                // clear UI khi offline
                adapter.submit(new ArrayList<>());
                clearOrderMarkers();
            }
        });

        actEarning.setOnClickListener(view -> openShipperEarningActivity());

        updateOnlineUI(); // set text & dot lần đầu
    }

    private void openShipperEarningActivity() {
        Context context = requireContext();

        // 2. Tạo Intent để mở ShipperEarningsActivity
        Intent intent = new Intent(context, ShipperEarningsActivity.class);

        // 3. (Tùy chọn) Truyền thêm dữ liệu nếu cần, ví dụ: ID của shipper
        // intent.putExtra("shipper_id", session.getUserId());

        // 4. Mở Activity mới
        startActivity(intent);
    }

    private void pushOfflineStatusImmediately() {
        ApiService api = RetrofitClient.getApi();
        // Đảm bảo gửi status='offline' và lat/lng hiện tại lần cuối cùng
        api.updateShipperLocation(session.getUserId(), curLat, curLng, "offline")
                .enqueue(new Callback<ApiResult>() {
                    @Override public void onResponse(Call<ApiResult> call, Response<ApiResult> resp) {
                        // Cập nhật thành công, có thể hiện Toast nếu cần
                    }
                    @Override public void onFailure(Call<ApiResult> call, Throwable t) {
                        // Báo lỗi nếu gửi offline thất bại
                        Toast.makeText(requireContext(), "Lỗi cập nhật Offline: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // ====== UI helpers ======
    private void updateOnlineUI() {
        if (!isOnline) {
            // Trạng thái 1: OFFLINE
            tvToggleText.setText("Bật kết nối");
            tvStatus.setText("Bạn đang offline.");
            dotStatus.setBackgroundResource(R.drawable.bg_dot_red);
            tvLastUpdate.setText(""); // Xóa thông báo cập nhật
            tvStatus.setTextColor(Color.GRAY);
        } else {
            // Trạng thái ONLINE (Kiểm tra fix)
            if (hasFix) {
                // Trạng thái 2: ONLINE VÀ ĐÃ FIX GPS
                tvToggleText.setText("Đang kết nối");
                tvStatus.setTextColor(Color.GREEN);
                // Để tvStatus được cập nhật bởi loadNearbyOrders (đã tối ưu trước đó)
                // Nếu không có API info mới:
                if (TextUtils.isEmpty(tvStatus.getText())) {
                    tvStatus.setText("Bạn đang online.");
                }
                dotStatus.setBackgroundResource(R.drawable.bg_dot_green);
            } else {
                // Trạng thái 3: ONLINE NHƯNG CHƯA FIX GPS
                tvToggleText.setText("Đang kết nối..."); // Thêm dấu ...
                tvStatus.setText("Vị trí đang được định vị.");
                tvStatus.setTextColor(Color.parseColor("#FFA500"));
                dotStatus.setBackgroundResource(R.drawable.bg_dot_yellow); // Dùng màu vàng/cam
            }
            // Thêm một đoạn nhỏ để reset màu cho tvStatus nếu trạng thái được fix lại
            if (hasFix && dotStatus.getBackground().getConstantState().equals(ContextCompat.getDrawable(requireContext(), R.drawable.bg_dot_green).getConstantState())) {
                tvStatus.setTextColor(Color.GREEN); // Đảm bảo status text có màu xanh khi mọi thứ ổn
            }
        }
    }

    // ====== Location ======
    private boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestLocationPermission() {
        requestPermissions(new String[] {
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
        }, REQ_LOCATION_PERM);
    }

    @SuppressLint("MissingPermission")
    private void startLocationUpdates() {
        if (!hasLocationPermission()) {
            requestLocationPermission();
            return;
        }
        fused.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());

        // Lấy last known cho nhanh
        fused.getLastLocation().addOnSuccessListener(loc -> {
            if (loc != null) {
                curLat = loc.getLatitude();
                curLng = loc.getLongitude();
                mapView.getController().setCenter(new GeoPoint(curLat, curLng));
                renderSelfMarker(curLat, curLng);

                // nếu last-known đã có, coi như có fix luôn để UI mượt hơn
                if (!hasFix) {
                    hasFix = true;
                    updateOnlineUI();
                    loadNearbyOrders();
                    startPollOrders();
                }
            }
        });
    }

    private void stopLocationUpdates() {
        if (fused != null && locationCallback != null) {
            fused.removeLocationUpdates(locationCallback);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] perms, @NonNull int[] results) {
        super.onRequestPermissionsResult(requestCode, perms, results);
        if (requestCode == REQ_LOCATION_PERM) {
            boolean granted = results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED;
            if (granted) startLocationUpdates();
            else Toast.makeText(requireContext(), "Cần quyền vị trí để hoạt động", Toast.LENGTH_SHORT).show();
        }
    }

    // ====== Push vị trí định kỳ ======
    private final Runnable pushTask = new Runnable() {
        @Override public void run() {
            if (!isAdded() || !isOnline) return;

            // Vẽ/cập nhật marker vị trí mình
            renderSelfMarker(curLat, curLng);

            // Gửi lên server
            ApiService api = RetrofitClient.getApi();
            api.updateShipperLocation(session.getUserId(), curLat, curLng, "online")
                    .enqueue(new Callback<ApiResult>() {
                        @Override public void onResponse(Call<ApiResult> call, Response<ApiResult> resp) {
                            tvLastUpdate.setText(
                                    "Cập nhật: " + java.text.DateFormat.getTimeInstance(java.text.DateFormat.SHORT).format(new java.util.Date())
                            );
                        }
                        @Override public void onFailure(Call<ApiResult> call, Throwable t) { /* ignore */ }
                    });

            handler.postDelayed(this, PUSH_INTERVAL_MS);
        }
    };
    private void startPushLocation(){ handler.post(pushTask); }
    private void stopPushLocation(){ handler.removeCallbacks(pushTask); }

    private void renderSelfMarker(double lat, double lng){
        if (selfMarker == null) {
            selfMarker = new Marker(mapView);
            selfMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            selfMarker.setTitle("Vị trí của tôi");
            // nếu có icon riêng: selfMarker.setIcon(ContextCompat.getDrawable(requireContext(), R.drawable.ic_shipper_me));
            mapView.getOverlays().add(selfMarker);
        }
        selfMarker.setPosition(new GeoPoint(lat, lng));
        mapView.invalidate();
    }

    // ====== Poll đơn gần ======
    private final Runnable pollTask = new Runnable() {
        @Override public void run() {
            if (!isAdded() || !isOnline || !hasFix) return; // chỉ poll khi có fix
            loadNearbyOrders();
            handler.postDelayed(this, POLL_INTERVAL_MS);
        }
    };
    private void startPollOrders(){ handler.post(pollTask); }
    private void stopPollOrders(){ handler.removeCallbacks(pollTask); }

private void loadNearbyOrders() {
    if (!hasFix) return;
    final int shipperId = session.getUserId();

    RetrofitClient.getApi()
            .getNearbyOrders(shipperId, curLat, curLng, 5000, 10)
            .enqueue(new Callback<ApiResultNearbyOrders>() {
                @Override public void onResponse(Call<ApiResultNearbyOrders> call, Response<ApiResultNearbyOrders> res) {
                    if (!isAdded()) return;
                    if (!res.isSuccessful() || res.body() == null) return;

                    ApiResultNearbyOrders body = res.body();

                    // 1) Hiển thị trạng thái tổng (Giữ nguyên logic hiển thị tvStatus)
                    String info = body.info;
                    if (!TextUtils.isEmpty(info)) {
                        if ("offline_or_stale".equals(info)) {
                            tvStatus.setText("Bạn đang offline hoặc vị trí chưa cập nhật gần đây.");
                            tvStatus.setTextColor(Color.GRAY); // Thêm màu cho rõ ràng
                        } else if ("low_rating".equals(info)) {
                            tvStatus.setText("Rating hiện chưa đủ điều kiện.");
                            tvStatus.setTextColor(Color.RED);
                        } else if ("max_active_reached".equals(info)) {
                            tvStatus.setText("Bạn đang đủ số đơn hoạt động.");
                            tvStatus.setTextColor(Color.BLUE);
                        } else if (info.startsWith("cooldown_")) {
                            tvStatus.setText("Chờ " + info.replace("cooldown_", "") + "s trước khi nhận đơn tiếp.");
                            tvStatus.setTextColor(Color.parseColor("#FF5722")); // Màu cam
                        } else {
                            tvStatus.setText(info);
                            tvStatus.setTextColor(Color.BLACK);
                        }
                    } else {
                        tvStatus.setText("Bạn đang online.");
                        tvStatus.setTextColor(Color.GREEN); // Màu xanh
                    }

                    // 2) Luôn render danh sách + marker
                    List<DonDatHang> list = (body.orders != null) ? body.orders : new ArrayList<>();

                    // BẮT BUỘC: Truyền info vào adapter để kích hoạt logic vô hiệu hóa nút
                    adapter.setGlobalInfo(info);

                    adapter.submit(list);
                    renderOrderMarkers(list);

                }
                @Override public void onFailure(Call<ApiResultNearbyOrders> call, Throwable t) {
                    // tuỳ chọn: hiện lỗi nhẹ nhàng
                }
            });
}

    private void clearOrderMarkers() {
        for (Marker m : orderMarkers) mapView.getOverlays().remove(m);
        orderMarkers.clear();
        mapView.invalidate();
    }

    /** Parse double an toàn từ String (model bạn dùng String lat/lng). */
    private static Double parseD(String s){
        if (s == null || s.trim().isEmpty()) return null;
        try { return Double.valueOf(s); } catch (Exception e) { return null; }
    }

    /** Vẽ/cập nhật các marker đơn hàng. */
    private void renderOrderMarkers(List<DonDatHang> list){
        clearOrderMarkers();

        for (DonDatHang d : list) {
            // CHANGED: parse an toàn
            Double lat = parseD(d.getDelivery_lat());
            Double lng = parseD(d.getDelivery_lng());
            if (lat == null || lng == null) continue;

            Marker mk = new Marker(mapView);
            mk.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            mk.setPosition(new GeoPoint(lat, lng));
            mk.setTitle("Đơn #" + d.getID());

            String pick = d.getPick_up_address();
            String del  = d.getDelivery_address();
            String dist = (d.distance >= 0)
                    ? ((d.distance < 1000) ? String.format("~ %.0f m", d.distance)
                    : String.format("~ %.1f km", d.distance/1000.0))
                    : "--";
            mk.setSnippet("Lấy: " + (TextUtils.isEmpty(pick) ? "—" : pick)
                    + "\nGiao: " + (TextUtils.isEmpty(del) ? "—" : del)
                    + "\nKhoảng cách: " + dist);

            mk.setOnMarkerClickListener((marker, map) -> {
                map.getController().animateTo(marker.getPosition());
                marker.showInfoWindow();
                return true;
            });

            mapView.getOverlays().add(mk);
            orderMarkers.add(mk);
        }
        mapView.invalidate();
    }

    /** (tuỳ chọn) Fit map để thấy cả mình + các đơn hàng. */
    private void fitMapToMeAndOrders(List<DonDatHang> list){
        final List<GeoPoint> pts = new ArrayList<>();
        pts.add(new GeoPoint(curLat, curLng));
        for (DonDatHang d : list) {
            Double lat = parseD(d.getDelivery_lat());
            Double lng = parseD(d.getDelivery_lng());
            if (lat != null && lng != null) pts.add(new GeoPoint(lat, lng));
        }
        if (pts.size() < 2) return;

        try {
            BoundingBox bb = BoundingBox.fromGeoPointsSafe(pts);
            mapView.zoomToBoundingBox(bb, true, 80);
        } catch (Exception ignore) { }
    }

    // ====== Nhận đơn (race-safe) + chống double-tap ======
    private void acceptOrder(int orderId){
        if (isAccepting) return;           // chặn double-tap
        isAccepting = true;

        RetrofitClient.getApi().acceptOrder(orderId, session.getUserId())
                .enqueue(new Callback<ApiResult>() {
                    @Override public void onResponse(Call<ApiResult> call, Response<ApiResult> res) {
                        isAccepting = false;
                        if (res.isSuccessful() && res.body()!=null && res.body().success) {
                            Toast.makeText(requireContext(), "Đã nhận đơn #" + orderId, Toast.LENGTH_SHORT).show();
                            loadNearbyOrders(); // làm mới để bỏ đơn vừa nhận
                        } else {
                            Toast.makeText(requireContext(), "Nhận đơn thất bại (có thể đã bị nhận trước)", Toast.LENGTH_SHORT).show();
                            loadNearbyOrders(); // đồng bộ lại danh sách
                        }
                    }
                    @Override public void onFailure(Call<ApiResult> call, Throwable t) {
                        isAccepting = false;
                        Toast.makeText(requireContext(), "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // ====== Adapter danh sách đơn gần ======
    private static class NearbyOrderAdapter extends RecyclerView.Adapter<NearbyOrderAdapter.VH> {
        interface OnAccept { void onAccept(int orderId); }
        private final OnAccept cb;                         // CHANGED: callback
        private final List<DonDatHang> data = new ArrayList<>();
        private String globalInfo = null;  // NEW
        NearbyOrderAdapter(OnAccept cb){ this.cb = cb; }   // CHANGED: inject callback

        void submit(List<DonDatHang> d){ data.clear(); if (d!=null) data.addAll(d); notifyDataSetChanged(); }

        void setGlobalInfo(String info) {   // NEW
            this.globalInfo = info;
            notifyDataSetChanged();
        }

        @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_nearby_order, parent, false);
            return new VH(v);
        }

        @Override public void onBindViewHolder(@NonNull VH h, int i) {
            DonDatHang o = data.get(i);
            h.tvOrderId.setText("Mã đơn: " + safe(o.getID()));
            h.tvShippingFee.setText("Phí ship: " + formatCurrencyVN(o.getShippingfee()));
            double codValue = parseDouble(o.getCOD_amount());
            if (codValue > 0) {
                h.tvCodAmount.setText("Ứng COD: " + formatCurrencyVN(o.getCOD_amount()));
                h.tvCodAmount.setVisibility(View.VISIBLE);
            } else {
                h.tvCodAmount.setVisibility(View.GONE);
            }
            String pick = o.getPick_up_address() != null ? o.getPick_up_address() : "";
            String del  = o.getDelivery_address() != null ? o.getDelivery_address() : "";
            h.tvPickup.setText("Lấy: " + pick);
            h.tvDelivery.setText("Giao: " + del);


            String dist = "--";
            try {
                Double m = o.distance;
                if (m != null) dist = (m < 1000) ? String.format("~ %.0f m", m) : String.format("~ %.1f km", m/1000.0);
            } catch (Exception ignore) { /* Bỏ qua */ }

            // --- KHỞI TẠO TRẠNG THÁI MẶC ĐỊNH ---
            h.tvDistance.setText(dist); // Đặt lại text mặc định
            h.tvDistance.setTextColor(Color.BLACK); // Reset màu chữ về mặc định
            h.btnAccept.setAlpha(1f);
            h.btnAccept.setEnabled(true);
            h.btnAccept.setOnClickListener(null); // Xóa listener cũ


            boolean isDisabledByGlobal = false;
            String globalWarningText = null;

            if (globalInfo != null) {
                if ("max_active_reached".equals(globalInfo)) {
                    isDisabledByGlobal = true;
                    globalWarningText = "Đủ đơn hoạt động";
                } else if (globalInfo.startsWith("cooldown_")) {
                    isDisabledByGlobal = true;
                    globalWarningText = "Cooldown " + globalInfo.replace("cooldown_", "") + "s";
                }
            }

            if (isDisabledByGlobal) {
                h.btnAccept.setEnabled(false);
                h.btnAccept.setAlpha(0.5f);
                // Hiển thị cảnh báo global (Màu Đỏ)
                if (globalWarningText != null) {
                    h.tvDistance.append(" • " + globalWarningText);
                    h.tvDistance.setTextColor(Color.RED);
                }
                return; // Dừng xử lý các bước sau, nút đã bị vô hiệu hóa
            }

            Boolean feasible = o.hint_feasible;
            String  reason   = o.hint_reason;

            if (feasible != null && !feasible) {
                // Vẫn cho phép nhận, nhưng cảnh báo nhẹ
                h.btnAccept.setAlpha(0.8f);

                String warn;
                if ("xa_pickup".equals(reason))        warn = "⚠ Lấy hàng xa tuyến";
                else if ("nguoc_huong".equals(reason)) warn = "⚠ Lệch hướng tuyến";
                else if ("detour_lon".equals(reason))  warn = "⚠ Tuyến vòng lớn";
                else                                   warn = "⚠ Không khuyến nghị";

                h.tvDistance.setTextColor(Color.parseColor("#FFA500"));
                h.tvDistance.append(" • " + warn);
            }

            h.btnAccept.setOnClickListener(v -> cb.onAccept(parseId(o.getID())));
        }

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

        private double parseDouble(String s) {
            if (s == null || s.trim().isEmpty()) {
                return 0.0;
            }
            try {
                return Double.parseDouble(s.trim());
            } catch (NumberFormatException e) {
                return 0.0;
            }
        }

        @Override public int getItemCount() { return data.size(); }

        static class VH extends RecyclerView.ViewHolder {
            TextView tvOrderId, tvPickup, tvDelivery, tvDistance, tvShippingFee, tvCodAmount;
            Button btnAccept;
            VH(@NonNull View v){
                super(v);
                tvOrderId = v.findViewById(R.id.tvOrderId);
                tvPickup = v.findViewById(R.id.tvPickup);
                tvDelivery = v.findViewById(R.id.tvDelivery);
                tvDistance = v.findViewById(R.id.tvDistance);
                btnAccept = v.findViewById(R.id.btnAccept);
                tvShippingFee = v.findViewById(R.id.tvShippingFee);
                tvCodAmount = v.findViewById(R.id.tvCodAmount);
            }
        }

        private static int parseId(String s){
            try { return Integer.parseInt(s); } catch (Exception e) { return 0; }
        }
        private static String safe(Object o){ return o==null ? "" : String.valueOf(o); }
    }

    // ====== Lifecycle cleanup ======
    @Override
    public void onDestroyView() {
        stopLocationUpdates();
        stopPushLocation();
        stopPollOrders();
        super.onDestroyView();
    }
}

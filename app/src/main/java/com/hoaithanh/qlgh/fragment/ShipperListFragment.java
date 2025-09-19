package com.hoaithanh.qlgh.fragment;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ShipperListFragment extends Fragment {

    // UI
    private MapView mapView;
    private LinearLayout btnToggleOnline;
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

    // NEW: đã có GPS fix thật chưa?
    private boolean hasFix = false;

    // Scheduler
    private final Handler handler = new Handler(Looper.getMainLooper());
    private static final int PUSH_INTERVAL_MS = 15_000;  // gửi vị trí lên server
    private static final int POLL_INTERVAL_MS = 10_000;  // tải đơn gần
    private static final int REQ_LOCATION_PERM = 1234;

    // Marker của shipper & danh sách marker đơn hàng
    private Marker selfMarker;
    private final List<Marker> orderMarkers = new ArrayList<>();

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

        rvNearby = v.findViewById(R.id.rvNearbyOrders);
        rvNearby.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new NearbyOrderAdapter();
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

                // NEW: sau lần fix đầu tiên, bắt đầu load & poll
                if (!hasFix) {
                    hasFix = true;
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
                // ⚠️ KHÔNG startPollOrders ở đây nữa — đợi tới khi có fix thật
            } else {
                stopLocationUpdates();
                stopPushLocation();
                stopPollOrders();
                hasFix = false; // reset khi offline
            }
        });

        updateOnlineUI(); // set text & dot lần đầu
    }

    // ====== UI helpers ======
    private void updateOnlineUI() {
        tvToggleText.setText(isOnline ? "Đang kết nối" : "Bật kết nối");
        tvStatus.setText(isOnline ? "Bạn đang online." : "Bạn đang offline.");
        dotStatus.setBackgroundResource(isOnline ? R.drawable.bg_dot_green : R.drawable.bg_dot_red);
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

                // NEW: nếu last-known đã có, coi như có fix luôn để UI mượt hơn
                if (!hasFix) {
                    hasFix = true;
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
            if (!isAdded() || !isOnline || !hasFix) return; // NEW: chỉ poll khi có fix
            loadNearbyOrders();
            handler.postDelayed(this, POLL_INTERVAL_MS);
        }
    };
    private void startPollOrders(){ handler.post(pollTask); }
    private void stopPollOrders(){ handler.removeCallbacks(pollTask); }

    private void loadNearbyOrders() {
        if (!hasFix) return; // NEW: chưa có fix thì không gọi
        RetrofitClient.getApi().getNearbyOrders(curLat, curLng, 5000, 10)
                .enqueue(new Callback<ApiResultNearbyOrders>() {
                    @Override public void onResponse(Call<ApiResultNearbyOrders> call, Response<ApiResultNearbyOrders> res) {
                        if (!isAdded()) return;
                        if (res.isSuccessful() && res.body()!=null && res.body().success) {
                            List<DonDatHang> list = res.body().orders;
                            if (list != null && !list.isEmpty()) {
                                adapter.submit(list);
                                renderOrderMarkers(list);      // vẽ pin đơn hàng
//                                fitMapToMeAndOrders(list);     // (tuỳ chọn) fit khung nhìn
                            } else {
                                adapter.submit(new ArrayList<>());
                                // cũng có thể xoá marker đơn hàng cũ
                                clearOrderMarkers();
                            }
                        }
                    }
                    @Override public void onFailure(Call<ApiResultNearbyOrders> call, Throwable t) { /* ignore */ }
                });
    }

    private void clearOrderMarkers() {
        for (Marker m : orderMarkers) mapView.getOverlays().remove(m);
        orderMarkers.clear();
        mapView.invalidate();
    }

    /** Vẽ/cập nhật các marker đơn hàng. */
    private void renderOrderMarkers(List<DonDatHang> list){
        // Xoá marker đơn hàng cũ (giữ nguyên selfMarker)
        clearOrderMarkers();

        for (DonDatHang d : list) {
            Double lat = Double.valueOf(d.getDelivery_lat());
            Double lng = Double.valueOf(d.getDelivery_lng());
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

            // nếu có icon riêng: mk.setIcon(ContextCompat.getDrawable(requireContext(), R.drawable.ic_product));

            mapView.getOverlays().add(mk);
            orderMarkers.add(mk);
        }
        mapView.invalidate();
    }

    /** Fit map để thấy cả mình + các đơn hàng (tuỳ chọn). */
    private void fitMapToMeAndOrders(List<DonDatHang> list){
        final List<GeoPoint> pts = new ArrayList<>();
        pts.add(new GeoPoint(curLat, curLng));
        for (DonDatHang d : list) {
            Double lat = Double.valueOf(d.getDelivery_lat());
            Double lng = Double.valueOf(d.getDelivery_lng());
            if (lat != null && lng != null) pts.add(new GeoPoint(lat, lng));
        }
        if (pts.size() < 2) return; // không đủ điểm để fit

        try {
            BoundingBox bb = BoundingBox.fromGeoPointsSafe(pts);
            mapView.zoomToBoundingBox(bb, true, 80); // padding 80px
        } catch (Exception ignore) { }
    }

    // ====== Adapter danh sách đơn gần ======
    private static class NearbyOrderAdapter extends RecyclerView.Adapter<NearbyOrderAdapter.VH> {
        private final List<DonDatHang> data = new ArrayList<>();
        void submit(List<DonDatHang> d){ data.clear(); if (d!=null) data.addAll(d); notifyDataSetChanged(); }

        @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_nearby_order, parent, false);
            return new VH(v);
        }

        @Override public void onBindViewHolder(@NonNull VH h, int i) {
            DonDatHang o = data.get(i);
            h.tvOrderId.setText("Mã đơn: " + safe(o.getID()));

            String pick = o.getPick_up_address() != null ? o.getPick_up_address() : "";
            String del  = o.getDelivery_address() != null ? o.getDelivery_address() : "";
            h.tvPickup.setText("Lấy: " + pick);
            h.tvDelivery.setText("Giao: " + del);

            String dist = "--";
            try {
                Double m = o.distance;
                if (m != null) dist = (m < 1000) ? String.format("~ %.0f m", m) : String.format("~ %.1f km", m/1000.0);
            } catch (Exception ignore) { }
            h.tvDistance.setText(dist);

            h.btnAccept.setOnClickListener(v -> {
                Toast.makeText(v.getContext(), "Nhận đơn " + safe(o.getID()), Toast.LENGTH_SHORT).show();
                // TODO: gọi API nhận đơn/assign shipper ở đây
            });
        }

        @Override public int getItemCount() { return data.size(); }

        static class VH extends RecyclerView.ViewHolder {
            TextView tvOrderId, tvPickup, tvDelivery, tvDistance;
            Button btnAccept;
            VH(@NonNull View v){
                super(v);
                tvOrderId = v.findViewById(R.id.tvOrderId);
                tvPickup = v.findViewById(R.id.tvPickup);
                tvDelivery = v.findViewById(R.id.tvDelivery);
                tvDistance = v.findViewById(R.id.tvDistance);
                btnAccept = v.findViewById(R.id.btnAccept);
            }
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
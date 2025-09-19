package com.hoaithanh.qlgh.activity;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.hoaithanh.qlgh.BuildConfig;
import com.hoaithanh.qlgh.R;
import com.hoaithanh.qlgh.api.ApiService;
import com.hoaithanh.qlgh.api.RetrofitClient;
import com.hoaithanh.qlgh.base.BaseActivity;
import com.hoaithanh.qlgh.model.DonDatHang;
import com.hoaithanh.qlgh.model.goong.DirectionResponse;
import com.hoaithanh.qlgh.model.goong.GeocodingResponse;
import com.hoaithanh.qlgh.model.goong.LatLng;
import com.hoaithanh.qlgh.model.goong.PolylineDecoder;
import com.hoaithanh.qlgh.repository.GoongRepository;

import org.osmdroid.config.Configuration;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChiTietDonHangActivity extends BaseActivity {
    private TextView tvId, tvStatus, tvRecipient, tvRecipientPhone, tvDeliveryAddr;
    private TextView tvDistanceTime, tvShip, tvCOD, tvCODFee, tvTotal, tvNote;
    private ProgressBar progress;
    private LinearLayout ctnContent;
    private MapView mapView;
    private final GoongRepository goongRepo = new GoongRepository();

    @Override
    public void initLayout() {
        setContentView(R.layout.activity_chi_tiet_don_hang);
    }

    @Override
    public void initData() {
        
    }

    @Override
    public void initView() {
        // Map needs user-agent
        Configuration.getInstance().setUserAgentValue(getPackageName());

        // bind UI
        tvId            = findViewById(R.id.tvId);
        tvStatus        = findViewById(R.id.tvStatus);
        tvRecipient     = findViewById(R.id.tvRecipient);
        tvRecipientPhone= findViewById(R.id.tvRecipientPhone);
        tvDeliveryAddr  = findViewById(R.id.tvDeliveryAddr);
        tvDistanceTime  = findViewById(R.id.tvDistanceTime);
        tvShip          = findViewById(R.id.tvShip);
        tvCOD           = findViewById(R.id.tvCOD);
        tvCODFee        = findViewById(R.id.tvCODFee);
        tvTotal         = findViewById(R.id.tvTotal);
        tvNote          = findViewById(R.id.tvNote);
        progress        = findViewById(R.id.progress);
        ctnContent      = findViewById(R.id.ctnContent);
        mapView         = findViewById(R.id.osmMap);
        mapView.setMultiTouchControls(true);

        // Lấy ID từ Intent
        String idStr = getIntent().getStringExtra("ID");
        if (idStr == null || idStr.trim().isEmpty()) {
            Toast.makeText(this, "Thiếu mã đơn hàng", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        int orderId;
        try { orderId = Integer.parseInt(idStr); } catch (Exception e) {
            Toast.makeText(this, "Mã đơn không hợp lệ", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        loadOrder(orderId);
    }

    private void loadOrder(int orderId) {
        showLoading(true);
        ApiService api = RetrofitClient.getApi();
        api.getOrderById(orderId).enqueue(new Callback<DonDatHang>() {
            @Override public void onResponse(Call<DonDatHang> call, Response<DonDatHang> res) {
                showLoading(false);
                if (!res.isSuccessful() || res.body() == null) {
                    toast("Lỗi server: " + res.code());
                    finish();
                    return;
                }
                bindUI(res.body());
            }
            @Override public void onFailure(Call<DonDatHang> call, Throwable t) {
                showLoading(false);
                toast("Lỗi kết nối: " + t.getMessage());
                finish();
            }
        });
    }
    private void bindUI(DonDatHang d) {
// ---- Bind text
        tvId.setText("Mã đơn: " + safe(d.getID()));
        tvStatus.setText(safe(d.getStatus()));
        tvRecipient.setText("Người nhận: " + safe(d.getRecipient()));
        tvRecipientPhone.setText("SĐT: " + safe(d.getRecipientPhone()));
        tvDeliveryAddr.setText("Giao đến: " + safe(d.getDelivery_address()));

        double cod = parseD(d.getCOD_amount());
        double ship = parseD(d.getShippingfee());
        double codFee = parseD(d.getCODFee());
        double total = ship + codFee;

        tvCOD.setText("COD: " + formatCurrency(cod));
        tvShip.setText("Phí ship: " + formatCurrency(ship));
        tvCODFee.setText("Phí COD: " + formatCurrency(codFee));
        tvTotal.setText("Tổng phí: " + formatCurrency(total));
        tvNote.setText("Ghi chú: " + safe(d.getNote()));

        // ---- Map: 2 marker + route
        Double pLat = toDouble(d.getPick_up_lat());
        Double pLng = toDouble(d.getPick_up_lng());
        Double dLat = toDouble(d.getDelivery_lat());
        Double dLng = toDouble(d.getDelivery_lng());

        if (pLat != null && pLng != null && dLat != null && dLng != null) {
            GeoPoint p1 = new GeoPoint(pLat, pLng);
            GeoPoint p2 = new GeoPoint(dLat, dLng);
            addMarker(p1, "Điểm lấy");
            addMarker(p2, "Điểm giao");
            fetchAndDrawRoute(p1, p2);
//            BoundingBox box = BoundingBox.fromGeoPointsSafe(List.of(p1, p2));
            List<GeoPoint> points = new ArrayList<>();
            points.add(p1);
            points.add(p2);
            BoundingBox box = BoundingBox.fromGeoPointsSafe(points);
            mapView.zoomToBoundingBox(box, true, 120);
        } else {
            // Fallback: geocode theo địa chỉ (đơn cũ)
            geocodeFallbackAndDraw(d.getPick_up_address(), d.getDelivery_address());
        }
    }

    // --- Geocoding fallback khi đơn thiếu lat/lng
    private void geocodeFallbackAndDraw(String pickAddr, String dropAddr) {
        final AtomicReference<GeoPoint> pRef = new AtomicReference<>(null);
        final AtomicReference<GeoPoint> dRef = new AtomicReference<>(null);
        String key = BuildConfig.GOONG_API_KEY;

        goongRepo.geocode(pickAddr, key).enqueue(new Callback<GeocodingResponse>() {
            @Override public void onResponse(Call<GeocodingResponse> call, Response<GeocodingResponse> r1) {
                if (okGeo(r1)) {
                    GeocodingResponse.Result r = r1.body().results.get(0);
                    pRef.set(new GeoPoint(r.geometry.location.lat, r.geometry.location.lng));
                }
                goongRepo.geocode(dropAddr, key).enqueue(new Callback<GeocodingResponse>() {
                    @Override public void onResponse(Call<GeocodingResponse> call2, Response<GeocodingResponse> r2) {
                        if (okGeo(r2)) {
                            GeocodingResponse.Result r = r2.body().results.get(0);
                            dRef.set(new GeoPoint(r.geometry.location.lat, r.geometry.location.lng));
                        }
                        if (pRef.get() != null && dRef.get() != null) {
                            addMarker(pRef.get(), "Điểm lấy");
                            addMarker(dRef.get(), "Điểm giao");
                            fetchAndDrawRoute(pRef.get(), dRef.get());
//                            BoundingBox box = BoundingBox.fromGeoPointsSafe(List.of(pRef.get(), dRef.get()));
                            List<GeoPoint> points = new ArrayList<>();
                            points.add(pRef.get());
                            points.add(dRef.get());
                            BoundingBox box = BoundingBox.fromGeoPointsSafe(points);
//                            mapView.zoomToBoundingBox(box, true, 120);
                            mapView.zoomToBoundingBox(box, true, 120);
                        } else {
                            tvDistanceTime.setText("Không xác định được tọa độ từ địa chỉ.");
                        }
                    }
                    @Override public void onFailure(Call<GeocodingResponse> call, Throwable t) {
                        tvDistanceTime.setText("Lỗi geocoding: " + t.getMessage());
                    }
                });
            }
            @Override public void onFailure(Call<GeocodingResponse> call, Throwable t) {
                tvDistanceTime.setText("Lỗi geocoding: " + t.getMessage());
            }
        });
    }

    private boolean okGeo(Response<GeocodingResponse> r) {
        return r.isSuccessful() && r.body()!=null && r.body().results!=null && !r.body().results.isEmpty()
                && r.body().results.get(0).geometry!=null && r.body().results.get(0).geometry.location!=null;
    }

    private void addMarker(GeoPoint p, String title) {
        Marker m = new Marker(mapView);
        m.setPosition(p);
        m.setTitle(title);
        m.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        mapView.getOverlays().add(m);
        mapView.invalidate();
    }

    private void fetchAndDrawRoute(GeoPoint origin, GeoPoint dest) {
        String o = origin.getLatitude() + "," + origin.getLongitude();
        String d = dest.getLatitude()   + "," + dest.getLongitude();

        goongRepo.getRoute(o, d, "car", BuildConfig.GOONG_API_KEY)
                .enqueue(new Callback<DirectionResponse>() {
                    @Override
                    public void onResponse(Call<DirectionResponse> call, Response<DirectionResponse> response) {
                        if (!response.isSuccessful() || response.body() == null ||
                                response.body().routes == null || response.body().routes.isEmpty()) {
                            tvDistanceTime.setText("Không lấy được tuyến đường.");
                            return;
                        }

                        DirectionResponse.Route route = response.body().routes.get(0);

                        double meter = 0, second = 0;
                        if (route.legs != null) {
                            for (DirectionResponse.Leg leg : route.legs) {
                                if (leg.distance != null) meter += leg.distance.value;
                                if (leg.duration != null) second += leg.duration.value;
                            }
                        }
                        if (meter > 0) {
                            tvDistanceTime.setText(String.format("Quãng đường: %.1f km • Thời gian: %.0f phút",
                                    meter/1000.0, second/60.0));
                        }

                        String encoded = (route.overview_polyline!=null) ? route.overview_polyline.points : null;
                        if (encoded == null) return;

                        List<LatLng> decoded = PolylineDecoder.decode(encoded);
                        List<GeoPoint> geoPts = new ArrayList<>();
                        for (LatLng p : decoded) geoPts.add(new GeoPoint(p.latitude, p.longitude));

                        Polyline line = new Polyline();
                        line.setPoints(geoPts);
                        line.setWidth(8f);
                        mapView.getOverlays().add(line);
                        mapView.invalidate();

                        BoundingBox box = BoundingBox.fromGeoPointsSafe(geoPts);
                        mapView.zoomToBoundingBox(box, true, 120);
                    }

                    @Override
                    public void onFailure(Call<DirectionResponse> call, Throwable t) {
                        tvDistanceTime.setText("Lỗi tuyến đường: " + t.getMessage());
                    }
                });
    }

    private void showLoading(boolean b) {
        if (progress != null) progress.setVisibility(b ? View.VISIBLE : View.GONE);
        if (ctnContent != null) ctnContent.setAlpha(b ? 0.4f : 1f);
    }

    private void toast(String m){ Toast.makeText(this, m, Toast.LENGTH_SHORT).show(); }
    private static String safe(String s){ return s==null?"":s.trim(); }
    private static double parseD(String s){ try{ return Double.parseDouble(s==null?"":s.trim()); }catch(Exception e){return 0;} }
    private static String formatCurrency(double v){ return String.format("%,.0fđ", v); }

    private static @Nullable Double toDouble(String s){
        try { return s==null?null:Double.parseDouble(s.trim()); }
        catch (Exception e){ return null; }
    }

    @Override protected void onResume() { super.onResume(); mapView.onResume(); }
    @Override protected void onPause()  { mapView.onPause(); super.onPause();  }
}
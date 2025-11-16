package com.hoaithanh.qlgh.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.VectorDrawable;
import android.net.Uri;
import android.os.*;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.gms.location.*;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.hoaithanh.qlgh.BuildConfig;
import com.hoaithanh.qlgh.R;
import com.hoaithanh.qlgh.base.BaseActivity;
import com.hoaithanh.qlgh.model.goong.DirectionResponse;
import com.hoaithanh.qlgh.model.goong.LatLng;
import com.hoaithanh.qlgh.repository.GoongRepository;
import com.hoaithanh.qlgh.model.*;
import com.hoaithanh.qlgh.model.goong.PolylineDecoder;
import com.hoaithanh.qlgh.session.SessionManager;
import com.hoaithanh.qlgh.viewmodel.DonDatHangViewModel;

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Locale;

import retrofit2.*;

public class ShipperOrdersDetailActivity extends BaseActivity {

    private MapView mapView;
    private IMapController mapController;
    private TextView tvStage, tvEta, tvPickupAddr, tvDeliveryAddr, tvRecipient, tvCod, tvShipFee, tvNote, tvSender;
    private TextView tvTotalCollect, tvFeePayer, tvTotalCollectLabel, tvCodFee;
    private MaterialButton btnCall, btnNavigate, btnCopyAddr, btnPrimary, btnFail, btnCancelByShipper;
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
    private Marker shipperLocationMarker;

    // Giới hạn gọi Directions
    private static final long MIN_INTERVAL_MS = 15000;
    private static final float MIN_MOVE_METERS = 50f;
    private long lastRouteTime = 0L;
    private double lastRouteLat = Double.NaN, lastRouteLng = Double.NaN;
    private String lastRouteKey = "";

    private DonDatHangViewModel viewModel;  //update trang thai don hang tren db

    private com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton fabTakePhoto;
    private static final int REQ_TAKE_PHOTO = 456;
    private static final int PERMISSION_REQ_CAMERA = 99;

    private String currentPhotoPath; // Đường dẫn file ảnh cục bộ tạm thời
    private String uploadedPhotoUrl = null; // ✅ URL ảnh đã upload thành công
    private String pendingUpdateStatus = null; // Trạng thái đang chờ cập nhật (picked_up hoặc delivered)

    private SessionManager session;

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
        tvCodFee = findViewById(R.id.tvCodFee);
        tvShipFee = findViewById(R.id.tvShipFee);
        tvTotalCollect = findViewById(R.id.tvTotalCollect);
        tvTotalCollectLabel = findViewById(R.id.tvTotalCollectLabel);
        tvFeePayer = findViewById(R.id.tvFeePayer);
        tvNote = findViewById(R.id.tvNote);
        btnCall = findViewById(R.id.btnCall);
        btnNavigate = findViewById(R.id.btnNavigate);
        btnCopyAddr = findViewById(R.id.btnCopyAddr);
        btnPrimary = findViewById(R.id.btnPrimary);
        btnFail = findViewById(R.id.btnFail);
        btnCancelByShipper = findViewById(R.id.btnCancelByShipper);
        btnMyLocation = findViewById(R.id.btnMyLocation);

        fabTakePhoto = findViewById(R.id.fabTakePhoto); // Ánh xạ nút mới
        fabTakePhoto.setOnClickListener(v -> checkCameraPermissions());
        session = new SessionManager(getApplicationContext());
        viewModel = new ViewModelProvider(this).get(DonDatHangViewModel.class);
        observeViewModel();

        signInAnonymouslyThenContinue();
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

    // 2a. Hàm kiểm tra quyền
    private void checkCameraPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    PERMISSION_REQ_CAMERA);
        } else {
            // Quyền đã được cấp
            dispatchTakePictureIntent();
        }
    }

    // 2b. Xử lý kết quả yêu cầu quyền
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQ_CAMERA) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                dispatchTakePictureIntent();
            } else {
                Toast.makeText(this, "Tính năng chụp ảnh yêu cầu quyền Camera.", Toast.LENGTH_LONG).show();
            }
        }
    }

    // 2c. Tạo file tạm thời
    private File createImageFile() throws IOException {
        @SuppressLint("SimpleDateFormat")
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);

        File image = File.createTempFile(imageFileName, ".jpg", storageDir);
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    // 2d. Mở Camera Intent
    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                Toast.makeText(this, "Không thể tạo file ảnh tạm thời", Toast.LENGTH_SHORT).show();
                return;
            }

            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        getPackageName() + ".fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
//                takePictureIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
//                takePictureIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                // ✅ BƯỚC SỬA 1: Dùng setFlags (thay vì addFlags) để đảm bảo quyền được áp dụng
                takePictureIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

                // ✅ BƯỚC SỬA 2: Cấp quyền ghi vào danh sách quyền tạm thời của Camera App
                List<ResolveInfo> resInfoList = getPackageManager().queryIntentActivities(takePictureIntent, PackageManager.MATCH_DEFAULT_ONLY);
                for (ResolveInfo resolveInfo : resInfoList) {
                    String packageName = resolveInfo.activityInfo.packageName;
                    grantUriPermission(packageName, photoURI, Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
                }

                startActivityForResult(takePictureIntent, REQ_TAKE_PHOTO);
            }
        }
    }

    private void uploadPhotoToFirebase(String filePath) {
        if (order == null || order.getID() == null || pendingUpdateStatus == null) return;

        Log.d("FIREBASE_UPLOAD", "Starting upload for path: " + filePath);

        Uri file = Uri.fromFile(new File(filePath));
        String path = "shipper_proofs/" + order.getID() + "/" + pendingUpdateStatus + "_" + System.currentTimeMillis() + ".jpg";
        StorageReference photoRef = FirebaseStorage.getInstance().getReference().child(path);

        // showLoadingDialog();

        photoRef.putFile(file)
                .addOnProgressListener(snapshot -> {
                    // 1. Tính toán phần trăm tiến trình
                    double progress = (100.0 * snapshot.getBytesTransferred()) / snapshot.getTotalByteCount();

                    // 2. Cập nhật UI trên Main Thread
                    runOnUiThread(() -> {
                        String progressText = String.format(Locale.getDefault(), "Đang tải lên: %.0f%%", progress);
                        fabTakePhoto.setText(progressText);
                        // Có thể vô hiệu hóa nút Primary tạm thời để tránh race condition
                        btnPrimary.setEnabled(false);
                    });
                })
                .addOnSuccessListener(taskSnapshot -> {
                    btnPrimary.setEnabled(true);
                    photoRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        String photoUrl = uri.toString();

                        // ✅ LƯU URL CỤC BỘ VÀ CẬP NHẬT UI CHỤP ẢNH
                        uploadedPhotoUrl = photoUrl;
                        String statusName = "picked_up".equals(pendingUpdateStatus) ? "Lấy hàng" : "Giao hàng";
                        fabTakePhoto.setText("Đã chụp (" + statusName + ")");
                        fabTakePhoto.setIcon(getDrawable(R.drawable.ic_check));

                        Toast.makeText(this, "Ảnh đã được tải lên Cloud.", Toast.LENGTH_SHORT).show();

                        // Dọn dẹp file tạm
                        new File(filePath).delete();
                        currentPhotoPath = null;
                        // hideLoadingDialog();
                        updateButtonsForStatus(); // Cập nhật lại nút Primary (Bắt đầu)
                    });
                })
                .addOnFailureListener(e -> {
                    btnPrimary.setEnabled(true);
                    // hideLoadingDialog();
                    Toast.makeText(this, "Upload ảnh thất bại: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    uploadedPhotoUrl = null;
                    new File(filePath).delete();
                    currentPhotoPath = null;
                    updateButtonsForStatus();
                });

    }

    private void compressAndUpload(String sourcePath) {
        new Thread(() -> {
            try {
                // 1. Đọc Bitmap từ file nguồn
                Bitmap bitmap = BitmapFactory.decodeFile(sourcePath);

                // 2. Tạo file đích mới (Ví dụ: nén)
                File compressedFile = new File(getCacheDir(), "compressed_" + System.currentTimeMillis() + ".jpg");

                FileOutputStream fos = new FileOutputStream(compressedFile);
                // ✅ NÉN ẢNH: Đặt chất lượng thấp hơn (Ví dụ: 70)
                bitmap.compress(Bitmap.CompressFormat.JPEG, 70, fos);
                fos.close();

                // 3. Xóa file gốc có kích thước lớn
                new File(sourcePath).delete();

                // 4. CHẠY LẠI TRÊN MAIN THREAD để gọi hàm upload
//                runOnUiThread(() -> {
//                    uploadPhotoToFirebase(compressedFile.getAbsolutePath());
//                });
                uploadPhotoToFirebase(compressedFile.getAbsolutePath());

            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(ShipperOrdersDetailActivity.this, "Lỗi nén ảnh: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private void signInAnonymouslyThenContinue() {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = auth.getCurrentUser();

        // Nếu đã có người dùng đăng nhập (dù là ẩn danh), thoát
        if (currentUser != null) {
            Log.d("FIREBASE_AUTH", "Người dùng đã đăng nhập.");
            return;
        }

        // Tiến hành đăng nhập ẩn danh
        auth.signInAnonymously()
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Log.d("FIREBASE_AUTH", "Đăng nhập ẩn danh thành công.");
                    } else {
                        Log.e("FIREBASE_AUTH", "Đăng nhập ẩn danh thất bại.", task.getException());
                        // Nếu đăng nhập ẩn danh thất bại, chặn chức năng chụp ảnh
                        Toast.makeText(this, "Lỗi xác thực, không thể upload ảnh bằng chứng.", Toast.LENGTH_LONG).show();
                        // ✅ Vô hiệu hóa nút chụp ảnh
                        fabTakePhoto.setEnabled(false);
                    }
                });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQ_TAKE_PHOTO) {
            if (resultCode == RESULT_OK) {
                if (currentPhotoPath != null) {
                    // ✅ Bắt đầu quá trình tải lên Firebase
                    // Xác định trạng thái chờ (cho việc đặt tên file Firebase)
                    String statusForPhoto = getNextStatus(order.getStatus());
                    pendingUpdateStatus = statusForPhoto != null ? statusForPhoto : order.getStatus();

//                    uploadPhotoToFirebase(currentPhotoPath);
                    compressAndUpload(currentPhotoPath);
                }
            } else {
                // Người dùng hủy chụp
                Toast.makeText(this, "Đã hủy chụp ảnh. Vui lòng chụp lại.", Toast.LENGTH_SHORT).show();
                if (currentPhotoPath != null) {
                    new File(currentPhotoPath).delete();
                    currentPhotoPath = null;
                }
            }
        }
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
            if (apiResponse != null) {
                if (apiResponse.isSuccess()) {
                    Toast.makeText(this, "Cập nhật thành công!", Toast.LENGTH_SHORT).show();

                    if (apiResponse.getMessage().contains("Đã hủy đơn")) {
                        order.setStatus("cancelled");
                    } else {
                        // Logic cũ của bạn: Tính toán trạng thái tiếp theo
                        String currentStatus = order.getStatus();
                        if (!"delivery_failed".equals(currentStatus)) {
                            String nextStatus = getNextStatus(currentStatus);
                            if (nextStatus != null) {
                                order.setStatus(nextStatus);
                            }
                        }
                    }
//                    String currentStatus = order.getStatus(); // Lấy trạng thái đã được set TẠM THỜI
//                    if (!"delivery_failed".equals(currentStatus)) {
//                        // Nếu KHÔNG phải trạng thái thất bại, ta tính toán trạng thái tiếp theo
//                        String nextStatus = getNextStatus(currentStatus);
//                        if (nextStatus != null) {
//                            order.setStatus(nextStatus); // Cập nhật trạng thái
//                        }
//                    }


                    tvStage.setText(statusToStage(order.getStatus()));
                    drawRouteForStatus();
                    updateButtonsForStatus();

                } else {
                    String message = (apiResponse.getMessage() != null) ? apiResponse.getMessage() : "Có lỗi xảy ra";
                    Toast.makeText(this, "Cập nhật thất bại: " + message, Toast.LENGTH_LONG).show();
                }
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
                updateShipperMarker();
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

    private void updateShipperMarker() {
        if (!hasFix || mapView == null) return;

        GeoPoint currentPos = new GeoPoint(curLat, curLng);

        if (shipperLocationMarker == null) {
            // Lần đầu tiên: Khởi tạo Marker Shipper
            Drawable icon = resizeDrawable(R.drawable.ic_service, 24);
            if (icon == null) return;

            shipperLocationMarker = new Marker(mapView);
            shipperLocationMarker.setPosition(currentPos);
            shipperLocationMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
            shipperLocationMarker.setIcon(icon);
            mapView.getOverlays().add(shipperLocationMarker);

        } else {
            // Các lần sau: Chỉ cập nhật vị trí
            shipperLocationMarker.setPosition(currentPos);
        }

        // Bắt buộc gọi invalidate để vẽ lại
        mapView.invalidate();
    }

    /** ============= MAP & ROUTE ============= **/
    private boolean shouldRecalculate(String origin, String dest) {
        long now = System.currentTimeMillis();
        if (now - lastRouteTime < MIN_INTERVAL_MS) return false;
        String key = origin + "|" + dest + "|bike";
        if (!key.equals(lastRouteKey)) return true;
        if (Double.isNaN(lastRouteLat) || Double.isNaN(lastRouteLng)) return true;

        return true;
    }

    private void drawRouteForStatus() {
        if (order == null || Double.isNaN(curLat) || Double.isNaN(curLng)) {
            return;
        }
        String status = order.getStatus();
        String origin = curLat + "," + curLng; // Điểm bắt đầu LUÔN LÀ vị trí hiện tại của shipper
        String dest; // Điểm đến

        if ("accepted".equals(status)) {
            // Đích đến là ĐIỂM LẤY HÀNG
            dest = order.getPick_up_lat() + "," + order.getPick_up_lng();
        } else if ("picked_up".equals(status) || "in_transit".equals(status)) {
            // Đích đến là ĐIỂM GIAO HÀNG
            dest = order.getDelivery_lat() + "," + order.getDelivery_lng();
        } else {
            // Nếu đơn hàng đã hoàn thành, bị hủy, hoặc đang pending
            // thì không cần tính ETA real-time
            return;
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

                if (mapView != null) {
                    // 1. Tạo danh sách mới chỉ giữ lại Marker Shipper (nếu đã tồn tại)
                    List<org.osmdroid.views.overlay.Overlay> newOverlays = new ArrayList<>();
                    if (shipperLocationMarker != null) {
                        newOverlays.add(shipperLocationMarker);
                    }

                    // 2. Xóa tất cả và thêm lại Marker Shipper (ngăn lỗi trùng lặp)
                    mapView.getOverlays().clear();
                    mapView.getOverlays().addAll(newOverlays);
                }
                // 3. VẼ POLYLINE VÀ MARKER TUYẾN ĐƯỜNG
                Polyline line = new Polyline();
                line.setPoints(points);
                line.setWidth(6f);
                mapView.getOverlays().add(line); // Thêm Polyline

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
        // --- Hiển thị thông tin cơ bản (giữ nguyên) ---
        tvPickupAddr.setText(order.getPick_up_address());
        tvDeliveryAddr.setText(order.getDelivery_address());
        tvRecipient.setText("Người nhận: " + order.getRecipient() + " (" + order.getRecipientPhone() + ")");
        tvSender.setText("Người gửi: " + order.getUserName());
        tvNote.setText("Ghi chú: " + (order.getNote() == null ? "--" : order.getNote()));
        tvStage.setText(statusToStage(order.getStatus()));

        double shippingFee = parseD(order.getShippingfee());
        double codAmount = parseD(order.getCOD_amount());
        double codFee = parseD(order.getCODFee());
        String feePayer = safe(order.getFee_payer()).toLowerCase();

        // Hiển thị các khoản phí riêng lẻ
        tvShipFee.setText(formatCurrency(String.valueOf(shippingFee)));
        tvCod.setText(formatCurrency(String.valueOf(codAmount)));
        tvCodFee.setText(formatCurrency(String.valueOf(codFee)));

        double totalToCollect; // Số tiền tổng cộng cần thu từ người nhận

        if ("sender".equals(feePayer)) {
            // --- TRƯỜNG HỢP 1: NGƯỜI GỬI TRẢ PHÍ ---
            tvFeePayer.setText("(Người gửi trả)");
            tvFeePayer.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray));
//            totalToCollect = codAmount;
            totalToCollect = codAmount + codFee;
            tvTotalCollectLabel.setText("Tổng thu (COD + Phí COD):");

            if (codAmount <= 0) {
                tvTotalCollectLabel.setText("Tổng thu tại điểm giao:");
            } else {
                tvTotalCollectLabel.setText("Tổng thu (chỉ COD):");
            }
        } else { // "receiver" pays
            // --- TRƯỜNG HỢP 2: NGƯỜI NHẬN TRẢ PHÍ ---
            tvFeePayer.setText("(Người nhận trả)");
            tvFeePayer.setTextColor(ContextCompat.getColor(this, R.color.main_route_color)); // Màu xanh để nhấn mạnh
            // Shipper phải thu cả tiền COD và phí vận chuyển
//            totalToCollect = codAmount + shippingFee;
            totalToCollect = codAmount + codFee + shippingFee;
        }

        Log.d("PAYER_DEBUG", "Giá trị được gán cho tvFeePayer: '" + feePayer + "'");
        // Hiển thị số tiền tổng cộng cần thu
        tvTotalCollect.setText(formatCurrency(String.valueOf(totalToCollect)));


        // --- Gán sự kiện cho các nút (giữ nguyên) ---
        btnCall.setOnClickListener(v -> showCallDialog());
        btnCopyAddr.setOnClickListener(v -> copyToClipboard(order.getDelivery_address()));
        btnNavigate.setOnClickListener(v -> openGoogleMaps(order.getDelivery_lat(), order.getDelivery_lng()));
        btnPrimary.setOnClickListener(v -> updateStatusNextStage());
        btnFail.setOnClickListener(v -> showDeliveryFailureDialog());
        btnCancelByShipper.setOnClickListener(v -> {
            // Hiển thị Dialog bắt buộc chọn lý do
            showCancelReasonDialog();
        });
    }

    private void showCancelReasonDialog() {
        final String[] reasons = new String[] {
                "Hỏng xe",
                "Tai nạn hoặc khẩn cấp",
                "Không liên lạc được người gửi",
                "Lý do khác"
        };

        new MaterialAlertDialogBuilder(this)
                .setTitle("Chọn lý do hủy đơn")
                .setItems(reasons, (dialog, which) -> {
                    String reason = reasons[which];

                    // Hiển thị dialog xác nhận lần 2
                    new MaterialAlertDialogBuilder(this)
                            .setTitle("Cảnh báo")
                            .setMessage("Việc hủy đơn sẽ ảnh hưởng tiêu cực đến điểm đánh giá (rating) của bạn. Bạn có chắc chắn muốn hủy?")
                            .setNegativeButton("Không", null)
                            .setPositiveButton("Vẫn Hủy", (d2, w2) -> {
                                // Gọi ViewModel để hủy đơn
                                int orderId = Integer.parseInt(order.getID());
                                viewModel.shipperCancelOrder(orderId, reason); // Bạn cần tạo hàm này trong ViewModel
                            })
                            .show();
                })
                .setNegativeButton("Đóng", null)
                .show();
    }

    private static double parseD(String s) {
        try {
            return Double.parseDouble(s == null ? "0" : s.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static String safe(String s) {
        return s == null ? "" : s.trim();
    }

    private void showDeliveryFailureDialog() {

        // Lý do thất bại phổ biến
        final String[] failureReasons = new String[] {
                "Người nhận không liên lạc được",
                "Người nhận từ chối nhận hàng",
                "Địa chỉ giao hàng không chính xác",
                "Không thể tiếp cận địa điểm giao hàng",
                "Lý do khác"
        };

        // Hiển thị dialog để chọn lý do
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle("Chọn lý do giao hàng thất bại")
                .setItems(failureReasons, (dialog, which) -> {
                    String reason = failureReasons[which];
                    String failedStatus = "delivery_failed";

                    // Kiểm tra và lấy orderId
                    int orderId;
                    try {
                        orderId = Integer.parseInt(order.getID());
                    } catch (NumberFormatException e) {
                        Toast.makeText(this, "ID đơn hàng không hợp lệ.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    order.setStatus(failedStatus);

                    viewModel.updateOrderStatus(orderId, failedStatus, reason);

                    Toast.makeText(this, "Đang báo cáo thất bại: " + reason, Toast.LENGTH_SHORT).show();

                })
                .setNegativeButton("Hủy", (dialog, which) -> dialog.dismiss())
                .show();
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
            case "delivery_failed": return "Giao hàng thất bại";
            case "cancelled": return "Đã hủy";
            default: return "Không xác định";
        }
    }

    private void updateStatusNextStage() {
        String nextStatus = getNextStatus(order.getStatus());
        if (nextStatus == null) {
            Toast.makeText(this, "Đơn đã hoàn thành", Toast.LENGTH_SHORT).show();
            return;
        }

        int orderId = Integer.parseInt(order.getID());

        // 1. Kiểm tra BẮT BUỘC chụp ảnh
        if (("picked_up".equals(nextStatus) || "delivered".equals(nextStatus))) {
            if (TextUtils.isEmpty(uploadedPhotoUrl)) {
                Toast.makeText(this, "Vui lòng chụp ảnh bằng chứng trước khi xác nhận!", Toast.LENGTH_LONG).show();
                return;
            }
            viewModel.updateOrderStatusWithPhoto(orderId, nextStatus, uploadedPhotoUrl);
            uploadedPhotoUrl = null; // Reset sau khi gọi API
        }else {
            // ✅ TRƯỜNG HỢP KHÔNG CẦN ẢNH: (accepted -> picked_up hoặc in_transit)
            viewModel.updateOrderStatus(orderId, nextStatus);
        }
    }


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
            case "delivery_failed":
            case "cancelled":
                btnPrimary.setText(statusToStage(status));
                btnPrimary.setEnabled(false); // Vô hiệu hóa nút
                break;
            default:
                btnPrimary.setText("Không rõ");
                btnPrimary.setEnabled(false);
        }

        // 2. Cập nhật nút Phụ (btnFail)
//        if ("delivered".equals(status) ||
//                "delivery_failed".equals(status) || // ✅ THÊM ĐIỀU KIỆN NÀY
//                "cancelled".equals(status)) {
//            btnFail.setVisibility(View.GONE); // Ẩn nút báo thất bại nếu đã giao
//        } else {
//            btnFail.setVisibility(View.VISIBLE);
//        }
        if ("picked_up".equals(status) || "in_transit".equals(status)) {
            btnFail.setVisibility(View.VISIBLE);
        } else {
            btnFail.setVisibility(View.GONE);
        }

        if (("accepted".equals(status) || "in_transit".equals(status)) && uploadedPhotoUrl == null) {
            fabTakePhoto.setVisibility(View.VISIBLE);

            // Cập nhật text dựa trên trạng thái tiếp theo
            String nextStatus = getNextStatus(status);
            String statusName = "picked_up".equals(nextStatus) ? "Lấy hàng" : "Giao hàng";
            fabTakePhoto.setText("Chụp ảnh " + statusName);
            fabTakePhoto.setIcon(getDrawable(R.drawable.ic_camera));
        } else {
            // ✅ Nút sẽ ẩn đi nếu ảnh đã được chụp (uploadedPhotoUrl != null)
            fabTakePhoto.setVisibility(View.GONE);
        }

        if ("accepted".equals(status)) {
            btnCancelByShipper.setVisibility(View.VISIBLE);
        } else {
            btnCancelByShipper.setVisibility(View.GONE);
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (fused != null && locCallback != null)
            fused.removeLocationUpdates(locCallback);
    }
}

package com.hoaithanh.qlgh.activity;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.hoaithanh.qlgh.R;
import com.hoaithanh.qlgh.base.BaseActivity;
import com.hoaithanh.qlgh.database.AppDatabase;
import com.hoaithanh.qlgh.database.DatabaseHelper;
import com.hoaithanh.qlgh.database.DonDatHangDAO;
import com.hoaithanh.qlgh.model.DonDatHang;


public class DonDatHangActivity extends BaseActivity {

    private CardView cardSenderInfo, cardReceiverInfo, cardProductInfo;
    private LinearLayout formSenderInfo, formReceiverInfo, formProductInfo;
    private TextView tvSenderPlaceholder, tvReceiverPlaceholder, tvProductPlaceholder;
    private TextView tvShippingFee, tvCodFee, tvTotal;
    private EditText etCodAmount;
    private RadioGroup rgService;
    private boolean isSenderExpanded = false;
    private boolean isReceiverExpanded = false;
    private boolean isProductExpanded = false;

    private AppDatabase database;
    private DonDatHangDAO donDatHangDAO;

    public DonDatHangActivity() {
    }

    @Override
    public void initLayout() {
        setContentView(R.layout.activity_don_dat_hang);
    }

    @Override
    public void initData() {

    }

    @Override
    public void initView() {
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

        // Khởi tạo database
        database = DatabaseHelper.getInstance(this);
        donDatHangDAO = database.donDatHangDAO();




        setupClickListeners();


        // Nút lưu thông tin
        findViewById(R.id.btn_sender_save).setOnClickListener(v -> collapseSenderInfo());
        findViewById(R.id.btn_receiver_save).setOnClickListener(v -> collapseReceiverInfo());
        findViewById(R.id.btn_product_save).setOnClickListener(v -> collapseProductInfo());

        // Nút hủy và đặt đơn
        findViewById(R.id.btn_cancel).setOnClickListener(v -> finish());
        findViewById(R.id.btn_submit).setOnClickListener(v -> submitOrder());
    }


    private void setupClickListeners() {
        // Xử lý click thông tin người gửi
        cardSenderInfo.setOnClickListener(v -> {
            if (isSenderExpanded) {
                collapseSenderInfo();
            } else {
                expandSenderInfo();
            }
        });

        // Xử lý click thông tin người nhận
        cardReceiverInfo.setOnClickListener(v -> {
            if (isReceiverExpanded) {
                collapseReceiverInfo();
            } else {
                expandReceiverInfo();
            }
        });

        // Xử lý click thông tin hàng hóa
        cardProductInfo.setOnClickListener(v -> {
            if (isProductExpanded) {
                collapseProductInfo();
            } else {
                expandProductInfo();
            }
        });

        // Xử lý thay đổi dịch vụ và số tiền COD
        rgService.setOnCheckedChangeListener((group, checkedId) -> calculateFees());
        etCodAmount.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                calculateFees();
            }
        });
    }

    private void expandSenderInfo() {
        formSenderInfo.setVisibility(View.VISIBLE);
        tvSenderPlaceholder.setVisibility(View.GONE);
        isSenderExpanded = true;

        // Thu gọn các form khác nếu đang mở
        if (isReceiverExpanded) collapseReceiverInfo();
        if (isProductExpanded) collapseProductInfo();
    }

    private void collapseSenderInfo() {
        formSenderInfo.setVisibility(View.GONE);
        tvSenderPlaceholder.setVisibility(View.VISIBLE);

        // Cập nhật placeholder nếu có dữ liệu
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

        // Thu gọn các form khác nếu đang mở
        if (isSenderExpanded) collapseSenderInfo();
        if (isProductExpanded) collapseProductInfo();
    }

    private void collapseReceiverInfo() {
        formReceiverInfo.setVisibility(View.GONE);
        tvReceiverPlaceholder.setVisibility(View.VISIBLE);

        // Cập nhật placeholder nếu có dữ liệu
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

        // Thu gọn các form khác nếu đang mở
        if (isSenderExpanded) collapseSenderInfo();
        if (isReceiverExpanded) collapseReceiverInfo();
    }

    private void collapseProductInfo() {
        formProductInfo.setVisibility(View.GONE);
        tvProductPlaceholder.setVisibility(View.VISIBLE);

        // Cập nhật placeholder nếu có dữ liệu
        EditText etName = findViewById(R.id.et_product_name);
        if (!TextUtils.isEmpty(etName.getText())) {
            tvProductPlaceholder.setText(etName.getText().toString());
        }

        isProductExpanded = false;
    }

    private void calculateFees() {
        int shippingFee = 25000;

        // Kiểm tra dịch vụ được chọn
        if (rgService.getCheckedRadioButtonId() == R.id.rb_express) {
            shippingFee = 45000;
        }

        // Tính phí COD (2% của số tiền thu hộ)
        int codAmount = 0;
        try {
            codAmount = Integer.parseInt(etCodAmount.getText().toString());
        } catch (NumberFormatException e) {
            // Nếu không nhập số, mặc định là 0
        }

        int codFee = (int) (codAmount * 0.02);
        int total = shippingFee + codFee;

        // Cập nhật giao diện
        tvShippingFee.setText(String.format("%,dđ", shippingFee));
        tvCodFee.setText(String.format("%,dđ", codFee));
        tvTotal.setText(String.format("%,dđ", total));
    }

    private void submitOrder() {
        // Lấy dữ liệu từ form
        String tenNguoiGui = ((EditText) findViewById(R.id.et_sender_name)).getText().toString();
        String sdtNguoiGui = ((EditText) findViewById(R.id.et_sender_phone)).getText().toString();
        String diaChiNguoiGui = ((AutoCompleteTextView) findViewById(R.id.actv_sender_address)).getText().toString();

        String tenNguoiNhan = ((EditText) findViewById(R.id.et_receiver_name)).getText().toString();
        String sdtNguoiNhan = ((EditText) findViewById(R.id.et_receiver_phone)).getText().toString();
        String diaChiNguoiNhan = ((AutoCompleteTextView) findViewById(R.id.actv_receiver_address)).getText().toString();

        String tenHangHoa = ((EditText) findViewById(R.id.et_product_name)).getText().toString();
        double khoiLuong = Double.parseDouble(((EditText) findViewById(R.id.et_product_weight)).getText().toString());
        int giaTri = Integer.parseInt(((EditText) findViewById(R.id.et_product_value)).getText().toString());
        String ghiChu = ((EditText) findViewById(R.id.et_product_note)).getText().toString();

        // Lấy loại dịch vụ
        String loaiDichVu = "Tiêu chuẩn";
        int phiVanChuyen = 25000;
        if (rgService.getCheckedRadioButtonId() == R.id.rb_express) {
            loaiDichVu = "Hỏa tốc";
            phiVanChuyen = 45000;
        }

        // Lấy thông tin COD
        int soTienThuHo = 0;
        try {
            soTienThuHo = Integer.parseInt(etCodAmount.getText().toString());
        } catch (NumberFormatException e) {
            soTienThuHo = 0;
        }

        int phiCOD = (int) (soTienThuHo * 0.02);
        int tongTien = phiVanChuyen + phiCOD;

        // Tạo đối tượng DonDatHang
        DonDatHang donDatHang = new DonDatHang();
        donDatHang.setTenNguoiGui(tenNguoiGui);
        donDatHang.setSdtNguoiGui(sdtNguoiGui);
        donDatHang.setDiaChiNguoiGui(diaChiNguoiGui);

        donDatHang.setTenNguoiNhan(tenNguoiNhan);
        donDatHang.setSdtNguoiNhan(sdtNguoiNhan);
        donDatHang.setDiaChiNguoiNhan(diaChiNguoiNhan);

        donDatHang.setTenHangHoa(tenHangHoa);
        donDatHang.setKhoiLuong(khoiLuong);
        donDatHang.setGiaTri(giaTri);
        donDatHang.setGhiChu(ghiChu);

        donDatHang.setLoaiDichVu(loaiDichVu);
        donDatHang.setPhiVanChuyen(phiVanChuyen);

        donDatHang.setSoTienThuHo(soTienThuHo);
        donDatHang.setPhiCOD(phiCOD);
        donDatHang.setTongTien(tongTien);

        // Lưu vào database
        new Thread(() -> {
            long id = donDatHangDAO.insert(donDatHang);

            runOnUiThread(() -> {
                if (id > 0) {
                    Toast.makeText(DonDatHangActivity.this,
                            "Đơn hàng đã được đặt thành công! Mã đơn: " + donDatHang.getMaDonHang(),
                            Toast.LENGTH_LONG).show();
//                    finish();
                    Intent intent = new Intent(DonDatHangActivity.this, DanhSachDonHangActivity.class);
                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(DonDatHangActivity.this,
                            "Có lỗi xảy ra khi đặt đơn hàng",
                            Toast.LENGTH_SHORT).show();
                }
            });
        }).start();
    }

}
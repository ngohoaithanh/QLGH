package com.hoaithanh.qlgh.model;

import androidx.databinding.adapters.Converters;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

//import com.hoaithanh.qlgh.database.Converters;

import java.util.Date;

@Entity(tableName = "don_dat_hang")
public class DonDatHang {
    @PrimaryKey(autoGenerate = true)
    private int id;

    private String maDonHang;
    private Date thoiGianTao;
    private String trangThai;

    // Thông tin người gửi
    private String tenNguoiGui;
    private String sdtNguoiGui;
    private String diaChiNguoiGui;

    // Thông tin người nhận
    private String tenNguoiNhan;
    private String sdtNguoiNhan;
    private String diaChiNguoiNhan;

    // Thông tin hàng hóa
    private String tenHangHoa;
    private double khoiLuong;
    private int giaTri;
    private String ghiChu;

    // Thông tin dịch vụ
    private String loaiDichVu;
    private int phiVanChuyen;

    // Thông tin COD
    private int soTienThuHo;
    private int phiCOD;
    private int tongTien;

    // Constructor
    public DonDatHang() {
        this.thoiGianTao = new Date();
        this.trangThai = "Mới";
        this.maDonHang = generateMaDonHang();
    }

    private String generateMaDonHang() {
        return "DDH" + System.currentTimeMillis();
    }

    // Getter và Setter methods
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getMaDonHang() { return maDonHang; }
    public void setMaDonHang(String maDonHang) { this.maDonHang = maDonHang; }

    public Date getThoiGianTao() { return thoiGianTao; }
    public void setThoiGianTao(Date thoiGianTao) { this.thoiGianTao = thoiGianTao; }

    public String getTrangThai() { return trangThai; }
    public void setTrangThai(String trangThai) { this.trangThai = trangThai; }

    public String getTenNguoiGui() { return tenNguoiGui; }
    public void setTenNguoiGui(String tenNguoiGui) { this.tenNguoiGui = tenNguoiGui; }

    public String getSdtNguoiGui() { return sdtNguoiGui; }
    public void setSdtNguoiGui(String sdtNguoiGui) { this.sdtNguoiGui = sdtNguoiGui; }

    public String getDiaChiNguoiGui() { return diaChiNguoiGui; }
    public void setDiaChiNguoiGui(String diaChiNguoiGui) { this.diaChiNguoiGui = diaChiNguoiGui; }

    public String getTenNguoiNhan() { return tenNguoiNhan; }
    public void setTenNguoiNhan(String tenNguoiNhan) { this.tenNguoiNhan = tenNguoiNhan; }

    public String getSdtNguoiNhan() { return sdtNguoiNhan; }
    public void setSdtNguoiNhan(String sdtNguoiNhan) { this.sdtNguoiNhan = sdtNguoiNhan; }

    public String getDiaChiNguoiNhan() { return diaChiNguoiNhan; }
    public void setDiaChiNguoiNhan(String diaChiNguoiNhan) { this.diaChiNguoiNhan = diaChiNguoiNhan; }

    public String getTenHangHoa() { return tenHangHoa; }
    public void setTenHangHoa(String tenHangHoa) { this.tenHangHoa = tenHangHoa; }

    public double getKhoiLuong() { return khoiLuong; }
    public void setKhoiLuong(double khoiLuong) { this.khoiLuong = khoiLuong; }

    public int getGiaTri() { return giaTri; }
    public void setGiaTri(int giaTri) { this.giaTri = giaTri; }

    public String getGhiChu() { return ghiChu; }
    public void setGhiChu(String ghiChu) { this.ghiChu = ghiChu; }

    public String getLoaiDichVu() { return loaiDichVu; }
    public void setLoaiDichVu(String loaiDichVu) { this.loaiDichVu = loaiDichVu; }

    public int getPhiVanChuyen() { return phiVanChuyen; }
    public void setPhiVanChuyen(int phiVanChuyen) { this.phiVanChuyen = phiVanChuyen; }

    public int getSoTienThuHo() { return soTienThuHo; }
    public void setSoTienThuHo(int soTienThuHo) { this.soTienThuHo = soTienThuHo; }

    public int getPhiCOD() { return phiCOD; }
    public void setPhiCOD(int phiCOD) { this.phiCOD = phiCOD; }

    public int getTongTien() { return tongTien; }
    public void setTongTien(int tongTien) { this.tongTien = tongTien; }
}
package com.quanlysinhvien.model;

/**
 * Dòng điểm kèm thông tin môn học (JOIN MonHoc) để hiển thị trên dialog.
 */
public class DiemDetail extends Diem {
    private String tenMH;
    private int soTC;

    public DiemDetail() {
    }

    public DiemDetail(String maSV, String maMH, String tenMH, int soTC,
                      float diemBaoCao, float diemChuyenCan, float diemCuoiKy) {
        super(maSV, maMH, diemBaoCao, diemChuyenCan, diemCuoiKy);
        this.tenMH = tenMH;
        this.soTC = soTC;
    }

    public String getTenMH() {
        return tenMH;
    }

    public void setTenMH(String tenMH) {
        this.tenMH = tenMH;
    }

    public int getSoTC() {
        return soTC;
    }

    public void setSoTC(int soTC) {
        this.soTC = soTC;
    }
}

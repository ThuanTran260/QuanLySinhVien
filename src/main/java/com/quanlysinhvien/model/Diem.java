package com.quanlysinhvien.model;

import java.util.Objects;

/**
 * Một dòng điểm thành phần của sinh viên ở một môn.
 * Điểm môn (40/10/50) luôn tính realtime qua {@link DiemCalculator},
 * không lưu cứng để tránh lệch khi sửa 1 đầu điểm.
 */
public class Diem {
    private String maSV;
    private String maMH;
    private float diemBaoCao;
    private float diemChuyenCan;
    private float diemCuoiKy;

    public Diem() {
    }

    public Diem(String maSV, String maMH, float diemBaoCao, float diemChuyenCan, float diemCuoiKy) {
        this.maSV = maSV;
        this.maMH = maMH;
        this.diemBaoCao = diemBaoCao;
        this.diemChuyenCan = diemChuyenCan;
        this.diemCuoiKy = diemCuoiKy;
    }

    public String getMaSV() {
        return maSV;
    }

    public void setMaSV(String maSV) {
        this.maSV = maSV;
    }

    public String getMaMH() {
        return maMH;
    }

    public void setMaMH(String maMH) {
        this.maMH = maMH;
    }

    public float getDiemBaoCao() {
        return diemBaoCao;
    }

    public void setDiemBaoCao(float diemBaoCao) {
        this.diemBaoCao = diemBaoCao;
    }

    public float getDiemChuyenCan() {
        return diemChuyenCan;
    }

    public void setDiemChuyenCan(float diemChuyenCan) {
        this.diemChuyenCan = diemChuyenCan;
    }

    public float getDiemCuoiKy() {
        return diemCuoiKy;
    }

    public void setDiemCuoiKy(float diemCuoiKy) {
        this.diemCuoiKy = diemCuoiKy;
    }

    /** Điểm môn = BC*0.4 + CC*0.1 + CK*0.5, làm tròn 2 chữ số. */
    public double getDiemMon() {
        return DiemCalculator.diemMon(diemBaoCao, diemChuyenCan, diemCuoiKy);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Diem diem = (Diem) o;
        return Objects.equals(maSV, diem.maSV) && Objects.equals(maMH, diem.maMH);
    }

    @Override
    public int hashCode() {
        return Objects.hash(maSV, maMH);
    }
}

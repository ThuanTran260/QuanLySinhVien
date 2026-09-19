package com.quanlysinhvien.model;

import java.util.Objects;

/**
 * Thực thể môn học (master data, seed từ TKB DCT.md).
 */
public class MonHoc {
    private String maMH;
    private String tenMH;
    private int soTC;

    public MonHoc() {
    }

    public MonHoc(String maMH, String tenMH, int soTC) {
        this.maMH = maMH;
        this.tenMH = tenMH;
        this.soTC = soTC;
    }

    public String getMaMH() {
        return maMH;
    }

    public void setMaMH(String maMH) {
        this.maMH = maMH;
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

    @Override
    public String toString() {
        return maMH + " - " + tenMH;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MonHoc monHoc = (MonHoc) o;
        return Objects.equals(maMH, monHoc.maMH);
    }

    @Override
    public int hashCode() {
        return Objects.hash(maMH);
    }
}

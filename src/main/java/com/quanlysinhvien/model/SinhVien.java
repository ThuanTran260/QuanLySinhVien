package com.quanlysinhvien.model;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.Objects;

/**
 * Thực thể SinhVien đại diện cho một sinh viên trong hệ thống.
 * Tuân thủ mô hình miền (Domain Model) và tài liệu CONTEXT.md.
 */
public class SinhVien {
    public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("uuuu-MM-dd")
            .withResolverStyle(ResolverStyle.STRICT);

    private String maSV;
    private String hoTen;
    private String lop;
    private LocalDate ngaySinh;
    private float diemTB;

    public SinhVien() {
    }

    public SinhVien(String maSV, String hoTen, String lop, LocalDate ngaySinh, float diemTB) {
        this.maSV = maSV;
        this.hoTen = hoTen;
        this.lop = lop;
        this.ngaySinh = ngaySinh;
        this.diemTB = diemTB;
    }

    public String getMaSV() {
        return maSV;
    }

    public void setMaSV(String maSV) {
        this.maSV = maSV;
    }

    public String getHoTen() {
        return hoTen;
    }

    public void setHoTen(String hoTen) {
        this.hoTen = hoTen;
    }

    public String getLop() {
        return lop;
    }

    public void setLop(String lop) {
        this.lop = lop;
    }

    public LocalDate getNgaySinh() {
        return ngaySinh;
    }

    public void setNgaySinh(LocalDate ngaySinh) {
        this.ngaySinh = ngaySinh;
    }

    public String getNgaySinhStr() {
        return ngaySinh != null ? ngaySinh.format(DATE_FORMATTER) : "";
    }

    public float getDiemTB() {
        return diemTB;
    }

    public void setDiemTB(float diemTB) {
        this.diemTB = diemTB;
    }

    /**
     * Lấy Tên (từ cuối cùng trong Họ và Tên) phục vụ sắp xếp chuẩn tiếng Việt.
     * Ví dụ: "Nguyễn Văn An" -> "An", "Trần Thị Bích" -> "Bích".
     */
    public String getTen() {
        if (hoTen == null || hoTen.trim().isEmpty()) {
            return "";
        }
        String trimmed = hoTen.trim();
        int lastSpace = trimmed.lastIndexOf(' ');
        if (lastSpace >= 0) {
            return trimmed.substring(lastSpace + 1);
        }
        return trimmed;
    }

    /**
     * Xếp loại học lực dựa trên Điểm TB
     */
    public String getXepLoai() {
        if (diemTB >= 9.0f) return "Xuất sắc";
        if (diemTB >= 8.0f) return "Giỏi";
        if (diemTB >= 6.5f) return "Khá";
        if (diemTB >= 5.0f) return "Trung bình";
        return "Yếu";
    }

    /**
     * Kiểm tra tính hợp lệ của các dữ liệu đầu vào.
     * Ném ngoại lệ IllegalArgumentException nếu dữ liệu không đúng chuẩn.
     */
    public static SinhVien validateAndCreate(String maSV, String hoTen, String lop, String ngaySinhStr, String diemTBStr) {
        if (maSV == null || maSV.trim().isEmpty()) {
            throw new IllegalArgumentException("Mã sinh viên không được để trống!");
        }
        String cleanMaSV = maSV.trim();
        // Xóa BOM UTF-8 (\uFEFF) nếu có từ file Notepad
        if (cleanMaSV.startsWith("\uFEFF")) {
            cleanMaSV = cleanMaSV.substring(1).trim();
        }
        if (cleanMaSV.isEmpty()) {
            throw new IllegalArgumentException("Mã sinh viên không được để trống!");
        }
        if (cleanMaSV.length() > 10) {
            throw new IllegalArgumentException("Mã sinh viên tối đa 10 ký tự!");
        }

        if (hoTen == null || hoTen.trim().isEmpty()) {
            throw new IllegalArgumentException("Họ và tên không được để trống!");
        }
        String cleanHoTen = hoTen.trim();
        if (cleanHoTen.length() > 50) {
            throw new IllegalArgumentException("Họ và tên tối đa 50 ký tự!");
        }

        if (lop == null || lop.trim().isEmpty()) {
            throw new IllegalArgumentException("Lớp không được để trống!");
        }
        String cleanLop = lop.trim();
        if (cleanLop.length() > 20) {
            throw new IllegalArgumentException("Lớp tối đa 20 ký tự!");
        }

        if (ngaySinhStr == null || ngaySinhStr.trim().isEmpty()) {
            throw new IllegalArgumentException("Ngày sinh không được để trống (định dạng yyyy-MM-dd)!");
        }
        LocalDate date;
        try {
            date = LocalDate.parse(ngaySinhStr.trim(), DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Ngày sinh không đúng định dạng hợp lệ (yyyy-MM-dd), ví dụ: 2003-05-15!");
        }
        if (date.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Ngày sinh không thể lớn hơn ngày hiện tại!");
        }

        if (diemTBStr == null || diemTBStr.trim().isEmpty()) {
            throw new IllegalArgumentException("Điểm trung bình không được để trống!");
        }
        float score;
        try {
            score = Float.parseFloat(diemTBStr.trim().replace(',', '.'));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Điểm trung bình phải là một số thực hợp lệ!");
        }

        if (Float.isNaN(score) || Float.isInfinite(score) || score < 0.0f || score > 10.0f) {
            throw new IllegalArgumentException("Điểm trung bình phải nằm trong khoảng từ 0.0 đến 10.0!");
        }

        return new SinhVien(cleanMaSV, cleanHoTen, cleanLop, date, score);
    }

    /**
     * Chuyển đổi thành dòng văn bản text file theo chuẩn phân tách bằng ký tự |
     */
    public String toFileFormat() {
        return maSV + "|" + hoTen + "|" + lop + "|" + getNgaySinhStr() + "|" + diemTB;
    }

    /**
     * Khởi tạo đối tượng SinhVien từ dòng text file
     */
    public static SinhVien fromFileFormat(String line) {
        if (line == null) {
            return null;
        }
        String trimmed = line.trim();
        // Xóa ký tự BOM UTF-8 (\uFEFF) nếu có
        if (trimmed.startsWith("\uFEFF")) {
            trimmed = trimmed.substring(1).trim();
        }
        // Bỏ qua dòng rỗng hoặc dòng chú thích
        if (trimmed.isEmpty() || trimmed.startsWith("#") || trimmed.startsWith("//")) {
            return null;
        }

        String[] parts = trimmed.split("\\|", -1);
        if (parts.length < 5) {
            throw new IllegalArgumentException("Dòng dữ liệu không đủ 5 trường (MaSV|HoTen|Lop|NgaySinh|DiemTB): " + line);
        }
        return validateAndCreate(parts[0], parts[1], parts[2], parts[3], parts[4]);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SinhVien sinhVien = (SinhVien) o;
        return Objects.equals(maSV, sinhVien.maSV);
    }

    @Override
    public int hashCode() {
        return Objects.hash(maSV);
    }

    @Override
    public String toString() {
        return "SinhVien{" +
                "maSV='" + maSV + '\'' +
                ", hoTen='" + hoTen + '\'' +
                ", lop='" + lop + '\'' +
                ", ngaySinh=" + ngaySinh +
                ", diemTB=" + diemTB +
                '}';
    }
}

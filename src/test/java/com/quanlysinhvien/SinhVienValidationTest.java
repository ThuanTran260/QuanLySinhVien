package com.quanlysinhvien;

import com.quanlysinhvien.model.SinhVien;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

public class SinhVienValidationTest {

    @Test
    @DisplayName("Tạo sinh viên với dữ liệu hợp lệ thành công")
    void testValidSinhVienCreation() {
        SinhVien sv = SinhVien.validateAndCreate("SV001", "Nguyễn Văn An", "CNTT1", "2003-05-15", "8.5");
        assertNotNull(sv);
        assertEquals("SV001", sv.getMaSV());
        assertEquals("Nguyễn Văn An", sv.getHoTen());
        assertEquals("An", sv.getTen());
        assertEquals("CNTT1", sv.getLop());
        assertEquals(LocalDate.of(2003, 5, 15), sv.getNgaySinh());
        assertEquals("2003-05-15", sv.getNgaySinhStr());
        assertEquals(8.5f, sv.getDiemTB(), 0.001f);
        assertEquals("Giỏi", sv.getXepLoai());
    }

    @Test
    @DisplayName("Kiểm tra phân loại học lực chính xác")
    void testXepLoai() {
        SinhVien svXuatSac = SinhVien.validateAndCreate("SV01", "A", "L1", "2003-01-01", "9.5");
        assertEquals("Xuất sắc", svXuatSac.getXepLoai());

        SinhVien svGioi = SinhVien.validateAndCreate("SV02", "B", "L1", "2003-01-01", "8.0");
        assertEquals("Giỏi", svGioi.getXepLoai());

        SinhVien svKha = SinhVien.validateAndCreate("SV03", "C", "L1", "2003-01-01", "6.5");
        assertEquals("Khá", svKha.getXepLoai());

        SinhVien svTB = SinhVien.validateAndCreate("SV04", "D", "L1", "2003-01-01", "5.0");
        assertEquals("Trung bình", svTB.getXepLoai());

        SinhVien svYeu = SinhVien.validateAndCreate("SV05", "E", "L1", "2003-01-01", "4.9");
        assertEquals("Yếu", svYeu.getXepLoai());
    }

    @Test
    @DisplayName("Kiểm tra tách Tên sinh viên chuẩn tiếng Việt")
    void testGetTen() {
        SinhVien sv1 = SinhVien.validateAndCreate("SV01", "Nguyễn Văn An", "L1", "2003-01-01", "8.0");
        assertEquals("An", sv1.getTen());

        SinhVien sv2 = SinhVien.validateAndCreate("SV02", "Đặng Quốc Việt", "L1", "2003-01-01", "8.0");
        assertEquals("Việt", sv2.getTen());

        SinhVien sv3 = SinhVien.validateAndCreate("SV03", "Lê", "L1", "2003-01-01", "8.0");
        assertEquals("Lê", sv3.getTen());
    }

    @Test
    @DisplayName("Mã sinh viên rỗng hoặc vượt quá 10 ký tự phải báo lỗi")
    void testInvalidMaSV() {
        assertThrows(IllegalArgumentException.class, () ->
                SinhVien.validateAndCreate("", "Nguyễn Văn An", "CNTT1", "2003-05-15", "8.5"));

        assertThrows(IllegalArgumentException.class, () ->
                SinhVien.validateAndCreate("MASVQUADAICHUAN10", "Nguyễn Văn An", "CNTT1", "2003-05-15", "8.5"));
    }

    @Test
    @DisplayName("Ngày sinh sai định dạng hoặc lớn hơn ngày hiện tại phải báo lỗi")
    void testInvalidNgaySinh() {
        // Sai định dạng ngày
        assertThrows(IllegalArgumentException.class, () ->
                SinhVien.validateAndCreate("SV001", "Nguyễn Văn An", "CNTT1", "15/05/2003", "8.5"));

        // Ngày không tồn tại (như 30/2)
        assertThrows(IllegalArgumentException.class, () ->
                SinhVien.validateAndCreate("SV001", "Nguyễn Văn An", "CNTT1", "2003-02-30", "8.5"));

        // Ngày tương lai
        assertThrows(IllegalArgumentException.class, () ->
                SinhVien.validateAndCreate("SV001", "Nguyễn Văn An", "CNTT1", "2099-01-01", "8.5"));
    }

    @Test
    @DisplayName("Điểm trung bình ngoài khoảng 0-10 hoặc không phải số phải báo lỗi")
    void testInvalidDiemTB() {
        assertThrows(IllegalArgumentException.class, () ->
                SinhVien.validateAndCreate("SV001", "Nguyễn Văn An", "CNTT1", "2003-05-15", "-1"));

        assertThrows(IllegalArgumentException.class, () ->
                SinhVien.validateAndCreate("SV001", "Nguyễn Văn An", "CNTT1", "2003-05-15", "10.5"));

        assertThrows(IllegalArgumentException.class, () ->
                SinhVien.validateAndCreate("SV001", "Nguyễn Văn An", "CNTT1", "2003-05-15", "abc"));
    }

    @Test
    @DisplayName("Điểm trung bình là NaN hoặc Infinity phải bị từ chối")
    void testRejectNaNAndInfinityScores() {
        assertThrows(IllegalArgumentException.class, () ->
                SinhVien.validateAndCreate("SV001", "Nguyễn Văn An", "CNTT1", "2003-05-15", "NaN"));

        assertThrows(IllegalArgumentException.class, () ->
                SinhVien.validateAndCreate("SV001", "Nguyễn Văn An", "CNTT1", "2003-05-15", "Infinity"));
    }

    @Test
    @DisplayName("Chuyển đổi Text File định dạng chuẩn hoạt động chính xác")
    void testTextFileFormatConversion() {
        SinhVien sv = SinhVien.validateAndCreate("SV001", "Nguyễn Văn An", "CNTT1", "2003-05-15", "8.5");
        String fileLine = sv.toFileFormat();
        assertEquals("SV001|Nguyễn Văn An|CNTT1|2003-05-15|8.5", fileLine);

        SinhVien parsed = SinhVien.fromFileFormat(fileLine);
        assertNotNull(parsed);
        assertEquals(sv.getMaSV(), parsed.getMaSV());
        assertEquals(sv.getHoTen(), parsed.getHoTen());
        assertEquals(sv.getLop(), parsed.getLop());
        assertEquals(sv.getNgaySinh(), parsed.getNgaySinh());
        assertEquals(sv.getDiemTB(), parsed.getDiemTB(), 0.001f);
    }

    @Test
    @DisplayName("Kiểm tra giá trị biên điểm 0.0 và 10.0 cùng định dạng dấu phẩy")
    void testBoundaryScoresAndCommaFormat() {
        SinhVien svMin = SinhVien.validateAndCreate("SV_MIN", "Min Score", "CNTT1", "2003-01-01", "0.0");
        assertEquals(0.0f, svMin.getDiemTB(), 0.001f);

        SinhVien svMax = SinhVien.validateAndCreate("SV_MAX", "Max Score", "CNTT1", "2003-01-01", "10.0");
        assertEquals(10.0f, svMax.getDiemTB(), 0.001f);

        // Chấp nhận cả định dạng dấu phẩy "8,5"
        SinhVien svComma = SinhVien.validateAndCreate("SV_COMMA", "Comma Score", "CNTT1", "2003-01-01", "8,5");
        assertEquals(8.5f, svComma.getDiemTB(), 0.001f);
    }

    @Test
    @DisplayName("Kiểm tra ngày sinh năm nhuận, BOM UTF-8 và bỏ qua chú thích text file")
    void testLeapYearAndBOMAndComments() {
        // 2004 là năm nhuận, ngày 2004-02-29 là hợp lệ
        SinhVien svLeap = SinhVien.validateAndCreate("SV_LEAP", "Leap Year", "CNTT1", "2004-02-29", "8.0");
        assertEquals(LocalDate.of(2004, 2, 29), svLeap.getNgaySinh());

        // File text thiếu trường
        assertThrows(IllegalArgumentException.class, () -> SinhVien.fromFileFormat("SV01|Ten|Lop|2003-01-01"));

        // File text có điểm không hợp lệ
        assertThrows(IllegalArgumentException.class, () -> SinhVien.fromFileFormat("SV01|Ten|Lop|2003-01-01|DiemKhongPhaiSo"));

        // Dòng chú thích hoặc dòng rỗng trả về null
        assertNull(SinhVien.fromFileFormat("# Đây là dòng chú thích"));
        assertNull(SinhVien.fromFileFormat("// Chú thích kiểu java"));
        assertNull(SinhVien.fromFileFormat("   "));

        // Dòng có ký tự BOM UTF-8 (\uFEFF)
        SinhVien svBOM = SinhVien.fromFileFormat("\uFEFFSV099|BOM Test|CNTT1|2003-05-15|8.5");
        assertNotNull(svBOM);
        assertEquals("SV099", svBOM.getMaSV());
    }
}

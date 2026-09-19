package com.quanlysinhvien;

import com.quanlysinhvien.dao.DiemDAO;
import com.quanlysinhvien.dao.MonHocDAO;
import com.quanlysinhvien.model.Diem;
import com.quanlysinhvien.model.DiemDetail;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.sql.Connection;
import java.util.List;

import com.quanlysinhvien.dao.DatabaseConnection;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class DiemDAOTest {
    private static boolean dbAvailable = false;
    private static String dbError = "";
    private static final MonHocDAO monHocDAO = new MonHocDAO();
    private static final DiemDAO diemDAO = new DiemDAO();

    @BeforeAll
    static void setUp() {
        try {
            DatabaseConnection.initializeDatabase();
            try (Connection conn = DatabaseConnection.getConnection()) {
                dbAvailable = (conn != null && !conn.isClosed());
            }
        } catch (Throwable t) {
            dbAvailable = false;
            dbError = String.valueOf(t.getMessage());
        }
    }

    @Test
    @Order(1)
    @DisplayName("Seed đủ 37 môn chuyên ngành")
    void testMonHocSeeded() {
        Assumptions.assumeTrue(dbAvailable, "Bỏ qua do MySQL không khả dụng: " + dbError);
        try {
            assertEquals(37, monHocDAO.count(), "Phải seed đủ 37 môn");
            assertEquals(37, monHocDAO.getAll().size());
        } catch (Exception e) {
            fail("Lỗi truy vấn MonHoc: " + e.getMessage());
        }
    }

    @Test
    @Order(2)
    @DisplayName("Mỗi SV có 4-7 môn sau seed, TB tích lũy 0-10")
    void testSeededDiemRange() {
        Assumptions.assumeTrue(dbAvailable, "Bỏ qua do MySQL không khả dụng: " + dbError);
        try {
            diemDAO.seedIfEmpty();
            int total = diemDAO.countAll();
            assertTrue(total >= 84 * 4, "Tối thiểu 84 SV x 4 môn, thực tế: " + total);
            // Kiểm tra 5 SV đầu
            List<DiemDetail> sample = diemDAO.getByMaSV("3124410003");
            assertTrue(sample.size() >= 4 && sample.size() <= 7,
                    "SV phải có 4-7 môn, thực tế: " + sample.size());
            double tb = diemDAO.calcTichLuy("3124410003");
            assertTrue(tb >= 0 && tb <= 10, "TB phải 0-10, thực tế: " + tb);
        } catch (Exception e) {
            fail("Lỗi seed/kiểm tra Diem: " + e.getMessage());
        }
    }

    @Test
    @Order(4)
    @DisplayName("saveBangDiem 1 transaction: giữ nguyên số dòng + cập nhật DiemTB")
    void testSaveBangDiemSingleTransaction() {
        Assumptions.assumeTrue(dbAvailable, "Bỏ qua do MySQL không khả dụng: " + dbError);
        try {
            List<DiemDetail> current = diemDAO.getByMaSV("3124410003");
            Assumptions.assumeFalse(current.isEmpty(), "SV mẫu chưa có điểm seed");
            java.util.List<com.quanlysinhvien.model.Diem> toSave = new java.util.ArrayList<>();
            java.util.List<Double> mons = new java.util.ArrayList<>();
            java.util.List<Integer> tcs = new java.util.ArrayList<>();
            for (DiemDetail d : current) {
                toSave.add(new Diem(d.getMaSV(), d.getMaMH(),
                        d.getDiemBaoCao(), d.getDiemChuyenCan(), d.getDiemCuoiKy()));
                mons.add(d.getDiemMon());
                tcs.add(d.getSoTC());
            }
            double tb = com.quanlysinhvien.model.DiemCalculator.diemTBTinChi(mons, tcs);
            int countBefore = diemDAO.countAll();
            diemDAO.saveBangDiem("3124410003", toSave, tb);
            assertEquals(countBefore, diemDAO.countAll(), "Lưu lại y nguyên không được đổi tổng dòng");
            assertEquals(tb, diemDAO.calcTichLuy("3124410003"), 0.001, "TB sau lưu phải khớp");
            // Bảng rỗng -> từ chối, không ghi gì
            assertThrows(IllegalArgumentException.class,
                    () -> diemDAO.saveBangDiem("3124410003", new java.util.ArrayList<>(), 0));
        } catch (IllegalArgumentException iae) {
            throw iae;
        } catch (Exception e) {
            fail("Lỗi saveBangDiem: " + e.getMessage());
        }
    }

    @Test
    @Order(3)
    @DisplayName("Upsert idempotent + từ chối điểm ngoài 0-10")
    void testUpsertAndValidation() {
        Assumptions.assumeTrue(dbAvailable, "Bỏ qua do MySQL không khả dụng: " + dbError);
        try {
            List<DiemDetail> before = diemDAO.getByMaSV("3124410003");
            Assumptions.assumeFalse(before.isEmpty(), "SV mẫu chưa có điểm seed");
            DiemDetail first = before.get(0);
            int countBefore = diemDAO.countAll();
            // Upsert cùng khóa -> không tăng tổng dòng
            diemDAO.upsert(new Diem("3124410003", first.getMaMH(), 8f, 9f, 7f));
            assertEquals(countBefore, diemDAO.countAll(), "Upsert trùng khóa không được tăng dòng");
            // Điểm sai -> IllegalArgumentException, không chạm DB
            assertThrows(IllegalArgumentException.class,
                    () -> diemDAO.upsert(new Diem("3124410003", first.getMaMH(), 11f, 9f, 7f)));
        } catch (IllegalArgumentException iae) {
            throw iae;
        } catch (Exception e) {
            fail("Lỗi upsert Diem: " + e.getMessage());
        }
    }

    @Test
    @Order(5)
    @DisplayName("syncTichLuyAll đồng bộ GPA toàn bộ sinh viên khớp với calcTichLuy")
    void testSyncTichLuyAll() {
        Assumptions.assumeTrue(dbAvailable, "Bỏ qua do MySQL không khả dụng: " + dbError);
        try {
            diemDAO.syncTichLuyAll();
            double tb = diemDAO.calcTichLuy("3124410003");
            try (Connection conn = DatabaseConnection.getConnection();
                 java.sql.PreparedStatement ps = conn.prepareStatement("SELECT DiemTB FROM SinhVien WHERE MaSV = ?")) {
                ps.setString(1, "3124410003");
                try (java.sql.ResultSet rs = ps.executeQuery()) {
                    assertTrue(rs.next(), "Phải tìm thấy sinh viên");
                    float gpaInDb = rs.getFloat(1);
                    assertEquals((float) tb, gpaInDb, 0.05f, "DiemTB trong CSDL phải khớp calcTichLuy");
                }
            }
        } catch (Exception e) {
            fail("Lỗi syncTichLuyAll: " + e.getMessage());
        }
    }

    @Test
    @Order(6)
    @DisplayName("saveBangDiem từ chối danh sách chứa môn học trùng lặp")
    void testSaveBangDiemRejectsDuplicateSubjects() {
        Assumptions.assumeTrue(dbAvailable, "Bỏ qua do MySQL không khả dụng: " + dbError);
        java.util.List<com.quanlysinhvien.model.Diem> duplicates = java.util.Arrays.asList(
                new Diem("3124410003", "841021", 8f, 8f, 8f),
                new Diem("3124410003", "841021", 9f, 9f, 9f)
        );
        assertThrows(IllegalArgumentException.class,
                () -> diemDAO.saveBangDiem("3124410003", duplicates, 8.5));
    }

    @Test
    @Order(7)
    @DisplayName("reseedDiverseGrades phân bổ đủ 5 bậc học lực: Xuất sắc, Giỏi, Khá, TB, Yếu")
    void testReseedDiverseGradesFullTiers() {
        Assumptions.assumeTrue(dbAvailable, "Bỏ qua do MySQL không khả dụng: " + dbError);
        try {
            diemDAO.reseedDiverseGrades();
            assertFalse(diemDAO.hasNarrowVariance(), "Dữ liệu sau khi reseed không được coi là bị hẹp phương sai");

            int countXuatSac = 0;
            int countGioi = 0;
            int countKha = 0;
            int countTB = 0;
            int countYeu = 0;
            int totalStudents = 0;

            try (Connection conn = DatabaseConnection.getConnection();
                 java.sql.Statement st = conn.createStatement();
                 java.sql.ResultSet rs = st.executeQuery("SELECT MaSV, DiemTB FROM SinhVien")) {
                while (rs.next()) {
                    totalStudents++;
                    String maSV = rs.getString(1);
                    float gpa = rs.getFloat(2);

                    if (gpa >= 9.0f) {
                        countXuatSac++;
                    } else if (gpa >= 8.0f) {
                        countGioi++;
                    } else if (gpa >= 6.5f) {
                        countKha++;
                    } else if (gpa >= 5.0f) {
                        countTB++;
                    } else {
                        countYeu++;
                    }

                    // Điểm TB trên SinhVien phải khớp với calcTichLuy từ Diem
                    double calcGpa = diemDAO.calcTichLuy(maSV);
                    assertEquals((float) calcGpa, gpa, 0.05f, "DiemTB của " + maSV + " phải khớp calcTichLuy");
                }
            }

            assertEquals(84, totalStudents, "Phải có đúng 84 sinh viên");
            assertTrue(countXuatSac >= 3, "Phải có sinh viên Xuất sắc (>= 9.0), thực tế: " + countXuatSac);
            assertTrue(countGioi >= 10, "Phải có sinh viên Giỏi (8.0 - 8.9), thực tế: " + countGioi);
            assertTrue(countKha >= 20, "Phải có sinh viên Khá (6.5 - 7.9), thực tế: " + countKha);
            assertTrue(countTB >= 10, "Phải có sinh viên Trung bình (5.0 - 6.4), thực tế: " + countTB);
            assertTrue(countYeu >= 3, "Phải có sinh viên Yếu (< 5.0, 3.2 - 4.9), thực tế: " + countYeu);
        } catch (Exception e) {
            fail("Lỗi reseedDiverseGrades: " + e.getMessage());
        }
    }

    @Test
    @Order(8)
    @DisplayName("Mỗi sinh viên được gán 4-7 môn và các điểm thành phần hợp lệ trong khoảng [0, 10]")
    void testSubjectCountPerStudentRange() {
        Assumptions.assumeTrue(dbAvailable, "Bỏ qua do MySQL không khả dụng: " + dbError);
        try {
            try (Connection conn = DatabaseConnection.getConnection();
                 java.sql.Statement st = conn.createStatement();
                 java.sql.ResultSet rs = st.executeQuery("SELECT MaSV FROM SinhVien ORDER BY MaSV ASC")) {
                while (rs.next()) {
                    String maSV = rs.getString(1);
                    List<DiemDetail> list = diemDAO.getByMaSV(maSV);
                    assertTrue(list.size() >= 4 && list.size() <= 7,
                            "Sinh viên " + maSV + " phải có từ 4 đến 7 môn, thực tế: " + list.size());
                    for (DiemDetail d : list) {
                        assertTrue(d.getDiemBaoCao() >= 0.0f && d.getDiemBaoCao() <= 10.0f);
                        assertTrue(d.getDiemChuyenCan() >= 0.0f && d.getDiemChuyenCan() <= 10.0f);
                        assertTrue(d.getDiemCuoiKy() >= 0.0f && d.getDiemCuoiKy() <= 10.0f);
                        assertTrue(d.getDiemMon() >= 0.0 && d.getDiemMon() <= 10.0);
                    }
                }
            }
        } catch (Exception e) {
            fail("Lỗi kiểm tra số môn và điểm thành phần: " + e.getMessage());
        }
    }

    @Test
    @Order(9)
    @DisplayName("hasNarrowVariance phát hiện >85% Khá nhưng không xóa nhầm khi lớp toàn sinh viên Giỏi")
    void testHasNarrowVarianceLogic() {
        Assumptions.assumeTrue(dbAvailable, "Bỏ qua do MySQL không khả dụng: " + dbError);
        try (Connection conn = DatabaseConnection.getConnection()) {
            // 1. Giả lập toàn bộ sinh viên có điểm Khá (7.0) -> phương sai hẹp >85%
            try (java.sql.PreparedStatement ps = conn.prepareStatement("UPDATE SinhVien SET DiemTB = 7.0")) {
                ps.executeUpdate();
            }
            assertTrue(diemDAO.hasNarrowVariance(), "Phải phát hiện phương sai hẹp khi 100% sinh viên điểm Khá");

            // 2. Giả lập một lớp học giỏi (100% Giỏi 8.5, không có Xuất sắc, không có Yếu)
            // CSDL không được coi đây là phương sai hẹp để tránh xóa sạch điểm của người dùng!
            try (java.sql.PreparedStatement ps = conn.prepareStatement("UPDATE SinhVien SET DiemTB = 8.5")) {
                ps.executeUpdate();
            }
            assertFalse(diemDAO.hasNarrowVariance(), "Lớp toàn Giỏi không được coi là phương sai hẹp (>85% Khá)");

            // 3. Khôi phục lại dữ liệu điểm đa dạng
            diemDAO.reseedDiverseGrades();
            assertFalse(diemDAO.hasNarrowVariance(), "Sau khi reseed đa dạng thì hasNarrowVariance phải là false");
        } catch (Exception e) {
            fail("Lỗi kiểm tra logic hasNarrowVariance: " + e.getMessage());
        }
    }
}

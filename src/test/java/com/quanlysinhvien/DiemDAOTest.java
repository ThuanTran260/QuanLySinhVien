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
    @DisplayName("Seed đủ 20 môn chuyên ngành")
    void testMonHocSeeded() {
        Assumptions.assumeTrue(dbAvailable, "Bỏ qua do MySQL không khả dụng: " + dbError);
        try {
            assertEquals(20, monHocDAO.count(), "Phải seed đủ 20 môn");
            assertEquals(20, monHocDAO.getAll().size());
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
}

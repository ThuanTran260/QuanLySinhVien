package com.quanlysinhvien;

import com.quanlysinhvien.dao.DatabaseConnection;
import com.quanlysinhvien.dao.SinhVienDAO;
import com.quanlysinhvien.model.SinhVien;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class SinhVienDAOTest {
    private static SinhVienDAO dao;

    @BeforeAll
    static void setUp() {
        DatabaseConnection.initializeDatabase();
        dao = new SinhVienDAO();
    }

    @Test
    @Order(1)
    @DisplayName("Kiểm tra kết nối tới MySQL Database thành công")
    void testDatabaseConnection() throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            assertNotNull(conn);
            assertFalse(conn.isClosed());
        }
    }

    @Test
    @Order(2)
    @DisplayName("Lấy danh sách tất cả sinh viên (ít nhất 10 bản ghi mẫu)")
    void testGetAllStudents() throws SQLException {
        List<SinhVien> list = dao.getAll();
        assertNotNull(list);
        assertTrue(list.size() >= 10, "CSDL phải có ít nhất 10 bản ghi mẫu");
    }

    @Test
    @Order(3)
    @DisplayName("Thêm, tìm kiếm, cập nhật và xóa sinh viên")
    void testCRUDLifecycle() throws SQLException {
        String testMaSV = "TEST999";
        // Đảm bảo không tồn tại trước khi thêm
        if (dao.existsById(testMaSV)) {
            dao.delete(testMaSV);
        }

        // 1. Thêm sinh viên mới
        SinhVien newSv = SinhVien.validateAndCreate(testMaSV, "Kiểm Thử Viên", "CNTT1", "2003-09-09", "9.0");
        boolean inserted = dao.insert(newSv);
        assertTrue(inserted);
        assertTrue(dao.existsById(testMaSV));

        // 2. Lấy theo ID
        SinhVien fetched = dao.getById(testMaSV);
        assertNotNull(fetched);
        assertEquals("Kiểm Thử Viên", fetched.getHoTen());

        // 3. Cập nhật sinh viên
        fetched.setHoTen("Kiểm Thử Viên Đã Sửa");
        fetched.setDiemTB(9.8f);
        boolean updated = dao.update(fetched);
        assertTrue(updated);

        SinhVien updatedSv = dao.getById(testMaSV);
        assertEquals("Kiểm Thử Viên Đã Sửa", updatedSv.getHoTen());
        assertEquals(9.8f, updatedSv.getDiemTB(), 0.001f);

        // 4. Tìm kiếm
        List<SinhVien> searchResults = dao.search("Đã Sửa", "Họ tên");
        assertFalse(searchResults.isEmpty());
        assertEquals(testMaSV, searchResults.get(0).getMaSV());

        // 5. Kiểm tra không cho phép thêm trùng khóa chính
        assertThrows(IllegalArgumentException.class, () -> dao.insert(newSv));

        // 6. Xóa sinh viên
        boolean deleted = dao.delete(testMaSV);
        assertTrue(deleted);
        assertFalse(dao.existsById(testMaSV));
    }

    @Test
    @Order(4)
    @DisplayName("Kiểm tra chức năng Sắp xếp theo Tên và Điểm TB")
    void testSorting() throws SQLException {
        // 1. Sắp xếp theo Tên (danh sách thực tế 84 SV: Anh ... Vỹ)
        List<SinhVien> sortedByNameAsc = dao.getAllSorted("TEN", true);
        assertFalse(sortedByNameAsc.isEmpty());
        assertEquals("Anh", sortedByNameAsc.get(0).getTen(), "Người đầu tiên khi sắp xếp theo Tên phải là Anh");
        assertEquals("Vỹ", sortedByNameAsc.get(sortedByNameAsc.size() - 1).getTen(), "Người cuối cùng phải là Vỹ");

        // 2. Sắp xếp theo Họ và Tên đầy đủ
        List<SinhVien> sortedByFullNameAsc = dao.getAllSorted("HOTEN", true);
        assertFalse(sortedByFullNameAsc.isEmpty());
        java.text.Collator viCollator = java.text.Collator.getInstance(java.util.Locale.forLanguageTag("vi-VN"));
        for (int i = 0; i < sortedByFullNameAsc.size() - 1; i++) {
            assertTrue(viCollator.compare(sortedByFullNameAsc.get(i).getHoTen(), sortedByFullNameAsc.get(i + 1).getHoTen()) <= 0);
        }

        // 3. Sắp xếp theo Điểm TB giảm dần
        List<SinhVien> sortedByDiemDesc = dao.getAllSorted("DIEM", false);
        assertFalse(sortedByDiemDesc.isEmpty());
        for (int i = 0; i < sortedByDiemDesc.size() - 1; i++) {
            assertTrue(sortedByDiemDesc.get(i).getDiemTB() >= sortedByDiemDesc.get(i + 1).getDiemTB());
        }
    }

    @Test
    @Order(5)
    @DisplayName("Kiểm tra chức năng Thống kê (Số lượng, Điểm TB theo lớp, Thủ khoa)")
    void testStatistics() throws SQLException {
        int count = dao.getCount();
        assertTrue(count >= 10);

        Map<String, Double> avgMap = dao.getAverageScoreByClass();
        assertNotNull(avgMap);
        assertFalse(avgMap.isEmpty());

        List<SinhVien> valedictorians = dao.getValedictorians();
        assertNotNull(valedictorians);
        assertFalse(valedictorians.isEmpty());
        // Điểm của thủ khoa phải >= tất cả sinh viên
        float topScore = valedictorians.get(0).getDiemTB();
        List<SinhVien> all = dao.getAll();
        for (SinhVien sv : all) {
            assertTrue(sv.getDiemTB() <= topScore);
        }
    }

    @Test
    @Order(6)
    @DisplayName("Kiểm tra Xuất và Nạp dữ liệu Text File (Bài 8)")
    void testFileExportAndImport() throws Exception {
        List<SinhVien> originalList = dao.getAll();
        File tempFile = File.createTempFile("test_sinhvien", ".txt");
        tempFile.deleteOnExit();

        // Xuất file
        dao.exportToFile(tempFile, originalList);
        assertTrue(tempFile.exists());
        assertTrue(tempFile.length() > 0);

        // Nạp lại file
        List<SinhVien> importedList = dao.importFromFile(tempFile);
        assertEquals(originalList.size(), importedList.size());
        assertEquals(originalList.get(0).getMaSV(), importedList.get(0).getMaSV());
        assertEquals(originalList.get(0).getHoTen(), importedList.get(0).getHoTen());
    }
}

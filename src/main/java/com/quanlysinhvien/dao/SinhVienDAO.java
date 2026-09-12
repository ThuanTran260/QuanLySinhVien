package com.quanlysinhvien.dao;

import com.quanlysinhvien.model.SinhVien;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Data Access Object (DAO) thực hiện các thao tác CRUD và nghiệp vụ trên CSDL MySQL
 * cũng như đọc / ghi Text File độc lập.
 */
public class SinhVienDAO {

    /**
     * Khởi tạo CSDL nếu chưa có bảng
     */
    public SinhVienDAO() {
        DatabaseConnection.initializeDatabase();
    }

    /**
     * Lấy toàn bộ danh sách sinh viên từ CSDL
     */
    public List<SinhVien> getAll() throws SQLException {
        List<SinhVien> list = new ArrayList<>();
        String sql = "SELECT MaSV, HoTen, Lop, NgaySinh, DiemTB FROM SinhVien ORDER BY MaSV ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapResultSetToSinhVien(rs));
            }
        }
        return list;
    }

    /**
     * Lấy thông tin một sinh viên theo Mã SV
     */
    public SinhVien getById(String maSV) throws SQLException {
        String sql = "SELECT MaSV, HoTen, Lop, NgaySinh, DiemTB FROM SinhVien WHERE MaSV = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, maSV.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToSinhVien(rs);
                }
            }
        }
        return null;
    }

    /**
     * Kiểm tra xem Mã SV đã tồn tại trong CSDL hay chưa
     */
    public boolean existsById(String maSV) throws SQLException {
        String sql = "SELECT 1 FROM SinhVien WHERE MaSV = ? LIMIT 1";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, maSV.trim());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /**
     * Thêm sinh viên mới vào CSDL
     */
    public boolean insert(SinhVien sv) throws SQLException {
        if (existsById(sv.getMaSV())) {
            throw new IllegalArgumentException("Mã sinh viên '" + sv.getMaSV() + "' đã tồn tại trong hệ thống!");
        }

        String sql = "INSERT INTO SinhVien (MaSV, HoTen, Lop, NgaySinh, DiemTB) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, sv.getMaSV());
            ps.setString(2, sv.getHoTen());
            ps.setString(3, sv.getLop());
            ps.setDate(4, Date.valueOf(sv.getNgaySinh()));
            ps.setFloat(5, sv.getDiemTB());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            if (e.getErrorCode() == 1062) {
                throw new IllegalArgumentException("Mã sinh viên '" + sv.getMaSV() + "' đã tồn tại trong hệ thống!", e);
            }
            throw e;
        }
    }

    /**
     * Cập nhật thông tin sinh viên (khóa chính MaSV không thay đổi)
     */
    public boolean update(SinhVien sv) throws SQLException {
        String sql = "UPDATE SinhVien SET HoTen = ?, Lop = ?, NgaySinh = ?, DiemTB = ? WHERE MaSV = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, sv.getHoTen());
            ps.setString(2, sv.getLop());
            ps.setDate(3, Date.valueOf(sv.getNgaySinh()));
            ps.setFloat(4, sv.getDiemTB());
            ps.setString(5, sv.getMaSV());
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Xóa sinh viên theo Mã SV
     */
    public boolean delete(String maSV) throws SQLException {
        String sql = "DELETE FROM SinhVien WHERE MaSV = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, maSV.trim());
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Tìm kiếm sinh viên theo tiêu chí (Mã SV, Họ tên, Lớp hoặc Tất cả)
     */
    public List<SinhVien> search(String keyword, String criteria) throws SQLException {
        List<SinhVien> list = new ArrayList<>();
        if (keyword == null || keyword.trim().isEmpty()) {
            return getAll();
        }

        String pattern = "%" + keyword.trim() + "%";
        String sql;
        if ("Mã SV".equalsIgnoreCase(criteria)) {
            sql = "SELECT MaSV, HoTen, Lop, NgaySinh, DiemTB FROM SinhVien WHERE MaSV LIKE ? ORDER BY MaSV ASC";
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, pattern);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) list.add(mapResultSetToSinhVien(rs));
                }
            }
        } else if ("Họ tên".equalsIgnoreCase(criteria)) {
            sql = "SELECT MaSV, HoTen, Lop, NgaySinh, DiemTB FROM SinhVien WHERE HoTen LIKE ? ORDER BY MaSV ASC";
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, pattern);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) list.add(mapResultSetToSinhVien(rs));
                }
            }
        } else if ("Lớp".equalsIgnoreCase(criteria)) {
            sql = "SELECT MaSV, HoTen, Lop, NgaySinh, DiemTB FROM SinhVien WHERE Lop LIKE ? ORDER BY MaSV ASC";
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, pattern);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) list.add(mapResultSetToSinhVien(rs));
                }
            }
        } else {
            // Mặc định tìm kiếm tất cả các trường
            sql = "SELECT MaSV, HoTen, Lop, NgaySinh, DiemTB FROM SinhVien " +
                    "WHERE MaSV LIKE ? OR HoTen LIKE ? OR Lop LIKE ? ORDER BY MaSV ASC";
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, pattern);
                ps.setString(2, pattern);
                ps.setString(3, pattern);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) list.add(mapResultSetToSinhVien(rs));
                }
            }
        }
        return list;
    }

    /**
     * Lấy danh sách sắp xếp theo trường mong muốn
     * @param sortBy "TEN" hoặc "DIEM" hoặc "MASV"
     * @param ascending true: tăng dần (ASC), false: giảm dần (DESC)
     */
    public List<SinhVien> getAllSorted(String sortBy, boolean ascending) throws SQLException {
        List<SinhVien> list = new ArrayList<>();
        String direction = ascending ? "ASC" : "DESC";
        String sql;

        if ("TEN".equalsIgnoreCase(sortBy)) {
            // Sắp xếp theo Tên (từ cuối cùng của Họ và Tên theo chuẩn tiếng Việt)
            sql = "SELECT MaSV, HoTen, Lop, NgaySinh, DiemTB FROM SinhVien " +
                    "ORDER BY SUBSTRING_INDEX(HoTen, ' ', -1) COLLATE utf8mb4_vietnamese_ci " + direction + ", HoTen COLLATE utf8mb4_vietnamese_ci " + direction;
        } else if ("HOTEN".equalsIgnoreCase(sortBy)) {
            sql = "SELECT MaSV, HoTen, Lop, NgaySinh, DiemTB FROM SinhVien ORDER BY HoTen COLLATE utf8mb4_vietnamese_ci " + direction;
        } else if ("DIEM".equalsIgnoreCase(sortBy)) {
            sql = "SELECT MaSV, HoTen, Lop, NgaySinh, DiemTB FROM SinhVien ORDER BY DiemTB " + direction;
        } else if ("LOP".equalsIgnoreCase(sortBy)) {
            sql = "SELECT MaSV, HoTen, Lop, NgaySinh, DiemTB FROM SinhVien " +
                    "ORDER BY Lop " + direction + ", SUBSTRING_INDEX(HoTen, ' ', -1) COLLATE utf8mb4_vietnamese_ci ASC";
        } else {
            sql = "SELECT MaSV, HoTen, Lop, NgaySinh, DiemTB FROM SinhVien ORDER BY MaSV " + direction;
        }

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapResultSetToSinhVien(rs));
            }
        }
        return list;
    }

    /**
     * Thống kê: Lấy tổng số lượng sinh viên
     */
    public int getCount() throws SQLException {
        String sql = "SELECT COUNT(*) FROM SinhVien";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }

    /**
     * Thống kê: Tính điểm trung bình theo từng lớp
     * Trả về Map với key là tên Lớp, value là Điểm TB làm tròn 2 chữ số thập phân
     */
    public Map<String, Double> getAverageScoreByClass() throws SQLException {
        Map<String, Double> map = new LinkedHashMap<>();
        String sql = "SELECT Lop, ROUND(AVG(DiemTB), 2) AS DiemTBLop FROM SinhVien GROUP BY Lop ORDER BY Lop ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                map.put(rs.getString("Lop"), rs.getDouble("DiemTBLop"));
            }
        }
        return map;
    }

    /**
     * Thống kê: Lấy danh sách sinh viên có điểm trung bình cao nhất (Thủ khoa)
     */
    public List<SinhVien> getValedictorians() throws SQLException {
        List<SinhVien> list = new ArrayList<>();
        String sql = "SELECT MaSV, HoTen, Lop, NgaySinh, DiemTB FROM SinhVien " +
                "WHERE DiemTB = (SELECT MAX(DiemTB) FROM SinhVien)";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapResultSetToSinhVien(rs));
            }
        }
        return list;
    }

    // ==========================================
    // CÁC PHƯƠNG THỨC XỬ LÝ TEXT FILE (BÀI 8)
    // ==========================================

    /**
     * Xuất danh sách sinh viên ra Text File (mã hóa UTF-8)
     */
    public void exportToFile(File file, List<SinhVien> list) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(file), StandardCharsets.UTF_8))) {
            for (SinhVien sv : list) {
                writer.write(sv.toFileFormat());
                writer.newLine();
            }
        }
    }

    /**
     * Nạp danh sách sinh viên từ Text File (mã hóa UTF-8)
     */
    public List<SinhVien> importFromFile(File file) throws IOException {
        List<SinhVien> list = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new FileInputStream(file), StandardCharsets.UTF_8))) {
            String line;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                String trimmed = line.trim();
                if (lineNumber == 1 && trimmed.startsWith("\uFEFF")) {
                    trimmed = trimmed.substring(1).trim();
                }
                if (trimmed.isEmpty() || trimmed.startsWith("#") || trimmed.startsWith("//")) {
                    continue;
                }
                try {
                    SinhVien sv = SinhVien.fromFileFormat(trimmed);
                    if (sv != null) {
                        list.add(sv);
                    }
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException("Lỗi dòng " + lineNumber + ": " + e.getMessage(), e);
                }
            }
        }
        return list;
    }

    private SinhVien mapResultSetToSinhVien(ResultSet rs) throws SQLException {
        Date date = rs.getDate("NgaySinh");
        return new SinhVien(
                rs.getString("MaSV"),
                rs.getString("HoTen"),
                rs.getString("Lop"),
                date != null ? date.toLocalDate() : null,
                rs.getFloat("DiemTB")
        );
    }
}

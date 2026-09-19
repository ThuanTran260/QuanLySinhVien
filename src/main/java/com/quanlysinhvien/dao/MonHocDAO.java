package com.quanlysinhvien.dao;

import com.quanlysinhvien.model.MonHoc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Master data môn học (seed 1 lần từ TKB DCT.md).
 */
public class MonHocDAO {

    /** Danh sách môn chuyên ngành seed cố định (mã thật trong DCT.md). */
    public static List<MonHoc> seedData() {
        List<MonHoc> list = new ArrayList<>();
        list.add(new MonHoc("841021", "Kiến trúc máy tính", 3));
        list.add(new MonHoc("841044", "Phương pháp lập trình hướng đối tượng", 4));
        list.add(new MonHoc("841047", "Công nghệ phần mềm", 4));
        list.add(new MonHoc("841070", "Thực tập tốt nghiệp DCT", 6));
        list.add(new MonHoc("841072", "Các công nghệ lập trình hiện đại", 3));
        list.add(new MonHoc("841111", "Phân tích thiết kế hướng đối tượng", 4));
        list.add(new MonHoc("841120", "An toàn và bảo mật dữ liệu trong HTTT", 3));
        list.add(new MonHoc("841302", "Cơ sở lập trình", 4));
        list.add(new MonHoc("841322", "Máy học", 4));
        list.add(new MonHoc("841403", "Cấu trúc rời rạc", 4));
        list.add(new MonHoc("841408", "Kiểm thử phần mềm", 4));
        list.add(new MonHoc("841411", "Quản trị mạng", 4));
        list.add(new MonHoc("841417", "Mỹ thuật ứng dụng trong CNTT", 2));
        list.add(new MonHoc("841422", "Ngôn ngữ lập trình Python", 4));
        list.add(new MonHoc("841429", "Cơ sở dữ liệu nâng cao", 4));
        list.add(new MonHoc("841431", "Quản lý dự án phần mềm", 4));
        list.add(new MonHoc("841432", "Phân tích dữ liệu", 4));
        list.add(new MonHoc("841434", "Thương mại điện tử và ứng dụng", 4));
        list.add(new MonHoc("841438", "Lập trình ứng dụng mạng", 4));
        list.add(new MonHoc("841443", "Phân tích mạng truyền thông xã hội", 3));
        list.add(new MonHoc("841444", "Quản trị và bảo trì hệ thống", 3));
        list.add(new MonHoc("841445", "Hệ thống ảo và khả năng mở rộng dữ liệu", 3));
        list.add(new MonHoc("841447", "Khai phá dữ liệu và ứng dụng", 4));
        list.add(new MonHoc("841448", "Xử lý ngôn ngữ tự nhiên", 4));
        list.add(new MonHoc("841452", "Tính toán thông minh", 3));
        list.add(new MonHoc("841453", "Phân tích và nhận dạng mẫu", 4));
        list.add(new MonHoc("841457", "Học sâu", 4));
        list.add(new MonHoc("841458", "Trí tuệ nhân tạo nâng cao", 4));
        list.add(new MonHoc("841467", "Công nghệ .NET", 4));
        list.add(new MonHoc("841468", "Chuyên đề J2EE", 4));
        list.add(new MonHoc("841476", "Đồ án chuyên ngành (ngành CNTT, KTPM)", 4));
        list.add(new MonHoc("841479", "Kiến trúc phần mềm", 4));
        list.add(new MonHoc("841482", "Seminar chuyên đề (ngành CNTT, KTPM)", 3));
        list.add(new MonHoc("841501", "Nhập môn công nghệ thông tin và truyền thông", 3));
        list.add(new MonHoc("864005", "Giải tích 1", 3));
        list.add(new MonHoc("864007", "Đại số tuyến tính", 3));
        list.add(new MonHoc("872518", "Lý thuyết đồ thị", 4));
        return list;
    }

    public List<MonHoc> getAll() throws SQLException {
        List<MonHoc> list = new ArrayList<>();
        String sql = "SELECT MaMH, TenMH, SoTC FROM MonHoc ORDER BY MaMH ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                list.add(new MonHoc(rs.getString("MaMH"), rs.getString("TenMH"), rs.getInt("SoTC")));
            }
        }
        return list;
    }

    public int count() throws SQLException {
        String sql = "SELECT COUNT(*) FROM MonHoc";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }

    /** Seed 1 lần, dùng INSERT IGNORE để không đè khi chạy lại. */
    public void ensureSeeded() throws SQLException {
        String sql = "INSERT IGNORE INTO MonHoc (MaMH, TenMH, SoTC) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            for (MonHoc mh : seedData()) {
                ps.setString(1, mh.getMaMH());
                ps.setString(2, mh.getTenMH());
                ps.setInt(3, mh.getSoTC());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }
}

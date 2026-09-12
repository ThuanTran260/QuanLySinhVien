package com.quanlysinhvien.dao;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

/**
 * Quản lý kết nối JDBC tới cơ sở dữ liệu MySQL.
 * Tự động đọc cấu hình từ file db.properties và có cơ chế dự phòng an toàn.
 */
public class DatabaseConnection {
    private static final String DEFAULT_HOST = "localhost:3306";
    private static final String DB_NAME = "QuanLySinhVien";
    private static final String DEFAULT_URL = "jdbc:mysql://" + DEFAULT_HOST + "/" + DB_NAME +
            "?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Ho_Chi_Minh&allowPublicKeyRetrieval=true&useSSL=false";

    private static String dbUrl = DEFAULT_URL;
    private static String dbUser = "root";
    private static String dbPassword = "";

    /**
     * Lấy URL kết nối root (không có tên database) dựa trên dbUrl cấu hình
     */
    public static String getRootUrl() {
        if (dbUrl != null && dbUrl.contains("/" + DB_NAME)) {
            return dbUrl.replace("/" + DB_NAME, "");
        }
        return "jdbc:mysql://" + DEFAULT_HOST +
                "?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Ho_Chi_Minh&allowPublicKeyRetrieval=true&useSSL=false";
    }

    static {
        loadConfiguration();
    }

    private static void loadConfiguration() {
        Properties props = new Properties();
        boolean loaded = false;

        // Ưu tiên 1: Đọc từ file db.properties ở thư mục làm việc hiện tại
        File externalFile = new File("db.properties");
        if (externalFile.exists() && externalFile.isFile()) {
            try (InputStream is = new FileInputStream(externalFile)) {
                props.load(is);
                loaded = true;
            } catch (Exception ignored) {
            }
        }

        // Ưu tiên 2: Đọc từ classpath resources
        if (!loaded) {
            try (InputStream is = DatabaseConnection.class.getClassLoader().getResourceAsStream("db.properties")) {
                if (is != null) {
                    props.load(is);
                    loaded = true;
                }
            } catch (Exception ignored) {
            }
        }

        if (loaded) {
            if (props.getProperty("db.url") != null) {
                dbUrl = props.getProperty("db.url").trim();
            }
            if (props.getProperty("db.user") != null) {
                dbUser = props.getProperty("db.user").trim();
            }
            if (props.getProperty("db.password") != null) {
                dbPassword = props.getProperty("db.password").trim();
            }
        }

        // Tải JDBC Driver
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            System.err.println("Cảnh báo: Không tìm thấy MySQL JDBC Driver!");
        }
    }

    /**
     * Lấy kết nối tới CSDL QuanLySinhVien.
     */
    public static Connection getConnection() throws SQLException {
        try {
            return DriverManager.getConnection(dbUrl, dbUser, dbPassword);
        } catch (SQLException ex) {
            // Nếu lỗi do database QuanLySinhVien chưa tồn tại (mã lỗi 1049 - Unknown database)
            if (ex.getErrorCode() == 1049 || (ex.getMessage() != null && ex.getMessage().contains("Unknown database"))) {
                initializeDatabase();
                return DriverManager.getConnection(dbUrl, dbUser, dbPassword);
            }
            throw new SQLException("Không thể kết nối đến MySQL: " + ex.getMessage() + 
                    "\nVui lòng kiểm tra lại MySQL Server (localhost:3306) và thông tin đăng nhập trong file db.properties.", ex);
        }
    }

    /**
     * Tự động khởi tạo database QuanLySinhVien và bảng SinhVien nếu chưa tồn tại
     */
    public static synchronized void initializeDatabase() {
        try (Connection rootConn = DriverManager.getConnection(getRootUrl(), dbUser, dbPassword);
             Statement stmt = rootConn.createStatement()) {

            // Tạo Database nếu chưa có
            stmt.executeUpdate("CREATE DATABASE IF NOT EXISTS " + DB_NAME + " CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
            stmt.executeUpdate("USE " + DB_NAME);

            // Tạo Bảng SinhVien nếu chưa có
            String createTableSql = "CREATE TABLE IF NOT EXISTS SinhVien (" +
                    "MaSV VARCHAR(10) NOT NULL PRIMARY KEY, " +
                    "HoTen VARCHAR(50) NOT NULL, " +
                    "Lop VARCHAR(20) NOT NULL, " +
                    "NgaySinh DATE NOT NULL, " +
                    "DiemTB FLOAT NOT NULL" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci";
            stmt.executeUpdate(createTableSql);

            // Kiểm tra số lượng bản ghi mẫu
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM SinhVien");
            int count = 0;
            if (rs.next()) {
                count = rs.getInt(1);
            }
            rs.close();

            // Nếu bảng rỗng, chèn 10 bản ghi mẫu phục vụ kiểm thử
            if (count == 0) {
                stmt.executeUpdate("INSERT INTO SinhVien (MaSV, HoTen, Lop, NgaySinh, DiemTB) VALUES " +
                        "('SV001', 'Nguyễn Văn An', 'CNTT1', '2003-05-15', 8.5), " +
                        "('SV002', 'Trần Thị Bích', 'CNTT1', '2003-08-20', 9.2), " +
                        "('SV003', 'Lê Hoàng Cường', 'CNTT2', '2003-01-10', 7.0), " +
                        "('SV004', 'Phạm Minh Đức', 'CNTT2', '2003-11-25', 6.5), " +
                        "('SV005', 'Hoàng Thu Hà', 'KTPM1', '2003-03-30', 8.8), " +
                        "('SV006', 'Đỗ Tuấn Hải', 'KTPM1', '2003-07-12', 7.8), " +
                        "('SV007', 'Vũ Thị Mai', 'HTTT1', '2003-09-05', 9.5), " +
                        "('SV008', 'Bùi Quang Nam', 'HTTT1', '2003-12-18', 5.5), " +
                        "('SV009', 'Ngô Phương Oanh', 'CNTT1', '2003-04-22', 8.0), " +
                        "('SV010', 'Đặng Quốc Việt', 'CNTT2', '2003-10-08', 9.0)");
            }
        } catch (SQLException e) {
            System.err.println("Lỗi khi tự động khởi tạo CSDL: " + e.getMessage());
        }
    }
}

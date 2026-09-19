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

        // Ưu tiên cao nhất: Đọc từ Biến môi trường và System Properties (Bảo mật, không lo lộ lên Git)
        String envUrl = System.getenv("DB_URL");
        if (envUrl != null && !envUrl.trim().isEmpty()) {
            dbUrl = envUrl.trim();
        }
        String sysUrl = System.getProperty("db.url");
        if (sysUrl != null && !sysUrl.trim().isEmpty()) {
            dbUrl = sysUrl.trim();
        }

        String envUser = System.getenv("DB_USER");
        if (envUser != null && !envUser.trim().isEmpty()) {
            dbUser = envUser.trim();
        }
        String sysUser = System.getProperty("db.user");
        if (sysUser != null && !sysUser.trim().isEmpty()) {
            dbUser = sysUser.trim();
        }

        String envPass = System.getenv("DB_PASSWORD");
        if (envPass != null) {
            dbPassword = envPass.trim();
        }
        String sysPass = System.getProperty("db.password");
        if (sysPass != null) {
            dbPassword = sysPass.trim();
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

            // Tạo Bảng SinhVien nếu chưa có
            String createTableSql = "CREATE TABLE IF NOT EXISTS " + DB_NAME + ".SinhVien (" +
                    "MaSV VARCHAR(10) NOT NULL PRIMARY KEY, " +
                    "HoTen VARCHAR(50) NOT NULL, " +
                    "Lop VARCHAR(20) NOT NULL, " +
                    "NgaySinh DATE NOT NULL, " +
                    "DiemTB FLOAT NOT NULL" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci";
            stmt.executeUpdate(createTableSql);

            // Kiểm tra số lượng bản ghi
            int count = 0;
            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM " + DB_NAME + ".SinhVien")) {
                if (rs.next()) {
                    count = rs.getInt(1);
                }
            }

            // Nếu bảng rỗng, nạp dữ liệu từ file sinhvien.txt hoặc classpath resource nếu có
            if (count == 0) {
                InputStream is = null;
                File txtFile = new File("sinhvien.txt");
                if (txtFile.exists() && txtFile.isFile()) {
                    try {
                        is = new FileInputStream(txtFile);
                    } catch (Exception ignored) {
                    }
                }
                if (is == null) {
                    is = DatabaseConnection.class.getClassLoader().getResourceAsStream("sinhvien.txt");
                }
                if (is != null) {
                    String insertSql = "INSERT INTO " + DB_NAME + ".SinhVien (MaSV, HoTen, Lop, NgaySinh, DiemTB) VALUES (?, ?, ?, ?, ?)";
                    try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(
                            is, java.nio.charset.StandardCharsets.UTF_8));
                         java.sql.PreparedStatement ps = rootConn.prepareStatement(insertSql)) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            String trimmed = line.trim();
                            if (trimmed.startsWith("\uFEFF")) trimmed = trimmed.substring(1).trim();
                            if (trimmed.isEmpty() || trimmed.startsWith("#") || trimmed.startsWith("//")) continue;
                            String[] parts = trimmed.split("\\|", -1);
                            if (parts.length >= 5) {
                                ps.setString(1, parts[0].trim());
                                ps.setString(2, parts[1].trim());
                                ps.setString(3, parts[2].trim());
                                ps.setDate(4, java.sql.Date.valueOf(parts[3].trim()));
                                ps.setFloat(5, Float.parseFloat(parts[4].trim().replace(',', '.')));
                                ps.addBatch();
                            }
                        }
                        ps.executeBatch();
                    } catch (Exception e) {
                        System.err.println("Lỗi khi nạp dữ liệu từ sinhvien.txt: " + e.getMessage());
                    }
                }
            }
            // Tạo bảng MonHoc / Diem cho tính năng bảng điểm theo môn (hướng A)
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS " + DB_NAME + ".MonHoc ("
                    + "MaMH VARCHAR(10) NOT NULL PRIMARY KEY, "
                    + "TenMH VARCHAR(100) NOT NULL, "
                    + "SoTC TINYINT NOT NULL DEFAULT 4"
                    + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci");
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS " + DB_NAME + ".Diem ("
                    + "MaSV VARCHAR(10) NOT NULL, "
                    + "MaMH VARCHAR(10) NOT NULL, "
                    + "DiemBaoCao FLOAT NOT NULL, "
                    + "DiemChuyenCan FLOAT NOT NULL, "
                    + "DiemCuoiKy FLOAT NOT NULL, "
                    + "PRIMARY KEY (MaSV, MaMH), "
                    + "FOREIGN KEY (MaSV) REFERENCES " + DB_NAME + ".SinhVien(MaSV) "
                    + "ON DELETE CASCADE ON UPDATE CASCADE, "
                    + "FOREIGN KEY (MaMH) REFERENCES " + DB_NAME + ".MonHoc(MaMH) "
                    + "ON DELETE RESTRICT ON UPDATE CASCADE"
                    + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci");
            try {
                stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_diem_masv ON "
                        + DB_NAME + ".Diem (MaSV)");
            } catch (SQLException ignored) {
                // MySQL cũ không hỗ trợ IF NOT EXISTS cho index — bỏ qua, không chặn khởi động
            }
            try {
                stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_diem_mamh ON "
                        + DB_NAME + ".Diem (MaMH)");
            } catch (SQLException ignored) {
                // Bỏ qua tương tự
            }
        } catch (SQLException e) {
            System.err.println("Lỗi khi tự động khởi tạo CSDL: " + e.getMessage());
            return;
        }
        // Seed master môn học + điểm random 4-7 môn/SV (chỉ chạy 1 lần, ngoài synchronized rootConn
        // để dùng đúng connection DB targets và transaction riêng; lỗi seed không chặn app).
        try {
            new MonHocDAO().ensureSeeded();
            new DiemDAO().seedIfEmpty();
            new DiemDAO().syncTichLuyAll();
        } catch (Exception e) {
            System.err.println("Lỗi khi seed bảng điểm: " + e.getMessage());
        }
    }
}

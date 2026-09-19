package com.quanlysinhvien.dao;

import com.quanlysinhvien.model.Diem;
import com.quanlysinhvien.model.DiemCalculator;
import com.quanlysinhvien.model.DiemDetail;
import com.quanlysinhvien.model.MonHoc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Điểm thành phần theo môn (MaSV, MaMH) + seeder random 4-7 môn/SV.
 */
public class DiemDAO {

    public List<DiemDetail> getByMaSV(String maSV) throws SQLException {
        List<DiemDetail> list = new ArrayList<>();
        String sql = "SELECT d.MaSV, d.MaMH, m.TenMH, m.SoTC, "
                + "d.DiemBaoCao, d.DiemChuyenCan, d.DiemCuoiKy "
                + "FROM Diem d JOIN MonHoc m ON d.MaMH = m.MaMH "
                + "WHERE d.MaSV = ? ORDER BY d.MaMH ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, maSV.trim());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new DiemDetail(
                            rs.getString("MaSV"), rs.getString("MaMH"),
                            rs.getString("TenMH"), rs.getInt("SoTC"),
                            rs.getFloat("DiemBaoCao"), rs.getFloat("DiemChuyenCan"),
                            rs.getFloat("DiemCuoiKy")));
                }
            }
        }
        return list;
    }

    public int countAll() throws SQLException {
        String sql = "SELECT COUNT(*) FROM Diem";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }

    public void upsert(Diem d) throws SQLException {
        upsertBatch(java.util.Collections.singletonList(d));
    }

    public void upsertBatch(List<Diem> list) throws SQLException {
        if (list == null || list.isEmpty()) {
            return;
        }
        String sql = "INSERT INTO Diem (MaSV, MaMH, DiemBaoCao, DiemChuyenCan, DiemCuoiKy) "
                + "VALUES (?, ?, ?, ?, ?) "
                + "ON DUPLICATE KEY UPDATE DiemBaoCao = VALUES(DiemBaoCao), "
                + "DiemChuyenCan = VALUES(DiemChuyenCan), DiemCuoiKy = VALUES(DiemCuoiKy)";
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                for (Diem d : list) {
                    DiemCalculator.validateDiem(d.getDiemBaoCao(), "Điểm báo cáo");
                    DiemCalculator.validateDiem(d.getDiemChuyenCan(), "Điểm chuyên cần");
                    DiemCalculator.validateDiem(d.getDiemCuoiKy(), "Điểm cuối kỳ");
                    ps.setString(1, d.getMaSV().trim());
                    ps.setString(2, d.getMaMH().trim());
                    ps.setFloat(3, d.getDiemBaoCao());
                    ps.setFloat(4, d.getDiemChuyenCan());
                    ps.setFloat(5, d.getDiemCuoiKy());
                    ps.addBatch();
                }
                ps.executeBatch();
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    public boolean delete(String maSV, String maMH) throws SQLException {
        String sql = "DELETE FROM Diem WHERE MaSV = ? AND MaMH = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, maSV.trim());
            ps.setString(2, maMH.trim());
            return ps.executeUpdate() > 0;
        }
    }

    public void deleteByMaSV(String maSV) throws SQLException {
        String sql = "DELETE FROM Diem WHERE MaSV = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, maSV.trim());
            ps.executeUpdate();
        }
    }

    /**
     * Lưu toàn bộ bảng điểm 1 SV trong đúng 1 transaction duy nhất:
     * xóa môn đã gỡ + upsert batch + ghi đè SinhVien.DiemTB.
     * Dialog phải gọi hàm này thay vì gọi 3 DAO rời rạc (tránh dở dang khi 1 bước lỗi).
     */
    public void saveBangDiem(String maSV, List<Diem> toSave, double tbTichLuy) throws SQLException {
        if (toSave == null || toSave.isEmpty()) {
            throw new IllegalArgumentException("Bảng điểm trống, thêm ít nhất 1 môn!");
        }
        String deleteSql = "DELETE FROM Diem WHERE MaSV = ? AND MaMH = ?";
        String upsertSql = "INSERT INTO Diem (MaSV, MaMH, DiemBaoCao, DiemChuyenCan, DiemCuoiKy) "
                + "VALUES (?, ?, ?, ?, ?) "
                + "ON DUPLICATE KEY UPDATE DiemBaoCao = VALUES(DiemBaoCao), "
                + "DiemChuyenCan = VALUES(DiemChuyenCan), DiemCuoiKy = VALUES(DiemCuoiKy)";
        String updateTbSql = "UPDATE SinhVien SET DiemTB = ? WHERE MaSV = ?";
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement psDel = conn.prepareStatement(deleteSql);
                 PreparedStatement psUp = conn.prepareStatement(upsertSql);
                 PreparedStatement psTb = conn.prepareStatement(updateTbSql);
                 Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery(
                         "SELECT MaMH FROM Diem WHERE MaSV = '" + escape(maSV.trim()) + "'")) {
                Set<String> keep = new java.util.HashSet<>();
                for (Diem d : toSave) {
                    DiemCalculator.validateDiem(d.getDiemBaoCao(), "Điểm báo cáo");
                    DiemCalculator.validateDiem(d.getDiemChuyenCan(), "Điểm chuyên cần");
                    DiemCalculator.validateDiem(d.getDiemCuoiKy(), "Điểm cuối kỳ");
                    keep.add(d.getMaMH().trim());
                }
                while (rs.next()) {
                    String oldMH = rs.getString(1);
                    if (!keep.contains(oldMH)) {
                        psDel.setString(1, maSV.trim());
                        psDel.setString(2, oldMH);
                        psDel.addBatch();
                    }
                }
                psDel.executeBatch();
                for (Diem d : toSave) {
                    psUp.setString(1, d.getMaSV().trim());
                    psUp.setString(2, d.getMaMH().trim());
                    psUp.setFloat(3, d.getDiemBaoCao());
                    psUp.setFloat(4, d.getDiemChuyenCan());
                    psUp.setFloat(5, d.getDiemCuoiKy());
                    psUp.addBatch();
                }
                psUp.executeBatch();
                psTb.setFloat(1, (float) tbTichLuy);
                psTb.setString(2, maSV.trim());
                psTb.executeUpdate();
                conn.commit();
            } catch (SQLException | IllegalArgumentException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    private static String escape(String s) {
        return s.replace("\\", "\\\\").replace("'", "\\'");
    }

    /** TB tích lũy chuẩn học vụ = Σ(DiemMon × TC) / Σ(TC), round2. Rỗng -> 0. */
    public double calcTichLuy(String maSV) throws SQLException {
        List<DiemDetail> list = getByMaSV(maSV);
        List<Double> mons = new ArrayList<>();
        List<Integer> tcs = new ArrayList<>();
        for (DiemDetail d : list) {
            mons.add(d.getDiemMon());
            tcs.add(d.getSoTC());
        }
        return DiemCalculator.diemTBTinChi(mons, tcs);
    }

    /**
     * Đồng bộ DiemTB cho mọi SV đã có dòng Diem (chạy mỗi lần khởi động, idempotent).
     * Dùng khi đổi công thức (trung bình cộng -> trọng số tín chỉ): TB là dữ liệu suy ra,
     * tính lại không mất thông tin điểm thành phần.
     */
    public void syncTichLuyAll() throws SQLException {
        String distinctSql = "SELECT DISTINCT MaSV FROM Diem";
        String updateSql = "UPDATE SinhVien SET DiemTB = ? WHERE MaSV = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(distinctSql);
             PreparedStatement ps = conn.prepareStatement(updateSql)) {
            List<String> ids = new ArrayList<>();
            while (rs.next()) {
                ids.add(rs.getString(1));
            }
            for (String id : ids) {
                ps.setFloat(1, (float) calcTichLuy(id));
                ps.setString(2, id);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    /**
     * Seed 1 lần duy nhất: mỗi SV random 4-7 môn distinct + điểm thực tế,
     * sau đó ghi đè SinhVien.DiemTB = AVG. Toàn bộ trong 1 transaction.
     * Nếu Diem đã có dữ liệu thì bỏ qua (không đè điểm người dùng đã sửa).
     */
    public void seedIfEmpty() throws SQLException {
        if (countAll() > 0) {
            return;
        }
        // Lấy đủ thông tin kết nối hiện tại để mở 1 connection dùng chung cho seed
        // (tái dùng cùng URL/user/pass mà DatabaseConnection đang dùng).
        try (Connection conn = DatabaseConnection.getConnection()) {
            // 1. Danh sách SV
            List<String> allMaSV = new ArrayList<>();
            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery("SELECT MaSV FROM SinhVien ORDER BY MaSV ASC")) {
                while (rs.next()) {
                    allMaSV.add(rs.getString(1));
                }
            }
            if (allMaSV.isEmpty()) {
                return;
            }
            // 2. Danh sách môn
            List<MonHoc> allMH = new MonHocDAO().getAll();
            if (allMH.isEmpty()) {
                return;
            }
            conn.setAutoCommit(false);
            String insertSql = "INSERT INTO Diem (MaSV, MaMH, DiemBaoCao, DiemChuyenCan, DiemCuoiKy) "
                    + "VALUES (?, ?, ?, ?, ?) "
                    + "ON DUPLICATE KEY UPDATE DiemBaoCao = VALUES(DiemBaoCao), "
                    + "DiemChuyenCan = VALUES(DiemChuyenCan), DiemCuoiKy = VALUES(DiemCuoiKy)";
            String updateTbSql = "UPDATE SinhVien SET DiemTB = ? WHERE MaSV = ?";
            try (PreparedStatement psIns = conn.prepareStatement(insertSql);
                 PreparedStatement psUpd = conn.prepareStatement(updateTbSql)) {
                ThreadLocalRandom rnd = ThreadLocalRandom.current();
                for (String maSV : allMaSV) {
                    List<MonHoc> shuffled = new ArrayList<>(allMH);
                    Collections.shuffle(shuffled, new java.util.Random(rnd.nextLong()));
                    int n = 4 + rnd.nextInt(4); // 4-7
                    n = Math.min(n, shuffled.size());
                    List<Double> mons = new ArrayList<>();
                    List<Integer> tcs = new ArrayList<>();
                    for (int i = 0; i < n; i++) {
                        MonHoc mh = shuffled.get(i);
                        float cc = round1(7 + rnd.nextDouble() * 3);   // chuyên cần 7-10
                        float bc = round1(5 + rnd.nextDouble() * 5);   // báo cáo 5-10
                        float ck = round1(4 + rnd.nextDouble() * 6);   // cuối kỳ 4-10
                        psIns.setString(1, maSV);
                        psIns.setString(2, mh.getMaMH());
                        psIns.setFloat(3, bc);
                        psIns.setFloat(4, cc);
                        psIns.setFloat(5, ck);
                        psIns.addBatch();
                        mons.add(DiemCalculator.diemMon(bc, cc, ck));
                        tcs.add(mh.getSoTC());
                    }
                    psUpd.setFloat(1, (float) DiemCalculator.diemTBTinChi(mons, tcs));
                    psUpd.setString(2, maSV);
                    psUpd.addBatch();
                }
                psIns.executeBatch();
                psUpd.executeBatch();
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    private static float round1(double x) {
        float v = (float) (Math.round(x * 10.0) / 10.0);
        if (v < 0f) return 0f;
        if (v > 10f) return 10f;
        return v;
    }

}
